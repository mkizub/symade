package org.symade.kiev.language.parser;

public interface KievTokenTypes {

    // Generic
    KievTokenType EOF                   = new KievTokenType("EOF", TokenConstants.EOF, true);
    KievTokenType BAD_CHARACTER         = new KievTokenType("BAD_CHARACTER", -2, true);
    KievTokenType WHITE_SPACE           = new KievTokenType("WHITE_SPACE", -1, true);

    // Comments
    KievTokenType DOC_COMMENT           = new KievTokenType("DOC_COMMENT", TokenConstants.FORMAL_COMMENT, true);
    KievTokenType BLOCK_COMMENT         = new KievTokenType("BLOCK_COMMENT", TokenConstants.MULTI_LINE_COMMENT, true);
    KievTokenType LINE_COMMENT          = new KievTokenType("LINE_COMMENT", TokenConstants.SINGLE_LINE_COMMENT, true);

    // Literals
    KievTokenType CHARACTER_LITERAL     = new KievTokenType("CHARACTER_LITERAL", TokenConstants.CHARACTER_LITERAL, true);
    KievTokenType STRING_LITERAL        = new KievTokenType("STRING_LITERAL", TokenConstants.STRING_LITERAL, true);
    KievTokenType INTEGER_LITERAL       = new KievTokenType("INTEGER_LITERAL", TokenConstants.INTEGER_LITERAL, true);
    KievTokenType LONG_LITERAL          = new KievTokenType("LONG_LITERAL", TokenConstants.LONG_INTEGER_LITERAL, true);
    KievTokenType FLOAT_LITERAL         = new KievTokenType("FLOAT_LITERAL", TokenConstants.FLOATING_POINT_LITERAL, true);
    KievTokenType DOUBLE_LITERAL        = new KievTokenType("DOUBLE_LITERAL", TokenConstants.DOUBLE_POINT_LITERAL, true);

    // Access keywords
    KievTokenType META_ACCESS           = new KievTokenType("@access", TokenConstants.META_ACCESS1);
    KievTokenType META_PUBLIC           = new KievTokenType("@public", TokenConstants.META_PUBLIC);
    KievTokenType META_PROTECTED        = new KievTokenType("@protected", TokenConstants.META_PROTECTED);
    KievTokenType META_PRIVATE          = new KievTokenType("@private", TokenConstants.PRIVATE);
    KievTokenType KW_PUBLIC             = new KievTokenType("public", TokenConstants.PUBLIC);
    KievTokenType KW_PROTECTED          = new KievTokenType("protected", TokenConstants.PROTECTED);
    KievTokenType KW_PRIVATE            = new KievTokenType("private", TokenConstants.PRIVATE);
    KievTokenType KW_ACC_RO             = new KievTokenType("ro", TokenConstants.READ_ONLY);
    KievTokenType KW_ACC_WO             = new KievTokenType("wo", TokenConstants.WRITE_ONLY);
    KievTokenType KW_ACC_RW             = new KievTokenType("rw", TokenConstants.READ_WRITE);
    KievTokenType KW_ACC_NO             = new KievTokenType("no", TokenConstants.NO_READ_WRITE);

    // Soft keywords
    KievTokenType KW_SYNTAX             = new KievTokenType("syntax", TokenConstants.IMPORT);

    // Identifier keywords
    KievTokenType KW_THIS               = new KievTokenType("this", TokenConstants.KW_THIS);
    KievTokenType KW_SUPER              = new KievTokenType("super", TokenConstants.KW_SUPER);
    KievTokenType KW_TRUE               = new KievTokenType("true", TokenConstants.KW_TRUE);
    KievTokenType KW_FALSE              = new KievTokenType("false", TokenConstants.KW_FALSE);
    KievTokenType KW_NULL               = new KievTokenType("false", TokenConstants.KW_NULL);

