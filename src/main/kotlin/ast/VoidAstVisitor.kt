package ast

interface VoidAstVisitor<T> {
    fun visit(simpleTypeReference: SimpleTypeReference, context: T)
    fun visit(arrayTypeReference: ArrayTypeReference, context: T)
    fun visit(intLiteral: IntLiteral, context: T)
    fun visit(floatLiteral: FloatLiteral, context: T)
    fun visit(boolLiteral: BoolLiteral, context: T)
    fun visit(identifier: Identifier, context: T)
    fun visit(arg: Null, context: T)
    fun visit(arg: This, context: T)
    fun visit(arrayAccess: ArrayAccess, context: T)
    fun visit(fieldAccess: FieldAccess, context: T)
    fun visit(parameter: Parameter, context: T)
    fun visit(binaryOperation: BinaryOperation, context: T)
    fun visit(unaryOperation: UnaryOperation, context: T)
    fun visit(constructorCall: ConstructorCall, context: T)
    fun visit(arrayCreation: ArrayCreation, context: T)
    fun visit(expressionStatement: ExpressionStatement, context: T)
    fun visit(ifStatement: IfStatement, context: T)
    fun visit(whileStatement: WhileStatement, context: T)
    fun visit(forStatement: ForStatement, context: T)
    fun visit(returnStatement: ReturnStatement, context: T)
    fun visit(assignment: Assignment, context: T)
    fun visit(modifier: Modifier, context: T)
    fun visit(modifiersList: ModifiersList, context: T)
    fun visit(parametersList: ParametersList, context: T)
    fun visit(argumentsList: ArgumentsList, context: T)
    fun visit(block: Block, context: T)
    fun visit(fieldDeclaration: FieldDeclaration, context: T)
    fun visit(localVariableDeclaration: LocalVariableDeclaration, context: T)
    fun visit(methodDeclaration: MethodDeclaration, context: T)
    fun visit(constructorDeclaration: ConstructorDeclaration, context: T)
    fun visit(staticInitBlock: StaticInitBlock, context: T)
    fun visit(classDeclaration: ClassDeclaration, context: T)
    fun visit(sourceFile: SourceFile, context: T)
    fun visit(methodCall: MethodCall, context: T)
    fun visit(stringLiteral: StringLiteral, context: T)
    fun visit(superCall: SuperCall, context: T)
    fun visit(breakStatement: Break, context: T)
    fun visit(continueStatement: Continue, context: T)
    fun visit(typeOp: TypeOp, context: T)
}