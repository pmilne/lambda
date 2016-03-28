package lambda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A side-effect free parser of lambda expressions. The goal is to get this into a canonical form which
 * could be auto-generated from action-annotated BNF -- more simplification still needed.
 *
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public class Reader {
    public final Expression.Visitor<Expression> constructor;

    public Reader(Expression.Visitor<Expression> constructor) {
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
                new TokenType("lambda", Parser::lambda), // this has to come before 'symbol' below as that also matches.
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

        public abstract T number(String s);

        public abstract T lambda(String s);

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
        public Parser lambda(String s) {
            return error(s);
        }

        @Override
        public Parser number(String s) {
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

    public static class DelegatingParser extends Parser {
        public final Parser delegate;

        public DelegatingParser(Parser delegate) {
            this.delegate = delegate;
        }

        @Override
        public Parser whiteSpace(String s) {
            return delegate.whiteSpace(s);
        }

        @Override
        public Parser symbol(String s) {
            return delegate.symbol(s);
        }

        @Override
        public Parser number(String s) {
            return delegate.number(s);
        }

        @Override
        public Parser lambda(String s) {
            return delegate.lambda(s);
        }

        @Override
        public Parser lParen(String s) {
            return delegate.lParen(s);
        }

        @Override
        public Parser rParen(String s) {
            return delegate.rParen(s);
        }
    }

    public static class DefaultParser extends DelegatingParser {
        public DefaultParser() {
            super(ERROR);
        }

        @Override
        public Parser whiteSpace(String s) {
            return this;
        }
    }

    private static interface Reduction {
        public Parser reduce(Expression e);
    }

    public Parser termParser(Reduction closer) {
        return new DefaultParser() {
            @Override
            public Parser lParen(String s) {
                return applicationParser(null, e -> new DefaultParser() {
                    @Override
                    public Parser rParen(String s) {
                        return closer.reduce(e);
                    }
                });
            }

            @Override
            public Parser number(String s) {
                return closer.reduce(constructor.constant(Integer.parseInt(s)));
            }

            @Override
            public Parser symbol(String s) {
                return closer.reduce((constructor.symbol(s)));
            }
        };
    }

    public Parser applicationParser(Expression exp, Reduction closer) {
        return new DefaultParser() {
            private Expression consIfNotFirst(Expression e) {
                return exp == null ? e : constructor.application(exp, e);
            }

            private Parser absorb(Expression e) {
                return applicationParser(consIfNotFirst(e), closer);
            }

            @Override
            public Parser lParen(String s) {
                return termParser(e -> absorb(e)).lParen(s);
//                return closer.reduce(exp).lParen(s);
//                return termParser(closer).lParen(s);
//                return cons(termParser(closer));
            }

            @Override
            public Parser number(String s) {
                return absorb(constructor.constant(Integer.parseInt(s)));
            }

            @Override
            public Parser symbol(String s) {
                return absorb(constructor.symbol(s));
            }

            @Override
            public Parser rParen(String s) {
                return closer.reduce(exp).rParen(s);
            }

            @Override
            public Parser lambda(String name) {
                return lambdaParser(closer);
            }
        };
    }

    private Parser lambdaParser(Reduction closer) {
        return new DefaultParser() {
            @Override
            public Parser lParen(String s) {
                return this;
            }

            @Override
            public Parser symbol(String s) { // create nested lambdas using right-association
                return lambdaParser(e -> closer.reduce(constructor.lambda(constructor.symbol(s), e)));
            }

            @Override
            public Parser rParen(String s) {
                return termParser(closer);
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
        lex(input, termParser(new Reduction() {
            Reduction that = this;

            @Override
            public Parser reduce(Expression e) {
                processor.process(e);
                return termParser(that);
            }
        }));
    }
}