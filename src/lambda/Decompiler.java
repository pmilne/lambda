package lambda;

import static lambda.Primitives.toPrimitive;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
public class Decompiler {
    private static String varName(int i) {
        return new String(new byte[]{(byte) ('a' + i)});
    }

    private static class Mole implements Function {
        public final Expression exp;
        public final Converter converter;

        public Mole(Expression exp, Converter converter) {
            this.exp = exp;
            this.converter = converter;
        }

        public Primitive apply(Primitive a) {
            return toPrimitive(new Mole(Expressions.CONSTRUCTOR.application(exp, converter.convert(a)), converter));
        }
    }

    public static abstract class Converter {
        public abstract Expression convert(Primitive o);

        public static Converter create(int level) {
            Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
            return new Converter() {
                public Expression convert(Primitive o) {
                    return o.accept(new Primitive.Visitor<Expression>() {
                        @Override
                        public Expression integer(int i) {
                            return c.constant(o);
                        }

                        @Override
                        public Expression string(String s) {
                            return c.constant(o);
                        }

                        @Override
                        public Expression function(Function f) {
                            if (f instanceof Mole) {
                                return ((Mole) f).exp;
                            }
                            Expression var = c.symbol(varName(level));
                            Converter converter = create(level + 1);
                            Primitive apply = f.apply(toPrimitive(new Mole(var, converter)));
                            return c.lambda(var, converter.convert(apply));
                        }
                    });
                }
            };
        }
    }

    public static final Converter TO_EXPRESSION = Converter.create(0);

    public static Expression toExpression(Primitive o) {
        return TO_EXPRESSION.convert(o);
    }
}
