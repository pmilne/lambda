package lambda;

import java.util.function.Function;

import static lambda.Primitives.toFunction;

/**
 * @author pmilne
 */
public class Evaluator {
    private static final Function<String, Primitive> TOP = name -> {
        throw new RuntimeException("Undefined variable: " + name);
    };

    public static Primitive eval(Expression e, Function<String, Primitive> env) {
        return e.accept(new Expression.Visitor<Primitive>() {
            @Override
            public Primitive constant(Primitive value) {
                return value;
            }

            @Override
            public Primitive symbol(String name) {
                return env.apply(name);
            }

            @Override
            public Primitive lambda(String var, Expression exp) {
                return Primitives.CONSTRUCTOR.function(arg -> eval(exp, s -> s.equals(var) ? arg : env.apply(s)));
            }

            @Override
            public Primitive application(Expression fun, Expression arg) {
                Primitive fun0 = fun.accept(this);
                Primitive arg0 = arg.accept(this);
                return toFunction(fun0).apply(arg0);
            }
        });
    }

    public static Primitive eval(Expression e) {
        return eval(e, TOP);
    }
}
