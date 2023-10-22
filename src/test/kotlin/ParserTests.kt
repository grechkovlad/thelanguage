import ast.dump
import org.junit.jupiter.api.Test
import parser.Parser
import kotlin.test.assertEquals

class ParserTests {

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

    private fun testOnExample(name: String) {
        val srcText = readFromResources("/examples/$name.lang")
        val expected = readFromResources("/parser_ethalons/$name.txt").trim('\n')
        assertEquals(expected, Parser(srcText, "test").parse().dump().trim('\n'))
    }
}