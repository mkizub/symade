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

	public TDecl				tdecl;
	public int					flags;

	@getter public TDecl get$tdecl() {
		return this.tdecl;
	}
	
	@setter final void set$tdecl(TDecl tdecl) {
		this.tdecl = tdecl;
	}
	
	public String qname() {
		if (tdecl != null)
			return tdecl.qname();
		return "<tdecl>";
	}
	
	public MetaType(TDecl tdecl, int flags) {
		this.tdecl = tdecl;
		this.flags = flags;
	}

	public abstract TemplateTVarSet getTemplBindings();
	
	public void callbackTypeVersionChanged() { /* ignore */ }

	public Type[] getMetaSupers(Type tp) {
		if (tdecl.super_types.length == 0)
			return Type.emptyArray;
		Type[] stps = new Type[tdecl.super_types.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = tdecl.super_types[i].getType().applay(tp);
		return stps;
	}

	public Type make(TVarBld set) {
		if (set == null)
			return new XType(this, null, null);
		else
			return new XType(this, getTemplBindings(), set);
	}
	public Type rebind(Type t, TVarBld set) {
		throw new RuntimeException("rebind() in DummyType");
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new XType(this, getTemplBindings(), t.bindings().applay_bld(bindings));
	}

	public rule resolveNameAccessR(Type tp, ResInfo info)
	{
		trace(Kiev.debug && Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in "+tp),
		tdecl.checkResolved(),
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
		n @= tdecl.getMembers(),
		n instanceof Field,
		info ?= n
	}
	private rule resolveNameR_3(Type tp, ResInfo info)
		TypeRef@ sup;
	{
		info.enterSuper(1, ResInfo.noForwards) : info.leaveSuper(),
		sup @= tdecl.super_types,
		sup.getTypeDecl().xmeta_type.resolveNameAccessR(tp,info)
	}

	private rule resolveNameR_4(Type tp, ResInfo info)
		ASTNode@ forw;
		TypeRef@ sup;
	{
		forw @= tdecl.getMembers(),
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).getType().applay(tp).resolveNameAccessR(info)
	;	info.isSuperAllowed(),
		sup @= tdecl.super_types,
		forw @= sup.getTypeDecl().getMembers(),
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).getType().applay(tp).resolveNameAccessR(info)
	}

	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt)
		ASTNode@ member;
		TypeRef@ sup;
		Field@ forw;
	{
		tp.checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving method "+info.getName()+" in "+this),
		{
			member @= tdecl.getMembers(),
			member instanceof Method,
			info ?= ((Method)member).equalsByCast(info.getName(),mt,tp,info)
		;
			info.isSuperAllowed(),
			info.enterSuper(1, ResInfo.noForwards) : info.leaveSuper(),
			sup @= tdecl.super_types,
			sup.getTypeDecl().xmeta_type.resolveCallAccessR(tp,info,mt)
		;
			info.isForwardsAllowed(),
			member @= tdecl.getMembers(),
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).getType().applay(tp).resolveCallAccessR(info,mt)
		;
			info.isForwardsAllowed(),
			sup @= tdecl.super_types,
			member @= sup.getTypeDecl().getMembers(),
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).getType().applay(tp).resolveCallAccessR(info,mt)
		}
	}

}

public final class CoreMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	CoreType core_type;
	
	private static TypeDecl makeTypeDecl(String name) {
		MetaTypeDecl tdecl = new MetaTypeDecl(null);
		tdecl.sname = name;
		tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(tdecl);
		return tdecl;
	}
	
	CoreMetaType(String name, Type super_type, int flags) {
		super(makeTypeDecl(name), flags);
		this.tdecl.xmeta_type = this;
		if (super_type != null)
			this.tdecl.super_types.add(new TypeRef(super_type));
	}

	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }

	public Type make(TVarBld set) {
		return core_type;
	}
	public Type rebind(Type t, TVarBld set) {
		throw new RuntimeException("rebind() in CoreType");
	}
	public Type applay(Type t, TVSet bindings) {
		return t;
	}
}

public final class ASTNodeMetaType extends MetaType {

	@virtual typedef TDecl = Struct;

