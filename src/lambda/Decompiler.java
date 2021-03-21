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

    private static interface Mole extends PrimitiveFunction {
        Expression getExpression();
    }

    public static abstract class Converter {
        public abstract Expression toExpression(Primitive o);

        public abstract Expression createLambda(String var, PrimitiveFunction f);

        public static Converter create(int level) {
            Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
            return new Converter() {
                private Mole createMole(Expression exp) {
                    return new Mole() {
                        @Override
                        public Expression getExpression() {
                            return exp;
                        }

                        public Primitive apply(Primitive a) {
                            return primitive(createMole(c.application(exp, toExpression(a))));
                        }
                    };
                }

                public Expression createLambda(String var, PrimitiveFunction f) {
                    return c.lambda(var, toExpression(f.apply(primitive(createMole(c.symbol(var))))));
                }

                public Expression toExpression(Primitive o) {
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
                        public Expression function(PrimitiveFunction f) {
                            if (f instanceof Mole) {
                                return ((Mole) f).getExpression();
                            }
                            return create(level + 1).createLambda(varName(level), f);
                        }
                    });
                }
            };
        }
    }

    public static final Converter TO_EXPRESSION = Converter.create(0);

    public static Expression toExpression(Primitive o) {
        return TO_EXPRESSION.toExpression(o);
    }
}
