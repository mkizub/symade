package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.be.java15.JBaseMetaType;
import kiev.be.java15.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public class MetaType {

	public final static MetaType[] emptyArray = new MetaType[0];
	static final MetaType dummy = new MetaType();

	public TypeDecl					tdecl;
	public int						version;
	

	public MetaType() {}
	public MetaType(TypeDecl tdecl) {
		this.tdecl = tdecl;
	}

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}
	
	public boolean checkTypeVersion(int version) {
		return this.version == version;
	}
	
	public Type make(TVSet bindings) {
		throw new RuntimeException("make() in DummyType");
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in DummyType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in DummyType");
	}
	public Type applay(Type t, TVSet bindings) {
		throw new RuntimeException("applay() in DummyType");
	}
	public TVarSet getTemplBindings() { return TVarSet.emptySet; }

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name) { false }

}

public final class CoreMetaType extends MetaType {

	CoreType core_type;
	
	CoreMetaType(KString name) {
		this.tdecl = new TypeDecl();
		this.tdecl.id = new Symbol(String.valueOf(name));
		this.tdecl.setResolved(true);
	}

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}
	
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

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name)
		MetaType@ sup;
	{
		node @= clazz.members,
		node instanceof Field && ((Field)node).id.equals(name) && info.check(node)
	;
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= clazz.getAllSuperTypes(),
		sup.make(tp.bindings()).resolveNameAccessR(node,info,name)
	}
}

public final class CompaundMetaType extends MetaType {

	private TVarSet			templ_bindings;
	
	public CompaundMetaType() {}
	public CompaundMetaType(Struct clazz) {
		super(clazz);
		if (this.tdecl == Env.root) Env.root.imeta_type = this;
		this.templ_bindings = TVarSet.emptySet;
	}
	
	public Type[] getMetaSupers(Type tp) {
		Type[] stps = new Type[tdecl.super_types.length];
		for (int i=0; i < tdecl.super_types.length; i++)
			stps[i] = tdecl.super_types[i].getType();
		return stps;
	}

	public boolean checkTypeVersion(int version) {
		return this.version == version && tdecl.type_decl_version == version;
	}
	
	public Type make(TVSet bindings) {
		return new CompaundType(this, getTemplBindings().bind_bld(bindings));
	}

	public Type bind(Type t, TVSet bindings) {
		if (!t.isAbstract()) return t;
		return new CompaundType(this, t.bindings().bind_bld(bindings));
	}
	
	public Type rebind(Type t, TVSet bindings) {
		return new CompaundType(this, t.bindings().rebind_bld(bindings));
	}
	
	public Type applay(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
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

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name)
	{
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in "+tp),
		tdecl.checkResolved(),
		{
			trace(Kiev.debugResolve,"Type: resolving in "+tp),
			resolveNameR_1(node,info,name),	// resolve in this class
			$cut
		;	info.isSuperAllowed(),
			trace(Kiev.debugResolve,"Type: resolving in super-type of "+tp),
			resolveNameR_3(tp,node,info,name),	// resolve in super-classes
			$cut
		;	info.isForwardsAllowed(),
			trace(Kiev.debugResolve,"Type: resolving in forwards of "+tp),
			resolveNameR_4(tp,node,info,name),	// resolve in forwards
			$cut
		}
	}
	private rule resolveNameR_1(ASTNode@ node, ResInfo info, String name)
	{
		node @= tdecl.members,
		node instanceof Field && ((Field)node).id.equals(name) && info.check(node)
	}
	private rule resolveNameR_3(Type tp, ASTNode@ node, ResInfo info, String name)
		MetaType@ sup;
	{
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= tdecl.getAllSuperTypes(),
		sup.make(tp.bindings()).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(Type tp, ASTNode@ node, ResInfo info, String name)
		ASTNode@ forw;
		MetaType@ sup;
	{
		forw @= tdecl.members,
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(tp).resolveNameAccessR(node,info,name)
	;	info.isSuperAllowed(),
		sup @= tdecl.getAllSuperTypes(),
		sup instanceof CompaundMetaType,
		forw @= ((CompaundMetaType)sup).tdecl.members,
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(tp).resolveNameAccessR(node,info,name)
	}

}

public final class ArrayMetaType extends MetaType {

	private static TVarSet			templ_bindings;
	public static final ArrayMetaType instance;
	static {
		templ_bindings = new TVarSet(new TVarBld(StdTypes.tpArrayArg, null).close());
		TypeDecl tdecl = new TypeDecl();
		tdecl.id = new Symbol("_array_");
		tdecl.package_clazz = Env.newPackage("kiev.stdlib");
		tdecl.flags = AccessFlags.ACC_MACRO|AccessFlags.ACC_PUBLIC|AccessFlags.ACC_FINAL;
		tdecl.super_types.insert(0, new TypeRef(StdTypes.tpObject));
		tdecl.args.add(StdTypes.tdArrayArg);
		tdecl.setResolved(true);
		tdecl.package_clazz.sub_decls.add(tdecl);
		Field length = new Field("length", StdTypes.tpInt, AccessFlags.ACC_PUBLIC|AccessFlags.ACC_FINAL);
		length.setMacro(true);
		length.acc = new Access(0xAA); //public:ro
		RewriteMatch rmatch = new RewriteMatch();
		length.init = rmatch;
		RewriteCase rget = new RewriteCase();
		rget.var = new RewritePattern("self", new ASTNodeType(Env.newStruct("IFldExpr", Env.newPackage("kiev.vlang"), AccessFlags.ACC_PUBLIC)));
		rmatch.cases += rget;
		rget.stats.append(
			new RewriteNodeFactory(
				ArrayLengthExpr.class,new RewriteNodeArg[]{
					new RewriteNodeArg("obj",   new IFldExpr(new LVarExpr(0, rget.var), "obj"  )),
					new RewriteNodeArg("ident", new IFldExpr(new LVarExpr(0, rget.var), "ident"))
				}
			)
		);
		tdecl.members.add(length);
		
		instance = new ArrayMetaType(tdecl);
	}
	private ArrayMetaType(TypeDecl tdecl) {
		super(tdecl);
	}

