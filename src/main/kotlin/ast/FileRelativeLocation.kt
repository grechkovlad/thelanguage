package ast

data class FileRelativeLocation(val lineStart: Int, val columnStart: Int, val lineEnd: Int, val columnEnd: Int) {
    override fun toString(): String {
        return "($lineStart:$columnStart - $lineEnd:$columnEnd)"
    }
}

val EMPTY_LOCATION = FileRelativeLocation(0, 0, 0, -1)