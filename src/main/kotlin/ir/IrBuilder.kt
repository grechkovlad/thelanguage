package ir

import ClassKind
import Location
import ModifierType
import ModifierType.*
import ast.*
import ir.IrBuilder.CompilationContext.Companion.tryToResolveField
import isAccessModifier
import kotlin.reflect.KFunction2
import ast.BinaryOperationKind.*
import ast.LocalVariableDeclaration


class IrBuilder(private val sources: List<SourceFile>) {

    private val classIdToDeclaration = mutableMapOf<String, ClassDeclaration>()
    private val classIdToAst = mutableMapOf<String, ast.ClassDeclaration>()
    private val fieldIdToDeclaration = mutableMapOf<String, FieldDeclaration>()
    private val classToMethodsInfo = mutableMapOf<ClassReference, MethodsOfClassScope>()
    private val methodDeclarationToAst = mutableMapOf<MethodDeclaration, ast.MethodDeclaration>()
    private val fieldDeclarationToAst = mutableMapOf<FieldDeclaration, ast.FieldDeclaration>()
    private val constructorDeclarationToAst = mutableMapOf<ConstructorDeclaration, ast.ConstructorDeclaration>()
    private val staticInitBlockToAst = mutableMapOf<StaticInitBlock, ast.StaticInitBlock>()

    class MethodsOfClassScope(
        val privates: Collection<MethodReference>, val nonPrivates: Collection<MethodReference>
    ) {
        val allMethods = buildList {
            addAll(privates)
            addAll(nonPrivates)
        }
    }

    private val allClassesAst
        get() = sources.flatMap { it.classes }

    private val ast.ClassDeclaration.fields get() = members.filterIsInstance<ast.FieldDeclaration>()
    private val ast.ClassDeclaration.methods get() = members.filterIsInstance<ast.MethodDeclaration>()
    private val ast.ClassDeclaration.constructors get() = members.filterIsInstance<ast.ConstructorDeclaration>()

    fun build(): Project {
        initBuiltinsInfo()
        doClassLevelCompilation()
        doMemberLevelCompilation()
        doBodyLevelCompilation()
        return Project(classIdToDeclaration.values.toList())
    }

    private fun initBuiltinsInfo() {
        initSystemClassInfo()
        classToMethodsInfo[ObjectClassReference] = MethodsOfClassScope(emptyList(), emptyList())
        classToMethodsInfo[StringClassReference] = MethodsOfClassScope(emptyList(), emptyList())
        classToMethodsInfo[UtilsClassReference] = MethodsOfClassScope(emptyList(), listOf(ParseIntMethod))
    }

    private fun initSystemClassInfo() {
        classToMethodsInfo[SystemClassReference] =
            MethodsOfClassScope(emptyList(), listOf(PrintIntMethod, PrintStringMethod))
    }

    private fun doBodyLevelCompilation() {
        allClassesAst.forEach { classAst ->
            val classIr = classIdToDeclaration[classAst.name.value]!!
            if (classIr.kind == ClassKind.INTERFACE) return@forEach
            classIr.declaredMethods.forEach { methodIr ->
                if (!methodIr.isAbstract) {
                    require(methodDeclarationToAst[methodIr]!!.body != null)
                    compileMethodOrStaticInitBlockBody(classIr, methodIr, methodDeclarationToAst[methodIr]!!.body!!)
                }
            }
            classIr.constructors.forEach { constructorIr ->
                compileConstructorBody(classIr, constructorIr, constructorDeclarationToAst[constructorIr]!!.body)
            }
            if (staticInitBlockToAst[classIr.staticInit] != null) {
                compileMethodOrStaticInitBlockBody(
                    classIr,
                    classIr.staticInit,
                    staticInitBlockToAst[classIr.staticInit]!!.body
                )
            }
            classIr.fields.forEach { fieldIr ->
                val fieldAst = fieldDeclarationToAst[fieldIr]!!
                if (fieldAst.initializer != null) {
                    fieldIr.initializer =
                        compileExpression(fieldAst.initializer, CompilationContext(classIr, VoidTypeReference))
                }
            }
        }
    }

    class CompilationContext(
        val classIr: ClassDeclaration,
        val returnTypeExpected: TypeReference
    ) {
        private val scopes = mutableListOf(Scope())
        private var outerLoops = 0

        fun addName(name: String, variable: VariableDeclaration) {
            scopes.last()[name] = variable
        }

        fun declaredInTopScope(name: String) = scopes.last().get(name) != null

        fun enterLoop() {
            outerLoops++
        }

        fun exitLoop() {
            outerLoops--
        }

        val isInLoop: Boolean get() = outerLoops > 0

        fun pushScope(scope: Scope) = scopes.add(scope)

        fun popScope() = scopes.removeLast()

        fun tryResolveVariable(name: String): VariableDeclaration? {
            for (scopeIndex in scopes.lastIndex downTo 0) {
                val scope = scopes[scopeIndex]
                scope[name]?.also { return it }
            }
            return null
        }

        companion object {
            fun ClassReference.tryToResolveField(name: String, includingPrivate: Boolean): FieldReference? {
                if (this is UserClassReference) {
                    declaration.fields.firstOrNull { it.name == name && (!it.isPrivate || includingPrivate) }
                        ?.also { return it.reference }
                    return declaration.superClass.tryToResolveField(name, false)
                }
                return null
            }
        }
    }

    private fun compileMethodOrStaticInitBlockBody(
        classIr: ClassDeclaration, methodIr: ExecutableMember, bodyAst: Block
    ) {
        val context = CompilationContext(classIr, methodIr.returnType)
        methodIr.parameters.forEach { context.addName(it.name, it) }
        val statements = bodyAst.statements.map { compileNotSuperCallStatement(it, context) }
        context.popScope()
        if (methodIr.returnType != VoidTypeReference && !alwaysExplicitlyReturns(statements)) {
            require(methodIr is MethodDeclaration)
            throw MissingReturnStatement(methodIr.nameLocationOrFail)
        }
        methodIr.body = statements
    }

