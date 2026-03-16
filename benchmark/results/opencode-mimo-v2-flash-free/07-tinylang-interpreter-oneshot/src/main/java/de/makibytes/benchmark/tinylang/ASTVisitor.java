package de.makibytes.benchmark.tinylang;

public interface ASTVisitor<T> {
    T visitBinaryExpr(Binary node);
    T visitUnaryExpr(Unary node);
    T visitLiteralExpr(Literal node);
    T visitVariableExpr(Variable node);
    T visitLogicalExpr(Logical node);
    T visitCallExpr(Call node);
    T visitGroupingExpr(Grouping node);
    
    T visitLetExpr(LetExpr node);
    T visitLetRecExpr(LetRecExpr node);
    T visitIfExpr(IfExpr node);
    T visitFnExpr(FnExpr node);
}
