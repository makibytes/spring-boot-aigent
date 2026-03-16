package de.makibytes.benchmark.tinylang;

public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, COMMA, MINUS, PLUS, STAR, SLASH, PERCENT, CARET,
    BANG, EQUAL, LESS, GREATER,

    // Two-character tokens.
    BANG_EQUAL, EQUAL_EQUAL, LESS_EQUAL, GREATER_EQUAL, ARROW, AND, OR,

    // Literals.
    IDENTIFIER, INTEGER, FLOAT, STRING,

    // Keywords.
    LET, LETREC, IN, FN, IF, THEN, ELSE, TRUE, FALSE,

    EOF
}
