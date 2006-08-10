package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import kiev.be.java15.JBaseMetaType;
import kiev.be.java15.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public class MetaType implements Constants {

	public final static MetaType[] emptyArray = new MetaType[0];
	static final MetaType dummy = new MetaType();

	public TypeDecl			tdecl;
	public int				version;
	private TVarSet			templ_bindings;

	public MetaType() {}
	public MetaType(TypeDecl tdecl) {
		this.tdecl = tdecl;
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
		throw new RuntimeException("bind() in DummyType");
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
		trace(Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in "+tp),
		tdecl.checkResolved(),
		{
			trace(Kiev.debugResolve,"Type: resolving in "+tp),
			resolveNameR_1(node,info),	// resolve in this class
			$cut
		;	info.isSuperAllowed(),
			trace(Kiev.debugResolve,"Type: resolving in super-type of "+tp),
			resolveNameR_3(tp,node,info),	// resolve in super-classes
			$cut
		;	info.isForwardsAllowed(),
			trace(Kiev.debugResolve,"Type: resolving in forwards of "+tp),
			resolveNameR_4(tp,node,info),	// resolve in forwards
			$cut
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
		trace(Kiev.debugResolve, "Resolving method "+info.getName()+" in "+this),
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
		TypeDecl tdecl = new TypeDecl();
		this.tdecl = tdecl;
		tdecl.u_name = name;
		tdecl.id = new Symbol<TypeDecl>(name);
		tdecl.package_clazz = Env.newPackage("kiev.stdlib");
		tdecl.flags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.setTypeDeclLoaded(true);
		tdecl.xmeta_type = this;
		tdecl.package_clazz.sub_decls.add(tdecl);
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

	public Struct clazz;

	private TVarSet			templ_bindings;

	public static ASTNodeMetaType instance(Struct clazz) {
		if (clazz.ameta_type == null)
			clazz.ameta_type = new ASTNodeMetaType(clazz);
		return clazz.ameta_type;
	}

	ASTNodeMetaType() {}
	ASTNodeMetaType(Struct clazz) {
		super(clazz);
		this.clazz = clazz;
		this.templ_bindings = TVarSet.emptySet;
	}

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}

	public boolean checkTypeVersion(int version) {
		return this.version == version && clazz.type_decl_version == version;
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
		if (this.version != clazz.type_decl_version)
			makeTemplBindings();
		return templ_bindings;
	}

	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeAssign ta; clazz.members; ta.id.sname.matches("attr\\$.*\\$type"))
			vs.append(ta.getAType(), null);
		foreach (TypeRef st; clazz.super_types; st.getType() ≢ null)
			vs.append(st.getType().bindings());
		templ_bindings = new TVarSet(vs.close());
		this.version = clazz.type_decl_version;
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info)
		MetaType@ sup;
		Type@ tmp;
	{
		node @= clazz.members,
		node instanceof Field && info.checkNodeName(node) && info.check(node)
	;
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= clazz.getAllSuperTypes(),
		tmp ?= sup.make(tp.bindings()),
		tmp.meta_type.resolveNameAccessR(tmp,node,info)
	}

	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt) { false }

}

public final class CompaundMetaType extends MetaType {

	private TVarSet			templ_bindings;
	
	public CompaundMetaType() {}
	public CompaundMetaType(Struct clazz) {
		super(clazz);
		if (this.tdecl == Env.root) Env.root.xmeta_type = this;
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

	private static TVarSet			templ_bindings;
	public static final ArrayMetaType instance;
	static {
		templ_bindings = new TVarSet(new TVarBld(StdTypes.tpArrayArg, null).close());
		TypeDecl tdecl = new TypeDecl();
		tdecl.u_name = "_array_";
		tdecl.id = new Symbol<TypeDecl>("_array_");
		tdecl.package_clazz = Env.newPackage("kiev.stdlib");
		tdecl.flags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.super_types.insert(0, new TypeRef(StdTypes.tpObject));
		tdecl.args.add(StdTypes.tdArrayArg);
		tdecl.setTypeDeclLoaded(true);
		tdecl.package_clazz.sub_decls.add(tdecl);
		Field length = new Field("length", StdTypes.tpInt, ACC_PUBLIC|ACC_FINAL|ACC_MACRO|ACC_NATIVE);
		length.meta.setF(new MetaAccess("public",0xAA)); //public:ro
		tdecl.members.add(length);
		Method get = new Method("get", StdTypes.tpArrayArg, ACC_PUBLIC|ACC_MACRO|ACC_NATIVE);
		get.params.add(new FormPar(0,"idx",StdTypes.tpInt,FormPar.PARAM_NORMAL,0));
		get.aliases += new ASTOperatorAlias(Constants.nameArrayGetOp);
		tdecl.members.add(get);
		
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
		tdecl.super_types.length == 0, $cut,
		StdTypes.tpObject.resolveCallAccessR(node, info, mt)
	;	sup @= tdecl.super_types,
		sup.getType().resolveCallAccessR(node, info, mt)
	}
}

public class WrapperMetaType extends MetaType {

	private static TVarSet			templ_bindings;
	static {
		templ_bindings = new TVarSet(new TVarBld(StdTypes.tpWrapperArg, null).close());
	}
	public final Struct		clazz;
	public final Field		field;
	public static WrapperMetaType instance(Struct clazz) {
		if (clazz.wmeta_type == null)
			clazz.wmeta_type = new WrapperMetaType(clazz);
		return clazz.wmeta_type;
	}
	private WrapperMetaType() {}
	private WrapperMetaType(Struct clazz) {
		super(clazz);
		this.clazz = clazz;
		this.field = clazz.getWrappedField(true);
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

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve,"Type: Resolving name "+info.getName()+" in wrapper type "+tp),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveNameAccessR(node, info),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			((WrapperType)tp).getEnclosedType().resolveNameAccessR(node, info)
		}
	;
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		((WrapperType)tp).getEnclosedType().resolveNameAccessR(node, info)
	}

	public rule resolveCallAccessR(Type tp, Method@ node, ResInfo info, CallType mt)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve, "Resolving method "+info.getName()+" in wrapper type "+this),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveCallAccessR(node, info, mt),
			$cut
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
		TypeDecl tdecl = new TypeDecl();
		tdecl.u_name = "_call_type_";
		tdecl.id = new Symbol<TypeDecl>("_call_type_");
		tdecl.package_clazz = Env.newPackage("kiev.stdlib");
		tdecl.flags = ACC_MACRO|ACC_PUBLIC|ACC_FINAL;
		tdecl.setTypeDeclLoaded(true);
		instance = new CallMetaType(tdecl);
	}
	private CallMetaType(TypeDecl tdecl) {
		super(tdecl);
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


