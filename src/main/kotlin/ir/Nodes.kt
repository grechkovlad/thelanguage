package ir

import ClassKind
import ModifierType
import java.lang.reflect.Type

sealed class IrNode

class Project(val classes: List<ClassDeclaration>)

class ClassDeclaration(val name: String, val kind: ClassKind, override val modifiers: Modifiers) : IrNode(),
    ModifiersBearer {
    lateinit var fields: List<FieldDeclaration>
    lateinit var declaredMethods: List<MethodDeclaration>
    lateinit var constructors: List<ConstructorDeclaration>
    lateinit var staticInit: StaticInitBlock
    lateinit var superClass: ClassReference
    lateinit var interfaces: List<UserClassReference>

    val reference = UserClassReference(this)
}

abstract class Member(override val modifiers: Modifiers) : IrNode(), ModifiersBearer

sealed class TypeReference

object IntTypeReference : TypeReference()
object FloatTypeReference : TypeReference()
object BoolTypeReference : TypeReference()
object VoidTypeReference : TypeReference()
object NullTypeReference : TypeReference()

sealed class ClassReference : TypeReference() {
    abstract val isInterface: Boolean
    abstract val name: String
    abstract val constructors: List<ConstructorReference>
}

object ObjectClassReference : ClassReference() {
    override val isInterface
        get() = false
    override val name: String
        get() = "Object"
    override val constructors: List<ConstructorReference> = listOf(ObjectConstructorReference)
}

object StringClassReference : ClassReference() {
    override val isInterface
        get() = false

    override val name: String
        get() = "String"
    override val constructors: List<ConstructorReference>
        get() = emptyList()
}

object SystemClassReference : ClassReference() {
    override val isInterface
        get() = false

    override val name: String
        get() = "System"
    override val constructors: List<ConstructorReference>
        get() = emptyList()
}

object UtilsClassReference : ClassReference() {
    override val isInterface: Boolean
        get() = false

    override val name: String
        get() = "Utils"

    override val constructors: List<ConstructorReference>
        get() = emptyList()
}

data class UserClassReference(val declaration: ClassDeclaration) : ClassReference() {
    override val isInterface
        get() = declaration.kind == ClassKind.INTERFACE
    override val name: String
        get() = declaration.name

    override val constructors get() = declaration.constructors.map { it.reference }
}

data class ArrayTypeReference(val componentType: TypeReference) : TypeReference()

open class VariableDeclaration(val name: String, val type: TypeReference)

class LocalVariableDeclaration(name: String, type: TypeReference, val initializer: Expression?) :
    VariableDeclaration(name, type), Statement

abstract class VariableReference(val type: TypeReference) {
    abstract val declaration: VariableDeclaration
}

data class LocalVariableReference(override val declaration: LocalVariableDeclaration) :
    VariableReference(declaration.type)

data class ParameterReference(override val declaration: ParameterDeclaration) : VariableReference(declaration.type)

class FieldDeclaration(
    val name: String,
    val type: TypeReference,
    modifiers: Modifiers,
    val declaringClass: UserClassReference
) : Member(modifiers),
    ModifiersBearer {
    var initializer: Expression? = null

    val reference = UserClassFieldReference(this)
}

sealed class FieldReference :
    ModifiersBearer {
    abstract val fieldType: TypeReference
    abstract val declaringClass: ClassReference
}

class SetField(fieldReference: FieldReference, target: Expression, val value: Expression) : Statement

class GetField(fieldReference: FieldReference, target: Expression) : Expression(fieldReference.fieldType)

class ArrayLength(val target: Expression) : Expression(IntTypeReference)

class ParameterRead(val parameterReference: ParameterReference) : Expression(parameterReference.type)

class ParameterWrite(val parameterReference: ParameterReference) : Expression(parameterReference.type)

abstract class ArrayElementAccess(val target: Expression, val index: Expression)

class GetArrayElement(array: Expression, index: Expression, type: TypeReference) : Expression(type)

class SetArrayElement(array: Expression, index: Expression, val value: Expression) : ArrayElementAccess(array, index),
    Statement

class CreateArray(val elementType: TypeReference, val dimensions: List<Expression>) :
    Expression((dimensions.indices).fold(elementType) { type, _ -> ArrayTypeReference(type) })


sealed class ExecutableReference(val declaringClass: ClassReference, val parameterTypes: List<TypeReference>) :
    ModifiersBearer

abstract class MethodReference(
    declaringClass: ClassReference,
    val returnType: TypeReference,
    parameterTypes: List<TypeReference>,
    val name: String
) :
    ExecutableReference(declaringClass, parameterTypes)

abstract class ConstructorReference(declaringClass: ClassReference, parameterTypes: List<TypeReference>) :
    ExecutableReference(declaringClass, parameterTypes)

object ObjectConstructorReference : ConstructorReference(ObjectClassReference, emptyList()) {
    override val modifiers = emptyList<ModifierType>()
}

