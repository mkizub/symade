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
package kiev.vlang.types;
import syntax kiev.Syntax;

import java.util.StringTokenizer;

/**
 * @author Maxim Kizub
 *
 */

public abstract class AType extends TVSet {
	
	@virtual typedef MType  ≤ MetaType;

	@abstract @virtual
	public:ro final			StdTypes			tenv;
	public final			MType				meta_type;
	private					TemplateTVarSet		template;
	private					Type[]				binds;
	private					ArgType[]			appls;
	public:ro,ro,rw,rw		int					flags;
	
	protected AType(MType meta_type, TemplateTVarSet template, int flags) {
		assert(meta_type != null && meta_type.tenv != null);
		this.meta_type = meta_type;
		this.template = template;
		this.flags = flags;
		this.binds = Type.emptyArray;
		this.appls = ArgType.emptyArray;
	}
	
	protected AType(MType meta_type, TemplateTVarSet template, int flags, TVarBld bld)
	{
		assert(meta_type != null && meta_type.tenv != null);
		this.meta_type = meta_type;
		this.template = template;
		this.flags = flags;
		if (bld != null)
			this.setFromBld(bld);
	}
	
	@getter
	public final StdTypes get$tenv() { return meta_type.tenv; }
	
	public final boolean isReference()		{ return (meta_type.flags & MetaType.flReference)		!= 0 ; }
	public final boolean isArray()			{ return (meta_type.flags & MetaType.flArray)			!= 0 ; }
	public final boolean isIntegerInCode()	{ return (meta_type.flags & MetaType.flIntegerInCode)	!= 0 ; }
	public final boolean isInteger()		{ return (meta_type.flags & MetaType.flInteger)			!= 0 ; }
	public final boolean isFloatInCode()	{ return (meta_type.flags & MetaType.flFloatInCode)		!= 0 ; }
	public final boolean isFloat()			{ return (meta_type.flags & MetaType.flFloat)			!= 0 ; }
	public final boolean isNumber()			{ return (meta_type.flags & MetaType.flNumber)			!= 0 ; }
	public final boolean isDoubleSize()		{ return (meta_type.flags & MetaType.flDoubleSize)		!= 0 ; }
	public final boolean isBoolean()		{ return (meta_type.flags & MetaType.flBoolean)			!= 0 ; }
	public final boolean isCallable()		{ return (meta_type.flags & MetaType.flCallable)		!= 0 ; }
	public final boolean isAbstract()		{ return (flags & StdTypes.flAbstract)					!= 0 ; }
	public final boolean isUnerasable()		{ return (flags & StdTypes.flUnerasable)				!= 0 ; }
	public final boolean isVirtual()		{ return (flags & StdTypes.flVirtual)					!= 0 ; }
	public final boolean isFinal()			{ return (flags & StdTypes.flFinal)						!= 0 ; }
	public final boolean isStatic()			{ return (flags & StdTypes.flStatic)					!= 0 ; }
	public final boolean isForward()		{ return (flags & StdTypes.flForward)					!= 0 ; }
	public final boolean isArgAppliable()	{ return (flags & StdTypes.flArgAppliable)				!= 0 ; }
	public final boolean isValAppliable()	{ return (flags & StdTypes.flValAppliable)				!= 0 ; }

	private void setFromBld(TVarBld bld) {
		bld.close(0);
		TemplateTVarSet template = this.template;
		TVar[] template_vars = template.getTVars();
		int n_free = template.n_free;
		if (n_free == 0)
			this.binds = Type.emptyArray;
		else
			this.binds = new Type[n_free];
		TVar[] bld_tvars = bld.getTVars();
		for (int i=0; i < bld_tvars.length; i++) {
			if (bld_tvars[i].isFree()) {
				this.binds[i] = template_vars[i].var;
				continue;
			}
			ArgType at = bld_tvars[i].var;
			for (int j=0; j < template_vars.length; j++) {
				if (at ≡ template_vars[j].var) {
					while (template_vars[j].ref >= 0) j = template_vars[j].ref;
					if (j >= n_free)
						break;
					if (this.binds[j] != null)
						break;
					this.binds[j] = bld.resolveArg(i);
					break;
				}
			}
		}
		for (int j=0; j < n_free; j++) {
			if (this.binds[j] == null)
				this.binds[j] = template.resolveArg(j);
		}

		flags &= ~(StdTypes.flAbstract|StdTypes.flValAppliable);
		for (int i=0; i < template_vars.length; i++) {
			TVar tv = template_vars[i];
			if (tv.isAlias())
				continue;
			Type r = tv.result();
			ArgType v = tv.var;
			if (r.isAbstract()) flags |= StdTypes.flAbstract;
			if (v.isUnerasable()) flags |= StdTypes.flUnerasable;
			if (v.isArgAppliable() && r.isValAppliable()) flags |= StdTypes.flValAppliable;
		}
	}
	
