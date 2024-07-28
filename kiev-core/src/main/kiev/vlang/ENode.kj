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
package kiev.vlang;
import syntax kiev.Syntax;

/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@ThisIsANode(lang=CoreLang)
public abstract class ENode extends ASTNode {

	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	final static class TypeSignature {
		final String name;
		final String sign;
		TypeSignature(String sign) {
			this.name = AType.getNameFromSignature(sign).intern();
			this.sign = sign;
		}
	}
	
	private Object	ident_or_symbol_or_type;
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr @abstract public String			ident;
	@AttrXMLDumpInfo(ignore=true)
	@nodeData @abstract public Symbol			symbol;
	@AttrXMLDumpInfo(attr=true, name="type")
	@nodeData @abstract public Type				type_lnk;
	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	@nodeData @abstract public:ro DNode			dnode;

	public void setNameAndUUID(NameAndUUID nid) {
		this.ident_or_symbol_or_type = nid;
	}

	public void setTypeSignature(String signature) {
		this.ident_or_symbol_or_type = new TypeSignature(signature);
	}
	
	public boolean isExptTypeSignature() {
		return this.ident_or_symbol_or_type instanceof TypeSignature;
	}
	
	@getter public final String get$ident() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof String)
			return (String)id;
		if (id instanceof Symbol) {
			//if (qualified)
			//	return ((Symbol)id).qname();
			return ((Symbol)id).sname;
		}
		if (id instanceof Type) {
			//if (qualified)
			//	return ((Type)id).meta_type.qname();
			return ((Type)id).meta_type.tdecl.sname;
		}
		if (id instanceof NameAndUUID) {
			return ((NameAndUUID)id).name;
		}
		if (id instanceof TypeSignature) {
			return ((TypeSignature)id).name;
		}
		return null;
	}

	@getter public final Symbol get$symbol() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Symbol)
			return (Symbol)id;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl.symbol;
		if (id instanceof NameAndUUID) {
			NameAndUUID nid = (NameAndUUID)id;
			Symbol sym = Env.getEnv().getSymbolByUUID(nid.uuid_high, nid.uuid_low);
			if (sym != null) {
				this.symbol = sym;
				return sym;
			}
		}
		if (id instanceof TypeSignature) {
			Type tp = AType.fromSignature(Env.getEnv(),id.sign,true);
			this.type_lnk = tp;
			return tp.meta_type.tdecl.symbol;
		}
		return null;
	}
	
	@getter public final DNode get$dnode() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Symbol)
			return ((Symbol)id).dnode;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		if (id instanceof NameAndUUID) {
			NameAndUUID nid = (NameAndUUID)id;
			Symbol sym = Env.getEnv().getSymbolByUUID(nid.uuid_high, nid.uuid_low);
			if (sym != null) {
				this.symbol = sym;
				return sym.dnode;
			}
		}
		if (id instanceof TypeSignature) {
			Type tp = AType.fromSignature(Env.getEnv(),id.sign,true);
			this.type_lnk = tp;
			return tp.meta_type.tdecl;;
		}
		return null;
	}
	
	@getter public final Type get$type_lnk() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Type) {
			Type tp = (Type)id;
			tp.bindings();
			return tp;
		}
		if (id instanceof TypeSignature) {
			Type tp = AType.fromSignature(Env.getEnv(),id.sign,true);
			this.type_lnk = tp;
			return tp;
		}
		return null;
	}
	
	@setter public final void set$ident(String val) {
		if (val != null) {
			val = val.intern();
			//if (val.indexOf('·') >= 0)
			//	qualified = true;
		}
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$symbol(Symbol val) {
		//if (val != null && val.target instanceof CoreOperation)
		//	assert (!(this instanceof CallExpr));
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$type_lnk(Type val) {
		assert (this instanceof TypeRef);
		ident_or_symbol_or_type = val;
	}
	
	//
	// Expr specific
	//

	// use as field (disable setter/getter calls for virtual fields)
	public final boolean isAsField() {
		return this.is_expr_as_field;
	}
	public final void setAsField(boolean on) {
		if (this.is_expr_as_field != on) {
			this.is_expr_as_field = on;
		}
	}
	// expression will generate void value
	public final boolean isGenVoidExpr() {
		return this.is_expr_gen_void;
	}
	public final void setGenVoidExpr(boolean on) {
		if (this.is_expr_gen_void != on) {
			this.is_expr_gen_void = on;
		}
	}
	// used bt for()
	public final boolean isForWrapper() {
		return this.is_expr_for_wrapper;
	}
	public final void setForWrapper(boolean on) {
		if (this.is_expr_for_wrapper != on) {
			this.is_expr_for_wrapper = on;
		}
	}
	// used for primary expressions, i.e. (a+b)
	public final boolean isPrimaryExpr() {
		return this.is_expr_primary;
	}
	public final void setPrimaryExpr(boolean on) {
		if (this.is_expr_primary != on) {
			this.is_expr_primary = on;
		}
	}
	// used for cast calls (to check for null)
	public final boolean isCastCall() {
		return this.is_expr_cast_call;
	}
	public final void setCastCall(boolean on) {
		if (this.is_expr_cast_call != on) {
			this.is_expr_cast_call = on;
		}
	}


	//
	// Statement specific flags
	//
	
	// abrupted
	public final boolean isAbrupted() {
		return this.is_stat_abrupted;
	}
	public final void setAbrupted(boolean on) {
		if (this.is_stat_abrupted != on) {
			this.is_stat_abrupted = on;
		}
	}
	// breaked
	public final boolean isBreaked() {
		return this.is_stat_breaked;
	}
	public final void setBreaked(boolean on) {
		if (this.is_stat_breaked != on) {
			this.is_stat_breaked = on;
		}
	}
	// method-abrupted
	public final boolean isMethodAbrupted() {
		return this.is_stat_method_abrupted;
	}
	public final void setMethodAbrupted(boolean on) {
		if (this.is_stat_method_abrupted != on) {
			this.is_stat_method_abrupted = on;
			if (on) this.is_stat_abrupted = true;
		}
	}
	// auto-returnable
	public final boolean isAutoReturnable() {
		return this.is_stat_auto_returnable;
	}
	public final void setAutoReturnable(boolean on) {
		if (this.is_stat_auto_returnable != on) {
			this.is_stat_auto_returnable = on;
		}
	}
	// reachable by direct control flow, with no jumps into
	public final boolean isDirectFlowReachable() {
		return this.is_direct_flow_reachable;
	}
	public final void setDirectFlowReachable(boolean on) {
		if (this.is_direct_flow_reachable != on) {
			this.is_direct_flow_reachable = on;
		}
	}

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() {}

	public void initFrom(ENode node, Symbol sym, ENode[] args) {
		throw new RuntimeException("Cannot init "+getClass()+" from "+node.getClass());
	}
	
	public Type[] getAccessTypes(Env env) {
		return new Type[]{getType(env)};
	}

	public Opdef resolveOpdef(Env env) {
		Symbol sym = this.symbol;
		if (sym == null) {
			String ident = this.ident;
			if (ident != null) {
				ResInfo info;
				info = new ResInfo<CoreOperation>(env,this,ident);
				if (!env.root.resolveNameR(info)) { //(!PassInfo.resolveNameR(this,info)) {
					info = new ResInfo<Opdef>(env,this,ident);
					if (!PassInfo.resolveNameR(this,info)) {
						if (Env.ctxMethod(this) == null || !Env.ctxMethod(this).isMacro())
							Kiev.reportError(this,"Unresolved operator "+ident);
						return null;
					}
				}
				this.symbol = info.resolvedSymbol();
			}
		}
		else if (sym.dnode instanceof Opdef)
			return (Opdef)sym.dnode;
		return null;
	}
	
	public CoreOperation getOperation(Env env) {
		DNode dn = this.dnode;
		if (dn instanceof CoreOperation)
			return (CoreOperation)dn;
		return null;
	}
	public Operator getOper() {
		DNode dn = this.dnode;
		if (dn instanceof CoreOperation)
			return ((CoreOperation)dn).getOperator();
		if (dn instanceof Opdef)
			return dn.resolved;
		return null;
	}
	public Opdef getFakeOpdef(Env env) {
		Opdef opd = null;
		Method m = null;
		Object id = this.ident_or_symbol_or_type;
		if (id instanceof Symbol) {
			ANode p = id.parent();
			if (p instanceof Opdef)
				return (Opdef)p;
			if (p instanceof Method)
				m = (Method)p;
			else
				m = getOperation(env);
		}
		if (m != null) {
			try {
				SyntaxScope ss = Env.ctxSyntaxScope(this);
				if (ss != null) {
					foreach (ImportSyntax imp; ss.syntaxes) {
						KievSyntax stx = imp.name.dnode;
						if (stx != null) {
							opd = stx.getOpdefForMethod(env, m);
							if (opd != null)
								return opd;
						}
					}
				}
				KievSyntax kiev_stx = (KievSyntax) env.loadAnyDecl("kiev·Syntax");
				opd = kiev_stx.getOpdefForMethod(env, m);
				if (opd != null)
					return opd;
			} catch (Throwable t) {}
		}
		return null;
	}

	public final Method resolveMethodAndNormalize(Env env, INode parent, AttrSlot slot) {
		DNode dn = this.dnode;
		Method m;
		Symbol sym;
		if (dn == null) {
			Opdef opd = resolveOpdef(env);
			dn = this.dnode;
		}
		if (dn instanceof Opdef) {
			Opdef opd = (Opdef)dn;
			sym = opd.resolveMethod(env,this);
			if (sym == null) {
				if (Env.ctxMethod(this) == null || !Env.ctxMethod(this).isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+opd);
				return null;
			}
			m = (Method)sym.dnode;
			this.symbol = sym;
		}
		else if (dn instanceof Method) {
			m = (Method)dn;
			sym = this.symbol;
		}
		else if (dn != null) {
			Kiev.reportError(this, "Unknown node typer "+dn.getClass()+" during expression normalization");
			return null;
		}
		else {
			if (Env.ctxMethod(this) == null || !Env.ctxMethod(this).isMacro())
				Kiev.reportError(this, "Unresolved '"+ident+"' during expression normalization");
			return null;
		}
		m.normilizeExpr(env, this, sym, parent, slot);
		return m;
	}

	public ENode[] getEArgs() { return null; }

	public int getLvalArity() { return -1; }

	public int getPriority(Env env) {
		if (isPrimaryExpr())
			return 255;
		Opdef opd = getFakeOpdef(env);
		if (opd == null)
			return 255;
		return opd.prior;
	}

	public boolean valueEquals(Object o) { return false; }
	public boolean isConstantExpr(Env env) { return false; }
	public Object	getConstValue(Env env) {
		throw new RuntimeException("Request for constant value of non-constant expression");
	}
	
	public ENode closeBuild() { return this; }

	public INode doRewrite(RewriteContext ctx) {
		ENode en = (ENode)super.doRewrite(ctx);
		String id = this.ident;
		String rw = ctx.replace(id);
		if (id != rw) {
			en.ident = rw;
		} else {
			Object id = this.ident_or_symbol_or_type;
			if (id == null)
				;
			else if (id instanceof String)
				;
			else if (id instanceof Type)
				;
			else if (id instanceof NameAndUUID)
				en.ident_or_symbol_or_type = id;
			else if (id instanceof TypeSignature)
				en.ident_or_symbol_or_type = id;
			else if (id instanceof Symbol) {
				DNode dn = id.dnode;
				if (dn instanceof Opdef)
					en.ident_or_symbol_or_type = id;
				else if (dn instanceof Method)
					en.ident_or_symbol_or_type = id;
				//else if (qualified)
				//	en.ident_or_symbol_or_type = id.qname();
				else
					en.ident_or_symbol_or_type = id.sname;
			}
		}
		return en;
	}

	public String toStringByOpdef() {
		Opdef opd = getFakeOpdef(Env.getEnv());
		if (opd != null)
			return opd.toString(this);
		StringBuffer sb = new StringBuffer();
		sb.append("( ");
		sb.append(this.getClass().getName());
		sb.append(": ");
		ENode[] eargs = getEArgs();
		if (eargs != null) {
			foreach (ENode e; eargs)
				sb.append(e).append(", ");
		}
		sb.append(")");
		return sb.toString();
	}
}

@ThisIsANode(name="NoOp", lang=CoreLang)
public final class NopExpr extends ENode {

	public static final ENode dummyNode = new NopExpr();

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public NopExpr() {}
	
	public String toString() { return ""; }

	public Type getType(Env env) {
		return env.tenv.tpVoid;
	}
}

@ThisIsANode(lang=CoreLang)
public final class ArgExpr extends ENode {

	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(out="this:in")			ENode		expr;
	}

	@nodeData public Var			var;
	@nodeAttr public ENode			expr;

	public ArgExpr() {}
	
	public ArgExpr(Var var, ENode expr) {
		this.var = var;
		this.expr = expr;
	}
	
	public ENode[] getEArgs() { return new ENode[]{expr}; }

	public String toString() { return var + ":" + expr; }

	public Type getType(Env env) {
		return expr.getType(env);
	}

	public boolean isConstantExpr(Env env) {
		return expr.isConstantExpr(env);
	}
	public Object getConstValue(Env env) {
		return expr.getConstValue(env);
	}
}


