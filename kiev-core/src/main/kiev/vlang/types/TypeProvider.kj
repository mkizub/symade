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

public abstract class MetaType implements Constants {
	
	@virtual typedef TDecl  ≤ TypeDecl;

	public static final int flReference		= 1 <<  0;
	public static final int flIntegerInCode	= 1 <<  1;
	public static final int flInteger			= 1 <<  2;
	public static final int flFloatInCode		= 1 <<  3;
	public static final int flFloat			= 1 <<  4;
	public static final int flNumber			= flFloat | flInteger;
	public static final int flDoubleSize		= 1 <<  5;
	public static final int flArray			= 1 <<  6;
	public static final int flBoolean			= 1 <<  7;
	public static final int flWrapper			= 1 <<  8;
	public static final int flCallable			= 1 <<  9;

	final
	public StdTypes				tenv;
	@virtual @abstract
	public:ro TDecl				tdecl;
	public Symbol				tdecl_symbol;
	public int					flags;

	@getter public TDecl get$tdecl() {
		return (TDecl)this.tdecl_symbol.dnode;
	}
	
	public abstract Type getDefType();
	
	public String qname() {
		if (tdecl != null)
			return tdecl.qname();
		return "<tdecl>";
	}
	
	public MetaType(StdTypes tenv, Symbol tdecl_symbol, int flags) {
		assert(tenv != null && tdecl_symbol != null);
		this.tenv = tenv;
		this.tdecl_symbol = tdecl_symbol;
		this.flags = flags;
		if!(this instanceof CallMetaType) {
			assert(tenv.allMetaTypes.get(tdecl_symbol) == null);
			tenv.allMetaTypes.put(tdecl_symbol, this);
		}
	}

	public MetaType(StdTypes tenv, TDecl tdecl, int flags) {
		this(tenv, tdecl.symbol, flags);
	}

	public abstract TemplateTVarSet getTemplBindings();
	
	protected void callbackTypeVersionChanged() { /* ignore */ }

	public Type[] getMetaSupers(Type tp) {
		if (tdecl.super_types.length == 0)
			return Type.emptyArray;
		Type[] stps = new Type[tdecl.super_types.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = tdecl.super_types[i].getType(tenv.env).applay(tp);
		return stps;
	}

	public Type make(TVarBld set) {
		if (set == null)
			return new XType(this, null, null);
		else
			return new XType(this, getTemplBindings(), set);
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		TVarBld vs = t.bindings().applay_bld(bindings);
		if (vs == null)
			return t;
		return new XType(this, getTemplBindings(), vs);
	}

	public rule resolveNameAccessR(Type tp, ResInfo info)
	{
		trace(Kiev.debug && Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in "+tp),
		tdecl.checkResolved(info.env),
		{
			trace(Kiev.debug && Kiev.debugResolve,"Type: resolving in "+tp),
			resolveNameR_1(info)	// resolve in this class
		;	info.isSuperAllowed(),
			trace(Kiev.debug && Kiev.debugResolve,"Type: resolving in super-type of "+tp),
			resolveNameR_3(tp,info)	// resolve in super-classes
		;	info.isForwardsAllowed(),
			trace(Kiev.debug && Kiev.debugResolve,"Type: resolving in forwards of "+tp),
			resolveNameR_4(tp,info)	// resolve in forwards
		}
	}
	private rule resolveNameR_1(ResInfo info)
		ASTNode@ n;
	{
		n @= tdecl.getContainerMembers(),
		n instanceof Field,
		info ?= n
	}
	private rule resolveNameR_3(Type tp, ResInfo info)
		TypeRef@ sup;
	{
		info.enterSuper(1, ResInfo.noForwards) : info.leaveSuper(),
		sup @= tdecl.super_types,
		sup.getTypeDecl(tenv.env).getMetaType(tenv.env).resolveNameAccessR(tp,info)
	}

	private rule resolveNameR_4(Type tp, ResInfo info)
		ASTNode@ forw;
		TypeRef@ sup;
	{
		forw @= tdecl.getContainerMembers(),
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).getType(tenv.env).applay(tp).resolveNameAccessR(info)
	;	info.isSuperAllowed(),
		sup @= tdecl.super_types,
		forw @= sup.getTypeDecl(tenv.env).getContainerMembers(),
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).getType(tenv.env).applay(tp).resolveNameAccessR(info)
	}

	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt)
		ASTNode@ member;
		TypeRef@ sup;
		Field@ forw;
	{
		tp.checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving method "+info.getName()+" in "+this),
		{
			member @= tdecl.getContainerMembers(),
			member instanceof Method,
			info ?= ((Method)member).equalsByCast(info.getName(),mt,tp,info)
		;
			info.isSuperAllowed(),
			info.enterSuper(1, ResInfo.noForwards) : info.leaveSuper(),
			sup @= tdecl.super_types,
			sup.getTypeDecl(tenv.env).getMetaType(tenv.env).resolveCallAccessR(tp,info,mt)
		;
			info.isForwardsAllowed(),
			member @= tdecl.getContainerMembers(),
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).getType(tenv.env).applay(tp).resolveCallAccessR(info,mt)
		;
			info.isForwardsAllowed(),
			sup @= tdecl.super_types,
			member @= sup.getTypeDecl(tenv.env).getContainerMembers(),
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).getType(tenv.env).applay(tp).resolveCallAccessR(info,mt)
		}
	}

}

