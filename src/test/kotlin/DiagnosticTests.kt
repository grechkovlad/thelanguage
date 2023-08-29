import kotlin.test.Test
import org.junit.jupiter.api.Nested

class DiagnosticTests {

    @Nested
    inner class Smoke{
        @Test
        fun testReturnFromVoid() {
            runDiagnosticTest("/smoke/returnFromVoid")
        }

        @Test
        fun testIllegalReturnType() {
            runDiagnosticTest("/smoke/illegalReturnType")
        }

        @Test
        fun testNoReturn() {
            runDiagnosticTest("/smoke/noReturn")
        }

    }
}
