package lambda;

/**
 * @author pmilne
 */
public class Expressions {
    public static final Expression.Visitor<Expression> CONSTRUCTOR =
            new Expression.Visitor<Expression>() {
                @Override
                public Expression constant(final Object c) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.constant(c);
                        }
                    };
                }

                @Override
                public Expression symbol(final String name) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.symbol(name);
                        }
                    };
                }

                @Override
                public Expression application(final Expression fun, final Expression arg) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.application(fun, arg);
                        }
                    };
                }

                @Override
                public Expression lambda(final Expression var, final Expression exp) {
                    return new Expression() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.lambda(var, exp);
                        }
                    };
                }
            };

    public static final Expression.Visitor<String> TO_STRING = new Expression.Visitor<String>() {
        @Override
        public String constant(final Object c) {
            return c.toString();
        }

        @Override
        public String symbol(final String name) {
            return name;
        }

        @Override
        public String application(final Expression fun, final Expression arg) {
            return "(" + fun.accept(this) + " " + arg.accept(this) + ")";
        }

        @Override
        public String lambda(final Expression var, final Expression exp) {
            return "(lambda (" + var.accept(this) + ") " + exp.accept(this) + ")";
        }
    };
}