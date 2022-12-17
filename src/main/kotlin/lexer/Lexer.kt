package lexer

import ast.Location
import java.io.EOFException

interface Lexer {
    val current: Token
    fun advance()
    fun rollback()
}

class IllegalRollbackException : RuntimeException()

val WHITESPACES = arrayOf(' ', '\n', '\t')

class LL2Lexer(private val input: CharSequence) : Lexer {

    private var line = 1
    private var column = 0
    private var pos = -1
    private var currentIndex = 0
    private val currents = arrayOf<Token>(SOF)

    override val current: Token = currents[currentIndex]

    override fun advance() {
        if (currentIndex == 0 && currents.size > 1) {
            currentIndex++
            return
        }
        if (current is EOF) throw EOFException()
        pos++
        column++
        val nextToken = readNextToken()
        if (currentIndex == 0) {
            currents[1] = nextToken
            currentIndex
        } else {
            currents[0] = currents[1]
            currents[1] = nextToken
        }
    }

    private fun readNextToken(): Token {
        eatWhitespaces()
        if (pos == input.length) return EOF(Location(line, column))
        TODO()
    }

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