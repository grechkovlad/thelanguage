package interpreter

import ir.*
import java.io.PrintStream


val TypeReference.defaultValue: Value
    get() = when (this) {
        BoolTypeReference -> False
        FloatTypeReference -> FloatValue(0f)
        IntTypeReference -> IntValue(0)
        else -> Null
    }

class Interpreter(private val out: PrintStream) {

    private val statics = mutableMapOf<FieldReference, Value>()
    private val loadedClasses = mutableSetOf<ClassReference>()

    private fun ensureClassLoaded(classReference: ClassReference) {
        if (classReference !is UserClassReference) return
        loadedClasses.add(classReference)
        ensureClassLoaded(classReference.declaration.superClass)
        classReference.declaration.fields.filter { it.isStatic }.forEach {
            statics[it.reference] = it.type.defaultValue
            if (it.initializer != null) statics[it.reference] = interpretExpression(it.initializer!!, EMPTY_STACK_FRAME)
        }
        interpretStaticInitBlock(classReference.declaration.staticInit)
    }

    private fun interpretStaticInitBlock(staticInit: StaticInitBlock) {
        val stackFrame = StackFrame(null, mutableMapOf())
        interpretStatements(staticInit.body, stackFrame)
    }

    private fun interpretConstructor(constructor: ConstructorReference, arguments: List<Value>): ObjectValue {
        ensureClassLoaded(constructor.declaringClass)
        val obj = ObjectValue(constructor.declaringClass)
        if (constructor is UserConstructorReference) {
            val stackFrame = StackFrame(obj, mutableMapOf())
            val userClass = constructor.declaringClass as UserClassReference
            val superCall = constructor.declaration.body.first() as ExpressionStatement
            require(superCall.expression is SuperCall)
            interpretStatement(superCall, stackFrame)
            userClass.declaration.fields.filter { !it.isStatic }.forEach {
                obj.fields[it.reference] = it.type.defaultValue
                if (it.initializer != null) obj.fields[it.reference] = interpretExpression(it.initializer!!, stackFrame)
            }
            interpretStatements(constructor.declaration.body, stackFrame)
        }
        return obj
    }

    fun interpretMethod(method: MethodReference, receiver: Value?, arguments: List<Value>): Value =
        when (method) {
            is UserMethodReference -> interpretUserMethod(method, receiver, arguments)
            ParseIntMethod -> interpretParseInt(arguments.single())
            PrintIntMethod -> {
                interpretPrintInt(arguments.single())
                Void
            }

            PrintStringMethod -> {
                interpretPrintString(arguments.single())
                Void
            }
        }

    private fun interpretPrintString(argument: Value) {
        require(argument is StringValue)
        out.print(argument.value)
    }

    private fun interpretPrintInt(argument: Value) {
        require(argument is IntValue)
        out.print(argument.value)
    }

    private fun interpretParseInt(argument: Value): IntValue {
        require(argument is StringValue)
        return IntValue(Integer.parseInt(argument.value))
    }

    private fun interpretUserMethod(method: UserMethodReference, receiver: Value?, arguments: List<Value>): Value {
        require(method.parameterTypes.size == arguments.size)
        if (method.isStatic) ensureClassLoaded(method.declaringClass)
        val methodDeclaration = method.declaration
        val argumentsMap = mutableMapOf<VariableDeclaration, Value>().apply {
            methodDeclaration.parameters.zip(arguments).forEach { put(it.first, it.second) }
        }
        val stackFrame = StackFrame(receiver, argumentsMap)
        val returnStatus = interpretStatements(method.declaration.body, stackFrame)
        return if (returnStatus is ReturnValue) returnStatus.value else Void
    }

    private fun interpretStatements(
        statements: List<Statement>,
        stackFrame: StackFrame
    ): Interruption {
        for (statement in statements) {
            val returnStatus = interpretStatement(statement, stackFrame)
            if (returnStatus !is NoInterruption) return returnStatus
        }
        return NoInterruption
    }

