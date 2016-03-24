package lambda;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class Evaluator {
    // A misnomer, this Stack is immutable -- and lives in the heap.
    public static interface Stack {
        public static final Stack EMPTY = n -> {
            throw new RuntimeException("Internal error: stack is empty! ");
        };

        public Object get(int n);

        public static Stack create(Stack parent, Object value) {
            return n -> n == 0 ? value : parent.get(n - 1);
        }
    }

    public static interface Implementation {
        public Object execute(Stack s);
    }

    // Used by the experimental 'Decompiler'.
    public static interface Marker extends Function<Object, Object> {}

    // 'Compiler' is a bit misleading! This visitor turns symbols into numbers at 'compile' time.
    public static final Expression.Visitor<Implementation> COMPILER =
            new Expression.Visitor<Implementation>() {
                private ArrayList<String> lexicalVars = new ArrayList<>();

                @Override
                public Implementation constant(Object c) {
                    return s -> c;
                }

                @Override
                public Implementation symbol(String name) {
                    int lastIndex = lexicalVars.lastIndexOf(name);
                    if (lastIndex == -1) {
                        throw new RuntimeException("Undefined variable: " + name);
                    }
                    int index = lexicalVars.size() - 1 - lastIndex;
                    return s -> s.get(index);
                }

                @Override
                public Implementation application(Expression fun, Expression arg) {
                    Implementation f = fun.accept(this);
                    Implementation a = arg.accept(this);
                    return s -> ((Function) f.execute(s)).apply(a.execute(s));
                }

                @Override
                public Implementation lambda(Expression var, Expression exp) {
                    lexicalVars.add(var.accept(Expressions.TO_STRING));
                    Implementation l = exp.accept(this);
                    lexicalVars.remove(lexicalVars.size() - 1);
                    return s -> (Marker) a -> l.execute(Stack.create(s, a));
                }
            };

    public static Object eval(Expression input) {
        return input.accept(COMPILER).execute(Stack.EMPTY);
    }
}
