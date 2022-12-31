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