    private fun alwaysExplicitlyReturns(statements: List<Statement>) = statements.any { alwaysExplicitlyReturns(it) }

    private fun alwaysExplicitlyReturns(statement: Statement): Boolean = when (statement) {
        is If -> alwaysExplicitlyReturns(statement.thenStatements) && alwaysExplicitlyReturns(statement.elseStatements)
        is Return -> true
        else -> false
    }

    private fun compileConstructorBody(
        classIr: ClassDeclaration,
        constructorIr: ConstructorDeclaration,
        bodyAst: Block
    ) {
        val context = CompilationContext(classIr, classIr.reference)
        constructorIr.parameters.forEach { context.addName(it.name, it) }
        if (bodyAst.statements.isEmpty()) {
            throw ConstructorMustBeginWithSuperCall(bodyAst.location, constructorIr.reference)
        }
        val firstStatement = compileStatement(bodyAst.first(), context)
        if (firstStatement !is ExpressionStatement || firstStatement.expression !is SuperCall) {
            throw ConstructorMustBeginWithSuperCall(bodyAst.first().location, constructorIr.reference)
        }
        val restStatements = bodyAst.statements.drop(1).map { compileNotSuperCallStatement(it, context) }
        constructorIr.body = buildList {
            add(firstStatement)
            addAll(restStatements)
        }
    }

    private fun compileNotSuperCallStatement(
        statementAst: ast.Statement,
        context: CompilationContext
    ): Statement {
        val statement = compileStatement(statementAst, context)
        if (statement is ExpressionStatement && statement.expression is SuperCall) {
            throw IllegalSuperCall(statementAst.location)
        }
        return statement
    }

    private fun compileStatement(statementAst: ast.Statement, context: CompilationContext): Statement {
        return when (statementAst) {
            is Assignment -> compileAssignment(statementAst, context)
            is ast.ExpressionStatement -> compileExpressionStatement(statementAst, context)
            is ForStatement -> compileForStatement(statementAst, context)
            is IfStatement -> compileIfStatement(statementAst, context)
            is ReturnStatement -> compileReturnStatement(statementAst, context)
            is LocalVariableDeclaration -> compileLocalVariableDeclaration(statementAst, context)
            is WhileStatement -> compileWhileStatement(statementAst, context)
            is ast.Break -> compileBreak(statementAst, context)
            is ast.Continue -> compileContinue(statementAst, context)
        }
    }

    private fun compileContinue(continueStatement: ast.Continue, context: CompilationContext): Statement {
        if (!context.isInLoop) throw ContinueOutsideLoop(continueStatement.location)
        return Continue
    }

    private fun compileBreak(breakStatement: ast.Break, context: CompilationContext): Statement {
        if (!context.isInLoop) throw BreakOutsideOfLoop(breakStatement.location)
        return Break
    }

    private fun compileSuperCall(superCallAst: ast.SuperCall, context: CompilationContext): ir.SuperCall {
        val arguments = superCallAst.arguments.map { compileExpression(it, context) }
        val argumentTypes = arguments.map { it.type }
        return when (val resolution = resolveConstructorCall(argumentTypes, context.classIr.superClass.constructors)) {
            is Ambiguity -> throw AmbiguousCall(
                superCallAst.location,
                resolution.firstCandidate,
                resolution.secondCandidate
            )

            NoCandidates -> throw UnresolvedConstructorCall(
                superCallAst.location,
                context.classIr.superClass,
                argumentTypes
            )

            is ResolvedSuccessfully -> return SuperCall(resolution.reference, arguments)
        }
    }

    private fun compileReturnStatement(
        returnStatementAst: ReturnStatement,
        context: CompilationContext
    ): Return {
        val value = returnStatementAst.value?.let { compileExpression(it, context) }
        return if (value == null) {
            if (context.returnTypeExpected != VoidTypeReference) {
                throw TypeMismatch(context.returnTypeExpected, VoidTypeReference, returnStatementAst.location)
            }
            Return(null)
        } else {
            if (!value.type.isSubtypeOf(context.returnTypeExpected)) {
                throw TypeMismatch(context.returnTypeExpected, value.type, returnStatementAst.value.location)
            }
            Return(value)
        }
    }

    private fun compileLocalVariableDeclaration(
        localVariableDeclaration: ast.LocalVariableDeclaration,
        context: CompilationContext
    ): Statement {
        if (context.declaredInTopScope(localVariableDeclaration.name.value)) {
            throw NameDeclarationClash(localVariableDeclaration.name.value, localVariableDeclaration.name.location)
        }
        val initializer = localVariableDeclaration.initializer?.let { compileExpression(it, context) }
        val type = resolveType(localVariableDeclaration.type)
        if (initializer != null && !initializer.type.isSubtypeOf(type)) {
            throw TypeMismatch(type, initializer.type, localVariableDeclaration.initializer.location)
        }
        val variableDeclaration = ir.LocalVariableDeclaration(localVariableDeclaration.name.value, type, initializer)
        context.addName(localVariableDeclaration.name.value, variableDeclaration)
        return variableDeclaration
    }

    private fun compileWhileStatement(whileStatement: WhileStatement, context: CompilationContext): While {
        val condition = compileBoolExpression(whileStatement.condition, context)
        context.pushScope(Scope())
        context.enterLoop()
        val statements = whileStatement.body.map { compileNotSuperCallStatement(it, context) }
        context.exitLoop()
        context.popScope()
        return While(condition, statements)
    }

    private fun compileIfStatement(ifStatement: IfStatement, context: CompilationContext): If {
        val condition = compileBoolExpression(ifStatement.condition, context)
        context.pushScope(Scope())
        val thenStatements = ifStatement.thenStatements.map { compileNotSuperCallStatement(it, context) }
        context.popScope()
        context.pushScope(Scope())
        val elseStatements =
            ifStatement.elseStatements?.map { compileNotSuperCallStatement(it, context) } ?: emptyList()
        context.popScope()
        return If(condition, thenStatements, elseStatements)
    }

