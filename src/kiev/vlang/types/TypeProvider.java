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

import kiev.be.java15.JBaseMetaType;
import kiev.be.java15.JStruct;

import syntax kiev.Syntax;

public class MetaType implements Constants {

	public final static MetaType[] emptyArray = new MetaType[0];
	static final MetaType dummy = new MetaType("<dummy>");

	Object					descr; // type description, TypeDecl for loaded types, String for names of not loaded yet types
	public int				version;
	private TVarSet			templ_bindings;

	@getter
	public final TypeDecl get$tdecl() {
		if (descr instanceof String) {
			TypeDecl td = Env.loadTypeDecl((String)descr, true);
			if (td != null)
				descr = td;
		}
		return ANode.getVersion((TypeDecl)this.descr);
	}
	
	public String qname() {
		if (descr instanceof String)
			return (String)descr;
		return ((TypeDecl)descr).qname();
	}
	
	public MetaType(String name) {
		this.descr = name;
	}
	public MetaType(TypeDecl tdecl) {
		this.descr = tdecl;
	}

	public Type[] getMetaSupers(Type tp) {
		if (tdecl.super_types.length == 0)
			return Type.emptyArray;
		Type[] stps = new Type[tdecl.super_types.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = tdecl.super_types[i].getType().applay(tp);
		return stps;
	}

	public boolean checkTypeVersion(int version) {
		return this.version == version;
	}
	
	public Type make(TVSet bindings) {
		return new XType(this, getTemplBindings().bind_bld(bindings));
	}
	public Type bind(Type t, TVSet bindings) {
		if (!t.isBindable()) return t;
		return new XType(this, t.bindings().bind_bld(bindings));
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in DummyType");
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getTVars().length == 0) return t;
		return new XType(this, t.bindings().applay_bld(bindings));
	}

	public TVarSet getTemplBindings() {
		if (this.version != tdecl.type_decl_version)
			makeTemplBindings();
		return templ_bindings;
	}
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; tdecl.args)
			vs.append(ad.getAType(), null);
		foreach (TypeDef td; tdecl.members) {
			vs.append(td.getAType(), null);
		}
		foreach (TypeRef st; tdecl.super_types; st.getType() ≢ null)
			vs.append(st.getType().bindings());
		templ_bindings = new TVarSet(vs.close());
		this.version = tdecl.type_decl_version;
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info)
	{
		trace(Kiev.debug && Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in "+tp),
		tdecl.checkResolved(),
		{
			trace(Kiev.debug && Kiev.debugResolve,"Type: resolving in "+tp),
			resolveNameR_1(node,info)	// resolve in this class
		;	info.isSuperAllowed(),
			trace(Kiev.debug && Kiev.debugResolve,"Type: resolving in super-type of "+tp),
			resolveNameR_3(tp,node,info)	// resolve in super-classes
		;	info.isForwardsAllowed(),
			trace(Kiev.debug && Kiev.debugResolve,"Type: resolving in forwards of "+tp),
			resolveNameR_4(tp,node,info)	// resolve in forwards
		}
	}
	private rule resolveNameR_1(ASTNode@ node, ResInfo info)
		ASTNode@ n;
		DNode@ dn;
	{
		n @= tdecl.members,
		{
			n instanceof DeclGroup,
			dn @= ((DeclGroup)n).decls,
			info.checkNodeName(dn),
			info.check(dn),
			node ?= dn
		;
			n instanceof Field && info.checkNodeName(n) && info.check(n),
			node ?= n
		}
	}
	private rule resolveNameR_3(Type tp, ASTNode@ node, ResInfo info)
		MetaType@ sup;
		Type@ tmp;
	{
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= tdecl.getAllSuperTypes(),
		tmp ?= sup.make(tp.bindings()),
		tmp.meta_type.resolveNameAccessR(tmp,node,info)
	}

	private rule resolveNameR_4(Type tp, ASTNode@ node, ResInfo info)
		ASTNode@ forw;
		MetaType@ sup;
	{
		forw @= tdecl.members,
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(tp).resolveNameAccessR(node,info)
	;	info.isSuperAllowed(),
		sup @= tdecl.getAllSuperTypes(),
		sup instanceof CompaundMetaType,
		forw @= ((CompaundMetaType)sup).tdecl.members,
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(tp).resolveNameAccessR(node,info)
	}

	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt)
		ASTNode@ member;
		MetaType@ sup;
		Type@ tmp;
		Field@ forw;
	{
		tp.checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving method "+info.getName()+" in "+this),
		{
			member @= tdecl.members,
			member instanceof Method,
			info.check(member),
			node ?= ((Method)member),
			((Method)node).equalsByCast(info.getName(),mt,tp,info)
		;
			info.isSuperAllowed(),
			info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
			sup @= tdecl.getAllSuperTypes(),
			tmp ?= sup.make(tp.bindings()),
			tmp.meta_type.resolveCallAccessR(tmp,node,info,mt)
		;
			info.isForwardsAllowed(),
			member @= tdecl.members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).type.applay(tp).resolveCallAccessR(node,info,mt)
		;
			info.isForwardsAllowed(),
			sup @= tdecl.getAllSuperTypes(),
			member @= sup.tdecl.members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).type.applay(tp).resolveCallAccessR(node,info,mt)
		}
	}

}

