package parser

import ClassKind
import EMPTY_LOCATION
import Location
import ast.*
import ast.Identifier
import lexer.*
import lexer.IntLiteral
import lexer.KeySeqType.*

class Parser(input: CharSequence, private val fileName: String) {

    private fun TokenLocation.toAstLocation() = Location(fileName, line, columnStart, line, columnEnd)

    private infix fun Location.between(to: Location) =
        Location(fileName, lineStart, columnStart, to.lineEnd, to.columnEnd)

    private val lexer: LL3Lexer = LL3Lexer(input)

    private fun eatKeySeqToken(vararg types: KeySeqType) =
        eatToken(types.joinToString { "'${it.stringValue}'" }) { it is KeySeq && it.type in types } as KeySeq


    private fun eatSOFToken() = eatToken("start of file") { it is SOF }

    private fun eatIdentifierToken() = eatToken("identifier") { it is lexer.Identifier } as lexer.Identifier

    private fun eatIntLiteral() = eatToken("int literal") { it is IntLiteral } as IntLiteral

    private fun eatStringLiteral() = eatToken("string literal") { it is lexer.StringLiteral } as lexer.StringLiteral

    private fun eatFloatLiteral() = eatToken("float literal") { it is lexer.FloatLiteral } as lexer.FloatLiteral

    private fun atKeySeq(vararg types: KeySeqType): Boolean {
        val current = lexer.current
        return current is KeySeq && current.type in types
    }

    private fun atIntLiteral() = lexer.current is IntLiteral

    private fun atStringLiteral() = lexer.current is lexer.StringLiteral

    private fun atFloatLiteral() = lexer.current is lexer.FloatLiteral

    private fun atIdentifier() = lexer.current is lexer.Identifier

    private fun atModifier(): Boolean = atKeySeq(STATIC, PRIVATE, PROTECTED, PUBLIC, ABSTRACT)

    private fun currentTokenToModifier(): Modifier {
        val token = lexer.current as KeySeq
        val location = token.location.toAstLocation()
        return when (token.type) {
            STATIC -> Modifier(ModifierType.STATIC, location)
            PUBLIC -> Modifier(ModifierType.PUBLIC, location)
            PROTECTED -> Modifier(ModifierType.PROTECTED, location)
            PRIVATE -> Modifier(ModifierType.PRIVATE, location)
            ABSTRACT -> Modifier(ModifierType.ABSTRACT, location)
            else -> throw ParsingException(token, "modifier")
        }
    }

    private fun eatToken(expected: String, predicate: (Token) -> Boolean): Token {
        val token = lexer.current
        if (predicate(token)) {
            lexer.advance()
            return token
        }
        throw ParsingException(token, expected)
    }

    fun parse(): SourceFile {
        val startLocation = eatSOFToken().location.toAstLocation()
        var endLocation = startLocation
        val classes = mutableListOf<ClassDeclaration>()
        while (lexer.current !is EOF) {
            val classDecl = parseClass()
            classes.add(classDecl)
            endLocation = classDecl.location
        }
        return SourceFile(classes, startLocation between endLocation)
    }

    private fun parseClass(): ClassDeclaration {
        val classOrInterfaceToken = eatKeySeqToken(CLASS, INTERFACE)
        val modifiers = parseModifiers()
        val name = parseIdentifier()
        val superClasses = parseSuperClasses()
        eatKeySeqToken(OPEN_CURLY_BRACKET)
        val members = mutableListOf<MemberDeclaration>()
        while (!atKeySeq(CLOSING_CURLY_BRACKET)) {
            val member = parseMember()
            members.add(member)
        }
        val endLocation = eatKeySeqToken(CLOSING_CURLY_BRACKET).location.toAstLocation()
        return ClassDeclaration(
            name,
            modifiers,
            if (classOrInterfaceToken.type == CLASS) ClassKind.CLASS else ClassKind.INTERFACE,
            superClasses,
            members.toList(),
            classOrInterfaceToken.location.toAstLocation() between endLocation
        )
    }

