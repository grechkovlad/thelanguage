import kotlin.test.Test
import org.junit.jupiter.api.Nested

class BoxTests {

    @Nested
    inner class Smoke {
        @Test
        fun testIf() {
            runBoxTest("/smoke/if")
        }

        @Test
        fun testInstanceField() {
            runBoxTest("/smoke/instanceField")
        }

        @Test
        fun testFor() {
            runBoxTest("/smoke/for")
        }

        @Nested
        inner class Initializing {
        }
        @Test
        fun testInvokeInstanceFunc() {
            runBoxTest("/smoke/invokeInstanceFunc")
        }

        @Test
        fun testInvokeStaticFun() {
            runBoxTest("/smoke/invokeStaticFun")
        }

        @Test
        fun testArray() {
            runBoxTest("/smoke/array")
        }

        @Test
        fun testSimplestReturn() {
            runBoxTest("/smoke/simplestReturn")
        }

        @Test
        fun testStaticFieldInit() {
            runBoxTest("/smoke/staticFieldInit")
        }

        @Nested
        inner class Resolution {
        }
        @Test
        fun testParameterInConstructor() {
            runBoxTest("/smoke/parameterInConstructor")
        }

        @Nested
        inner class Expressions {
            @Test
            fun testLazyLogical() {
                runBoxTest("/smoke/expressions/lazyLogical")
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
            runBoxTest("/smoke/staticInit")
        }

        @Test
        fun testWhile() {
            runBoxTest("/smoke/while")
        }

        @Nested
        inner class Scopes {
        }
        @Nested
        inner class ControlFlow {
            @Test
            fun testBreaksInWhiles() {
                runBoxTest("/smoke/controlFlow/breaksInWhiles")
            }

            @Test
            fun testContinuesInWhiles() {
                runBoxTest("/smoke/controlFlow/continuesInWhiles")
            }

            @Test
            fun testBreaksInFors() {
                runBoxTest("/smoke/controlFlow/breaksInFors")
            }

            @Test
            fun testManyReturns() {
                runBoxTest("/smoke/controlFlow/manyReturns")
            }

            @Test
            fun testContinueInFors() {
                runBoxTest("/smoke/controlFlow/continueInFors")
            }

            @Test
            fun testNestedIfs() {
                runBoxTest("/smoke/controlFlow/nestedIfs")
            }

            @Test
            fun testTrickyFors() {
                runBoxTest("/smoke/controlFlow/trickyFors")
            }

            @Test
            fun testNestedFor() {
                runBoxTest("/smoke/controlFlow/nestedFor")
            }

            @Test
            fun testComplexInterruptions() {
                runBoxTest("/smoke/controlFlow/complexInterruptions")
            }

        }
        @Test
        fun testLocalVariable() {
            runBoxTest("/smoke/localVariable")
        }

    }
}
