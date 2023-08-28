import lexer.TokenLocation

data class Location(
    val fileName: String,
    val lineStart: Int,
    val columnStart: Int,
    val lineEnd: Int,
    val columnEnd: Int
) {

    fun dumpWithoutFilename() = "($lineStart:$columnStart - $lineEnd:$columnEnd)"
}

val EMPTY_LOCATION = Location("file", 0, 0, 0, -1)
infix fun Location.between(to: Location) =
    Location(fileName, lineStart, columnStart, to.lineEnd, to.columnEnd)

fun TokenLocation.toAstLocation(fileName: String) = Location(fileName, line, columnStart, line, columnEnd)