    private fun parseSuperClasses(): List<Identifier> {
        if (!atKeySeq(COLON)) return emptyList()
        eatKeySeqToken(COLON)
        val res = mutableListOf<Identifier>()
        res.add(parseIdentifier())
        while (!atKeySeq(OPEN_CURLY_BRACKET)) {
            eatKeySeqToken(COMMA)
            res.add(parseIdentifier())
        }
        return res
    }

    private fun parseIdentifier(): Identifier {
        val token = eatIdentifierToken()
        return Identifier(token.value, token.location.toAstLocation())
    }

    private fun parseIdentifierOrThis(): Expression {
        return when {
            atIdentifier() -> {
                parseIdentifier()
            }

            atKeySeq(THIS) -> {
                val res = This(lexer.current.location.toAstLocation())
                eatKeySeqToken(THIS)
                res
            }

            else -> throw ParsingException(lexer.current, "'this' or identifier")
        }
    }

    private fun parseMember(): MemberDeclaration {
        if (atKeySeq(FIELD)) {
            return parseFieldDeclaration()
        }
        if (atKeySeq(FUN)) {
            return parseFunDeclaration()
        }
        if (atKeySeq(CONSTRUCTOR)) {
            return parseConstructorDeclaration()
        }
        if (atKeySeq(STATIC)) {
            return parseStaticInitBlock()
        }
        throw ParsingException(lexer.current, "field or fun declaration")
    }

    private fun parseStaticInitBlock(): MemberDeclaration {
        val startLocation = eatKeySeqToken(STATIC).location.toAstLocation()
        val body = parseBlock()
        return StaticInitBlock(body, startLocation between body.location)
    }

    private fun parseConstructorDeclaration(): MemberDeclaration {
        val startLocation = eatKeySeqToken(CONSTRUCTOR).location.toAstLocation()
        val modifiers = parseModifiers()
        val parameters = parseParametersList()
        val body = parseBlock()
        return ConstructorDeclaration(modifiers, parameters, body, startLocation between body.location)
    }

    private fun parseModifiers(): ModifiersList {
        if (!atModifier()) return ModifiersList(emptyList(), EMPTY_LOCATION)
        val startLocation = lexer.current.location.toAstLocation()
        var endLocation = startLocation
        val modifiers = mutableListOf<Modifier>()
        while (atModifier()) {
            val modifier = currentTokenToModifier()
            endLocation = modifier.location
            modifiers.add(modifier)
            lexer.advance()
        }
        return ModifiersList(modifiers, startLocation between endLocation)
    }

    private fun parseBlock(): Block {
        val startLocation = eatKeySeqToken(OPEN_CURLY_BRACKET).location.toAstLocation()
        val statements = mutableListOf<Statement>()
        while (!atKeySeq(CLOSING_CURLY_BRACKET)) {
            statements.add(parseStatement())
        }
        val endLocation = eatKeySeqToken(CLOSING_CURLY_BRACKET).location.toAstLocation()
        return Block(statements, startLocation between endLocation)
    }

    private fun parseParametersList(): ParametersList {
        val parameters = mutableListOf<Parameter>()
        val startLocation = eatKeySeqToken(OPEN_PARENTHESIS).location.toAstLocation()
        if (atKeySeq(CLOSING_PARENTHESIS)) {
            val endLocation = eatKeySeqToken(CLOSING_PARENTHESIS).location.toAstLocation()
            return ParametersList(parameters, startLocation between endLocation)
        }
        parameters.add(parseParameter())
        while (!atKeySeq(CLOSING_PARENTHESIS)) {
            eatKeySeqToken(COMMA)
            parameters.add(parseParameter())
        }
        val endLocation = eatKeySeqToken(CLOSING_PARENTHESIS).location.toAstLocation()
        return ParametersList(parameters, startLocation between endLocation)
    }

    private fun parseFunDeclaration(): MethodDeclaration {
        val startLocation = eatKeySeqToken(FUN).location.toAstLocation()
        val modifiers = parseModifiers()
        val returnType = parseReturnType()
        val name = parseIdentifier()
        val parameters = parseParametersList()
        var body: Block? = null
        val endLocation: Location
        if (!atKeySeq(SEMICOLON)) {
            body = parseBlock()
            endLocation = body.location
        } else {
            endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
        }
        return MethodDeclaration(
            name, returnType, modifiers, parameters, body, startLocation between endLocation
        )
    }

