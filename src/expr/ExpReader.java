package expr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lambda.Expression;

/**
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
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

    private interface VisitorMethod {
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

    private static Parser ERROR = new Parser() {
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

    private static abstract class DelegatingParser0 extends Parser {
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

    private static class DelegatingParser extends DelegatingParser0 {
        public final Parser delegate;

        public DelegatingParser(Parser delegate) {
            this.delegate = delegate;
        }

        @Override
        public Parser getDelegate() {
            return delegate;
        }

        @Override
        public Parser whiteSpace(String s) {
            return this;
        }
    }

    private static interface Reduction {
        public Parser reduce(Expression e);
    }

    private static abstract class ParserBase extends DelegatingParser0 {
        @Override
        public Parser whiteSpace(String s) {
            return this;
        }
    }

    private static class Parser0 extends ParserBase {
        public final Reduction outer;
        private Parser delegate;

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

    private static class Parser1 extends ParserBase {
        public final Reduction outer;
        public final Expression exp;
        private Parser delegate;

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

    public Parser atomParser(Parser fail, Reduction success) {
        return new DelegatingParser(fail) {
            @Override
            public Parser number(String s) {
                return success.reduce(constructor.constant(Integer.parseInt(s)));
            }

            @Override
            public Parser symbol(String s) {
                return success.reduce(constructor.symbol(s));
            }
        };
    }

    public Parser atomParser(Reduction outer) {
        return atomParser(outer.reduce(null), outer);
    }

    public Parser implicitFunctionApplicationParser(Reduction outer) {
        return atomParser(new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return atomParser(outer.reduce(arg1), e -> reduce(constructor.application(arg1, e)));
            }
        });
    }

    @SuppressWarnings("Convert2Lambda")
    public Parser termParser(Reduction outer) {
        return implicitFunctionApplicationParser(new Reduction() {
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

    private static interface ParserFactory1 {
        Parser create(Reduction success);
    }

    private static interface ParserFactory2 {
        Parser create(Parser fail, Reduction success);
    }

    public Parser operatorParser(Reduction outer, ParserFactory1 domainParser, ParserFactory2 opParser) {
        return domainParser.create(new Reduction() {
            @Override
            public Parser reduce(Expression arg1) { // note this is called recursively from the closure
                return opParser.create(outer.reduce(arg1),
                        op -> domainParser.create(
                                arg2 -> reduce(constructor.application(constructor.application(op, arg1), arg2))));
            }
        });
    }

    public Parser productParser(Reduction outer) {
        return operatorParser(outer, this::termParser, (fail, success) -> new DelegatingParser(fail) {
            @Override
            public Parser prodOp(String s) {
                return success.reduce(constructor.symbol(s));
            }
        });
    }

    public Parser sumParser(Reduction outer) {
        return operatorParser(outer, this::productParser, (fail, success) -> new DelegatingParser(fail) {
            @Override
            public Parser sumOp(String s) {
                return success.reduce(constructor.symbol(s));
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