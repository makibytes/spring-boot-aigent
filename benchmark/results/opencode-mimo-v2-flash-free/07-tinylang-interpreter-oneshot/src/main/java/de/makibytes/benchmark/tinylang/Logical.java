package de.makibytes.benchmark.tinylang;

public class Logical extends ASTNode {
    public final ASTNode left;
    public final Token operator;
    public final ASTNode right;

    public Logical(ASTNode left, Token operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLogicalExpr(this);
    }
}
