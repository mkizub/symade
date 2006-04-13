package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.ir.java.RLvalueExpr;
import kiev.be.java.JLvalueExpr;
import kiev.ir.java.RAccessExpr;
import kiev.be.java.JAccessExpr;
import kiev.ir.java.RIFldExpr;
import kiev.be.java.JIFldExpr;
import kiev.ir.java.RContainerAccessExpr;
import kiev.be.java.JContainerAccessExpr;
import kiev.ir.java.RThisExpr;
import kiev.be.java.JThisExpr;
import kiev.ir.java.RLVarExpr;
import kiev.be.java.JLVarExpr;
import kiev.ir.java.RSFldExpr;
import kiev.be.java.JSFldExpr;
import kiev.ir.java.ROuterThisAccessExpr;
import kiev.be.java.JOuterThisAccessExpr;
import kiev.ir.java.RReinterpExpr;
import kiev.be.java.JReinterpExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public abstract class LvalueExpr extends ENode {

	@virtual typedef This  = LvalueExpr;
	@virtual typedef VView = VLvalueExpr;
	@virtual typedef JView = JLvalueExpr;
	@virtual typedef RView = RLvalueExpr;

	@nodeview
	public abstract static view VLvalueExpr of LvalueExpr extends VENode {
	}

	public LvalueExpr() {}
}

