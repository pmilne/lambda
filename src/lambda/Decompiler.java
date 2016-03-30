package lambda;

import static lambda.Primitives.primitive;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
public class Decompiler {
    private static String varName(int i) {
        return new String(new byte[]{(byte) ('a' + i)});
    }

    private static interface Mole extends Function {
        Expression getExp();
    }

    public static abstract class Converter {
        public abstract Expression convert(Primitive o);
        public abstract Mole createMole(Expression exp);

        public static Converter create(int level) {
            Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
            return new Converter() {
                public Mole createMole(Expression exp) {
                    return new Mole() {
                        @Override
                        public Expression getExp() {
                            return exp;
                        }

                        public Primitive apply(Primitive a) {
                            return primitive(createMole(c.application(exp, convert(a))));
                        }
                    };
                }

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
                                return ((Mole) f).getExp();
                            }
                            Expression var = c.symbol(varName(level));
                            Converter converter = create(level + 1);
                            Primitive application = f.apply(primitive(converter.createMole(var)));
                            return c.lambda(var, converter.convert(application));
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