	public static Hashtable<Class,ASTNodeMetaType> allASTNodeMetaTypes;
	public static final Hashtable<String,Class>	allNodes;
	static {
		allASTNodeMetaTypes = new Hashtable<Class,ASTNodeMetaType>();
		allNodes = new Hashtable<String,Class>(256);
		allNodes.put("Any",					Object.class);
		allNodes.put("Node",				ANode.class);
		allNodes.put("Field",				Field.class);
		allNodes.put("StrConcat",			StringConcatExpr.class);
		allNodes.put("Set",					AssignExpr.class);
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
	private boolean            built;

	public static ASTNodeMetaType instance(Class clazz) {
		ASTNodeMetaType mt = allASTNodeMetaTypes.get(clazz);
		if (mt != null)
			return mt;
		mt = new ASTNodeMetaType(clazz);
		return mt;
	}

	ASTNodeMetaType(Class clazz) {
		super(StdTypes.tdASTNodeType,0);
		this.templ_bindings = TemplateTVarSet.emptySet;
		this.clazz = clazz;
		allASTNodeMetaTypes.put(clazz,this);
		foreach (String key; allNodes.keys(); clazz.equals(allNodes.get(key))) {
			this.name = key;
			break;
		}
	}

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}

	public Type make(TVarBld set) {
		throw new RuntimeException("make() in ASTNodeMetaType");
	}
	public Type rebind(Type t, TVarBld set) {
		throw new RuntimeException("rebind() in ASTNodeMetaType");
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
				ast_sup = ASTNodeMetaType.instance(sup);
			else
				ast_sup = ASTNodeMetaType.instance(Object.class);
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
					types[i] = new TypeAssign("attr$"+a.name+"$type", new ASTNodeType(a.clazz));
					fields[i] = new Field(a.name,new ASTNodeType(a.clazz),ACC_PUBLIC);
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
			vs.append(ta.getAType(), null);
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
	
	public XMetaType(MetaTypeDecl clazz, int flags) {
		super(clazz, flags);
	}
	
	public Type make(TVarBld set) {
		if (set == null)
			return new XType(this, null, null);
		else
			return new XType(this, getTemplBindings(), set);
	}
	public Type rebind(Type t, TVarBld set) {
		throw new RuntimeException("rebind() in DummyType");
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new XType(this, getTemplBindings(), t.bindings().applay_bld(bindings));
	}

	public void callbackTypeVersionChanged() {
		templ_bindings = null;
	}

	public TemplateTVarSet getTemplBindings() {
		assert (tdecl.xmeta_type == this);
		if (this.templ_bindings == null)
			makeTemplBindings();
		return templ_bindings;
	}
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; tdecl.args)
			vs.append(ad.getAType(), null);
		foreach (TypeDef td; tdecl.getMembers()) {
			if (td instanceof TypeAssign && td.sname != "This")
				vs.append(td.getAType(), td.type_ref.getType());
			else
				vs.append(td.getAType(), null);
		}
		int n_free = vs.getArgsLength();
		foreach (TypeRef st; tdecl.super_types; st.getType() ≢ null) {
			Type stp = st.getType();
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

	private static Hashtable<String,CompaundMetaType> compaundMetaTypes = new Hashtable<String,CompaundMetaType>();
	
	private String					descr;
	private TemplateTVarSet			templ_bindings;
	
	@getter
	public final TDecl get$tdecl() {
		if (this.tdecl == null) {
			TDecl td = (TDecl)Env.getRoot().loadTypeDecl(descr, true);
			if (td != null) {
				descr = null;
				this.tdecl = td;
				assert (td.xmeta_type == this);
			}
		}
		return this.tdecl;
	}
	
	public static void checkNotDeferred(Struct clazz) {
		String qname = clazz.qname();
		if (qname != null)
			assert (compaundMetaTypes.get(qname) == null);
	}
	
	public static CompaundMetaType newCompaundMetaType(String clazz_name) alias lfy operator new {
		TypeDecl td = (TypeDecl)Env.getRoot().resolveGlobalDNode(clazz_name);
		if (td != null) {
			assert (td.xmeta_type != null);
			return (CompaundMetaType)td.xmeta_type;
		}
		CompaundMetaType mt = compaundMetaTypes.get(clazz_name);
		if (mt != null)
			return mt;
		mt = new CompaundMetaType(clazz_name);
		compaundMetaTypes.put(clazz_name,mt);
		return mt;
	}
	
	public static CompaundMetaType newCompaundMetaType(Struct clazz) alias lfy operator new {
		assert (clazz.xmeta_type == null);
		String qname = clazz.qname();
		if (qname != null) {
			CompaundMetaType mt = compaundMetaTypes.get(qname);
			if (mt != null) {
				compaundMetaTypes.remove(qname);
				return mt;
			}
		}
		return new CompaundMetaType(clazz);
	}
	
	private CompaundMetaType(String clazz_name) {
		super(null, MetaType.flReference);
		this.descr = clazz_name;
		this.templ_bindings = TemplateTVarSet.emptySet;
	}
	
	private CompaundMetaType(Struct clazz) {
		super(clazz, MetaType.flReference);
		this.templ_bindings = TemplateTVarSet.emptySet;
	}
	
	public String qname() {
		if (descr != null)
			return descr;
		return tdecl.qname();
	}
	
	public Type make(TVarBld set) {
		if (set == null)
			return new CompaundType(this, null, null);
		else
			return new CompaundType(this, getTemplBindings(), set);
	}

	public Type rebind(Type t, TVarBld set) {
		return new CompaundType(this, getTemplBindings(), t.bindings().rebind_bld(set));
	}
	
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new CompaundType(this, getTemplBindings(), t.bindings().applay_bld(bindings));
	}
	
