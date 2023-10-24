import kotlin.test.Test
import org.junit.jupiter.api.Nested

class CodegenBoxTests {

    @Nested
    inner class Smoke {
        @Nested
        inner class Generics {
            @Test
            fun testSimpleGenericMethod() {
                runCodegenBoxTest("/smoke/generics/simpleGenericMethod")
            }

            @Test
            fun testContrvariantFunc() {
                runCodegenBoxTest("/smoke/generics/contrvariantFunc")
            }

            @Test
            fun testCovarianFun() {
                runCodegenBoxTest("/smoke/generics/covarianFun")
            }

            @Test
            fun testClassWithTypeParameterField() {
                runCodegenBoxTest("/smoke/generics/classWithTypeParameterField")
            }

            @Test
            fun testRecursiveUpperBound() {
                runCodegenBoxTest("/smoke/generics/recursiveUpperBound")
            }

            @Test
            fun testGenericClassInstantiation() {
                runCodegenBoxTest("/smoke/generics/genericClassInstantiation")
            }

            @Test
            fun testPair() {
                runCodegenBoxTest("/smoke/generics/pair")
            }

            @Test
            fun testClassWithNonTrivialUpperBoundParam() {
                runCodegenBoxTest("/smoke/generics/classWithNonTrivialUpperBoundParam")
            }

        }
        @Test
        fun testIf() {
            runCodegenBoxTest("/smoke/if")
        }

        @Test
        fun testInstanceField() {
            runCodegenBoxTest("/smoke/instanceField")
        }

        @Test
        fun testFor() {
            runCodegenBoxTest("/smoke/for")
        }

        @Nested
        inner class Initializing {
            @Test
            fun testFieldInitDebug() {
                runCodegenBoxTest("/smoke/initializing/fieldInitDebug")
            }

            @Test
            fun testSimpleFieldsInit() {
                runCodegenBoxTest("/smoke/initializing/simpleFieldsInit")
            }

            @Test
            fun testFieldsAndInheritance() {
                runCodegenBoxTest("/smoke/initializing/fieldsAndInheritance")
            }

            @Test
            fun testFieldAccess() {
                runCodegenBoxTest("/smoke/initializing/fieldAccess")
            }

            @Test
            fun testFieldInheritance() {
                runCodegenBoxTest("/smoke/initializing/fieldInheritance")
            }

            @Test
            fun testComplexFieldInit() {
                runCodegenBoxTest("/smoke/initializing/complexFieldInit")
            }

            @Test
            fun testHomogenousFieldAccess() {
                runCodegenBoxTest("/smoke/initializing/homogenousFieldAccess")
            }

        }
        @Test
        fun testInvokeInstanceFunc() {
            runCodegenBoxTest("/smoke/invokeInstanceFunc")
        }

        @Test
        fun testInvokeStaticFun() {
            runCodegenBoxTest("/smoke/invokeStaticFun")
        }

        @Test
        fun testArray() {
            runCodegenBoxTest("/smoke/array")
        }

        @Nested
        inner class Casts {
            @Test
            fun testCastToArray() {
                runCodegenBoxTest("/smoke/casts/castToArray")
            }

            @Test
            fun testSimpleCast() {
                runCodegenBoxTest("/smoke/casts/simpleCast")
            }

        }
        @Test
        fun testSimplestReturn() {
            runCodegenBoxTest("/smoke/simplestReturn")
        }

        @Test
        fun testStaticFieldInit() {
            runCodegenBoxTest("/smoke/staticFieldInit")
        }

        @Nested
        inner class Resolution {
            @Test
            fun testSimpleInhritance() {
                runCodegenBoxTest("/smoke/resolution/simpleInhritance")
            }

            @Test
            fun testIncrementerTest() {
                runCodegenBoxTest("/smoke/resolution/incrementerTest")
            }

            @Test
            fun testParamAssignment() {
                runCodegenBoxTest("/smoke/resolution/paramAssignment")
            }

            @Test
            fun testMethodOverridding() {
                runCodegenBoxTest("/smoke/resolution/methodOverridding")
            }

        }
        @Test
        fun testParameterInConstructor() {
            runCodegenBoxTest("/smoke/parameterInConstructor")
        }

        @Nested
        inner class Arrays {
            @Test
            fun testDefaultArray() {
                runCodegenBoxTest("/smoke/arrays/defaultArray")
            }

            @Test
            fun testSimpleArrayAndLoops() {
                runCodegenBoxTest("/smoke/arrays/simpleArrayAndLoops")
            }

            @Test
            fun testArray2d() {
                runCodegenBoxTest("/smoke/arrays/array2d")
            }

        }
        @Nested
        inner class Expressions {
            @Test
            fun testAddition() {
                runCodegenBoxTest("/smoke/expressions/addition")
            }

            @Test
            fun testMultiplication() {
                runCodegenBoxTest("/smoke/expressions/multiplication")
            }

            @Test
            fun testDiscriminant() {
                runCodegenBoxTest("/smoke/expressions/discriminant")
            }

            @Test
            fun testLazyLogical() {
                runCodegenBoxTest("/smoke/expressions/lazyLogical")
            }

        }
        @Nested
        inner class Stdlib {
            @Test
            fun testStringEquality() {
                runCodegenBoxTest("/smoke/stdlib/stringEquality")
            }

            @Test
            fun testEqualsOverridding() {
                runCodegenBoxTest("/smoke/stdlib/equalsOverridding")
            }

            @Test
            fun testHashCodeOverridding() {
                runCodegenBoxTest("/smoke/stdlib/hashCodeOverridding")
            }

            @Test
            fun testOveriddenToString() {
                runCodegenBoxTest("/smoke/stdlib/overiddenToString")
            }

        }
        @Nested
        inner class Instantiating {
        }
        @Test
        fun testStaticInit() {
            runCodegenBoxTest("/smoke/staticInit")
        }

        @Test
        fun testInterface() {
            runCodegenBoxTest("/smoke/interface")
        }

        @Test
        fun testWhile() {
            runCodegenBoxTest("/smoke/while")
        }

        @Nested
        inner class Scopes {
            @Test
            fun testFieldsAndInheritance() {
                runCodegenBoxTest("/smoke/scopes/fieldsAndInheritance")
            }

            @Test
            fun testHidingLocalVariables() {
                runCodegenBoxTest("/smoke/scopes/hidingLocalVariables")
            }

        }
        @Test
        fun testAbsTest() {
            runCodegenBoxTest("/smoke/absTest")
        }

        @Nested
        inner class ControlFlow {
            @Test
            fun testBreaksInWhiles() {
                runCodegenBoxTest("/smoke/controlFlow/breaksInWhiles")
            }

            @Test
            fun testContinuesInWhiles() {
                runCodegenBoxTest("/smoke/controlFlow/continuesInWhiles")
            }

            @Test
            fun testLoops() {
                runCodegenBoxTest("/smoke/controlFlow/loops")
            }

            @Test
            fun testBreaksInFors() {
                runCodegenBoxTest("/smoke/controlFlow/breaksInFors")
            }

            @Test
            fun testManyReturns() {
                runCodegenBoxTest("/smoke/controlFlow/manyReturns")
            }

            @Test
            fun testBreaksAndContinues() {
                runCodegenBoxTest("/smoke/controlFlow/breaksAndContinues")
            }

            @Test
            fun testContinueInFors() {
                runCodegenBoxTest("/smoke/controlFlow/continueInFors")
            }

            @Test
            fun testNestedIfs() {
                runCodegenBoxTest("/smoke/controlFlow/nestedIfs")
            }

            @Test
            fun testTrickyFors() {
                runCodegenBoxTest("/smoke/controlFlow/trickyFors")
            }

            @Test
            fun testIfReturn() {
                runCodegenBoxTest("/smoke/controlFlow/ifReturn")
            }

            @Test
            fun testNestedFor() {
                runCodegenBoxTest("/smoke/controlFlow/nestedFor")
            }

            @Test
            fun testComplexInterruptions() {
                runCodegenBoxTest("/smoke/controlFlow/complexInterruptions")
            }

        }
        @Test
        fun testLocalVariable() {
            runCodegenBoxTest("/smoke/localVariable")
        }

    }
}