    private fun compileForStatement(forStatementAst: ForStatement, context: CompilationContext): For {
        context.pushScope(Scope())
        val init = compileNotSuperCallStatement(forStatementAst.initStatement, context)
        require(init is ir.LocalVariableDeclaration || init is SetVariable || init is ExpressionStatement)
        val condition = compileBoolExpression(forStatementAst.condition, context)
        val update = compileNotSuperCallStatement(forStatementAst.updateStatement, context)
        context.enterLoop()
        val body = forStatementAst.body.map { compileNotSuperCallStatement(it, context) }
        context.exitLoop()
        context.popScope()
        return For(init, condition, update, body)
    }

    private fun compileExpressionStatement(
        expressionStatement: ast.ExpressionStatement, context: CompilationContext
    ): ir.ExpressionStatement {
        return ExpressionStatement(compileExpression(expressionStatement.expression, context))
    }

    private fun assertAssignability(assigneeType: TypeReference, assignedType: TypeReference, location: Location) {
        if (!assigneeType.isSubtypeOf(assignedType)) throw TypeMismatch(assigneeType, assignedType, location)
    }

    private fun compileAssignment(assignment: Assignment, context: CompilationContext): Statement {
        val rValue = compileExpression(assignment.rValue, context)
        if (assignment.lValue is Identifier) {
            val variable = context.tryResolveVariable(assignment.lValue.value)
            if (variable != null) {
                assertAssignability(variable.type, rValue.type, assignment.rValue.location)
                return SetVariable(variable, rValue)
            }
            throw UnresolvedReference(assignment.lValue.value, assignment.lValue.location)
        }
        if (assignment.lValue is ast.FieldAccess) {
            val target = compileExpression(assignment.lValue.target, context)
            if (target.type !is ClassReference) {
                throw UnresolvedReference(assignment.lValue.name.value, assignment.lValue.location)
            }
            val field = target.type.tryToResolveField(assignment.lValue.name.value, true) ?: throw UnresolvedReference(
                assignment.lValue.name.value, assignment.lValue.location
            )
            if (target is TypeAccess && !field.isStatic) {
                throw AccessToNonStaticSymbolFromStaticContext(assignment.lValue.location)
            }
            if (field.declaringClass != context.classIr.reference) {
                if (field.isPrivate) throw CanNotAccessPrivateMember(
                    assignment.lValue.name.value,
                    assignment.lValue.name.location
                )
                if (field.isProtected) throw CanNotAccessProtectedMember(
                    assignment.lValue.name.value,
                    assignment.lValue.name.location
                )
            }
            return SetField(field, target, rValue)
        }
        if (assignment.lValue is ast.ArrayAccess) {
            val array = compileExpression(assignment.lValue.array, context)
            val index = compileExpression(assignment.lValue.index, context)
            if (index.type != IntTypeReference) {
                throw TypeMismatch(IntTypeReference, index.type, assignment.lValue.index.location)
            }
            if (array.type !is ArrayTypeReference) {
                throw TypeMismatch(ArrayTypeReference(rValue.type), array.type, assignment.lValue.array.location)
            }
            if (!rValue.type.isSubtypeOf(array.type.componentType)) {
                throw TypeMismatch(array.type.componentType, rValue.type, assignment.rValue.location)
            }
            return SetArrayElement(array, index, rValue)
        }
        throw IllegalLValue(assignment.lValue.location)
    }

    private fun compileBoolExpression(expressionAst: ast.Expression, context: CompilationContext): Expression {
        val expression = compileExpression(expressionAst, context)
        if (expression.type != BoolTypeReference) {
            throw TypeMismatch(BoolTypeReference, expression.type, expressionAst.location)
        }
        return expression
    }

    private fun compileExpression(expressionAst: ast.Expression, context: CompilationContext): Expression {
        return when (expressionAst) {
            is ast.ArrayAccess -> compileArrayElementRead(expressionAst, context)
            is ast.ArrayCreation -> compileArrayCreation(expressionAst, context)
            is ast.BinaryOperation -> compileBinaryOperation(expressionAst, context)
            is ast.ConstructorCall -> compileConstructorCall(expressionAst, context)
            is ast.FieldAccess -> compileFieldRead(expressionAst, context)
            is ast.FloatLiteral -> FloatLiteral(expressionAst.value)
            is ast.Identifier -> compileIdentifierRead(expressionAst, context)
            is ast.IntLiteral -> IntLiteral(expressionAst.value)
            is ast.MethodCall -> compileMethodCall(expressionAst, context)
            is ast.Null -> Null
            is ast.StringLiteral -> StringLiteral(expressionAst.value)
            is ast.This -> ThisAccess(context.classIr.reference)
            is ast.UnaryOperation -> compileUnaryOperation(expressionAst, context)
            is ast.BoolLiteral -> if (expressionAst.value) TrueLiteral else FalseLiteral
            is ast.SuperCall -> compileSuperCall(expressionAst, context)
        }
    }

