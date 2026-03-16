package de.makibytes.benchmark.tinylang;

import java.util.List;

public class FnExpr extends ASTNode {
    public final List<Token> params;
    public final ASTNode body;

    public FnExpr(List<Token> params, ASTNode body) {
        this.params = params;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFnExpr(this);
    }
}