public final class CoreMetaType extends MetaType {

	CoreType core_type;
	
	CoreMetaType(String name, Type super_type) {
		super("kiev\u001fstdlib\u001f"+name);
		MetaTypeDecl tdecl = new MetaTypeDecl(this);
		this.descr = tdecl;
		tdecl.sname = name;
		tdecl.package_clazz.symbol = Env.newPackage("kiev\u001fstdlib");
		tdecl.meta.mflags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.xmeta_type = this;
		tdecl.package_clazz.dnode.sub_decls.add(tdecl);
		if (super_type != null)
			tdecl.super_types.add(new TypeRef(super_type));
	}

	public TVarSet getTemplBindings() { return TVarSet.emptySet; }

	public Type make(TVSet bindings) {
		return core_type;
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in CoreType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in CoreType");
	}
	public Type applay(Type t, TVSet bindings) {
		return t;
	}
}

public final class ASTNodeMetaType extends MetaType {

	public static Hashtable<Class,ASTNodeMetaType> allASTNodeMetaTypes;
	public static final Hashtable<String,Class>	allNodes;
	static {
		allASTNodeMetaTypes = new Hashtable<Class,ASTNodeMetaType>();
		allNodes = new Hashtable<String,Class>(256);
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
		allNodes.put("ENode",				ENode.class);
		allNodes.put("NoOp",				NopExpr.class);
		allNodes.put("AssertEnabled",		AssertEnabledExpr.class);
		allNodes.put("IFld",				IFldExpr.class);
		allNodes.put("CBool",				ConstBoolExpr.class);
		allNodes.put("CInt",				ConstIntExpr.class);
		allNodes.put("CString",				ConstStringExpr.class);
		allNodes.put("ASTRuleNode",			ASTRuleNode.class);
		allNodes.put("RuleIstheExpr",		RuleIstheExpr.class);
		allNodes.put("RuleIsoneofExpr",		RuleIsoneofExpr.class);
		allNodes.put("RuleCutExpr",			RuleCutExpr.class);
		allNodes.put("RuleCallExpr",		RuleCallExpr.class);
		allNodes.put("RuleExpr",			RuleExpr.class);
		allNodes.put("RuleWhileExpr",		RuleWhileExpr.class);
		allNodes.put("MacroAccessExpr",		MacroAccessExpr.class);
	}


	final public Class			clazz;
	final public String			name;

	private AttrSlot[]		values;
	private TypeAssign[]	types;
	private Field[]			fields;
	private TVarSet			templ_bindings;

	public static ASTNodeMetaType instance(Class clazz) {
		ASTNodeMetaType mt = allASTNodeMetaTypes.get(clazz);
		if (mt != null)
			return mt;
		mt = new ASTNodeMetaType(clazz);
		return mt;
	}