	public void callbackTypeVersionChanged() {
		templ_bindings = null;
	}

	public TemplateTVarSet getTemplBindings() {
		assert (tdecl.xmeta_type == this);
		if (this.templ_bindings == null)
			makeTemplBindings();
		return templ_bindings;
	}
	
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; tdecl.args)
			vs.append(ad.getAType(), null);
		foreach (TypeDef td; tdecl.getMembers()) {
			if (td instanceof TypeAssign && td.sname != "This")
				vs.append(td.getAType(), td.type_ref.getType());
			else
				vs.append(td.getAType(), null);
		}
		int n_free = vs.getArgsLength();
		foreach (TypeRef st; tdecl.super_types; st.getType() ≢ null) {
			vs.append(st.getType().bindings());
		}
		if (vs.tvars.length == 0)
			templ_bindings = TemplateTVarSet.emptySet;
		else
			templ_bindings = new TemplateTVarSet(n_free, vs);
	}

}

public final class ArrayMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private static final TemplateTVarSet	templ_bindings;
	public static final ArrayMetaType		instance;
	static {
		templ_bindings = new TemplateTVarSet(-1, new TVarBld(StdTypes.tpArrayArg, null));
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.getRoot().resolveGlobalDNode("kiev·stdlib·_array_");
		assert  (tdecl == null);
		tdecl = new MetaTypeDecl(null);
		tdecl.sname = "_array_";
		tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.super_types.insert(0, new TypeRef(StdTypes.tpObject));
		tdecl.args.add(StdTypes.tdArrayArg);
		tdecl.uuid = "bbf03b4b-62d4-3e29-8f0d-acd6c47b9a04";
		instance = new ArrayMetaType(tdecl);
		tdecl.xmeta_type = instance;
		tdecl.xtype = ArrayType.newArrayType(Type.tpAny);
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(tdecl);
		Field length = new Field("length", StdTypes.tpInt, ACC_PUBLIC|ACC_FINAL|ACC_MACRO|ACC_NATIVE);
		length.setMeta(new MetaAccess("public",0xAA)); //public:ro
		tdecl.members.add(length);
		Method get = new MethodImpl("get", StdTypes.tpArrayArg, ACC_PUBLIC|ACC_MACRO|ACC_NATIVE);
		get.params.add(new LVar(0,"idx",StdTypes.tpInt,Var.PARAM_NORMAL,0));
		get.aliases += new ASTOperatorAlias(Constants.nameArrayGetOp);
		//get.body = CoreExpr.makeInstance("");
		tdecl.members.add(get);
		
	}
	private ArrayMetaType(TypeDecl tdecl) {
		super(tdecl, MetaType.flArray | MetaType.flReference);
	}

	public TemplateTVarSet getTemplBindings() { return templ_bindings; }

	public Type make(TVarBld set) {
		return ArrayType.newArrayType(set.resolve(StdTypes.tpArrayArg));
	}
	public Type rebind(Type t, TVarBld set) {
		return ArrayType.newArrayType(t.bindings().rebind_bld(set).resolve(StdTypes.tpArrayArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() || bindings.getArgsLength() == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.applay(bindings));
	}
	
}

