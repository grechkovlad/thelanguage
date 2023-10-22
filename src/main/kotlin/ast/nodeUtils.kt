package ast

import ClassKind

fun AstNode.dump(): String {
    val stringBuilder = StringBuilder()
    this.accept(StringifierVisitor(stringBuilder), 0)
    return stringBuilder.toString()
}

private class StringifierVisitor(private val builder: StringBuilder) : VoidAstVisitor<Int>, Appendable by builder {

    private fun addIdent(context: Int) {
        repeat(context) { builder.append("  ") }
    }

    override fun visit(simpleTypeReference: SimpleTypeReference, context: Int) {
        addIdent(context)
        append("${simpleTypeReference.identifier.value} ${simpleTypeReference.location.dumpWithoutFilename()}")
    }

    override fun visit(arrayTypeReference: ArrayTypeReference, context: Int) {
        addIdent(context)
        arrayTypeReference.componentTypeReference.accept(this, 0)
        append("[]")
    }

    override fun visit(intLiteral: IntLiteral, context: Int) {
        addIdent(context)
        append("${intLiteral.value} ${intLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(floatLiteral: FloatLiteral, context: Int) {
        addIdent(context)
        append("${floatLiteral.value} ${floatLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(boolLiteral: BoolLiteral, context: Int) {
        addIdent(context)
        append("${boolLiteral.value} ${boolLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(identifier: Identifier, context: Int) {
        addIdent(context)
        append("${identifier.value} ${identifier.location.dumpWithoutFilename()}")
    }

    override fun visit(arg: Null, context: Int) {
        addIdent(context)
        append("NULL ${arg.location.dumpWithoutFilename()}")
    }

    override fun visit(arg: This, context: Int) {
        addIdent(context)
        append("THIS ${arg.location.dumpWithoutFilename()}")
    }

    override fun visit(arrayAccess: ArrayAccess, context: Int) {
        addIdent(context)
        append("ARRAY ACCESS: ${arrayAccess.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("ARRAY:\n")
        arrayAccess.array.accept(this, context + 2)
        append("\n")
        addIdent(context + 1)
        append("INDEX:\n")
        arrayAccess.index.accept(this, context + 2)
    }

    override fun visit(fieldAccess: FieldAccess, context: Int) {
        addIdent(context)
        append("FIELD ACCESS: ${fieldAccess.name.value} ${fieldAccess.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("TARGET:\n")
        fieldAccess.target.accept(this, context + 2)
    }

    override fun visit(parameter: Parameter, context: Int) {
        addIdent(context)
        append("PARAMETER: ${parameter.name.value} ${parameter.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("type: ")
        parameter.type.accept(this, 0)
    }

    override fun visit(binaryOperation: BinaryOperation, context: Int) {
        addIdent(context)
        append("BINARY OP: ${binaryOperation.kind.name} ${binaryOperation.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("LEFT:\n")
        binaryOperation.leftOperand.accept(this, context + 2)
        append('\n')
        addIdent(context + 1)
        append("RIGHT:\n")
        binaryOperation.rightOperand.accept(this, context + 2)
    }

    override fun visit(unaryOperation: UnaryOperation, context: Int) {
        addIdent(context)
        append("UNARY OP: ${unaryOperation.kind.name} ${unaryOperation.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("OPERAND:\n")
        unaryOperation.operand.accept(this, context + 2)
    }

    override fun visit(constructorCall: ConstructorCall, context: Int) {
        addIdent(context)
        append("CONSTRUCTOR CALL: ${constructorCall.type.identifier.value} ${constructorCall.location.dumpWithoutFilename()}")
        if (constructorCall.arguments.isNotEmpty()) {
            append('\n')
            constructorCall.arguments.accept(this, context + 1)
        }
    }

    override fun visit(arrayCreation: ArrayCreation, context: Int) {
        addIdent(context)
        append("ARRAY CREATION: ${arrayCreation.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("COMPONENT TYPE:\n")
        arrayCreation.type.accept(this, context + 2)
        append('\n')
        addIdent(context + 1)
        append("DIMENSIONS:")
        arrayCreation.dimensions.forEach {
            append('\n')
            it.accept(this, context + 2)
        }
    }

    override fun visit(expressionStatement: ExpressionStatement, context: Int) {
        addIdent(context)
        append("EXPRESSION STATEMENT: ${expressionStatement.location.dumpWithoutFilename()}\n")
        expressionStatement.expression.accept(this, context + 1)
    }

    override fun visit(ifStatement: IfStatement, context: Int) {
        addIdent(context)
        append("IF: ${ifStatement.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("CONDITION:\n")
        ifStatement.condition.accept(this, context + 2)
        append('\n')
        addIdent(context + 1)
        append("THEN:\n")
        ifStatement.thenStatements.accept(this, context + 2)
        if (ifStatement.elseStatements != null) {
            append("\n")
            addIdent(context + 1)
            append("ELSE:\n")
            ifStatement.elseStatements.accept(this, context + 2)
        }
    }

    override fun visit(whileStatement: WhileStatement, context: Int) {
        addIdent(context)
        append("WHILE: ${whileStatement.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("CONDITION:\n")
        whileStatement.condition.accept(this, context + 2)
        append('\n')
        addIdent(context + 1)
        append("BODY:\n")
        whileStatement.body.accept(this, context + 2)
    }

    override fun visit(forStatement: ForStatement, context: Int) {
        addIdent(context)
        append("FOR: ${forStatement.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("INIT:\n")
        forStatement.initStatement.accept(this, context + 2)
        append("\n")
        addIdent(context + 1)
        append("CONDITION:\n")
        forStatement.condition.accept(this, context + 2)
        append("\n")
        addIdent(context + 1)
        append("UPDATE:\n")
        forStatement.updateStatement.accept(this, context + 2)
        append("\n")
        addIdent(context + 1)
        append("BODY:\n")
        forStatement.body.accept(this, context + 2)
    }

    override fun visit(returnStatement: ReturnStatement, context: Int) {
        addIdent(context)
        append("RETURN ${returnStatement.location.dumpWithoutFilename()}")
        if (returnStatement.value != null) {
            append(":\n")
            addIdent(context + 1)
            append("VALUE:\n")
            returnStatement.value.accept(this, context + 2)
        }
    }

    override fun visit(assignment: Assignment, context: Int) {
        addIdent(context)
        append("ASSIGN: ${assignment.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("LVALUE:\n")
        assignment.lValue.accept(this, context + 2)
        append("\n")
        addIdent(context + 1)
        append("RVALUE:\n")
        assignment.rValue.accept(this, context + 2)
    }

    override fun visit(modifier: Modifier, context: Int) {
        addIdent(context)
        append(modifier.type.name)
    }

    override fun visit(modifiersList: ModifiersList, context: Int) {
        addIdent(context)
        append(modifiersList.joinToString(prefix = "[", postfix = "]") { it.type.name })
    }

    override fun visit(parametersList: ParametersList, context: Int) {
        addIdent(context)
        append("PARAMETERS: ${parametersList.location.dumpWithoutFilename()}")
        parametersList.forEach {
            append('\n')
            it.accept(this, context + 1)
        }
    }

    override fun visit(argumentsList: ArgumentsList, context: Int) {
        addIdent(context)
        append("ARGUMENTS: ${argumentsList.location.dumpWithoutFilename()}")
        argumentsList.forEach {
            append('\n')
            it.accept(this, context + 1)
        }
    }

    override fun visit(block: Block, context: Int) {
        addIdent(context)
        append("BLOCK: ${block.location.dumpWithoutFilename()}")
        block.forEach {
            append('\n')
            it.accept(this, context + 1)
        }
    }

    override fun visit(fieldDeclaration: FieldDeclaration, context: Int) {
        addIdent(context)
        append("FIELD: ${fieldDeclaration.name.value} ")
        if (fieldDeclaration.modifiers.isNotEmpty()) {
            fieldDeclaration.modifiers.accept(this, 0)
        }
        append(" ${fieldDeclaration.location.dumpWithoutFilename()}")
        append('\n')
        addIdent(context + 1)
        append("TYPE: ")
        fieldDeclaration.type.accept(this, 0)
        if (fieldDeclaration.initializer != null) {
            append('\n')
            addIdent(context + 1)
            append("INIT:\n")
            fieldDeclaration.initializer.accept(this, context + 2)
        }
    }

    override fun visit(localVariableDeclaration: LocalVariableDeclaration, context: Int) {
        addIdent(context)
        append("VAR: ${localVariableDeclaration.name.value} ${localVariableDeclaration.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("TYPE: ")
        localVariableDeclaration.type.accept(this, 0)
        if (localVariableDeclaration.initializer != null) {
            append('\n')
            addIdent(context + 1)
            append("INIT:\n")
            localVariableDeclaration.initializer.accept(this, context + 2)
        }
    }

    override fun visit(methodDeclaration: MethodDeclaration, context: Int) {
        addIdent(context)
        append("METHOD: ${methodDeclaration.name.value}")
        if (methodDeclaration.modifiers.isNotEmpty()) {
            methodDeclaration.modifiers.accept(this, 0)
        }
        append(" ${methodDeclaration.location.dumpWithoutFilename()}")
        append('\n')
        addIdent(context + 1)
        append("RETURN TYPE: ")
        if (methodDeclaration.returnType != null) {
            methodDeclaration.returnType.accept(this, 0)
        } else {
            append("VOID")
        }
        if (methodDeclaration.parameters.isNotEmpty()) {
            append('\n')
            methodDeclaration.parameters.accept(this, context + 1)
        }
        if (methodDeclaration.body != null) {
            append('\n')
            addIdent(context + 1)
            append("BODY:\n")
            methodDeclaration.body.accept(this, context + 2)
        }
    }

    override fun visit(constructorDeclaration: ConstructorDeclaration, context: Int) {
        addIdent(context)
        append("CONSTRUCTOR: ")
        constructorDeclaration.modifiers.accept(this, 0)
        if (constructorDeclaration.parameters.isNotEmpty()) {
            append('\n')
            constructorDeclaration.parameters.accept(this, context + 1)
        }
        append(" ${constructorDeclaration.location.dumpWithoutFilename()}")
        append('\n')
        addIdent(context + 1)
        append("BODY:\n")
        constructorDeclaration.body.accept(this, context + 2)
    }

    override fun visit(staticInitBlock: StaticInitBlock, context: Int) {
        addIdent(context)
        append("STATIC INIT: ${staticInitBlock.location.dumpWithoutFilename()}\n")
        staticInitBlock.body.accept(this, context + 1)
    }

    override fun visit(classDeclaration: ClassDeclaration, context: Int) {
        addIdent(context)
        append("${if (classDeclaration.kind == ClassKind.INTERFACE) "interface" else "class"}: ${classDeclaration.name.value} ${classDeclaration.location.dumpWithoutFilename()}")
        if (classDeclaration.modifiers.isNotEmpty()) {
            classDeclaration.modifiers.accept(this, 0)
        }
        if (classDeclaration.superClasses.isNotEmpty()) {
            append('\n')
            addIdent(context + 1)
            append("SUPERCLASSES: ${classDeclaration.superClasses.joinToString { it.value }}")
        }
        classDeclaration.members.forEach {
            append('\n')
            it.accept(this, context + 1)
        }
    }

    override fun visit(sourceFile: SourceFile, context: Int) {
        sourceFile.classes.forEach {
            append('\n')
            it.accept(this, context)
        }
    }

    override fun visit(methodCall: MethodCall, context: Int) {
        addIdent(context)
        append("METHOD CALL: ${methodCall.name.value} ${methodCall.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("TARGET:\n")
        methodCall.target.accept(this, context + 2)
        if (methodCall.arguments.isNotEmpty()) {
            append('\n')
            methodCall.arguments.accept(this, context + 1)
        }
    }

    override fun visit(stringLiteral: StringLiteral, context: Int) {
        addIdent(context)
        append("\"${stringLiteral.value}\" ${stringLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(superCall: SuperCall, context: Int) {
        addIdent(context)
        append("SUPER CALL:")
        if (superCall.arguments.isNotEmpty()) {
            append('\n')
            superCall.arguments.accept(this, context + 1)
        }
    }

    override fun visit(breakStatement: Break, context: Int) {
        addIdent(context)
        append("BREAK")
    }

    override fun visit(continueStatement: Continue, context: Int) {
        addIdent(context)
        append("CONTINUE")
    }

    override fun visit(typeOp: TypeOp, context: Int) {
        addIdent(context)
        append("TYPE OP: ${typeOp.kind} ${typeOp.location.dumpWithoutFilename()}\n")
        addIdent(context + 1)
        append("OPERAND:\n")
        typeOp.expression.accept(this, context + 2)
        append("\n")
        addIdent(context + 1)
        append("TYPE:\n")
        typeOp.type.accept(this, context + 2)
    }
}