	ASTNodeMetaType(Class clazz) {
		super(StdTypes.tdASTNodeType);
		this.templ_bindings = TVarSet.emptySet;
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

	public boolean checkTypeVersion(int version) {
		return this.version == version;
	}
	
	public Type make(TVSet bindings) {
		throw new RuntimeException("make() in ASTNodeMetaType");
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in ASTNodeMetaType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in ASTNodeMetaType");
	}
	public Type applay(Type t, TVSet bindings) {
		return t;
	}

	public TVarSet getTemplBindings() {
		if (this.version != 1)
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
		templ_bindings = new TVarSet(vs.close());
		this.version = 1;
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info)
	{
		getTemplBindings(),
		node @= this.fields,
		node instanceof Field && info.checkNodeName(node) && info.check(node)
	}

	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt) { false }

}

public final class CompaundMetaType extends MetaType {

	private static Hashtable<String,CompaundMetaType> compaundMetaTypes = new Hashtable<String,CompaundMetaType>();
	
	private TVarSet			templ_bindings;
	
	public static CompaundMetaType newCompaundMetaType(String clazz_name) alias lfy operator new {
		TypeDecl td = Env.resolveGlobalDNode(clazz_name);
		if (td != null)
			return (CompaundMetaType)td.xmeta_type;
		CompaundMetaType mt = compaundMetaTypes.get(clazz_name);
		if (mt != null)
			return mt;
		mt = new CompaundMetaType(clazz_name);
		compaundMetaTypes.put(clazz_name,mt);
		return mt;
	}
	
	public static CompaundMetaType newCompaundMetaType(Struct clazz) alias lfy operator new {
		if (clazz.xmeta_type != null)
			return (CompaundMetaType)clazz.xmeta_type;
		String qname = clazz.qname();
		if (qname != null) {
			CompaundMetaType mt = compaundMetaTypes.get(clazz.qname());
			if (mt != null)
				return mt;
		}
		return new CompaundMetaType(clazz);
	}
	
	private CompaundMetaType(String clazz_name) {
		super(clazz_name);
		this.templ_bindings = TVarSet.emptySet;
	}
	
	private CompaundMetaType(Struct clazz) {
		super(clazz);
		if (this.tdecl == Env.getRoot()) Env.getRoot().xmeta_type = this;
		this.templ_bindings = TVarSet.emptySet;
	}
	
	public boolean checkTypeVersion(int version) {
		return this.version == version && tdecl.type_decl_version == version;
	}
	
	public Type make(TVSet bindings) {
		return new CompaundType(this, getTemplBindings().bind_bld(bindings));
	}

	public Type bind(Type t, TVSet bindings) {
		if (!t.isBindable()) return t;
		return new CompaundType(this, t.bindings().bind_bld(bindings));
	}
	
	public Type rebind(Type t, TVSet bindings) {
		return new CompaundType(this, t.bindings().rebind_bld(bindings));
	}
	
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getTVars().length == 0) return t;
		return new CompaundType(this, t.bindings().applay_bld(bindings));
	}
	
	public TVarSet getTemplBindings() {
		if (this.version != tdecl.type_decl_version)
			makeTemplBindings();
		return templ_bindings;
	}
	
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; tdecl.args)
			vs.append(ad.getAType(), null);
		foreach (TypeDef td; tdecl.members) {
			vs.append(td.getAType(), null);
		}
		foreach (TypeRef st; tdecl.super_types; st.getType() ≢ null)
			vs.append(st.getType().bindings());
		templ_bindings = new TVarSet(vs.close());
		this.version = tdecl.type_decl_version;
	}

}

public final class ArrayMetaType extends MetaType {

