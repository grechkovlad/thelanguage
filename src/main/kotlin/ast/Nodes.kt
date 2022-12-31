package ast

import ClassKind

sealed class AstNode(val location: FileRelativeLocation) {
    abstract fun <T> accept(visitor: VoidAstVisitor<T>, context: T)
}

sealed class TypeReference(location: FileRelativeLocation) : AstNode(location)

class SimpleTypeReference(val identifier: Identifier) : TypeReference(identifier.location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayTypeReference(val componentTypeReference: TypeReference, location: FileRelativeLocation) :
    TypeReference(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class StringLiteral(val value: String, location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class IntLiteral(val value: Int, location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FloatLiteral(val value: Float, location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Identifier(val value: String, location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Null(location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class This(location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayAccess(val array: Expression, val index: Expression, location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FieldAccess(val target: Expression, val name: Identifier, location: FileRelativeLocation) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Parameter(val type: TypeReference, val name: Identifier, location: FileRelativeLocation) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

abstract class Expression(location: FileRelativeLocation) : AstNode(location)

enum class BinaryOperationKind {
    PLUS, MINUS, MULT, DIV, LESS, LEQ, GREATER, GEQ, AND, OR, EQ, NOT_EQ
}

class BinaryOperation(
    val kind: BinaryOperationKind,
    val leftOperand: Expression,
    val rightOperand: Expression,
    location: FileRelativeLocation
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

enum class UnaryOperationKind {
    MINUS, NOT
}

class UnaryOperation(val kind: UnaryOperationKind, val operand: Expression, location: FileRelativeLocation) :
    Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class MethodCall(
    val target: Expression, val name: Identifier, val arguments: ArgumentsList, location: FileRelativeLocation
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ConstructorCall(
    val type: SimpleTypeReference, val arguments: ArgumentsList, location: FileRelativeLocation
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class SuperCall(val arguments: ArgumentsList, location: FileRelativeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayCreation(val type: SimpleTypeReference, val dimensions: List<Expression>, location: FileRelativeLocation) :
    Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

sealed class Statement(location: FileRelativeLocation) : AstNode(location)

class ExpressionStatement(val expression: Expression, location: FileRelativeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class IfStatement(
    val condition: Expression,
    val thenStatements: Block,
    val elseStatements: Block?,
    location: FileRelativeLocation
) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class WhileStatement(val condition: Expression, val body: Block, location: FileRelativeLocation) :
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
    location: FileRelativeLocation
) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ReturnStatement(val value: Expression?, location: FileRelativeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Assignment(val lValue: Expression, val rValue: Expression, location: FileRelativeLocation) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

sealed class MemberDeclaration(val modifiers: ModifiersList, location: FileRelativeLocation) : Statement(location)

enum class ModifierType {
    STATIC, PUBLIC, PRIVATE, PROTECTED, ABSTRACT
}

class Modifier(val type: ModifierType, location: FileRelativeLocation) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ModifiersList(val modifiers: List<Modifier>, location: FileRelativeLocation) : AstNode(location),
    List<Modifier> by modifiers {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

private val EMPTY_MODIFIER_LIST = ModifiersList(emptyList(), EMPTY_LOCATION)

class ParametersList(val parameters: List<Parameter>, location: FileRelativeLocation) : AstNode(location),
    List<Parameter> by parameters {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArgumentsList(val arguments: List<Expression>, location: FileRelativeLocation) : AstNode(location),
    List<Expression> by arguments {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Block(val statements: List<Statement>, location: FileRelativeLocation) : AstNode(location),
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
    location: FileRelativeLocation,
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class VariableDeclaration(
    val name: Identifier,
    val type: TypeReference,
    val initializer: Expression?,
    location: FileRelativeLocation
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
    location: FileRelativeLocation
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ConstructorDeclaration(
    modifiers: ModifiersList,
    val parameters: ParametersList,
    val body: Block,
    location: FileRelativeLocation
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class StaticInitBlock(val body: Block, location: FileRelativeLocation) :
    MemberDeclaration(EMPTY_MODIFIER_LIST, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ClassDeclaration(
    val name: Identifier,
    val modifiers: ModifiersList,
    val type: ClassKind,
    val superClasses: List<Identifier>,
    val members: List<MemberDeclaration>,
    location: FileRelativeLocation
) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class SourceFile(val classes: List<ClassDeclaration>, val file: String, location: FileRelativeLocation) :
    AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}