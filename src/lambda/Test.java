package lambda;

import java.util.function.Function;

/**
 * @author pmilne
 */
public class Test {
    public static final Expression ZERO = Expressions.CONSTRUCTOR.constant(0);
    public static final Expression INC = Expressions.CONSTRUCTOR.constant((Function<Integer, Integer>) x -> x + 1);

    public static int asInteger(Object o) {
        Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
        return (int) Evaluator.eval(c.application(c.application(c.constant(o), INC), ZERO));
    }

    private static void test(String input, String... outputs) {
        new Reader(Expressions.CONSTRUCTOR).parse(input, new Reader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + exp);
                Object value = Evaluator.eval(exp);
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
            Object value = Evaluator.eval(exp);
            int out = asInteger(value);
            System.out.println("Output: " + out);
            assert output == out;
        });
    }

    public static void main(String[] args) {
        test("1 2", "1", "2");
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
//        test("((lambda (f) (f f)) (lambda (f) (f f))))", -1); // hangs
        test("((lambda (f) (f (f f) f)) (lambda (f x) (f (f x))))", 65536); // 2^16
        test("((lambda (f) (f (f f f))) (lambda (f x) (f (f x))))", 256); // 2^8
        test("((lambda (f) (f f (f f))) (lambda (f x) (f (f x))))", 256); // 2^8
        System.out.println("Starting evaluator performance test (typical run time is ~70s)... ");
        long start = System.currentTimeMillis();
        test("((lambda (f) (f f f (f f))) (lambda (f x) (f (f x))))", 0); // 2^32 about 1 min
        System.out.println("Time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
    }
}