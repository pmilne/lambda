package lisp;

import lambda.*;

import static lambda.Primitives.toInt;
import static lambda.Primitives.primitive;

/**
 * @author pmilne
 */
public class Test {
    private static final Primitive ZERO = primitive(0);
    private static final Primitive INC = primitive(new Function() {
        @Override
        public Primitive apply(Primitive x) {
            return primitive(toInt(x) + 1);
        }

        @Override
        public String toString() {
            return "inc";
        }
    });

    private static final boolean TEST_PERFORMANCE = false;

    private static int asInteger(Primitive o) {
        Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
        return toInt(Evaluator.eval(c.application(c.application(c.constant(o), c.constant(INC)), c.constant(ZERO))));
    }

    private static void test(String input, String... outputs) {
        new Reader(Expressions.CONSTRUCTOR).parse(input, new Reader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + exp);
                Primitive value = Evaluator.eval(exp);
                Expression out = Decompiler.toExpression(value);
                String outString = out.toString();
                System.out.println("Output: " + outString);
                String output = outputs[index++];
                assert output.equals(outString);
            }
        });
    }

    private static void test(String input, int output) {
        new Reader(Expressions.CONSTRUCTOR).parse(input, exp -> {
            System.out.println("Input: " + exp);
            Primitive value = Evaluator.eval(exp);
            int out = asInteger(value);
            System.out.println("Output: " + out);
            assert output == out;
        });
    }

    private static void test(String input, Class<?> c) {
        try {
            new Reader(Expressions.CONSTRUCTOR).parse(input, exp -> {
                System.out.println("Input: " + exp);
                Primitive value = Evaluator.eval(exp);
                int out = asInteger(value);
                System.out.println("Output: " + out);
                assert false;
            });
        } catch (Exception e) {
            assert c.isAssignableFrom(e.getClass());
        }
    }

    public static void main(String[] args) {
        test("1", "1");
//        test("(1 2)", "");
//        test("(1 (2 3) ((4 5 6) 7))", "");
        test("(lambda (x) x)", "(lambda (a) a)");
        test("((lambda (x) x) 1)", "1");
        test("((lambda (x) x) (lambda (x) x))", "(lambda (a) a)");
        test("((lambda (x) (x x)) (lambda (x) x))", "(lambda (a) a)");
        test("((lambda (f) (f f)) (lambda (f x) (f (f x))))", "(lambda (a) (lambda (b) (a (a (a (a b))))))");
        test("(lambda (f) (f (lambda (x) (f x 1))))", "(lambda (a) (a (lambda (b) ((a b) 1))))");
        test("((lambda (f) (f f)) (lambda (f x) (f (f x))))", 4); // 2^2
        test("((lambda (f) (f f f)) (lambda (f x) (f (f x))))", 16); // 2^4
        test("((lambda (f) (f f f f)) (lambda (f x) (f (f x))))", 65536); // 2^16
//        test("((lambda (f) (f f f f f)) (lambda (f x) (f (f x))))", 0); // 2^65536 - stack overflow
//        test("((lambda (f) (f f)) (lambda (f) (f f))))", -1); // hangs (correctly)
        test("((lambda (f) (f (f f) f)) (lambda (f x) (f (f x))))", 65536); // 2^16
        test("((lambda (f) (f (f f f))) (lambda (f x) (f (f x))))", 256); // 2^8
        test("((lambda (f) (f f (f f))) (lambda (f x) (f (f x))))", 256); // 2^8
        test("(lambda (x) c)", RuntimeException.class);
//        test("1 2", "1", "2");
        if (TEST_PERFORMANCE) {
            System.out.println("Starting evaluator performance test (typical run time is ~70s)... ");
            long start = System.currentTimeMillis();
            test("((lambda (f) (f f f (f f))) (lambda (f x) (f (f x))))", 0); // 2^32 about 1 min
            System.out.println("Time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
        }
    }
}