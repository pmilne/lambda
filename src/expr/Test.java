package expr;

import java.util.HashMap;
import java.util.Map;

import lambda.*;

import static lambda.Primitives.toInt;
import static lambda.Primitives.toPrimitive;

/**
 * @author pmilne
 */
public class Test {
    private static final Primitive INC = toPrimitive(new Function() {
        @Override
        public Primitive apply(Primitive x) {
            return toPrimitive(toInt(x) + 1);
        }

        @Override
        public String toString() {
            return "inc";
        }
    });

    private static final Primitive SUM = toPrimitive(new Function() {
        @Override
        public Primitive apply(Primitive x) {
            return toPrimitive(y -> toPrimitive(toInt(x) + toInt(y)));
        }

        @Override
        public String toString() {
            return "+";
        }
    });

    private static final Primitive PRD = toPrimitive(new Function() {
        @Override
        public Primitive apply(Primitive x) {
            return toPrimitive(y -> toPrimitive(toInt(x) * toInt(y)));
        }

        @Override
        public String toString() {
            return "*";
        }
    });

    private static Map<String, Primitive> getGlobals() {
        Map<String, Primitive> globals = new HashMap<>();
        globals.put("inc", INC);
        globals.put("*", PRD);
        globals.put("+", SUM);
        return globals;
    }

    private static final Map<String, Primitive> GLOBALS = getGlobals();

    private static void test(String input, String... outputs) {
//        long start = System.currentTimeMillis();
        new Reader(Expressions.CONSTRUCTOR).parse(input, new Reader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + (input.length() > 1000 ? "<too long>" : exp));
//                System.out.println("Parse time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
                Expression subst = Expressions.substitute(exp, GLOBALS);
                Primitive value = Evaluator.eval(subst);
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