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
 * @version $Revision$
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

	public int		getPriority() { return Constants.opAccessPriority; }

	public final ENode makeExpr(ResInfo info, ASTNode o) {
		DNode dn = info.resolvedDNode();
		if (dn instanceof Field) {
			return info.buildAccess(this, o, info.resolvedSymbol());
		}
		else if (dn instanceof TypeDecl) {
			TypeRef tr = new TypeRef(dn.xtype);
			return tr;
		}
		else {
			throw new CompilerException(this,"Identifier "+ident+" must be a class's field");
		}
	}
	
	public String toString() {
    	return obj+"."+ident;
	}

	public void mainResolveOut() {
		ENode[] res;
		Type[] tps;

		ENode obj = this.obj;
		// pre-resolve result
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType() };
			res = new ENode[1];
			if( ident.equals(nameThis) )
				this.replaceWithNodeReWalk(new OuterThisAccessExpr(pos,(TypeRef)~obj));
		}
		else {
			tps = obj.getAccessTypes();
			res = new ENode[tps.length];
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			ResInfo info;
			if (tp.resolveNameAccessR(info=new ResInfo(this,this.ident,ResInfo.noStatic | ResInfo.noSyntaxContext)) ) {
				res[si] = makeExpr(info,obj);
			}
			else if (tp.meta_type.tdecl.resolveNameR(info=new ResInfo(this,this.ident))) {
				if (obj instanceof TypeRef && obj.getType() ≈ tp) {
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
			if (ctx_method != null && ctx_method.isMacro())
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
		this.replaceWithNodeReWalk(e);
	}

	public ANode doRewrite(RewriteContext ctx) {
		Type ot = obj.getType();
		if (ot.getErasedType() instanceof ASTNodeType) {
			boolean prim = obj.isPrimaryExpr();
			ANode o = this.obj.doRewrite(ctx);
			if (!prim)
				return (ANode)o.getVal(this.ident);
			return new AccessExpr(pos,(ENode)ctx.fixup(obj.pslot(),o),new SymbolRef<DNode>(ctx.replace(ident)));
		}
		return super.doRewrite(ctx);
	}
}

@ThisIsANode(name="IFld", lang=CoreLang)
public final class IFldExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="obj") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			obj;
	}

	@nodeAttr public ENode			obj;
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

	public Operator getOper() { return Operator.Access; }

	public Type getType() {
		Type ot = obj.getType();
		if (var == null)
			return StdTypes.tpVoid;
		if (ot.getErasedType() instanceof ASTNodeType) {
			String name = ("attr$"+var.sname+"$type").intern();
			int n = ot.getArgsLength();
			for (int i=0; i < n; i++) {
				if (ot.getArg(i).name == name)
					return ot.resolveArg(i);
			}
			if (var.getType().getErasedType() instanceof ASTNodeType)
				return var.getType();
			return new ASTNodeType(var.getType());
		} else {
			return Type.getRealType(ot,var.getType());
		}
	}

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr())
				return true;
			else if (var.const_value != null)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		MetaAccess.verifyRead(this,var);
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr())
				return var.init.getConstValue();
			else if (var.const_value != null) {
				return var.const_value.getConstValue();
			}
		}
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public String toString() {
		if (obj == null)
			return this.ident;
		if (obj.getPriority() < opAccessPriority)
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

	public void mainResolveOut() {
		if (this.var != null)
			return;

		ENode obj = this.obj;
		// pre-resolve result
		Type[] tps = obj.getAccessTypes();
		int len = tps.length;
		Field[] res = new Field[len];
		for (int si=0; si < len; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			ResInfo<Field> info = new ResInfo<Field>(this,this.ident,ResInfo.noStatic | ResInfo.noSyntaxContext | ResInfo.noForwards);
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
			if (ctx_method != null && ctx_method.isMacro())
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

	public ANode doRewrite(RewriteContext ctx) {
		Type ot = obj.getType();
		if (ot.getErasedType() instanceof ASTNodeType) {
			ANode obj = this.obj.doRewrite(ctx);
			return (ANode)ctx.toANode(obj.getVal(this.ident));
		}
		return super.doRewrite(ctx);
	}

	// verify resolved tree
	public boolean preVerify() {
		Field f = this.var;
		if (!f.isAttached()) {
			Type tp = obj.getType();
			ResInfo info = new ResInfo(this,f.sname,ResInfo.noStatic | ResInfo.noSyntaxContext);
			if (tp.resolveNameAccessR(info) ) {
				DNode dn = info.resolvedDNode();
				if (!info.isEmpty() || !(dn instanceof Field) || dn.getType() != f.getType()) {
					Kiev.reportError(this, "Re-resolved field "+dn+" does not match old field "+f);
				} else {
					f = (Field)dn;
					this.symbol = info.resolvedSymbol();
				}
			} else {
				Kiev.reportError(this, "Error resolving "+f+" in "+tp);
			}
		}
		if (f.isStatic() || (f.isMacro() && !f.isNative()))
			Kiev.reportError(this, "Bad instance field "+f+" access from "+obj);
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

	public int getPriority() { return opContainerElementPriority; }

	public ENode[] getEArgs() { return new ENode[]{obj,index}; }

	public Type getType() {
		try {
			Type t = obj.getType();
			if (t instanceof ArrayType)
				return Type.getRealType(t,t.arg);
			// Resolve overloaded access method
			CallType mt = new CallType(t,null,new Type[]{index.getType()},Type.tpAny,false);
			ResInfo<Method> info = new ResInfo<Method>(this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(t,info,mt) )
				return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayGetOp,mt)+" in "+t);
			return Type.getRealType(t,info.resolvedDNode().mtype.ret());
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public String toString() {
		if( obj.getPriority() < opContainerElementPriority )
			return "("+obj.toString()+")["+index.toString()+"]";
		else
			return obj.toString()+"["+index.toString()+"]";
	}

	public Type[] getAccessTypes() {
		Type t = obj.getType();
		if (t instanceof ArrayType)
			return new Type[]{Type.getRealType(t,t.arg)};
		CallType mt = new CallType(t,null,new Type[]{index.getType()},Type.tpAny,false);
		ResInfo<Method> info = new ResInfo<Method>(this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if( !PassInfo.resolveBestMethodR(t,info,mt) )
			return Type.emptyArray; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayGetOp,mt)+" in "+t);
		return new Type[]{Type.getRealType(t,info.resolvedDNode().mtype.ret())};
	}
}

@ThisIsANode(name="This", lang=CoreLang)
public final class ThisExpr extends LvalueExpr {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	static public final LVar thisPar = new LVar(0,Constants.nameThis,Type.tpVoid,Var.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
	
	public ThisExpr() {}
	public ThisExpr(int pos) {
		this.pos = pos;
	}

	public Type getType() {
		try {
			ComplexTypeDecl td = ctx_tdecl;
			if (td == null)
				return Type.tpVoid;
			if (td.sname == nameIFaceImpl)
				return td.ctx_tdecl.xtype;
			return td.xtype;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public String toString() { return "this"; }

	public void mainResolveOut() {
		Method m = ctx_method;
		boolean rt = (m != null && m.isMacro());
		if (rt != isRewriteTarget())
			setRewriteTarget(rt);
	}

	public ANode doRewrite(RewriteContext ctx) {
		if (isRewriteTarget()) {
			if (ctx.root instanceof CallExpr)
				return ((CallExpr)ctx.root).obj;
		}
		return super.doRewrite(ctx);
	}
}

@ThisIsANode(name="Super", lang=CoreLang)
public final class SuperExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	public SuperExpr() {
		setSuperExpr(true);
	}
	public SuperExpr(int pos) {
		this.pos = pos;
		setSuperExpr(true);
	}

	public Type getType() {
		try {
			if (ctx_tdecl == null)
				return Type.tpVoid;
			return ctx_tdecl.super_types[0].getType();
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
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

	public Type getType() {
		try {
			return var.getType();
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Var getVarSafe() {
		if (var != null)
			return var;
		ResInfo<Var> info = new ResInfo<Var>(this,this.ident);
		if( !PassInfo.resolveNameR((ASTNode)this,info) )
			throw new CompilerException(this,"Unresolved var "+ident);
		this.symbol = info.resolvedSymbol();
		return info.resolvedDNode();
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.ident = t.image;
	}
	
	public boolean preResolveIn() {
		Var v = getVarSafe(); // calls resolving
		if (v.ctx_root != this.ctx_root) {
			this.ident = v.sname;
			getVarSafe();
		}
		return false;
	}

	public boolean mainResolveIn() {
		Var v = getVarSafe(); // calls resolving
		if (v.ctx_root != this.ctx_root) {
			this.ident = v.sname;
			getVarSafe();
		}
		return false;
	}

	public String toString() {
		return ident.toString();
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = DataFlowInfo.getDFlow(this).out().getNodeInfo(new Var[]{getVarSafe()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.getType()};
		return (Type[])sni.getTypes().clone();
	}
	
	public ANode doRewrite(RewriteContext ctx) {
		if (ctx.args.containsKey(ident))
			return ctx.toANode(ctx.args.get(ident));
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
			Field f = obj.getType().meta_type.tdecl.resolveField(ident, false);
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
		else if (var.ctx_tdecl == null) {
			this.obj = new TypeRef(Type.tpVoid);
			this.obj.setAutoGenerated(true);
		}
		else {
			this.obj = new TypeRef(var.ctx_tdecl.xtype);
			this.obj.setAutoGenerated(true);
		}
		this.symbol = var.symbol;
	}

	public Operator getOper() { return Operator.Access; }

	public Type getType() {
		try {
			return var.getType();
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr())
				return true;
			else if (var.const_value != null)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		MetaAccess.verifyRead((ASTNode)this,var);
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr())
				return var.init.getConstValue();
			else if (var.const_value != null) {
				return var.const_value.getConstValue();
			}
		}
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public String toString() { return ident.toString(); }

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = DataFlowInfo.getDFlow(this).out().getNodeInfo(new Var[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.getType()};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "obj" && this.dnode != null && this.dnode.parent() == Env.getRoot())
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public void mainResolveOut() {
		if (var != null) {
			if (!var.isStatic())
				throw new CompilerException(this, "Field "+var+" is not static");
			if (obj == null) {
				if (var.ctx_tdecl == null) {
					this.obj = new TypeRef(Type.tpVoid);
					this.obj.setAutoGenerated(true);
				} else {
					obj = new TypeRef(var.ctx_tdecl.xtype);
					obj.setAutoGenerated(true);
				}
			}
			return;
		}
		
		if (this.obj == null)
			this.obj = new TypeRef(StdTypes.tpVoid);
		
		Type tp = this.obj.getType();
		ScopeOfNames scope = tp.meta_type.tdecl;
		if (tp ≡ StdTypes.tpVoid)
			scope = Env.getRoot();
		else
			scope = tp.meta_type.tdecl;
		ResInfo info = new ResInfo(this,this.ident);
		if (!scope.resolveNameR(info))
			throw new CompilerException(this, "Unresolved static field "+ident+" in "+scope);
		DNode res = info.resolvedDNode();
		if !(res instanceof Field || !res.isStatic())
			throw new CompilerException(this, "Resolved "+ident+" in "+scope+" is not a static field");
		this.symbol = info.resolvedSymbol();
	}

	// verify resolved tree
	public boolean preVerify() {
		Field f = this.var;
		if (!f.isAttached()) {
			Type tp = obj.getType();
			ResInfo info = new ResInfo(this,f.sname);
			if (tp.meta_type.tdecl.resolveNameR(info)) {
				DNode dn = info.resolvedDNode();
				if (!info.isEmpty() || !(dn instanceof Field) || dn.getType() != f.getType()) {
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
	@nodeData public Var∅			outer_refs;

	public OuterThisAccessExpr() {}

	public OuterThisAccessExpr(int pos, TypeRef outer) {
		this.pos = pos;
		this.outer = outer;
		this.ident = nameThis;
	}

	public Operator getOper() { return Operator.Access; }

	public Type getType() {
		try {
			if (ctx_tdecl == null || outer_refs.length == 0)
				return outer.getType();
			Type tp = ctx_tdecl.xtype;
			foreach (Field f; outer_refs)
				tp = f.getType().applay(tp);
			return tp;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return outer.getType();
		}
	}

	public String toString() { return getType().meta_type.tdecl.qname().replace('·','.')+".this"; }

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
		Field ou_ref = OuterThisAccessExpr.outerOf((Struct)ctx_tdecl);
		if( ou_ref == null )
			throw new CompilerException(this, "Outer 'this' reference in non-inner or static inner class "+ctx_tdecl);
		do {
			outer_refs.append(ou_ref);
			if( ou_ref.getType().isInstanceOf(outer.getType()) ) break;
			ou_ref = OuterThisAccessExpr.outerOf(ou_ref.getType().getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].getType().isInstanceOf(outer.getType()) )
			throw new CompilerException(this, "Outer class "+outer+" not found for inner class "+ctx_tdecl);
		if( ctx_method.isStatic() && !ctx_method.isVirtualStatic() )
			throw new CompilerException(this, "Access to 'this' in static method "+ctx_method);
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

	public Operator getOper() { return Operator.Reinterp; }

	public ENode[] getEArgs() { return new ENode[]{ctype, expr}; }

	public String toString() { return getOper().toString(this); }

	public int getPriority() { return opCastPriority; }

	public Type getType() {
		return this.ctype.getType();
	}
}


