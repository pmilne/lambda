package expr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lambda.Expression;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class ExpReader {
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

    public static abstract class DelegatingParser0 extends Parser {
        public abstract Parser getDelegate();

        @Override
        public Parser whiteSpace(String s) {
            return getDelegate().whiteSpace(s);
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

    public static class DelegatingParser extends DelegatingParser0 {
        public final Parser delegate;

        public DelegatingParser(Parser delegate) {
            this.delegate = delegate;
        }

        @Override
        public Parser getDelegate() {
            return delegate;
        }
    }

    private static interface Reduction {
        public Parser reduce(Expression e);
    }

    public static abstract class ParserBase extends DelegatingParser0 {
        @Override
        public Parser whiteSpace(String s) {
            return this;
        }
    }

    public static class Parser0 extends ParserBase {
        public final Reduction outer;
        public Parser delegate;

        public Parser0(Reduction outer) {
            this.outer = outer;
        }

        public Parser getDelegate() {
            if (delegate == null) {
                delegate = outer.reduce(null);
            }
            return delegate;
        }
    }

    public static class Parser1 extends ParserBase {
        public final Reduction outer;
        public final Expression exp;
        public Parser delegate;

        public Parser1(Reduction outer, Expression exp) {
            this.exp = exp;
            this.outer = outer;
        }

        public Parser getDelegate() {
            if (delegate == null) {
                delegate = outer.reduce(exp);
            }
            return delegate;
        }
    }

    public Parser atomParser(Reduction outer) {
        return new Parser0(outer) {
            @Override
            public Parser number(String s) {
                return outer.reduce(constructor.constant(Integer.parseInt(s)));
            }

            @Override
            public Parser symbol(String s) {
                return outer.reduce(constructor.symbol(s));
            }
        };
    }

    public Parser applicationParser(Reduction outer) {
        return atomParser(new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return new Parser1(outer, arg1) {
                    @Override
                    public Parser number(String s) {
                        return reduce(constructor.application(arg1, constructor.constant(Integer.parseInt(s))));
                    }

                    @Override
                    public Parser symbol(String s) {
                        return reduce(constructor.application(arg1, constructor.symbol(s)));
                    }
                };
            }
        });
    }

    @SuppressWarnings("Convert2Lambda")
    public Parser termParser(Reduction outer) {
        return applicationParser(new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return new Parser1(outer, arg1) {
                    @Override
                    public Parser lParen(String s) {
                        return sumParser(e -> new Parser0(outer) {
                            @Override
                            public Parser rParen(String s) {
                                return outer.reduce(e);
                            }
                        });
                    }
                };
            }
        });
    }

    private Parser productParser(Reduction outer) {
        return termParser(new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return new Parser1(outer, arg1) {
                    @Override
                    public Parser prodOp(String s) {
                        Expression prd1 = constructor.application(constructor.symbol(s), arg1);
                        return termParser(e -> reduce(constructor.application(prd1, e)));
                    }
                };
            }
        });
    }

    public Parser sumParser(Reduction outer) {
        return productParser(new Reduction() {
            @Override
            public Parser reduce(Expression arg1) { // todo eliminate recursive call from below (?)
                return new Parser1(outer, arg1) {
                    @Override
                    public Parser sumOp(String s) {
                        Expression sum1 = constructor.application(constructor.symbol(s), arg1);
                        return productParser(e -> reduce(constructor.application(sum1, e)));
                    }
                };
            }
        });
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
        lex(input, sumParser(new Reduction() {
            private Reduction that = this; // don't seem to be able to inline here -- compiler bug?

            @Override
            public Parser reduce(Expression e) {
                return new DelegatingParser(ERROR) {
                    @Override
                    public Parser semicolon(String s) {
                        processor.process(e);
                        return sumParser(that);
                    }
                };
            }
        }));
    }
}