@node
public final class AccessExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	@virtual typedef This  = AccessExpr;
	@virtual typedef VView = VAccessExpr;
	@virtual typedef JView = JAccessExpr;
	@virtual typedef RView = RAccessExpr;

	@att public ENode			obj;
	@att public NameRef			ident;

	@nodeview
	public static final view VAccessExpr of AccessExpr extends VLvalueExpr {
		public ENode	obj;
		public NameRef	ident;

		public final ENode makeExpr(ASTNode v, ResInfo info, ASTNode o);

		public void mainResolveOut() {
			ASTNode[] res;
			Type[] tps;

			ENode obj = this.obj;
			// pre-resolve result
			if( obj instanceof TypeRef ) {
				tps = new Type[]{ ((TypeRef)obj).getType() };
				res = new ASTNode[1];
				if( ident.name.equals(nameThis) )
					res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
			}
			else {
				ENode e = obj;
				tps = e.getAccessTypes();
				res = new ASTNode[tps.length];
				for (int si=0; si < tps.length; si++) {
					Type tp = tps[si];
					if( ident.name.equals(nameLength) ) {
						if( tp.isArray() ) {
							tps[si] = Type.tpInt;
							res[si] = new ArrayLengthExpr(pos, e.ncopy(), ident.ncopy());
						}
					}
				}
				// fall down
			}
			for (int si=0; si < tps.length; si++) {
				if (res[si] != null)
					continue;
				Type tp = tps[si];
				DNode@ v;
				ResInfo info;
				if (tp.resolveNameAccessR(v,info=new ResInfo(this,ResInfo.noStatic | ResInfo.noImports),ident.name) ) {
					if (this.obj != null)
						res[si] = makeExpr(v,info,~this.obj);
					else
						res[si] = makeExpr(v,info,obj.ncopy());
				}
				else if (tp.resolveStaticNameR(v,info=new ResInfo(this),ident.name))
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
			this.replaceWithNode(res[idx]);
		}
	}
	
	public AccessExpr() {}

	public AccessExpr(int pos) {
		this.pos = pos;
	}
	
	public AccessExpr(int pos, ENode obj, NameRef ident) {
		this.pos = pos;
		this.obj = obj;
		this.ident = ident;
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public final ENode makeExpr(ASTNode v, ResInfo info, ASTNode o) {
		if( v instanceof Field ) {
			return info.buildAccess((AccessExpr)this, o, v);
		}
		else if( v instanceof Struct ) {
			TypeRef tr = new TypeRef(((Struct)v).ctype);
			return tr;
		}
		else {
			throw new CompilerException(this,"Identifier "+ident+" must be a class's field");
		}
	}
	
	public String toString() {
    	return obj+"."+ident;
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(ident.name);
		return dmp;
	}
}

@node
public final class IFldExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	@virtual typedef This  = IFldExpr;
	@virtual typedef VView = VIFldExpr;
	@virtual typedef JView = JIFldExpr;
	@virtual typedef RView = RIFldExpr;

	@att public ENode			obj;
	@att public NameRef			ident;
	@ref public Field			var;

	@nodeview
	public static final view VIFldExpr of IFldExpr extends VLvalueExpr {
		public ENode		obj;
		public NameRef		ident;
		public Field		var;
	}
	
	public IFldExpr() {}

	public IFldExpr(int pos, ENode obj, NameRef ident, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.ident = ident;
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		this.pos = pos;
		this.obj = obj;
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		this.pos = pos;
		this.obj = obj;
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Type getType() {
		return Type.getRealType(obj.getType(),var.type);
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
		Access.verifyRead(this,var);
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
		if( obj.getPriority() < opAccessPriority )
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
	
	public Dumper toJava(Dumper dmp) {
		if( obj.getPriority() < opAccessPriority ) {
			dmp.append('(').append(obj).append(").");
		} else {
			dmp.append(obj).append('.');
		}
		return dmp.append(var.name).space();
	}
}

@node
public final class ContainerAccessExpr extends LvalueExpr {
	
	@dflow(out="index") private static class DFI {
	@dflow(in="this:in")	ENode		obj;
	@dflow(in="obj")		ENode		index;
	}

	@virtual typedef This  = ContainerAccessExpr;
	@virtual typedef VView = VContainerAccessExpr;
	@virtual typedef JView = JContainerAccessExpr;
	@virtual typedef RView = RContainerAccessExpr;

	@att public ENode		obj;
	@att public ENode		index;

	@nodeview
	public static final view VContainerAccessExpr of ContainerAccessExpr extends VLvalueExpr {
		public ENode		obj;
		public ENode		index;
	}
	
	public ContainerAccessExpr() {}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		this.pos = pos;
		this.obj = obj;
		this.index = index;
	}

	public int getPriority() { return opContainerElementPriority; }

	public Type getType() {
		try {
			Type t = obj.getType();
			if( t.isArray() )
				return Type.getRealType(t,((ArrayType)t).arg);
			// Resolve overloaded access method
			Method@ v;
			CallType mt = new CallType(new Type[]{index.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(t,v,info,nameArrayOp,mt) )
				return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+t);
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
		if( t.isArray() )
			return new Type[]{Type.getRealType(t,((ArrayType)t).arg)};
		Method@ v;
		CallType mt = new CallType(new Type[]{index.getType()},Type.tpAny);
		ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
		if( !PassInfo.resolveBestMethodR(t,v,info,nameArrayOp,mt) )
			return Type.emptyArray; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+t);
		return new Type[]{Type.getRealType(t,((Method)v).type.ret())};
	}

	public Dumper toJava(Dumper dmp) {
		if( obj.getPriority() < opContainerElementPriority ) {
			dmp.append('(').append(obj).append(')');
		} else {
			dmp.append(obj);
		}
		dmp.append('[').append(index).append(']');
		return dmp;
	}
}

@node
public final class ThisExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static public final FormPar thisPar = new FormPar(0,Constants.nameThis,Type.tpVoid,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
	
	@virtual typedef This  = ThisExpr;
	@virtual typedef VView = VThisExpr;
	@virtual typedef JView = JThisExpr;
	@virtual typedef RView = RThisExpr;

	@nodeview
	public static final view VThisExpr of ThisExpr extends VLvalueExpr {
	}
	
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
			if (ctx_clazz == null)
				return Type.tpVoid;
			if (ctx_clazz.name.short_name == nameIFaceImpl)
				return ctx_clazz.package_clazz.ctype;
			if (isSuperExpr())
				ctx_clazz.super_type;
			return ctx_clazz.ctype;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public String toString() { return isSuperExpr() ? "super" : "this"; }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(toString()).space();
	}
}

@node
public final class LVarExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static final KString namePEnv = KString.from("$env");

	@virtual typedef This  = LVarExpr;
	@virtual typedef VView = VLVarExpr;
	@virtual typedef JView = JLVarExpr;
	@virtual typedef RView = RLVarExpr;

	@att public NameRef		ident;
	@ref public Var			var;

	@nodeview
	public static final view VLVarExpr of LVarExpr extends VLvalueExpr {
		public NameRef	ident;
		public Var		var;

		public Var getVar();

		public boolean preResolveIn() {
			getVar(); // calls resolving
			return false;
		}
	
		public boolean mainResolveIn() {
			getVar(); // calls resolving
			return false;
		}
	}
	
	public LVarExpr() {}
	public LVarExpr(int pos, Var var) {
		this.pos = pos;
		this.var = var;
		this.ident = new NameRef(pos, var.name.name);
	}
	public LVarExpr(int pos, KString name) {
		this.pos = pos;
		this.ident = new NameRef(pos, name);
	}
	public LVarExpr(KString name) {
		this.ident = new NameRef(name);
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
		ResInfo info = new ResInfo(this);
		if( !PassInfo.resolveNameR((ASTNode)this,v,info,ident.name) )
			throw new CompilerException(this,"Unresolved var "+ident);
		var = v;
		return var;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.ident = new NameRef(pos, ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2)));
		else
			this.ident = new NameRef(pos, KString.from(t.image));
	}
	
	public String toString() {
		if (var == null)
			return ident.toString();
		return var.toString();
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{getVar()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.type};
		return (Type[])sni.getTypes().clone();
	}
	
	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		return dmp.space();
	}
}

