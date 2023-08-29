import kotlin.test.Test
import org.junit.jupiter.api.Nested

class DiagnosticTests {

    @Nested
    inner class Smoke {
        @Test
        fun testStaticConstructor() {
            runDiagnosticTest("/smoke/staticConstructor")
        }

        @Test
        fun testAbstractConstructor() {
            runDiagnosticTest("/smoke/abstractConstructor")
        }

        @Test
        fun testPublicClass() {
            runDiagnosticTest("/smoke/publicClass")
        }

        @Test
        fun testInterfaceWithStaticInitBlock() {
            runDiagnosticTest("/smoke/interfaceWithStaticInitBlock")
        }

        @Test
        fun testReturnFromVoid() {
            runDiagnosticTest("/smoke/returnFromVoid")
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
        fun testNoReturn() {
            runDiagnosticTest("/smoke/noReturn")
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
        fun testStringSubtyping() {
            runDiagnosticTest("/smoke/stringSubtyping")
        }

        @Test
        fun testInterfaceInheritsClass() {
            runDiagnosticTest("/smoke/interfaceInheritsClass")
        }

        @Test
        fun testAbstractField() {
            runDiagnosticTest("/smoke/abstractField")
        }

        @Test
        fun testProtectedClass() {
            runDiagnosticTest("/smoke/protectedClass")
        }

    }
}
