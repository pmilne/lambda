package lambda;

import java.util.function.Function;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class Evaluator {
    // A misnomer, this Stack is immutable -- and lives in the heap.
    private static class Stack<T> {
        private final Stack<T> parent;
        private final T value;

        private Stack(Stack<T> parent, T value) {
            this.parent = parent;
            this.value = value;
        }

        public static <T> Stack<T> create(Stack<T> parent, T value) {
            return new Stack<T>(parent, value);
        }

        public T get(int n) {
            Stack<T> that = this;
            for (int i = 0; i < n; i++) {
                that = that.parent;
            }
            return that.value;
        }

        public int indexOf(T o) {
            return value.equals(o) ? 0 : 1 + parent.indexOf(o);
        }

        public String toString() {
            return "{" + parent + " " + value + "}";
        }
    }

    public static interface Implementation {
        public Object execute(Stack<Object> s);
    }

    // Used by the experimental 'Decompiler'.
    public static interface Marker extends Function<Object, Object> {}

    // 'Compiler' is a bit misleading!
    // This visitor turns symbols into numbers at 'compile' time and provides a mechanism for evaluation.
    public static final Expression.Visitor<Implementation> COMPILER = new Expression.Visitor<Implementation>() {
                private Stack<String> lexicalVars = new Stack<String>(null, null) {
                    @Override
                    public int indexOf(String name) {
                        throw new RuntimeException("Undefined variable: " + name);
                    }
                };

                @Override
                public Implementation constant(Object c) {
                    return s -> c;
                }

                @Override
                public Implementation symbol(String name) {
                    int index = lexicalVars.indexOf(name);
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
                    lexicalVars = Stack.create(lexicalVars, var.accept(Expressions.TO_STRING));
                    Implementation l = exp.accept(this);
                    lexicalVars = lexicalVars.parent;
                    return s -> (Marker) a -> l.execute(Stack.create(s, a));
                }
            };

    public static Object eval(Expression input) {
        return input.accept(COMPILER).execute(null);
    }
}
