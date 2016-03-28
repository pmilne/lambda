package lambda;

import java.util.function.Function;

/**
 * @author pmilne
 */
public class TestExpReader {
    public static final Expression ZERO = Expressions.CONSTRUCTOR.constant(0);
    public static final Expression INC = Expressions.CONSTRUCTOR.constant((Function<Integer, Integer>) x -> x + 1);

    public static int asInteger(Object o) {
        Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
        return (int) Evaluator.eval(c.application(c.application(c.constant(o), INC), ZERO));
    }

    private static void test(String input, String... outputs) {
        new ExpReader(Expressions.CONSTRUCTOR).parse(input, new ExpReader.Processor<Expression>() {
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
        new ExpReader(Expressions.CONSTRUCTOR).parse(input, exp -> {
            System.out.println("Input: " + exp);
            Object value = Evaluator.eval(exp);
            int out = asInteger(value);
            System.out.println("Output: " + out);
            assert output == out;
        });
    }

    private static void test(String input, Class<?> c) {
        try {
            new ExpReader(Expressions.CONSTRUCTOR).parse(input, exp -> {
                System.out.println("Input: " + exp);
                Object value = Evaluator.eval(exp);
                int out = asInteger(value);
                System.out.println("Output: " + out);
                assert false;
            });
        } catch (Exception e) {
            assert c.isAssignableFrom(e.getClass());
        }
    }

    public static void main(String[] args) {
        test("1 + 2 * 3 ;", "7");
        test("1 + 2 * 3 * 4 ;", "25");
        test("1 + 2 + 3 * 4 ", "15");
        test("1 * 2 + 3 ;", "5");
        test("1 * 2 + 3 * 4 + 5 ;", "19");
    }
}