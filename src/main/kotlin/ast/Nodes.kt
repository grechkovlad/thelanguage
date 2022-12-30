package ast

sealed class AstNode(val location: AstNodeLocation) {
    abstract fun <T> accept(visitor: VoidAstVisitor<T>, context: T)
}

sealed class TypeReference(location: AstNodeLocation) : AstNode(location)

class SimpleTypeReference(val identifier: Identifier) : TypeReference(identifier.location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayTypeReference(val componentTypeReference: TypeReference, location: AstNodeLocation) :
    TypeReference(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class StringLiteral(val value: String, location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class IntLiteral(val value: Int, location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FloatLiteral(val value: Float, location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Identifier(val value: String, location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Null(location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class This(location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayAccess(val array: Expression, val index: Expression, location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FieldAccess(val target: Expression, val name: Identifier, location: AstNodeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Parameter(val type: TypeReference, val name: Identifier, location: AstNodeLocation) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

abstract class Expression(location: AstNodeLocation) : AstNode(location)

enum class BinaryOperationKind {
    PLUS, MINUS, MULT, DIV, LESS, LEQ, GREATER, GEQ, AND, OR, EQ, NOT_EQ
}

class BinaryOperation(
    val kind: BinaryOperationKind, val leftOperand: Expression, val rightOperand: Expression, location: AstNodeLocation
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

enum class UnaryOperationKind {
    MINUS, NOT
}

class UnaryOperation(val kind: UnaryOperationKind, val operand: Expression, location: AstNodeLocation) :
    Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class MethodCall(
    val target: Expression, val name: Identifier, val arguments: ArgumentsList, location: AstNodeLocation
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ConstructorCall(
    val type: SimpleTypeReference, val arguments: ArgumentsList, location: AstNodeLocation
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class SuperCall(val arguments: ArgumentsList, location: AstNodeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayCreation(val type: SimpleTypeReference, val dimensions: List<Expression>, location: AstNodeLocation) :
    Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

abstract class Statement(location: AstNodeLocation) : AstNode(location)

class ExpressionStatement(val expression: Expression, location: AstNodeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class IfStatement(
    val condition: Expression,
    val thenStatements: Block,
    val elseStatements: Block?,
    location: AstNodeLocation
) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class WhileStatement(val condition: Expression, val body: Block, location: AstNodeLocation) :
    Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ForStatement(
    val initStatement: Statement,
    val condition: Expression,
    val updateStatement: Statement,
    val body: Block,
    location: AstNodeLocation
) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ReturnStatement(val value: Expression?, location: AstNodeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Assignment(val lValue: Expression, val rValue: Expression, location: AstNodeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

abstract class MemberDeclaration(val modifiers: ModifiersList, location: AstNodeLocation) : Statement(location)

enum class ModifierType {
    STATIC, PUBLIC, PRIVATE, PROTECTED, ABSTRACT
}

class Modifier(val type: ModifierType, location: AstNodeLocation) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ModifiersList(val modifiers: List<Modifier>, location: AstNodeLocation) : AstNode(location),
    List<Modifier> by modifiers {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

private val EMPTY_MODIFIER_LIST = ModifiersList(emptyList(), EMPTY_LOCATION)

class ParametersList(val parameters: List<Parameter>, location: AstNodeLocation) : AstNode(location),
    List<Parameter> by parameters {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArgumentsList(val arguments: List<Expression>, location: AstNodeLocation) : AstNode(location),
    List<Expression> by arguments {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Block(val statements: List<Statement>, location: AstNodeLocation) : AstNode(location),
    List<Statement> by statements {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FieldDeclaration(
    val name: Identifier,
    val type: TypeReference,
    val initializer: Expression?,
    modifiers: ModifiersList,
    location: AstNodeLocation,
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class VariableDeclaration(
    val name: Identifier,
    val type: TypeReference,
    val initializer: Expression?,
    location: AstNodeLocation
) :
    Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class MethodDeclaration(
    val name: Identifier,
    val returnType: TypeReference?,
    modifiers: ModifiersList,
    val parameters: ParametersList,
    val body: Block?,
    location: AstNodeLocation
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ConstructorDeclaration(
    modifiers: ModifiersList,
    val parameters: ParametersList,
    val body: Block,
    location: AstNodeLocation
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class StaticInitBlock(val body: Block, location: AstNodeLocation) : MemberDeclaration(EMPTY_MODIFIER_LIST, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

enum class ClassType {
    CLASS, INTERFACE
}

class ClassDeclaration(
    val name: Identifier,
    val modifiers: ModifiersList,
    val type: ClassType,
    val superClasses: List<Identifier>,
    val members: List<MemberDeclaration>,
    location: AstNodeLocation
) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class SourceFile(val classes: List<ClassDeclaration>, location: AstNodeLocation) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}