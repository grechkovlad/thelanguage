package ir

import ClassKind


class Project(val classes: List<ClassDeclaration>)

class ClassDeclaration(val kind: ClassKind, val members: List<Member>)

abstract class Member

sealed class TypeReference

object IrInt : TypeReference()
object IrFloat : TypeReference()
object IrBool : TypeReference()
object IrString : TypeReference()

class UserClassReference(val declaration: ClassDeclaration) : TypeReference()

class ArrayTypeReference(val componentType: TypeReference) : TypeReference()

class LocalVariableDeclaration(val name: String, val type: TypeReference, val initializer: Expression?)

class LocalVariableReference(val declaration: LocalVariableDeclaration)

class FieldDeclaration(val name: String, val type: TypeReference, val initializer: Expression?) : Member()

class FieldReference(val declaration: FieldDeclaration)

class FieldWriteAccess(val fieldReference: FieldReference)

class FieldReadAccess(val fieldReference: FieldReference) : Expression()

class ParameterReference(val declaration: ParameterDeclaration)

class ParameterRead(val parameterReference: ParameterReference) : Expression()

class ParameterWrite(val parameterReference: ParameterReference) : Expression()

class ArrayElementReference(val target: Expression, val index: Expression)

class ArrayElementRead(val arrayElementAccess: ArrayElementReference) : Expression()

class ArrayElementWrite(val arrayElementAccess: ArrayElementReference) : Expression()

sealed class MethodReference

sealed class ConstructorReference

class UserConstructorReference(val declaration: ConstructorDeclaration)

class UserMethodReference(val declaration: MethodDeclaration)

class ParameterDeclaration(val type: TypeReference, val name: String, val index: Int)

class MethodDeclaration(
    val declaringClass: ClassDeclaration,
    val name: String,
    val parameters: List<ParameterDeclaration>,
    val returnType: TypeReference,
    val body: List<Statement>?
) : Member()

class StaticInitBlock(val body : List<Statement>) : Member()

class ConstructorDeclaration(
    val declaringClass: ClassDeclaration,
    val parameters: List<ParameterDeclaration>,
    val body: List<Statement>
) : Member()

class IntLiteral(val value: Int) : Expression()
class FloatLiteral(val value: Float) : Expression()
class StringLiteral(val value: String) : Expression()

sealed class BoolLiteral : Expression() {
    abstract val value: Boolean
}

object TrueLiteral : BoolLiteral() {
    override val value: Boolean
        get() = true
}

object FalseLiteral : BoolLiteral() {
    override val value: Boolean
        get() = false
}

class ThisAccess

class SuperCall(val constructor: ConstructorReference, val arguments: List<Expression>)

sealed class Statement

class ExpressionStatement(val expression: Expression) : Statement()

sealed class Expression

class If(val condition: Expression, val thenStatements: List<Statement>, val elseStatements: List<Statement>)

class While(val condition: Expression, val body: List<Statement>)

class For(val init: Statement, val condition: Expression, val update: Statement, val body: List<Statement>)

object Null : Expression()