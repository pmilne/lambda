package lambda;

import java.util.function.Function;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class Evaluator {
    // A misnomer, this Stack is immutable -- and lives in the heap.
    public static class Stack<T> {
        private final Stack<T> parent;
        private final T value;

        private Stack(Stack<T> parent, T value) {
            this.parent = parent;
            this.value = value;
        }

        public static <T> Stack<T> create(Stack<T> parent, T value) {
            return new Stack<>(parent, value);
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
        public Object execute(Stack<Object> valueStack);
    }

    // Used by the experimental 'Decompiler'.
    public static interface Marker extends Function<Object, Object> {}

    // This visitor turns symbols into numbers at 'compile' time and provides a mechanism for evaluation.
    private static Expression.Visitor<Implementation> createCompiler(final Stack<String> nameStack) {
        return new Expression.Visitor<Implementation>() {
                    @Override
                    public Implementation constant(Object value) {
                        return (Stack<Object> values) -> value;
                    }

                    @Override
                    public Implementation symbol(String name) {
                        int index = nameStack.indexOf(name);
                        return (Stack<Object> values) -> values.get(index);
                    }

                    @Override
                    public Implementation application(Expression fun, Expression arg) {
                        Implementation fun0 = fun.accept(this);
                        Implementation arg0 = arg.accept(this);
                        return (Stack<Object> values) -> ((Function) fun0.execute(values)).apply(arg0.execute(values));
                    }

                    @Override
                    public Implementation lambda(Expression var, Expression exp) {
                        String varName = var.accept(Expressions.TO_STRING);
                        Implementation exp0 = exp.accept(createCompiler(Stack.create(nameStack, varName)));
                        return (Stack<Object> values) -> (Marker) arg -> exp0.execute(Stack.create(values, arg));
                    }
                };
    }

    public static final Expression.Visitor<Implementation> COMPILER = createCompiler(new Stack<String>(null, null) {
        @Override
        public int indexOf(String name) {
            throw new RuntimeException("Undefined variable: " + name);
        }
    });

    public static Object eval(Expression input) {
        return input.accept(COMPILER).execute(null);
    }
}
