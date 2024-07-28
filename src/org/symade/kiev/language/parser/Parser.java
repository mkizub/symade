package org.symade.kiev.language.parser;
import syntax kiev.Syntax;

import kiev.Kiev;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import java.io.Reader;

public final class Parser {

    public Env				curEnv;
    public FileUnit			curFileUnit;
    public boolean			interface_only = false;

    public Parser(char[] file_chars, int start, int length, Env env) {
        super(new JFlexToJavaCCAdapter(file_chars, start, length));
        this.curEnv = env;
    }


    FileUnit FileUnit(String filename) {
        SymbolRef pkg;
        FileUnit fu = FileUnit.makeFile(filename, curEnv.proj, false);
        curFileUnit = fu;
        ASTModifiers modifiers;
        try {
            (
                    LOOKAHEAD({ getToken(1).kind==IMPORT && getToken(2).kind==IDENTIFIER && getToken(2).image.equals("syntax") })
            fu.syntaxes += ImportSyntax()
                    |
                    pkg = Package()
            {
                if (fu.srpkg.name == null) {
                    fu.srpkg.symbol = pkg.symbol;
                    fu.srpkg.pos = pkg.pos;
                } else
                    Kiev.reportError(pkg,"Duplicate package declaration "+pkg);
            }
		)*
            TopLevelDeclarations(fu)
            { fu.line_count = getToken(0).endLine; }
		<EOF>
        }
        catch(ParseError e) { rpe("Bad declaration",e); }
        catch(Throwable e) { rperr(e); }
        finally {
            curFileUnit = oldFileUnit;
            declMode = true;
            return fu;
        }
    }

	private boolean FileUnit_PackageOrSyntax() {
		get
	}
}