    private fun parseTypeReference(): TypeReference {
        var res: TypeReference = SimpleTypeReference(parseIdentifier())
        while (atKeySeq(OPEN_SQUARE_BRACKET)) {
            eatKeySeqToken(OPEN_SQUARE_BRACKET)
            val closingBracketLocation = eatKeySeqToken(CLOSING_SQUARE_BRACKET).location.toAstLocation()
            res = ArrayTypeReference(res, res.location between closingBracketLocation)
        }
        return res
    }

    private fun parseReturnType(): TypeReference? {
        if (atKeySeq(VOID)) {
            lexer.advance()
            return null
        }
        return parseTypeReference()
    }

    private fun parseStatement(): Statement {
        if (atKeySeq(VAR)) return parseVariableDeclaration()
        if (atKeySeq(IF)) return parseIfStatement()
        if (atKeySeq(WHILE)) return parseWhileStatement()
        if (atKeySeq(FOR)) return parseForStatement()
        if (atKeySeq(RETURN)) return parseReturnStatement()
        if (atKeySeq(SUPER)) return parseSuperCall()
        if (atIdentifier() || atKeySeq(THIS, OPEN_PARENTHESIS, NEW)) {
            val expr = parseExpression()
            return when {
                atKeySeq(SEMICOLON) -> {
                    val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
                    ExpressionStatement(expr, expr.location between endLocation)
                }

                atKeySeq(ASSIGN) -> {
                    eatKeySeqToken(ASSIGN)
                    val rValue = parseExpression()
                    val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
                    Assignment(expr, rValue, expr.location between endLocation)
                }

                else -> throw ParsingException(lexer.current, "'=' or ';'")
            }
        }
        throw ParsingException(lexer.current, "statement")
    }

    private fun parseReturnStatement(): Statement {
        val startLocation = eatKeySeqToken(RETURN).location.toAstLocation()
        if (atKeySeq(SEMICOLON)) {
            val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
            return ReturnStatement(null, startLocation between endLocation)
        }
        val value = parseExpression()
        val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
        return ReturnStatement(value, startLocation between endLocation)
    }

    private fun parseForStatement(): Statement {
        val startLocation = eatKeySeqToken(FOR).location.toAstLocation()
        eatKeySeqToken(OPEN_PARENTHESIS)
        val initStatement = parseStatement()
        val condition = parseExpression()
        eatKeySeqToken(SEMICOLON)
        val updateStatement = parseStatement()
        eatKeySeqToken(CLOSING_PARENTHESIS)
        val body = parseBlock()
        return ForStatement(initStatement, condition, updateStatement, body, startLocation between body.location)
    }

    private fun parseWhileStatement(): Statement {
        val startLocation = eatKeySeqToken(WHILE).location.toAstLocation()
        eatKeySeqToken(OPEN_PARENTHESIS)
        val condition = parseExpression()
        eatKeySeqToken(CLOSING_PARENTHESIS)
        val body = parseBlock()
        return WhileStatement(condition, body, startLocation between body.location)
    }

    private fun parseIfStatement(): Statement {
        val startLocation = eatKeySeqToken(IF).location.toAstLocation()
        eatKeySeqToken(OPEN_PARENTHESIS)
        val condition = parseExpression()
        eatKeySeqToken(CLOSING_PARENTHESIS)
        val thenBlock = parseBlock()
        if (!atKeySeq(ELSE)) {
            return IfStatement(condition, thenBlock, null, startLocation between thenBlock.location)
        }
        eatKeySeqToken(ELSE)
        val elseBlock = parseBlock()
        return IfStatement(condition, thenBlock, elseBlock, startLocation between elseBlock.location)
    }

    private fun parseVariableDeclaration(): Statement {
        val startLocation = eatKeySeqToken(VAR).location.toAstLocation()
        val type = parseTypeReference()
        val name = parseIdentifier()
        if (atKeySeq(SEMICOLON)) {
            val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
            return VariableDeclaration(name, type, initializer = null, startLocation between endLocation)
        }
        eatKeySeqToken(ASSIGN)
        val initializer = parseExpression()
        val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
        return VariableDeclaration(name, type, initializer, startLocation between endLocation)
    }

