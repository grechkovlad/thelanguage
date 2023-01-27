package ast

import ClassKind
import EMPTY_LOCATION
import Location
import ModifierType

sealed class AstNode(val location: Location) {
    abstract fun <T> accept(visitor: VoidAstVisitor<T>, context: T)
}

sealed class TypeReference(location: Location) : AstNode(location)

class SimpleTypeReference(val identifier: Identifier) : TypeReference(identifier.location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayTypeReference(val componentTypeReference: TypeReference, location: Location) :
    TypeReference(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class StringLiteral(val value: String, location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class IntLiteral(val value: Int, location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FloatLiteral(val value: Float, location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Identifier(val value: String, location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Null(location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class This(location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayAccess(val array: Expression, val index: Expression, location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class FieldAccess(val target: Expression, val name: Identifier, location: Location) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Parameter(val type: TypeReference, val name: Identifier, location: Location) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

sealed class Expression(location: Location) : AstNode(location)

enum class BinaryOperationKind {
    PLUS, MINUS, MULT, DIV, LESS, LEQ, GREATER, GEQ, AND, OR, EQ, NOT_EQ
}

class BinaryOperation(
    val kind: BinaryOperationKind,
    val leftOperand: Expression,
    val rightOperand: Expression,
    location: Location
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

enum class UnaryOperationKind {
    MINUS, NOT
}

class UnaryOperation(val kind: UnaryOperationKind, val operand: Expression, location: Location) :
    Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class MethodCall(
    val target: Expression, val name: Identifier, val arguments: ArgumentsList, location: Location
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ConstructorCall(
    val type: SimpleTypeReference, val arguments: ArgumentsList, location: Location
) : Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class SuperCall(val arguments: ArgumentsList, location: Location) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArrayCreation(val type: SimpleTypeReference, val dimensions: List<Expression>, location: Location) :
    Expression(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

sealed class Statement(location: Location) : AstNode(location)

class ExpressionStatement(val expression: Expression, location: Location) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class IfStatement(
    val condition: Expression,
    val thenStatements: Block,
    val elseStatements: Block?,
    location: Location
) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class WhileStatement(val condition: Expression, val body: Block, location: Location) :
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
    location: Location
) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ReturnStatement(val value: Expression?, location: Location) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Assignment(val lValue: Expression, val rValue: Expression, location: Location) : Statement(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

sealed class MemberDeclaration(val modifiers: ModifiersList, location: Location) : Statement(location)

class Modifier(val type: ModifierType, location: Location) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ModifiersList(val modifiers: List<Modifier>, location: Location) : AstNode(location),
    List<Modifier> by modifiers {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

private val EMPTY_MODIFIER_LIST = ModifiersList(emptyList(), EMPTY_LOCATION)

class ParametersList(val parameters: List<Parameter>, location: Location) : AstNode(location),
    List<Parameter> by parameters {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ArgumentsList(val arguments: List<Expression>, location: Location) : AstNode(location),
    List<Expression> by arguments {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class Block(val statements: List<Statement>, location: Location) : AstNode(location),
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
    location: Location,
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class VariableDeclaration(
    val name: Identifier,
    val type: TypeReference,
    val initializer: Expression?,
    location: Location
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
    location: Location
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class ConstructorDeclaration(
    modifiers: ModifiersList,
    val parameters: ParametersList,
    val body: Block,
    location: Location
) : MemberDeclaration(modifiers, location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class StaticInitBlock(val body: Block, location: Location) :
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
    location: Location
) : AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}

class SourceFile(val classes: List<ClassDeclaration>, location: Location) :
    AstNode(location) {
    override fun <T> accept(visitor: VoidAstVisitor<T>, context: T) {
        visitor.visit(this, context)
    }
}