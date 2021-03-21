package lambda;

import static lambda.Primitives.toFunction;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
public class Evaluator {

    public static interface Implementation {
        public Primitive eval(List<Primitive> valueStack);
    }

    // This visitor turns symbols into numbers at 'compile' time and provides a mechanism for evaluation.
    private static Expression.Visitor<Implementation> createEvaluator(List<String> nameStack) {
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
                    public Implementation lambda(String var, Expression exp) {
                        Implementation exp0 = exp.accept(createEvaluator(List.create(nameStack, var)));
                        return env -> Primitives.CONSTRUCTOR.function(arg -> exp0.eval(List.create(env, arg)));
                    }
                };
    }

    public static final Expression.Visitor<Implementation> EVALUATOR = createEvaluator(new List<String>(null, null) {
        @Override
        public int indexOf(String name) {
            throw new RuntimeException("Undefined variable: " + name);
        }
    });

    public static Primitive eval(Expression input) {
        return input.accept(EVALUATOR).eval(null);
    }
}
