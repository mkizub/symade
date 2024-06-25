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
 * @author Maxim Kizub
 * @version $Revision: 298 $
 *
 */

@ThisIsANode(lang=CoreLang)
public abstract class LvalueExpr extends ENode {

	public LvalueExpr() {}
}

@ThisIsANode(name="Access", lang=CoreLang)
public final class AccessExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="obj") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			obj;
	}

	@nodeAttr public ENode			obj;

	public AccessExpr() {}

	public AccessExpr(int pos) {
		this.pos = pos;
	}
	
	public AccessExpr(int pos, ENode obj, SymbolRef<DNode> ident) {
		this.pos = pos;
		this.obj = obj;
		if (ident.symbol != null)
			this.symbol = ident.symbol;
		else
			this.ident = ident.name;
	}

	public AccessExpr(int pos, ENode obj, String ident) {
		this.pos = pos;
		this.obj = obj;
		this.ident = ident;
	}

	public int getLvalArity() { return 1; }

	public int		getPriority(Env env) { return Constants.opAccessPriority; }

	public final ENode makeExpr(ResInfo info, ASTNode o) {
		DNode dn = info.resolvedDNode();
		if (dn instanceof Field) {
			return info.buildAccess(this, o, info.resolvedSymbol());
		}
		else if (dn instanceof TypeDecl) {
			TypeRef tr = new TypeRef(dn.getType(info.env));
			return tr;
		}
		else {
			throw new CompilerException(this,"Identifier "+ident+" must be a class's field");
		}
	}
	
	public String toString() {
    	return obj+"."+ident;
	}

	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {
		ENode[] res;
		Type[] tps;

		ENode obj = this.obj;
		// pre-resolve result
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType(env) };
			res = new ENode[1];
			if( ident.equals(nameThis) )
				this.replaceWithNodeReWalk(new OuterThisAccessExpr(pos,(TypeRef)~obj),parent,slot);
		}
		else {
			tps = obj.getAccessTypes(env);
			res = new ENode[tps.length];
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			// try to resolve instance members
			ResInfo info = new ResInfo(env,this,this.ident,ResInfo.noStatic | ResInfo.noSyntaxContext);
			if (obj instanceof TypeRef) {
				if (obj.getTypeDecl(env).isSingleton()) {
					info.enterForward(obj.getTypeDecl(env).resolveField(env,nameInstance));
					if (tp.resolveNameAccessR(info) ) {
						// resolved in a singleton
						res[si] = makeExpr(info,obj);
						continue;
					}
				}
			}
			else if (tp.resolveNameAccessR(info) ) {
				// resolved in an instance
				res[si] = makeExpr(info,obj);
				continue;
			}
			// try to resolve static members
			info = new ResInfo(env,this,this.ident);
			if (tp.meta_type.tdecl.resolveNameR(info)) {
				if (obj instanceof TypeRef && obj.getType(env) ≈ tp) {
					res[si] = makeExpr(info,obj);
				} else {
					TypeRef tr = new TypeRef(tp);
					if (obj == null)
						tr.setAutoGenerated(true);
					else
						tr.pos = obj.pos;
					res[si] = makeExpr(info,tr);
				}
			}
		}
		int cnt = 0;
		int idx = -1;
		for (int si=0; si < res.length; si++) {
			if (res[si] != null) {
				cnt ++;
				if (idx < 0) idx = si;
			}
		}
		if (cnt > 1) {
			StringBuffer msg = new StringBuffer("Umbigous access:\n");
			for(int si=0; si < res.length; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(res[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		if (cnt == 0) {
			if (Env.ctxMethod(this) != null && Env.ctxMethod(this).isMacro())
				return;
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		ENode e = res[idx].closeBuild();
		if (isPrimaryExpr())
			e.setPrimaryExpr(true);
		this.replaceWithNodeReWalk(e,parent,slot);
	}
}

@ThisIsANode(name="IFld", lang=CoreLang)
public final class IFldExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="obj") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			obj;
	}

	@nodeAttr public ENode			obj;
	@AttrBinDumpInfo(ignore=true)
	@abstract
	@nodeData public:ro Field		var;

	@getter public Field get$var() {
		DNode sym = this.dnode;
		if (sym instanceof Field)
			return (Field)sym;
		return null;
	}

	public IFldExpr() {}

	public IFldExpr(int pos, ENode obj, SymbolRef<DNode> ident, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.symbol = var.symbol;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.symbol = var.symbol;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		this.pos = pos;
		this.obj = obj;
		this.symbol = var.symbol;
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public IFldExpr(ENode obj, String ident) {
		this.obj = obj;
		this.ident = ident;
	}

	public CoreOperation getOperation(Env env) { env.coreFuncs.fIFldAccess.operation }

	public int getLvalArity() { return 1; }

	public Type getType(Env env) {
		Type ot = obj.getType(env);
		if (var == null)
			return env.tenv.tpVoid;
		if (ot.getErasedType() instanceof ASTNodeType) {
			String name = ("attr$"+var.sname+"$type").intern();
			int n = ot.getArgsLength();
			for (int i=0; i < n; i++) {
				if (ot.getArg(i).name == name)
					return ot.resolveArg(i);
			}
			if (var.getType(env).getErasedType() instanceof ASTNodeType)
				return var.getType(env);
			return new ASTNodeType(var.getType(env));
		} else {
			return Type.getRealType(ot,var.getType(env));
		}
	}

	public boolean	isConstantExpr(Env env) {
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr(env))
				return true;
			else if (var.const_value != null)
				return true;
		}
		return false;
	}
	public Object	getConstValue(Env env) {
		MetaAccess.verifyRead(this,var);
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr(env))
				return var.init.getConstValue(env);
			else if (var.const_value != null) {
				return var.const_value.getConstValue(env);
			}
		}
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public String toString() {
		if (obj == null)
			return this.ident;
		if (obj.getPriority(Env.getEnv()) < opAccessPriority)
			return "("+obj.toString()+")."+this.ident;
		else
			return obj.toString()+"."+this.ident;
	}

	public Var[] getAccessPath() {
		if (obj instanceof LVarExpr) {
			LVarExpr va = (LVarExpr)obj;
			if (va.getVarSafe().isFinal() && va.getVarSafe().isForward())
				return new Var[]{va.getVarSafe(), this.var};
			return null;
		}
		if (obj instanceof IFldExpr) {
			IFldExpr ae = (IFldExpr)obj;
			if !(ae.var.isFinal() || ae.var.isForward())
				return null;
			Var[] path = ae.getAccessPath();
			if (path == null)
				return null;
			return (Var[])Arrays.append(path, var);
		}
		return null;
	}

	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {
		if (this.var != null)
			return;

		ENode obj = this.obj;
		// pre-resolve result
		Type[] tps = obj.getAccessTypes(env);
		int len = tps.length;
		Field[] res = new Field[len];
		for (int si=0; si < len; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			ResInfo<Field> info = new ResInfo<Field>(env,this,this.ident,ResInfo.noStatic | ResInfo.noSyntaxContext | ResInfo.noForwards);
			if (tp.resolveNameAccessR(info) ) {
				res[si] = info.resolvedDNode();
			}
		}
		int cnt = 0;
		int idx = -1;
		for (int si=0; si < len; si++) {
			if (res[si] != null) {
				cnt ++;
				if (idx < 0) idx = si;
			}
		}
		if (cnt > 1) {
			StringBuffer msg = new StringBuffer("Umbigous access:\n");
			for(int si=0; si < len; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('.').append(res[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		if (cnt == 0) {
			if (Env.ctxMethod(this) != null && Env.ctxMethod(this).isMacro())
				return;
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < len; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		this.symbol = res[idx].symbol;
	}

	public INode doRewrite(RewriteContext ctx) {
		Type ot = obj.getType(ctx.env);
		if (ot.getErasedType() instanceof ASTNodeType) {
			INode obj = this.obj.doRewrite(ctx);
			return ctx.toINode(obj.getVal(obj.getAttrSlot(this.ident)));
		}
		return super.doRewrite(ctx);
	}

	// verify resolved tree
	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		Field f = this.var;
		if (f == null || !f.isAttached()) {
			Type tp = obj.getType(env);
			ResInfo info = new ResInfo(env,this,ident,ResInfo.noStatic | ResInfo.noSyntaxContext);
			if (tp.resolveNameAccessR(info) ) {
				DNode dn = info.resolvedDNode();
				if (!info.isEmpty() || !(dn instanceof Field) || (f != null && dn.getType(env) != f.getType(env))) {
					Kiev.reportError(this, "Re-resolved field "+dn+" does not match old field "+f);
				} else {
					f = (Field)dn;
					this.symbol = info.resolvedSymbol();
				}
			} else {
				Kiev.reportError(this, "Error resolving "+ident+" in "+tp);
			}
		}
		if (f == null || f.isStatic() || (f.isMacro() && !f.isNative()))
			Kiev.reportError(this, "Bad instance field "+ident+" access from "+obj);
		return true;
	}
}

@ThisIsANode(name="SetAccess", lang=CoreLang)
public final class ContainerAccessExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="index") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		obj;
	@DataFlowDefinition(in="obj")		ENode		index;
	}

	@nodeAttr public ENode		obj;
	@nodeAttr public ENode		index;

	public ContainerAccessExpr() {}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		this.pos = pos;
		this.obj = obj;
		this.index = index;
	}

	public int getLvalArity() { return 2; }

	public int getPriority(Env env) { return opContainerElementPriority; }

	public ENode[] getEArgs() { return new ENode[]{obj,index}; }

	public Type getType(Env env) {
		try {
			Type t = obj.getType(env);
			if (t instanceof ArrayType)
				return Type.getRealType(t,t.arg);
			// Resolve overloaded access method
			CallType mt = new CallType(t,null,new Type[]{index.getType(env)},env.tenv.tpAny,false);
			ResInfo<Method> info = new ResInfo<Method>(env,this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(t,info,mt) )
				return env.tenv.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayGetOp,mt)+" in "+t);
			return Type.getRealType(t,info.resolvedDNode().mtype.ret());
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return env.tenv.tpVoid;
		}
	}

	public String toString() {
		if( obj.getPriority(Env.getEnv()) < opContainerElementPriority )
			return "("+obj.toString()+")["+index.toString()+"]";
		else
			return obj.toString()+"["+index.toString()+"]";
	}

	public Type[] getAccessTypes(Env env) {
		Type t = obj.getType(env);
		if (t instanceof ArrayType)
			return new Type[]{Type.getRealType(t,t.arg)};
		CallType mt = new CallType(t,null,new Type[]{index.getType(env)},env.tenv.tpAny,false);
		ResInfo<Method> info = new ResInfo<Method>(env,this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( !PassInfo.resolveBestMethodR(t,info,mt) )
			return Type.emptyArray; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayGetOp,mt)+" in "+t);
		return new Type[]{Type.getRealType(t,info.resolvedDNode().mtype.ret())};
	}
}

