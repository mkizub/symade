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

import java.util.regex.Pattern;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

@node(name="EToken")
public final class EToken extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = EToken;
	
	public static final Pattern patternIdent = Pattern.compile("[\\p{Alpha}_$][\\p{Alnum}_$]*");
	public static final Pattern patternOper = Pattern.compile("[\\!\\#\\%\\-\\/\\:\\;\\<\\=\\>\\?\\[\\\\\\]\\^\\{\\|\\}\\~\\u2190-\\u22F1]+");
	public static final Pattern patternIntConst = Pattern.compile("\\p{Digit}+");
	public static final Pattern patternFloatConst = Pattern.compile("\\p{Digit}+\\.\\p{Digit}*(?:[Ee][\\+\\-]?\\p{Digit}+)?");

	public static final int IS_IDENTIFIER =  1;
	public static final int IS_OPERATOR   =  2;
	public static final int IS_KEYWORD    =  4;
	public static final int IS_CONSTANT   =  8;
	public static final int IS_TYPE_DECL  = 16;
	
	// temporary associated object, is
	// Var, Field, etc for identifiers
	// Opdef for operator
	// null for keywords (will be StatDef)
	// ConstExpr for constants
	// TypeDecl for types (usually identifiers)
	@UnVersioned
	private Object pre_resolved;

	@getter public boolean isIdentifier() { is_token_ident }
	@getter public boolean isOperator()   { is_token_operator }
	@getter public boolean isKeyword()    { is_token_keyword }
	@getter public boolean isConstant()   { is_token_constant }
	@getter public boolean isTypeDecl()   { is_token_type_decl }

	public EToken() {}
	public EToken(Token t, int is_kind) {
		set(t);
		setKind(is_kind);
	}
	public EToken(int pos, String ident, int is_kind) {
		this.pos = pos;
		this.ident = ident;
		setKind(is_kind);
	}
	
	private void setKind(int kind) {
		this.is_token_ident     = (kind & IS_IDENTIFIER) != 0;
		this.is_token_operator  = (kind & IS_OPERATOR) != 0;
		this.is_token_keyword   = (kind & IS_KEYWORD) != 0;
		this.is_token_constant  = (kind & IS_CONSTANT) != 0;
		this.is_token_type_decl = (kind & IS_TYPE_DECL) != 0;
	}
	
	public String toString() {
		return this.ident;
	}

	public int getPriority() { return 256; }

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.ident = t.image;
	}
	
	public TypeRef asType() {
		if (pre_resolved == null)
			guessKind();
		if (pre_resolved instanceof TypeDecl)
			return new TypeNameRef(pos, ident, ((TypeDecl)pre_resolved).xtype);
		return null;
	}
	
	public EToken asOperator() {
		if (pre_resolved == null)
			guessKind();
		if (pre_resolved instanceof Opdef)
			return this;
		return null;
	}
	
	public void guessKind() {
		pre_resolved = "";
		String ident = this.ident;
		if (ident == null || ident == "") {
			setKind(0);
			return;
		}
		if (ident == Constants.nameThis || ident == Constants.nameSuper) {
			setKind(IS_IDENTIFIER | IS_KEYWORD);
			return;
		}
		if (ident == Constants.nameNull) {
			setKind(IS_IDENTIFIER | IS_KEYWORD | IS_CONSTANT | IS_TYPE_DECL);
			return;
		}
		if (ident == "true" || ident == "false") {
			setKind(IS_IDENTIFIER | IS_KEYWORD | IS_CONSTANT);
			return;
		}
		if (ident.charAt(0) == '\"' || ident.charAt(0) == '\'' || Character.isDigit(ident.charAt(0))) {
			setKind(IS_CONSTANT);
			return;
		}
		this.is_token_ident = patternIdent.matcher(ident).matches();
		this.is_token_operator = patternOper.matcher(ident).matches();
		// resolve in the path of scopes
		ASTNode@ v;
		ResInfo info = new ResInfo(this,ident);
		if (PassInfo.resolveNameR(this,v,info)) {
			if (v instanceof Opdef) {
				this.is_token_operator = true;
				pre_resolved = v.$var;
			}
			if (v instanceof TypeDecl) {
				this.is_token_type_decl = true;
				pre_resolved = v.$var;
			}
		}
	}
	
	public boolean preResolveIn() {
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
		if (first_ch == '\"') {
			if (ident.length() < 3 || last_ch != '\"')
				throw new CompilerException(this,"Bad string token");
			replaceWithNodeReWalk(new ConstStringExpr(ConstExpr.source2ascii(ident.substring(1,ident.length()-1))));
		}
		if (first_ch == '\'') {
			if (ident.length() < 3 || last_ch != '\'')
				throw new CompilerException(this,"Bad char token");
			char ch;
			if (ident.length() == 3)
				ch = ident.charAt(1);
			else
				ch = ConstExpr.source2ascii(ident.substring(1,ident.length()-1)).charAt(0);
			replaceWithNodeReWalk(new ConstCharExpr(ch));
		}
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
		ASTNode@ v;
		ResInfo info = new ResInfo(this,ident);
		if( !PassInfo.resolveNameR((ASTNode)this,v,info) )
			throw new CompilerException(this,"Unresolved token "+ident);
		if( v instanceof Opdef ) {
			this.is_token_operator = true;
			pre_resolved = v.$var;
			//replaceWithNodeReWalk(op);
		}
		else if( v instanceof TypeDecl ) {
			this.is_token_type_decl = true;
			pre_resolved = v.$var;
			TypeDecl td = (TypeDecl)pre_resolved;
			//td.checkResolved();
			replaceWithNodeReWalk(new TypeNameRef(pos, ident, td.xtype));
		}
		else {
			replaceWithNodeReWalk(info.buildAccess((ASTNode)this, null, v).closeBuild());
		}
		return false;
	}

}

