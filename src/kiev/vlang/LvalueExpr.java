package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;

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
import kiev.ir.java15.RLVarExpr;
import kiev.be.java15.JLVarExpr;
import kiev.ir.java15.RSFldExpr;
import kiev.be.java15.JSFldExpr;
import kiev.ir.java15.ROuterThisAccessExpr;
import kiev.be.java15.JOuterThisAccessExpr;
import kiev.ir.java15.RReinterpExpr;
import kiev.be.java15.JReinterpExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public abstract class LvalueExpr extends ENode {

	@virtual typedef This  ≤ LvalueExpr;
	@virtual typedef JView ≤ JLvalueExpr;
	@virtual typedef RView ≤ RLvalueExpr;

	public LvalueExpr() {}
}

@node(name="Access")
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
			if( ident.name.equals(nameThis) )
				this.replaceWithNodeReWalk(new OuterThisAccessExpr(pos,(TypeRef)~obj));
		}
		else {
			ENode e = obj;
			tps = e.getAccessTypes();
			res = new ENode[tps.length];
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			DNode@ v;
			ResInfo info;
			if (tp.resolveNameAccessR(v,info=new ResInfo(this,ident.name,ResInfo.noStatic | ResInfo.noImports)) ) {
				if (this.obj != null)
					res[si] = makeExpr(v,info,this.obj);
				else
					res[si] = makeExpr(v,info,obj);
			}
			else if (tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,ident.name)))
				res[si] = makeExpr(v,info,tp.getStruct());
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
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			this.obj = obj;
			throw new CompilerException(this, msg.toString());
		}
		ENode e = res[idx].closeBuild();
		this.replaceWithNodeReWalk(e);
	}
}

