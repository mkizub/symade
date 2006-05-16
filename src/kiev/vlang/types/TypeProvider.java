package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.be.java15.JBaseMetaType;
import kiev.be.java15.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final class TVarSet extends AType {

	public static final TVarSet emptySet = new TVarSet();

	private TVarSet() {
		super(MetaType.dummy, 0, TVar.emptyArray, TArg.emptyArray);
	}
	
	TVarSet(TVarBld bld) {
		super(MetaType.dummy, 0, bld);
	}
	
}


public final class TVar {
	public static final TVar[] emptyArray = new TVar[0];

	public final TVSet			set;	// the set this TVar belongs to
	public final int			idx;	// position in the set (set.tvars[idx] == this)
	public final ArgType		var;	// variable
	public final Type			val;	// value of the TVar (null for free vars, ArgType for aliases) 
	public final int			ref;	// reference to actual TVar, for aliases

	// copy
	private TVar(TVSet set, int idx, ArgType var, Type val, int ref) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.val = val;
		this.ref = ref;
	}
	
	// free vars
	TVar(TVSet set, int idx, ArgType var) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.ref = -1;
	}

	// bound vars
	TVar(TVSet set, int idx, ArgType var, Type val) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.val = val;
		this.ref = -1;
	}

	// aliases vars
	TVar(TVSet set, int idx, ArgType var, ArgType val, int ref) {
		this.set = set;
		this.idx = idx;
		this.var = var;
		this.val = val;
		this.ref = ref;
	}

	public Type result() {
		return val == null? var : val;
	}
	
	public TVar copy(TVSet set) {
		return new TVar(set, idx, var, val, ref);
	}

	public TVar unalias() {
		TVar r = this;
		while (r.ref >= 0) r = set.getTVars()[r.ref];
		return r;
	}
	
	public boolean isFree() { return val == null; }
	
	public boolean isAlias() { return ref >= 0; }

	public String toString() {
		if (isFree())
			return idx+": free  "+var.definer.parent()+"."+var.definer+"."+var.name;
		else if (isAlias())
			return idx+": alias "+var.definer.parent()+"."+var.definer+"."+var.name+" > "+set.getTVars()[this.ref];
		else
			return idx+": bound "+var.definer.parent()+"."+var.definer+"."+var.name+" = "+val;
	}
}

public final class TArg {
	public static final TArg[] emptyArray = new TArg[0];

	public final TVSet			set;	// the set this TVar belongs to
	public final int			idx;	// position in the set (set.appls[idx] == this)
	public final ArgType		var;	// variable

	TArg(TVSet set, int idx, ArgType var) {
		this.set = set;
		this.idx = idx;
		this.var = var;
	}

	public TArg copy(TVSet set) {
		return new TArg(set, idx, var);
	}
}


@node
public class MetaType extends TypeDecl {

	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}
	@virtual typedef This  = MetaType;

	public final static MetaType[] emptyArray = new MetaType[0];
	static final MetaType dummy = new MetaType();

	@ref public Struct						package_clazz;
		 public String						q_name;	// qualified name

	public final String qname() {
		if (q_name != null)
			return q_name;
		Struct pkg = package_clazz;
		if (pkg == null || pkg == Env.root)
			q_name = id.uname;
		else
			q_name = (pkg.qname()+"."+id.uname).intern();
		return q_name;
	}

	@nodeview
	public static final view VMetaType of MetaType extends VTypeDecl {
		public				Struct					package_clazz;
	}

	public MetaType() {}
	public MetaType(Symbol id, Struct outer, int flags) {
		this.flags = flags | ACC_INTERFACE | ACC_MACRO;
		this.id = id;
		package_clazz = outer;
	}

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}
	
	public boolean checkTypeVersion(int version) {
		return type_decl_version == version;
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

	static class MetaTypeDFFunc extends DFFunc {
		final int res_idx;
		MetaTypeDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = DFState.makeNewState();
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new MetaTypeDFFunc(dfi);
	}

}

@node
public final class CoreMetaType extends MetaType {

	@virtual typedef This  = CoreMetaType;

	CoreType core_type;
	
	CoreMetaType() {}

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

@node
public final class ASTNodeMetaType extends MetaType {

	@virtual typedef This  = ASTNodeMetaType;

	@ref public Struct clazz;

	private TVarSet			templ_bindings;

	public static ASTNodeMetaType instance(Struct clazz) {
		if (clazz.ameta_type == null)
			clazz.ameta_type = new ASTNodeMetaType(clazz);
		return clazz.ameta_type;
	}

	ASTNodeMetaType() {}
	ASTNodeMetaType(Struct clazz) {
		this.clazz = clazz;
		this.templ_bindings = TVarSet.emptySet;
	}

	public Type[] getMetaSupers(Type tp) {
		return Type.emptyArray;
	}

