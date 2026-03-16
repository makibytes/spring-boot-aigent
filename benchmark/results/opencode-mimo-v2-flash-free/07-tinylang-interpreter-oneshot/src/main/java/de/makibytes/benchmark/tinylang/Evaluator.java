package de.makibytes.benchmark.tinylang;

import java.util.ArrayList;
import java.util.List;

public class Evaluator implements ASTVisitor<Object> {
    private Environment environment = new Environment();

    public Object eval(ASTNode node, Environment env) {
        this.environment = env;
        return node.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Literal node) {
        return node.value;
    }

    @Override
    public Object visitVariableExpr(Variable node) {
        return environment.get(node.name);
    }

    @Override
    public Object visitGroupingExpr(Grouping node) {
        return evaluate(node.expression);
    }

    @Override
    public Object visitUnaryExpr(Unary node) {
        Object right = evaluate(node.right);
        switch (node.operator.type) {
            case MINUS:
                checkNumberOperand(node.operator, right);
                if (right instanceof Long) return -(long) right;
                if (right instanceof Double) return -(double) right;
                throw new TinyLang.TinyLangException("Operand must be a number.");
            case BANG:
                return !isTruthy(right);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Binary node) {
        Object left = evaluate(node.left);
        Object right = evaluate(node.right);

        switch (node.operator.type) {
            case PLUS:
                if (left instanceof Long && right instanceof Long) return (long) left + (long) right;
                if (left instanceof Double && right instanceof Double) return (double) left + (double) right;
                if (left instanceof Long && right instanceof Double) return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                if (left instanceof Double && right instanceof Long) return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                if (left instanceof String && right instanceof String) return (String) left + (String) right;
                throw new TinyLang.TinyLangException("Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(node.operator, left, right);
                if (left instanceof Long && right instanceof Long) return (long) left - (long) right;
                return ((Number) left).doubleValue() - ((Number) right).doubleValue();
            case STAR:
                checkNumberOperands(node.operator, left, right);
                if (left instanceof Long && right instanceof Long) return (long) left * (long) right;
                return ((Number) left).doubleValue() * ((Number) right).doubleValue();
            case SLASH:
                checkNumberOperands(node.operator, left, right);
                if (left instanceof Long && right instanceof Long) {
                    if ((long) right == 0) throw new TinyLang.TinyLangException("Division by zero.");
                    return (long) left / (long) right;
                }
                return ((Number) left).doubleValue() / ((Number) right).doubleValue();
            case PERCENT:
                checkNumberOperands(node.operator, left, right);
                if (left instanceof Long && right instanceof Long) {
                    if ((long) right == 0) throw new TinyLang.TinyLangException("Modulo by zero.");
                    return (long) left % (long) right;
                }
                throw new TinyLang.TinyLangException("Modulo requires integer operands.");
            case CARET:
                checkNumberOperands(node.operator, left, right);
                if (left instanceof Long && right instanceof Long) {
                    long base = (long) left;
                    long exp = (long) right;
                    if (exp < 0) throw new TinyLang.TinyLangException("Negative exponent not allowed for integer base.");
                    return (long) Math.pow(base, exp);
                }
                return Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
            case GREATER:
                checkNumberOperands(node.operator, left, right);
                return ((Number) left).doubleValue() > ((Number) right).doubleValue();
            case GREATER_EQUAL:
                checkNumberOperands(node.operator, left, right);
                return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
            case LESS:
                checkNumberOperands(node.operator, left, right);
                return ((Number) left).doubleValue() < ((Number) right).doubleValue();
            case LESS_EQUAL:
                checkNumberOperands(node.operator, left, right);
                return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical node) {
        Object left = evaluate(node.left);
        if (node.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(node.right);
    }

    @Override
    public Object visitCallExpr(Call node) {
        Object callee = evaluate(node.callee);
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }

        if (callee instanceof TinyLangFunction) {
            TinyLangFunction function = (TinyLangFunction) callee;
            if (arguments.size() != function.arity()) {
                throw new TinyLang.TinyLangException("Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
            }
            return function.call(this, arguments);
        }
        throw new TinyLang.TinyLangException("Can only call functions.");
    }

    @Override
    public Object visitLetExpr(LetExpr node) {
        Object value = evaluate(node.value);
        Environment previous = environment;
        try {
            environment = new Environment(previous);
            environment.define(node.name.lexeme, value);
            return evaluate(node.body);
        } finally {
            environment = previous;
        }
    }

    @Override
    public Object visitLetRecExpr(LetRecExpr node) {
        Environment previous = environment;
        Environment letrecEnv = new Environment(previous);
        TinyLangFunction function = new TinyLangFunction(node.params, node.body, letrecEnv);
        letrecEnv.define(node.name.lexeme, function);
        try {
            environment = letrecEnv;
            return evaluate(node.in);
        } finally {
            environment = previous;
        }
    }

    @Override
    public Object visitIfExpr(IfExpr node) {
        if (isTruthy(evaluate(node.condition))) {
            return evaluate(node.thenBranch);
        } else {
            return evaluate(node.elseBranch);
        }
    }

    @Override
    public Object visitFnExpr(FnExpr node) {
        return new TinyLangFunction(node.params, node.body, environment);
    }

    // Helper methods
    private Object evaluate(ASTNode expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        // Handle numeric equality
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Long || operand instanceof Double) return;
        throw new TinyLang.TinyLangException("Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Long && right instanceof Long) return;
        if (left instanceof Double && right instanceof Double) return;
        if (left instanceof Long && right instanceof Double) return;
        if (left instanceof Double && right instanceof Long) return;
        throw new TinyLang.TinyLangException("Operands must be numbers.");
    }
}
