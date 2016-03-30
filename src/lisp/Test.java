package lisp;

import java.util.*;

import lambda.*;

import static lambda.Primitives.toInt;
import static lambda.Primitives.primitive;

/**
 * @author pmilne
 */
public class Test {
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
    private static final Primitive SUM = primitive(new Function() {
        @Override
        public Primitive apply(Primitive x) {
            return primitive(y -> primitive(toInt(x) + toInt(y)));
        }

        @Override
        public String toString() {
            return "+";
        }
    });

    private static final Primitive PRD = primitive(new Function() {
        @Override
        public Primitive apply(Primitive x) {
            return primitive(y -> primitive(toInt(x) * toInt(y)));
        }

        @Override
        public String toString() {
            return "*";
        }
    });


    private static final boolean TEST_PERFORMANCE = false;

    private static Map<String, Primitive> getGlobals() {
        Map<String, Primitive> globals = new HashMap<>();
        globals.put("inc", INC);
        globals.put("*", PRD);
        globals.put("+", SUM);
        return globals;
    }

    private static final Map<String, Primitive> GLOBALS = getGlobals();

    private static void test(String input, Object... outputs) {
        new Reader(Expressions.CONSTRUCTOR).parse(input, new Reader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + exp);
                Expression subst = Expressions.substitute(exp, GLOBALS);
                Primitive value = Evaluator.eval(subst);
                Expression out = Decompiler.toExpression(value);
                String outString = out.toString();
                System.out.println("Output: " + outString);
                Object output = outputs[index++];
                assert output.toString().equals(outString);
            }
        });
    }

    private static Primitive[] read(String input) {
        List<Primitive> result = new ArrayList<>();
        new Reader(Expressions.CONSTRUCTOR).parse(input, exp -> {
            Expression subst = Expressions.substitute(exp, GLOBALS);
            Primitive value = Evaluator.eval(subst);
            result.add(value);
        });
        return result.toArray(new Primitive[result.size()]);
    }

    private static void test(String input, Class<?> c) {
        try {
            new Reader(Expressions.CONSTRUCTOR).parse(input, exp -> {
                System.out.println("Input: " + exp);
                Expression subst = Expressions.substitute(exp, GLOBALS);
                Primitive value = Evaluator.eval(subst);
                Expression out = Decompiler.toExpression(value);
                String outString = out.toString();
                System.out.println("Output: " + outString);
                assert false;
            });
        } catch (Exception e) {
            assert c.isAssignableFrom(e.getClass());
        }
    }

    public static void main(String[] args) {
        GLOBALS.put("two", read("(lambda (f x) (f (f x)))")[0]);
        test("(lambda (x) x)", "(lambda (a) a)");
        test("((lambda (x) x) 1)", "1");
        test("((lambda (x) x) (lambda (x) x))", "(lambda (a) a)");
        test("((lambda (x) (x x)) (lambda (x) x))", "(lambda (a) a)");
        test("((lambda (f) (f f)) two)", "(lambda (a) (lambda (b) (a (a (a (a b))))))");
        test("(lambda (f) (f (lambda (x) (f x 1))))", "(lambda (a) (a (lambda (b) ((a b) 1))))");
        test("((lambda (f) (f f)) two inc 0)", 4); // 2^2
        test("((lambda (f) (f f f)) two inc 0)", 16); // 2^4
        test("((lambda (f) (f f f f)) two inc 0)", 65536); // 2^16
//        test("((lambda (f) (f f f f f)) two)", 0); // 2^65536 - stack overflow
//        test("((lambda (f) (f f)) (lambda (f) (f f))))", -1); // hangs (correctly)
        test("((lambda (f) (f (f f) f)) two inc 0)", 65536); // 2^16
        test("((lambda (f) (f (f f f))) two inc 0)", 256); // 2^8
        test("((lambda (f) (f f (f f))) two inc 0)", 256); // 2^8
        test("1", 1);
        test("(2 3)", 9);
        test("(3)", 3);
        test("(3 two)", "(lambda (a) (lambda (b) (a (a (a (a (a (a (a (a b))))))))))");
        test("(3 two inc 0)", 8);
        test("(two 3)", "(lambda (a) (lambda (b) (a (a (a (a (a (a (a (a (a b)))))))))))");
        test("(two 3 inc 0)", 9);
        test("(7 8)", 2097152);
        test("(9 8)", 134217728);
        test("(* 2 3)", 6);
        test("(+ 2 3)", 5);
        test("(+ 1 (* 2 3))", 7);
        test("((2 3) (3 2))", 134217728);
        test("(1 (2 3) ((4 5) 6))", 0);
        test("(lambda (x) c)", RuntimeException.class);
        test("1 2", "1", "2");
        if (TEST_PERFORMANCE) {
            System.out.println("Starting evaluator performance test (typical run time is ~115s)... ");
            long start = System.currentTimeMillis();
            test("((lambda (f) (f f f (f f))) two inc 0)", 0); // 2^32 about 1 min
            System.out.println("Time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
        }
    }
}