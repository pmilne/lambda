package lambda;

import java.util.function.Function;

/**
 *  Experimental code to convert functions into Expressions.
 *
 * @author pmilne
 */
public class Decompiler {
    private static String varName(int i) {
        return new String(new byte[]{(byte) ('a' + i)});
    }

    private static class Mole implements Function<Object, Object> {
        public final Expression exp;
        public final Converter converter;

        public Mole(Expression exp, Converter converter) {
            this.exp = exp;
            this.converter = converter;
        }

        public Object apply(Object a) {
            return new Mole(Expressions.CONSTRUCTOR.application(exp, converter.convert(a)), converter);
        }
    }

    public static abstract class Converter {
        public abstract Expression convert(Object o);

        public static Converter create(int level) {
            return new Converter() {
                public Expression convert(Object o) {
                    if (o instanceof Mole) {
                        Mole mole = (Mole) o;
                        return mole.exp;
                    }
                    if (o instanceof Evaluator.Marker) {
                        Evaluator.Marker f = (Evaluator.Marker) o;
                        Expression var = Expressions.CONSTRUCTOR.symbol(varName(level));
                        Converter converter = create(level + 1);
                        return Expressions.CONSTRUCTOR.lambda(var, converter.convert(f.apply(new Mole(var, converter))));
                    }
                    return Expressions.CONSTRUCTOR.constant(o);
                }
            };
        }
    }

    public static final Converter TO_EXPRESSION = Converter.create(0);

    public static Expression toExpression(Object o) {
        return TO_EXPRESSION.convert(o);
    }
}