    private fun interpretStatement(statement: Statement, stackFrame: StackFrame): Interruption =
        when (statement) {
            is ExpressionStatement -> {
                interpretExpression(statement.expression, stackFrame)
                NoInterruption
            }

            is For -> interpretFor(statement, stackFrame)
            is If -> interpretIf(statement, stackFrame)
            is LocalVariableDeclaration -> {
                interpretLocalVariableDeclaration(statement, stackFrame)
                NoInterruption
            }

            is Return -> interpretReturn(statement, stackFrame)
            is SetArrayElement -> {
                interpretSetArrayElement(statement, stackFrame)
                NoInterruption
            }

            is SetField -> {
                interpretSetField(statement, stackFrame)
                NoInterruption
            }

            is SetVariable -> {
                interpretSetVariable(statement, stackFrame)
                NoInterruption
            }

            is While -> interpretWhile(statement, stackFrame)
            ir.Break -> Break
            ir.Continue -> Continue
        }

    private fun interpretWhile(whileStatement: While, stackFrame: StackFrame): Interruption {
        while (true) {
            val cond = interpretExpression(whileStatement.condition, stackFrame)
            require(cond is BooleanValue)
            if (cond == False) break
            return when (val interruption = interpretStatements(whileStatement.body, stackFrame)) {
                Break -> break
                Continue -> continue
                NoInterruption -> continue
                is ReturnValue -> interruption
                ReturnVoid -> interruption
            }
        }
        return NoInterruption
    }

    private fun interpretSetVariable(setVariable: SetVariable, stackFrame: StackFrame) {
        val value = interpretExpression(setVariable.expression, stackFrame)
        stackFrame.locals[setVariable.variable] = value
    }

    private fun interpretSetField(setField: SetField, stackFrame: StackFrame) {
        val target = interpretExpression(setField.target, stackFrame)
        require(target is ObjectValue)
        val value = interpretExpression(setField.value, stackFrame)
        target.fields[setField.fieldReference] = value
    }

    private fun interpretSetArrayElement(
        setArray: SetArrayElement,
        stackFrame: StackFrame
    ) {
        val array = interpretExpression(setArray.target, stackFrame)
        require(array is ArrayValue)
        val index = interpretExpression(setArray.index, stackFrame)
        require(index is IntValue)
        val value = interpretExpression(setArray.value, stackFrame)
        array.array[index.value] = value
    }

    private fun interpretLocalVariableDeclaration(
        variable: LocalVariableDeclaration,
        stackFrame: StackFrame
    ) {
        val value = if (variable.initializer != null) interpretExpression(
            variable.initializer,
            stackFrame
        ) else variable.type.defaultValue
        stackFrame.locals[variable] = value
    }

    private fun interpretFor(forStatement: For, stackFrame: StackFrame): Interruption {
        interpretStatement(forStatement.init, stackFrame)
        while (true) {
            val cond = interpretExpression(forStatement.condition, stackFrame)
            require(cond is BooleanValue)
            if (cond == False) break
            return when (val interruption = interpretStatements(forStatement.body, stackFrame)) {
                Break -> break
                Continue -> {
                    interpretStatement(forStatement.update, stackFrame)
                    continue
                }

                NoInterruption -> {
                    interpretStatement(forStatement.update, stackFrame)
                    continue
                }

                is ReturnValue -> interruption
                ReturnVoid -> interruption
            }
        }
        return NoInterruption
    }

    private fun interpretReturn(returnStatement: Return, stackFrame: StackFrame): Interruption {
        if (returnStatement.expression == null) return ReturnVoid
        return ReturnValue(interpretExpression(returnStatement.expression, stackFrame))
    }

    private fun interpretIf(ifStatement: If, stackFrame: StackFrame): Interruption {
        val condition = interpretExpression(ifStatement.condition, stackFrame)
        require(condition is BooleanValue)
        return if (condition.value) {
            interpretStatements(ifStatement.thenStatements, stackFrame)
        } else {
            interpretStatements(ifStatement.elseStatements, stackFrame)
        }
    }