data class UserClassConstructorReference(val declaration: ConstructorDeclaration) :
    ConstructorReference(declaration.declaringClass.reference, declaration.parameters.map { it.type }) {
    override val modifiers: Modifiers
        get() = declaration.modifiers
}

class UserConstructorReference(val declaration: ConstructorDeclaration) :
    ConstructorReference(declaration.declaringClass.reference, declaration.parameters.map { it.type }) {
    override val modifiers: Modifiers
        get() = declaration.modifiers
}

class BuiltinMethodReference(
    override val modifiers: Modifiers, declaringClass: ClassReference,
    returnType: TypeReference, parameterTypes: List<TypeReference>, name: String
) : MethodReference(
    declaringClass, returnType, parameterTypes, name
)

class UserMethodReference(val declaration: MethodDeclaration) :
    MethodReference(
        declaration.declaringClass.reference,
        declaration.returnType,
        declaration.parameters.map { it.type },
        declaration.name
    ) {
    override val modifiers: Modifiers
        get() = declaration.modifiers
}

class ParameterDeclaration(name: String, type: TypeReference) : VariableDeclaration(name, type)

abstract class ExecutableMember(
    val parameters: List<ParameterDeclaration>,
    modifiers: Modifiers,
    val returnType: TypeReference
) : Member(modifiers) {
    lateinit var body: List<Statement>
}

class MethodDeclaration(
    val declaringClass: ClassDeclaration,
    modifiers: Modifiers,
    val name: String,
    parameters: List<ParameterDeclaration>,
    returnType: TypeReference,
) : ExecutableMember(parameters, modifiers, returnType), ModifiersBearer {

    val reference = UserMethodReference(this)
}

class StaticInitBlock : ExecutableMember(emptyList(), emptyList(), VoidTypeReference)

class ConstructorDeclaration(
    val declaringClass: ClassDeclaration, parameters: List<ParameterDeclaration>, modifiers: Modifiers
) : ExecutableMember(parameters, modifiers, declaringClass.reference) {
    val reference = UserConstructorReference(this)
}

class IntLiteral(val value: Int) : Expression(IntTypeReference)
class FloatLiteral(val value: Float) : Expression(FloatTypeReference)
class StringLiteral(val value: String) : Expression(StringClassReference)

abstract class BinaryOperation(val leftOperand: Expression, val rightOperand: Expression, type: TypeReference) :
    Expression(type)

class Concat(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, StringClassReference)

class Add(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class Subtract(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class Minus(operand: Expression) : Expression(operand.type)

class Not(operand: Expression) : Expression(BoolTypeReference)

class Divide(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class Less(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Leq(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Equals(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class NotEquals(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Geq(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Greater(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Multiply(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class BoolEq(leftOperand: Expression, rightOperand: Expression, val inverted: Boolean) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class NumericEq(leftOperand: Expression, rightOperand: Expression, val inverted: Boolean) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class RefEq(leftOperand: Expression, rightOperand: Expression, val inverted: Boolean) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class And(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Or(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

sealed class BoolLiteral : Expression(BoolTypeReference) {
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

class ThisAccess(type: UserClassReference) : Expression(type)

abstract class Call(returnType: TypeReference, val arguments: List<Expression>) : Expression(returnType)

class Return(val expression: Expression?) : Statement

class SuperCall(val constructor: ConstructorReference, arguments: List<Expression>) :
    Call(VoidTypeReference, arguments), Statement

class ConstructorCall(val constructor: ConstructorReference, arguments: List<Expression>) :
    Call(constructor.declaringClass, arguments)


class MethodCall(val method: MethodReference, val target: Expression, arguments: List<Expression>) :
    Call(method.returnType, arguments)

sealed interface Statement

class SetVariable(val variable: VariableDeclaration, val expression: Expression) : Statement

class GetVariable(val variable: VariableDeclaration) : Expression(variable.type)

class ExpressionStatement(val expression: Expression) : Statement

sealed class Expression(val type: TypeReference)

class If(val condition: Expression, val thenStatements: List<Statement>, val elseStatements: List<Statement>) :
    Statement

class While(val condition: Expression, val body: List<Statement>) : Statement

class For(val init: Statement, val condition: Expression, val update: Statement, val body: List<Statement>) : Statement

object Null : Expression(NullTypeReference)

typealias Modifiers = Collection<ModifierType>

class UserClassFieldReference(val declaration: FieldDeclaration) : FieldReference() {
    override val fieldType: TypeReference
        get() = declaration.type
    override val modifiers: Modifiers
        get() = declaration.modifiers

    override val declaringClass get() = declaration.declaringClass
}

class TypeAccess(val typeReference: TypeReference) : Expression(typeReference)

fun FieldDeclaration.createReference() = UserClassFieldReference(this)

interface ModifiersBearer {
    val modifiers: Modifiers
}