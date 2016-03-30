package lisp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lambda.Expression;
import lambda.Primitives;

/**
 * A side-effect free parser of lambda expressions. The goal is to get this into a canonical form which
 * could be auto-generated from action-annotated BNF -- more simplification still needed.
 *
 * @author pmilne
 */
@SuppressWarnings("UnnecessaryInterfaceModifier, WeakerAccess")
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
                new TokenType("[a-zA-Z_]\\w*|\\*|\\+", Parser::symbol),
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
        public Parser lambda(String s) {
            return getDelegate().lambda(s);
        }

        @Override
        public Parser lParen(String s) {
            return getDelegate().lParen(s);
        }

        @Override
        public Parser rParen(String s) {
            return getDelegate().rParen(s);
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


    public Parser termParser(Reduction outer) {
        return new DefaultParser() {
            @Override
            public Parser lParen(String s) {
                return applicationParser(e -> new DefaultParser() {
                    @Override
                    public Parser rParen(String s) {
                        return outer.reduce(e);
                    }
                });
            }

            @Override
            public Parser number(String s) {
                return outer.reduce(constructor.constant(Primitives.CONSTRUCTOR.integer(Integer.parseInt(s))));
            }

            @Override
            public Parser symbol(String s) {
                return outer.reduce((constructor.symbol(s)));
            }
        };
    }

    private Expression consIfNecessary(Expression e1, Expression e2) {
        return e1 == null ? e2 : constructor.application(e1, e2);
    }

    private Parser lambdaParser(Reduction outer) {
        return new DefaultParser() {
            @Override
            public Parser lParen(String s) {
                return this;
            }

            @Override
            public Parser symbol(String s) { // create nested lambdas using right-association
                return lambdaParser(e -> outer.reduce(constructor.lambda(constructor.symbol(s), e)));
            }

            @Override
            public Parser rParen(String s) {
                return termParser(outer);
            }
        };
    }

    private Parser applicationParser(Reduction outer) {
        return new Reduction() {
            @Override
            public Parser reduce(Expression exp) {
                return new DelegatingParser(termParser(e -> reduce(consIfNecessary(exp, e)))) {
                    @Override
                    public Parser rParen(String s) {
                        return outer.reduce(exp).rParen(s);
                    }

                    @Override
                    public Parser lambda(String name) {
                        return lambdaParser(outer);
                    }
                };
            }
        }.reduce(null);
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
            @Override
            public Parser reduce(Expression e) {
                processor.process(e);
                return termParser(this);
            }
        }));
    }
}