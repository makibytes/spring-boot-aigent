package de.makibytes.benchmark.tinylang;

import java.util.List;

public class TinyLangFunction {
    private final List<Token> params;
    private final ASTNode body;
    private final Environment closure;

    public TinyLangFunction(List<Token> params, ASTNode body, Environment closure) {
        this.params = params;
        this.body = body;
        this.closure = closure;
    }

    public int arity() {
        return params.size();
    }

    public Object call(Evaluator evaluator, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).lexeme, arguments.get(i));
        }
        return evaluator.eval(body, environment);
    }
    
    @Override
    public String toString() {
        return "<fn>";
    }
}
