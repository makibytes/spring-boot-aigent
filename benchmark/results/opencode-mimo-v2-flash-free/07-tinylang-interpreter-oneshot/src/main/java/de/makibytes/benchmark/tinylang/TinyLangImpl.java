package de.makibytes.benchmark.tinylang;

public class TinyLangImpl implements TinyLang {

    @Override
    public Object eval(String source) {
        if (source == null) {
            throw new NullPointerException();
        }
        // 1. Lexing
        Lexer lexer = new Lexer(source);
        // 2. Parsing
        Parser parser = new Parser(lexer);
        ASTNode program = parser.parse();
        // 3. Evaluation
        Evaluator evaluator = new Evaluator();
        return evaluator.eval(program, new Environment());
    }
}
