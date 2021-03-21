package lambda;

/**
 * @author pmilne
 */
@SuppressWarnings("WeakerAccess")
public abstract class Primitive {
    public abstract <T> T accept(Visitor<T> visitor);

    @Override
    public String toString() {
        return accept(Primitives.TO_STRING);
    }

    public static abstract class Visitor<T> {
        public abstract T integer(int i);

        public abstract T string(String s);

        public abstract T function(PrimitiveFunction f);
    }
}