	public final AType bindings() {
		TemplateTVarSet template = this.meta_type.getTemplBindings();
		if (this.binds == null || this.template != template) {
			int n_free = template.n_free;
			if (n_free == 0) {
				this.binds = Type.emptyArray;
			} else {
				if (this.binds == null) {
					this.binds = new Type[n_free];
					for (int i=0; i < n_free; i++)
						this.binds[i] = template.resolveArg(i);
				} else {
					Type[] new_binds = new Type[n_free];
					for (int i=0; i < n_free; i++)
						new_binds[i] = this.resolve(template.getArg(i));
					this.binds = new_binds;
				}
			}
			this.template = template;
			TVar[] template_vars = template.getTVars();
			int flags = this.flags & ~(StdTypes.flAbstract|StdTypes.flValAppliable);
			for (int i=0; i < template_vars.length; i++) {
				TVar tv = template_vars[i];
				if (tv.isAlias())
					continue;
				Type r = tv.result();
				ArgType v = tv.var;
				if (r.isAbstract()) flags |= StdTypes.flAbstract;
				if (v.isUnerasable()) flags |= StdTypes.flUnerasable;
				if (v.isArgAppliable() && r.isValAppliable()) flags |= StdTypes.flValAppliable;
			}
			this.flags = flags;
		}
		return this;
	}
	
	public final void checkResolved() {
		meta_type.tdecl.checkResolved(tenv.env);
		this.bindings();
	}

	public static boolean identity(AType t1, AType t2) operator "V ≡ V" {
		return t1 == t2;
	}

	public static boolean not_identity(AType t1, AType t2) operator "V ≢ V" {
		return t1 != t2;
	}

	public static boolean type_not_equals(AType t1, AType t2) operator "V ≉ V" {
		if (t1 == null || t2 == null) return true;
		return !(t1 ≈ t2);
	}
	
	public static boolean type_equals(AType t1, AType t2) operator "V ≈ V" {
		if (t1 == null || t2 == null) return false;
		if (t1 == t2) return true;
		if (t1.meta_type.tdecl != t2.meta_type.tdecl) return false;
		t1.checkResolved();
		t2.checkResolved();
		if (t1.getArgsLength() != t2.getArgsLength()) return false;
		final int n = t1.getArgsLength();
		for (int i=0; i < n; i++) {
			if (t1.getArg(i) ≢ t2.getArg(i))
				return false;
			if (t1.resolveArg(i) ≉ t2.resolveArg(i))
				return false;
		}
		return true;
	}

	public final boolean equals(Object to) {
		if (to instanceof AType) return AType.type_equals(this,(AType)to);
		return false;
	}

	// find bound value for an abstract type
	public final Type resolve(ArgType arg) {
		if (template == null)
			return arg;
		TVar[] template_tvars = this.template.getTVars();
		final int n = template_tvars.length;
		for(int i=0; i < n; i++) {
			if (template_tvars[i].var ≡ arg) {
				while (template_tvars[i].ref >= 0) i = template_tvars[i].ref;
				Type tp = (i < this.binds.length) ? this.binds[i] : template_tvars[i].val;
				if (tp == null)
					tp = template_tvars[i].var;
				return tp;
			}
		}
		return arg;
	}
	
	public final int getArgsLength() { return this.template == null ? 0 : this.template.getArgsLength(); }
	public final ArgType getArg(int i) { return this.template.getArg(i); }
	public final Type resolveArg(int i)  {
		TVar[] template_tvars = this.template.getTVars();
		while (template_tvars[i].ref >= 0) i = template_tvars[i].ref;
		Type tp = (i < this.binds.length) ? this.binds[i] : template_tvars[i].val;
		if (tp == null)
			tp = template_tvars[i].var;
		return tp;
	}
	public final boolean isAliasArg(int i) { return this.template.isAliasArg(i); }

	// Re-bind type set, replace all abstract types in current set
	// with results of another set. It binds unbound vars, and re-binds
	// (changes) vars bound to abstract types, i.e. it changes only 'TVar.bnd' field.
	//
	// having a re-bind pair A -> V, will re-bind
	// A:?      -> A:V			; bind
	// B:A      -> B:V			; re-bind 
	// C:X<A:?> -> C X<A:V>		; recursive
	//
	// This operation is used in access expressions:
	//
	// class Bar<B> { B b; }
	// class Foo<F> { F f; Foo<Bar<F>> fbf; }
	// Foo<String> a;
	// a.* :- binds Foo.F with String, and applays Bar.B (bound to Foo.F) with String
	//        producing: a.f = String; a.fbf = Foo<Bar<String>>; a.b = String 
	// Foo<Bar<Foo<F>>> x;
	// a.* :- binds Foo.F with Bar<Foo<F>>, and applays Bar.B (bound to Foo.F) with Bar<Foo<F>>,
	//        producing: a.f = Bar<Foo<F>>; a.fbf = Foo<Bar<Bar<Foo<F>>>>; a.b = Bar<Foo<F>>
	// a.fbf.* :- a.fbf.f = 
	//
	// my.bnd ≡ vs.var -> (my.var, vs.result())
	
