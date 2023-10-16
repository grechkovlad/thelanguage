package codegen

import ClassKind
import ModifierType
import ir.*
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.jetbrains.org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

private val String.fqNameToJvmDescriptor get() = "L$this;"

private val TypeReference.jvmDescriptor: String
    get() = when (this) {
        IntTypeReference -> "I"
        is ArrayTypeReference -> "[${componentType.jvmDescriptor}"
        BoolTypeReference -> "Z"
        ObjectClassReference -> ObjectClassReference.jvmName.fqNameToJvmDescriptor
        StringClassReference -> StringClassReference.jvmName.fqNameToJvmDescriptor
        SystemClassReference -> SystemClassReference.jvmName.fqNameToJvmDescriptor
        is UserClassReference -> name.fqNameToJvmDescriptor
        UtilsClassReference -> error("Utils class has no runtime counterpart")
        FloatTypeReference -> "F"
        NullTypeReference -> error("null type has no jvm descriptor")
        VoidTypeReference -> "V"
    }

private val MethodReference.jvmDescriptor: String
    get() {
        return "(${parameterTypes.joinToString(separator = "") { it.jvmDescriptor }})${returnType.jvmDescriptor}"
    }

private val ConstructorReference.jvmDescriptor: String
    get() {
        return "(${parameterTypes.joinToString(separator = "") { it.jvmDescriptor }})V"
    }

private val ModifiersBearer.asmModifier: Int
    get() = effectiveAccessModifier.asAsm or modifiers.sumOf {
        when (it) {
            ModifierType.STATIC -> Opcodes.ACC_STATIC
            ModifierType.PUBLIC -> 0
            ModifierType.PRIVATE -> 0
            ModifierType.PROTECTED -> 0
            ModifierType.ABSTRACT -> Opcodes.ACC_ABSTRACT
        }
    }


private val ModifierType.asAsm: Int
    get() = when (this) {
        ModifierType.STATIC -> Opcodes.ACC_STATIC
        ModifierType.PUBLIC -> Opcodes.ACC_PUBLIC
        ModifierType.PRIVATE -> Opcodes.ACC_PRIVATE
        ModifierType.PROTECTED -> Opcodes.ACC_PROTECTED
        ModifierType.ABSTRACT -> Opcodes.ACC_ABSTRACT
    }

private val ClassReference.jvmName: String
    get() = when (this) {
        ObjectClassReference -> "java/lang/Object"
        StringClassReference -> "java/lang/String"
        SystemClassReference -> "java/lang/System"
        is UserClassReference -> this.name
        UtilsClassReference -> error("Utils class does not map to jvm class")
    }

private const val JVM_INIT = "<init>"
private const val STRING_BUILDER_FQN = "java/lang/StringBuilder"
private const val STRING_BUILDER_DESCRIPTOR = "L$STRING_BUILDER_FQN;"


class CodeGenerator(val project: Project) {
    fun generate(): Map<String, ClassWriter> {
        return project.classes.associate { it.name to generateClass(it) }
    }

    fun loadClass(name: String) = when (name) {
        "java/lang/Object" -> ObjectClassReference
        "java/lang/String" -> StringClassReference
        "java/lang/System" -> SystemClassReference
        "Utils" -> UtilsClassReference
        else -> project.classes.singleOrNull { it.name == name }?.reference ?: error("Class not found: $name")
    }

    private fun generateClass(clazz: ClassDeclaration): ClassWriter {
        val classWriter = object : ClassWriter(COMPUTE_MAXS or COMPUTE_FRAMES) {
            override fun getCommonSuperClass(type1: String, type2: String): String {
                val class1 = loadClass(type1)
                val class2 = loadClass(type2)
                if (class1.isSubtypeOf(class2)) return class2.jvmName
                if (class2.isSubtypeOf(class1)) return class1.jvmName
                if (class1.isInterface || class2.isInterface) return ObjectClassReference.jvmName
                return getCommonSuperClass(class1.superClass.jvmName, type2)
            }
        }
        val accessFlags = buildList {
            add(Opcodes.ACC_PUBLIC)
            if (clazz.isAbstract) {
                add(Opcodes.ACC_ABSTRACT)
            }
            if (clazz.reference.isInterface) {
                add(Opcodes.ACC_ABSTRACT)
                add(Opcodes.ACC_INTERFACE)
            }
        }.sum()
        classWriter.visit(
            Opcodes.V1_8,
            accessFlags,
            clazz.name,
            null,
            clazz.superClass.jvmName,
            clazz.interfaces.map { it.jvmName }.toTypedArray()
        )
        clazz.declaredMethods.forEach { generateMethod(it, classWriter) }
        clazz.fields.forEach { generateFieldDeclaration(it, classWriter) }
        clazz.constructors.forEach { generateConstructor(clazz, it, classWriter) }
        generateStaticInitBlock(clazz, classWriter)
        return classWriter.also { classWriter.visitEnd() }
    }

