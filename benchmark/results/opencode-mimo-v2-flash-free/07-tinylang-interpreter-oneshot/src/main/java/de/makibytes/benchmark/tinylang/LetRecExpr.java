package de.makibytes.benchmark.tinylang;

import java.util.List;

public class LetRecExpr extends ASTNode {
    public final Token name;
    public final List<Token> params;
    public final ASTNode body;
    public final ASTNode in;

    public LetRecExpr(Token name, List<Token> params, ASTNode body, ASTNode in) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.in = in;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLetRecExpr(this);
    }
}
