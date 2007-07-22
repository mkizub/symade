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

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RLvalueExpr;
import kiev.be.java15.JLvalueExpr;
import kiev.ir.java15.RAccessExpr;
import kiev.be.java15.JAccessExpr;
import kiev.ir.java15.RIFldExpr;
import kiev.be.java15.JIFldExpr;
import kiev.ir.java15.RContainerAccessExpr;
import kiev.be.java15.JContainerAccessExpr;
import kiev.ir.java15.RThisExpr;
import kiev.be.java15.JThisExpr;
import kiev.ir.java15.RSuperExpr;
import kiev.be.java15.JSuperExpr;
import kiev.ir.java15.RLVarExpr;
import kiev.be.java15.JLVarExpr;
import kiev.ir.java15.RSFldExpr;
import kiev.be.java15.JSFldExpr;
import kiev.ir.java15.ROuterThisAccessExpr;
import kiev.be.java15.JOuterThisAccessExpr;
import kiev.ir.java15.RReinterpExpr;
import kiev.be.java15.JReinterpExpr;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(lang=CoreLang)
public abstract class LvalueExpr extends ENode {

	@virtual typedef This  ≤ LvalueExpr;
	@virtual typedef JView ≤ JLvalueExpr;
	@virtual typedef RView ≤ RLvalueExpr;

	public LvalueExpr() {}
}

@node(name="Access", lang=CoreLang)
public final class AccessExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	@virtual typedef This  = AccessExpr;
	@virtual typedef JView = JAccessExpr;
	@virtual typedef RView = RAccessExpr;

	@att public ENode			obj;

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

	public final ENode makeExpr(ASTNode v, ResInfo info, ASTNode o) {
		if( v instanceof Field ) {
			return info.buildAccess((AccessExpr)this, o, v);
		}
		else if( v instanceof TypeDecl ) {
			TypeRef tr = new TypeRef(((TypeDecl)v).xtype);
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
			DNode@ v;
			ResInfo info;
			if (tp.resolveNameAccessR(v,info=new ResInfo(this,this.ident,ResInfo.noStatic | ResInfo.noImports)) ) {
				res[si] = makeExpr(v,info,obj);
			}
			else if (tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,this.ident))) {
				if (obj instanceof TypeRef && obj.getType() ≈ tp) {
					res[si] = makeExpr(v,info,obj);
				} else {
					TypeRef tr = new TypeRef(tp);
					if (obj == null)
						tr.setAutoGenerated(true);
					else
						tr.pos = obj.pos;
					res[si] = makeExpr(v,info,tr);
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
			return new AccessExpr(pos,(ENode)ctx.fixup(obj.pslot(),o),new SymbolRef(ctx.replace(ident)));
		}
		return super.doRewrite(ctx);
	}
}