    private fun generateConstructor(
        clazz: ClassDeclaration, constructor: ConstructorDeclaration, classWriter: ClassWriter
    ) {
        val anyCode = clazz.fields.any { !it.isStatic && it.initializer != null } || constructor.body.isNotEmpty()
        if (!anyCode) return
        val methodVisitor = classWriter.visitMethod(
            constructor.asmModifier, JVM_INIT, constructor.reference.jvmDescriptor, null, emptyArray()
        )
        val instructionAdapter = InstructionAdapter(methodVisitor)
        val context = Context(constructor)
        require(constructor.body.isNotEmpty())
        val firstStatement = constructor.body.first()
        require(firstStatement is ExpressionStatement)
        require(firstStatement.expression is SuperCall)
        generateStatement(firstStatement, instructionAdapter, context)
        clazz.fields.filter { !it.isStatic && it.initializer != null }
            .forEach { generateFieldInitialization(it, instructionAdapter, context) }
        generateStatements(constructor.body.drop(1), instructionAdapter, context)
        instructionAdapter.areturn(Type.VOID_TYPE)
        methodVisitor.visitMaxs(-1, -1)
        methodVisitor.visitEnd()
    }

    private fun generateFieldInitialization(
        field: FieldDeclaration, instructionAdapter: InstructionAdapter, context: Context
    ) {
        instructionAdapter.load(0, field.declaringClass.asmType)
        generateExpression(field.initializer!!, instructionAdapter, context)
        instructionAdapter.putfield(field.declaringClass.jvmName, field.name, field.type.jvmDescriptor)
    }