public final class CoreMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	CoreType core_type;
	
	private static TypeDecl makeTypeDecl(StdTypes tenv, String name) {
		MetaTypeDecl tdecl = new MetaTypeDecl(new AHandle(), tenv.env.makeGlobalSymbol("kiev·stdlib·"+name));
		tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tenv.env.newPackage("kiev·stdlib").pkg_members.add(tdecl);
		return tdecl;
	}
	
	CoreMetaType(StdTypes tenv, String name, Type super_type, int flags) {
		super(tenv, makeTypeDecl(tenv,name), flags);
		if (super_type != null)
			this.tdecl.super_types.add(new TypeRef(super_type));
	}

	public CoreType getDefType() { core_type }

	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }

	public Type make(TVarBld set) {
		return core_type;
	}
	public Type applay(Type t, TVSet bindings) {
		return t;
	}
}

public final class ASTNodeMetaType extends MetaType {

	@virtual typedef TDecl = TypeDecl;

	public static final Hashtable<String,Class>	allNodes = new Hashtable<String,Class>(256);
	
	static void init() {
		allNodes.put("Any",					Object.class);
		allNodes.put("Node",				ANode.class);
		allNodes.put("Var",					Var.class);
		allNodes.put("Field",				Field.class);
		allNodes.put("StrConcat",			StringConcatExpr.class);
		allNodes.put("Set",					AssignExpr.class);
		allNodes.put("Modify",				ModifyExpr.class);
		allNodes.put("SetAccess",			ContainerAccessExpr.class);
		allNodes.put("InstanceOf",			InstanceofExpr.class);
		allNodes.put("RuleIstheExpr",		RuleIstheExpr.class);
		allNodes.put("RuleIsoneofExpr",		RuleIsoneofExpr.class);
		allNodes.put("UnaryOp",				UnaryExpr.class);
		allNodes.put("BinOp",				BinaryExpr.class);
		allNodes.put("Cmp",					BinaryBoolExpr.class);
		allNodes.put("Or",					BinaryBooleanOrExpr.class);
		allNodes.put("And",					BinaryBooleanAndExpr.class);
		allNodes.put("Not",					BooleanNotExpr.class);
		allNodes.put("Call",				CallExpr.class);
		allNodes.put("Cast",				CastExpr.class);
		allNodes.put("ENode",				ENode.class);
		allNodes.put("NoOp",				NopExpr.class);
		allNodes.put("AssertEnabled",		AssertEnabledExpr.class);
		allNodes.put("IFld",				IFldExpr.class);
		allNodes.put("SFld",				SFldExpr.class);
		allNodes.put("CBool",				ConstBoolExpr.class);
		allNodes.put("CInt",				ConstIntExpr.class);
		allNodes.put("CString",				ConstStringExpr.class);
		allNodes.put("EThis",				ThisExpr.class);
		allNodes.put("ESuper",				SuperExpr.class);
		allNodes.put("ASTRuleNode",			ASTRuleNode.class);
		allNodes.put("RuleIstheExpr",		RuleIstheExpr.class);
		allNodes.put("RuleIsoneofExpr",		RuleIsoneofExpr.class);
		allNodes.put("RuleCutExpr",			RuleCutExpr.class);
		allNodes.put("RuleCallExpr",		RuleCallExpr.class);
		allNodes.put("RuleExpr",			RuleExpr.class);
		allNodes.put("RuleWhileExpr",		RuleWhileExpr.class);
		allNodes.put("MacroAccessExpr",		MacroAccessExpr.class);
		allNodes.put("CmpNode",				MacroBinaryBoolExpr.class);
		allNodes.put("TypeDecl",			TypeDecl.class);
	}

