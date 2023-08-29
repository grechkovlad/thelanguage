import java.io.File
import java.util.*

private const val PREFIX = "src/test/resources/diagnostics"

fun main() {

    File("src/test/kotlin/DiagnosticTests.kt").printWriter().use { out ->
        out.println("import kotlin.test.Test")
        out.println("import org.junit.jupiter.api.Nested\n")

        out.println("class DiagnosticTests {\n")

        fun tabs(n: Int) = buildString { repeat(n) { append("    ") } }

        fun String.capitalize() = this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        var depth = 0
        File(PREFIX).walk()
            .onEnter {
                if (depth > 0) {
                    out.println("${tabs(depth)}@Nested")
                    out.println("${tabs(depth)}inner class ${it.name.capitalize()}{")
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
                val relative = it.path.substring(PREFIX.length)
                val pathWithoutExtension = relative.substring(0, relative.lastIndexOf('.'))

                out.println("${tabs(depth)}@Test")
                out.println("${tabs(depth)}fun test${nameWithoutExtension.capitalize()}() {")
                out.println("${tabs(depth + 1)}runDiagnosticTest(\"$pathWithoutExtension\")")
                out.println("${tabs(depth)}}")
                out.println()
            }

        out.println("}")
    }
}