    private fun compileMethodCall(methodCallAst: ast.MethodCall, context: CompilationContext): Expression {
        val target = compileExpression(methodCallAst.target, context)
        if (target.type !is ClassReference) {
            throw UnresolvedReference(methodCallAst.name.value, methodCallAst.name.location)
        }
        val arguments = methodCallAst.arguments.map { compileExpression(it, context) }
        val argumentTypes = arguments.map { it.type }
        return when (val resolution =
            resolveMethodCall(
                methodCallAst.name.value,
                argumentTypes,
                calculateMethodsOfClassScope(target.type).allMethods
            )) {
            is Ambiguity -> throw AmbiguousCall(
                methodCallAst.location,
                resolution.firstCandidate,
                resolution.secondCandidate
            )

            NoCandidates -> throw UnresolvedReference(methodCallAst.name.value, methodCallAst.name.location)

            is ResolvedSuccessfully -> {
                if (resolution.reference.isPrivate && context.classIr.reference != target.type) {
                    throw CanNotAccessPrivateMember(methodCallAst.name.value, methodCallAst.name.location)
                }
                if (resolution.reference.isProtected && !context.classIr.reference
                        .isSubtypeOf(resolution.reference.declaringClass)
                ) {
                    throw CanNotAccessProtectedMember(methodCallAst.name.value, methodCallAst.name.location)
                }
                if (!resolution.reference.isStatic && target is TypeAccess) {
                    throw AccessToNonStaticSymbolFromStaticContext(methodCallAst.location)
                }
                if (resolution.reference.isStatic && target !is TypeAccess) {
                    throw StaticMemberAccessViaInstance(methodCallAst.target.location)
                }
                return MethodCall(resolution.reference, target, arguments)
            }
        }
    }

    private fun compileConstructorCall(
        constructorCallAst: ast.ConstructorCall, context: CompilationContext
    ): Expression {
        val classRef = resolveClassReferenceOrReportError(
            constructorCallAst.type.identifier.value, constructorCallAst.type.identifier.location
        )
        val arguments = constructorCallAst.arguments.map { compileExpression(it, context) }
        val argumentTypes = arguments.map { it.type }
        return when (val resolution = resolveConstructorCall(argumentTypes, classRef.constructors)) {
            is NoCandidates -> throw UnresolvedConstructorCall(constructorCallAst.location, classRef, argumentTypes)

            is Ambiguity -> throw AmbiguousCall(
                constructorCallAst.location, resolution.firstCandidate, resolution.secondCandidate
            )

            is ResolvedSuccessfully -> ConstructorCall(resolution.reference, arguments)
        }
    }

    private inline fun <T : ExecutableReference> T.fitsFor(
        argumentTypes: List<TypeReference>, predicate: (T) -> Boolean
    ) = parameterTypes.size == argumentTypes.size && parameterTypes.zip(argumentTypes)
        .all { (paramType, argType) -> argType.isSubtypeOf(paramType) } && predicate(this)

    // Precondition: this != other and numbers of parameters are equal
    private fun <T : ExecutableReference> T.isMoreSpecific(other: T) = parameterTypes.zip(other.parameterTypes)
        .all { (thisParamType, otherParamType) -> thisParamType.isSubtypeOf(otherParamType) }

    private fun <T : ExecutableReference> resolveExecutable(
        argumentTypes: List<TypeReference>, candidates: Collection<T>, additionalPredicate: (T) -> Boolean
    ): CallResolutionResult<T> {
        if (candidates.isEmpty()) return NoCandidates
        val remainedCandidates = mutableListOf<T>().apply { addAll(candidates) }
        while (remainedCandidates.size > 1) {
            val it = remainedCandidates.iterator()
            val one = it.next()
            if (!one.fitsFor(argumentTypes, additionalPredicate)) {
                it.remove()
                continue
            }
            val another = it.next()
            if (!another.fitsFor(argumentTypes, additionalPredicate)) {
                it.remove()
                continue
            }
            if (one.isMoreSpecific(another)) {
                it.remove()
            } else {
                remainedCandidates.removeFirst()
            }
        }
        val survivor = remainedCandidates.single()
        if (!survivor.fitsFor(argumentTypes, additionalPredicate)) return NoCandidates
        val otherCandidate = candidates.firstOrNull {
            it != survivor && it.fitsFor(
                argumentTypes, additionalPredicate
            ) && !survivor.isMoreSpecific(it)
        }
        if (otherCandidate != null) return Ambiguity(survivor, otherCandidate)
        return ResolvedSuccessfully(survivor)
    }

    private fun resolveConstructorCall(
        argumentTypes: List<TypeReference>, candidates: Collection<ConstructorReference>
    ): CallResolutionResult<ConstructorReference> = resolveExecutable(argumentTypes, candidates) { true }

    private fun resolveMethodCall(
        name: String, argumentTypes: List<TypeReference>, candidates: Collection<MethodReference>
    ): CallResolutionResult<MethodReference> = resolveExecutable(argumentTypes, candidates) { it.name == name }

    sealed class CallResolutionResult<out T : ExecutableReference>
    class ResolvedSuccessfully<T : ExecutableReference>(val reference: T) : CallResolutionResult<T>()
    class Ambiguity<T : ExecutableReference>(val firstCandidate: T, val secondCandidate: T) : CallResolutionResult<T>()
    object NoCandidates : CallResolutionResult<Nothing>()

    private fun compileFieldRead(fieldAccess: FieldAccess, context: CompilationContext): Expression {
        val target = compileExpression(fieldAccess.target, context)
        if (fieldAccess.name.value == "length" && target.type is ArrayTypeReference) {
            return ArrayLength(target)
        }
        if (target.type !is ClassReference) {
            throw UnresolvedReference(fieldAccess.name.value, fieldAccess.name.location)
        }
        val field = target.type.tryToResolveField(fieldAccess.name.value, true)
            ?: throw UnresolvedReference(fieldAccess.name.value, fieldAccess.name.location)
        if (field.declaringClass != context.classIr.reference) {
            if (field.isPrivate) throw CanNotAccessPrivateMember(fieldAccess.name.value, fieldAccess.name.location)
            if (field.isProtected) throw CanNotAccessProtectedMember(fieldAccess.name.value, fieldAccess.name.location)
        }
        if (field.isStatic && target !is TypeAccess) {
            throw StaticMemberAccessViaInstance(fieldAccess.target.location)
        }
        return GetField(field, target)
    }

    private fun compileIdentifierRead(identifier: Identifier, context: CompilationContext): Expression {
        val variable = context.tryResolveVariable(identifier.value)
        if (variable != null) {
            return GetVariable(variable)
        }
        val resolvedType = resolveType(identifier)
        return TypeAccess(resolvedType)
    }

