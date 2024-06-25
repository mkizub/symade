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

public static enum ETokenKind {
	UNKNOWN,
	MAYBE_IDENTIFIER,
	MAYBE_OPERATOR,
	EXPL_IDENTIFIER,
	EXPL_OPERATOR,
	TYPE_DECL,
	SCOPE_DECL,
	EXPR_IDENT,
	EXPR_NUMBER,
	EXPR_STRING,
	EXPR_CHAR;
	
	public boolean isExplicit() { return ordinal() >= 3; }
};
	
@ThisIsANode(name="EToken", lang=CoreLang)
public final class EToken extends ENode implements ASTToken {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final Pattern patternIdent = Pattern.compile("[\\p{Alpha}_$][\\p{Alnum}_$]*");
	public static final Pattern patternOper = Pattern.compile("[\\!\\#\\%-\\/\\:\\;\\<\\=\\>\\?\\[\\\\\\]\\^\\{\\|\\}\\~\\u2190-\\u22F1]+");
	
	@nodeAttr public ETokenKind base_kind;
	@AttrBinDumpInfo(ignore=true)
	@nodeData public ANode      value;

	public String getTokenText() {
		return this.ident;
	}
	// for GUI
	public final ETokenKind getKind() { base_kind }
	// for GUI
	public final void setKind(ETokenKind val) { base_kind = val; }
	
	
	public EToken() {
		this.base_kind = ETokenKind.UNKNOWN;
	}
	public EToken(Token t, ETokenKind kind) {
		this.base_kind = kind;
		set(t);
	}
	public EToken(int pos, String ident, ETokenKind kind) {
		this.base_kind = kind;
		this.pos = pos;
		this.ident = ident;
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
		this.value = ce;
	}
	
	public String toString() {
		return this.ident;
	}

	public int getPriority(Env env) { return 256; }

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\"")) {
			this.ident = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
			this.base_kind = ETokenKind.EXPL_IDENTIFIER;
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
		return base_kind == ETokenKind.MAYBE_IDENTIFIER || base_kind == ETokenKind.EXPL_IDENTIFIER || base_kind == ETokenKind.TYPE_DECL || base_kind == ETokenKind.SCOPE_DECL;
	}

	public boolean isOperator() {
		if (base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.MAYBE_IDENTIFIER)
			guessKind();
		return base_kind == ETokenKind.MAYBE_OPERATOR || base_kind == ETokenKind.EXPL_OPERATOR;
	}

	public boolean isMaybeOper() {
		if (base_kind == ETokenKind.UNKNOWN)
			guessKind();
		return base_kind == ETokenKind.MAYBE_OPERATOR || base_kind == ETokenKind.EXPL_OPERATOR || base_kind == ETokenKind.MAYBE_IDENTIFIER;
	}

	public TypeRef asType(Env env) {
		if (base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.MAYBE_IDENTIFIER)
			guessKind();
		if (base_kind == ETokenKind.TYPE_DECL && value instanceof TypeDecl)
			return new TypeNameRef(pos, ident, ((TypeDecl)value).getType(env));
		return null;
	}
	
	public DNode asScope(Env env) {
		if (base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.MAYBE_IDENTIFIER)
			guessKind();
		if ((base_kind == ETokenKind.TYPE_DECL || base_kind == ETokenKind.SCOPE_DECL) && value instanceof DNode)
			return (DNode)value;
		return null;
	}
	
	public ENode asExpr(Env env) {
		return this;
	}

	public void guessKind() {
		if (base_kind.isExplicit() || value != null)
			return;
		if (value == null)
			value = NopExpr.dummyNode;
		String ident = this.ident;

		SyntaxScope ss = Env.ctxSyntaxScope(this);
		if (ss != null && ss.isOperator(ident)) {
			this.base_kind = ETokenKind.MAYBE_OPERATOR;
			return;
		}

		if (ident == null || ident == "") {
			if (base_kind != ETokenKind.UNKNOWN)
				this.base_kind = ETokenKind.UNKNOWN;
			return;
		}
		if (patternIdent.matcher(ident).matches())
			this.base_kind = ETokenKind.MAYBE_IDENTIFIER;
		else if (patternOper.matcher(ident).matches())
			this.base_kind = ETokenKind.MAYBE_OPERATOR;
		
		// resolve in the path of scopes
		ResInfo info = new ResInfo(Env.getEnv(),this,ident);
		if (PassInfo.resolveNameR(this,info)) {
			if (info.resolvedSymbol().parent() instanceof OpArgOPER) {
				this.base_kind = ETokenKind.MAYBE_OPERATOR;
			}
			DNode dn = info.resolvedDNode();
			if (dn instanceof KievPackage) {
				this.base_kind = ETokenKind.SCOPE_DECL;
				value = dn;
			}
			if (dn instanceof TypeDecl) {
				this.base_kind = ETokenKind.TYPE_DECL;
				value = dn;
			}
		}
	}
	
	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		String ident = this.ident;
		if (ident == null || ident == "")
			throw new CompilerException(this,"Empty token");
		guessKind();
		return true;
	}
	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) {
		String ident = this.ident;
		if (ident == null || ident == "")
			throw new CompilerException(this,"Empty token");

		if (!base_kind.isExplicit()) {
			SyntaxScope ss = Env.ctxSyntaxScope(this);
			if (ss != null && ss.isOperator(ident)) {
				this.base_kind = ETokenKind.MAYBE_OPERATOR;
				throw new CompilerException(this,"Operator as expression");
				//return false;
			}
		}

		// resolve in the path of scopes
		ResInfo info = new ResInfo(env,this,ident);
		if (!PassInfo.resolveNameR((ASTNode)this,info))
			throw new CompilerException(this,"Unresolved token "+ident);
		if (info.resolvedSymbol().parent() instanceof OpArgOPER) {
			this.base_kind = ETokenKind.MAYBE_OPERATOR;
			throw new CompilerException(this,"Operator as expression");
			//return false;
		}
		DNode dn = info.resolvedDNode();
		if (dn instanceof TypeDecl) {
			value = dn;
			TypeDecl td = (TypeDecl)dn;
			replaceWithNodeReWalk(new TypeNameRef(pos, ident, td.getType(env)),parent,slot);
		}
		else {
			replaceWithNodeReWalk(info.buildAccess(this, null, info.resolvedSymbol()).closeBuild(),parent,slot);
		}
		return false;
	}

}