public class ArgMetaType extends MetaType {

	@virtual typedef TDecl = TypeDecl;

	public final ArgType atype;
	
	public ArgMetaType(TypeDef definer) {
		super(definer, MetaType.flReference);
		atype = new ArgType(this);
		definer.xtype = atype;
	}

	public Type[] getMetaSupers(Type tp) {
		ArgType at = (ArgType)tp;
		TypeRef[] ups = tdecl.super_types;
		if (ups.length == 0)
			return new Type[]{StdTypes.tpObject};
		Type[] stps = new Type[ups.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = ups[i].getType();
		return stps;
	}

	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }

	public Type make(TVarBld set) {
		return atype;
	}
	public Type rebind(Type t, TVarBld set) {
		return t; //throw new RuntimeException("bind() in ArgType");
	}
	public Type applay(Type t, TVSet bindings) {
		return bindings.resolve((ArgType)t);
	}

	public rule resolveNameAccessR(Type tp, ResInfo info)
		TypeRef@ sup;
	{
		sup @= tdecl.super_types,
		sup.getType().resolveNameAccessR(info)
	}

	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt)
		TypeRef@ sup;
	{
		tdecl.super_types.length == 0,
		StdTypes.tpObject.resolveCallAccessR(info, mt)
	;	sup @= tdecl.super_types,
		sup.getType().resolveCallAccessR(info, mt)
	}
}

public class WildcardCoMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private static final TemplateTVarSet		templ_bindings;
	public static final WildcardCoMetaType		instance;
	static {
		templ_bindings = new TemplateTVarSet(-1, new TVarBld(StdTypes.tpWildcardCoArg, null));
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.getRoot().resolveGlobalDNode("kiev·stdlib·_wildcard_co_variant_");
		assert (tdecl == null);
		tdecl = new MetaTypeDecl(null);
		tdecl.sname = "_wildcard_co_variant_";
		tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.super_types.insert(0, new TypeRef(StdTypes.tpAny));
		tdecl.args.add(StdTypes.tdWildcardCoArg);
		tdecl.uuid = "6c99b10d-3003-3176-8086-71be6cee5c51";
		instance = new WildcardCoMetaType(tdecl);
		tdecl.xmeta_type = instance;
		tdecl.xtype = new WildcardCoType(Type.tpAny);
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(tdecl);
	}

	private WildcardCoMetaType(TypeDecl td) {
		super(td, MetaType.flReference | MetaType.flWrapper);
	}

	public Type[] getMetaSupers(Type tp) {
		WildcardCoType wt = (WildcardCoType)tp;
		return new Type[]{tp.getEnclosedType()};
	}
	public TemplateTVarSet getTemplBindings() { return templ_bindings; }


	public Type make(TVarBld set) {
		return new WildcardCoType(set.resolve(StdTypes.tpWildcardCoArg));
	}
	public Type rebind(Type t, TVarBld set) {
		return new WildcardCoType(((WildcardCoType)t).getEnclosedType().rebind(set));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new WildcardCoType(((WildcardCoType)t).getEnclosedType().applay(bindings));
	}
}

public class WildcardContraMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private static final TemplateTVarSet			templ_bindings;
	public static final WildcardContraMetaType		instance;
	static {
		templ_bindings = new TemplateTVarSet(-1, new TVarBld(StdTypes.tpWildcardContraArg, null));
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.getRoot().resolveGlobalDNode("kiev·stdlib·_wildcard_contra_variant_");
		assert (tdecl == null);
		tdecl = new MetaTypeDecl(null);
		tdecl.sname = "_wildcard_contra_variant_";
		tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.super_types.insert(0, new TypeRef(StdTypes.tpAny));
		tdecl.args.add(StdTypes.tdWildcardContraArg);
		tdecl.uuid = "933ac6b8-4d03-3799-9bb3-3c9bc1883707";
		instance = new WildcardContraMetaType(tdecl);
		tdecl.xmeta_type = instance;
		tdecl.xtype = new WildcardContraType(Type.tpAny);
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(tdecl);
	}

	private WildcardContraMetaType(TypeDecl td) {
		super(td, MetaType.flReference | MetaType.flWrapper);
	}

	public Type[] getMetaSupers(Type tp) {
		WildcardContraType wt = (WildcardContraType)tp;
		return new Type[]{tp.getEnclosedType()};
	}
	public TemplateTVarSet getTemplBindings() { return templ_bindings; }


	public Type make(TVarBld set) {
		return new WildcardContraType(set.resolve(StdTypes.tpWildcardContraArg));
	}
	public Type rebind(Type t, TVarBld set) {
		return new WildcardContraType(((WildcardContraType)t).getEnclosedType().rebind(set));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new WildcardContraType(((WildcardContraType)t).getEnclosedType().applay(bindings));
	}
}

