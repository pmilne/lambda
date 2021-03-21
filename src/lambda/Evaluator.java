package lambda;

import java.util.function.Function;

import static lambda.Primitives.toFunction;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
public class Evaluator {

    public static interface Implementation {
        public Primitive eval(List<Primitive> valueStack);
    }

    // This visitor returns the value of an expression.
    private static Expression.Visitor<Primitive> createEvaluator(Function<String, Primitive> env) {
        return new Expression.Visitor<Primitive>() {
            @Override
            public Primitive constant(Primitive value) {
                return value;
            }

            @Override
            public Primitive symbol(String name) {
                return env.apply(name);
            }

            @Override
            public Primitive application(Expression fun, Expression arg) {
                Primitive fun0 = fun.accept(this);
                Primitive arg0 = arg.accept(this);
                return toFunction(fun0).apply(arg0);
            }

            @Override
            public Primitive lambda(String var, Expression exp) {
                return Primitives.CONSTRUCTOR.function(arg -> exp.accept(createEvaluator(s -> s.equals(var) ? arg : env.apply(s))));
            }
        };
    }

    public static final Expression.Visitor<Primitive> EVALUATOR = createEvaluator(name -> {
                throw new RuntimeException("Undefined variable: " + name);
            }
    );

    public static Primitive eval(Expression input) {
        return input.accept(EVALUATOR);
    }
}