    private fun parseParameter(): Parameter {
        val typeReference = parseTypeReference()
        val name = parseIdentifier()
        return Parameter(typeReference, name, typeReference.location between name.location)
    }

    private fun parseFieldDeclaration(): FieldDeclaration {
        val startLocation = eatKeySeqToken(FIELD).location.toAstLocation()
        val modifiers = parseModifiers()
        val type = parseTypeReference()
        val name = parseIdentifier()
        if (atKeySeq(SEMICOLON)) {
            val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
            return FieldDeclaration(name, type, initializer = null, modifiers, startLocation between endLocation)
        }
        eatKeySeqToken(ASSIGN)
        val initExpr = parseExpression()
        val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
        return FieldDeclaration(name, type, initExpr, modifiers, startLocation between endLocation)
    }

    private fun parseBinaryOperation(
        operationTypes: Map<KeySeqType, BinaryOperationKind>, childParsingFun: () -> Expression
    ): Expression {
        var res = childParsingFun()
        while (atKeySeq(*operationTypes.keys.toTypedArray())) {
            val operatorToken = lexer.current as KeySeq
            lexer.advance()
            val rightOperand = childParsingFun()
            res = BinaryOperation(
                operationTypes[operatorToken.type]!!, res, rightOperand, res.location between rightOperand.location
            )
        }
        return res
    }

    private fun parseTargetedExpression(): Expression {
        var res = parseAtom()
        while (atKeySeq(DOT, OPEN_SQUARE_BRACKET)) {
            res = if (atKeySeq(DOT)) {
                eatKeySeqToken(DOT)
                val rightOfDot = parseIdentifier()
                if (atKeySeq(OPEN_PARENTHESIS)) {
                    val arguments = parseArgumentsList()
                    MethodCall(res, rightOfDot, arguments, res.location between arguments.location)
                } else {
                    FieldAccess(res, rightOfDot, res.location between rightOfDot.location)
                }
            } else {
                eatKeySeqToken(OPEN_SQUARE_BRACKET)
                val index = parseExpression()
                val endLocation = eatKeySeqToken(CLOSING_SQUARE_BRACKET).location.toAstLocation()
                ArrayAccess(res, index, res.location between endLocation)
            }
        }
        return res
    }

    private fun parseExpression() = parseBinaryOperation(mapOf(OR to BinaryOperationKind.OR), ::parseDisjunct)

    private fun parseDisjunct() = parseBinaryOperation(mapOf(AND to BinaryOperationKind.AND), ::parseConjunct)

    private fun parseConjunct() = parseBinaryOperation(
        mapOf(
            LESS to BinaryOperationKind.LESS,
            LEQ to BinaryOperationKind.LEQ,
            GREATER to BinaryOperationKind.GREATER,
            GEQ to BinaryOperationKind.GEQ,
            EQUALS to BinaryOperationKind.EQ,
            NOT_EQUALS to BinaryOperationKind.NOT_EQ,
        ), ::parseComparison
    )

    private fun parseComparison() = parseBinaryOperation(
        mapOf(PLUS to BinaryOperationKind.PLUS, MINUS to BinaryOperationKind.MINUS), ::parseSummand
    )

    private fun parseSummand() = parseBinaryOperation(
        mapOf(
            MULTIPLY to BinaryOperationKind.MULT, DIV to BinaryOperationKind.DIV
        ), ::parseMultiplier
    )

    private fun parseMultiplier(): Expression {
        if (!atKeySeq(EXCLAMATION, MINUS)) return parseTargetedExpression()
        val operatorToken = eatKeySeqToken(EXCLAMATION, MINUS)
        val operatorKind = if (operatorToken.type == EXCLAMATION) UnaryOperationKind.NOT else UnaryOperationKind.MINUS
        val operand = parseTargetedExpression()
        return UnaryOperation(operatorKind, operand, operatorToken.location.toAstLocation() between operand.location)
    }