public class WrapperMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private static final TemplateTVarSet	templ_bindings;
	public static final MetaTypeDecl		wrapper_tdecl;
	static {
		templ_bindings = new TemplateTVarSet(-1, new TVarBld(StdTypes.tpWrapperArg, null));
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.getRoot().resolveGlobalDNode("kiev·stdlib·_wrapper_");
		assert (tdecl == null);
		tdecl = new MetaTypeDecl(null);
		tdecl.sname = "_wrapper_";
		tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.super_types.insert(0, new TypeRef(StdTypes.tpObject));
		tdecl.args.add(StdTypes.tdWrapperArg);
		tdecl.uuid = "67544053-836d-3bac-b94d-0c4b14ae9c55";
		wrapper_tdecl = tdecl;
		tdecl.xmeta_type = WrapperMetaType.instance(StdTypes.tpWrapperArg);
		tdecl.xtype = WrapperType.newWrapperType(StdTypes.tpWrapperArg);
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(tdecl);
	}
	
	public final TypeDecl	clazz;
	public final Field		field;
	
	public static WrapperMetaType instance(Type tp) {
		TypeDecl td = tp.meta_type.tdecl;
		if (td.wmeta_type == null)
			td.wmeta_type = new WrapperMetaType(td);
		return td.wmeta_type;
	}
	private WrapperMetaType(TypeDecl td) {
		super(wrapper_tdecl, MetaType.flReference | MetaType.flWrapper);
		this.clazz = td;
		clazz.checkResolved();
		this.field = getWrappedField(td,false);
	}

	public Type[] getMetaSupers(Type tp) {
		WrapperType wt = (WrapperType)tp;
		return new Type[]{tp.getEnclosedType(),tp.getUnboxedType()};
	}
	public TemplateTVarSet getTemplBindings() { return templ_bindings; }


	public Type make(TVarBld set) {
		return WrapperType.newWrapperType(set.resolve(StdTypes.tpWrapperArg));
	}
	public Type rebind(Type t, TVarBld set) {
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().rebind(set));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().applay(bindings));
	}

	private static Field getWrappedField(TypeDecl td, boolean required) {
		foreach (TypeRef st; td.super_types; st.getTypeDecl() != null) {
			Field wf = getWrappedField(st.getTypeDecl(), false);
			if (wf != null)
				return wf;
		}
		Field wf = null;
		foreach(Field n; td.getMembers(); n.isForward()) {
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

	public static MetaTypeDecl    tuple_tdecl;
	public static TupleMetaType[] instancies;
	static {
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.getRoot().resolveGlobalDNode("kiev·stdlib·_tuple_");
		assert (tdecl == null);
		tuple_tdecl = new MetaTypeDecl(null);
		tuple_tdecl.sname = "_tuple_";
		tuple_tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tuple_tdecl.super_types.add(new TypeRef(StdTypes.tpAny));
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(tuple_tdecl);

		instancies = new TupleMetaType[128];
		for (int i=0; i < instancies.length; i++)
			instancies[i] = new TupleMetaType(tuple_tdecl,i);

		tuple_tdecl.xmeta_type = instancies[0];
		tuple_tdecl.xtype = new TupleType(instancies[0],TVarBld.emptySet);
	}
	
	private final TemplateTVarSet		templ_bindings;
	public  final int					arity;
	
	private TupleMetaType(TypeDecl tdecl, int arity) {
		super(tdecl, 0);
		this.arity = arity;
		TVarBld bld = new TVarBld();
		for (int i=0; i < arity; i++)
			bld.append(StdTypes.tpCallParamArgs[i], null);
		this.templ_bindings = new TemplateTVarSet(-1, bld);
	}
	
	public Type make(TVarBld set) {
		return new TupleType(this, set);
	}

	public Type rebind(Type t, TVarBld set) {
		return new TupleType(this, t.bindings().rebind_bld(set));
	}
	
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getArgsLength() == 0) return t;
		return new TupleType(this, t.bindings().applay_bld(bindings));
	}
	
	public TemplateTVarSet getTemplBindings() {
		return templ_bindings;
	}
}

