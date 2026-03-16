package de.makibytes.benchmark.tinylang;

public class Grouping extends ASTNode {
    public final ASTNode expression;

    public Grouping(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitGroupingExpr(this);
    }
}
