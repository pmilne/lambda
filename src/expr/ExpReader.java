package expr;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lambda.Expression;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class ExpReader {
    public static final Function<Integer, Function<Integer, Integer>> SUM = new Function<Integer, Function<Integer, Integer>>() {
        @Override
        public Function<Integer, Integer> apply(Integer x) {
            return y -> x + y;
        }

        @Override
        public String toString() {
            return "+";
        }
    };

    public static final Function<Integer, Function<Integer, Integer>> PRD = new Function<Integer, Function<Integer, Integer>>() {
        @Override
        public Function<Integer, Integer> apply(Integer x) {
            return y -> x * y;
        }

        @Override
        public String toString() {
            return "*";
        }
    };

    public final Expression.Visitor<Expression> constructor;

    public ExpReader(Expression.Visitor<Expression> constructor) {
        this.constructor = constructor;
    }

    public static class TokenType {
        public final String pattern;
        public final VisitorMethod method;

        private TokenType(String pattern, VisitorMethod method) {
            this.pattern = pattern;
            this.method = method;
        }

        //    http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
        public static TokenType[] ALL = {
                new TokenType("-?\\d+", Parser::number),
                new TokenType("\\(", Parser::lParen),
                new TokenType("\\)", Parser::rParen),
                new TokenType("\\;", Parser::semicolon),
                new TokenType("\\+", Parser::sumOp),
                new TokenType("\\*", Parser::prodOp),
                new TokenType("[a-zA-Z_]\\w*", Parser::symbol),
                new TokenType("\\s+", Parser::whiteSpace)
        };
    }

    private static String getRegex() {
        StringBuilder buffer = new StringBuilder();
        for (TokenType tokenType : TokenType.ALL)
            buffer.append(String.format("|(%s)", tokenType.pattern));
        return buffer.substring(1);
    }

    public interface VisitorMethod {
        public Parser call(Parser t, String s);
    }

    @SuppressWarnings("unused")
    public static abstract class TokenVisitor<T> {
        public abstract T lParen(String s);

        public abstract T rParen(String s);

        public abstract T semicolon(String s);

        public abstract T number(String s);

        public abstract T sumOp(String s);

        public abstract T prodOp(String s);

        public abstract T symbol(String s);

        public abstract T whiteSpace(String s);
    }

    public static abstract class Parser extends TokenVisitor<Parser> {
    }

    public static Parser ERROR = new Parser() {
        private Parser error(String s) {
            throw new RuntimeException("Syntax error: " + s);
        }

        @Override
        public Parser lParen(String s) {
            return error(s);
        }

        @Override
        public Parser rParen(String s) {
            return error(s);
        }

        @Override
        public Parser semicolon(String s) {
            return error(s);
        }

        @Override
        public Parser number(String s) {
            return error(s);
        }

        @Override
        public Parser sumOp(String s) {
            return error(s);
        }

        @Override
        public Parser prodOp(String s) {
            return error(s);
        }

        @Override
        public Parser symbol(String s) {
            return error(s);
        }

        @Override
        public Parser whiteSpace(String s) {
            return error(s);
        }
    };

    public static abstract class DelegatingParser extends Parser {
        //        @Override
//        public Parser whiteSpace(String s) {
//            return getDelegate().whiteSpace(s);
//        }
        public abstract Parser getDelegate();

        @Override
        public Parser whiteSpace(String s) {
            return this;
        }

        @Override
        public Parser symbol(String s) {
            return getDelegate().symbol(s);
        }

        @Override
        public Parser number(String s) {
            return getDelegate().number(s);
        }

        @Override
        public Parser lParen(String s) {
            return getDelegate().lParen(s);
        }

        @Override
        public Parser rParen(String s) {
            return getDelegate().rParen(s);
        }

        @Override
        public Parser semicolon(String s) {
            return getDelegate().semicolon(s);
        }

        @Override
        public Parser sumOp(String s) {
            return getDelegate().sumOp(s);
        }

        @Override
        public Parser prodOp(String s) {
            return getDelegate().prodOp(s);
        }
    }

    private static interface Reduction {
        public Parser reduce(Expression e);
    }

    public static class Parser0 extends DelegatingParser {
        public final Reduction closer;

        public Parser0(Reduction closer) {
            this.closer = closer;
        }

        public Parser getDelegate() {
            return closer.reduce(null);
        }
    }

    public static class Parser1 extends DelegatingParser {
        public final Expression exp;
        public final Reduction closer;

        public Parser1(Reduction closer, Expression exp) {
            this.exp = exp;
            this.closer = closer;
        }

        public Parser getDelegate() {
            return closer.reduce(exp);
        }
    }

    public Parser sum0Parser(Reduction closer) {
        return new Parser0(closer) {
            @Override
            public Parser number(String s) {
                Expression arg1 = constructor.constant(Integer.parseInt(s));
                return sum1Parser(closer).reduce(arg1);
            }
        };
    }

    public Reduction sum1Parser(Reduction closer) {
        return new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return new Parser1(closer, arg1) {
                    @Override
                    public Parser sumOp(String s) {
                        Expression op = constructor.constant(SUM);
                        Expression sum1 = constructor.application(op, arg1);
                        return new Parser0(closer) {
                            @Override
                            public Parser number(String s) {
                                Expression arg2 = constructor.constant(Integer.parseInt(s));
                                return prd1Parser(e -> reduce(constructor.application(sum1, e))).reduce(arg2);
                            }
                        };
                    }

                    @Override
                    public Parser prodOp(String s) {
                        Expression op = constructor.constant(PRD);
                        Expression prd1 = constructor.application(op, arg1);
                        return new Parser0(closer) {
                            @Override
                            public Parser number(String s) {
                                Expression arg2 = constructor.constant(Integer.parseInt(s));
                                return prd1Parser(e -> reduce(constructor.application(prd1, e))).reduce(arg2);
                            }
                        };
                    }
                };
            }
        };
    }

    public Reduction prd1Parser(Reduction closer) {
        return new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return new Parser1(closer, arg1) {
                    @Override
                    public Parser prodOp(String s) {
                        Expression op = constructor.constant(PRD);
                        Expression prd1 = constructor.application(op, arg1);
                        return new Parser0(closer) {
                            @Override
                            public Parser number(String s) {
                                Expression arg2 = constructor.constant(Integer.parseInt(s));
                                return reduce(constructor.application(prd1, arg2));
                            }
                        };
                    }
                };
            }
        };
    }

    public static void lex(CharSequence input, Parser parser) {
        Matcher matcher = Pattern.compile(getRegex()).matcher(input);
        while (matcher.find()) {
            TokenType[] types = TokenType.ALL;
            for (int i = 0; i < types.length; i++) {
                String s = matcher.group(i + 1); // group 0 is whole regexp
                if (s != null) {
//                    System.out.println("s = " + s);
                    parser = types[i].method.call(parser, s);
                    break;
                }
            }
        }
    }

    public static interface Processor<T> {
        void process(T e);
    }

    public void parse(CharSequence input, Processor<Expression> processor) {
        lex(input, sum0Parser(new Reduction() { // don't seem to be able to use a lambda here
            private Reduction that = this; // compiler bug?

            @Override
            public Parser reduce(Expression e) {
                processor.process(e);
                return new DelegatingParser() {
                    @Override
                    public Parser getDelegate() {
                        return ERROR;
                    }

                    @Override
                    public Parser semicolon(String s) {
                        return sum0Parser(that);
                    }
                };
            }
        }));
    }
}