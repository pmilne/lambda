package lambda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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

    public static class Parser extends TokenVisitor<TokenVisitor> {
        @Override
        public Parser whiteSpace(String s) {
            return this;
        }

        public Parser error(String s) {
            throw new RuntimeException("Syntax error: " + s);
        }

        @Override
        public Parser lambda(String s) { // in Scheme, "lambda" may also be used as a symbol
            return symbol(s);
        }

        @Override
        public Parser symbol(String s) {
            return error(s);
        }

        @Override
        public Parser number(String s) {
            return error(s);
        }

        @Override
        public Parser lParen(String s) {
            return error(s);
        }

        @Override
        public Parser rParen(String s) {
            return error(s);
        }
    }

    private static interface Reduction {
        public Parser reduce(Expression e);
    }

    public class ApplicationParser extends Parser {
        public final Reduction reduction;

        public ApplicationParser(Reduction reduction) {
            this.reduction = reduction;
        }

        @Override
        public Parser lParen(String s) {
            return funParser(e -> e != null ? reduction.reduce(e) : this); // treat () as whitespace (!)
        }

        @Override
        public Parser symbol(String s) {
            return reduction.reduce(constructor.symbol(s));
        }

        @Override
        public Parser number(String s) {
            return reduction.reduce(constructor.constant(Integer.parseInt(s)));
        }
    }

    public Parser funParser(Reduction closer) {
        return new ApplicationParser(e -> argParser(e, closer)) {
            @Override
            public Parser rParen(String s) {
                return closer.reduce(null);
            }

            @Override
            public Parser lambda(String name) {
                return lambdaParser(closer);
            }
        };
    }

    public Parser argParser(Expression exp, Reduction closer) {
        return new ApplicationParser(e -> argParser(Expressions.CONSTRUCTOR.application(exp, e), closer)) {
            @Override
            public Parser rParen(String s) {
                return closer.reduce(exp);
            }
        };
    }

    private Parser lambdaParser(Reduction closer) {
        return new Parser() {
            @Override
            public Parser lParen(String s) {
                return this;
            }

            @Override
            public Parser symbol(String s) { // create nested lambdas using right-association
                return lambdaParser(exp -> closer.reduce(constructor.lambda(constructor.symbol(s), exp)));
            }

            @Override
            public Parser rParen(String s) {
                return funParser(closer);
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
        lex(input, new ApplicationParser(new Reduction() { // don't seem to be able to use a lambda here
            @Override
            public Parser reduce(Expression e) {
                processor.process(e);
                return new ApplicationParser(this);
            }
        }));
    }
}