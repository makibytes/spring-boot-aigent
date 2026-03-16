package de.makibytes.benchmark.tinylang;

import java.util.List;

public class Call extends ASTNode {
    public final ASTNode callee;
    public final Token paren;
    public final List<ASTNode> arguments;

    public Call(ASTNode callee, Token paren, List<ASTNode> arguments) {
        this.callee = callee;
        this.paren = paren;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitCallExpr(this);
    }
}