	public TVarBld applay_bld(TVSet vs)
	{
		if (!this.hasApplayables(vs))
			return null;
		TVarBld sr = new TVarBld(this);
		final TVar[] template_vars = this.template.getTVars();
		final int vs_size = vs.getArgsLength();
		final int my_size = template_vars.length;

	next_my:
		for(int i=0; i < my_size; i++) {
			int p = i;
			while (template_vars[p].ref >= 0) p = template_vars[p].ref;
			if (!template_vars[p].var.isArgAppliable())
				continue;
			Type bnd = (p < this.binds.length) ? this.binds[p] : null;
			if (bnd == null)
				continue;
			if (bnd ≡ tenv.tpSelfTypeArg && vs instanceof Type) {
				sr.set(i, (Type)vs);
			}
			else if (bnd instanceof ArgType) {
				for(int j=0; j < vs_size; j++) {
					if (bnd ≡ vs.getArg(j)) {
						// re-bind
						sr.set(i, vs.resolveArg(j));
						continue next_my;
					}
				}
			}
			else if (bnd.bindings().hasApplayables(vs)) {
				// recursive
				Type t = bnd.applay(vs);
				if (t ≉ bnd)
					sr.set(i, t);
			}
		}
		return sr;
	}
	
	private boolean hasApplayables(TVSet vs) {
		final ArgType[] appls = this.getTArgs();
		final int my_size = appls.length;
		if (my_size == 0)
			return false;
		for (int i=0; i < my_size; i++) {
			if (appls[i] ≡ tenv.tpSelfTypeArg)
				return true;
		}
		final int tp_size = vs.getArgsLength();
		if (tp_size == 0)
			return false;
		for (int i=0; i < my_size; i++) {
			for (int j=0; j < tp_size; j++) {
				if (appls[i] ≡ vs.getArg(j))
					return true;
			}
		}
		return false;
	}
	
	public boolean hasApplayable(ArgType at) {
		final ArgType[] appls = this.getTArgs();
		final int my_size = appls.length;
		if (my_size == 0)
			return false;
		for (int i=0; i < my_size; i++) {
			if (appls[i] ≡ at)
				return true;
		}
		return false;
	}
	
	public final ArgType[] getTArgs() {
		if (this.appls == null)
			buildApplayables();
		return this.appls;
	}

	private void buildApplayables() {
		this.appls = ArgType.emptyArray;
		final TVar[] template_vars = this.template.getTVars();
		int n = template_vars.length;
		for (int i=0; i < n; i++) {
			if (template_vars[i].isAlias())
				continue;
			addApplayables(resolveArg(i));
		}
	}
	
