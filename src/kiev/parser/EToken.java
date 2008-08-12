/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.parser;
import syntax kiev.Syntax;

import java.util.regex.Pattern;

/**
 * EToken is an (unresolved) expression token used by ASTExpression in
 * flattened expressions. EToken has it's base kind to be either a generic
 * IDENTIFIER (as an identifier or a keyword), or a
 * TYPE_DECL (as a type reference), or an
 * OPERATOR (as a sequence of operator characters or a keyword), or a
 * EXPR_IDENT (null, true & false and others named constants).
 * EXPR_NUMBER (all kind of integer and float constants).
 * EXPR_STRING (string constant).
 * EXPR_CHAR (char constants).
 *
 * The EToken instance may have it's base type fixed (by editor) or auto-updated.
 * Those base types may have additional specifications. For example, the
 * EXPRESSION may have different types and formats. The format is stored as
 * an interned string.
 *
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public static enum ETokenKind { UNKNOWN, IDENTIFIER, TYPE_DECL, SCOPE_DECL, OPERATOR, EXPR_IDENT, EXPR_NUMBER, EXPR_STRING, EXPR_CHAR };
	
@ThisIsANode(name="EToken", lang=CoreLang)
public final class EToken extends ENode {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final Pattern patternIdent = Pattern.compile("[\\p{Alpha}_$][\\p{Alnum}_$]*");
	public static final Pattern patternOper = Pattern.compile("[\\!\\#\\%-\\/\\:\\;\\<\\=\\>\\?\\[\\\\\\]\\^\\{\\|\\}\\~\\u2190-\\u22F1]+");
	public static final Pattern patternIntConst = Pattern.compile("\\p{Digit}+");
	public static final Pattern patternFloatConst = Pattern.compile("\\p{Digit}+\\.\\p{Digit}*(?:[Ee][\\+\\-]?\\p{Digit}+)?");

	@nodeAttr public ETokenKind base_kind;
	@nodeData public ANode      value;
	@abstract
	@nodeAttr public boolean		explicit;		// if the base type if explicitly set
	
	@getter public final boolean get$explicit() { is_explicit }
	@setter public final void set$explicit(boolean val) { is_explicit = val; }

	// for GUI
	public final ETokenKind getKind() { base_kind }
	// for GUI
	public final void setKind(ETokenKind val) { base_kind = val; }
	
	
	public EToken() {}
	public EToken(Token t, ETokenKind kind) {
		set(t);
		this.base_kind = kind;
	}
	public EToken(int pos, String ident, ETokenKind kind, boolean explicit) {
		this.pos = pos;
		this.ident = ident;
		this.base_kind = kind;
		this.explicit = explicit;
	}
	public EToken(ConstExpr ce) {
		this.pos = ce.pos;
		if (ce instanceof ConstStringExpr) {
			this.base_kind = ETokenKind.EXPR_STRING;
			this.ident = ce.value;
		}
		else if (ce instanceof ConstCharExpr) {
			this.base_kind = ETokenKind.EXPR_CHAR;
			this.ident = String.valueOf((char)ce.value);
		}
		else if (ce instanceof ConstLongExpr || ce instanceof ConstIntExpr || ce instanceof ConstShortExpr || ce instanceof ConstByteExpr) {
			this.base_kind = ETokenKind.EXPR_NUMBER;
			this.ident = String.valueOf(ce);
		}
		else if (ce instanceof ConstDoubleExpr || ce instanceof ConstFloatExpr) {
			this.base_kind = ETokenKind.EXPR_NUMBER;
			this.ident = String.valueOf(ce);
		}
		else {
			this.base_kind = ETokenKind.EXPR_IDENT;
			this.ident = String.valueOf(ce);
		}
		this.explicit = true;
	}
	
	public String toString() {
		return this.ident;
	}

	public int getPriority() { return 256; }

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\"")) {
			this.ident = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
			this.explicit = true;
		} else {
			this.ident = t.image;
		}
	}
	
	public void setText(String text) {
		this.value = null; 
		this.ident = text;
		this.guessKind();
	}
	
	public boolean isIdentifier() {
		if (base_kind == ETokenKind.UNKNOWN)
			guessKind();
		return base_kind == ETokenKind.IDENTIFIER || base_kind == ETokenKind.TYPE_DECL || base_kind == ETokenKind.SCOPE_DECL;
	}

	public boolean isOperator()   {
		if ((base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.IDENTIFIER) && value == null)
			guessKind();
		return base_kind == ETokenKind.OPERATOR;
	}

	public TypeRef asType() {
		if ((base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.IDENTIFIER) && value == null)
			guessKind();
		if (base_kind == ETokenKind.TYPE_DECL && value instanceof TypeDecl)
			return new TypeNameRef(pos, ident, ((TypeDecl)value).xtype);
		return null;
	}
	
	public DNode asScope() {
		if ((base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.IDENTIFIER) && value == null)
			guessKind();
		if ((base_kind == ETokenKind.TYPE_DECL || base_kind == ETokenKind.SCOPE_DECL) && value instanceof DNode)
			return (DNode)value;
		return null;
	}
	
	public EToken asOperator() {
		if ((base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.IDENTIFIER) && value == null)
			guessKind();
		if (base_kind == ETokenKind.OPERATOR && value instanceof Opdef)
			return this;
		return null;
	}
	
	public void guessKind() {
		if (explicit)
			return;
		if (base_kind == ETokenKind.EXPR_STRING || base_kind == ETokenKind.EXPR_CHAR)
			return;
		if (value == null)
			value = NopExpr.dummyNode;
		String ident = this.ident;
		if (ident == null || ident == "") {
			if (base_kind != ETokenKind.UNKNOWN)
				this.base_kind = ETokenKind.UNKNOWN;
			return;
		}
		if (ident == "\"" || ident == "\"\"") {
			if (base_kind != ETokenKind.EXPR_STRING)
				this.base_kind = ETokenKind.EXPR_STRING;
			this.ident = "";
			return;
		}
		if (ident == "\'" || ident == "\'\'") {
			if (base_kind != ETokenKind.EXPR_CHAR)
				this.base_kind = ETokenKind.EXPR_CHAR;
			this.ident = "";
			return;
		}
		if (ident == Constants.nameThis) {
			if (this.base_kind != ETokenKind.IDENTIFIER)
				this.base_kind = ETokenKind.IDENTIFIER; // used for ThisExpr/SuperExpr and CtorCallExpr
			if!(this.value instanceof ThisExpr)
				this.value = new ThisExpr();
			return;
		}
		if (ident == Constants.nameSuper) {
			if (this.base_kind != ETokenKind.IDENTIFIER)
				this.base_kind = ETokenKind.IDENTIFIER; // used for ThisExpr/SuperExpr and CtorCallExpr
			if!(this.value instanceof SuperExpr)
				this.value = new SuperExpr();
			return;
		}
		if (ident == Constants.nameNull) {
			if (this.base_kind != ETokenKind.EXPR_IDENT)
				this.base_kind = ETokenKind.EXPR_IDENT;
			return;
		}
		if (ident == "true") {
			if (this.base_kind != ETokenKind.EXPR_IDENT)
				this.base_kind = ETokenKind.EXPR_IDENT;
			return;
		}
		if (ident == "false") {
			if (this.base_kind != ETokenKind.EXPR_IDENT)
				this.base_kind = ETokenKind.EXPR_IDENT;
			return;
		}
		if (patternIntConst.matcher(ident).matches()) {
			if (this.base_kind != ETokenKind.EXPR_NUMBER)
				this.base_kind = ETokenKind.EXPR_NUMBER;
			return;
		}
		if (patternFloatConst.matcher(ident).matches()) {
			if (this.base_kind != ETokenKind.EXPR_NUMBER)
				this.base_kind = ETokenKind.EXPR_NUMBER;
			return;
		}
		if (patternIdent.matcher(ident).matches())
			this.base_kind = ETokenKind.IDENTIFIER; // used for ThisExpr/SuperExpr and CtorCallExpr
		else if (patternOper.matcher(ident).matches())
			this.base_kind = ETokenKind.OPERATOR;
		// resolve in the path of scopes
		ResInfo info = new ResInfo(this,ident);
		if (PassInfo.resolveNameR(this,info)) {
			ISymbol isym = info.resolvedSymbol();
			if (isym instanceof OpdefSymbol) {
				this.base_kind = ETokenKind.OPERATOR;
				value = isym;
			}
			if (isym instanceof KievPackage) {
				this.base_kind = ETokenKind.SCOPE_DECL;
				value = isym;
			}
			if (isym instanceof TypeDecl) {
				this.base_kind = ETokenKind.TYPE_DECL;
				value = isym;
			}
		}
	}
	
	public boolean preResolveIn() {
		if (base_kind == ETokenKind.UNKNOWN)
			guessKind();
		if (base_kind == ETokenKind.EXPR_STRING)
			replaceWithNodeReWalk(new ConstStringExpr(ConstExpr.source2ascii(ident)));
		if (base_kind == ETokenKind.EXPR_CHAR) {
			if (ident.length() == 1)
				replaceWithNodeReWalk(new ConstCharExpr(ident.charAt(0)));
			replaceWithNodeReWalk(new ConstCharExpr(ConstExpr.source2ascii(ident).charAt(0)));
		}
		String ident = this.ident;
		if (ident == null || ident == "")
			throw new CompilerException(this,"Empty token");
//		if (ident == Constants.nameThis)
//			replaceWithNodeReWalk(new ThisExpr(pos));
//		if (ident == Constants.nameSuper)
//			replaceWithNodeReWalk(new SuperExpr(pos));
		if (ident == Constants.nameNull)
			replaceWithNodeReWalk(new ConstNullExpr());
		if (ident == "true")
			replaceWithNodeReWalk(new ConstBoolExpr(true));
		if (ident == "false")
			replaceWithNodeReWalk(new ConstBoolExpr(false));
		char first_ch = ident.charAt(0);
		int last_ch = ident.charAt(ident.length()-1);
		if (Character.isDigit(first_ch)) {
			int tokenKind = ParserConstants.INTEGER_LITERAL;
			if (last_ch == 'D' || last_ch == 'd')
				tokenKind = ParserConstants.DOUBLE_POINT_LITERAL;
			else if (last_ch == 'F' || last_ch == 'f')
				tokenKind = ParserConstants.FLOATING_POINT_LITERAL;
			else if (last_ch == 'L' || last_ch == 'l')
				tokenKind = ParserConstants.LONG_INTEGER_LITERAL;
			else if (patternFloatConst.matcher(ident).matches())
				tokenKind = ParserConstants.DOUBLE_POINT_LITERAL;
			Token t = Token.newToken(tokenKind);
			t.kind = tokenKind;
			t.image = ident;
			replaceWithNodeReWalk(ConstExpr.fromSource(t));
		}
		return true;
	}
	public boolean mainResolveIn() {
		String ident = this.ident;
		if (ident == null || ident == "")
			throw new CompilerException(this,"Empty token");
		if (ident == Constants.nameThis)
			replaceWithNodeReWalk(new ThisExpr(pos));
		if (ident == Constants.nameSuper)
			replaceWithNodeReWalk(new SuperExpr(pos));
//		else if( name == Constants.nameFILE ) {
//			FileUnit fu = ctx_file_unit;
//			if (fu != null && fu.fname != null)
//				replaceWithNode(new ConstStringExpr(fu.fname));
//			else
//				replaceWithNode(new ConstStringExpr("<no-file>"));
//			return false;
//		}
//		else if( name == Constants.nameLINENO ) {
//			replaceWithNode(new ConstIntExpr(pos>>>11));
//			return false;
//		}
//		else if( name == Constants.nameMETHOD ) {
//			Method m = ctx_method;
//			if( m instanceof Constructor )
//				replaceWithNode(new ConstStringExpr("<constructor>"));
//			else if (m != null && m.sname != null)
//				replaceWithNode(new ConstStringExpr(m.sname));
//			else
//				replaceWithNode(new ConstStringExpr("<no-method>"));
//			return false;
//		}
//		else if( name == Constants.nameDEBUG ) {
//			replaceWithNode(new ConstBoolExpr(Kiev.debugOutputA));
//			return false;
//		}

		// resolve in the path of scopes
		ResInfo info = new ResInfo(this,ident);
		if (!PassInfo.resolveNameR((ASTNode)this,info))
			throw new CompilerException(this,"Unresolved token "+ident);
		ISymbol isym = info.resolvedSymbol();
		if (isym instanceof OpdefSymbol) {
			this.is_token_operator = true;
			value = isym;
			//replaceWithNodeReWalk(op);
		}
		else if (isym instanceof TypeDecl) {
			this.is_token_type_decl = true;
			value = isym;
			TypeDecl td = (TypeDecl)isym;
			//td.checkResolved();
			replaceWithNodeReWalk(new TypeNameRef(pos, ident, td.xtype));
		}
		else {
			replaceWithNodeReWalk(info.buildAccess((ASTNode)this, null, info.resolvedSymbol()).closeBuild());
		}
		return false;
	}

}

