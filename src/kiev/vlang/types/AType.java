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

import kiev.be.java15.JType;
import kiev.be.java15.JBaseType;
import kiev.be.java15.JArrayType;
import kiev.be.java15.JMethodType;
import kiev.be.java15.JStruct;

import java.util.StringTokenizer;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class AType extends TVSet implements StdTypes {
	
	public final			MetaType			meta_type;
	//private					TemplateTVarSet		type_template;
	@access:no,no,ro,rw		TVar[]				tvars;
	private					ArgType[]			appls;
	private					int					version_and_flags;
	@packed:16,version_and_flags,0
	public:ro,ro,rw,rw		int					flags;
	@packed:16,version_and_flags,16
	private					int					version;
	
	protected AType(MetaType meta_type, int flags) {
		this.meta_type = meta_type;
		//this.type_template = meta_type.getTemplBindings();
		this.flags = flags;
		this.tvars = TVar.emptyArray;
		this.appls = ArgType.emptyArray;
	}
	
	protected AType(MetaType meta_type, int flags, TVarBld bld)
	{
		this.meta_type = meta_type;
		//this.type_template = meta_type.getTemplBindings();
		this.flags = flags;
		this.setFromBld(bld);
	}
	
	public       boolean isReference()		{ return (meta_type.flags & MetaType.flReference)		!= 0 ; }
	public       boolean isArray()			{ return (meta_type.flags & MetaType.flArray)			!= 0 ; }
	public final boolean isIntegerInCode()	{ return (meta_type.flags & MetaType.flIntegerInCode)	!= 0 ; }
	public final boolean isInteger()		{ return (meta_type.flags & MetaType.flInteger)		!= 0 ; }
	public final boolean isFloatInCode()	{ return (meta_type.flags & MetaType.flFloatInCode)	!= 0 ; }
	public final boolean isFloat()			{ return (meta_type.flags & MetaType.flFloat)			!= 0 ; }
	public final boolean isNumber()		{ return (meta_type.flags & MetaType.flNumber)			!= 0 ; }
	public final boolean isDoubleSize()	{ return (meta_type.flags & MetaType.flDoubleSize)		!= 0 ; }
	public final boolean isBoolean()		{ return (meta_type.flags & MetaType.flBoolean)		!= 0 ; }
	public final boolean isCallable()		{ return (meta_type.flags & MetaType.flCallable)		!= 0 ; }
	public final boolean isAbstract()		{ return (flags & flAbstract)							!= 0 ; }
	public final boolean isUnerasable()	{ return (flags & flUnerasable)						!= 0 ; }
	public final boolean isVirtual()		{ return (flags & flVirtual)							!= 0 ; }
	public final boolean isFinal()			{ return (flags & flFinal)								!= 0 ; }
	public final boolean isStatic()		{ return (flags & flStatic)								!= 0 ; }
	public final boolean isForward()		{ return (flags & flForward)							!= 0 ; }
	public final boolean isHidden()		{ return (flags & flHidden)								!= 0 ; }
	public final boolean isArgAppliable()	{ return (flags & flArgAppliable)						!= 0 ; }
	public final boolean isValAppliable()	{ return (flags & flValAppliable)						!= 0 ; }
	public final boolean isBindable()		{ return (flags & flBindable)							!= 0 ; }

	private void setFromBld(TVarBld bld) {
		bld.close();
		TVar[] bld_tvars = bld.getTVars();
		int n = bld_tvars.length;
		if (n > 0) {
			this.tvars = new TVar[n];
			for (int i=0; i < n; i++)
				this.tvars[i] = bld_tvars[i];
		} else {
			this.tvars = TVar.emptyArray;
		}

		flags &= ~(flAbstract|flValAppliable|flBindable);
		foreach(TVar tv; this.tvars; !tv.isAlias()) {
			Type r = tv.result();
			ArgType v = tv.var;
			if (tv.isFree()) flags |= flBindable;
			if (r.isAbstract()) flags |= flAbstract;
			if (v.isUnerasable()) flags |= flUnerasable;
			if (v.isArgAppliable() && r.isValAppliable()) flags |= flValAppliable;
		}
	}
	
	public final AType bindings() {
		if (!this.meta_type.checkTypeVersion(this.version)) {
			this.setFromBld(meta_type.getTemplBindings().bind_bld(this));
			this.version = this.meta_type.version;
		}
		return this;
	}
	
	public static boolean identity(AType t1, AType t2) alias xfx operator ≡ {
		return t1 == t2;
	}

	public static boolean not_identity(AType t1, AType t2) alias xfx operator ≢ {
		return t1 != t2;
	}

	public static boolean type_not_equals(AType t1, AType t2) alias xfx operator ≉ {
		if (t1 == null || t2 == null) return true;
		return !(t1 ≈ t2);
	}
	
	public static boolean type_equals(AType t1, AType t2) alias xfx operator ≈ {
		if (t1 == null || t2 == null) return false;
		if (t1 == t2) return true;
		if (t1.meta_type != t2.meta_type) return false;
		TVar[] b1 = t1.bindings().tvars;
		TVar[] b2 = t2.bindings().tvars;
		if (b1.length != b2.length) return false;
		final int n = b1.length;
		for (int i=0; i < n; i++) {
			TVar tv1 = b1[i];
			TVar tv2 = b2[i];
			if (tv1.var != tv2.var) return false;
			if (tv1.unalias(t1).result() ≉ tv2.unalias(t2).result())
				return false;
		}
		return true;
	}

	public final boolean equals(Object to) {
		if (to instanceof AType) return AType.type_equals(this,(AType)to);
		return false;
	}

	public TVar[] getTVars() {
		return this.tvars;
	}

	// find bound value for an abstract type
	public final Type resolve(ArgType arg) {
		TVar[] tvars = this.tvars;
		final int n = tvars.length;
		for(int i=0; i < n; i++) {
			if (tvars[i].var ≡ arg)
				return tvars[i].unalias(this).result();
		}
		return arg;
	}
	
	public int getArgsLength() { return this.tvars.length; }
	public ArgType getArg(int i) { return this.tvars[i].var; }
	public Type resolveArg(int i)  { return this.tvars[i].unalias(this).result(); }

	// change bound types, for virtual args, outer args, etc
	public TVarBld rebind_bld(TVSet vs) {
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i];
			// TVarBound already bound
			if (!x.isAlias()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(i, y.unalias(vs).result());
						continue next_my;
					}
				}
				continue next_my;
			}
			// bind virtual aliases
			if (x.var.isVirtual()) {
				for (int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (x.var ≡ y.var) {
						sr.set(i, y.unalias(vs).result());
						continue next_my;
					}
				}
				continue next_my;
			}
		}
		return sr.close();
	}

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
		TVar[] my_vars = this.tvars;
		TVar[] vs_vars = vs.getTVars();
		final int my_size = my_vars.length;
		final int vs_size = vs_vars.length;
		TVarBld sr = new TVarBld(this);
		if (!this.hasApplayables(vs))
			return sr.close();

	next_my:
		for(int i=0; i < my_size; i++) {
			TVar x = my_vars[i].unalias(this);
			Type bnd = x.val;
			if (x.isFree() || !x.var.isArgAppliable())
				continue;
			if (bnd instanceof ArgType) {
				for(int j=0; j < vs_size; j++) {
					TVar y = vs_vars[j];
					if (bnd ≡ y.var) {
						// re-bind
						sr.set(i, y.unalias(vs).result());
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
		return sr.close();
	}
	
	private boolean hasApplayables(TVSet vs) {
		final ArgType[] appls = this.getTArgs();
		final int my_size = appls.length;
		if (my_size == 0)
			return false;
		TVar[] vs_vars = vs.getTVars();
		final int tp_size = vs_vars.length;
		if (tp_size == 0)
			return false;
		for (int i=0; i < my_size; i++) {
			for (int j=0; j < tp_size; j++) {
				if (appls[i] ≡ vs_vars[j].var)
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
	
	final ArgType[] getTArgs() {
		if (this.appls == null)
			buildApplayables();
		return this.appls;
	}

	private void buildApplayables() {
		this.appls = ArgType.emptyArray;
		foreach (TVar tv; tvars; !tv.isAlias())
			addApplayables(tv.result());
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
		for (int i=0; i < tvars.length; i++)
			sb.append(i).append(": ").append(tvars[i]).append('\n');
		sb.append("}");
		return sb.toString();
	}

	public String makeSignature() {
		StringBuffer str = new StringBuffer();
		TypeDecl tdecl = meta_type.tdecl;
		str.append(tdecl.qname());
		String uuid = tdecl.uuid;
		if (uuid != null)
			str.append('@').append(uuid);
		boolean hasArgs = false;
		TemplateTVarSet templ = meta_type.getTemplBindings();
		AType self = this.bindings();
		for(int i=0; i < self.tvars.length; i++) {
			TVar t = templ.tvars[i];
			TVar x = self.tvars[i];
			if (!t.isFree())
				continue;
			if (x.var == x.val)
				continue; // self-bound
			if (!hasArgs) {
				str.append('(');
				hasArgs = true;
			} else {
				str.append(',');
			}
			str.append(x.var.name);
			str.append('=');
			String val = x.val.makeSignature();
			assert (val != null && !"null".equals(val));
			str.append(x.val.makeSignature());
		}
		if (hasArgs)
			str.append(')');
		return str.toString();
	}
	
	public static Type fromSignature(String sign, boolean full) {
		StringTokenizer st = new StringTokenizer(sign,"=,()",true);
		String[] sep = {""};
		return fromSignature(st,sep,full);
	}
	private static Type fromSignature(StringTokenizer st, String[] sep, boolean full) {
		String name = st.nextToken();
		String uuid = null;
		int p = name.indexOf('@');
		if (p > 0) {
			uuid = name.substring(p+1);
			name = name.substring(0, p);
		}
		TypeDecl tdecl = null;
		if (uuid != null) {
			tdecl = (TypeDecl)Env.getRoot().getISymbolByUUID(uuid);
			if (tdecl != null)
				assert (tdecl.qname().equals(name));
		}
		if (tdecl == null) {
			tdecl = (TypeDecl)Env.getRoot().resolveGlobalDNode(name);
		}
		if (tdecl == null) {
			// pre-load all top-level classes, to find an inner class
			int p = name.indexOf('\u001f');
			while (p > 0) {
				String pnm = name.substring(0,p);
				if (Env.getRoot().existsTypeDecl(pnm))
					Env.getRoot().loadTypeDecl(pnm,false);
				p = name.indexOf('\u001f', p+1);
			}
			if (Env.getRoot().existsTypeDecl(name))
				tdecl = Env.getRoot().loadTypeDecl(name,false);
			if (tdecl == null) {
				System.out.println("Warning: Cannot find TypeDecl "+name);
				tdecl = StdTypes.tpVoid.meta_type.tdecl;
			}
		}
		if (!st.hasMoreElements())
			return tdecl.xtype;
		sep[0] = st.nextToken();
		if (!sep[0].equals("(")) {
			assert (sep[0].equals(",")||sep[0].equals(")"));
			if (tdecl instanceof TypeDef)
				return tdecl.getAType();
			return tdecl.xtype;
		}
		TVarBld set = new TVarBld();
		while (!sep[0].equals(")")) {
			String aname = st.nextToken();
			sep[0] = st.nextToken();
			assert (sep[0].equals("="));
			Type tp = fromSignature(st,sep,full);
			ArgType a = null;
			foreach (TVar t; tdecl.xtype.bindings().tvars; t.var.name.equals(aname)) {
				a = t.var;
				break;
			}
			if (a != null)
				set.append(a, tp);
			else
				System.out.println("Warning: Cannot find var "+aname+" in type "+tdecl);
			assert (sep[0].equals(",")||sep[0].equals(")"));
		}
		return tdecl.xmeta_type.make(set);
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
	
	public TVar unalias(TVSet tp) {
		TVar r = this;
		while (r.ref >= 0) r = tp.getTVars()[r.ref];
		return r;
	}
	
	public int unalias_idx(TVSet tp, int idx) {
		TVar r = tp.getTVars()[idx];
		assert(r == this); 
		while (r.ref >= 0) {
			idx = r.ref;
			r = tp.getTVars()[idx];
		}
		return idx;
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


