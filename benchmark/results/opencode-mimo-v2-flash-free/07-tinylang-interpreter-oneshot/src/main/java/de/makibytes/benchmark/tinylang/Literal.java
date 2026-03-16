package de.makibytes.benchmark.tinylang;

public class Literal extends ASTNode {
    public final Object value;

    public Literal(Object value) {
        this.value = value;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLiteralExpr(this);
    }
}