@node(name="IFld")
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
		DNode sym = ident.symbol;
		if (sym instanceof Field)
			return (Field)sym;
		return null;
	}

	public IFldExpr() {}

	public IFldExpr(int pos, ENode obj, SymbolRef<DNode> ident, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.ident = ident;
		this.ident.symbol = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.ident = new SymbolRef<DNode>(pos,var);
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		this.pos = pos;
		this.obj = obj;
		this.ident = new SymbolRef<DNode>(pos,var);
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public IFldExpr(ENode obj, String ident) {
		this.obj = obj;
		this.ident = new SymbolRef<DNode>(0,ident);
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		Type ot = obj.getType();
		if (ot instanceof ASTNodeType) {
			String name = ("attr$"+var.id+"$type").intern();
			foreach (TVar tv; ot.bindings().tvars; tv.var.name == name) {
				return ot.resolve(tv.var);
			}
			return new ASTNodeType(var.type.getStruct());
		} else {
			return Type.getRealType(ot,var.type);
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

	public LvalDNode[] getAccessPath() {
		if (obj instanceof LVarExpr) {
			LVarExpr va = (LVarExpr)obj;
			if (va.getVar().isFinal() && va.getVar().isForward())
				return new LvalDNode[]{va.getVar(), this.var};
			return null;
		}
		if (obj instanceof IFldExpr) {
			IFldExpr ae = (IFldExpr)obj;
			if !(ae.var.isFinal() || ae.var.isForward())
				return null;
			LvalDNode[] path = ae.getAccessPath();
			if (path == null)
				return null;
			return (LvalDNode[])Arrays.append(path, var);
		}
		return null;
	}

	public Object doRewrite(RewriteContext ctx) {
		ASTNode obj = (ASTNode)obj.doRewrite(ctx);
		return obj.getVal(ident.name);
	}

	// verify resolved tree
	public boolean preVerify() {
		Field f = this.var;
		if (!f.isAttached()) {
			Type tp = obj.getType();
			DNode@ v;
			ResInfo info;
			if (tp.resolveNameAccessR(v,info=new ResInfo(this,f.id.sname,ResInfo.noStatic | ResInfo.noImports)) ) {
				if (!info.isEmpty() || !(v instanceof Field) || ((Field)v).type != f.type) {
					Kiev.reportError(this, "Re-resolved field "+v+" does not match old field "+f);
				} else {
					f = (Field)v;
					ident.symbol = f;
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

@node(name="SetAccess")
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

@node(name="This")
public final class ThisExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static public final FormPar thisPar = new FormPar(0,Constants.nameThis,Type.tpVoid,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
	
	@virtual typedef This  = ThisExpr;
	@virtual typedef JView = JThisExpr;
	@virtual typedef RView = RThisExpr;

	public ThisExpr() {}
	public ThisExpr(int pos) {
		this.pos = pos;
	}
	public ThisExpr(boolean super_flag) {
		if (super_flag)
			this.setSuperExpr(true);
	}

	public Type getType() {
		try {
			if (ctx_tdecl == null)
				return Type.tpVoid;
			if (ctx_tdecl.id.uname == nameIFaceImpl)
				return ctx_tdecl.package_clazz.xtype;
			if (isSuperExpr())
				ctx_tdecl.super_types[0].getType();
			return ctx_tdecl.xtype;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public String toString() { return isSuperExpr() ? "super" : "this"; }
}

@node(name="LVar")
public final class LVarExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = LVarExpr;
	@virtual typedef JView = JLVarExpr;
	@virtual typedef RView = RLVarExpr;

	@getter public Var get$var() {
		DNode sym = ident.symbol;
		if (sym instanceof Var)
			return (Var)sym;
		return null;
	}

	public LVarExpr() {}
	public LVarExpr(int pos, Var var) {
		this.pos = pos;
		this.ident = new SymbolRef<DNode>(pos, var);
	}
	public LVarExpr(int pos, String name) {
		this.pos = pos;
		this.ident = new SymbolRef<DNode>(pos, name);
	}
	public LVarExpr(String name) {
		this.ident = new SymbolRef<DNode>(name);
	}

	public Type getType() {
		try {
			return var.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Var getVar() {
		if (var != null)
			return var;
		Var@ v;
		ResInfo info = new ResInfo(this,ident.name);
		if( !PassInfo.resolveNameR((ASTNode)this,v,info) )
			throw new CompilerException(this,"Unresolved var "+ident);
		ident.symbol = v;
		return (Var)v;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = new SymbolRef<DNode>(pos, ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2)));
		else
			this.ident = new SymbolRef<DNode>(pos, t.image);
	}
	
	public boolean preResolveIn() {
		getVar(); // calls resolving
		return false;
	}

	public boolean mainResolveIn() {
		getVar(); // calls resolving
		return false;
	}

	public String toString() {
		return ident.toString();
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{getVar()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.type};
		return (Type[])sni.getTypes().clone();
	}
	
	public Object doRewrite(RewriteContext ctx) {
		Var var = this.var;
		if (var instanceof RewritePattern)
			return ctx.root;
		if (var instanceof FormPar && ctx.root instanceof CallExpr && var.parent() == ((CallExpr)ctx.root).func) {
			int idx = 0;
			foreach (FormPar fp; ((Method)var.parent()).params; fp.kind == FormPar.PARAM_NORMAL) {
				if (fp == var)
					return ctx.args[idx];
				idx++;
			}
		}
		throw new RuntimeException("doRewrite on unresolved var "+this);
	}
}

@node(name="SFld")
public final class SFldExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SFldExpr;
	@virtual typedef JView = JSFldExpr;
	@virtual typedef RView = RSFldExpr;

	@att public ENode			obj;

	@getter public Field get$var() {
		DNode sym = ident.symbol;
		if (sym instanceof Field)
			return (Field)sym;
		return null;
	}

	public SFldExpr() {}

	public SFldExpr(int pos, Field var) {
		this.pos = pos;
		this.obj = new TypeRef(var.ctx_tdecl.xtype);
		this.ident = new SymbolRef<DNode>(pos,var);
	}

	public SFldExpr(int pos, Field var, boolean direct_access) {
		this.pos = pos;
		this.obj = new TypeRef(var.ctx_tdecl.xtype);
		this.ident = new SymbolRef<DNode>(pos,var);
		if (direct_access) setAsField(true);
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		try {
			return var.type;
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
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.type};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public void mainResolveOut() {
		if (var != null) {
			if (!var.isStatic())
				throw new CompilerException(this, "Field "+var+" is not static");
			if (obj == null)
				obj = new TypeRef(var.ctx_tdecl.xtype);
			return;
		}
		
		if !(obj instanceof TypeRef)
			throw new CompilerException(this, "Static field access requires type as accessor");
		Type tp = this.obj.getType();
		DNode@ v;
		ResInfo info;
		tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,ident.name));
		DNode res = (DNode)v;
		if (res == null)
			throw new CompilerException(this, "Unresolved static field "+ident+" in "+tp);
		if !(res instanceof Field || !res.isStatic())
			throw new CompilerException(this, "Resolved "+ident+" in "+tp+" is not a static field");
		ident.symbol = res;
	}

	// verify resolved tree
	public boolean preVerify() {
		Field f = this.var;
		if (!f.isAttached()) {
			Type tp = obj.getType();
			DNode@ v;
			ResInfo info;
			if (tp.meta_type.tdecl.resolveNameR(v,info=new ResInfo(this,f.id.sname))) {
				if (!info.isEmpty() || !(v instanceof Field) || ((Field)v).type != f.type) {
					Kiev.reportError(this, "Re-resolved field "+v+" does not match old field "+f);
				} else {
					f = (Field)v;
					ident.symbol = f;
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

@node(name="OuterThis")
public final class OuterThisAccessExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = OuterThisAccessExpr;
	@virtual typedef JView = JOuterThisAccessExpr;
	@virtual typedef RView = ROuterThisAccessExpr;

	@att public TypeRef			outer;
	@ref public Field[]			outer_refs;

	public OuterThisAccessExpr() {}

	public OuterThisAccessExpr(int pos, TypeRef outer) {
		this.pos = pos;
		this.outer = outer;
		this.ident = new SymbolRef<DNode>(pos,nameThis);
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

	public String toString() { return getType().meta_type.tdecl.qname().toString()+".this"; }

	public static Field outerOf(TypeDecl clazz) {
		foreach (Field f; clazz.members) {
			if( f.id.uname.startsWith(nameThisDollar) ) {
				trace(Kiev.debugResolve,"Name of field "+f+" starts with this$");
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

@node(name="Reinterp")
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