@node(name="IFld", lang=CoreLang)
public final class IFldExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	@virtual typedef This  = IFldExpr;
	@virtual typedef JView = JIFldExpr;
	@virtual typedef RView = RIFldExpr;

	@att public ENode			obj;
	@abstract
	@ref public:ro Field		var;

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
		this.symbol = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.symbol = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		this.pos = pos;
		this.obj = obj;
		this.symbol = var;
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public IFldExpr(ENode obj, String ident) {
		this.obj = obj;
		this.ident = ident;
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		Type ot = obj.getType();
		if (var == null)
			return StdTypes.tpVoid;
		if (ot.getErasedType() instanceof ASTNodeType) {
			String name = ("attr$"+var.sname+"$type").intern();
			foreach (TVar tv; ot.bindings().tvars; tv.var.name == name) {
				return ot.resolve(tv.var);
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
			return String.valueOf(ident);
		if (obj.getPriority() < opAccessPriority)
			return "("+obj.toString()+")."+var.toString();
		else
			return obj.toString()+"."+var.toString();
	}

	public Var[] getAccessPath() {
		if (obj instanceof LVarExpr) {
			LVarExpr va = (LVarExpr)obj;
			if (va.getVar().isFinal() && va.getVar().isForward())
				return new Var[]{va.getVar(), this.var};
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
			Field@ v;
			ResInfo info;
			if (tp.resolveNameAccessR(v,info=new ResInfo(this,this.ident,ResInfo.noStatic | ResInfo.noImports | ResInfo.noForwards)) ) {
				res[si] = v;
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
		this.symbol = res[idx];
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
			DNode@ v;
			ResInfo info;
			if (tp.resolveNameAccessR(v,info=new ResInfo(this,f.sname,ResInfo.noStatic | ResInfo.noImports)) ) {
				if (!info.isEmpty() || !(v instanceof Field) || ((Field)v).type != f.type) {
					Kiev.reportError(this, "Re-resolved field "+v+" does not match old field "+f);
				} else {
					f = (Field)v;
					this.symbol = f;
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

@node(name="SetAccess", lang=CoreLang)
public final class ContainerAccessExpr extends LvalueExpr {
	
	@dflow(out="index") private static class DFI {
	@dflow(in="this:in")	ENode		obj;
	@dflow(in="obj")		ENode		index;
	}

	@virtual typedef This  = ContainerAccessExpr;
	@virtual typedef JView = JContainerAccessExpr;
	@virtual typedef RView = RContainerAccessExpr;

	@att public ENode		obj;
	@att public ENode		index;

	public ContainerAccessExpr() {}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		this.pos = pos;
		this.obj = obj;
		this.index = index;
	}

	public int getPriority() { return opContainerElementPriority; }

	public ENode[] getArgs() { return new ENode[]{obj,index}; }

	public Type getType() {
		try {
			Type t = obj.getType();
			if (t instanceof ArrayType)
				return Type.getRealType(t,t.arg);
			// Resolve overloaded access method
			Method@ v;
			CallType mt = new CallType(t,null,new Type[]{index.getType()},Type.tpAny,false);
			ResInfo info = new ResInfo(this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(t,v,info,mt) )
				return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayGetOp,mt)+" in "+t);
			return Type.getRealType(t,((Method)v).type.ret());
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
		Method@ v;
		CallType mt = new CallType(t,null,new Type[]{index.getType()},Type.tpAny,false);
		ResInfo info = new ResInfo(this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
		if( !PassInfo.resolveBestMethodR(t,v,info,mt) )
			return Type.emptyArray; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayGetOp,mt)+" in "+t);
		return new Type[]{Type.getRealType(t,((Method)v).type.ret())};
	}
}

@node(name="This", lang=CoreLang)
public final class ThisExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static public final LVar thisPar = new LVar(0,Constants.nameThis,Type.tpVoid,Var.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
	
	@virtual typedef This  = ThisExpr;
	@virtual typedef JView = JThisExpr;
	@virtual typedef RView = RThisExpr;

	public ThisExpr() {}
	public ThisExpr(int pos) {
		this.pos = pos;
	}

	public Type getType() {
		try {
			if (ctx_tdecl == null)
				return Type.tpVoid;
			if (ctx_tdecl.sname == nameIFaceImpl)
				return ctx_tdecl.package_clazz.dnode.xtype;
			return ctx_tdecl.xtype;
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
		if (isRewriteTarget())
			return ((CallExpr)ctx.root).obj;
		return super.doRewrite(ctx);
	}
}

@node(name="Super", lang=CoreLang)
public final class SuperExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SuperExpr;
	@virtual typedef JView = JSuperExpr;
	@virtual typedef RView = RSuperExpr;

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

@node(name="LVar", lang=CoreLang)
public final class LVarExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = LVarExpr;
	@virtual typedef JView = JLVarExpr;
	@virtual typedef RView = RLVarExpr;

	@getter public Var get$var() {
		DNode sym = this.dnode;
		if (sym instanceof Var)
			return (Var)sym;
		return null;
	}

	public LVarExpr() {}
	public LVarExpr(int pos, Var var) {
		this.pos = pos;
		this.symbol = var;
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

	public Var getVar() {
		if (var != null)
			return var;
		Var@ v;
		ResInfo info = new ResInfo(this,this.ident);
		if( !PassInfo.resolveNameR((ASTNode)this,v,info) )
			throw new CompilerException(this,"Unresolved var "+ident);
		this.symbol = v;
		return (Var)v;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.ident = t.image;
	}
	
	public boolean preResolveIn() {
		Var v = getVar(); // calls resolving
		if (v.ctx_root != this.ctx_root) {
			this.ident = v.sname;
			getVar();
		}
		return false;
	}

	public boolean mainResolveIn() {
		Var v = getVar(); // calls resolving
		if (v.ctx_root != this.ctx_root) {
			this.ident = v.sname;
			getVar();
		}
		return false;
	}

	public String toString() {
		return ident.toString();
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = DataFlowInfo.getDFlow(this).out().getNodeInfo(new Var[]{getVar()});
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

@node(name="SFld", lang=CoreLang)
public final class SFldExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SFldExpr;
	@virtual typedef JView = JSFldExpr;
	@virtual typedef RView = RSFldExpr;

	@att public TypeRef			obj;

	@getter public Field get$var() {
		DNode sym = this.dnode;
		if (sym instanceof Field)
			return (Field)sym;
		if (obj != null) {
			Field f = obj.getType().meta_type.tdecl.resolveField(ident, false);
			if (f != null && f.isStatic()) {
				this.symbol = f;
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
		} else {
			this.obj = new TypeRef(var.ctx_tdecl.xtype);
			this.obj.setAutoGenerated(true);
		}
		this.symbol = var;
	}

	public Operator getOp() { return Operator.Access; }

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

	public void mainResolveOut() {
		if (var != null) {
			if (!var.isStatic())
				throw new CompilerException(this, "Field "+var+" is not static");
			if (obj == null) {
				obj = new TypeRef(var.ctx_tdecl.xtype);
				obj.setAutoGenerated(true);
			}
			return;
		}
		
		if (this.obj == null) {
			this.obj = new TypeRef(Env.getRoot().xtype);
		}
		
		Type tp = this.obj.getType();
		DNode@ v;
		ResInfo info;
		tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,this.ident));
		DNode res = (DNode)v;
		if (res == null)
			throw new CompilerException(this, "Unresolved static field "+ident+" in "+tp);
		if !(res instanceof Field || !res.isStatic())
			throw new CompilerException(this, "Resolved "+ident+" in "+tp+" is not a static field");
		this.symbol = res;
	}

	// verify resolved tree
	public boolean preVerify() {
		Field f = this.var;
		if (!f.isAttached()) {
			Type tp = obj.getType();
			DNode@ v;
			ResInfo info;
			if (tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,f.sname))) {
				if (!info.isEmpty() || !(v instanceof Field) || ((Field)v).type != f.type) {
					Kiev.reportError(this, "Re-resolved field "+v+" does not match old field "+f);
				} else {
					f = (Field)v;
					this.symbol = f;
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

@node(name="OuterThis", lang=CoreLang)
public final class OuterThisAccessExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = OuterThisAccessExpr;
	@virtual typedef JView = JOuterThisAccessExpr;
	@virtual typedef RView = ROuterThisAccessExpr;

	@att public TypeRef			outer;
	@ref public Var[]			outer_refs;

	public OuterThisAccessExpr() {}

	public OuterThisAccessExpr(int pos, TypeRef outer) {
		this.pos = pos;
		this.outer = outer;
		this.ident = nameThis;
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		try {
			if (ctx_tdecl == null || outer_refs.length == 0)
				return outer.getType();
			Type tp = ctx_tdecl.xtype;
			foreach (Field f; outer_refs)
				tp = f.type.applay(tp);
			return tp;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return outer.getType();
		}
	}

	public String toString() { return getType().meta_type.tdecl.qname().replace('\u001f','.')+".this"; }

	public static Field outerOf(TypeDecl clazz) {
		foreach (Field f; clazz.getAllFields()) {
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
			if( ou_ref.type.isInstanceOf(outer.getType()) ) break;
			ou_ref = OuterThisAccessExpr.outerOf(ou_ref.type.getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].type.isInstanceOf(outer.getType()) )
			throw new CompilerException(this, "Outer class "+outer+" not found for inner class "+ctx_tdecl);
		if( ctx_method.isStatic() && !ctx_method.isVirtualStatic() )
			throw new CompilerException(this, "Access to 'this' in static method "+ctx_method);
	}
}

@node(name="Reinterp", lang=CoreLang)
public final class ReinterpExpr extends LvalueExpr {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ReinterpExpr;
	@virtual typedef JView = JReinterpExpr;
	@virtual typedef RView = RReinterpExpr;

	@att public TypeRef		type;
	@att public ENode		expr;

	public ReinterpExpr() {}

	public ReinterpExpr(int pos, Type type, ENode expr) {
		this.pos = pos;
		this.type = new TypeRef(type);
		this.expr = expr;
	}

	public ReinterpExpr(int pos, TypeRef type, ENode expr) {
		this.pos = pos;
		this.type = type;
		this.expr = expr;
	}

	public Operator getOp() { return Operator.Reinterp; }

	public ENode[] getArgs() { return new ENode[]{type, expr}; }

	public String toString() { return getOp().toString(this); }

	public int getPriority() { return opCastPriority; }

	public Type getType() {
		return this.type.getType();
	}
}


