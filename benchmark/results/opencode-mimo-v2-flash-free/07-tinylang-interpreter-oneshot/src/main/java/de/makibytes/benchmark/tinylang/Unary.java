package de.makibytes.benchmark.tinylang;

public class Unary extends ASTNode {
    public final Token operator;
    public final ASTNode right;

    public Unary(Token operator, ASTNode right) {
        this.operator = operator;
        this.right = right;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryExpr(this);
    }
}
