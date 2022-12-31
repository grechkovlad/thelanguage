package ast

import ClassKind

fun AstNode.dump(): String {
    val stringBuilder = StringBuilder()
    this.accept(StringifierVisitor(stringBuilder), 0)
    return stringBuilder.toString()
}

private class StringifierVisitor(private val builder: StringBuilder) : VoidAstVisitor<Int>, Appendable by builder {

    private fun addIdent(tabsCount: Int) {
        repeat(tabsCount) { builder.append("  ") }
    }

    override fun visit(simpleTypeReference: SimpleTypeReference, tabsCount: Int) {
        addIdent(tabsCount)
        append("${simpleTypeReference.identifier.value} ${simpleTypeReference.location.dumpWithoutFilename()}")
    }

    override fun visit(arrayTypeReference: ArrayTypeReference, tabsCount: Int) {
        addIdent(tabsCount)
        arrayTypeReference.componentTypeReference.accept(this, 0)
        append("[]")
    }

    override fun visit(intLiteral: IntLiteral, tabsCount: Int) {
        addIdent(tabsCount)
        append("${intLiteral.value} ${intLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(floatLiteral: FloatLiteral, tabsCount: Int) {
        addIdent(tabsCount)
        append("${floatLiteral.value} ${floatLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(identifier: Identifier, tabsCount: Int) {
        addIdent(tabsCount)
        append("${identifier.value} ${identifier.location.dumpWithoutFilename()}")
    }

    override fun visit(arg: Null, tabsCount: Int) {
        addIdent(tabsCount)
        append("NULL ${arg.location.dumpWithoutFilename()}")
    }

    override fun visit(arg: This, tabsCount: Int) {
        addIdent(tabsCount)
        append("THIS ${arg.location.dumpWithoutFilename()}")
    }

    override fun visit(arrayAccess: ArrayAccess, tabsCount: Int) {
        addIdent(tabsCount)
        append("ARRAY ACCESS: ${arrayAccess.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("ARRAY:\n")
        arrayAccess.array.accept(this, tabsCount + 2)
        append("\n")
        addIdent(tabsCount + 1)
        append("INDEX:\n")
        arrayAccess.index.accept(this, tabsCount + 2)
    }

    override fun visit(fieldAccess: FieldAccess, tabsCount: Int) {
        addIdent(tabsCount)
        append("FIELD ACCESS: ${fieldAccess.name.value} ${fieldAccess.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("TARGET:\n")
        fieldAccess.target.accept(this, tabsCount + 2)
    }

    override fun visit(parameter: Parameter, tabsCount: Int) {
        addIdent(tabsCount)
        append("PARAMETER: ${parameter.name.value} ${parameter.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("type: ")
        parameter.type.accept(this, 0)
    }

    override fun visit(binaryOperation: BinaryOperation, tabsCount: Int) {
        addIdent(tabsCount)
        append("BINARY OP: ${binaryOperation.kind.name} ${binaryOperation.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("LEFT:\n")
        binaryOperation.leftOperand.accept(this, tabsCount + 2)
        append('\n')
        addIdent(tabsCount + 1)
        append("RIGHT:\n")
        binaryOperation.rightOperand.accept(this, tabsCount + 2)
    }

    override fun visit(unaryOperation: UnaryOperation, tabsCount: Int) {
        addIdent(tabsCount)
        append("UNARY OP: ${unaryOperation.kind.name} ${unaryOperation.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("OPERAND:\n")
        unaryOperation.operand.accept(this, tabsCount + 2)
    }

    override fun visit(constructorCall: ConstructorCall, tabsCount: Int) {
        addIdent(tabsCount)
        append("CONSTRUCTOR CALL: ${constructorCall.type.identifier.value} ${constructorCall.location.dumpWithoutFilename()}")
        if (constructorCall.arguments.isNotEmpty()) {
            append('\n')
            constructorCall.arguments.accept(this, tabsCount + 1)
        }
    }

    override fun visit(arrayCreation: ArrayCreation, tabsCount: Int) {
        addIdent(tabsCount)
        append("ARRAY CREATION: ${arrayCreation.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("COMPONENT TYPE:\n")
        arrayCreation.type.accept(this, tabsCount + 2)
        append('\n')
        addIdent(tabsCount + 1)
        append("DIMENSIONS:")
        arrayCreation.dimensions.forEach {
            append('\n')
            it.accept(this, tabsCount + 2)
        }
    }

    override fun visit(expressionStatement: ExpressionStatement, tabsCount: Int) {
        addIdent(tabsCount)
        append("EXPRESSION STATEMENT: ${expressionStatement.location.dumpWithoutFilename()}\n")
        expressionStatement.expression.accept(this, tabsCount + 1)
    }

    override fun visit(ifStatement: IfStatement, tabsCount: Int) {
        addIdent(tabsCount)
        append("IF: ${ifStatement.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("CONDITION:\n")
        ifStatement.condition.accept(this, tabsCount + 2)
        append('\n')
        addIdent(tabsCount + 1)
        append("THEN:\n")
        ifStatement.thenStatements.accept(this, tabsCount + 2)
        if (ifStatement.elseStatements != null) {
            append("\n")
            addIdent(tabsCount + 1)
            append("ELSE:\n")
            ifStatement.elseStatements.accept(this, tabsCount + 2)
        }
    }

    override fun visit(whileStatement: WhileStatement, tabsCount: Int) {
        addIdent(tabsCount)
        append("WHILE: ${whileStatement.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("CONDITION:\n")
        whileStatement.condition.accept(this, tabsCount + 2)
        append('\n')
        addIdent(tabsCount + 1)
        append("BODY:\n")
        whileStatement.body.accept(this, tabsCount + 2)
    }

    override fun visit(forStatement: ForStatement, tabsCount: Int) {
        addIdent(tabsCount)
        append("FOR: ${forStatement.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("INIT:\n")
        forStatement.initStatement.accept(this, tabsCount + 2)
        append("\n")
        addIdent(tabsCount + 1)
        append("CONDITION:\n")
        forStatement.condition.accept(this, tabsCount + 2)
        append("\n")
        addIdent(tabsCount + 1)
        append("UPDATE:\n")
        forStatement.updateStatement.accept(this, tabsCount + 2)
        append("\n")
        addIdent(tabsCount + 1)
        append("BODY:\n")
        forStatement.body.accept(this, tabsCount + 2)
    }

    override fun visit(returnStatement: ReturnStatement, tabsCount: Int) {
        addIdent(tabsCount)
        append("RETURN ${returnStatement.location.dumpWithoutFilename()}")
        if (returnStatement.value != null) {
            append(":\n")
            addIdent(tabsCount + 1)
            append("VALUE:\n")
            returnStatement.value.accept(this, tabsCount + 2)
        }
    }

    override fun visit(assignment: Assignment, tabsCount: Int) {
        addIdent(tabsCount)
        append("ASSIGN: ${assignment.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("LVALUE:\n")
        assignment.lValue.accept(this, tabsCount + 2)
        append("\n")
        addIdent(tabsCount + 1)
        append("RVALUE:\n")
        assignment.rValue.accept(this, tabsCount + 2)
    }

    override fun visit(modifier: Modifier, tabsCount: Int) {
        addIdent(tabsCount)
        append(modifier.type.name)
    }

    override fun visit(modifiersList: ModifiersList, tabsCount: Int) {
        addIdent(tabsCount)
        append(modifiersList.joinToString(prefix = "[", postfix = "]") { it.type.name })
    }

    override fun visit(parametersList: ParametersList, tabsCount: Int) {
        addIdent(tabsCount)
        append("PARAMETERS: ${parametersList.location.dumpWithoutFilename()}")
        parametersList.forEach {
            append('\n')
            it.accept(this, tabsCount + 1)
        }
    }

    override fun visit(argumentsList: ArgumentsList, tabsCount: Int) {
        addIdent(tabsCount)
        append("ARGUMENTS: ${argumentsList.location.dumpWithoutFilename()}")
        argumentsList.forEach {
            append('\n')
            it.accept(this, tabsCount + 1)
        }
    }

    override fun visit(block: Block, tabsCount: Int) {
        addIdent(tabsCount)
        append("BLOCK: ${block.location.dumpWithoutFilename()}")
        block.forEach {
            append('\n')
            it.accept(this, tabsCount + 1)
        }
    }

    override fun visit(fieldDeclaration: FieldDeclaration, tabsCount: Int) {
        addIdent(tabsCount)
        append("FIELD: ${fieldDeclaration.name.value} ")
        if (fieldDeclaration.modifiers.isNotEmpty()) {
            fieldDeclaration.modifiers.accept(this, 0)
        }
        append(" ${fieldDeclaration.location.dumpWithoutFilename()}")
        append('\n')
        addIdent(tabsCount + 1)
        append("TYPE: ")
        fieldDeclaration.type.accept(this, 0)
        if (fieldDeclaration.initializer != null) {
            append('\n')
            addIdent(tabsCount + 1)
            append("INIT:\n")
            fieldDeclaration.initializer.accept(this, tabsCount + 2)
        }
    }

    override fun visit(variableDeclaration: VariableDeclaration, tabsCount: Int) {
        addIdent(tabsCount)
        append("VAR: ${variableDeclaration.name.value} ${variableDeclaration.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("TYPE: ")
        variableDeclaration.type.accept(this, 0)
        if (variableDeclaration.initializer != null) {
            append('\n')
            addIdent(tabsCount + 1)
            append("INIT:\n")
            variableDeclaration.initializer.accept(this, tabsCount + 2)
        }
    }

    override fun visit(methodDeclaration: MethodDeclaration, tabsCount: Int) {
        addIdent(tabsCount)
        append("METHOD: ${methodDeclaration.name.value}")
        if (methodDeclaration.modifiers.isNotEmpty()) {
            methodDeclaration.modifiers.accept(this, 0)
        }
        append(" ${methodDeclaration.location.dumpWithoutFilename()}")
        append('\n')
        addIdent(tabsCount + 1)
        append("RETURN TYPE: ")
        if (methodDeclaration.returnType != null) {
            methodDeclaration.returnType.accept(this, 0)
        } else {
            append("VOID")
        }
        if (methodDeclaration.parameters.isNotEmpty()) {
            append('\n')
            methodDeclaration.parameters.accept(this, tabsCount + 1)
        }
        if (methodDeclaration.body != null) {
            append('\n')
            addIdent(tabsCount + 1)
            append("BODY:\n")
            methodDeclaration.body.accept(this, tabsCount + 2)
        }
    }

    override fun visit(constructorDeclaration: ConstructorDeclaration, tabsCount: Int) {
        addIdent(tabsCount)
        append("CONSTRUCTOR: ")
        constructorDeclaration.modifiers.accept(this, 0)
        if (constructorDeclaration.parameters.isNotEmpty()) {
            append('\n')
            constructorDeclaration.parameters.accept(this, tabsCount + 1)
        }
        append(" ${constructorDeclaration.location.dumpWithoutFilename()}")
        append('\n')
        addIdent(tabsCount + 1)
        append("BODY:\n")
        constructorDeclaration.body.accept(this, tabsCount + 2)
    }

    override fun visit(staticInitBlock: StaticInitBlock, tabsCount: Int) {
        addIdent(tabsCount)
        append("STATIC INIT: ${staticInitBlock.location.dumpWithoutFilename()}\n")
        staticInitBlock.body.accept(this, tabsCount + 1)
    }

    override fun visit(classDeclaration: ClassDeclaration, tabsCount: Int) {
        addIdent(tabsCount)
        append("${if (classDeclaration.type == ClassKind.INTERFACE) "interface" else "class"}: ${classDeclaration.name.value} ${classDeclaration.location.dumpWithoutFilename()}")
        if (classDeclaration.modifiers.isNotEmpty()) {
            classDeclaration.modifiers.accept(this, 0)
        }
        if (classDeclaration.superClasses.isNotEmpty()) {
            append('\n')
            addIdent(tabsCount + 1)
            append("SUPERCLASSES: ${classDeclaration.superClasses.joinToString { it.value }}")
        }
        classDeclaration.members.forEach {
            append('\n')
            it.accept(this, tabsCount + 1)
        }
    }

    override fun visit(sourceFile: SourceFile, tabsCount: Int) {
        sourceFile.classes.forEach {
            append('\n')
            it.accept(this, tabsCount)
        }
    }

    override fun visit(methodCall: MethodCall, tabsCount: Int) {
        addIdent(tabsCount)
        append("METHOD CALL: ${methodCall.name.value} ${methodCall.location.dumpWithoutFilename()}\n")
        addIdent(tabsCount + 1)
        append("TARGET:\n")
        methodCall.target.accept(this, tabsCount + 2)
        if (methodCall.arguments.isNotEmpty()) {
            append('\n')
            methodCall.arguments.accept(this, tabsCount + 1)
        }
    }

    override fun visit(stringLiteral: StringLiteral, tabsCount: Int) {
        addIdent(tabsCount)
        append("\"${stringLiteral.value}\" ${stringLiteral.location.dumpWithoutFilename()}")
    }

    override fun visit(superCall: SuperCall, tabsCount: Int) {
        addIdent(tabsCount)
        append("SUPER CALL:")
        if (superCall.arguments.isNotEmpty()) {
            append('\n')
            superCall.arguments.accept(this, tabsCount + 1)
        }
    }
}