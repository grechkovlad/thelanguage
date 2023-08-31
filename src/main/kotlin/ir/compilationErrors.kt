package ir

import Location
import ModifierType

class FinalClassSubtyping(val name: String, location: Location) : CompilationError(location)

class MultipleInheritance(location: Location) : CompilationError(location)

class InterfaceInheritsClass(location: Location) : CompilationError(location)

class CyclicInheritance(val name: String, location: Location) : CompilationError(location)

class InterfaceHasFields(location: Location) : CompilationError(location)

class InterfaceHasStaticInitBlock(location: Location) : CompilationError(location)

class ForbiddenModifier(val modifierType: ModifierType, location: Location) : CompilationError(location)

class RepeatedModifier(val modifierType: ModifierType, location: Location) : CompilationError(location)

class IllegalReturnTypeInOverriding(location: Location) : CompilationError(location)

class MissingReturnStatement(location: Location) : CompilationError(location)

class StaticMethodCanNotBeOverridden(location: Location) : CompilationError(location)

class StaticMethodCanNotOverrideInstanceMethod(location: Location) : CompilationError(location)

class OverridingRestrictsVisibility(location: Location) : CompilationError(location)

class NonAbstractClassMustOverrideAbstractMethod(
    location: Location, val abstractMethod: MethodReference
) : CompilationError(location)

class MultipleDeclarationOfStaticInitBlock(location: Location) : CompilationError(location)

class IllegalLValue(location: Location) : CompilationError(location)

class AccessToNonStaticSymbolFromStaticContext(location: Location) : CompilationError(location)

class CanNotAccessPrivateMember(val name: String, location: Location) : CompilationError(location)

class CanNotAccessProtectedMember(val name: String, location: Location) : CompilationError(location)

class ConstructorInInterface(location: Location) : CompilationError(location)

class TypeMismatch(val expected: TypeReference, val actual: TypeReference, location: Location) :
    CompilationError(location)

class NumericTypeExpected(location: Location) : CompilationError(location)

class BinaryOperatorInapplicable(val leftType: TypeReference, val rightType: TypeReference, location: Location) :
    CompilationError(location)

class ArrayExpected(location: Location) : CompilationError(location)

sealed class CompilationError(val location: Location) : RuntimeException()

class AbstractMethodInNonAbstractClass(location: Location) : CompilationError(location)

class MethodWithBodyInInterface(location: Location) : CompilationError(location)

class AbstractMethodWithBody(location: Location) : CompilationError(location)

class NameDeclarationClash(val name: String, location: Location) : CompilationError(location)

class ConflictingAccessModifiers(location: Location) : CompilationError(location)

class SignatureDeclarationClash(val signature: String, location: Location) :
    CompilationError(location)

class ReservedClassName(val name: String, location: Location) : CompilationError(location)

class UnresolvedReference(val name: String, location: Location) : CompilationError(location)

class UnresolvedConstructorCall(
    location: Location,
    val classReference: ClassReference,
    val argTypes: List<TypeReference>
) :
    CompilationError(location)

class IllegalSuperCall(location: Location) : CompilationError(location)

class AmbiguousCall(
    location: Location, val candidateOne: ExecutableReference, val candidateTwo: ExecutableReference
) : CompilationError(location)

class ConstructorMustBeginWithSuperCall(
    location: Location,
    val constructorReference: UserConstructorReference
) : CompilationError(location)