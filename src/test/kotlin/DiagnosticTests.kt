import ir.IrBuilder
import parser.Parser
import kotlin.reflect.cast
import kotlin.test.Test
import kotlin.test.assertEquals

class DiagnosticTests {

    @Test
    fun testIllegalReturnType() {
        testOnExample("illegalReturnType")
    }

    @Test
    fun returnFromVoid() {
        testOnExample("returnFromVoid")
    }

    @Test
    fun noReturn() {
        testOnExample("noReturn")
    }

    private fun testOnExample(name: String) {
        val srcText = readFromResources("/diagnostics/$name.lang")
        val expectedCompilationError = retrieveExpectedDiagnostic(srcText, name)
        val ast = Parser(srcText, name, true).parse()
        var exceptionThrown = false
        try {
            IrBuilder(listOf(ast)).build()
        } catch (t: Throwable) {
            exceptionThrown = true
            assert(expectedCompilationError.type.isInstance(t)) {
                "Expected ${expectedCompilationError.type.simpleName}, got ${t::class.simpleName}"
            }
            assertEquals(
                expectedCompilationError.location, expectedCompilationError.type.cast(t).location,
                "Expected $expectedCompilationError, got $t"
            )
        }
        if (!exceptionThrown) {
            assert(false) { "Expected ${expectedCompilationError.type.simpleName}, no diagnostic found" }
        }
    }
}