	final public Class			clazz;
	final public String			name;

	private AttrSlot[]			values;
	private TypeAssign[]		types;
	private Field[]				fields;
	private TemplateTVarSet		templ_bindings;
	private boolean				built;

	public static ASTNodeMetaType instance(StdTypes tenv, Class clazz) {
		Symbol sym = tenv.symbolTDeclASTNodeType.makeGlobalSubSymbol("<ast-node-"+clazz.getName()+">");
		synchronized (tenv) { 
			MetaType mt = tenv.getExistingMetaType(sym);
			if (mt == null)
				mt = new ASTNodeMetaType(tenv, sym, clazz);
			return (ASTNodeMetaType)mt;
		}
	}

	private ASTNodeMetaType(StdTypes tenv, Symbol tdecl_symbol, Class clazz) {
		super(tenv, tdecl_symbol, 0);
		this.templ_bindings = TemplateTVarSet.emptySet;
		this.clazz = clazz;
		foreach (String key; allNodes.keys(); clazz.equals(allNodes.get(key))) {
			this.name = key;
			break;
		}
	}

	public XType getDefType() { null }

	@getter public TDecl get$tdecl() { (TypeDecl)tenv.symbolTDeclASTNodeType.dnode }

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}

	public Type make(TVarBld set) {
		throw new RuntimeException("make() in ASTNodeMetaType");
	}

	public Type applay(Type t, TVSet bindings) {
		return t;
	}

	public TemplateTVarSet getTemplBindings() {
		if (!built)
			makeTemplBindings();
		return templ_bindings;
	}

	private void makeTemplBindings() {
		if (ANode.class.isAssignableFrom(clazz)) {
			Class sup = clazz.getSuperclass();
			ASTNodeMetaType ast_sup;
			if (sup != null)
				ast_sup = ASTNodeMetaType.instance(tenv, sup);
			else
				ast_sup = ASTNodeMetaType.instance(tenv, Object.class);
			ast_sup.getTemplBindings();
			java.lang.reflect.Field rf = clazz.getDeclaredField("$values");
			rf.setAccessible(true);
			this.values = (AttrSlot[])rf.get(null);
			this.types = new TypeAssign[values.length];
			this.fields = new Field[values.length];
			int n = values.length - ast_sup.values.length;
			for (int i=0; i < values.length; i++) {
				if (i < n ) {
					AttrSlot a = values[i];
					types[i] = new TypeAssign(new Symbol("attr$"+a.name+"$type"), new ASTNodeType(a.typeinfo.clazz));
					fields[i] = new Field(a.name,new ASTNodeType(a.typeinfo.clazz),ACC_PUBLIC);
				} else {
					assert(values[i] == ast_sup.values[i-n]);
					types[i] = ast_sup.types[i-n];
					fields[i] = ast_sup.fields[i-n];
				}
			}
		} else {
			values = AttrSlot.emptyArray;
			types = new TypeAssign[0];
			fields = new Field[0];
		}
		TVarBld vs = new TVarBld();
		foreach (TypeAssign ta; this.types /*; ta.sname.matches("attr\\$.*\\$type")*/)
			vs.append(ta.getAType(tenv.env), null);
		templ_bindings = new TemplateTVarSet(-1, vs);
		built = true;
	}

	public rule resolveNameAccessR(Type tp, ResInfo info)
	{
		getTemplBindings(),
		info @= this.fields
	}

	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt) { false }

}

