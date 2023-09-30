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

        @Test
        fun testParameterInConstructor() {
            runBoxTest("/smoke/parameterInConstructor")
        }

        @Test
        fun testStaticInit() {
            runBoxTest("/smoke/staticInit")
        }

        @Test
        fun testWhile() {
            runBoxTest("/smoke/while")
        }

        @Test
        fun testLocalVariable() {
            runBoxTest("/smoke/localVariable")
        }

    }
}
