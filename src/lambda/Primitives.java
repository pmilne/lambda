package lambda;

import java.util.ArrayList;
import java.util.List;

import lisp.Reader;
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
                public Primitive function(PrimitiveFunction f) {
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
        public String function(PrimitiveFunction f) {
            return f.toString();
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

    private static PrimitiveFunction toFunction(int n1) {
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
            public Primitive function(PrimitiveFunction f2) {
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

    public static final Primitive.Visitor<PrimitiveFunction> TO_FUNCTION = new Primitive.Visitor<PrimitiveFunction>() {
        @Override
        public PrimitiveFunction integer(int n1) {
            return toFunction(n1);
        }

        @Override
        public PrimitiveFunction string(String s) {
            throw new NotImplementedException();
        }

        @Override
        public PrimitiveFunction function(PrimitiveFunction f) {
            return f;
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
        public Integer function(PrimitiveFunction f) {
            throw new NotImplementedException();
        }
    };

    public static Primitive[] read(String input) {
        List<Primitive> result = new ArrayList<>();
        new Reader(Expressions.CONSTRUCTOR).parse(input, exp -> result.add(Compiler.eval(exp)));
        return result.toArray(new Primitive[result.size()]);
    }

    // Constructors

    public static Primitive primitive(int i) {
        return CONSTRUCTOR.integer(i);
    }

    public static Primitive primitive(PrimitiveFunction m) {
        return CONSTRUCTOR.function(m);
    }

    // Accessors

    public static int toInt(Primitive p) {
        return p.accept(TO_INT);
    }

    public static PrimitiveFunction toFunction(Primitive p) {
        return p.accept(TO_FUNCTION);
    }

    // Some useful constants

    public static final Primitive INC = primitive(new PrimitiveFunction() {
        @Override
        public Primitive apply(Primitive x) {
            return primitive(toInt(x) + 1);
        }

        @Override
        public String toString() {
            return "inc";
        }
    });

    public static final Primitive SUM = primitive(new PrimitiveFunction() {
        @Override
        public Primitive apply(Primitive x) {
            return primitive(y -> primitive(toInt(x) + toInt(y)));
        }

        @Override
        public String toString() {
            return "+";
        }
    });

    public static final Primitive PRD = primitive(new PrimitiveFunction() {
        @Override
        public Primitive apply(Primitive x) {
            return primitive(y -> primitive(toInt(x) * toInt(y)));
        }

        @Override
        public String toString() {
            return "*";
        }
    });
}