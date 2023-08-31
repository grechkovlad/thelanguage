import ir.CompilationError
import ir.IrBuilder
import lexer.*
import parser.Parser
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.test.assertEquals

fun readFromResources(path: String) = object {}.javaClass.enclosingClass.getResource(path)!!.readText()

private fun retrieveExpectedDiagnostic(srcText: String, fileName: String): ExpectedCompilationError {

    fun Token.isAtToken() = this is KeySeq && type == KeySeqType.AT
    fun Token.isSharpToken() = this is KeySeq && type == KeySeqType.SHARP

    val lexer = LL3Lexer(srcText, DiagnosticMarkupSupportMode.SUPPORT)
    while (!lexer.current.isAtToken() && lexer.current !is EOF) lexer.advance()
    if (lexer.current is EOF) throw NoDiagnosticInTest
    lexer.advance()
    val errorIdent = lexer.current
    require(errorIdent is Identifier)
    val type = Class.forName("ir.${errorIdent.value}").kotlin as KClass<CompilationError>
    lexer.advance()
    if (lexer.current.isSharpToken()) throw EmptyDiagnostic
    var location = lexer.current.location.toAstLocation(fileName)
    while (!lexer.current.isSharpToken()) {
        location = location between lexer.current.location.toAstLocation(fileName)
        lexer.advance()
    }
    return ExpectedCompilationError(type, location)
}

data class UnknownDiagnostic(val name: String) : RuntimeException()
object EmptyDiagnostic : RuntimeException()
object NoDiagnosticInTest : RuntimeException()
data class ExpectedCompilationError(val type: KClass<out CompilationError>, val location: Location)

fun runDiagnosticTest(path: String) {
    val srcText = readFromResources("/diagnostics/$path.lang")
    val expectedCompilationError = retrieveExpectedDiagnostic(srcText, path)
    val ast = Parser(srcText, path, true).parse()
    var exceptionThrown = false
    try {
        IrBuilder(listOf(ast)).build()
    } catch (t: CompilationError) {
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