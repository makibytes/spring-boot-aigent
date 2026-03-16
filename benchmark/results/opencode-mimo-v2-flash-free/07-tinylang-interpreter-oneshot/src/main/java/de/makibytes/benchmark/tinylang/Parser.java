package de.makibytes.benchmark.tinylang;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(Lexer lexer) {
        lexer.scanTokens();
        this.tokens = lexer.getTokens();
    }

    public ASTNode parse() {
        try {
            return parseExpression();
        } catch (TinyLang.TinyLangException e) {
            throw e;
        } catch (Exception e) {
            throw new TinyLang.TinyLangException("Parse error: " + e.getMessage());
        }
    }

    // parseExpression handles the top-level alternatives
    private ASTNode parseExpression() {
        if (match(TokenType.LET)) return letExpr();
        if (match(TokenType.LETREC)) return letrecExpr();
        if (match(TokenType.IF)) return ifExpr();
        if (match(TokenType.FN)) return fnExpr();
        return orExpr();
    }

    // or_expr ::= and_expr ('||' and_expr)*
    private ASTNode orExpr() {
        ASTNode expr = andExpr();
        while (match(TokenType.OR)) {
            Token operator = previous();
            ASTNode right = andExpr();
            expr = new Logical(expr, operator, right);
        }
        return expr;
    }

    // and_expr ::= not_expr ('&&' not_expr)*
    private ASTNode andExpr() {
        ASTNode expr = notExpr();
        while (match(TokenType.AND)) {
            Token operator = previous();
            ASTNode right = notExpr();
            expr = new Logical(expr, operator, right);
        }
        return expr;
    }

    // not_expr ::= '!' not_expr | cmp_expr
    private ASTNode notExpr() {
        if (match(TokenType.BANG)) {
            Token operator = previous();
            ASTNode right = notExpr();
            return new Unary(operator, right);
        }
        return cmpExpr();
    }

    // cmp_expr ::= add_expr (('==' | '!=' | '<' | '<=' | '>' | '>=') add_expr)?
    private ASTNode cmpExpr() {
        ASTNode expr = addExpr();
        if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            ASTNode right = addExpr();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    // add_expr ::= mul_expr (('+' | '-') mul_expr)*
    private ASTNode addExpr() {
        ASTNode expr = mulExpr();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            ASTNode right = mulExpr();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    // mul_expr ::= pow_expr (('*' | '/' | '%') pow_expr)*
    private ASTNode mulExpr() {
        ASTNode expr = powExpr();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token operator = previous();
            ASTNode right = powExpr();
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    // pow_expr ::= unary ('^' pow_expr)?     ← right-associative
    private ASTNode powExpr() {
        ASTNode expr = unary();
        if (match(TokenType.CARET)) {
            Token operator = previous();
            ASTNode right = powExpr(); // Right recursion for right-associativity
            expr = new Binary(expr, operator, right);
        }
        return expr;
    }

    // unary ::= '-' unary | call_expr
    private ASTNode unary() {
        if (match(TokenType.MINUS)) {
            Token operator = previous();
            ASTNode right = unary();
            return new Unary(operator, right);
        }
        return callExpr();
    }

    // call_expr ::= primary ('(' args ')')*
    private ASTNode callExpr() {
        ASTNode expr = primary();
        while (match(TokenType.LEFT_PAREN)) {
            List<ASTNode> args = arguments();
            Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
            expr = new Call(expr, paren, args);
        }
        return expr;
    }

    // primary ::= '(' expr ')' | INT | FLOAT | BOOL | STRING | IDENT
    private ASTNode primary() {
        if (match(TokenType.INTEGER)) return new Literal(previous().literal);
        if (match(TokenType.FLOAT)) return new Literal(previous().literal);
        if (match(TokenType.TRUE)) return new Literal(true);
        if (match(TokenType.FALSE)) return new Literal(false);
        if (match(TokenType.STRING)) return new Literal(previous().literal);
        if (match(TokenType.IDENTIFIER)) return new Variable(previous());
        if (match(TokenType.LEFT_PAREN)) {
            ASTNode expr = parseExpression(); // Recursive call to parse full expression
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    // params ::= IDENT (',' IDENT)*
    private List<Token> parameters() {
        List<Token> params = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (params.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 parameters.");
                }
                params.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        return params;
    }

    // args ::= (expr (',' expr)*)?
    private List<ASTNode> arguments() {
        List<ASTNode> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (args.size() >= 255) {
                    throw error(peek(), "Can't have more than 255 arguments.");
                }
                args.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        return args;
    }

    // let_expr ::= 'let' IDENT '=' expr 'in' expr
    private ASTNode letExpr() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        consume(TokenType.EQUAL, "Expect '=' after variable name.");
        ASTNode value = parseExpression();
        consume(TokenType.IN, "Expect 'in' after let binding.");
        ASTNode body = parseExpression();
        return new LetExpr(name, value, body);
    }

    // letrec_expr ::= 'letrec' IDENT '=' 'fn' '(' params ')' '=>' expr 'in' expr
    private ASTNode letrecExpr() {
        Token name = consume(TokenType.IDENTIFIER, "Expect function name.");
        consume(TokenType.EQUAL, "Expect '=' after function name.");
        consume(TokenType.FN, "Expect 'fn' keyword.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'fn'.");
        List<Token> params = parameters();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.ARROW, "Expect '=>' after parameters.");
        ASTNode body = parseExpression();
        consume(TokenType.IN, "Expect 'in' after letrec body.");
        ASTNode in = parseExpression();
        return new LetRecExpr(name, params, body, in);
    }

    // if_expr ::= 'if' expr 'then' expr 'else' expr
    private ASTNode ifExpr() {
        ASTNode condition = parseExpression();
        consume(TokenType.THEN, "Expect 'then' after if condition.");
        ASTNode thenBranch = parseExpression();
        consume(TokenType.ELSE, "Expect 'else' after then branch.");
        ASTNode elseBranch = parseExpression();
        return new IfExpr(condition, thenBranch, elseBranch);
    }

    // fn_expr ::= 'fn' '(' params ')' '=>' expr
    private ASTNode fnExpr() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'fn'.");
        List<Token> params = parameters();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.ARROW, "Expect '=>' after parameters.");
        ASTNode body = parseExpression();
        return new FnExpr(params, body);
    }

    // Helper methods
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private TinyLang.TinyLangException error(Token token, String message) {
        return new TinyLang.TinyLangException(" at '" + token.lexeme + "' " + message);
    }
}
