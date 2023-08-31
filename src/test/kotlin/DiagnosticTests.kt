import kotlin.test.Test
import org.junit.jupiter.api.Nested

class DiagnosticTests {

    @Nested
    inner class Smoke {
        @Test
        fun testReservedClassName() {
            runDiagnosticTest("/smoke/reservedClassName")
        }

        @Test
        fun testStaticConstructor() {
            runDiagnosticTest("/smoke/staticConstructor")
        }

        @Test
        fun testSignatureDeclarationClash() {
            runDiagnosticTest("/smoke/signatureDeclarationClash")
        }

        @Test
        fun testMultipleDeclarationOfStaticInitBlock() {
            runDiagnosticTest("/smoke/multipleDeclarationOfStaticInitBlock")
        }

        @Test
        fun testOverridingRestrictsVisibility() {
            runDiagnosticTest("/smoke/overridingRestrictsVisibility")
        }

        @Test
        fun testCanNotAccessProtectedMember() {
            runDiagnosticTest("/smoke/canNotAccessProtectedMember")
        }

        @Test
        fun testAbstractConstructor() {
            runDiagnosticTest("/smoke/abstractConstructor")
        }

        @Test
        fun testMissingReturnStatement() {
            runDiagnosticTest("/smoke/missingReturnStatement")
        }

        @Test
        fun testAbstractMethodWithBody() {
            runDiagnosticTest("/smoke/abstractMethodWithBody")
        }

        @Test
        fun testStaticMethodCanNotOverrideInstanceMethod() {
            runDiagnosticTest("/smoke/staticMethodCanNotOverrideInstanceMethod")
        }

        @Test
        fun testMethodWithBodyInInterface() {
            runDiagnosticTest("/smoke/methodWithBodyInInterface")
        }

        @Test
        fun testPublicClass() {
            runDiagnosticTest("/smoke/publicClass")
        }

        @Test
        fun testConstructorMustBeginWithSuperCall() {
            runDiagnosticTest("/smoke/constructorMustBeginWithSuperCall")
        }

        @Test
        fun testAmbiguousCall() {
            runDiagnosticTest("/smoke/ambiguousCall")
        }

        @Test
        fun testUnresolvedConstructorCall() {
            runDiagnosticTest("/smoke/unresolvedConstructorCall")
        }

        @Test
        fun testInterfaceWithStaticInitBlock() {
            runDiagnosticTest("/smoke/interfaceWithStaticInitBlock")
        }

        @Test
        fun testNonAbstractClassMustOverrideAbstractMethod() {
            runDiagnosticTest("/smoke/nonAbstractClassMustOverrideAbstractMethod")
        }

        @Test
        fun testReturnFromVoid() {
            runDiagnosticTest("/smoke/returnFromVoid")
        }

        @Test
        fun testBinaryOperatorInapplicable() {
            runDiagnosticTest("/smoke/binaryOperatorInapplicable")
        }

        @Test
        fun testInterfaceWithModifier() {
            runDiagnosticTest("/smoke/interfaceWithModifier")
        }

        @Test
        fun testIllegalReturnType() {
            runDiagnosticTest("/smoke/illegalReturnType")
        }

        @Test
        fun testInterfaceHasField() {
            runDiagnosticTest("/smoke/interfaceHasField")
        }

        @Test
        fun testCanNotAccessPrivateMember() {
            runDiagnosticTest("/smoke/canNotAccessPrivateMember")
        }

        @Test
        fun testIllegalReturnTypeInOverriding() {
            runDiagnosticTest("/smoke/illegalReturnTypeInOverriding")
        }

        @Test
        fun testCyclicInheritance() {
            runDiagnosticTest("/smoke/cyclicInheritance")
        }

        @Test
        fun testMultipleInheritance() {
            runDiagnosticTest("/smoke/multipleInheritance")
        }

        @Test
        fun testPrivateClass() {
            runDiagnosticTest("/smoke/privateClass")
        }

        @Test
        fun testAccessToNonStaticSymbolFromStaticContext() {
            runDiagnosticTest("/smoke/accessToNonStaticSymbolFromStaticContext")
        }

        @Test
        fun testUnresolvedReference() {
            runDiagnosticTest("/smoke/unresolvedReference")
        }

        @Test
        fun testIllegalSuperCall() {
            runDiagnosticTest("/smoke/illegalSuperCall")
        }

        @Test
        fun testRepeatedModifer() {
            runDiagnosticTest("/smoke/repeatedModifer")
        }

        @Test
        fun testStaticClass() {
            runDiagnosticTest("/smoke/staticClass")
        }

        @Test
        fun testConstructorInInterface() {
            runDiagnosticTest("/smoke/constructorInInterface")
        }

        @Test
        fun testStringSubtyping() {
            runDiagnosticTest("/smoke/stringSubtyping")
        }

        @Test
        fun testNumericTypeExpected() {
            runDiagnosticTest("/smoke/numericTypeExpected")
        }

        @Test
        fun testArrayExpected() {
            runDiagnosticTest("/smoke/arrayExpected")
        }

        @Test
        fun testInterfaceInheritsClass() {
            runDiagnosticTest("/smoke/interfaceInheritsClass")
        }

        @Test
        fun testConflictingAccessModifiers() {
            runDiagnosticTest("/smoke/conflictingAccessModifiers")
        }

        @Test
        fun testAbstractField() {
            runDiagnosticTest("/smoke/abstractField")
        }

        @Test
        fun testStaticMethodCanNotBeOverridden() {
            runDiagnosticTest("/smoke/staticMethodCanNotBeOverridden")
        }

        @Test
        fun testProtectedClass() {
            runDiagnosticTest("/smoke/protectedClass")
        }

        @Test
        fun testNameDeclarationClash() {
            runDiagnosticTest("/smoke/nameDeclarationClash")
        }

        @Test
        fun testAbstractMethodInNonAbstractClass() {
            runDiagnosticTest("/smoke/abstractMethodInNonAbstractClass")
        }

        @Test
        fun testIllegalLValue() {
            runDiagnosticTest("/smoke/illegalLValue")
        }

    }
}