    private fun compileUnaryOperation(
        unaryOperation: UnaryOperation, context: CompilationContext
    ): Expression {
        return when (unaryOperation.kind) {
            UnaryOperationKind.MINUS -> compileMinus(unaryOperation, context)
            UnaryOperationKind.NOT -> compileNot(unaryOperation, context)
        }
    }

    private fun compileMinus(unaryOperation: UnaryOperation, context: CompilationContext): Expression {
        require(unaryOperation.kind == UnaryOperationKind.MINUS)
        val operand = compileExpression(unaryOperation.operand, context)
        if (operand.type.isNumeric) {
            return Minus(operand)
        }
        throw NumericTypeExpected(unaryOperation.operand.location)
    }

    private fun compileNot(unaryOperation: UnaryOperation, context: CompilationContext): Expression {
        require(unaryOperation.kind == UnaryOperationKind.NOT)
        val operand = compileExpression(unaryOperation.operand, context)
        if (operand.type == BoolTypeReference) {
            return Not(operand)
        }
        throw TypeMismatch(BoolTypeReference, operand.type, unaryOperation.operand.location)
    }

    private fun compileBinaryOperation(binaryOperation: ast.BinaryOperation, context: CompilationContext): Expression {
        return when (binaryOperation.kind) {
            PLUS -> compilePlus(binaryOperation, context)
            MINUS -> processBinaryNumericOperation(binaryOperation, context, ::Subtract)
            MULT -> processBinaryNumericOperation(binaryOperation, context, ::Multiply)
            DIV -> processBinaryNumericOperation(binaryOperation, context, ::Divide)
            LESS -> processBinaryNumericOperation(binaryOperation, context, ::Less)
            LEQ -> processBinaryNumericOperation(binaryOperation, context, ::Leq)
            GREATER -> processBinaryNumericOperation(binaryOperation, context, ::Greater)
            GEQ -> processBinaryNumericOperation(binaryOperation, context, ::Geq)
            AND -> processBinaryLogicalOperation(binaryOperation, context, ::And)
            OR -> processBinaryLogicalOperation(binaryOperation, context, ::Or)
            EQ -> compileEq(binaryOperation, context, false)
            NOT_EQ -> Not(compileEq(binaryOperation, context, true))
        }
    }

    private fun compileEq(
        operationAst: ast.BinaryOperation, context: CompilationContext, inverted: Boolean
    ): Expression {
        val leftOperand = compileExpression(operationAst.leftOperand, context)
        val rightOperand = compileExpression(operationAst.rightOperand, context)
        if (!leftOperand.type.isPrimitive && !rightOperand.type.isPrimitive) {
            return RefEq(leftOperand, rightOperand)
        }
        if (leftOperand.type.isNumeric && rightOperand.type.isNumeric) {
            return NumericEq(leftOperand, rightOperand)
        }
        if (leftOperand.type == BoolTypeReference && rightOperand.type == BoolTypeReference) {
            return BoolEq(leftOperand, rightOperand)
        }
        throw BinaryOperatorInapplicable(leftOperand.type, rightOperand.type, operationAst.location)
    }

    private fun compilePlus(plus: ast.BinaryOperation, context: CompilationContext): Expression {
        require(plus.kind == PLUS)
        val leftOperand = compileExpression(plus.leftOperand, context)
        val rightOperand = compileExpression(plus.rightOperand, context)
        if (leftOperand.type == StringClassReference) {
            if (rightOperand.type != StringClassReference) {
                throw TypeMismatch(StringClassReference, rightOperand.type, plus.rightOperand.location)
            }
            return Concat(leftOperand, rightOperand)
        }
        return processBinaryNumericOperation(plus, context, ::Add)
    }

    private fun processBinaryLogicalOperation(
        operationAst: ast.BinaryOperation,
        context: CompilationContext,
        irOperationFactory: KFunction2<Expression, Expression, BinaryOperation>
    ): Expression {
        val leftOperand = compileExpression(operationAst.leftOperand, context)
        val rightOperand = compileExpression(operationAst.rightOperand, context)
        if (leftOperand.type == BoolTypeReference && rightOperand.type == BoolTypeReference) {
            return irOperationFactory(leftOperand, rightOperand)
        }
        throw BinaryOperatorInapplicable(leftOperand.type, rightOperand.type, operationAst.location)
    }

    private fun processBinaryNumericOperation(
        operationAst: ast.BinaryOperation,
        context: CompilationContext,
        irOperationFactory: (Expression, Expression) -> BinaryOperation
    ): Expression {
        val leftOperand = compileExpression(operationAst.leftOperand, context)
        val rightOperand = compileExpression(operationAst.rightOperand, context)
        if (leftOperand.type.isNumeric && rightOperand.type.isNumeric && leftOperand.type == rightOperand.type) {
            return irOperationFactory(leftOperand, rightOperand)
        }
        throw BinaryOperatorInapplicable(leftOperand.type, rightOperand.type, operationAst.location)
    }

    private fun compileArrayCreation(arrayCreation: ast.ArrayCreation, context: CompilationContext): Expression {
        val type = resolveType(arrayCreation.type.identifier)
        require(type !is ArrayTypeReference)
        val dimensions = arrayCreation.dimensions.map {
            val dimension = compileExpression(it, context)
            if (dimension.type != IntTypeReference) throw TypeMismatch(IntTypeReference, dimension.type, it.location)
            dimension
        }
        return CreateArray(type, dimensions)
    }

    private fun resolveType(type: Identifier): TypeReference {
        stdlibTypeReferences[type.value]?.also { return it }
        if (classIdToDeclaration.containsKey(type.value)) {
            return classIdToDeclaration[type.value]!!.reference
        } else {
            throw UnresolvedReference(type.value, type.location)
        }
    }

