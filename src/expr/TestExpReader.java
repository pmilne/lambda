package expr;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import lambda.*;

/**
 * @author pmilne
 */
public class TestExpReader {
    public static final Function<Integer, Integer> INC = new Function<Integer, Integer>() {
        @Override
        public Integer apply(Integer x) {
            return x + 1;
        }

        @Override
        public String toString() {
            return "inc";
        }
    };

    public static final Function<Integer, Function<Integer, Integer>> SUM = new Function<Integer, Function<Integer, Integer>>() {
        @Override
        public Function<Integer, Integer> apply(Integer x) {
            return y -> x + y;
        }

        @Override
        public String toString() {
            return "+";
        }
    };

    public static final Function<Integer, Function<Integer, Integer>> PRD = new Function<Integer, Function<Integer, Integer>>() {
        @Override
        public Function<Integer, Integer> apply(Integer x) {
            return y -> x * y;
        }

        @Override
        public String toString() {
            return "*";
        }
    };

    private static Map<String, Object> getGlobals() {
        Map<String, Object> globals = new HashMap<>();
        globals.put("*", PRD);
        globals.put("+", SUM);
        globals.put("inc", INC);
        return globals;
    }

    public static final Map<String, Object> GLOBALS = getGlobals();

    private static void test(String input, String... outputs) {
//        long start = System.currentTimeMillis();
        new ExpReader(Expressions.CONSTRUCTOR).parse(input, new ExpReader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + (input.length() > 1000 ? "<too long>" : exp));
//                System.out.println("Parse time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
                Expression subst = Expressions.substitute(exp, GLOBALS);
                Object value = Evaluator.eval(subst);
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
        test("(inc 2) * (inc 3) ;", "12");
        test("inc 2 * (inc 4) ;", "15");
        StringBuilder builder = new StringBuilder();
        builder.append(0);
        for(int i = 1; i < 1000; i++) {
            builder.append("+").append(i);
        }
        test(builder.toString() + ";", "499500");
    }
}