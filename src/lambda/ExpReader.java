package lambda;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static class Parser extends TokenVisitor<Parser> {
        @Override
        public Parser whiteSpace(String s) {
            return this;
        }

        public Parser error(String s) {
            throw new RuntimeException("Syntax error: " + s);
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

        @Override
        public Parser semicolon(String s) {
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
    }

    private static interface Reduction {
        public Parser reduce(Expression e);
    }

    public Parser sum0Parser(Reduction closer) {
        return new Parser() {
            @Override
            public Parser number(String s) {
                Expression arg1 = constructor.constant(Integer.parseInt(s));
                return sum1Parser(arg1, closer);
            }

            @Override
            public Parser semicolon(String s) {
                return this;
            }
        };
    }

    public Parser sum1Parser(Expression arg1, Reduction closer) {
        return new Parser() {
            @Override
            public Parser sumOp(String s) {
                Expression op = constructor.constant(SUM);
                Expression sum1 = constructor.application(op, arg1);
                return new Parser() {
                    @Override
                    public Parser number(String s) {
                        Expression arg2 = constructor.constant(Integer.parseInt(s));
                        return prd1Parser(arg2, e -> sum1Parser(constructor.application(sum1, e), closer));
                    }
                };
            }

            @Override
            public Parser prodOp(String s) {
                Expression op = constructor.constant(PRD);
                Expression prd1 = constructor.application(op, arg1);
                return new Parser() {
                    @Override
                    public Parser number(String s) {
                        Expression arg2 = constructor.constant(Integer.parseInt(s));
                        return prd1Parser(arg2, e -> sum1Parser(constructor.application(prd1, e), closer));
                    }
                };
            }

            @Override
            public Parser semicolon(String s) {
                return closer.reduce(arg1).semicolon(s);
            }
        };
    }

    public Parser prd1Parser(Expression arg1, Reduction closer) {
        return new Parser() {
            @Override
            public Parser prodOp(String s) {
                Expression op = constructor.constant(PRD);
                Expression prd1 = constructor.application(op, arg1);
                return new Parser() {
                    @Override
                    public Parser number(String s) {
                        Expression arg2 = constructor.constant(Integer.parseInt(s));
                        return prd1Parser(constructor.application(prd1, arg2), closer);
                    }
                };
            }

            @Override
            public Parser sumOp(String s) {
                return closer.reduce(arg1).sumOp(s);
            }

            @Override
            public Parser semicolon(String s) {
                return closer.reduce(arg1).semicolon(s);
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
            @Override
            public Parser reduce(Expression e) {
                processor.process(e);
                return sum0Parser(this);
            }
        }));
    }
}