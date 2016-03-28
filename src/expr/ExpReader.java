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

        public Parser0(Reduction outer) {
            this.outer = outer;
        }

        public Parser getDelegate() {
            return outer.reduce(null);
        }
    }

    public static class Parser1 extends ParserBase {
        public final Reduction outer;
        public final Expression exp;

        public Parser1(Reduction outer, Expression exp) {
            this.exp = exp;
            this.outer = outer;
        }

        public Parser getDelegate() {
            return outer.reduce(exp);
        }
    }

    public Parser parseNumber(Reduction fail, Reduction succeed) {
        return new Parser0(fail) {
            @Override
            public Parser number(String s) {
                return succeed.reduce(constructor.constant(Integer.parseInt(s)));
            }
        };
    }

    public Parser parseSum(Reduction outer) {
        return parseNumber(outer, parseSum1(outer));
    }

    public Reduction parseSum1(Reduction outer) {
        return new Reduction() {
            private Reduction inner = this; // doesn't seem to be possible to inline -- compiler bug?
            @Override
            public Parser reduce(Expression arg1) { // todo eliminate recursive call from below (?)
                return new Parser1(outer, arg1) {
                    @Override
                    public Parser sumOp(String s) {
                        Expression op = constructor.constant(SUM);
                        Expression sum1 = constructor.application(op, arg1);
                        return parseNumber(outer, parseProduct1(e -> reduce(constructor.application(sum1, e))));
                    }

                    @Override
                    public Parser prodOp(String s) {
                        return parseProduct1(inner).reduce(arg1).prodOp(s);
                    }
                };
            }
        };
    }

    public Reduction parseProduct1(Reduction outer) {
        return new Reduction() {
            @Override
            public Parser reduce(Expression arg1) {
                return new Parser1(outer, arg1) {
                    @Override
                    public Parser prodOp(String s) {
                        Expression op = constructor.constant(PRD);
                        Expression prd1 = constructor.application(op, arg1);
                        return new Parser0(outer) {
                            @Override
                            public Parser number(String s) {
                                Expression arg2 = constructor.constant(Integer.parseInt(s));
                                return reduce(constructor.application(prd1, arg2));
//                                return parseProduct1(e -> reduce(constructor.application(prd1, e))).reduce(arg2);
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
        lex(input, parseSum(new Reduction() { // don't seem to be able to use a lambda here
            private Reduction that = this; // compiler bug?

            @Override
            public Parser reduce(Expression e) {
                processor.process(e);
                return new DelegatingParser(ERROR) {
                    @Override
                    public Parser semicolon(String s) {
                        return parseSum(that);
                    }
                };
            }
        }));
    }
}