package lexer

import ast.Location

enum class KeySeqType {
    OPEN_PARENTHESIS,
    CLOSING_PARENTHESIS,
    OPEN_CURLY_BRACKET,
    CLOSING_CURLY_BRACKET,
    OPEN_SQUARE_BRACKET,
    CLOSING_SQUARE_BRACKET,
    EXCLAMATION,
    OR,
    AND,
    PLUS,
    MINUS,
    MULTIPLY,
    COMMA,
    DOT,
    SEMICOLON,
    LESS,
    GREATER,
    LEQ,
    GEQ
}

sealed class Token(val location: Location)

class Identifier(val value: String, location: Location) : Token(location)

class IntLiteral(val value: Int, location: Location) : Token(location)

class StringLiteral(val value: String, location: Location) : Token(location)

object SOF : Token(Location(0, 1))

class EOF(location: Location) : Token(location)

class KeySeq(val type: KeySeqType, location: Location) : Token(location)