	private static TVarSet					templ_bindings;
	public static final ArrayMetaType		instance;
	static {
		templ_bindings = new TVarSet(new TVarBld(StdTypes.tpArrayArg, null).close());
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.resolveGlobalDNode("kiev\u001fstdlib\u001f_array_");
		if (tdecl == null) {
			tdecl = new MetaTypeDecl(null);
			tdecl.sname = "_array_";
			tdecl.package_clazz.symbol = Env.newPackage("kiev\u001fstdlib");
			tdecl.meta.mflags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdecl.super_types.insert(0, new TypeRef(StdTypes.tpObject));
			tdecl.args.add(StdTypes.tdArrayArg);
			tdecl.package_clazz.dnode.sub_decls.add(tdecl);
			tdecl.setUUID("bbf03b4b-62d4-3e29-8f0d-acd6c47b9a04");
			Field length = new Field("length", StdTypes.tpInt, ACC_PUBLIC|ACC_FINAL|ACC_MACRO|ACC_NATIVE);
			length.setMeta(new MetaAccess("public",0xAA)); //public:ro
			tdecl.members.add(length);
			Method get = new MethodImpl("get", StdTypes.tpArrayArg, ACC_PUBLIC|ACC_MACRO|ACC_NATIVE);
			get.params.add(new LVar(0,"idx",StdTypes.tpInt,Var.PARAM_NORMAL,0));
			get.aliases += new ASTOperatorAlias(Constants.nameArrayGetOp);
			//get.body = CoreExpr.makeInstance("");
			tdecl.members.add(get);
		}
		
		instance = new ArrayMetaType(tdecl);
		tdecl.xmeta_type = instance;
		tdecl.xtype = ArrayType.newArrayType(Type.tpAny);
	}
	private ArrayMetaType(TypeDecl tdecl) {
		super(tdecl);
	}

	public TVarSet getTemplBindings() { return templ_bindings; }

	public Type make(TVSet bindings) {
		return ArrayType.newArrayType(bindings.resolve(StdTypes.tpArrayArg));
	}
	public Type bind(Type t, TVSet bindings) {
		if (!t.isBindable()) return t;
		return ArrayType.newArrayType(t.bindings().bind_bld(bindings).resolve(StdTypes.tpArrayArg));
	}
	public Type rebind(Type t, TVSet bindings) {
		return ArrayType.newArrayType(t.bindings().rebind_bld(bindings).resolve(StdTypes.tpArrayArg));
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() || bindings.getTVars().length == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.applay(bindings));
	}
	
}

public class ArgMetaType extends MetaType {

	public final ArgType atype;
	
	public ArgMetaType(TypeDef definer) {
		super(definer);
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

	public TVarSet getTemplBindings() { return TVarSet.emptySet; }

	public Type make(TVSet bindings) {
		return atype;
	}
	public Type bind(Type t, TVSet bindings) {
		return t; //throw new RuntimeException("bind() in ArgType");
	}
	public Type rebind(Type t, TVSet bindings) {
		return t; //throw new RuntimeException("bind() in ArgType");
	}
	public Type applay(Type t, TVSet bindings) {
		ArgType at = (ArgType)t;
		foreach (TVar v; bindings.getTVars()) {
			if (v.var ≡ at || v.val ≡ at)
				return v.unalias().result();
		}
		// Not found, return itself
		return t;
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info)
		TypeRef@ sup;
	{
		sup @= tdecl.super_types,
		sup.getType().resolveNameAccessR(node, info)
	}

	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt)
		TypeRef@ sup;
	{
		tdecl.super_types.length == 0,
		StdTypes.tpObject.resolveCallAccessR(node, info, mt)
	;	sup @= tdecl.super_types,
		sup.getType().resolveCallAccessR(node, info, mt)
	}
}

public class WrapperMetaType extends MetaType {

