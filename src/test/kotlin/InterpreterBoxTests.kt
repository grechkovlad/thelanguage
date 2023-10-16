import kotlin.test.Test
import org.junit.jupiter.api.Nested

class InterpreterBoxTests {

    @Nested
    inner class Smoke {
        @Test
        fun testIf() {
            runInterpreterBoxTest("/smoke/if")
        }

        @Test
        fun testInstanceField() {
            runInterpreterBoxTest("/smoke/instanceField")
        }

        @Test
        fun testFor() {
            runInterpreterBoxTest("/smoke/for")
        }

        @Nested
        inner class Initializing {
            @Test
            fun testFieldInitDebug() {
                runInterpreterBoxTest("/smoke/initializing/fieldInitDebug")
            }

            @Test
            fun testSimpleFieldsInit() {
                runInterpreterBoxTest("/smoke/initializing/simpleFieldsInit")
            }

            @Test
            fun testFieldsAndInheritance() {
                runInterpreterBoxTest("/smoke/initializing/fieldsAndInheritance")
            }

            @Test
            fun testFieldAccess() {
                runInterpreterBoxTest("/smoke/initializing/fieldAccess")
            }

            @Test
            fun testFieldInheritance() {
                runInterpreterBoxTest("/smoke/initializing/fieldInheritance")
            }

            @Test
            fun testComplexFieldInit() {
                runInterpreterBoxTest("/smoke/initializing/complexFieldInit")
            }

        }
        @Test
        fun testInvokeInstanceFunc() {
            runInterpreterBoxTest("/smoke/invokeInstanceFunc")
        }

        @Test
        fun testInvokeStaticFun() {
            runInterpreterBoxTest("/smoke/invokeStaticFun")
        }

        @Test
        fun testArray() {
            runInterpreterBoxTest("/smoke/array")
        }

        @Test
        fun testSimplestReturn() {
            runInterpreterBoxTest("/smoke/simplestReturn")
        }

        @Test
        fun testStaticFieldInit() {
            runInterpreterBoxTest("/smoke/staticFieldInit")
        }

        @Nested
        inner class Resolution {
        }
        @Test
        fun testParameterInConstructor() {
            runInterpreterBoxTest("/smoke/parameterInConstructor")
        }

        @Nested
        inner class Arrays {
            @Test
            fun testDefaultArray() {
                runInterpreterBoxTest("/smoke/arrays/defaultArray")
            }

            @Test
            fun testSimpleArrayAndLoops() {
                runInterpreterBoxTest("/smoke/arrays/simpleArrayAndLoops")
            }

            @Test
            fun testArray2d() {
                runInterpreterBoxTest("/smoke/arrays/array2d")
            }

        }
        @Nested
        inner class Expressions {
            @Test
            fun testAddition() {
                runInterpreterBoxTest("/smoke/expressions/addition")
            }

            @Test
            fun testDiscriminant() {
                runInterpreterBoxTest("/smoke/expressions/discriminant")
            }

            @Test
            fun testLazyLogical() {
                runInterpreterBoxTest("/smoke/expressions/lazyLogical")
            }

        }
        @Nested
        inner class Stdlib {
        }
        @Nested
        inner class Instantiating {
        }
        @Test
        fun testStaticInit() {
            runInterpreterBoxTest("/smoke/staticInit")
        }

        @Test
        fun testInterface() {
            runInterpreterBoxTest("/smoke/interface")
        }

        @Test
        fun testWhile() {
            runInterpreterBoxTest("/smoke/while")
        }

        @Nested
        inner class Scopes {
            @Test
            fun testFieldsAndInheritance() {
                runInterpreterBoxTest("/smoke/scopes/fieldsAndInheritance")
            }

            @Test
            fun testHidingLocalVariables() {
                runInterpreterBoxTest("/smoke/scopes/hidingLocalVariables")
            }

        }
        @Test
        fun testAbsTest() {
            runInterpreterBoxTest("/smoke/absTest")
        }

        @Nested
        inner class ControlFlow {
            @Test
            fun testBreaksInWhiles() {
                runInterpreterBoxTest("/smoke/controlFlow/breaksInWhiles")
            }

            @Test
            fun testContinuesInWhiles() {
                runInterpreterBoxTest("/smoke/controlFlow/continuesInWhiles")
            }

            @Test
            fun testBreaksInFors() {
                runInterpreterBoxTest("/smoke/controlFlow/breaksInFors")
            }

            @Test
            fun testManyReturns() {
                runInterpreterBoxTest("/smoke/controlFlow/manyReturns")
            }

            @Test
            fun testBreaksAndContinues() {
                runInterpreterBoxTest("/smoke/controlFlow/breaksAndContinues")
            }

            @Test
            fun testContinueInFors() {
                runInterpreterBoxTest("/smoke/controlFlow/continueInFors")
            }

            @Test
            fun testNestedIfs() {
                runInterpreterBoxTest("/smoke/controlFlow/nestedIfs")
            }

            @Test
            fun testTrickyFors() {
                runInterpreterBoxTest("/smoke/controlFlow/trickyFors")
            }

            @Test
            fun testNestedFor() {
                runInterpreterBoxTest("/smoke/controlFlow/nestedFor")
            }

            @Test
            fun testComplexInterruptions() {
                runInterpreterBoxTest("/smoke/controlFlow/complexInterruptions")
            }

        }
        @Test
        fun testLocalVariable() {
            runInterpreterBoxTest("/smoke/localVariable")
        }

    }
}