	public Type[] getMetaSupers(Type tp) {
		Type[] stps = new Type[tdecl.super_types.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = tdecl.super_types[i].getType();
		return stps;
	}

	public TVarSet getTemplBindings() { return templ_bindings; }

	public Type make(TVSet bindings) {
		return ArrayType.newArrayType(bindings.resolve(StdTypes.tpArrayArg));
	}
	public Type bind(Type t, TVSet bindings) {
		throw new RuntimeException("bind() in ArrayType");
	}
	public Type rebind(Type t, TVSet bindings) {
		throw new RuntimeException("rebind() in ArrayType");
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isAbstract() || bindings.getTVars().length == 0 ) return t;
		return ArrayType.newArrayType(((ArrayType)t).arg.applay(bindings));
	}
	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name)
		Type@ st;
	{
		node @= tdecl.members,
		node instanceof Field && ((Field)node).id.equals(name) && info.check(node)
	;
		st @= getMetaSupers(tp),
		st.resolveNameAccessR(node,info,name)
	}
}

public class ArgMetaType extends MetaType {

	public final ArgType atype;
	
	public ArgMetaType(TypeDef definer) {
		super(definer);
		atype = new ArgType(this);
	}

	public Type[] getMetaSupers(Type tp) {
		ArgType at = (ArgType)tp;
		TypeRef[] ups = tdecl.super_types.getArray();
		if (ups.length == 0)
			return new Type[]{StdTypes.tpObject};
		Type[] stps = new Type[ups.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = ups[i].getType();
		return stps;
	}

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
	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name)
		TypeRef@ sup;
	{
		sup @= tdecl.super_types.getArray(),
		sup.getType().resolveNameAccessR(node, info, name)
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
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
		return WrapperType.newWrapperType(((WrapperType)t).getEnclosedType().applay(bindings));
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in wrapper type "+tp),
		tp.checkResolved(),
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		{
			info.enterForward(field, 0) : info.leaveForward(field, 0),
			((WrapperType)tp).getUnboxedType().resolveNameAccessR(node, info, name),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			((WrapperType)tp).getEnclosedType().resolveNameAccessR(node, info, name)
		}
	;
		info.enterReinterp(((WrapperType)tp).getEnclosedType()) : info.leaveReinterp(),
		((WrapperType)tp).getEnclosedType().resolveNameAccessR(node, info, name)
	}

}

public class OuterMetaType extends MetaType {

	private TVarSet			templ_bindings;
	public final Struct		clazz;
	public final TypeDef	tdef;
	public static OuterMetaType instance(Struct clazz, TypeDef tdef) {
		if (clazz.ometa_type == null)
			clazz.ometa_type = new OuterMetaType(clazz, tdef);
		return clazz.ometa_type;
	}
	private OuterMetaType() {}
	private OuterMetaType(Struct clazz, TypeDef tdef) {
		super(clazz);
		this.clazz = clazz;
		this.tdef = tdef;
		this.templ_bindings = new TVarSet(new TVarBld(tdef.getAType(), null).close());
	}

	public Type[] getMetaSupers(Type tp) {
		OuterType ot = (OuterType)tp;
		return new Type[]{ot.outer};
	}
	public TVarSet getTemplBindings() { return templ_bindings; }


	public Type make(TVSet bindings) {
		return OuterType.newOuterType(clazz,bindings.resolve(tdef.getAType()));
	}
	public Type bind(Type t, TVSet bindings) {
		return OuterType.newOuterType(clazz,((OuterType)t).outer.bind(bindings));
	}
	public Type rebind(Type t, TVSet bindings) {
		return OuterType.newOuterType(clazz,((OuterType)t).outer.rebind(bindings));
	}
	public Type applay(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0) return t;
		return OuterType.newOuterType(clazz,((OuterType)t).outer.applay(bindings));
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name) {
		((OuterType)tp).outer.resolveNameAccessR(node,info,name)
	}
}

public class CallMetaType extends MetaType {

	public static final CallMetaType instance;
	static {
		TypeDecl tdecl = new TypeDecl();
		tdecl.id = new Symbol("_call_type_");
		tdecl.package_clazz = Env.newPackage("kiev.stdlib");
		tdecl.flags = AccessFlags.ACC_MACRO|AccessFlags.ACC_PUBLIC|AccessFlags.ACC_FINAL;
		tdecl.setResolved(true);
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
		if (!t.isAbstract() || bindings.getTVars().length == 0 || t.bindings().getTVars().length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().bind_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type rebind(Type t, TVSet bindings) {
		if (!t.isAbstract() || bindings.getTVars().length == 0 || t.bindings().tvars.length == 0) return t;
		if!(t instanceof CallType) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().rebind_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
	public Type applay(Type t, TVSet bindings) {
		if( !t.isAbstract() || bindings.getTVars().length == 0 ) return t;
		CallType mt = (CallType)t;
		mt = new CallType(mt.bindings().applay_bld(bindings),mt.arity,mt.isReference());
		return mt;
	}
}


