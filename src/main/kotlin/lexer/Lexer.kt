package lexer

import java.io.EOFException

import lexer.KeySeqType.*

interface Lexer {
    val current: Token
    fun advance()
    fun rollback()
}

class IllegalRollbackException : RuntimeException()

val WHITESPACES = arrayOf(' ', '\n', '\t')

enum class DiagnosticMarkupSupportMode {
    SUPPORT, IGNORE, DONT_SUPPORT
}

class LL3Lexer(
    private val input: CharSequence,
    private val diagnosticMarkupSupportMode: DiagnosticMarkupSupportMode
) : Lexer {

    private var line = 1
    private var column = 0
    private var pos = -1
    private var currentIndex = 0
    private val currents = arrayOf<Token?>(SOF, null, null)

    override val current: Token
        get() = currents[currentIndex]!!

    override fun advance() {
        if (currentIndex < currents.size - 1 && currents[currentIndex + 1] != null) {
            currentIndex++
            return
        }
        if (current is EOF) throw EOFException()
        pos++
        column++
        var nextToken = readNextToken()
        if (diagnosticMarkupSupportMode == DiagnosticMarkupSupportMode.IGNORE) {
            if (nextToken is KeySeq && nextToken.type == AT) {
                advance()
                advance()
                return
            } else if (nextToken is KeySeq && nextToken.type == SHARP) {
                advance()
                return
            }
        }
        if (currentIndex < currents.size - 1) {
            currents[++currentIndex] = nextToken
        } else {
            currents[0] = currents[1]
            currents[1] = currents[2]
            currents[2] = nextToken
        }
    }

    private val singleSymbolTokensMap = mapOf(
        '(' to OPEN_PARENTHESIS,
        ')' to CLOSING_PARENTHESIS,
        '{' to OPEN_CURLY_BRACKET,
        '}' to CLOSING_CURLY_BRACKET,
        '|' to OR,
        '&' to AND,
        '+' to PLUS,
        '-' to MINUS,
        '*' to MULTIPLY,
        ',' to COMMA,
        ';' to SEMICOLON,
        '[' to OPEN_SQUARE_BRACKET,
        ']' to CLOSING_SQUARE_BRACKET,
        '.' to DOT,
        ':' to COLON,
        '/' to DIV
    )

    private val singleSymbolOrderMap = mapOf('<' to LESS, '>' to GREATER, '=' to ASSIGN, '!' to EXCLAMATION)
    private val singleSymbolOrderChars = singleSymbolOrderMap.keys
    private val twoSymbolOrderMap = mapOf("<=" to LEQ, ">=" to GEQ, "==" to EQUALS, "!=" to NOT_EQUALS)
    private val boolRegex = "true|false".toRegex()
    private val wordRegex = "[a-zA-Z][_a-zA-Z0-9]*".toRegex()
    private val floatRegex = "\\d+\\.\\d+".toRegex()
    private val intRegex = "\\d+".toRegex()
    private val stringRegex = "\"[^\"]*\"".toRegex()

    private val keywordsMap = listOf(
        IF,
        ELSE,
        ABSTRACT,
        PRIVATE,
        PROTECTED,
        PUBLIC,
        VOID,
        WHILE,
        FOR,
        CLASS,
        INTERFACE,
        RETURN,
        STATIC,
        NEW,
        FUN,
        FIELD,
        VAR,
        CONSTRUCTOR,
        THIS,
        NULL,
        SUPER
    ).associateBy { it.stringValue }

    private fun readNextToken(): Token {
        eatWhitespaces()
        if (pos == input.length) return EOF(TokenLocation(line, column, column))
        singleSymbolTokensMap[input[pos]]?.also { return KeySeq(it, TokenLocation(line, column, column)) }
        if (diagnosticMarkupSupportMode != DiagnosticMarkupSupportMode.DONT_SUPPORT && input[pos] == '@') {
            return KeySeq(AT, TokenLocation(line, column, column))
        }
        if (diagnosticMarkupSupportMode != DiagnosticMarkupSupportMode.DONT_SUPPORT && input[pos] == '#') {
            return KeySeq(SHARP, TokenLocation(line, column, column))
        }
        if (input[pos] in singleSymbolOrderChars) {
            if (!charAtIs(pos + 1, '=')) {
                return KeySeq(singleSymbolOrderMap[input[pos]]!!, TokenLocation(line, column, column))
            }
            val token =
                KeySeq(twoSymbolOrderMap[input.substring(pos, pos + 2)]!!, TokenLocation(line, column, column + 1))
            pos++
            column++
            return token
        }
        boolRegex.matchAt(input, pos)?.also {
            val skipSymbolsCount = it.value.length - 1
            val token = BoolLiteral(it.value.toBoolean(), TokenLocation(line, column, column + skipSymbolsCount))
            column += skipSymbolsCount
            pos += skipSymbolsCount
            return token
        }
        wordRegex.matchAt(input, pos)?.also { match ->
            val skipSymbolsCount = match.value.length - 1
            val token = keywordsMap[match.value]?.let {
                KeySeq(it, TokenLocation(line, column, column + skipSymbolsCount))
            } ?: Identifier(match.value, TokenLocation(line, column, column + skipSymbolsCount))
            column += skipSymbolsCount
            pos += skipSymbolsCount
            return token
        }
        floatRegex.matchAt(input, pos)?.also {
            val skipSymbolsCount = it.value.length - 1
            val token = FloatLiteral(it.value.toFloat(), TokenLocation(line, column, column + skipSymbolsCount))
            column += skipSymbolsCount
            pos += skipSymbolsCount
            return token
        }
        intRegex.matchAt(input, pos)?.also {
            val skipSymbolsCount = it.value.length - 1
            val token = IntLiteral(it.value.toInt(), TokenLocation(line, column, column + skipSymbolsCount))
            column += skipSymbolsCount
            pos += skipSymbolsCount
            return token
        }
        stringRegex.matchAt(input, pos)?.also {
            val skipSymbolsCount = it.value.length - 1
            val token = StringLiteral(
                it.value.substring(1, it.value.length - 1), TokenLocation(line, column, column + skipSymbolsCount)
            )
            column += skipSymbolsCount
            pos += skipSymbolsCount
            return token
        }
        throw UnexpectedCharException(input[pos], line, column)
    }

    private fun charAtIs(pos: Int, expected: Char) = pos < input.length && input[pos] == expected

    private fun eatWhitespaces() {
        while (pos < input.length && input[pos] in WHITESPACES) {
            when (input[pos]) {
                ' ' -> column += 1
                '\n' -> {
                    line += 1
                    column = 1
                }

                '\t' -> column += 4
            }
            pos += 1
        }
    }

    override fun rollback() {
        if (currentIndex == 0) throw IllegalRollbackException()
        currentIndex--
    }
}

data class UnexpectedCharException(val char: Char, val line: Int, val column: Int) : RuntimeException()