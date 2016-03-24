package lambda;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class Evaluator {
    // A misnomer, this Stack is immutable -- and lives in the heap.
    private static class Stack {
        public static final Stack EMPTY = null;

        private final Stack parent;
        private final Object value;

        public Stack(Stack parent, Object value) {
            this.parent = parent;
            this.value = value;
        }

        public Object get(int n) {
            Stack that = this;
            for (int i = 0; i < n; i++) { // Time: 66942
                that = that.parent;
            }
            return that.value;
        }

        public String toString() {
            return "{" + parent + " " + value + "}";
        }
    }

    public static interface Implementation {
        public Object execute(Stack s);
    }

    // Used by the experimental 'Decompiler'.
    public static interface Marker extends Function<Object, Object> {}

    // 'Compiler' is a bit misleading!
    // This visitor turns symbols into numbers at 'compile' time and provides a mechanism for evaluation.
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
                    return s -> (Marker) a -> l.execute(new Stack(s, a));
                }
            };

    public static Object eval(Expression input) {
        return input.accept(COMPILER).execute(Stack.EMPTY);
    }
}
