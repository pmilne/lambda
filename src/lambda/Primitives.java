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

    // Java's Math.pow pow function does not overflow correctly if its result is coerced to an int.
    public static int pow(int x, int n) {
        int result = 1;
        while (n != 0) {
            if (n % 2 != 0) {
                n = n - 1;
                result = result * x;
            }
            n = n >>> 1;
            x = x * x;
        }
        return result;
    }

    public static final Primitive.Visitor<Function> TO_FUNCTION = new Primitive.Visitor<Function>() {
        @Override
        public Function integer(int n1) {
            return arg2 -> arg2.accept(new Primitive.Visitor<Primitive>() {
                @Override
                public Primitive integer(int n2) {
                    return primitive(pow(n2, n1));
                }

                @Override
                public Primitive string(String s2) {
                    throw new NotImplementedException();
                }

                @Override
                public Primitive function(Function f2) {
                    return primitive(x -> {
                        Primitive result = x;
                        for (int i = 0; i < n1; i++) {
                            result = f2.apply(result);
                        }
                        return result;
                    });
                }
            });
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

    // constructors

    public static Primitive primitive(int i) {
        return CONSTRUCTOR.integer(i);
    }

    public static Primitive primitive(Function m) {
        return CONSTRUCTOR.function(m);
    }

    // accessors

    public static int toInt(Primitive p) {
        return p.accept(TO_INT);
    }

    public static Function toFunction(Primitive p) {
        return p.accept(TO_FUNCTION);
    }

}