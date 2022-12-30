package ast

data class AstNodeLocation(val lineStart: Int, val columnStart: Int, val lineEnd: Int, val columnEnd: Int) {
    override fun toString(): String {
        return "($lineStart:$columnStart - $lineEnd:$columnEnd)"
    }
}

val EMPTY_LOCATION = AstNodeLocation(0, 0, 0, -1)