    private fun compileArrayElementRead(arrayAccess: ast.ArrayAccess, context: CompilationContext): GetArrayElement {
        val array = compileExpression(arrayAccess.array, context)
        if (array.type !is ArrayTypeReference) {
            throw ArrayExpected(arrayAccess.array.location)
        }
        val index = compileExpression(arrayAccess.index, context)
        if (index.type != IntTypeReference) {
            throw TypeMismatch(IntTypeReference, index.type, arrayAccess.index.location)
        }
        return GetArrayElement(array, index, array.type.componentType)
    }

    private fun doMemberLevelCompilation() {
        allClassesAst.forEach { classAst ->
            val classIr = classIdToDeclaration[classAst.name.value]!!
            collectFieldDeclarations(classAst, classIr)
            collectExecutablesDeclarations(classAst, classIr)
        }
        classIdToDeclaration.values.forEach { classDecl -> calculateMethodsOfClassScope(classDecl) }
    }

    private fun TypeReference.isSubtypeOf(other: TypeReference): Boolean = when (this) {
        is ArrayTypeReference -> this == other || other == ObjectClassReference
        BoolTypeReference -> this == other
        ObjectClassReference -> this == other
        StringClassReference -> this == other || other == ObjectClassReference
        SystemClassReference -> this == other || other == ObjectClassReference
        is UserClassReference -> this == other || declaration.superClass.isSubtypeOf(other) || declaration.interfaces.any {
            it.isSubtypeOf(other)
        }

        FloatTypeReference -> this == other
        IntTypeReference -> this == other
        NullTypeReference -> other.isSubtypeOf(ObjectClassReference)
        VoidTypeReference -> this == other
        UtilsClassReference -> this == other
    }

    private fun MethodReference.overridesBySignature(other: MethodReference) =
        name == other.name && this.parameterTypes == other.parameterTypes

    private val ClassDeclaration.isAbstractClass: Boolean
        get() = kind == ClassKind.CLASS && modifiers.contains(ABSTRACT)

    private fun calculateMethodsOfClassScope(clazz: ClassReference): MethodsOfClassScope {
        classToMethodsInfo[clazz]?.also { return it }
        require(clazz is UserClassReference)
        val res = calculateMethodsOfClassScope(clazz.declaration)
        classToMethodsInfo[clazz] = res
        return res
    }

    private val MethodDeclaration.nameLocationOrFail get() = methodDeclarationToAst[this]!!.name.location

    private fun ModifierType.isMoreRestrictiveAccessModifier(other: ModifierType): Boolean {
        require(this.isAccessModifier)
        require(other.isAccessModifier)
        if (this == PRIVATE) return other != PRIVATE
        if (this == PROTECTED) return other == PUBLIC
        return false
    }

    private fun MethodReference.isLessVisible(other: MethodReference) =
        effectiveAccessModifier.isMoreRestrictiveAccessModifier(other.effectiveAccessModifier)

    private fun calculateMethodsOfClassScope(clazz: ClassDeclaration): MethodsOfClassScope {
        val res: MethodsOfClassScope
        if (clazz.kind == ClassKind.INTERFACE) {
            val nonPrivates = mutableListOf<MethodReference>().apply {
                clazz.interfaces.forEach { parentInterface ->
                    val parentInterfaceMethodsScope = calculateMethodsOfClassScope(parentInterface.declaration)
                    require(parentInterfaceMethodsScope.privates.isEmpty())
                    addAll(parentInterfaceMethodsScope.nonPrivates)
                }
            }
            nonPrivates.removeAll { inherited ->
                clazz.declaredMethods.any { declared ->
                    val overridesBySignature = declared.reference.overridesBySignature(inherited)
                    if (overridesBySignature && !declared.returnType.isSubtypeOf(inherited.returnType))
                        throw IllegalReturnTypeInOverriding(declared.nameLocationOrFail)
                    overridesBySignature
                }
            }
            nonPrivates.addAll(clazz.declaredMethods.map { it.reference })
            res = MethodsOfClassScope(privates = emptyList(), nonPrivates = nonPrivates)
        } else {
            val nonPrivates = mutableListOf<MethodReference>().apply {
                addAll(calculateMethodsOfClassScope(clazz.superClass).nonPrivates)
                addAll(clazz.interfaces.flatMap { calculateMethodsOfClassScope(it).nonPrivates })
            }
            val overriddenMethods = mutableListOf<MethodReference>()
            clazz.declaredMethods.forEach { declaredMethod ->
                nonPrivates.filter { methodFromSuperClassScope ->
                    methodFromSuperClassScope.overridesBySignature(declaredMethod.reference)
                }.forEach { potentiallyOverriddenMethod ->
                    if (!declaredMethod.returnType.isSubtypeOf(potentiallyOverriddenMethod.returnType)) {
                        throw IllegalReturnTypeInOverriding(declaredMethod.nameLocationOrFail)
                    }
                    if (potentiallyOverriddenMethod.isStatic && !declaredMethod.isStatic) {
                        throw StaticMethodCanNotBeOverridden(declaredMethod.nameLocationOrFail)
                    }
                    if (!potentiallyOverriddenMethod.isStatic && declaredMethod.isStatic) {
                        throw StaticMethodCanNotOverrideInstanceMethod(declaredMethod.nameLocationOrFail)
                    }
                    if (declaredMethod.reference.isLessVisible(potentiallyOverriddenMethod)) {
                        throw OverridingRestrictsVisibility(declaredMethod.nameLocationOrFail)
                    }
                }
            }
            nonPrivates.removeAll(overriddenMethods)

            if (!clazz.isAbstractClass) {
                nonPrivates.firstOrNull { it.isAbstract }
                    ?.also {
                        throw NonAbstractClassMustOverrideAbstractMethod(
                            classIdToAst[clazz.name]!!.name.location,
                            it
                        )
                    }
            }

            val privates = mutableListOf<MethodReference>()
            clazz.declaredMethods.forEach { declaredMethod ->
                if (declaredMethod.isPrivate) {
                    privates.add(declaredMethod.reference)
                } else {
                    nonPrivates.add(declaredMethod.reference)
                }
            }
            res = MethodsOfClassScope(privates = privates, nonPrivates = nonPrivates)
        }
        return res
    }

