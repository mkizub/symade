package org.symade.kiev.language.parser;

public class KievTokenType {
    public final String  text;
    public final int     jjKind;
    public final boolean regex;

    public KievTokenType(String text, int jjKind) {
        this.text = text;
        this.jjKind = jjKind;
        this.regex = false;
    }

    public KievTokenType(String text, int jjKind, boolean regex) {
        this.text = text;
        this.jjKind = jjKind;
        this.regex = regex;
    }
}
