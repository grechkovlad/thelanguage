import ast.dump
import parser.Parser
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val input = Files.readString(Paths.get("/Users/vladislav.grechko/IdeaProjects/thelanguage/src/test/resources/examples/arithmeticExpressions.lang"))
    val ast = Parser(input, "test").parse()
    println(ast.dump())
}