public class CallMetaType extends MetaType {

	@virtual typedef TDecl = MetaTypeDecl;

	private static TemplateTVarSet   templ_bindings_static;
	private static TemplateTVarSet   templ_bindings_this;
	
	public  static MetaTypeDecl      call_tdecl;
	
	private static final CallMetaType call_static_instance;
	private static final CallMetaType call_this_instance;
	private static final CallMetaType closure_static_instance;
	private static final CallMetaType closure_this_instance;
	
	static {
		TVarBld set = new TVarBld();
		set.append(StdTypes.tpCallRetArg, null);
		set.append(StdTypes.tpCallTupleArg, null);
		templ_bindings_static = new TemplateTVarSet(-1, set);

		set = new TVarBld();
		set.append(StdTypes.tpCallRetArg, null);
		set.append(StdTypes.tpCallTupleArg, null);
		set.append(StdTypes.tpSelfTypeArg, null);
		templ_bindings_this = new TemplateTVarSet(-1, set);

		call_tdecl = (MetaTypeDecl)Env.getRoot().resolveGlobalDNode("kiev·stdlib·_call_type_");
		assert (call_tdecl == null);
		call_tdecl = new MetaTypeDecl();
		call_tdecl.sname = "_call_type_";
		call_tdecl.nodeflags |= ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		call_tdecl.uuid = "25395a72-2b16-317a-85b2-5490309bdffc";
		call_static_instance    = new CallMetaType(templ_bindings_static, MetaType.flCallable);
		call_this_instance      = new CallMetaType(templ_bindings_this,   MetaType.flCallable);
		closure_static_instance = new CallMetaType(templ_bindings_static, MetaType.flCallable | MetaType.flReference);
		closure_this_instance   = new CallMetaType(templ_bindings_this,   MetaType.flCallable | MetaType.flReference);
		Env.getRoot().newPackage("kiev·stdlib").pkg_members.add(call_tdecl);
	}
	
	private TemplateTVarSet		templ_bindings;
	
	public static CallMetaType newCallMetaType(boolean is_static, boolean is_closure, ArgType[] targs) {
		if (targs == null || targs.length == 0) {
			if (is_static) {
				if (is_closure)
					return closure_static_instance;
				else
					return call_static_instance;
			} else {
				if (is_closure)
					return closure_this_instance;
				else
					return call_this_instance;
			}
		}
		TVarBld set = new TVarBld();
		set.append(StdTypes.tpCallRetArg, null);
		set.append(StdTypes.tpCallTupleArg, null);
		if (!is_static)
			set.append(StdTypes.tpSelfTypeArg, null);
		foreach (ArgType ta; targs)
			set.append(ta, null);
		int flags = MetaType.flCallable;
		if (is_closure)
			flags |= MetaType.flReference;
		CallMetaType cmt = new CallMetaType(new TemplateTVarSet(-1, set), flags);
		return cmt;
	}
	
	private CallMetaType(TemplateTVarSet templ_bindings, int flags) {
		super(call_tdecl, flags);
		this.templ_bindings = templ_bindings;
	}

	public Type[] getMetaSupers(Type tp) {
		CallType ct = (CallType)tp;
		if (ct.isReference())
			return new Type[]{StdTypes.tpObject};
		return Type.emptyArray;
	}

	public Type rebind(Type t, TVarBld set) {
		if (set.getArgsLength() == 0 || t.bindings().getArgsLength() == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType((CallMetaType)mt.meta_type, mt.bindings().rebind_bld(set), mt.arity);
		return mt;
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() /*|| bindings.getArgsLength() == 0*/ ) return t;
		CallType mt = (CallType)t;
		mt = new CallType((CallMetaType)mt.meta_type, mt.bindings().applay_bld(bindings), mt.arity);
		return mt;
	}

	public rule resolveNameAccessR(Type tp, ResInfo info) { false }
	public rule resolveCallAccessR(Type tp, ResInfo info, CallType mt) { false }

	public TemplateTVarSet getTemplBindings() {
		return templ_bindings;
	}
}


