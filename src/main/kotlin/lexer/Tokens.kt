package lexer

data class TokenLocation(val line: Int, val columnStart: Int, val columnEnd: Int)

enum class KeySeqType(val stringValue: String) {
    OPEN_PARENTHESIS ("("),
    CLOSING_PARENTHESIS(")"),
    OPEN_CURLY_BRACKET("{"),
    CLOSING_CURLY_BRACKET("}"),
    OPEN_SQUARE_BRACKET("["),
    CLOSING_SQUARE_BRACKET("]"),
    EXCLAMATION("!"),
    OR("|"),
    AND("&"),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIV("/"),
    COMMA(","),
    DOT("."),
    COLON(":"),
    SEMICOLON(";"),
    LESS("<"),
    GREATER(">"),
    LEQ("<="),
    GEQ(">="),
    EQUALS("=="),
    NOT_EQUALS("!="),
    ASSIGN("="),
    NEW("new"),
    IF("if"),
    ELSE("else"),
    ABSTRACT("abstract"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    VOID("void"),
    WHILE("while"),
    FOR("for"),
    CLASS("class"),
    INTERFACE("interface"),
    RETURN("return"),
    STATIC("static"),
    FUN("fun"),
    FIELD("field"),
    VAR("var"),
    CONSTRUCTOR("constructor"),
    THIS("this"),
    NULL("null"),
    SUPER("super")
}

sealed class Token {
    abstract val location: TokenLocation
}

data class Identifier(val value: String, override val location: TokenLocation) : Token()

data class IntLiteral(val value: Int, override val location: TokenLocation) : Token()

data class FloatLiteral(val value: Float, override val location: TokenLocation) : Token()

data class StringLiteral(val value: String, override val location: TokenLocation) : Token()

object SOF : Token() {
    override val location = TokenLocation(1, 0, 0)

    override fun toString() = "SOF"
}

class EOF(override val location: TokenLocation) : Token()

data class KeySeq(val type: KeySeqType, override val location: TokenLocation) : Token()