    private fun generateStaticInitBlock(clazz: ClassDeclaration, classWriter: ClassWriter) {
        if (clazz.kind == ClassKind.INTERFACE) return
        val anyCode =
            clazz.fields.any { it.isStatic && it.initializer != null } || clazz.staticInit.body.isNotEmpty()
        if (!anyCode) return
        val staticInitBlock = classWriter.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, emptyArray())
        val instructionAdapter = InstructionAdapter(staticInitBlock)
        val context = Context(clazz.staticInit)
        clazz.fields.filter { it.isStatic && it.initializer != null }
            .forEach { generateStaticFieldInitialization(it, instructionAdapter, context) }
        generateStatements(clazz.staticInit.body, instructionAdapter, context)
        instructionAdapter.areturn(Type.VOID_TYPE)
        staticInitBlock.visitMaxs(-1, -1)
        staticInitBlock.visitEnd()
    }

    private fun generateStaticFieldInitialization(
        field: FieldDeclaration, instructionAdapter: InstructionAdapter, context: Context
    ) {
        generateExpression(field.initializer!!, instructionAdapter, context)
        instructionAdapter.putstatic(field.declaringClass.jvmName, field.name, field.type.jvmDescriptor)
    }

    private fun generateFieldDeclaration(field: FieldDeclaration, classWriter: ClassWriter) {
        classWriter.visitField(field.reference.asmModifier, field.name, field.type.jvmDescriptor, null, null)
    }

    private fun generateMethod(methodDeclaration: MethodDeclaration, classWriter: ClassWriter) {
        val methodVisitor = classWriter.visitMethod(
            methodDeclaration.reference.asmModifier,
            methodDeclaration.name,
            methodDeclaration.reference.jvmDescriptor,
            null,
            emptyArray()
        )
        if (methodDeclaration.reference.isAbstract) return
        val adapter = InstructionAdapter(methodVisitor)
        val context = Context(methodDeclaration)
        generateMethodBody(methodDeclaration, adapter, context)
        methodVisitor.visitMaxs(-1, -1)
        methodVisitor.visitEnd()
    }

    private fun generateMethodBody(
        methodDeclaration: MethodDeclaration, adapter: InstructionAdapter, context: Context
    ) {
        generateStatements(methodDeclaration.body, adapter, context)
        if (methodDeclaration.returnType == VoidTypeReference) adapter.areturn(Type.VOID_TYPE)
    }

    private fun generateStatements(statements: List<Statement>, adapter: InstructionAdapter, context: Context) =
        statements.forEach { generateStatement(it, adapter, context) }

    private fun generateStatement(statement: Statement, adapter: InstructionAdapter, context: Context) =
        when (statement) {
            Break -> generateBreak(adapter, context)
            Continue -> generateContinue(adapter, context)
            is ExpressionStatement -> generateExpressionStatement(statement, adapter, context)
            is For -> generateFor(statement, adapter, context)
            is If -> generateIf(statement, adapter, context)
            is LocalVariableDeclaration -> generateLocalVariableDeclaration(statement, adapter, context)
            is Return -> generateReturn(statement, adapter, context)
            is SetArrayElement -> generateSetArrayElement(statement, adapter, context)
            is SetField -> generateSetField(statement, adapter, context)
            is SetVariable -> setVariable(statement, adapter, context)
            is While -> generateWhile(statement, adapter, context)
        }

    private fun generateBreak(adapter: InstructionAdapter, context: Context) {
        adapter.goTo(context.breakLabel)
    }

    private fun generateContinue(adapter: InstructionAdapter, context: Context) {
        adapter.goTo(context.continueLabel)
    }

    private fun generateWhile(whileStatement: While, adapter: InstructionAdapter, context: Context) {
        val startLabel = Label()
        val endLabel = Label()
        adapter.visitLabel(startLabel)
        generateExpression(whileStatement.condition, adapter, context)
        adapter.visitJumpInsn(Opcodes.IFEQ, endLabel)
        context.enterLoop(startLabel, endLabel)
        generateStatements(whileStatement.body, adapter, context)
        adapter.goTo(startLabel)
        context.exitLoop()
        adapter.visitLabel(endLabel)
    }

    private fun generateFor(forStatement: For, adapter: InstructionAdapter, context: Context) {
        generateStatement(forStatement.init, adapter, context)
        val startLabel = Label()
        val updateLabel = Label()
        val endLabel = Label()
        adapter.visitLabel(startLabel)
        generateExpression(forStatement.condition, adapter, context)
        adapter.visitJumpInsn(Opcodes.IFEQ, endLabel)
        context.enterLoop(updateLabel, endLabel)
        generateStatements(forStatement.body, adapter, context)
        adapter.visitLabel(updateLabel)
        generateStatement(forStatement.update, adapter, context)
        adapter.goTo(startLabel)
        context.exitLoop()
        adapter.visitLabel(endLabel)
    }

    private fun generateIf(ifStatement: If, adapter: InstructionAdapter, context: Context) {
        if (ifStatement.elseStatements.isEmpty()) {
            val afterIfLabel = Label()
            generateExpression(ifStatement.condition, adapter, context)
            adapter.visitJumpInsn(Opcodes.IFEQ, afterIfLabel)
            generateStatements(ifStatement.thenStatements, adapter, context)
            adapter.visitLabel(afterIfLabel)
        } else {
            val elseLabel = Label()
            val afterIfLabel = Label()
            generateExpression(ifStatement.condition, adapter, context)
            adapter.visitJumpInsn(Opcodes.IFEQ, elseLabel)
            generateStatements(ifStatement.thenStatements, adapter, context)
            adapter.goTo(afterIfLabel)
            adapter.visitLabel(elseLabel)
            generateStatements(ifStatement.elseStatements, adapter, context)
            adapter.visitLabel(afterIfLabel)
        }
    }

    private fun generateSetArrayElement(
        setArrayElement: SetArrayElement, adapter: InstructionAdapter, context: Context
    ) {
        generateExpression(setArrayElement.target, adapter, context)
        generateExpression(setArrayElement.index, adapter, context)
        generateExpression(setArrayElement.value, adapter, context)
        val arrayType = setArrayElement.target.type
        require(arrayType is ArrayTypeReference)
        adapter.astore(arrayType.componentType.asmType)
    }

    private fun generateLocalVariableDeclaration(
        localVariable: LocalVariableDeclaration, adapter: InstructionAdapter, context: Context
    ) {
        if (localVariable.initializer == null) return
        generateExpression(localVariable.initializer, adapter, context)
        adapter.store(context.localIndices[localVariable]!!, localVariable.type.asmType)
    }

    private fun setVariable(setVariable: SetVariable, adapter: InstructionAdapter, context: Context) {
        generateExpression(setVariable.expression, adapter, context)
        adapter.store(context.localIndices[setVariable.variable]!!, setVariable.variable.type.asmType)
    }

    private fun generateSetField(setField: SetField, adapter: InstructionAdapter, context: Context) {
        if (!setField.fieldReference.isStatic) generateExpression(setField.target, adapter, context)
        generateExpression(setField.value, adapter, context)

        val owner = setField.fieldReference.declaringClass.jvmName
        val name = setField.fieldReference.name
        val descriptor = setField.fieldReference.fieldType.jvmDescriptor

        if (setField.fieldReference.isStatic) {
            adapter.putstatic(owner, name, descriptor)
        } else {
            adapter.putfield(owner, name, descriptor)
        }
    }

    private fun generateExpressionStatement(
        expressionStatement: ExpressionStatement, adapter: InstructionAdapter, context: Context
    ) {
        generateExpression(expressionStatement.expression, adapter, context)
        if (expressionStatement.expression.type != VoidTypeReference) adapter.pop()
    }

    private val Expression.asmType get() = Type.getType(type.jvmDescriptor)

    private val TypeReference.asmType get() = Type.getType(this.jvmDescriptor)

    private fun generateExpression(expression: Expression, adapter: InstructionAdapter, context: Context) =
        when (expression) {
            is ArrayLength -> generateArrayLength(expression, adapter, context)
            is Add -> generateSimpleBinaryOp(expression, adapter, context) { adapter.add(expression.asmType) }

            is And -> generateShortcutBoolOperation(expression, adapter, context, InstructionAdapter::ifeq, false)
            is BoolEq -> generateNoShortcutBoolOperation(expression, adapter, context, InstructionAdapter::ificmpne)

            is Concat -> generateConcat(expression, adapter, context)
            is Divide -> generateSimpleBinaryOp(expression, adapter, context) { adapter.div(expression.asmType) }

            is Geq -> generateGeq(expression, adapter, context)
            is Greater -> generateGreater(expression, adapter, context)
            is Leq -> generateLeq(expression, adapter, context)
            is Less -> generateLess(expression, adapter, context)
            is Multiply -> generateSimpleBinaryOp(expression, adapter, context) { adapter.mul(expression.asmType) }

            is NumericEq -> generateNumericEq(expression, adapter, context)
            is Or -> generateShortcutBoolOperation(expression, adapter, context, InstructionAdapter::ifne, true)
            is RefEq -> generateNoShortcutBoolOperation(expression, adapter, context, InstructionAdapter::ifacmpne)
            is Subtract -> generateSimpleBinaryOp(expression, adapter, context) { adapter.sub(expression.asmType) }

            FalseLiteral -> adapter.iconst(0)
            TrueLiteral -> adapter.iconst(1)
            is ConstructorCall -> generateConstructorCall(expression, adapter, context)
            is MethodCall -> generateMethodCall(expression, adapter, context)
            is SuperCall -> generateSuperCall(expression, adapter, context)
            is CreateArray -> generateCreateArray(expression, adapter, context)
            is FloatLiteral -> adapter.fconst(expression.value)
            is GetArrayElement -> generateGetArrayElement(expression, adapter, context)
            is GetField -> generateField(expression, adapter, context)
            is GetVariable -> generateGetVariable(expression, adapter, context)
            is IntLiteral -> adapter.iconst(expression.value)
            is Minus -> generateMinus(expression, adapter, context)
            is Not -> generateNot(expression, adapter, context)
            Null -> adapter.aconst(null)
            is StringLiteral -> adapter.aconst(expression.value)
            is ThisAccess -> adapter.load(0, context.method.declaringClass.reference.asmType)
            is TypeAccess -> error("Type access should not be evaluated")
        }

    private fun generateCreateArray(
        creatArray: CreateArray,
        adapter: InstructionAdapter,
        context: Context
    ) {
        require(creatArray.dimensions.isNotEmpty())
        creatArray.dimensions.forEach { generateExpression(it, adapter, context) }
        if (creatArray.dimensions.size == 1) {
            adapter.newarray(creatArray.elementType.asmType)
        } else {
            adapter.multianewarray(creatArray.type.jvmDescriptor, creatArray.dimensions.size)
        }
    }

    private fun generateMethodCall(
        methodCall: MethodCall,
        adapter: InstructionAdapter,
        context: Context
    ) {
        if (methodCall.method.isStatic) {
            generateStaticMethodCall(methodCall, adapter, context)
        } else {
            generateInstanceMethodCall(methodCall, adapter, context)
        }
    }

    private fun generateStaticMethodCall(
        methodCall: MethodCall,
        adapter: InstructionAdapter,
        context: Context
    ) {
        methodCall.arguments.forEach { generateExpression(it, adapter, context) }
        adapter.invokestatic(
            methodCall.method.declaringClass.jvmName,
            methodCall.method.name,
            methodCall.method.jvmDescriptor,
            false
        )
    }

    private fun generateInstanceMethodCall(
        methodCall: MethodCall,
        adapter: InstructionAdapter,
        context: Context
    ) {
        generateExpression(methodCall.target, adapter, context)
        methodCall.arguments.forEach { generateExpression(it, adapter, context) }
        val className = methodCall.method.declaringClass.jvmName
        val methodName = methodCall.method.name
        val descriptor = methodCall.method.jvmDescriptor
        if (methodCall.method.declaringClass.isInterface) {
            adapter.invokeinterface(className, methodName, descriptor)
        } else {
            adapter.invokevirtual(className, methodName, descriptor, false)
        }
    }

    private fun generateNot(not: Not, adapter: InstructionAdapter, context: Context) {
        val falseLabel = Label()
        val endLabel = Label()
        generateExpression(not.operand, adapter, context)
        adapter.ifne(falseLabel)
        adapter.iconst(1)
        adapter.goTo(endLabel)
        adapter.visitLabel(falseLabel)
        adapter.iconst(0)
        adapter.visitLabel(endLabel)
    }

    private fun generateMinus(minus: Minus, adapter: InstructionAdapter, context: Context) {
        require(minus.type == IntTypeReference || minus.type == FloatTypeReference)
        generateExpression(minus.operand, adapter, context)
        adapter.neg(minus.asmType)
    }

    private fun generateNumericEq(numericEq: NumericEq, adapter: InstructionAdapter, context: Context) {
        require(numericEq.leftOperand.type == IntTypeReference || numericEq.leftOperand.type == FloatTypeReference)
        if (numericEq.leftOperand.type == IntTypeReference) {
            generateNoShortcutBoolOperation(numericEq, adapter, context, InstructionAdapter::ificmpne)
        } else {
            generateFloatEq(numericEq, adapter, context)
        }
    }

    private fun generateLess(less: Less, adapter: InstructionAdapter, context: Context) {
        require(less.leftOperand.type == IntTypeReference || less.leftOperand.type == FloatTypeReference)
        if (less.leftOperand.type == IntTypeReference) {
            generateNoShortcutBoolOperation(less, adapter, context, InstructionAdapter::ificmpge)
        } else {
            generateFloatLess(less, adapter, context)
        }
    }

    private fun generateLeq(leq: Leq, adapter: InstructionAdapter, context: Context) {
        require(leq.leftOperand.type == IntTypeReference || leq.leftOperand.type == FloatTypeReference)
        if (leq.leftOperand.type == IntTypeReference) {
            generateNoShortcutBoolOperation(leq, adapter, context, InstructionAdapter::ificmpgt)
        } else {
            generateFloatLeq(leq, adapter, context)
        }
    }

    private fun generateGreater(greater: Greater, adapter: InstructionAdapter, context: Context) {
        require(greater.leftOperand.type == IntTypeReference || greater.leftOperand.type == FloatTypeReference)
        if (greater.leftOperand.type == IntTypeReference) {
            generateNoShortcutBoolOperation(greater, adapter, context, InstructionAdapter::ificmple)
        } else {
            generateFloatGreater(greater, adapter, context)
        }
    }

    private fun generateFloatEq(numericEq: NumericEq, adapter: InstructionAdapter, context: Context) {
        generateNoShortcutBoolOperation(
            numericEq, adapter, context, InstructionAdapter::ifne
        ) { adapterParam: InstructionAdapter -> adapterParam.cmpg(Type.FLOAT_TYPE) }
    }

    private fun generateFloatLess(less: Less, adapter: InstructionAdapter, context: Context) {
        generateNoShortcutBoolOperation(
            less, adapter, context, InstructionAdapter::ifge
        ) { adapterParam: InstructionAdapter -> adapterParam.cmpg(Type.FLOAT_TYPE) }
    }

    private fun generateFloatLeq(leq: Leq, adapter: InstructionAdapter, context: Context) {
        generateNoShortcutBoolOperation(
            leq, adapter, context, InstructionAdapter::ifgt
        ) { adapterParam: InstructionAdapter -> adapterParam.cmpg(Type.FLOAT_TYPE) }
    }

    private fun generateFloatGreater(greater: Greater, adapter: InstructionAdapter, context: Context) {
        generateNoShortcutBoolOperation(
            greater, adapter, context, InstructionAdapter::ifle
        ) { adapterParam: InstructionAdapter ->
            adapterParam.cmpl(Type.FLOAT_TYPE)
        }
    }

    private fun generateFloatGeq(geq: Geq, adapter: InstructionAdapter, context: Context) {
        generateNoShortcutBoolOperation(
            geq, adapter, context, InstructionAdapter::iflt
        ) { adapterParam: InstructionAdapter -> adapterParam.cmpl(Type.FLOAT_TYPE) }
    }

    private fun generateGeq(geq: Geq, adapter: InstructionAdapter, context: Context) {
        require(geq.leftOperand.type == IntTypeReference || geq.leftOperand.type == FloatTypeReference)
        if (geq.leftOperand.type == IntTypeReference) {
            generateNoShortcutBoolOperation(geq, adapter, context, InstructionAdapter::ificmplt)
        } else {
            generateFloatGeq(geq, adapter, context)
        }
    }

    private inline fun generateNoShortcutBoolOperation(
        binaryOperation: BinaryOperation,
        adapter: InstructionAdapter,
        context: Context,
        falseBranchJump: (InstructionAdapter, Label) -> Unit,
        additionalOperandsProcessing: (InstructionAdapter) -> Unit = {}
    ) {
        val falseLabel = Label()
        val endLabel = Label()
        generateExpression(binaryOperation.leftOperand, adapter, context)
        generateExpression(binaryOperation.rightOperand, adapter, context)
        additionalOperandsProcessing(adapter)
        falseBranchJump(adapter, falseLabel)
        adapter.iconst(1)
        adapter.goTo(endLabel)
        adapter.visitLabel(falseLabel)
        adapter.iconst(0)
        adapter.visitLabel(endLabel)
    }

    private fun generateConcat(concat: Concat, adapter: InstructionAdapter, context: Context) {

        fun invokeStringBuilderAppend() {
            adapter.invokevirtual(
                STRING_BUILDER_FQN, "append", "(${StringClassReference.jvmDescriptor})$STRING_BUILDER_DESCRIPTOR", false
            )
        }

        // Create new StringBuilder
        adapter.anew(Type.getType(STRING_BUILDER_DESCRIPTOR))
        adapter.dup()
        adapter.invokespecial(
            STRING_BUILDER_FQN, JVM_INIT, "()V", false
        )
        // Push left operand on the stack
        generateExpression(concat.leftOperand, adapter, context)
        // Append to the StringBuilder
        invokeStringBuilderAppend()
        // Push right operand on the stack
        generateExpression(concat.rightOperand, adapter, context)
        // Append to the StringBuilder
        invokeStringBuilderAppend()
        // Invoke toString
        adapter.invokevirtual(STRING_BUILDER_FQN, "toString", "()${StringClassReference.jvmDescriptor}", false)
    }

    private fun generateSimpleBinaryOp(
        binaryOperation: BinaryOperation, adapter: InstructionAdapter, context: Context, bytecode: () -> Unit
    ) {
        generateExpression(binaryOperation.leftOperand, adapter, context)
        generateExpression(binaryOperation.rightOperand, adapter, context)
        bytecode()
    }

    private fun generateShortcutBoolOperation(
        expression: BinaryOperation,
        adapter: InstructionAdapter,
        context: Context,
        shortcutJump: (InstructionAdapter, Label) -> Unit,
        shortcutVal: Boolean
    ) {

        fun Boolean.toInt() = if (this) 1 else 0

        val shortcutLabel = Label()
        val afterLabel = Label()
        generateExpression(expression.leftOperand, adapter, context)
        shortcutJump(adapter, shortcutLabel)
        generateExpression(expression.rightOperand, adapter, context)
        shortcutJump(adapter, shortcutLabel)
        adapter.iconst((!shortcutVal).toInt())
        adapter.goTo(afterLabel)
        adapter.visitLabel(shortcutLabel)
        adapter.iconst(shortcutVal.toInt())
        adapter.visitLabel(afterLabel)
    }

    private fun generateField(getField: GetField, adapter: InstructionAdapter, context: Context) {
        val owner = getField.fieldReference.declaringClass.jvmName
        val name = getField.fieldReference.name
        val descriptor = getField.fieldReference.fieldType.jvmDescriptor
        if (getField.fieldReference.isStatic) {
            adapter.getstatic(owner, name, descriptor)
        } else {
            generateExpression(getField.target, adapter, context)
            adapter.getfield(owner, name, descriptor)
        }
    }

    private fun generateGetArrayElement(
        getArrayElement: GetArrayElement, adapter: InstructionAdapter, context: Context
    ) {
        generateExpression(getArrayElement.array, adapter, context)
        generateExpression(getArrayElement.index, adapter, context)
        adapter.aload(getArrayElement.asmType)
    }

    private fun generateSuperCall(superCall: SuperCall, adapter: InstructionAdapter, context: Context) {
        adapter.load(0, context.method.declaringClass.reference.asmType)
        superCall.arguments.forEach { generateExpression(it, adapter, context) }
        adapter.invokespecial(
            superCall.constructor.declaringClass.jvmName, JVM_INIT, superCall.constructor.jvmDescriptor, false
        )
    }

    private fun generateConstructorCall(
        constructorCall: ConstructorCall, adapter: InstructionAdapter, context: Context
    ) {
        adapter.anew(constructorCall.constructor.declaringClass.asmType)
        adapter.dup()
        constructorCall.arguments.forEach { generateExpression(it, adapter, context) }
        adapter.invokespecial(
            constructorCall.constructor.declaringClass.jvmName,
            JVM_INIT,
            constructorCall.constructor.jvmDescriptor,
            false
        )
    }

    private fun generateGetVariable(
        getVariable: GetVariable, adapter: InstructionAdapter, context: Context
    ) {
        adapter.load(context.localIndices[getVariable.variable]!!, getVariable.variable.type.asmType)
    }

    private fun generateArrayLength(
        arrayLength: ArrayLength, adapter: InstructionAdapter, context: Context
    ) {
        generateExpression(arrayLength.target, adapter, context)
        adapter.arraylength()
    }

    private fun generateReturn(returnStatement: Return, adapter: InstructionAdapter, context: Context) {
        if (returnStatement.expression != null) {
            generateExpression(returnStatement.expression, adapter, context)
            adapter.areturn(returnStatement.expression.asmType)
            return
        }
        adapter.areturn(Type.VOID_TYPE)
    }

    private class Context(val method: ExecutableMember) {
        val localIndices = method.enumerateLocals()
        val loopStack = mutableListOf<LoopContext>()

        fun enterLoop(continueLabel: Label, breakLabel: Label) {
            loopStack.add(LoopContext(continueLabel, breakLabel))
        }

        fun exitLoop() {
            loopStack.removeLast()
        }

        val continueLabel: Label get() = loopStack.last().startLabel

        val breakLabel: Label get() = loopStack.last().endLabel

        private class LoopContext(val startLabel: Label, val endLabel: Label)
    }
}

private fun ExecutableMember.enumerateLocals(): Map<VariableDeclaration, Int> {
    val result = mutableMapOf<VariableDeclaration, Int>()

    var counter = if (this is StaticInitBlock || (this as? MethodDeclaration)?.reference?.isStatic == true) 0 else 1
    parameters.withIndex().forEach { result[it.value] = counter++ }

    fun List<Statement>.collectLocals() {
        fun Statement.collectLocals() {
            when (this) {
                is For -> {
                    this.init.collectLocals()
                    this.update.collectLocals()
                    this.body.collectLocals()
                }

                is If -> {
                    this.thenStatements.collectLocals()
                    this.elseStatements.collectLocals()
                }

                is LocalVariableDeclaration -> result[this] = counter++
                is While -> this.body.collectLocals()
                else -> {}
            }
        }
        forEach { it.collectLocals() }
    }

    body.collectLocals()

    return result
}