    private val fieldApplicableModifiers = arrayOf(PRIVATE, PUBLIC, STATIC, PROTECTED)

    private fun collectFieldDeclarations(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
        val fields = mutableListOf<FieldDeclaration>()
        val nameToLocation = mutableMapOf<String, Location>()
        classAst.fields.forEach { fieldDeclarationAst ->
            val name = fieldDeclarationAst.name.value
            val location = fieldDeclarationAst.name.location
            if (classAst.kind == ClassKind.INTERFACE) throw InterfaceHasFields(location)
            nameToLocation[name]?.also { throw NameDeclarationClash(name, location) }
            val type = resolveType(fieldDeclarationAst.type)
            val fieldDeclarationIr = FieldDeclaration(
                name,
                type,
                checkRetrieveModifiers(fieldDeclarationAst.modifiers, *fieldApplicableModifiers),
                classIr.reference
            )
            fieldIdToDeclaration["${classAst.name.value}:${name}"] = fieldDeclarationIr
            fields.add(fieldDeclarationIr)
            nameToLocation[name] = location
            fieldDeclarationToAst[fieldDeclarationIr] = fieldDeclarationAst
        }
        classIr.fields = fields
    }

    private fun compileParameterList(parametersList: ParametersList): List<ParameterDeclaration> {
        val parameterNameToLocation = mutableMapOf<String, Location>()
        return parametersList.map { parameter ->
            val name = parameter.name.value
            val location = parameter.location
            parameterNameToLocation[name]?.also { throw NameDeclarationClash(name, location) }
            parameterNameToLocation[name] = location
            val type = resolveType(parameter.type)
            ParameterDeclaration(name, type)
        }
    }

    private fun collectExecutablesDeclarations(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
        collectMethods(classAst, classIr)
        collectConstructors(classAst, classIr)
        collectStaticInitBlock(classAst, classIr)
    }

    private fun collectConstructors(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
        val constructorSignatureToLocation = mutableMapOf<String, Location>()
        classIr.constructors = classAst.constructors.map { constructorAst ->
            if (classIr.kind == ClassKind.INTERFACE) {
                throw ConstructorInInterface(constructorAst.location)
            }
            val modifiers = checkRetrieveModifiers(constructorAst.modifiers, PRIVATE, PROTECTED, PUBLIC)
            checkAccessModifiers(constructorAst)
            val parameters = compileParameterList(constructorAst.parameters)
            val stringId = parameters.stringify()
            constructorSignatureToLocation[stringId]?.also {
                throw SignatureDeclarationClash("constructor$stringId", constructorAst.location)
            }
            val constructorDeclaration = ConstructorDeclaration(classIr, parameters, modifiers)
            constructorDeclarationToAst[constructorDeclaration] = constructorAst
            constructorDeclaration
        }
    }

    private fun collectMethods(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
        val methodSignatureToLocation = mutableMapOf<String, Location>()
        classIr.declaredMethods = classAst.methods.map { methodAst ->
            val modifiers = checkRetrieveModifiers(methodAst.modifiers, *ModifierType.values())
            if (!classIr.modifiers.contains(ABSTRACT)) {
                methodAst.modifiers.firstOrNull { it.type == ABSTRACT }?.also {
                    throw AbstractMethodInNonAbstractClass(it.location)
                }
            }
            checkAccessModifiers(methodAst)
            if (methodAst.body != null && classAst.kind == ClassKind.INTERFACE) {
                throw MethodWithBodyInInterface(methodAst.name.location)
            }
            if (methodAst.body != null) {
                methodAst.modifiers.firstOrNull { it.type == ABSTRACT }?.also {
                    throw AbstractMethodWithBody(it.location)
                }
            }
            val parameters = compileParameterList(methodAst.parameters)
            val stringId = "${methodAst.name.value}${parameters.stringify()}"
            methodSignatureToLocation[stringId]?.also {
                throw SignatureDeclarationClash(stringId, methodAst.name.location)
            }
            methodSignatureToLocation[stringId] = methodAst.name.location
            val returnType = methodAst.returnType?.let { resolveType(it) } ?: VoidTypeReference
            val methodDeclaration = MethodDeclaration(classIr, modifiers, methodAst.name.value, parameters, returnType)
            methodDeclarationToAst[methodDeclaration] = methodAst
            methodDeclaration
        }
    }

    private fun collectStaticInitBlock(classAst: ast.ClassDeclaration, classIr: ClassDeclaration) {
        val staticInitBlocks = classAst.members.filterIsInstance<ast.StaticInitBlock>()
        if (staticInitBlocks.size > 1) {
            throw MultipleDeclarationOfStaticInitBlock(staticInitBlocks[1].location)
        }
        if (staticInitBlocks.isEmpty()) {
            classIr.staticInit = StaticInitBlock().apply { body = emptyList() }
            return
        }
        val staticInitBlockAst = staticInitBlocks.single()
        if (classIr.kind == ClassKind.INTERFACE) {
            throw InterfaceHasStaticInitBlock(staticInitBlockAst.location)
        }
        classIr.staticInit = StaticInitBlock()
        staticInitBlockToAst[classIr.staticInit] = staticInitBlockAst
    }

    private fun List<ParameterDeclaration>.stringify() =
        joinToString(separator = ", ", prefix = "(", postfix = ")") { it.type.toString() }

    private fun checkAccessModifiers(methodAst: ast.MemberDeclaration) {
        val accessModifiers = methodAst.modifiers.filter { it.type.isAccessModifier }
        if (accessModifiers.size > 1) {
            throw ConflictingAccessModifiers(accessModifiers[1].location)
        }
    }