	public boolean checkTypeVersion(int version) {
		return type_decl_version == version && clazz.type_decl_version == version;
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
		if (this.type_decl_version != clazz.type_decl_version)
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
		type_decl_version = clazz.type_decl_version;
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

@node
public final class CompaundMetaType extends MetaType {

	@virtual typedef This  = CompaundMetaType;

	public final Struct			clazz;
	
	private TVarSet			templ_bindings;
	
	public CompaundMetaType() {}
	public CompaundMetaType(Struct clazz) {
		this.clazz = clazz;
		if (this.clazz == Env.root) Env.root.imeta_type = this;
		this.templ_bindings = TVarSet.emptySet;
	}
	
	public Type[] getMetaSupers(Type tp) {
		Type[] stps = new Type[clazz.super_types.length];
		for (int i=0; i < clazz.super_types.length; i++)
			stps[i] = clazz.super_types[i].getType();
		return stps;
	}

	public boolean checkTypeVersion(int version) {
		return type_decl_version == version && clazz.type_decl_version == version;
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
		if (this.type_decl_version != clazz.type_decl_version)
			makeTemplBindings();
		return templ_bindings;
	}
	
	private void makeTemplBindings() {
		TVarBld vs = new TVarBld();
		foreach (TypeDef ad; clazz.args)
			vs.append(ad.getAType(), null);
		foreach (TypeDef td; clazz.members) {
			vs.append(td.getAType(), null);
		}
		foreach (TypeRef st; clazz.super_types; st.getType() ≢ null)
			vs.append(st.getType().bindings());
		templ_bindings = new TVarSet(vs.close());
		type_decl_version = clazz.type_decl_version;
	}

	public rule resolveNameAccessR(Type tp, ASTNode@ node, ResInfo info, String name)
	{
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in "+tp),
		clazz.checkResolved(),
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
		node @= clazz.members,
		node instanceof Field && ((Field)node).id.equals(name) && info.check(node)
	}
	private rule resolveNameR_3(Type tp, ASTNode@ node, ResInfo info, String name)
		MetaType@ sup;
	{
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= clazz.getAllSuperTypes(),
		sup.make(tp.bindings()).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(Type tp, ASTNode@ node, ResInfo info, String name)
		ASTNode@ forw;
		MetaType@ sup;
	{
		forw @= clazz.members,
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(tp).resolveNameAccessR(node,info,name)
	;	info.isSuperAllowed(),
		sup @= clazz.getAllSuperTypes(),
		sup instanceof CompaundMetaType,
		forw @= ((CompaundMetaType)sup).clazz.members,
		forw instanceof Field && ((Field)forw).isForward() && !((Field)forw).isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(tp).resolveNameAccessR(node,info,name)
	}

}

@node
public final class ArrayMetaType extends MetaType {

	@virtual typedef This  = ArrayMetaType;

	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	private static TVarSet			templ_bindings;
	public static final ArrayMetaType instance;
	static {
		templ_bindings = new TVarSet(new TVarBld(StdTypes.tpArrayArg, null).close());
		instance = new ArrayMetaType();
		instance.setResolved(true);
		instance.package_clazz.sub_decls.add(instance);
		Field length = new Field("length", StdTypes.tpInt, ACC_PUBLIC|ACC_FINAL);
		length.setMacro(true);
		length.acc = new Access(0xAA); //public:ro
		RewriteMatch rmatch = new RewriteMatch();
		length.init = rmatch;
		RewriteCase rget = new RewriteCase();
		rget.var = new RewritePattern("self", new ASTNodeType(Env.newStruct("IFldExpr", Env.newPackage("kiev.vlang"), ACC_PUBLIC)));
		rmatch.cases += rget;
		rget.stats.append(
			new RewriteNodeFactory(
				ArrayLengthExpr.class,new RewriteNodeArg[]{
					new RewriteNodeArg("obj",   new IFldExpr(new LVarExpr(0, rget.var), "obj"  )),
					new RewriteNodeArg("ident", new IFldExpr(new LVarExpr(0, rget.var), "ident"))
				}
			)
		);
		instance.members.add(length);
	}
	private ArrayMetaType() {
		super(new Symbol("_array_"), Env.newPackage("kiev.stdlib"), ACC_PUBLIC|ACC_FINAL);
		this.super_types.insert(0, new TypeRef(StdTypes.tpObject));
		this.args.add(StdTypes.tdArrayArg);
	}

	public Type[] getMetaSupers(Type tp) {
		Type[] stps = new Type[super_types.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = super_types[i].getType();
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
		node @= members,
		node instanceof Field && ((Field)node).id.equals(name) && info.check(node)
	;
		st @= getMetaSupers(tp),
		st.resolveNameAccessR(node,info,name)
	}
}

@node
public class ArgMetaType extends MetaType {

	@virtual typedef This  = ArgMetaType;

	public static final ArgMetaType instance = new ArgMetaType();
	private ArgMetaType() {}

	public Type[] getMetaSupers(Type tp) {
		ArgType at = (ArgType)tp;
		TypeRef[] ups = at.definer.super_types.getArray();
		if (ups.length == 0)
			return new Type[]{StdTypes.tpObject};
		Type[] stps = new Type[ups.length];
		for (int i=0; i < stps.length; i++)
			stps[i] = ups[i].getType();
		return stps;
	}

	public Type make(TVSet bindings) {
		throw new RuntimeException("make() in ArgType");
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
		sup @= ((ArgType)tp).definer.super_types.getArray(),
		sup.getType().resolveNameAccessR(node, info, name)
	}
}

@node
public class WrapperMetaType extends MetaType {

	@virtual typedef This  = WrapperMetaType;

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

@node
public class OuterMetaType extends MetaType {

	@virtual typedef This  = OuterMetaType;

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

@node
public class CallMetaType extends MetaType {

	@virtual typedef This  = CallMetaType;

	public static final CallMetaType instance = new CallMetaType();
	private CallMetaType() {}

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


