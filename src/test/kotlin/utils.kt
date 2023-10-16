import codegen.CodeGenerator
import interpreter.Interpreter
import interpreter.StringValue
import ir.*
import lexer.*
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import parser.Parser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.*
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

fun runInterpreterBoxTest(path: String) {
    val srcText = readFromResources("/box/$path.lang")
    val project = IrBuilder(listOf(Parser(srcText, path, false).parse())).build()
    val out = ByteArrayOutputStream()
    val result = Interpreter(PrintStream(out)).interpretMethod(project.boxMethod, null, emptyList())
    assert(result is StringValue)
    require(result is StringValue)
    assertEquals("OK", result.value)
}

fun runCodegenBoxTest(path: String) {
    val srcText = readFromResources("/box/$path.lang")
    val classes = CodeGenerator(IrBuilder(listOf(Parser(srcText, path, false).parse())).build()).generate()
    println(classes.stringify())
    val classloader = object : URLClassLoader(emptyArray()) {
        override fun findClass(name: String?): Class<*> {
            require(name != null)
            classes[name]?.let {
                val bytecode = it.toByteArray()
                try {
                    return defineClass(name, bytecode, 0, bytecode.size)
                } catch (t: Throwable) {
                    println(bytecode.stringify())
                    throw t
                }
            }
            return super.findClass(name)
        }
    }
    val mainClass = classloader.loadClass("Main")
    try {
        val boxMethod = mainClass.getMethod("box")
        require(Modifier.isStatic(boxMethod.modifiers))
        assertEquals("OK", boxMethod.invoke(null))
    } catch (t: Throwable) {
        println(classes.stringify())
        throw t
    }
}

private fun Map<String, ClassWriter>.stringify() = values.joinToString("\n") { it.toByteArray().stringify() }

private fun ByteArray.stringify(): String {
    val out = ByteArrayOutputStream()
    val printWriter = PrintWriter(out)
    val traceClassVisitor = TraceClassVisitor(printWriter)
    val classReader = ClassReader(this)
    classReader.accept(traceClassVisitor, SKIP_FRAMES)
    return out.toString()
}

private val Project.boxMethod: MethodReference
    get() = classes.single { it.name == "Main" }.declaredMethods.single { it.name == "box" && it.reference.isStatic }.reference

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

fun generateTestsSet(directory: String, name: String, method: String) {

    File("src/test/kotlin/$name.kt").printWriter().use { out ->
        out.println("import kotlin.test.Test")
        out.println("import org.junit.jupiter.api.Nested\n")

        out.println("class $name {\n")

        fun tabs(n: Int) = buildString { repeat(n) { append("    ") } }

        fun String.capitalize() = this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        var depth = 0
        File(directory).walk()
            .onEnter {
                if (depth > 0) {
                    out.println("${tabs(depth)}@Nested")
                    out.println("${tabs(depth)}inner class ${it.name.capitalize()} {")
                }
                depth++
                true
            }
            .onLeave {
                depth--
                if (depth > 0) out.println("${tabs(depth)}}")
            }
            .forEach {
                if (!it.isFile) return@forEach
                val nameWithoutExtension = it.name.substring(0, it.name.lastIndexOf('.'))
                val relative = it.path.substring(directory.length)
                val pathWithoutExtension = relative.substring(0, relative.lastIndexOf('.'))

                out.println("${tabs(depth)}@Test")
                out.println("${tabs(depth)}fun test${nameWithoutExtension.capitalize()}() {")
                out.println("${tabs(depth + 1)}$method(\"$pathWithoutExtension\")")
                out.println("${tabs(depth)}}")
                out.println()
            }

        out.println("}")
    }
}