    // Regexp tokens
    KievTokenType ESC_IDENTIFIER        = new KievTokenType("ESC_IDENTIFIER", TokenConstants.IDENTIFIER, true);
    KievTokenType IDENTIFIER            = new KievTokenType("IDENTIFIER", TokenConstants.IDENTIFIER, true);
    KievTokenType OPERATOR              = new KievTokenType("OPERATOR", TokenConstants.OPERATOR, true);

    // keywords
    KievTokenType KW_ABSTRACT           = new KievTokenType("abstract", TokenConstants.ABSTRACT);
    KievTokenType KW_ALIAS              = new KievTokenType("alias", TokenConstants.ALIAS);
    KievTokenType KW_BREAK              = new KievTokenType("break", TokenConstants.BREAK);
    KievTokenType KW_CASE               = new KievTokenType("case", TokenConstants.CASE);
    KievTokenType KW_CATCH              = new KievTokenType("catch", TokenConstants.CATCH);
    KievTokenType KW_CLASS              = new KievTokenType("class", TokenConstants.CLASS);
    KievTokenType KW_CONTINUE           = new KievTokenType("continue", TokenConstants.CONTINUE);
    KievTokenType KW_DEFAULT            = new KievTokenType("default", TokenConstants._DEFAULT);
    KievTokenType KW_DO                 = new KievTokenType("do", TokenConstants.DO);
    KievTokenType KW_ELSE               = new KievTokenType("else", TokenConstants.ELSE);
    KievTokenType KW_ENSURE             = new KievTokenType("ensure", TokenConstants.ENSURE);
    KievTokenType KW_ENUM               = new KievTokenType("enum", TokenConstants.ENUM);
    KievTokenType KW_EXTENDS            = new KievTokenType("extends", TokenConstants.EXTENDS);
    KievTokenType KW_FINAL              = new KievTokenType("final", TokenConstants.FINAL);
    KievTokenType KW_FINALLY            = new KievTokenType("finally", TokenConstants.FINALLY);
    KievTokenType KW_FOR                = new KievTokenType("for", TokenConstants.FOR);
    KievTokenType KW_FOREACH            = new KievTokenType("foreach", TokenConstants.FOREACH);
    KievTokenType KW_FUN                = new KievTokenType("fun", TokenConstants.FUNCTION);
    KievTokenType KW_GOTO               = new KievTokenType("goto", TokenConstants.GOTO);
    KievTokenType KW_IF                 = new KievTokenType("if", TokenConstants.IF);
    KievTokenType KW_IMPLEMENTS         = new KievTokenType("implements", TokenConstants.IMPLEMENTS);
    KievTokenType KW_IMPORT             = new KievTokenType("import", TokenConstants.IMPORT);
    KievTokenType KW_INTERFACE          = new KievTokenType("interface", TokenConstants.INTERFACE);
    KievTokenType KW_INVARIANT          = new KievTokenType("invariant", TokenConstants.INVARIANT);
    KievTokenType KW_NATIVE             = new KievTokenType("native", TokenConstants.NATIVE);
    KievTokenType KW_NEW                = new KievTokenType("new", TokenConstants.NEW);
    KievTokenType KW_OPERATOR           = new KievTokenType("operator", TokenConstants.OPERATOR_ID);
    KievTokenType KW_PACKAGE            = new KievTokenType("package", TokenConstants.PACKAGE);
    KievTokenType KW_PCUT               = new KievTokenType("$cut", TokenConstants.PCUT);
    KievTokenType KW_PRAGMA             = new KievTokenType("pragma", TokenConstants.PRAGMA);
    KievTokenType KW_REQUIRE            = new KievTokenType("require", TokenConstants.REQUIRE);
    KievTokenType KW_RETURN             = new KievTokenType("return", TokenConstants.RETURN);
    KievTokenType KW_STATIC             = new KievTokenType("static", TokenConstants.STATIC);
    KievTokenType KW_SWITCH             = new KievTokenType("switch", TokenConstants.SWITCH);
    KievTokenType KW_SYNCHRONIZED       = new KievTokenType("synchornized", TokenConstants.SYNCHRONIZED);
    KievTokenType KW_THROW              = new KievTokenType("throw", TokenConstants.THROW);
    KievTokenType KW_THROWS             = new KievTokenType("throws", TokenConstants.THROWS);
    KievTokenType KW_TRANSIENT          = new KievTokenType("transient", TokenConstants.TRANSIENT);
    KievTokenType KW_TRY                = new KievTokenType("try", TokenConstants.TRY);
    KievTokenType KW_TYPE               = new KievTokenType("type", TokenConstants.TYPE);
    KievTokenType KW_TYPEDEF            = new KievTokenType("typedef", TokenConstants.TYPEDEF);
    KievTokenType KW_VOLATILE           = new KievTokenType("volatile", TokenConstants.VOLATILE);
    KievTokenType KW_WHILE              = new KievTokenType("while", TokenConstants.WHILE);
    KievTokenType KW_WITH               = new KievTokenType("with", TokenConstants.WITH);

