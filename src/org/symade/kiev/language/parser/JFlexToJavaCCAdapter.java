package org.symade.kiev.language.parser;

import kiev.parser.Token;
import kiev.parser.TokenManager;

public class JFlexToJavaCCAdapter implements TokenManager {

    private final KievLexer lexer;
    private boolean failed;

    public JFlexToJavaCCAdapter(char[] file_chars, int start, int length) {
        this.lexer = new KievLexer(null);
        //String content = new String(file_chars, start, length);
        CharSequence content = java.nio.CharBuffer.wrap(file_chars, start, length);
        this.lexer.reset(content, 0, length, 0);
    }

    @Override
    public Token getNextToken() {
        int beginLine = lexer.getLineNo();
        int beginColumn  = lexer.getColumn();
        Token special = null;
        KievTokenType tokenType = KievTokenTypes.EOF;
        while (!failed) {
            try {
                tokenType = lexer.advance();
                if (tokenType == null)
                    tokenType = KievTokenTypes.EOF;
                Token t = new Token();
                t.kind = tokenType.jjKind;
                t.image = tokenType.regex ? lexer.yytext().toString() : tokenType.text;
                t.beginLine = beginLine;
                t.beginColumn = beginColumn;
                t.endLine = lexer.getLineNo();
                t.endColumn = lexer.getColumn();
                t.specialToken = special;
                if (tokenType == KievTokenTypes.WHITE_SPACE || tokenType == KievTokenTypes.LINE_COMMENT || tokenType == KievTokenTypes.BLOCK_COMMENT || tokenType == KievTokenTypes.DOC_COMMENT) {
                    beginLine = lexer.getLineNo();
                    beginColumn = lexer.getColumn();
                    special = t;
                    continue;
                }
                return t;
            } catch (Exception e) {
                failed = true;
                break;
            }
        }
        Token t = new Token();
        t.kind = tokenType.jjKind;
        t.image = tokenType.regex ? lexer.yytext().toString() : tokenType.text;
        t.beginLine = beginLine;
        t.beginColumn = beginColumn;
        t.endLine = lexer.getLineNo();
        t.endColumn = lexer.getColumn();
        t.specialToken = special;
        return t;
    }
}
