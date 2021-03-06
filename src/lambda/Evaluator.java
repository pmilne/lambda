package lambda;

import static lambda.Primitives.toFunction;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
public class Evaluator {
    // A misnomer, Stack is immutable and heap allocated.
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
        public Primitive eval(Stack<Primitive> valueStack);
    }

    // This visitor turns symbols into numbers at 'compile' time and provides a mechanism for evaluation.
    private static Expression.Visitor<Implementation> createCompiler(Stack<String> nameStack) {
        return new Expression.Visitor<Implementation>() {
                    @Override
                    public Implementation constant(Primitive value) {
                        return env -> value;
                    }

                    @Override
                    public Implementation symbol(String name) {
                        int index = nameStack.indexOf(name);
                        return env -> env.get(index);
                    }

                    @Override
                    public Implementation application(Expression fun, Expression arg) {
                        Implementation fun0 = fun.accept(this);
                        Implementation arg0 = arg.accept(this);
                        return env -> toFunction(fun0.eval(env)).apply(arg0.eval(env));
                    }

                    @Override
                    public Implementation lambda(Expression var, Expression exp) {
                        String varName = var.accept(Expressions.TO_STRING);
                        Implementation exp0 = exp.accept(createCompiler(Stack.create(nameStack, varName)));
                        return env -> Primitives.CONSTRUCTOR.function(arg -> exp0.eval(Stack.create(env, arg)));
                    }
                };
    }

    public static final Expression.Visitor<Implementation> COMPILER = createCompiler(new Stack<String>(null, null) {
        @Override
        public int indexOf(String name) {
            throw new RuntimeException("Undefined variable: " + name);
        }
    });

    public static Primitive eval(Expression input) {
        return input.accept(COMPILER).eval(null);
    }
}