    // operators, delimiters, separators
    KievTokenType OP_ARROW              = new KievTokenType("->", TokenConstants.ARROW);
    KievTokenType OP_ASSIGN             = new KievTokenType("=", TokenConstants.ASSIGN);
    KievTokenType OP_AT                 = new KievTokenType("@", TokenConstants.OPERATOR_AT);
    KievTokenType OP_COLON              = new KievTokenType(":", TokenConstants.COLON);
    KievTokenType OP_COMMA              = new KievTokenType(",", TokenConstants.COMMA);
    KievTokenType OP_DOT                = new KievTokenType(".", TokenConstants.DOT);
//    KievTokenType OP_GE                 = new KievTokenType(">=", TokenConstants.GE);
    KievTokenType OP_GT                 = new KievTokenType(">", TokenConstants.GT);
    KievTokenType OP_IS_ONE_OF          = new KievTokenType("@=", TokenConstants.IS_ONE_OF);
    KievTokenType OP_IS_THE             = new KievTokenType("?=", TokenConstants.IS_THE);
    KievTokenType OP_LBRACE             = new KievTokenType("{", TokenConstants.LBRACE);
    KievTokenType OP_LBRACKET           = new KievTokenType("[", TokenConstants.LBRACKET);
//    KievTokenType OP_LE                 = new KievTokenType("<=", TokenConstants.LE);
    KievTokenType OP_LOWER_BOUND        = new KievTokenType("≥", TokenConstants.OPERATOR_LOWER_BOUND);
    KievTokenType OP_LPAREN             = new KievTokenType("(", TokenConstants.LPAREN);
    KievTokenType OP_LRBRACKETS         = new KievTokenType("[]", TokenConstants.OPERATOR_LRBRACKETS);
    KievTokenType OP_LT                 = new KievTokenType("<", TokenConstants.LT);
    KievTokenType OP_QUESTION           = new KievTokenType("?", TokenConstants.OP_QUESTION);
    KievTokenType OP_RBRACE             = new KievTokenType("}", TokenConstants.RBRACE);
    KievTokenType OP_RBRACKET           = new KievTokenType("]", TokenConstants.RBRACKET);
    KievTokenType OP_RPAREN             = new KievTokenType(")", TokenConstants.RPAREN);
    KievTokenType OP_SEMICOLON          = new KievTokenType(";", TokenConstants.SEMICOLON);
    KievTokenType OP_SHARP              = new KievTokenType("#", TokenConstants.OPERATOR_SHARP);
    KievTokenType OP_SUPERSCRIPT_MINUS  = new KievTokenType("⁻", TokenConstants.OPERATOR_SUPERSCRIPT_MINUS);
    KievTokenType OP_SUPERSCRIPT_PLUS   = new KievTokenType("⁺", TokenConstants.OPERATOR_SUPERSCRIPT_PLUS);
    KievTokenType OP_UPPER_BOUND        = new KievTokenType("≤", TokenConstants.OPERATOR_UPPER_BOUND);
    KievTokenType OP_VARARGS            = new KievTokenType("...", TokenConstants.VARARGS);
}
