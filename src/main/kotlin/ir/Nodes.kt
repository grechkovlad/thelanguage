package ir

import ClassKind
import ModifierType
import ast.TypeOpKind

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

object PrintStringMethod : BuiltinMethodReference(
    listOf(ModifierType.STATIC),
    SystemClassReference,
    VoidTypeReference,
    listOf(StringClassReference),
    "print"
)

object ParseIntMethod : BuiltinMethodReference(
    listOf(ModifierType.STATIC),
    UtilsClassReference,
    IntTypeReference,
    listOf(StringClassReference),
    "parseInt"
)

object PrintIntMethod : BuiltinMethodReference(
    listOf(ModifierType.STATIC),
    SystemClassReference,
    VoidTypeReference,
    listOf(IntTypeReference),
    "print"
)

object ToStringMethod : BuiltinMethodReference(
    emptyList(),
    ObjectClassReference,
    StringClassReference,
    emptyList(),
    "toString"
)

object EqualsMethodInObject : BuiltinMethodReference(
    emptyList(),
    ObjectClassReference,
    BoolTypeReference,
    listOf(ObjectClassReference),
    "equals"
)

object EqualsMethodInString : BuiltinMethodReference(
    emptyList(),
    StringClassReference,
    BoolTypeReference,
    listOf(ObjectClassReference),
    "equals"
)

object HashCodeMethod : BuiltinMethodReference(
    emptyList(),
    ObjectClassReference,
    IntTypeReference,
    emptyList(),
    "hashCode"
)

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

sealed class FieldReference(val name: String) :
    ModifiersBearer {
    abstract val fieldType: TypeReference
    abstract val declaringClass: ClassReference
}

class SetField(val fieldReference: FieldReference, val target: Expression, val value: Expression) : Statement

class GetField(val fieldReference: FieldReference, val target: Expression) : Expression(fieldReference.fieldType)

class ArrayLength(val target: Expression) : Expression(IntTypeReference)

abstract class ArrayElementAccess(val target: Expression, val index: Expression)

class GetArrayElement(val array: Expression, val index: Expression, type: TypeReference) : Expression(type)

class SetArrayElement(array: Expression, index: Expression, val value: Expression) : ArrayElementAccess(array, index),
    Statement

class CreateArray(val elementType: TypeReference, val dimensions: List<Expression>) :
    Expression((dimensions.indices).fold(elementType) { type, _ -> ArrayTypeReference(type) })


sealed class ExecutableReference(val declaringClass: ClassReference, val parameterTypes: List<TypeReference>) :
    ModifiersBearer

sealed class MethodReference(
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

sealed class BuiltinMethodReference(
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
    val declaringClass: ClassDeclaration,
    val parameters: List<ParameterDeclaration>,
    modifiers: Modifiers,
    val returnType: TypeReference
) : Member(modifiers) {
    lateinit var body: List<Statement>
}

class MethodDeclaration(
    declaringClass: ClassDeclaration,
    modifiers: Modifiers,
    val name: String,
    parameters: List<ParameterDeclaration>,
    returnType: TypeReference,
) : ExecutableMember(declaringClass, parameters, modifiers, returnType), ModifiersBearer {

    val reference = UserMethodReference(this)
}

class StaticInitBlock(declaringClass: ClassDeclaration) :
    ExecutableMember(declaringClass, emptyList(), emptyList(), VoidTypeReference)

class ConstructorDeclaration(
    declaringClass: ClassDeclaration, parameters: List<ParameterDeclaration>, modifiers: Modifiers
) : ExecutableMember(declaringClass, parameters, modifiers, declaringClass.reference) {
    val reference = UserConstructorReference(this)
}

class IntLiteral(val value: Int) : Expression(IntTypeReference)
class FloatLiteral(val value: Float) : Expression(FloatTypeReference)
class StringLiteral(val value: String) : Expression(StringClassReference)

sealed class TypeOp(
    val expression: Expression,
    val kind: TypeOpKind,
    val typeOperand: TypeReference,
    type: TypeReference
) : Expression(type)

class Is(expression: Expression, typeOperand: TypeReference) :
    TypeOp(expression, TypeOpKind.IS, typeOperand, BoolTypeReference)

class As(expression: Expression, typeOperand: TypeReference) :
    TypeOp(expression, TypeOpKind.AS, typeOperand, typeOperand)

sealed class BinaryOperation(val leftOperand: Expression, val rightOperand: Expression, type: TypeReference) :
    Expression(type)

class Concat(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, StringClassReference)

class Add(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class Subtract(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class Minus(val operand: Expression) : Expression(operand.type)

class Not(val operand: Expression) : Expression(BoolTypeReference)

class Divide(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class Less(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Leq(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Geq(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Greater(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class Multiply(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, binaryNumericPromote(leftOperand.type, rightOperand.type))

class BoolEq(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class NumericEq(leftOperand: Expression, rightOperand: Expression) :
    BinaryOperation(leftOperand, rightOperand, BoolTypeReference)

class RefEq(leftOperand: Expression, rightOperand: Expression) :
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

sealed class Call(returnType: TypeReference, val arguments: List<Expression>) : Expression(returnType)

class Return(val expression: Expression?) : Statement

object Break : Statement

object Continue : Statement

class SuperCall(val constructor: ConstructorReference, arguments: List<Expression>) :
    Call(VoidTypeReference, arguments)

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

class UserClassFieldReference(val declaration: FieldDeclaration) : FieldReference(declaration.name) {
    override val fieldType: TypeReference
        get() = declaration.type
    override val modifiers: Modifiers
        get() = declaration.modifiers

    override val declaringClass get() = declaration.declaringClass
}

class TypeAccess(val typeReference: TypeReference) : Expression(typeReference)

interface ModifiersBearer {
    val modifiers: Modifiers
}