@ThisIsANode(name="This", lang=CoreLang)
public final class ThisExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	public ThisExpr() {}
	public ThisExpr(int pos) {
		this.pos = pos;
	}

	public int getLvalArity() { return 0; }

	public Type getType(Env env) {
		try {
			ComplexTypeDecl td = Env.ctxTDecl(this);
			if (td == null)
				return env.tenv.tpVoid;
			if (td.sname == nameIFaceImpl)
				return Env.ctxTDecl(td).getType(env);
			return td.getType(env);
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return env.tenv.tpVoid;
		}
	}

	public String toString() { return "this"; }
}

@ThisIsANode(name="Super", lang=CoreLang)
public final class SuperExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	public SuperExpr() {
	}
	public SuperExpr(int pos) {
		this.pos = pos;
	}

	public int getLvalArity() { return 0; }

	public Type getType(Env env) {
		try {
			if (Env.ctxTDecl(this) == null)
				return env.tenv.tpVoid;
			return Env.ctxTDecl(this).super_types[0].getType(env);
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return env.tenv.tpVoid;
		}
	}

	public String toString() { return "super"; }
}

@ThisIsANode(name="LVarExpr", lang=CoreLang)
public final class LVarExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	@virtual @abstract
	public:ro Var			var;

	@getter public Var get$var() {
		DNode sym = this.dnode;
		if (sym instanceof Var)
			return (Var)sym;
		return null;
	}

	public LVarExpr() {}
	public LVarExpr(int pos, Var var) {
		this.pos = pos;
		this.symbol = var.symbol;
	}
	public LVarExpr(int pos, String name) {
		this.pos = pos;
		this.ident = name;
	}
	public LVarExpr(String name) {
		this.ident = name;
	}

	public int getLvalArity() { return 0; }

	public Type getType(Env env) {
		try {
			return var.getType(env);
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return env.tenv.tpVoid;
		}
	}

	public Var getVarSafe() {
		if (var != null)
			return var;
		ResInfo<Var> info = new ResInfo<Var>(Env.getEnv(),this,this.ident);
		if( !PassInfo.resolveNameR((ASTNode)this,info) )
			throw new CompilerException(this,"Unresolved var "+ident);
		this.symbol = info.resolvedSymbol();
		return info.resolvedDNode();
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		Var v = getVarSafe(); // calls resolving
		if (!Env.hasSameRoot(v, this)) {
			this.ident = v.sname;
			getVarSafe();
		}
		return false;
	}

	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) {
		Var v = getVarSafe(); // calls resolving
		if (!Env.hasSameRoot(v, this)) {
			this.ident = v.sname;
			getVarSafe();
		}
		return false;
	}

	public String toString() {
		return ident.toString();
	}

	public Type[] getAccessTypes(Env env) {
		ScopeNodeInfo sni = DataFlowInfo.getDFlow(this).out().getNodeInfo(new Var[]{getVarSafe()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.getType(env)};
		return (Type[])sni.getTypes().clone();
	}
	
	public INode doRewrite(RewriteContext ctx) {
		if (ctx.args.containsKey(ident))
			return ctx.toINode(ctx.args.get(ident));
		return super.doRewrite(ctx); //throw new RuntimeException("doRewrite on unresolved var "+this);
	}
}