    private fun interpretExpression(expression: Expression, stackFrame: StackFrame): Value =
        when (expression) {
            is ArrayLength -> interpretArrayLength(expression, stackFrame)
            is BinaryOperation -> interpretBinaryOperation(expression, stackFrame)
            FalseLiteral -> False
            TrueLiteral -> True
            is Call -> interpretCall(expression, stackFrame)
            is CreateArray -> interpretCreateArray(expression, stackFrame)
            is FloatLiteral -> FloatValue(expression.value)
            is GetArrayElement -> interpretGetArrayElement(expression, stackFrame)
            is GetField -> interpretGetField(expression, stackFrame)
            is GetVariable -> stackFrame.locals[expression.variable]!!
            is IntLiteral -> IntValue(expression.value)
            is Minus -> interpretMinus(expression, stackFrame)
            is Not -> interpretNot(expression, stackFrame)
            ir.Null -> Null
            is StringLiteral -> StringValue(expression.value)
            is ThisAccess -> stackFrame.receiver!!
            is TypeAccess -> error("Should not evaluate type access")
        }

    private fun interpretNot(expression: Not, stackFrame: StackFrame): Value {
        return when (interpretExpression(expression.operand, stackFrame)) {
            False -> True
            True -> False
            else -> error("Boolean operand was expected")
        }
    }

    private fun interpretMinus(expression: Minus, stackFrame: StackFrame): Value {
        return when (val operand = interpretExpression(expression.operand, stackFrame)) {
            is IntValue -> IntValue(-operand.value)
            is FloatValue -> FloatValue(-operand.value)
            else -> error("Numeric operand was expected")
        }
    }

    private fun interpretCall(expression: Call, stackFrame: StackFrame): Value = when (expression) {
        is ConstructorCall -> {
            val arguments = expression.arguments.map { interpretExpression(it, stackFrame) }
            interpretConstructor(expression.constructor, arguments)
        }

        is MethodCall -> {
            val target = if (expression.method.isStatic) null else interpretExpression(expression.target, stackFrame)
            val arguments = expression.arguments.map { interpretExpression(it, stackFrame) }
            interpretMethod(expression.method, target, arguments)
        }

        is SuperCall -> {
            val arguments = expression.arguments.map { interpretExpression(it, stackFrame) }
            interpretConstructor(expression.constructor, arguments)
        }
    }

    private fun interpretBinaryOperation(expression: BinaryOperation, stackFrame: StackFrame): Value {
        val leftOperand = interpretExpression(expression.leftOperand, stackFrame)
        val rightOperand = interpretExpression(expression.rightOperand, stackFrame)
        return when (expression) {
            is Add -> interpretArithmeticOperation(leftOperand, rightOperand, Int::plus, Float::plus)
            is And -> interpretBooleanOperation(leftOperand, rightOperand, Boolean::and)
            is BoolEq -> interpretBooleanOperation(leftOperand, rightOperand) { val1, val2 -> val1 == val2 }
            is Concat -> interpretConcat(leftOperand, rightOperand)
            is Divide -> interpretArithmeticOperation(leftOperand, rightOperand, Int::div, Float::div)
            is Geq -> interpretNumericToBoolOperation(
                leftOperand,
                rightOperand,
                { val1, val2 -> val1 >= val2 },
                { val1, val2 -> val1 >= val2 }
            )

            is Greater -> interpretNumericToBoolOperation(
                leftOperand,
                rightOperand,
                { val1, val2 -> val1 > val2 },
                { val1, val2 -> val1 > val2 }
            )

            is Leq -> interpretNumericToBoolOperation(
                leftOperand,
                rightOperand,
                { val1, val2 -> val1 <= val2 },
                { val1, val2 -> val1 <= val2 }
            )

            is Less -> interpretNumericToBoolOperation(
                leftOperand,
                rightOperand,
                { val1, val2 -> val1 < val2 },
                { val1, val2 -> val1 < val2 }
            )

            is Multiply -> interpretArithmeticOperation(leftOperand, rightOperand, Int::times, Float::times)

            is NumericEq -> interpretNumericToBoolOperation(
                leftOperand,
                rightOperand,
                { val1, val2 -> val1 == val2 },
                { val1, val2 -> val1 == val2 }
            )

            is Or -> interpretBooleanOperation(leftOperand, rightOperand, Boolean::or)
            is RefEq -> (leftOperand === rightOperand).toInterpreterValue()
            is Subtract -> interpretArithmeticOperation(leftOperand, rightOperand, Int::minus, Float::minus)
        }
    }

