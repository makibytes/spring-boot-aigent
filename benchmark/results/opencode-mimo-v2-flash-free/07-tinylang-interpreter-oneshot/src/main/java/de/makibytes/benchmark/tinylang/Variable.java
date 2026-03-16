package de.makibytes.benchmark.tinylang;

public class Variable extends ASTNode {
    public final Token name;

    public Variable(Token name) {
        this.name = name;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariableExpr(this);
    }
}