@ThisIsANode(name="SFld", lang=CoreLang)
public final class SFldExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public TypeRef			obj;

	@virtual @abstract
	public:ro Field			var;

	@getter public Field get$var() {
		DNode sym = this.dnode;
		if (sym instanceof Field)
			return (Field)sym;
		if (obj != null) {
			Field f = obj.getType(Env.getEnv()).meta_type.tdecl.resolveField(Env.getEnv(), ident, false);
			if (f != null && f.isStatic()) {
				this.symbol = f.symbol;
				return f;
			}
		}
		return null;
	}

	public SFldExpr() {}

	public SFldExpr(int pos, Field var) {
		this(pos, null, var);
	}

	public SFldExpr(int pos, TypeRef obj, Field var) {
		this.pos = pos;
		if (obj != null) {
			this.obj = obj;
		}
		else if (Env.ctxTDecl(var) == null) {
			this.obj = new TypeRef(Env.getEnv().tenv.tpVoid);
			this.obj.setAutoGenerated(true);
		}
		else {
			this.obj = new TypeRef(Env.ctxTDecl(var).getType(Env.getEnv()));
			this.obj.setAutoGenerated(true);
		}
		this.symbol = var.symbol;
	}

	public int getLvalArity() { return 0; }

	public CoreOperation getOperation(Env env) { env.coreFuncs.fSFldAccess.operation }

	public Type getType(Env env) {
		try {
			return var.getType(env);
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return env.tenv.tpVoid;
		}
	}

	public boolean	isConstantExpr(Env env) {
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr(env))
				return true;
			else if (var.const_value != null)
				return true;
		}
		return false;
	}
	public Object	getConstValue(Env env) {
		MetaAccess.verifyRead((ASTNode)this,var);
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr(env))
				return var.init.getConstValue(env);
			else if (var.const_value != null) {
				return var.const_value.getConstValue(env);
			}
		}
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public String toString() { return ident.toString(); }

	public Type[] getAccessTypes(Env env) {
		Type[] types;
		ScopeNodeInfo sni = DataFlowInfo.getDFlow(this).out().getNodeInfo(new Var[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.getType(env)};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "obj" && this.dnode != null && this.dnode.parent() == Env.getEnv())
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {
		if (var != null) {
			if (!var.isStatic())
				throw new CompilerException(this, "Field "+var+" is not static");
			if (obj == null) {
				if (Env.ctxTDecl(var) == null) {
					this.obj = new TypeRef(env.tenv.tpVoid);
					this.obj.setAutoGenerated(true);
				} else {
					obj = new TypeRef(Env.ctxTDecl(var).getType(env));
					obj.setAutoGenerated(true);
				}
			}
			return;
		}
		
		if (this.obj == null)
			this.obj = new TypeRef(env.tenv.tpVoid);
		
		Type tp = this.obj.getType(env);
		ScopeOfNames scope = tp.meta_type.tdecl;
		if (tp ≡ env.tenv.tpVoid)
			scope = Env.getEnv().root;
		else
			scope = tp.meta_type.tdecl;
		ResInfo info = new ResInfo(env,this,this.ident);
		if (!scope.resolveNameR(info))
			throw new CompilerException(this, "Unresolved static field "+ident+" in "+scope);
		DNode res = info.resolvedDNode();
		if !(res instanceof Field || !res.isStatic())
			throw new CompilerException(this, "Resolved "+ident+" in "+scope+" is not a static field");
		this.symbol = info.resolvedSymbol();
	}

	// verify resolved tree
	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		Field f = this.var;
		if (f == null || !f.isAttached()) {
			Type tp = obj.getType(env);
			ResInfo info = new ResInfo(env,this,f.sname);
			if (tp.meta_type.tdecl.resolveNameR(info)) {
				DNode dn = info.resolvedDNode();
				if (!info.isEmpty() || !(dn instanceof Field) || dn.getType(env) != f.getType(env)) {
					Kiev.reportError(this, "Re-resolved field "+dn+" does not match old field "+f);
				} else {
					f = (Field)dn;
					this.symbol = info.resolvedSymbol();
				}
			} else {
				Kiev.reportError(this, "Error resolving "+f+" in "+tp);
			}
		}
		if (!f.isStatic() || (f.isMacro() && !f.isNative()))
			Kiev.reportError(this, "Bad static field "+f+" access from "+obj);
		return true;
	}
}

