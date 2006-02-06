package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.be.java.JLvalueExpr;
import kiev.be.java.JAccessExpr;
import kiev.be.java.JIFldExpr;
import kiev.be.java.JContainerAccessExpr;
import kiev.be.java.JThisExpr;
import kiev.be.java.JLVarExpr;
import kiev.ir.java.RLVarExpr;
import kiev.be.java.JSFldExpr;
import kiev.be.java.JOuterThisAccessExpr;
import kiev.be.java.JUnwrapExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public abstract class LvalueExpr extends ENode {

	@virtual typedef This  = LvalueExpr;
	@virtual typedef NImpl = LvalueExprImpl;
	@virtual typedef VView = LvalueExprView;
	@virtual typedef JView = JLvalueExpr;

	@nodeimpl
	public abstract static class LvalueExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = LvalueExpr;
	}
	@nodeview
	public abstract static view LvalueExprView of LvalueExprImpl extends ENodeView {
	}

	public LvalueExpr(LvalueExprImpl impl) { super(impl); }
}

@nodeset
public class AccessExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	@virtual typedef This  = AccessExpr;
	@virtual typedef NImpl = AccessExprImpl;
	@virtual typedef VView = AccessExprView;
	@virtual typedef JView = JAccessExpr;

	@nodeimpl
	public static class AccessExprImpl extends LvalueExprImpl {		
		@virtual typedef ImplOf = AccessExpr;
		@att public ENode			obj;
		@att public NameRef			ident;
	}
	@nodeview
	public static view AccessExprView of AccessExprImpl extends LvalueExprView {
		public ENode	obj;
		public NameRef	ident;

		public int		getPriority() { return Constants.opAccessPriority; }

		public void mainResolveOut() {
			ASTNode[] res;
			Type[] tps;
	
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
				if (tp.resolveNameAccessR(v,info=new ResInfo(this,ResInfo.noStatic | ResInfo.noImports),ident.name) )
					res[si] = makeExpr(v,info,~obj);
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
					msg.append("\t").append(res).append('\n');
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
				this.obj = this.obj;
				throw new CompilerException(this, msg.toString());
			}
			this.replaceWithNode(res[idx]);
		}

		final ENode makeExpr(ASTNode v, ResInfo info, ASTNode o) {
			if( v instanceof Field ) {
				return info.buildAccess(this.getNode(), o, v);
			}
			else if( v instanceof Struct ) {
				TypeRef tr = new TypeRef(((Struct)v).ctype);
				return tr;
			}
			else {
				throw new CompilerException(this,"Identifier "+ident+" must be a class's field");
			}
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public AccessExpr() {
		super(new AccessExprImpl());
	}

	public AccessExpr(AccessExprImpl impl) {
		super(impl);
	}

	public AccessExpr(int pos) {
		this();
		this.pos = pos;
	}
	
	public AccessExpr(int pos, ENode obj, NameRef ident) {
		this();
		this.pos = pos;
		this.obj = obj;
		this.ident = ident;
	}
	
	public void resolve(Type reqType) throws CompilerException {
		ENode[] res;
		Type[] tps;

		// resolve access
		obj.resolve(null);

	try_static:
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType() };
			res = new ENode[1];
			if( ident.name.equals(nameThis) )
				res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
		}
		else {
			ENode e = obj;
			tps = e.getAccessTypes();
			res = new ENode[tps.length];
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				if( ident.name.equals(nameLength) ) {
					if( tp.isArray() ) {
						tps[si] = Type.tpInt;
						res[si] = new ArrayLengthExpr(pos,e.ncopy(), ident.ncopy());
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
			if (!(obj instanceof TypeRef) &&
				tp.resolveNameAccessR(v,info=new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),ident.name) )
				res[si] = makeExpr(v,info,~obj);
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
				msg.append("\t").append(res).append('\n');
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
			this.obj = this.obj;
			throw new CompilerException(this, msg.toString());
			//return;
		}
		this.replaceWithNodeResolve(reqType,~res[idx]);
	}

	public String toString() {
    	return obj+"."+ident;
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(ident.name);
		return dmp;
	}
}

