package expr;

import lambda.*;

/**
 * @author pmilne
 */
public class TestExpReader {
    private static void test(String input, String... outputs) {
//        long start = System.currentTimeMillis();
        new ExpReader(Expressions.CONSTRUCTOR).parse(input, new ExpReader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + (input.length() > 1000 ? "<too long>" : exp));
//                System.out.println("Time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
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
        StringBuilder builder = new StringBuilder();
        builder.append(0);
        for(int i = 1; i < 1000; i++) {
            builder.append("+").append(i);
        }
        test(builder.toString() + ";", "499500");
    }
}