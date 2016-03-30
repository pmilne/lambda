package lambda;

import java.util.Map;

/**
 * @author pmilne
 */
public class Expressions {
    public static final Expression.Visitor<Expression> CONSTRUCTOR =
            new Expression.Visitor<Expression>() {
                @Override
                public Expression constant(Primitive c) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.constant(c);
                        }
                    };
                }

                @Override
                public Expression symbol(String name) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.symbol(name);
                        }
                    };
                }

                @Override
                public Expression application(Expression fun, Expression arg) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.application(fun, arg);
                        }
                    };
                }

                @Override
                public Expression lambda(Expression var, Expression exp) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.lambda(var, exp);
                        }
                    };
                }
            };

    @SuppressWarnings("WeakerAccess")
    public static final Expression.Visitor<String> TO_STRING = new Expression.Visitor<String>() {
        @Override
        public String constant(Primitive c) {
            return c.toString();
        }

        @Override
        public String symbol(String name) {
            return name;
        }

        @Override
        public String application(Expression fun, Expression arg) {
            return "(" + fun.accept(this) + " " + arg.accept(this) + ")";
        }

        @Override
        public String lambda(Expression var, Expression exp) {
            return "(lambda (" + var.accept(this) + ") " + exp.accept(this) + ")";
        }
    };


    public static Expression substitute(Expression input, Map<String, Primitive> env) {
        Expression.Visitor<Expression> c = Expressions.CONSTRUCTOR;
        return input.accept(new Expression.Visitor<Expression>() {
            @Override
            public Expression constant(Primitive value) {
                return c.constant(value);
            }

            @Override
            public Expression symbol(String name) {
                return env.containsKey(name) ? c.constant(env.get(name)) : c.symbol(name);
            }

            @Override
            public Expression lambda(Expression var, Expression exp) {
                return env.containsKey(var.toString()) ?
                        c.lambda(var, exp) : c.lambda(var.accept(this), exp.accept(this));
            }

            @Override
            public Expression application(Expression fun, Expression arg) {
                return c.application(fun.accept(this), arg.accept(this));
            }
        });
    }
}