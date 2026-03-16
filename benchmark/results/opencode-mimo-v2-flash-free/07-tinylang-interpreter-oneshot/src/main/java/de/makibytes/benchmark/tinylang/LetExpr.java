package de.makibytes.benchmark.tinylang;

public class LetExpr extends ASTNode {
    public final Token name;
    public final ASTNode value;
    public final ASTNode body;

    public LetExpr(Token name, ASTNode value, ASTNode body) {
        this.name = name;
        this.value = value;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLetExpr(this);
    }
}
