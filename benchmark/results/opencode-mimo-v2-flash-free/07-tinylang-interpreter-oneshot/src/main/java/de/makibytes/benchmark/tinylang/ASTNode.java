package de.makibytes.benchmark.tinylang;

public abstract class ASTNode {
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
