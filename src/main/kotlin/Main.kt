import ast.dump
import ir.IrBuilder
import parser.Parser
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val input =
        Files.readString(Paths.get("/Users/vladislavgrecko/thelanguage/src/test/resources/diagnostics/illegalReturnType.lang"))
    val ast = Parser(input, "test").parse()
    IrBuilder(listOf(ast)).build()
}