	private void addApplayables(Type t) {
		if (t instanceof ArgType) {
			addApplayable((ArgType)t);
		} else {
			ArgType[] tappls = t.bindings().getTArgs();
			for (int i=0; i < tappls.length; i++)
				addApplayable(tappls[i]);
		}
	}
	private void addApplayable(ArgType at) {
		int sz = this.appls.length;
		for (int i=0; i < sz; i++) {
			if (this.appls[i] ≡ at)
				return;
		}
		ArgType[] tmp = new ArgType[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = this.appls[i];
		tmp[sz] = at;
		this.appls = tmp;
	}

	public String toDump() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getClass());
		sb.append(" {\n");
		for (int i=0; i < binds.length; i++)
			sb.append(i).append(": ").append(binds[i]).append('\n');
		sb.append("} ").append(template.toDump());
		return sb.toString();
	}

	public String makeSignature(Env env) {
		StringBuffer str = new StringBuffer();
		TypeDecl tdecl = meta_type.tdecl;
		str.append(tdecl.qname());
		SymUUID suuid = tdecl.symbol.suuid();
		if (suuid == null && suuid != SymUUID.Empty && !tdecl.isInterfaceOnly())
			suuid = tdecl.symbol.getUUID(env);
		if (suuid != null && suuid != SymUUID.Empty)
			str.append('‣').append(suuid);
		this.bindings();
		if (this.binds.length == 0)
			return str.toString();
		boolean params = false;
		TemplateTVarSet template = this.template;
		for(int i=0; i < this.binds.length; i++) {
			ArgType at = getArg(i);
			Type tp = resolveArg(i);
			if (at ≡ tp)
				continue;
			if (!params) {
				str.append('(');
				params = true;
			} else {
				str.append(',');
			}
			str.append(at.name);
			str.append('=');
			str.append(tp.makeSignature(env));
		}
		if (params)
			str.append(')');
		return str.toString();
	}
	
	public static String getNameFromSignature(String sign) {
		int p1 = sign.indexOf('‣');
		if (p1 < 0)
			p1 = sign.indexOf('@');
		int p2 = sign.indexOf('(');
		int p = -1;
		if (p1 >= 0 && p2 >= 0)
			p = Math.min(p1, p2);
		else if (p1 >= 0)
			p = p1;
		else if (p2 >= 0)
			p = p2;
		if (p >= 0)
			return sign.substring(0, p);
		return sign;
	}
	public static Type fromSignature(Env env, String sign, boolean full) {
		StringTokenizer st = new StringTokenizer(sign,"=,()",true);
		String[] sep = {""};
		return fromSignature(env,st,sep,full);
	}
	private static Type fromSignature(Env env, StringTokenizer st, String[] sep, boolean full) {
		String name = st.nextToken();
		String uuid = null;
		int p = name.indexOf('‣');
		if (p < 0)
			p = name.indexOf('@');
		if (p > 0) {
			uuid = name.substring(p+1);
			name = name.substring(0, p);
		}
		TypeDecl tdecl = null;
		if (uuid != null) {
			Symbol sym = env.getSymbolByUUID(uuid);
			if (sym != null && sym.dnode instanceof TypeDecl)
				tdecl = (TypeDecl)sym.dnode;
			//if (tdecl != null)
			//	assert (tdecl.qname().equals(name));
		}
		if (tdecl == null) {
			tdecl = (TypeDecl)env.resolveGlobalDNode(name);
		}
		if (tdecl == null) {
			// pre-load all top-level classes, to find an inner class
			int p = name.indexOf('·');
			while (p > 0) {
				String pnm = name.substring(0,p);
				if (env.existsTypeDecl(pnm))
					env.loadAnyDecl(pnm);
				p = name.indexOf('·', p+1);
			}
			if (env.existsTypeDecl(name))
				tdecl = env.loadTypeDecl(name,false);
			if (tdecl == null) {
				System.out.println("Warning: Cannot find TypeDecl "+name);
				tdecl = env.tenv.tpVoid.meta_type.tdecl;
			}
		}
		if (!st.hasMoreElements())
			return tdecl.getType(env);
		sep[0] = st.nextToken();
		if (!sep[0].equals("(")) {
			assert (sep[0].equals(",")||sep[0].equals(")"));
			if (tdecl instanceof TypeDef)
				return tdecl.getAType(env);
			return tdecl.getType(env);
		}
		TVar[] template_vars = tdecl.getMetaType(env).getTemplBindings().getTVars();
		TVarBld set = new TVarBld();
		while (!sep[0].equals(")")) {
			String aname = st.nextToken();
			sep[0] = st.nextToken();
			assert (sep[0].equals("="));
			Type tp = fromSignature(env,st,sep,full);
			ArgType a = null;
			foreach (TVar t; template_vars; t.var.name.equals(aname)) {
				a = t.var;
				break;
			}
			if (a != null)
				set.append(a, tp);
			else
				System.out.println("Warning: Cannot find var "+aname+" in type "+tdecl);
			assert (sep[0].equals(",")||sep[0].equals(")"));
		}
		return tdecl.getMetaType(env).make(set);
	}
}

public final class TVar {
	
	public static final int MODE_FREE  = -1;
	public static final int MODE_BOUND = -2;
	
	public static final TVar[] emptyArray = new TVar[0];

	public final ArgType		var;	// variable
	public final Type			val;	// value of the TVar (null for free vars, ArgType for aliases) 
	public final int			ref;	// reference to actual TVar, for aliases

	TVar(ArgType var, Type val, int ref) {
		assert ((ref>=0 && val instanceof ArgType) || (ref==MODE_FREE && val==null) || (ref==MODE_BOUND && val!=null));
		this.var = var;
		this.val = val;
		this.ref = ref;
	}
	
	public Type result() {
		return val == null? var : val;
	}
	
	public boolean isFree() { return val == null; }
	
	public boolean isAlias() { return ref >= 0; }

	public String toString() {
		if (isFree())
			return "free  "+var.definer.parent()+"."+var.definer;
		else if (isAlias())
			return "alias "+var.definer.parent()+"."+var.definer+" > "+this.ref;
		else
			return "bound "+var.definer.parent()+"."+var.definer+" = "+val;
	}
}


