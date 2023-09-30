import kotlin.test.Test
import org.junit.jupiter.api.Nested

class BoxTests {

    @Nested
    inner class Smoke {
        @Test
        fun testInstanceField() {
            runBoxTest("/smoke/instanceField")
        }

        @Test
        fun testFor() {
            runBoxTest("/smoke/for")
        }

        @Test
        fun testInvokeStaticFun() {
            runBoxTest("/smoke/invokeStaticFun")
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
        fun testWhile() {
            runBoxTest("/smoke/while")
        }

        @Test
        fun testLocalVariable() {
            runBoxTest("/smoke/localVariable")
        }

    }
}