    private fun interpretConcat(leftOperand: Value, rightOperand: Value): StringValue {
        require(leftOperand is StringValue)
        require(rightOperand is StringValue)
        return StringValue(leftOperand.value + rightOperand.value)
    }

    private inline fun interpretBooleanOperation(
        leftOperand: Value,
        rightOperand: Value,
        booleanOperation: (Boolean, Boolean) -> Boolean
    ): BooleanValue {
        require(leftOperand is BooleanValue)
        require(rightOperand is BooleanValue)
        return (booleanOperation(leftOperand.value, rightOperand.value)).toInterpreterValue()
    }

    private inline fun interpretNumericToBoolOperation(
        leftOperand: Value,
        rightOperand: Value,
        intOperation: (Int, Int) -> Boolean,
        floatOperation: (Float, Float) -> Boolean
    ): BooleanValue {
        if (leftOperand is IntValue) {
            require(rightOperand is IntValue)
            return intOperation(leftOperand.value, rightOperand.value).toInterpreterValue()
        }
        require(leftOperand is FloatValue)
        require(rightOperand is FloatValue)
        return floatOperation(leftOperand.value, rightOperand.value).toInterpreterValue()
    }

    private inline fun interpretArithmeticOperation(
        leftOperand: Value,
        rightOperand: Value,
        intOperation: (Int, Int) -> Int,
        floatOperation: (Float, Float) -> Float
    ): Value {
        if (leftOperand is IntValue) {
            require(rightOperand is IntValue)
            return IntValue(intOperation(leftOperand.value, rightOperand.value))
        }
        require(leftOperand is FloatValue)
        require(rightOperand is FloatValue)
        return FloatValue(floatOperation(leftOperand.value, rightOperand.value))
    }

    private fun interpretCreateArray(createArray: CreateArray, stackFrame: StackFrame): Value {
        val dimensionValues = createArray.dimensions.map { interpretExpression(it, stackFrame) as IntValue }
        var currentArray = ArrayValue(Array(dimensionValues.last().value) {
            when (createArray.type) {
                BoolTypeReference -> False
                FloatTypeReference -> FloatValue(0f)
                IntTypeReference -> IntValue(0)
                else -> Null
            }
        })
        for (index in dimensionValues.size - 2..0) {
            currentArray = ArrayValue(Array(dimensionValues[index].value) { currentArray })
        }
        return currentArray
    }

    private fun interpretGetField(getField: GetField, stackFrame: StackFrame): Value {
        if (getField.isStatic) return statics[getField.fieldReference]!!
        val target = interpretExpression(getField.target, stackFrame)
        require(target is ObjectValue)
        return target.fields[getField.fieldReference]!!
    }

    private fun interpretGetArrayElement(
        expression: GetArrayElement,
        stackFrame: StackFrame
    ): Value {
        val array = interpretExpression(expression.array, stackFrame)
        val index = interpretExpression(expression.index, stackFrame)
        require(array is ArrayValue)
        require(index is IntValue)
        return array.array[index.value]
    }

    private fun interpretArrayLength(expression: ArrayLength, stackFrame: StackFrame): Value {
        val array = interpretExpression(expression.target, stackFrame)
        require(array is ArrayValue)
        return IntValue(array.array.size)
    }

    sealed interface Interruption
    object NoInterruption : Interruption
    object ReturnVoid : Interruption
    class ReturnValue(val value: Value) : Interruption
    object Break : Interruption
    object Continue : Interruption

    class StackFrame(val receiver: Value?, val locals: MutableMap<VariableDeclaration, Value>)
}

private val EMPTY_STACK_FRAME = Interpreter.StackFrame(null, mutableMapOf())

sealed interface Value

class FloatValue(val value: Float) : Value
class IntValue(val value: Int) : Value
class StringValue(val value: String) : Value
class ObjectValue(val clazz: ClassReference) : Value {
    val fields = mutableMapOf<FieldReference, Value>()
}

class ArrayValue(val array: Array<Value>) : Value

object Null : Value
object Void : Value

sealed interface BooleanValue : Value {
    val value: Boolean
}

fun Boolean.toInterpreterValue() = if (this) True else False

object True : BooleanValue {
    override val value: Boolean
        get() = true
}

object False : BooleanValue {
    override val value: Boolean
        get() = false
}