@nodeset
public final class IFldExpr extends AccessExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	@virtual typedef This  = IFldExpr;
	@virtual typedef NImpl = IFldExprImpl;
	@virtual typedef VView = IFldExprView;
	@virtual typedef JView = JIFldExpr;

	@nodeimpl
	public static final class IFldExprImpl extends AccessExprImpl {		
		@virtual typedef ImplOf = IFldExpr;
		@ref public Field		var;
	}
	@nodeview
	public static final view IFldExprView of IFldExprImpl extends AccessExprView {
		public Field		var;

		public Operator getOp() { return BinaryOperator.Access; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public IFldExpr() {
		super(new IFldExprImpl());
	}

	public IFldExpr(int pos, ENode obj, NameRef ident, Field var) {
		this();
		this.pos = pos;
		this.obj = obj;
		this.ident = ident;
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		this();
		this.pos = pos;
		this.obj = obj;
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		this();
		this.pos = pos;
		this.obj = obj;
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public String toString() {
		if( obj.getPriority() < opAccessPriority )
			return "("+obj.toString()+")."+var.toString();
		else
			return obj.toString()+"."+var.toString();
	}

	public Type getType() {
		return Type.getRealType(obj.getType(),var.type);
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
	
	public void resolve(Type reqType) throws RuntimeException {
		obj.resolve(null);

		// Set violation of the field
		if( ctx_method != null
		 && obj instanceof LVarExpr && ((LVarExpr)obj).ident.equals(nameThis)
		)
			ctx_method.addViolatedField(var);

		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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

	public Dumper toJava(Dumper dmp) {
		if( obj.getPriority() < opAccessPriority ) {
			dmp.append('(').append(obj).append(").");
		} else {
			dmp.append(obj).append('.');
		}
		return dmp.append(var.name).space();
	}
}

@nodeset
public final class ContainerAccessExpr extends LvalueExpr {
	
	@dflow(out="index") private static class DFI {
	@dflow(in="this:in")	ENode		obj;
	@dflow(in="obj")		ENode		index;
	}

	@virtual typedef This  = ContainerAccessExpr;
	@virtual typedef NImpl = ContainerAccessExprImpl;
	@virtual typedef VView = ContainerAccessExprView;
	@virtual typedef JView = JContainerAccessExpr;

	@nodeimpl
	public static final class ContainerAccessExprImpl extends LvalueExprImpl {		
		@virtual typedef ImplOf = ContainerAccessExpr;
		@att public ENode		obj;
		@att public ENode		index;
	}
	@nodeview
	public static final view ContainerAccessExprView of ContainerAccessExprImpl extends LvalueExprView {
		public ENode		obj;
		public ENode		index;

		public int getPriority() { return opContainerElementPriority; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public ContainerAccessExpr() {
		super(new ContainerAccessExprImpl());
	}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		this();
		this.pos = pos;
		this.obj = obj;
		this.index = index;
	}

	public String toString() {
		if( obj.getPriority() < opContainerElementPriority )
			return "("+obj.toString()+")["+index.toString()+"]";
		else
			return obj.toString()+"["+index.toString()+"]";
	}

	public Type getType() {
		try {
			Type t = obj.getType();
			if( t.isArray() ) {
				return Type.getRealType(t,((ArrayType)t).arg);
			}
			else {
				// Resolve overloaded access method
				Method@ v;
				CallType mt = new CallType(new Type[]{index.getType()},Type.tpAny);
				ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(t,v,info,nameArrayOp,mt) )
					return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+t);
				return Type.getRealType(t,((Method)v).type.ret());
			}
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type t = obj.getType();
		if( t.isArray() ) {
			return new Type[]{Type.getRealType(t,((ArrayType)t).arg)};
		} else {
			Struct s = t.getStruct();
		lookup_op:
			for(;;) {
				s.checkResolved();
				if (s instanceof Struct) {
					Struct ss = (Struct)s;
					foreach(ASTNode n; ss.members; n instanceof Method && ((Method)n).name.equals(nameArrayOp))
						return new Type[]{Type.getRealType(t,((Method)n).type.ret())};
				}
				if( s.super_type != null ) {
					s = s.super_type.clazz;
					continue;
				}
				//throw new RuntimeException("Resolved object "+obj+" of type "+t+" is not an array and does not overrides '[]' operator");
				return Type.emptyArray;
			}
		}
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		obj.resolve(null);
		if( obj.getType().getStruct() != null ) {
			// May be an overloaded '[]' operator, ensure overriding
			Struct s = obj.getType().getStruct();
		lookup_op:
			for(;;) {
				s.checkResolved();
				if (s instanceof Struct) {
					Struct ss = (Struct)s;
					foreach(ASTNode n; ss.members; n instanceof Method && ((Method)n).name.equals(nameArrayOp))
						break lookup_op;
				}
				if( s.super_type != null ) {
					s = s.super_type.clazz;
					continue;
				}
				throw new RuntimeException("Resolved object "+obj+" of type "+obj.getType()+" is not an array and does not overrides '[]' operator");
			}
		}
		index.resolve(null);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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

@nodeset
public final class ThisExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static public final FormPar thisPar = new FormPar(0,Constants.nameThis,Type.tpVoid,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD);
	
	@virtual typedef This  = ThisExpr;
	@virtual typedef NImpl = ThisExprImpl;
	@virtual typedef VView = ThisExprView;
	@virtual typedef JView = JThisExpr;

	@nodeimpl
	public static final class ThisExprImpl extends LvalueExprImpl {		
		@virtual typedef ImplOf = ThisExpr;
	}
	@nodeview
	public static final view ThisExprView of ThisExprImpl extends LvalueExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public ThisExpr() {
		super(new ThisExprImpl());
	}
	public ThisExpr(int pos) {
		this();
		this.pos = pos;
	}
	public ThisExpr(boolean super_flag) {
		super(new ThisExprImpl());
		if (super_flag)
			this.setSuperExpr(true);
	}

	public String toString() { return isSuperExpr() ? "super" : "this"; }

	public Type getType() {
		try {
			if (ctx_clazz == null)
				return Type.tpVoid;
			if (ctx_clazz.name.short_name.equals(nameIdefault))
				return ctx_clazz.package_clazz.ctype;
			if (isSuperExpr())
				ctx_clazz.super_type;
			return ctx_clazz.ctype;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		if (ctx_method != null &&
			ctx_method.isStatic() &&
			!ctx_clazz.name.short_name.equals(nameIdefault)
		)
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(toString()).space();
	}
}

@nodeset
public final class LVarExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static final KString namePEnv = KString.from("$env");

	@virtual typedef This  = LVarExpr;
	@virtual typedef NImpl = LVarExprImpl;
	@virtual typedef VView = VLVarExpr;
	@virtual typedef JView = JLVarExpr;
	@virtual typedef RView = RLVarExpr;

	@nodeimpl
	public static final class LVarExprImpl extends LvalueExprImpl {		
		@virtual typedef ImplOf = LVarExpr;
		@att public NameRef		ident;
		@ref public Var			var;
	}
	@nodeview
	public static abstract view LVarExprView of LVarExprImpl extends LvalueExprView {
		public NameRef	ident;
		public Var		var;

		public Var getVar() {
			if (var != null)
				return var;
			Var@ v;
			ResInfo info = new ResInfo(this);
			if( !PassInfo.resolveNameR(this.getNode(),v,info,ident.name) )
				throw new CompilerException(this,"Unresolved var "+ident);
			var = v;
			return var;
		}
	}

	@nodeview
	public static final view VLVarExpr of LVarExprImpl extends LVarExprView {
		public boolean preResolveIn() {
			getVar(); // calls resolving
			return false;
		}
	
		public boolean mainResolveIn() {
			getVar(); // calls resolving
			return false;
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public LVarExpr() {
		super(new LVarExprImpl());
	}
	public LVarExpr(int pos, Var var) {
		this();
		this.pos = pos;
		this.var = var;
		this.ident = new NameRef(pos, var.name.name);
	}
	public LVarExpr(int pos, KString name) {
		this();
		this.pos = pos;
		this.ident = new NameRef(pos, name);
	}
	public LVarExpr(KString name) {
		super(new LVarExprImpl());
		this.ident = new NameRef(name);
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

	public Type getType() {
		try {
			return var.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{getVar()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.type};
		return (Type[])sni.getTypes().clone();
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		// Check if we try to access this var from local inner/anonymouse class
		if( ctx_clazz.isLocal() ) {
			if( getVar().ctx_clazz != this.ctx_clazz ) {
				var.setNeedProxy(true);
				setAsField(true);
				// Now we need to add this var as a fields to
				// local class and to initializer of this class
				Field vf;
				if( (vf = ctx_clazz.resolveField(ident.name,false)) == null ) {
					// Add field
					vf = ctx_clazz.addField(new Field(ident.name,var.type,ACC_PUBLIC));
					vf.setNeedProxy(true);
					vf.init = this.ncopy();
				}
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		return dmp.space();
	}
}

@nodeset
public final class SFldExpr extends AccessExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SFldExpr;
	@virtual typedef NImpl = SFldExprImpl;
	@virtual typedef VView = SFldExprView;
	@virtual typedef JView = JSFldExpr;

	@nodeimpl
	public static final class SFldExprImpl extends AccessExprImpl {		
		@virtual typedef ImplOf = SFldExpr;
		@ref public Field		var;
	}
	@nodeview
	public static final view SFldExprView of SFldExprImpl extends AccessExprView {
		public Field		var;

		public Operator getOp() { return BinaryOperator.Access; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public SFldExpr() {
		super(new SFldExprImpl());
	}

	public SFldExpr(int pos, Field var) {
		this();
		this.pos = pos;
		this.obj = new TypeRef(pos,var.ctx_clazz.ctype);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
	}

	public SFldExpr(int pos, Field var, boolean direct_access) {
		this();
		this.pos = pos;
		this.obj = new TypeRef(pos,var.ctx_clazz.ctype);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		if (direct_access) setAsField(true);
	}

	public String toString() { return var.toString(); }

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

	public Type getType() {
		try {
			return var.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new LvalDNode[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.type};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		// Set violation of the field
		if( ctx_method != null )
			ctx_method.addViolatedField(var);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

	public Dumper toJava(Dumper dmp) {
		Struct cl = (Struct)var.parent;
		ClazzName cln = cl.name;
		return dmp.space().append(cln).append('.').append(var.name).space();
	}

}

@nodeset
public final class OuterThisAccessExpr extends AccessExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = OuterThisAccessExpr;
	@virtual typedef NImpl = OuterThisAccessExprImpl;
	@virtual typedef VView = OuterThisAccessExprView;
	@virtual typedef JView = JOuterThisAccessExpr;

	@nodeimpl
	public static final class OuterThisAccessExprImpl extends AccessExprImpl {		
		@virtual typedef ImplOf = OuterThisAccessExpr;
		@ref public Struct			outer;
		@ref public NArr<Field>		outer_refs;
	}
	@nodeview
	public static final view OuterThisAccessExprView of OuterThisAccessExprImpl extends AccessExprView {
		public				Struct			outer;
		public:ro	NArr<Field>		outer_refs;

		public Operator getOp() { return BinaryOperator.Access; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public OuterThisAccessExpr() {
		super(new OuterThisAccessExprImpl());
	}

	public OuterThisAccessExpr(int pos, Struct outer) {
		this();
		this.pos = pos;
		this.obj = new TypeRef(pos,outer.ctype);
		this.ident = new NameRef(pos,nameThis);
		this.outer = outer;
	}

	public String toString() { return outer.name.toString()+".this"; }

	public Type getType() {
		try {
			if (ctx_clazz == null)
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

	public void resolve(Type reqType) throws RuntimeException {
		outer_refs.delAll();
		trace(Kiev.debugResolve,"Resolving "+this);
		Field ou_ref = outerOf(ctx_clazz);
		if( ou_ref == null )
			throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+ctx_clazz);
		do {
			trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
			outer_refs.append(ou_ref);
			if( ou_ref.type.isInstanceOf(outer.ctype) ) break;
			ou_ref = outerOf(ou_ref.type.getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].type.isInstanceOf(outer.ctype) )
			throw new RuntimeException("Outer class "+outer+" not found for inner class "+ctx_clazz);
		if( Kiev.debugResolve ) {
			StringBuffer sb = new StringBuffer("Outer 'this' resolved as this");
			for(int i=0; i < outer_refs.length; i++)
				sb.append("->").append(outer_refs[i].name);
			System.out.println(sb.toString());
		}
		if( ctx_method.isStatic() && !ctx_method.isVirtualStatic() ) {
			throw new RuntimeException("Access to 'this' in static method "+ctx_method);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}

@nodeset
public final class UnwrapExpr extends LvalueExpr {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = UnwrapExpr;
	@virtual typedef NImpl = UnwrapExprImpl;
	@virtual typedef VView = UnwrapExprView;
	@virtual typedef JView = JUnwrapExpr;

	@nodeimpl
	public static final class UnwrapExprImpl extends LvalueExprImpl {		
		@virtual typedef ImplOf = UnwrapExpr;
		@att public ENode		expr;
	}
	@nodeview
	public static final view UnwrapExprView of UnwrapExprImpl extends LvalueExprView {
		public ENode		expr;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public UnwrapExpr() {
		super(new UnwrapExprImpl());
	}

	public UnwrapExpr(ENode expr) {
		this();
		this.pos = pos;
		this.expr = expr;
	}

	public String toString() { return "(($unwrap)"+expr+")"; }

	public Type getType() {
		Type tp = expr.getType();
		if (tp instanceof CTimeType)
			return ((WrapperType)tp).getEnclosedType();
		return tp;
	}

	public void resolve(Type reqType) throws RuntimeException {
		trace(Kiev.debugResolve,"Resolving "+this);
		expr.resolve(reqType);
		Type tp = expr.getType();
		if!(tp instanceof CTimeType) {
			replaceWithNode(~expr);
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("(($unwrap)").append(expr).append(")");
	}
}


