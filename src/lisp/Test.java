package lisp;

import java.util.*;
import java.util.function.Function;

import lambda.*;
import lambda.Compiler;

import static lambda.Primitives.*;

/**
 * @author pmilne
 */
public class Test {
    private static final boolean TEST_PERFORMANCE = false;

    private static Map<String, Primitive> getGlobals() {
        Map<String, Primitive> globals = new HashMap<>();
        globals.put("inc", INC);
        globals.put("*", PRD);
        globals.put("+", SUM);
        return globals;
    }

    private static final Map<String, Primitive> GLOBALS = getGlobals();

    private static void test(Function<Expression, Primitive> evaluator, String input, Object... outputs) {
        new Reader(Expressions.CONSTRUCTOR).parse(input, new Reader.Processor<Expression>() {
            private int index = 0;

            @Override
            public void process(Expression exp) {
                System.out.println("Input: " + exp);
                Expression subst = Expressions.substitute(exp, GLOBALS);
                Primitive value = evaluator.apply(subst);
//                System.out.println("Value: " + value);
                Expression out = Decompiler.toExpression(value);
                String outString = out.toString();
                System.out.println("Output: " + outString);
                Object output = outputs[index++];
                assert output.toString().equals(outString);
            }
        });
    }

    private static void test(Function<Expression, Primitive> evaluator, String input, Class<?> c) {
        try {
            new Reader(Expressions.CONSTRUCTOR).parse(input, exp -> {
                System.out.println("Input: " + exp);
                Expression subst = Expressions.substitute(exp, GLOBALS);
                Primitive value = evaluator.apply(subst);
                Expression out = Decompiler.toExpression(value);
                String outString = out.toString();
                System.out.println("Output: " + outString);
                assert false;
            });
        } catch (Exception e) {
            assert c.isAssignableFrom(e.getClass());
        }
    }

    private static void test(String input, Object... outputs) {
        test(Evaluator::eval, input, outputs);
//        test(Compiler::eval, input, outputs);
    }

    private static void test(String input, Class<?> c) {
        test(Evaluator::eval, input, c);
//        test(Compiler::eval, input, c);
    }

    public static void main(String[] args) {
//        System.out.println("decomp: " + Decompiler.toExpression(read("((lambda (f) (f (lambda (x) x))))")[0]));
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
            System.out.println("Starting evaluator performance test (typical run time is ~105s)... ");
            long start = System.currentTimeMillis();
            test("((lambda (f) (f f f (f f))) two inc 0)", 0); // 2^32
            System.out.println("Time: " + (System.currentTimeMillis() - start)/1000.0 + "s");
        }
    }
}