package lambda;

/**
 * @author pmilne
 */
public abstract class Expression {
    public abstract <T> T accept(Visitor<T> visitor);

    @Override
    public String toString() {
        return accept(Expressions.TO_STRING);
    }

    public static abstract class Visitor<T> {
        public abstract T constant(Primitive c);

        public abstract T symbol(String name);

        public abstract T lambda(String var, Expression exp);

        public abstract T application(Expression fun, Expression arg);
    }
}