    private fun parseAtom(): Expression {
        if (atKeySeq(OPEN_PARENTHESIS)) {
            eatKeySeqToken(OPEN_PARENTHESIS).location.toAstLocation()
            val res = parseExpression()
            eatKeySeqToken(CLOSING_PARENTHESIS).location.toAstLocation()
            return res
        }
        if (atIntLiteral()) return parseIntLiteral()
        if (atStringLiteral()) return parseStringLiteral()
        if (atFloatLiteral()) return parseFloatLiteral()
        if (atKeySeq(NULL)) return Null(eatKeySeqToken(NULL).location.toAstLocation())
        if (atKeySeq(NEW)) return parseNewOperator()
        if (atIdentifier() || atKeySeq(THIS)) {
            return parseIdentifierOrThis()
        }
        throw ParsingException(lexer.current, "'(', 'this', a literal, or an identifier")
    }

    private fun parseArrayCreation(): Expression {
        val startLocation = eatKeySeqToken(NEW).location.toAstLocation()
        var endLocation: Location
        val type = parseIdentifier()
        val dimensions = mutableListOf<Expression>()
        do {
            eatKeySeqToken(OPEN_SQUARE_BRACKET)
            dimensions.add(parseExpression())
            endLocation = eatKeySeqToken(CLOSING_SQUARE_BRACKET).location.toAstLocation()
        } while (atKeySeq(OPEN_SQUARE_BRACKET))
        return ArrayCreation(SimpleTypeReference(type), dimensions, startLocation between endLocation)
    }

    private fun parseSuperCall(): Statement {
        val startLocation = eatKeySeqToken(SUPER).location.toAstLocation()
        val argumentsList = parseArgumentsList()
        val endLocation = eatKeySeqToken(SEMICOLON).location.toAstLocation()
        return SuperCall(argumentsList, startLocation between argumentsList.location)
    }

    private fun parseConstructorCall(): Expression {
        val startLocation = eatKeySeqToken(NEW).location.toAstLocation()
        val type = parseIdentifier()
        val argumentsList = parseArgumentsList()
        return ConstructorCall(SimpleTypeReference(type), argumentsList, startLocation between argumentsList.location)
    }

    private fun parseArgumentsList(): ArgumentsList {
        val startLocation = eatKeySeqToken(OPEN_PARENTHESIS).location.toAstLocation()
        val arguments = mutableListOf<Expression>()
        if (atKeySeq(CLOSING_PARENTHESIS)) {
            val endLocation = eatKeySeqToken(CLOSING_PARENTHESIS).location.toAstLocation()
            return ArgumentsList(arguments, startLocation between endLocation)
        }
        arguments.add(parseExpression())
        while (!atKeySeq(CLOSING_PARENTHESIS)) {
            eatKeySeqToken(COMMA)
            arguments.add(parseExpression())
        }
        val endLocation = eatKeySeqToken(CLOSING_PARENTHESIS).location.toAstLocation()
        return ArgumentsList(arguments, startLocation between endLocation)
    }

    private fun parseNewOperator(): Expression {
        eatKeySeqToken(NEW)
        eatIdentifierToken()
        return when {
            atKeySeq(OPEN_PARENTHESIS) -> {
                lexer.rollback()
                lexer.rollback()
                return parseConstructorCall()
            }

            atKeySeq(OPEN_SQUARE_BRACKET) -> {
                lexer.rollback()
                lexer.rollback()
                return parseArrayCreation()
            }

            else -> throw ParsingException(lexer.current, "'(' or '['")
        }
    }

    private fun parseFloatLiteral(): Expression {
        val token = eatFloatLiteral()
        return ast.FloatLiteral(token.value, token.location.toAstLocation())
    }

    private fun parseStringLiteral(): Expression {
        val token = eatStringLiteral()
        return ast.StringLiteral(token.value, token.location.toAstLocation())
    }

    private fun parseIntLiteral(): Expression {
        val token = eatIntLiteral()
        return ast.IntLiteral(token.value, token.location.toAstLocation())
    }
}

data class ParsingException(val badToken: Token, val expected: String) : RuntimeException()