@ThisIsANode(name="OuterThis", lang=CoreLang)
public final class OuterThisAccessExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public TypeRef			outer;
	@AttrBinDumpInfo(ignore=true)
	@nodeData public Var∅			outer_refs;

	public OuterThisAccessExpr() {}

	public OuterThisAccessExpr(int pos, TypeRef outer) {
		this.pos = pos;
		this.outer = outer;
		this.ident = nameThis;
	}

	public int getLvalArity() { return 0; }

	public CoreOperation getOperation(Env env) { env.coreFuncs.fOuterThisAccess.operation }

	public Type getType(Env env) {
		try {
			if (Env.ctxTDecl(this) == null || outer_refs.length == 0)
				return outer.getType(env);
			Type tp = Env.ctxTDecl(this).getType(env);
			foreach (Field f; outer_refs)
				tp = f.getType(env).applay(tp);
			return tp;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return outer.getType(env);
		}
	}

	public String toString() { return getType(Env.getEnv()).meta_type.tdecl.qname().replace('·','.')+".this"; }

	public static Field outerOf(ComplexTypeDecl clazz) {
		foreach (Field f; clazz.members) {
			if( f.sname.startsWith(nameThisDollar) ) {
				trace(Kiev.debug && Kiev.debugResolve,"Name of field "+f+" starts with this$");
				return f;
			}
		}
		return null;
	}
	
	public void setupOuterFields() {
		this.outer_refs.delAll();
		Env env = Env.getEnv();
		Field ou_ref = OuterThisAccessExpr.outerOf((Struct)Env.ctxTDecl(this));
		if( ou_ref == null )
			throw new CompilerException(this, "Outer 'this' reference in non-inner or static inner class "+Env.ctxTDecl(this));
		do {
			outer_refs.append(ou_ref);
			if( ou_ref.getType(env).isInstanceOf(outer.getType(env)) ) break;
			ou_ref = OuterThisAccessExpr.outerOf(ou_ref.getType(env).getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].getType(env).isInstanceOf(outer.getType(env)) )
			throw new CompilerException(this, "Outer class "+outer+" not found for inner class "+Env.ctxTDecl(this));
		if( Env.ctxMethod(this).isStatic() && !Env.ctxMethod(this).isVirtualStatic() )
			throw new CompilerException(this, "Access to 'this' in static method "+Env.ctxMethod(this));
	}
}

@ThisIsANode(name="Reinterp", lang=CoreLang)
public final class ReinterpExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	}

	@nodeAttr public TypeRef		ctype;
	@nodeAttr public ENode			expr;

	public ReinterpExpr() {}

	public ReinterpExpr(int pos, Type ctype, ENode expr) {
		this.pos = pos;
		this.ctype = new TypeRef(ctype);
		this.expr = expr;
	}

	public ReinterpExpr(int pos, TypeRef ctype, ENode expr) {
		this.pos = pos;
		this.ctype = ctype;
		this.expr = expr;
	}

	public int getLvalArity() {
		ENode expr = this.expr;
		if (expr == null)
			return -1;
		return expr.getLvalArity();
	}

	public CoreOperation getOperation(Env env) { env.coreFuncs.fReinterp.operation }

	public ENode[] getEArgs() { return new ENode[]{ctype, expr}; }

	public String toString() { toStringByOpdef() }

	public int getPriority(Env env) { return opCastPriority; }

	public Type getType(Env env) {
		return this.ctype.getType(env);
	}
}


