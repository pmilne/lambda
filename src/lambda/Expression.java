package lambda;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public abstract class Expression {
    public abstract <S> S accept(Visitor<S> visitor);

    @Override
    public String toString() {
        return accept(Expressions.TO_STRING);
    }

    public static abstract class Visitor<T> {
        public abstract T constant(Object c);

        public abstract T symbol(String name);

        public abstract T lambda(Expression var, Expression exp);

        public abstract T application(Expression fun, Expression arg);
    }
}
