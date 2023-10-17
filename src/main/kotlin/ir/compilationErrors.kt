package ir

import Location
import ModifierType

data class FinalClassSubtyping(val name: String, override val location: Location) : CompilationError(location)

data class MultipleInheritance(override val location: Location) : CompilationError(location)

data class InterfaceInheritsClass(override val location: Location) : CompilationError(location)

data class CyclicInheritance(val name: String, override val location: Location) : CompilationError(location)

data class InterfaceHasFields(override val location: Location) : CompilationError(location)

data class InterfaceHasStaticInitBlock(override val location: Location) : CompilationError(location)

data class ForbiddenModifier(val modifierType: ModifierType, override val location: Location) : CompilationError(location)

data class RepeatedModifier(val modifierType: ModifierType, override val location: Location) : CompilationError(location)

data class IllegalReturnTypeInOverriding(override val location: Location) : CompilationError(location)

data class MissingReturnStatement(override val location: Location) : CompilationError(location)

data class StaticMethodCanNotBeOverridden(override val location: Location) : CompilationError(location)

data class StaticMethodCanNotOverrideInstanceMethod(override val location: Location) : CompilationError(location)

data class OverridingRestrictsVisibility(override val location: Location) : CompilationError(location)

data class NonAbstractClassMustOverrideAbstractMethod(
    override val location: Location, val abstractMethod: MethodReference
) : CompilationError(location)

data class MultipleDeclarationOfStaticInitBlock(override val location: Location) : CompilationError(location)

data class IllegalLValue(override val location: Location) : CompilationError(location)

data class AccessToNonStaticSymbolFromStaticContext(override val location: Location) : CompilationError(location)

data class CanNotAccessPrivateMember(val name: String, override val location: Location) : CompilationError(location)

data class StaticMemberAccessViaInstance(override val location: Location) : CompilationError(location)

data class CanNotAccessProtectedMember(val name: String, override val location: Location) : CompilationError(location)

data class ConstructorInInterface(override val location: Location) : CompilationError(location)

data class BreakOutsideOfLoop(override val location: Location) : CompilationError(location)

data class ContinueOutsideLoop(override val location: Location) : CompilationError(location)

data class TypeMismatch(val expected: TypeReference, val actual: TypeReference, override val location: Location) :
    CompilationError(location)

data class NumericTypeExpected(override val location: Location) : CompilationError(location)

data class BinaryOperatorInapplicable(val leftType: TypeReference, val rightType: TypeReference, override val location: Location) :
    CompilationError(location)

data class ArrayExpected(override val location: Location) : CompilationError(location)

sealed class CompilationError(open val location: Location) : RuntimeException()

data class AbstractMethodInNonAbstractClass(override val location: Location) : CompilationError(location)

data class MethodWithBodyInInterface(override val location: Location) : CompilationError(location)

data class AbstractMethodWithBody(override val location: Location) : CompilationError(location)

data class NameDeclarationClash(val name: String, override val location: Location) : CompilationError(location)

data class ConflictingAccessModifiers(override val location: Location) : CompilationError(location)

data class SignatureDeclarationClash(val signature: String, override val location: Location) :
    CompilationError(location)

data class ReservedClassName(val name: String, override val location: Location) : CompilationError(location)

data class UnresolvedReference(val name: String, override val location: Location) : CompilationError(location)

data class UnresolvedConstructorCall(
    override val location: Location,
    val classReference: ClassReference,
    val argTypes: List<TypeReference>
) :
    CompilationError(location)

data class IllegalSuperCall(override val location: Location) : CompilationError(location)

data class AmbiguousCall(
    override val location: Location, val candidateOne: ExecutableReference, val candidateTwo: ExecutableReference
) : CompilationError(location)

data class ConstructorMustBeginWithSuperCall(
    override val location: Location,
    val constructorReference: UserConstructorReference
) : CompilationError(location)