    private val stdlibTypeReferences = mapOf(
        "int" to IntTypeReference,
        "float" to FloatTypeReference,
        "bool" to BoolTypeReference,
        "System" to SystemClassReference,
        "String" to StringClassReference,
        "Object" to ObjectClassReference,
        "Utils" to UtilsClassReference
    )

    private fun resolveType(type: ast.TypeReference): TypeReference {
        when (type) {
            is ast.ArrayTypeReference -> return ArrayTypeReference(resolveType(type.componentTypeReference))
            is ast.SimpleTypeReference -> {
                stdlibTypeReferences[type.identifier.value]?.also { return it }
                classIdToDeclaration[type.identifier.value]?.also { return UserClassReference(it) }
            }
        }
        throw UnresolvedReference(type.identifier.value, type.location)
    }

    private fun doClassLevelCompilation() {
        initClassDeclarations()
        analyzeInheritance()
    }


    private fun resolveClassReferenceOrReportError(name: String, location: Location): ClassReference {
        return systemClassReferences[name] ?: classIdToDeclaration[name]?.let { UserClassReference(it) }
        ?: throw UnresolvedReference(name, location)
    }

    private fun analyzeInheritance() {
        fillSupertypesReferences()
        checkCyclicInheritance()
    }

    private fun checkCyclicInheritance() {
        val color = mutableMapOf<String, Boolean>()

        fun dfs(node: ClassDeclaration) {
            if (color[node.name] == true) return
            color[node.name] = false
            for (superRef in listOf(node.superClass, *node.interfaces.toTypedArray())) {
                if (color[superRef.name] == false) throw CyclicInheritance(
                    node.name,
                    classIdToAst[node.name]!!.name.location
                )
                if (superRef is UserClassReference) dfs(superRef.declaration)
            }
            color[node.name] = true
        }

        classIdToDeclaration.values.forEach { dfs(it) }
    }

    private fun fillSupertypesReferences() {
        allClassesAst.forEach { clazz ->
            var superClass: ClassReference? = null
            val interfaces = mutableListOf<UserClassReference>()
            for (supertypeIdentifier in clazz.superClasses) {
                val superTypeRef =
                    resolveClassReferenceOrReportError(supertypeIdentifier.value, supertypeIdentifier.location)
                if (superTypeRef in finalClassesRefs) throw FinalClassSubtyping(
                    supertypeIdentifier.value, supertypeIdentifier.location
                )
                if (!superTypeRef.isInterface) {
                    if (clazz.kind == ClassKind.INTERFACE) throw InterfaceInheritsClass(supertypeIdentifier.location)
                    superClass =
                        if (superClass == null) superTypeRef else throw MultipleInheritance(supertypeIdentifier.location)
                } else {
                    interfaces.add(superTypeRef as UserClassReference)
                }
            }
            val currentClassDeclaration = classIdToDeclaration[clazz.name.value]!!
            currentClassDeclaration.interfaces = interfaces.toList()
            currentClassDeclaration.superClass = superClass ?: ObjectClassReference
        }
    }

    private val reservedClassNames = listOf("Object", "String")
    private val finalClassesRefs = listOf(SystemClassReference, StringClassReference, UtilsClassReference)
    private val systemClassReferences = mapOf(
        "Object" to ObjectClassReference, "String" to StringClassReference, "System" to SystemClassReference
    )


    private fun initClassDeclarations() {
        val nameToLocation = mutableMapOf<String, Location>()
        allClassesAst.forEach { classAst ->
            val name = classAst.name.value
            classIdToAst[name] = classAst
            val location = classAst.name.location
            if (name in reservedClassNames) throw ReservedClassName(name, location)
            nameToLocation[name]?.also { throw NameDeclarationClash(name, location) }
            val allowedModifiers =
                if (classAst.kind == ClassKind.INTERFACE) listOf() else listOf(ABSTRACT)
            val declaration = ClassDeclaration(
                name, classAst.kind, checkRetrieveModifiers(classAst.modifiers, *allowedModifiers.toTypedArray())
            )
            classIdToDeclaration[name] = declaration
            nameToLocation[name] = location
        }
    }

    private fun checkRetrieveModifiers(
        modifiers: ModifiersList, vararg allowedTypes: ModifierType
    ): List<ModifierType> {
        val modifierKingToLocation = mutableMapOf<ModifierType, Location>()
        modifiers.forEach { modifier ->
            modifierKingToLocation[modifier.type]?.also {
                throw RepeatedModifier(modifier.type, modifier.location)
            }
            if (modifier.type !in allowedTypes) throw ForbiddenModifier(modifier.type, modifier.location)
            modifierKingToLocation[modifier.type] = modifier.location
        }
        return modifierKingToLocation.keys.toList()
    }
}

private val ModifiersBearer.isStatic get() = this.modifiers.contains(STATIC)

private val ModifiersBearer.effectiveAccessModifier
    get() = modifiers.firstOrNull { it.isAccessModifier } ?: PUBLIC

private val ModifiersBearer.isPrivate get() = modifiers.contains(PRIVATE)

private val ModifiersBearer.isProtected get() = modifiers.contains(PROTECTED)

private val ModifiersBearer.isAbstract get() = modifiers.contains(ABSTRACT)

private val TypeReference.isNumeric get() = this == IntTypeReference || this == FloatTypeReference

private val TypeReference.isPrimitive get() = this.isNumeric || this == BoolTypeReference

fun binaryNumericPromote(leftOperandType: TypeReference, rightOperandType: TypeReference): TypeReference {
    require(leftOperandType.isNumeric)
    require(rightOperandType.isNumeric)
    if (leftOperandType == FloatTypeReference || rightOperandType == FloatTypeReference) return FloatTypeReference
    return IntTypeReference
}

class Scope {
    operator fun get(name: String) = identifiers[name]

    operator fun set(name: String, variable: VariableDeclaration) {
        require(!identifiers.containsKey(name))
        identifiers[name] = variable
    }

    private val identifiers = mutableMapOf<String, VariableDeclaration>()
}