import lexer.*
import java.lang.StringBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LexerTests {
    @Test
    fun testFib() {
        testOnExample("fib")
    }

    @Test
    fun testSorts() {
        testOnExample("sorts")
    }

    @Test
    fun testBasicOOP() {
        testOnExample("basicOOP")
    }

    @Test
    fun testArithmeticExpressions() {
        testOnExample("expressions")
    }

    @Test
    fun testTargetedExpressions() {
        testOnExample("targetedExpressions")

    }

    @Test
    fun testMisc() {
        testOnExample("misc")
    }

    @Test
    fun testCast() {
        testOnExample("simpleCast")
    }

    @Test
    fun testRollback() {
        val lexer = LL3Lexer("class A { }", DiagnosticMarkupSupportMode.DONT_SUPPORT)
        assertTrue(assertThrows<IllegalRollbackException> { lexer.rollback() })
        lexer.advance()
        val classToken = lexer.current
        lexer.advance()
        val aToken = lexer.current
        lexer.rollback()
        assertEquals(classToken, lexer.current)
        lexer.advance()
        assertEquals(aToken, lexer.current)
        lexer.rollback()
        lexer.rollback()
        assertEquals(SOF, lexer.current)
    }

    private inline fun <reified T> assertThrows(block: () -> Unit): Boolean {
        try {
            block.invoke()
        } catch (t: Throwable) {
            return t is T
        }
        return false
    }

    private fun testOnExample(name: String) {
        val srcText = readFromResources("/examples/$name.lang")
        val expected = readFromResources("/lexer_ethalons/$name.txt").trim('\n')
        val actual = StringBuilder()
        val lexer = LL3Lexer(srcText, DiagnosticMarkupSupportMode.DONT_SUPPORT)
        do {
            actual.append(lexer.current).append("\n")
            lexer.advance()
        } while (lexer.current !is EOF)
        assertEquals(expected, actual.toString().trim('\n'))
    }

}