public final class XMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private TemplateTVarSet			templ_bindings;
	private XType					xtype;
	
	public XMetaType(StdTypes tenv, MetaTypeDecl clazz, int flags) {
		super(tenv, clazz, flags);
		this.xtype = new XType(this, null, null);
	}
	
	public XType getDefType() { xtype }
	
	public Type make(TVarBld set) {
		if (set == null)
			return new XType(this, null, null);
		else
			return new XType(this, getTemplBindings(), set);
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		TVarBld vs = t.bindings().applay_bld(bindings);
		if (vs == null)
			return t;
		return new XType(this, getTemplBindings(), vs);
	}

	protected void callbackTypeVersionChanged() {
		templ_bindings = null;
	}

	public TemplateTVarSet getTemplBindings() {
		if (this.templ_bindings == null)
			makeTemplBindings();
		return templ_bindings;
	}
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; tdecl.args)
			vs.append(ad.getAType(tenv.env), null);
		foreach (TypeDef td; tdecl.getContainerMembers()) {
			if (td instanceof TypeAssign && td.sname != "This")
				vs.append(td.getAType(tenv.env), td.type_ref.getType(tenv.env));
			else
				vs.append(td.getAType(tenv.env), null);
		}
		int n_free = vs.getArgsLength();
		foreach (TypeRef st; tdecl.super_types; st.getType(tenv.env) ≢ null) {
			Type stp = st.getType(tenv.env);
			vs.append(stp.bindings());
			if ((stp.meta_type.flags & flReference) != 0) this.flags |= flReference;
			if ((stp.meta_type.flags & flArray)     != 0) this.flags |= flArray;
		}
		if (vs.tvars.length == 0)
			templ_bindings = TemplateTVarSet.emptySet;
		else
			templ_bindings = new TemplateTVarSet(n_free, vs);
	}

}

public final class CompaundMetaType extends MetaType {

	@virtual typedef TDecl = Struct;

	private TemplateTVarSet			templ_bindings;
	private CompaundType			ctype;

	@getter
	public final TDecl get$tdecl() {
		TDecl td = (TDecl)this.tdecl_symbol.dnode;
		if (td == null) {
			td = (TDecl)tenv.env.loadTypeDecl(this.tdecl_symbol.qname(), true);
			if (td != null) {
				assert (this.tdecl_symbol.dnode == td);
			}
		}
		return td;
	}
	
	public static CompaundMetaType newCompaundMetaType(StdTypes tenv, String clazz_name) operator "new T" {
		synchronized (tenv) {
			Symbol sym = tenv.env.makeGlobalSymbol(clazz_name);
			MetaType mt = tenv.getExistingMetaType(sym);
			if (mt == null)
				mt = new CompaundMetaType(tenv, sym);
			return (CompaundMetaType)mt;
		}
 	}
	
	public static CompaundMetaType newCompaundMetaType(StdTypes tenv, Struct clazz) operator "new T" {
		synchronized (tenv) {
			Symbol sym = clazz.symbol;
			MetaType mt = tenv.getExistingMetaType(sym);
			if (mt == null)
				mt = new CompaundMetaType(tenv, sym);
			return (CompaundMetaType)mt;
		}
	}
	
	private CompaundMetaType(StdTypes tenv, Symbol clazz_symbol) {
		super(tenv, clazz_symbol, MetaType.flReference);
		this.templ_bindings = null; //TemplateTVarSet.emptySet;
		this.ctype = new CompaundType(this, TemplateTVarSet.emptySet, TVarBld.emptySet);
	}
	
