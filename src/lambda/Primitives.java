package lambda;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
public class Primitives {
    public static final Primitive.Visitor<Primitive> CONSTRUCTOR =
            new Primitive.Visitor<Primitive>() {
                @Override
                public Primitive integer(int i) {
                    return new Primitive() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.integer(i);
                        }
                    };
                }

                @Override
                public Primitive string(String s) {
                    return new Primitive() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.string(s);
                        }
                    };
                }

                @Override
                public Primitive function(Function f) {
                    return new Primitive() {
                        @Override
                        public <T> T accept(Visitor<T> visitor) {
                            return visitor.function(f);
                        }
                    };
                }
            };

    public static final Primitive.Visitor<String> TO_STRING = new Primitive.Visitor<String>() {
        @Override
        public String integer(int i) {
            return Integer.toString(i);
        }

        @Override
        public String string(String s) {
            return s;
        }

        @Override
        public String function(Function m) {
            return m.toString();
        }
    };

    public static final Primitive.Visitor<Function> TO_FUNCTION = new Primitive.Visitor<Function>() {
        @Override
        public Function integer(int i) {
            throw new NotImplementedException();
        }

        @Override
        public Function string(String s) {
            throw new NotImplementedException();
        }

        @Override
        public Function function(Function m) {
            return m;
        }
    };

    public static final Primitive.Visitor<Integer> TO_INT = new Primitive.Visitor<Integer>() {
        @Override
        public Integer integer(int i) {
            return i;
        }

        @Override
        public Integer string(String s) {
            throw new NotImplementedException();
        }

        @Override
        public Integer function(Function m) {
            throw new NotImplementedException();
        }
    };

    public static int toInt(Primitive p) {
        return p.accept(TO_INT);
    }

    public static Function toFunction(Primitive p) {
        return p.accept(TO_FUNCTION);
    }

    public static Primitive toPrimitive(int i) {
        return CONSTRUCTOR.integer(i);
    }

    public static Primitive toPrimitive(Function m) {
        return CONSTRUCTOR.function(m);
    }

}