	private static TVarSet					templ_bindings;
	private static final MetaTypeDecl		wrapper_tdecl;
	static {
		templ_bindings = new TVarSet(new TVarBld(StdTypes.tpWrapperArg, null).close());
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.resolveGlobalDNode("kiev\u001fstdlib\u001f_wrapper_");
		if (tdecl == null) {
			tdecl = new MetaTypeDecl();
			tdecl.sname = "_wrapper_";
			tdecl.package_clazz.symbol = Env.newPackage("kiev\u001fstdlib");
			tdecl.meta.mflags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdecl.super_types.insert(0, new TypeRef(StdTypes.tpObject));
			tdecl.args.add(StdTypes.tdWrapperArg);
			tdecl.package_clazz.dnode.sub_decls.add(tdecl);
			tdecl.setUUID("67544053-836d-3bac-b94d-0c4b14ae9c55");
		}
		wrapper_tdecl = tdecl;
		tdecl.xmeta_type = WrapperMetaType.instance(StdTypes.tpWrapperArg);
		tdecl.xtype = WrapperType.newWrapperType(StdTypes.tpWrapperArg);
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
		super(wrapper_tdecl);
		this.clazz = td;
		clazz.checkResolved();
		this.field = getWrappedField(td,false);
	}

	public Type[] getMetaSupers(Type tp) {
		WrapperType wt = (WrapperType)tp;
		return new Type[]{tp.getEnclosedType(),tp.getUnboxedType()};
	}
	public TVarSet getTemplBindings() { return templ_bindings; }


	public Type make(TVSet bindings) {
		return WrapperType.newWrapperType(bindings.resolve(StdTypes.tpWrapperArg));
	}
	public Type bind(Type t, TVSet bindings) {
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().bind(bindings));
	}
	public Type rebind(Type t, TVSet bindings) {
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().rebind(bindings));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isValAppliable() || bindings.getTVars().length == 0) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().applay(bindings));
	}

	private static Field getWrappedField(TypeDecl td, boolean required) {
		foreach (TypeRef st; td.super_types; st.getTypeDecl() != null) {
			Field wf = getWrappedField(st.getTypeDecl(), false);
			if (wf != null)
				return wf;
		}
		Field wf = null;
		foreach(Field n; td.getAllFields(); n.isForward()) {
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
	
	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info)
	{
		info.isForwardsAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in wrapper type "+tp),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveNameAccessR(node, info)
		;	info.enterSuper(10) : info.leaveSuper(10),
			((WrapperType)tp).getEnclosedType().resolveNameAccessR(node, info)
		}
	;
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		((WrapperType)tp).getEnclosedType().resolveNameAccessR(node, info)
	}

	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt)
	{
		info.isForwardsAllowed(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving method "+info.getName()+" in wrapper type "+this),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveCallAccessR(node, info, mt)
		;	info.enterSuper(10) : info.leaveSuper(10),
			((WrapperType)tp).getEnclosedType().resolveCallAccessR(node, info, mt)
		}
	;
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		((WrapperType)tp).getEnclosedType().resolveCallAccessR(node, info, mt)
	}
	
}

public class CallMetaType extends MetaType {

	public static final CallMetaType instance;
	static {
		MetaTypeDecl tdecl = (MetaTypeDecl)Env.resolveGlobalDNode("kiev\u001fstdlib\u001f_call_type_");
		if (tdecl == null) {
			tdecl = new MetaTypeDecl();
			tdecl.sname = "_call_type_";
			tdecl.package_clazz.symbol = Env.newPackage("kiev\u001fstdlib");
			tdecl.meta.mflags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
			tdecl.setUUID("25395a72-2b16-317a-85b2-5490309bdffc");
		}
		instance = new CallMetaType(tdecl);
	}
	private CallMetaType(TypeDecl tdecl) {
		super(tdecl);
	}

	public boolean checkTypeVersion(int version) {
		return true;
	}

	public Type[] getMetaSupers(Type tp) {
		CallType ct = (CallType)tp;
		if (ct.isReference())
			return new Type[]{StdTypes.tpObject};
		return Type.emptyArray;
	}

	public Type bind(Type t, TVSet bindings) {
		if (!t.isBindable() || bindings.getTVars().length == 0 || t.bindings().getTVars().length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().bind_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type rebind(Type t, TVSet bindings) {
		if (bindings.getTVars().length == 0 || t.bindings().tvars.length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().rebind_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isValAppliable() || bindings.getTVars().length == 0 ) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().applay_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info) { false }
	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt) { false }

}