	public CompaundType getDefType() { ctype }
	
	public String qname() {
		return tdecl_symbol.qname();
	}
	
	public Type make(TVarBld set) {
		if (set == null)
			return new CompaundType(this, null, null);
		else
			return new CompaundType(this, getTemplBindings(), set);
	}

	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		TVarBld vs = t.bindings().applay_bld(bindings);
		if (vs == null)
			return t;
		return new CompaundType(this, getTemplBindings(), vs);
	}
	
	protected void callbackTypeVersionChanged() {
		templ_bindings = null;
	}

	public TemplateTVarSet getTemplBindings() {
		if (this.templ_bindings == null)
			makeTemplBindings();
		return templ_bindings;
	}
	
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; tdecl.args)
			vs.append(ad.getAType(tenv.env), null);
		foreach (TypeDef td; tdecl.getContainerMembers()) {
			if (td instanceof TypeAssign && td.sname != "This")
				vs.append(td.getAType(tenv.env), td.type_ref.getType(tenv.env));
			else
				vs.append(td.getAType(tenv.env), null);
		}
		int n_free = vs.getArgsLength();
		foreach (TypeRef st; tdecl.super_types; st.getType(tenv.env) ≢ null) {
			vs.append(st.getType(tenv.env).bindings());
		}
		if (vs.tvars.length == 0)
			templ_bindings = TemplateTVarSet.emptySet;
		else
			templ_bindings = new TemplateTVarSet(n_free, vs);
	}

}

public final class ArrayMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	ArrayMetaType(StdTypes tenv, TypeDecl tdecl) {
		super(tenv, tdecl, MetaType.flArray | MetaType.flReference);
	}

	public TemplateTVarSet getTemplBindings() { return tenv.arrayTemplBindings; }

	public ArrayType getDefType() { tenv.tpArrayOfAny }

	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolArrayTDecl.dnode }

	public Type make(TVarBld set) {
		return ArrayType.newArrayType(set.resolve(tenv.tpArrayArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() || bindings.getArgsLength() == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.applay(bindings));
	}
	
}

public final class VarargMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	VarargMetaType(StdTypes tenv, TypeDecl tdecl) {
		super(tenv, tdecl, MetaType.flArray | MetaType.flReference);
	}

	public TemplateTVarSet getTemplBindings() { return tenv.varargTemplBindings; }

	public VarargType getDefType() { tenv.tpVarargOfAny }

	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolVarargTDecl.dnode }

	public Type make(TVarBld set) {
		return VarargType.newVarargType(set.resolve(tenv.tpVarargArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() || bindings.getArgsLength() == 0 ) return t;
		return VarargType.newVarargType(((VarargType)t).arg.applay(bindings));
	}
	
}

public class ArgMetaType extends MetaType {

	@virtual typedef TDecl = TypeDecl;

	public final ArgType atype;
	
	public ArgMetaType(StdTypes tenv, TypeDef definer) {
		super(tenv, definer, MetaType.flReference);
		atype = new ArgType(this);
	}

	public ArgType getDefType() { atype }

	public Type[] getMetaSupers(Type tp) {
		ArgType at = (ArgType)tp;
		TypeRef[] ups = tdecl.super_types;
		if (ups.length == 0)
			return new Type[]{tenv.tpObject};
		Type[] stps = new Type[ups.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = ups[i].getType(tenv.env);
		return stps;
	}

	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }

	public Type make(TVarBld set) {
		return atype;
	}
	public Type applay(Type t, TVSet bindings) {
		return bindings.resolve((ArgType)t);
	}

	public rule resolveNameAccessR(Type tp, ResInfo info)
		TypeRef@ sup;
	{
		sup @= tdecl.super_types,
		sup.getType(tenv.env).resolveNameAccessR(info)
	}

	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt)
		TypeRef@ sup;
	{
		tdecl.super_types.length == 0,
		tenv.tpObject.resolveCallAccessR(info, mt)
	;	sup @= tdecl.super_types,
		sup.getType(tenv.env).resolveCallAccessR(info, mt)
	}
}