@node
public final class SFldExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SFldExpr;
	@virtual typedef VView = VSFldExpr;
	@virtual typedef JView = JSFldExpr;
	@virtual typedef RView = RSFldExpr;

	@att public ENode			obj;
	@att public NameRef			ident;
	@ref public Field			var;

	@nodeview
	public static final view VSFldExpr of SFldExpr extends VLvalueExpr {
		public ENode		obj;
		public NameRef		ident;
		public Field		var;
	}

	public SFldExpr() {}

	public SFldExpr(int pos, Field var) {
		this.pos = pos;
		this.obj = new TypeRef(pos,var.ctx_clazz.ctype);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
	}

	public SFldExpr(int pos, Field var, boolean direct_access) {
		this.pos = pos;
		this.obj = new TypeRef(pos,var.ctx_clazz.ctype);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		if (direct_access) setAsField(true);
	}

	public Operator getOp() { return BinaryOperator.Access; }

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
		Access.verifyRead((ASTNode)this,var);
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr())
				return var.init.getConstValue();
			else if (var.const_value != null) {
				return var.const_value.getConstValue();
			}
		}
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public String toString() { return var.toString(); }

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.type};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public Dumper toJava(Dumper dmp) {
		Struct cl = var.ctx_clazz;
		ClazzName cln = cl.name;
		return dmp.space().append(cln).append('.').append(var.name).space();
	}

}

@node
public final class OuterThisAccessExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = OuterThisAccessExpr;
	@virtual typedef VView = VOuterThisAccessExpr;
	@virtual typedef JView = JOuterThisAccessExpr;
	@virtual typedef RView = ROuterThisAccessExpr;

	@att public ENode			obj;
	@att public NameRef			ident;
	@ref public Struct			outer;
	@ref public NArr<Field>		outer_refs;

	@nodeview
	public static final view VOuterThisAccessExpr of OuterThisAccessExpr extends VLvalueExpr {
		public		ENode			obj;
		public		NameRef			ident;
		public		Struct			outer;
		public:ro	NArr<Field>		outer_refs;
	}

	public OuterThisAccessExpr() {}

	public OuterThisAccessExpr(int pos, Struct outer) {
		this.pos = pos;
		this.obj = new TypeRef(pos,outer.ctype);
		this.ident = new NameRef(pos,nameThis);
		this.outer = outer;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Type getType() {
		try {
			if (ctx_clazz == null || outer_refs.size() == 0)
				return outer.ctype;
			Type tp = ctx_clazz.ctype;
			foreach (Field f; outer_refs)
				tp = f.type.applay(tp);
			return tp;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return outer.ctype;
		}
	}

	public String toString() { return outer.name.toString()+".this"; }

	public static Field outerOf(Struct clazz) {
		foreach (ASTNode n; clazz.members; n instanceof Field) {
			Field f = (Field)n;
			if( f.name.name.startsWith(nameThisDollar) ) {
				trace(Kiev.debugResolve,"Name of field "+f+" starts with this$");
				return f;
			}
		}
		return null;
	}

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}

@node
public final class ReinterpExpr extends LvalueExpr {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ReinterpExpr;
	@virtual typedef VView = VReinterpExpr;
	@virtual typedef JView = JReinterpExpr;
	@virtual typedef RView = RReinterpExpr;

	@att public TypeRef		type;
	@att public ENode		expr;

	@nodeview
	public static final view VReinterpExpr of ReinterpExpr extends VLvalueExpr {
		public TypeRef		type;
		public ENode		expr;
	}

	public ReinterpExpr() {}

	public ReinterpExpr(Type type) {
		this.type = new TypeRef(type);
	}

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

	public int getPriority() { return opCastPriority; }

	public Type getType() {
		return this.type.getType();
	}

	public String toString() { return "(($reinterp "+type+")"+expr+")"; }

	public Dumper toJava(Dumper dmp) {
		return dmp.append(expr);
	}
}


