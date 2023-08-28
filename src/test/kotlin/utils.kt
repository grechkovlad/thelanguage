import ir.CompilationError
import lexer.*
import kotlin.reflect.KClass

fun readFromResources(path: String) = object {}.javaClass.enclosingClass.getResource(path)!!.readText()

fun retrieveExpectedDiagnostic(srcText: String, fileName: String): ExpectedCompilationError {
    fun Token.isAtToken() = this is KeySeq && type == KeySeqType.AT
    fun Token.isSharpToken() = this is KeySeq && type == KeySeqType.SHARP
    val lexer = LL3Lexer(srcText, DiagnosticMarkupSupportMode.SUPPORT)
    while (!lexer.current.isAtToken()) lexer.advance()
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
data class ExpectedCompilationError(val type: KClass<out CompilationError>, val location: Location)