public class WildcardCoMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	WildcardCoMetaType(StdTypes tenv, TypeDecl td) {
		super(tenv, td, MetaType.flReference | MetaType.flWrapper);
	}

	public Type[] getMetaSupers(Type tp) {
		WildcardCoType wt = (WildcardCoType)tp;
		return new Type[]{tp.getEnclosedType()};
	}

	public TemplateTVarSet getTemplBindings() { return tenv.wildcardCoTemplBindings; }

	public WildcardCoType getDefType() { tenv.wildcardCoOfAny }

	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolWildcardCoTDecl.dnode }

	public Type make(TVarBld set) {
		return new WildcardCoType(set.resolve(tenv.tpWildcardCoArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new WildcardCoType(((WildcardCoType)t).getEnclosedType().applay(bindings));
	}
}

public class WildcardContraMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	WildcardContraMetaType(StdTypes tenv, TypeDecl td) {
		super(tenv, td, MetaType.flReference | MetaType.flWrapper);
	}

	public Type[] getMetaSupers(Type tp) {
		WildcardContraType wt = (WildcardContraType)tp;
		return new Type[]{tp.getEnclosedType()};
	}

	public TemplateTVarSet getTemplBindings() { return tenv.wildcardContraTemplBindings; }

	public WildcardContraType getDefType() { tenv.tpWildcardContraOfAny }

	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolWildcardContraTDecl.dnode }

	public Type make(TVarBld set) {
		return new WildcardContraType(set.resolve(tenv.tpWildcardContraArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new WildcardContraType(((WildcardContraType)t).getEnclosedType().applay(bindings));
	}
}

public class WrapperMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	public final TypeDecl	clazz;
	public final Field		field;
	
	public static WrapperMetaType instance(StdTypes tenv, Type tp) {
		TypeDecl td = tp.meta_type.tdecl;
		Symbol sym = td.symbol.makeSubSymbol("<wrapper-meta-type>");
		synchronized (tenv) { 
			MetaType mt = tenv.getExistingMetaType(sym);
			if (mt != null)
				return (WrapperMetaType)mt;
			return new WrapperMetaType(tenv, td, sym);
		}
	}
	WrapperMetaType(StdTypes tenv) {
		super(tenv, tenv.symbolWrapperTDecl, MetaType.flReference | MetaType.flWrapper);
	}
	private WrapperMetaType(StdTypes tenv, TypeDecl td, Symbol tdecl_symbol) {
		super(tenv, tdecl_symbol, MetaType.flReference | MetaType.flWrapper);
		this.clazz = td.checkResolved(tenv.env);
		this.field = getWrappedField(this.clazz,false,tenv);
	}

	public Type[] getMetaSupers(Type tp) {
		WrapperType wt = (WrapperType)tp;
		return new Type[]{tp.getEnclosedType(),tp.getUnboxedType()};
	}

	public TemplateTVarSet getTemplBindings() { return tenv.wrapperTemplBindings; }

	public WrapperType getDefType() { null }

	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolWrapperTDecl.dnode }

	public Type make(TVarBld set) {
		return WrapperType.newWrapperType(set.resolve(tenv.tpWrapperArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().applay(bindings));
	}

	private static Field getWrappedField(TypeDecl td, boolean required, StdTypes tenv) {
		foreach (TypeRef st; td.super_types; st.getTypeDecl(tenv.env) != null) {
			Field wf = getWrappedField(st.getTypeDecl(tenv.env), false, tenv);
			if (wf != null)
				return wf;
		}
		Field wf = null;
		foreach(Field n; td.getContainerMembers(); n.isForward()) {
			if (wf == null)
				wf = (Field)n;
			else
				throw new CompilerException(n,"Wrapper class with multiple forward fields");
		}
		if ( wf == null ) {
			if (required)
				throw new CompilerException(td,"Wrapper class "+td+" has no forward field");
			return null;
		}
		if( Kiev.verbose ) System.out.println("Class "+td+" is a wrapper for field "+wf);
		return wf;
	}
	
	public rule resolveNameAccessR(Type tp, ResInfo info)
	{
		info.isForwardsAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in wrapper type "+tp),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveNameAccessR(info)
		;	info.enterSuper(10) : info.leaveSuper(10),
			((WrapperType)tp).getEnclosedType().resolveNameAccessR(info)
		}
	;
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		((WrapperType)tp).getEnclosedType().resolveNameAccessR(info)
	}

	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt)
	{
		info.isForwardsAllowed(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving method "+info.getName()+" in wrapper type "+this),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveCallAccessR(info, mt)
		;	info.enterSuper(10) : info.leaveSuper(10),
			((WrapperType)tp).getEnclosedType().resolveCallAccessR(info, mt)
		}
	;
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		((WrapperType)tp).getEnclosedType().resolveCallAccessR(info, mt)
	}
	
}

