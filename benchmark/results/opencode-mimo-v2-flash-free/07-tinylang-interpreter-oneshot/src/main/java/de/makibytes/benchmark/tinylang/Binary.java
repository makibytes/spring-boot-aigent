package de.makibytes.benchmark.tinylang;

public class Binary extends ASTNode {
    public final ASTNode left;
    public final Token operator;
    public final ASTNode right;

    public Binary(ASTNode left, Token operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}
