package expr;

import lambda.*;

/**
 * @author pmilne
 */
public class TestExpReader {
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
    public static void main(String[] args) {
        test("1 + 2 * 3 ;", "7");
        test("1 + 2 * 3 * 4 ;", "25");
        test("1 + 2 + 3 * 4 ;", "15");
        test("1 + (2 + 3) * 4 ;", "21");
        test("1 * 2 + 3 ;", "5");
        test("1 * 2 + 3 * 4 + 5 ;", "19");
        test("1 * 2 + 3 * (4 + 5) ;", "29");
    }
}