public final class TupleMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private final TemplateTVarSet		templ_bindings;
	public  final int					arity;
	
	TupleMetaType(StdTypes tenv, Symbol tdecl_symbol, int arity) {
		super(tenv, tdecl_symbol, 0);
		this.arity = arity;
		TVarBld bld = new TVarBld();
		for (int i=0; i < arity; i++)
			bld.append(tenv.tpCallParamArgs[i], null);
		this.templ_bindings = new TemplateTVarSet(-1, bld);
	}

	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolTupleTDecl.dnode }

	public TupleType getDefType() { null }

	public Type make(TVarBld set) {
		return new TupleType(this, set);
	}

	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		TVarBld vs = t.bindings().applay_bld(bindings);
		if (vs == null)
			return t;
		return new TupleType(this, vs);
	}
	
	public TemplateTVarSet getTemplBindings() {
		return templ_bindings;
	}
}

public class CallMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private TemplateTVarSet		templ_bindings;
	
	@getter public TDecl get$tdecl() { (MetaTypeDecl)tenv.symbolCallTDecl.dnode }
	
	public static CallMetaType newCallMetaType(StdTypes tenv, boolean is_static, boolean is_closure, ArgType[] targs) {
		if (targs == null || targs.length == 0) {
			if (is_static) {
				if (is_closure)
					return tenv.closure_static_instance;
				else
					return tenv.call_static_instance;
			} else {
				if (is_closure)
					return tenv.closure_this_instance;
				else
					return tenv.call_this_instance;
			}
		}
		TVarBld set = new TVarBld();
		set.append(tenv.tpCallRetArg, null);
		set.append(tenv.tpCallTupleArg, null);
		if (!is_static)
			set.append(tenv.tpSelfTypeArg, null);
		foreach (ArgType ta; targs)
			set.append(ta, null);
		int flags = MetaType.flCallable;
		if (is_closure)
			flags |= MetaType.flReference;
		CallMetaType cmt = new CallMetaType(tenv, new TemplateTVarSet(-1, set), flags);
		return cmt;
	}
	
	CallMetaType(StdTypes tenv, TemplateTVarSet templ_bindings, int flags) {
		super(tenv, tenv.symbolCallTDecl, flags);
		this.templ_bindings = templ_bindings;
	}

	public CallType getDefType() { null }

	public Type[] getMetaSupers(Type tp) {
		CallType ct = (CallType)tp;
		if (ct.isReference())
			return new Type[]{tenv.tpObject};
		return Type.emptyArray;
	}

	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() /*|| bindings.getArgsLength() == 0*/ ) return t;
		TVarBld vs = t.bindings().applay_bld(bindings);
		if (vs == null)
			return t;
		CallType mt = (CallType)t;
		mt = new CallType((CallMetaType)mt.meta_type, vs, mt.arity);
		return mt;
	}

	public rule resolveNameAccessR(Type tp, ResInfo info) { false }
	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt) { false }

	public TemplateTVarSet getTemplBindings() {
		return templ_bindings;
	}
}


