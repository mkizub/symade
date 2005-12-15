package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.Instr.*;
import kiev.vlang.Operator.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JLvalueExprView;
import kiev.be.java.JAccessExprView;
import kiev.be.java.JIFldExprView;
import kiev.be.java.JContainerAccessExprView;
import kiev.be.java.JThisExprView;
import kiev.be.java.JLVarExprView;
import kiev.be.java.JSFldExprView;
import kiev.be.java.JOuterThisAccessExprView;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public abstract class LvalueExpr extends ENode {

	@node
	public abstract static class LvalueExprImpl extends ENodeImpl {
		public LvalueExprImpl() {}
		public LvalueExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public abstract static view LvalueExprView of LvalueExprImpl extends ENodeView {
		public LvalueExprView(LvalueExprImpl $view) {
			super($view);
		}
	}

	public abstract LvalueExprView		getLvalueExprView() alias operator(210,fy,$cast);
	public abstract JLvalueExprView		getJLvalueExprView() alias operator(210,fy,$cast);
	
	public LvalueExpr(LvalueExprImpl impl) { super(impl); }
}

@node
public class AccessExpr extends LvalueExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	private static KString nameWrapperSelf = KString.from("$self");

	@node
	public static class AccessExprImpl extends LvalueExprImpl {		
		@att public ENode			obj;
		@att public NameRef			ident;

		public AccessExprImpl() {}
		public AccessExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static view AccessExprView of AccessExprImpl extends LvalueExprView {
		public ENode	obj;
		public NameRef	ident;
	}
	
	@att public abstract virtual ENode			obj;
	
	@att public abstract virtual NameRef		ident;
	
	
	public NodeView			getNodeView()			{ return new AccessExprView((AccessExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new AccessExprView((AccessExprImpl)this.$v_impl); }
	public LvalueExprView	getLvalueExprView()		{ return new AccessExprView((AccessExprImpl)this.$v_impl); }
	public AccessExprView	getAccessExprView()		{ return new AccessExprView((AccessExprImpl)this.$v_impl); }

	@getter public ENode		get$obj()			{ return this.getAccessExprView().obj; }
	@getter public NameRef		get$ident()			{ return this.getAccessExprView().ident; }
	
	@setter public void set$obj(ENode val)			{ this.getAccessExprView().obj = val; }
	@setter public void set$ident(NameRef val)		{ this.getAccessExprView().ident = val; }
	

	public AccessExpr() {
		super(new AccessExprImpl());
	}

	public AccessExpr(AccessExprImpl impl) {
		super(impl);
	}

	public AccessExpr(int pos) {
		this(new AccessExprImpl(pos));
	}
	
	public AccessExpr(int pos, ENode obj, NameRef ident) {
		this(new AccessExprImpl(pos));
		this.obj = obj;
		this.ident = ident;
	}

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
			//tps = new Type[]{e.getType()};
			tps = e.getAccessTypes();
			res = new ASTNode[tps.length];
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				if( ident.name.equals(nameWrapperSelf) && tp.isReference() ) {
					if (tp.isWrapper()) {
						tps[si] = ((WrapperType)tp).getUnwrappedType();
						res[si] = obj;
					}
					// compatibility with previois version
					else if (tp.isInstanceOf(Type.tpPrologVar)) {
						tps[si] = tp;
						res[si] = (ENode)~obj;
					}
				}
				else if (ident.name.byteAt(0) == '$') {
					while (tp.isWrapper())
						tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
				}
				else if( ident.name.equals(nameLength) ) {
					if( tp.isArray() ) {
						tps[si] = Type.tpInt;
						res[si] = new ArrayLengthExpr(pos,(ENode)e.copy(), (NameRef)ident.copy());
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
				res[si] = makeExpr(v,info,(ENode)~obj);
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
				if( ident.name.equals(nameWrapperSelf) && tp.isReference() ) {
					if (tp.isWrapper()) {
						tps[si] = ((WrapperType)tp).getUnwrappedType();
						res[si] = obj;
					}
					else if (tp.isInstanceOf(Type.tpPrologVar)) {
						tps[si] = tp;
						res[si] = obj;
					}
				}
				else if (ident.name.byteAt(0) == '$') {
					while (tp.isWrapper())
						tps[si] = tp = ((WrapperType)tp).getUnwrappedType();
				}
				else if( ident.name.equals(nameLength) ) {
					if( tp.isArray() ) {
						tps[si] = Type.tpInt;
						res[si] = new ArrayLengthExpr(pos,(ENode)e.copy(), (NameRef)ident.copy());
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
				res[si] = makeExpr(v,info,(ENode)~obj);
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
		this.replaceWithNodeResolve(reqType,(ENode)~res[idx]);
	}

	private ENode makeExpr(ASTNode v, ResInfo info, ASTNode o) {
		if( v instanceof Field ) {
			return info.buildAccess(this, o, v);
		}
		else if( v instanceof Struct ) {
			TypeRef tr = new TypeRef(((Struct)v).type);
			return tr;
		}
		else {
			throw new CompilerException(this,"Identifier "+ident+" must be a class's field");
		}
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
    	return obj+"."+ident;
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(ident.name);
		return dmp;
	}
}

@node
public final class IFldExpr extends AccessExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}

	public static final int[] masks =
		{	0,
			0x1       ,0x3       ,0x7       ,0xF       ,
			0x1F      ,0x3F      ,0x7F      ,0xFF      ,
			0x1FF     ,0x3FF     ,0x7FF     ,0xFFF     ,
			0x1FFF    ,0x3FFF    ,0x7FFF    ,0xFFFF    ,
			0x1FFFF   ,0x3FFFF   ,0x7FFFF   ,0xFFFFF   ,
			0x1FFFFF  ,0x3FFFFF  ,0x7FFFFF  ,0xFFFFFF  ,
			0x1FFFFFF ,0x3FFFFFF ,0x7FFFFFF ,0xFFFFFFF ,
			0x1FFFFFFF,0x3FFFFFFF,0x7FFFFFFF,0xFFFFFFFF
		};

	@node
	public static final class IFldExprImpl extends AccessExprImpl {		
		@ref public Field		var;

		public IFldExprImpl() {}
		public IFldExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static final view IFldExprView of IFldExprImpl extends AccessExprView {
		public Field		var;
	}
	
	@ref public abstract virtual Field			var;
	
	public NodeView			getNodeView()			{ return new IFldExprView((IFldExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new IFldExprView((IFldExprImpl)this.$v_impl); }
	public LvalueExprView	getLvalueExprView()		{ return new IFldExprView((IFldExprImpl)this.$v_impl); }
	public AccessExprView	getAccessExprView()		{ return new IFldExprView((IFldExprImpl)this.$v_impl); }
	public IFldExprView		getIFldExprView()		{ return new IFldExprView((IFldExprImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			{ return new JIFldExprView((IFldExprImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			{ return new JIFldExprView((IFldExprImpl)this.$v_impl); }
	public JLvalueExprView	getJLvalueExprView()	{ return new JIFldExprView((IFldExprImpl)this.$v_impl); }
	public JAccessExprView	getJAccessExprView()	{ return new JIFldExprView((IFldExprImpl)this.$v_impl); }
	public JIFldExprView	getJIFldExprView()		{ return new JIFldExprView((IFldExprImpl)this.$v_impl); }

	@getter public Field		get$var()			{ return this.getIFldExprView().var; }
	@setter public void 		set$var(Field val)	{ this.getIFldExprView().var = val; }

	public IFldExpr() {
		super(new IFldExprImpl());
	}

	public IFldExpr(int pos, ENode obj, NameRef ident, Field var) {
		super(new IFldExprImpl(pos));
		this.obj = obj;
		this.ident = ident;
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var) {
		super(new IFldExprImpl(pos));
		this.obj = obj;
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		assert(obj != null && var != null);
	}

	public IFldExpr(int pos, ENode obj, Field var, boolean direct_access) {
		super(new IFldExprImpl(pos));
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

	public Operator getOp() { return BinaryOperator.Access; }

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
		if( pctx.method != null
		 && obj instanceof LVarExpr && ((LVarExpr)obj).ident.equals(nameThis)
		)
			pctx.method.addViolatedField(var);

		setResolved(true);
	}

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.isConstantExpr();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				return true;
			}
		}
		return false;
	}
	public Object	getConstValue() {
		var.acc.verifyReadAccess(this,var);
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.getConstValue();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				ConstantValueAttr cva = (ConstantValueAttr)var.getAttr(attrConstantValue);
				return cva.value;
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

@node
public final class ContainerAccessExpr extends LvalueExpr {
	
	@dflow(out="index") private static class DFI {
	@dflow(in="this:in")	ENode		obj;
	@dflow(in="obj")		ENode		index;
	}

	@node
	public static final class ContainerAccessExprImpl extends LvalueExprImpl {		
		@att public ENode		obj;
		@att public ENode		index;

		public ContainerAccessExprImpl() {}
		public ContainerAccessExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static final view ContainerAccessExprView of ContainerAccessExprImpl extends LvalueExprView {
		public ENode		obj;
		public ENode		index;
	}
	
	@att public abstract virtual ENode			obj;
	
	@att public abstract virtual ENode			index;
	
	
	public NodeView						getNodeView()					{ return new ContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public ENodeView					getENodeView()					{ return new ContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public LvalueExprView				getLvalueExprView()				{ return new ContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public ContainerAccessExprView		getContainerAccessExprView()	{ return new ContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public JNodeView					getJNodeView()					{ return new JContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public JENodeView					getJENodeView()					{ return new JContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public JLvalueExprView				getJLvalueExprView()			{ return new JContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }
	public JContainerAccessExprView		getJContainerAccessExprView()	{ return new JContainerAccessExprView((ContainerAccessExprImpl)this.$v_impl); }

	@getter public ENode		get$obj()			{ return this.getContainerAccessExprView().obj; }
	@getter public ENode		get$index()			{ return this.getContainerAccessExprView().index; }
	
	@setter public void set$obj(ENode val)			{ this.getContainerAccessExprView().obj = val; }
	@setter public void set$index(ENode val)		{ this.getContainerAccessExprView().index = val; }
	
	public ContainerAccessExpr() {
		super(new ContainerAccessExprImpl());
	}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		super(new ContainerAccessExprImpl(pos));
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
				return Type.getRealType(t,t.args[0]);
			}
			else {
				// Resolve overloaded access method
				Method@ v;
				MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
				ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(t,v,info,nameArrayOp,mt) )
					return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+t);
				return Type.getRealType(t,((Method)v).type.ret);
			}
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type t = obj.getType();
		if( t.isArray() ) {
			return new Type[]{Type.getRealType(t,t.args[0])};
		} else {
			Struct s = t.getStruct();
		lookup_op:
			for(;;) {
				s.checkResolved();
				if (s instanceof Struct) {
					Struct ss = (Struct)s;
					foreach(ASTNode n; ss.members; n instanceof Method && ((Method)n).name.equals(nameArrayOp))
						return new Type[]{Type.getRealType(t,((Method)n).type.ret)};
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

	public int getPriority() { return opContainerElementPriority; }

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

	static public final FormPar thisPar = new FormPar(0,Constants.nameThis,Type.tpVoid,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD);
	
	@node
	public static final class ThisExprImpl extends LvalueExprImpl {		
		@att public boolean super_flag;
		public ThisExprImpl() {}
		public ThisExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static final view ThisExprView of ThisExprImpl extends LvalueExprView {
		public boolean	super_flag;
	}
	
	@att public abstract virtual boolean			super_flag;
	
	public NodeView				getNodeView()			{ return new ThisExprView((ThisExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new ThisExprView((ThisExprImpl)this.$v_impl); }
	public LvalueExprView		getLvalueExprView()		{ return new ThisExprView((ThisExprImpl)this.$v_impl); }
	public ThisExprView			getThisExprView()		{ return new ThisExprView((ThisExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JThisExprView((ThisExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JThisExprView((ThisExprImpl)this.$v_impl); }
	public JLvalueExprView		getJLvalueExprView()	{ return new JThisExprView((ThisExprImpl)this.$v_impl); }
	public JThisExprView		getJThisExprView()		{ return new JThisExprView((ThisExprImpl)this.$v_impl); }

	@getter public boolean		get$super_flag()				{ return this.getThisExprView().super_flag; }
	@setter public void 		set$super_flag(boolean val)		{ this.getThisExprView().super_flag = val; }
	
	public ThisExpr() {
		super(new ThisExprImpl());
	}
	public ThisExpr(int pos) {
		super(new ThisExprImpl(pos));
	}
	public ThisExpr(boolean super_flag) {
		super(new ThisExprImpl());
		this.super_flag = super_flag;
	}

	public String toString() { return super_flag ? "super" : "this"; }

	public Type getType() {
		try {
			if (pctx.clazz == null)
				return Type.tpVoid;
			if (pctx.clazz.name.short_name.equals(nameIdefault))
				return pctx.clazz.package_clazz.type;
			if (super_flag)
				pctx.clazz.type.getSuperType();
			return pctx.clazz.type;
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return Type.tpVoid;
		}
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		if (pctx.method != null &&
			pctx.method.isStatic() &&
			!pctx.clazz.name.short_name.equals(nameIdefault)
		)
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(toString()).space();
	}
}

@node
public final class LVarExpr extends LvalueExpr {
	
	@dflow(out="this:in") private static class DFI {}

	static final KString namePEnv = KString.from("$env");

	@node
	public static final class LVarExprImpl extends LvalueExprImpl {		
		@att public NameRef		ident;
		@ref public Var			var;

		public LVarExprImpl() {}
		public LVarExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static final view LVarExprView of LVarExprImpl extends LvalueExprView {
		public NameRef	ident;
		public Var		var;
	}
	
	@att public abstract virtual NameRef		ident;
	@ref public abstract virtual Var			var;
	
	public NodeView			getNodeView()			{ return new LVarExprView((LVarExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new LVarExprView((LVarExprImpl)this.$v_impl); }
	public LvalueExprView	getLvalueExprView()		{ return new LVarExprView((LVarExprImpl)this.$v_impl); }
	public LVarExprView		getLVarExprView()		{ return new LVarExprView((LVarExprImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			{ return new JLVarExprView((LVarExprImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			{ return new JLVarExprView((LVarExprImpl)this.$v_impl); }
	public JLvalueExprView	getJLvalueExprView()	{ return new JLVarExprView((LVarExprImpl)this.$v_impl); }
	public JLVarExprView	getJLVarExprView()		{ return new JLVarExprView((LVarExprImpl)this.$v_impl); }

	@getter public NameRef		get$ident()			{ return this.getLVarExprView().ident; }
	@getter public Var			get$var()			{ return this.getLVarExprView().var; }
	
	@setter public void set$ident(NameRef val)		{ this.getLVarExprView().ident = val; }
	@setter public void set$var(Var val)			{ this.getLVarExprView().var = val; }

	public LVarExpr() {
		super(new LVarExprImpl());
	}
	public LVarExpr(int pos, Var var) {
		super(new LVarExprImpl(pos));
		this.var = var;
		this.ident = new NameRef(pos, var.name.name);
	}
	public LVarExpr(int pos, KString name) {
		super(new LVarExprImpl(pos));
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

	public Var getVar() {
		if (var != null)
			return var;
		Var@ v;
		ResInfo info = new ResInfo(this);
		if( !PassInfo.resolveNameR(this,v,info,ident.name) )
			throw new CompilerException(this,"Unresolved var "+ident);
		var = v;
		return var;
	}

	public boolean preResolveIn(TransfProcessor proc) {
		getVar(); // calls resolving
		return false;
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		getVar(); // calls resolving
		return false;
	}
	
	public boolean preGenerate() {
		if (getVar().isLocalRuleVar()) {
			RuleMethod rm = (RuleMethod)pctx.method;
			assert(rm.params[0].type == Type.tpRule);
			Var pEnv = null;
			foreach (ENode n; rm.body.stats; n instanceof VarDecl) {
				VarDecl vd = (VarDecl)n;
				if (vd.var.name.equals(namePEnv)) {
					assert(vd.var.type.isInstanceOf(Type.tpRule));
					pEnv = vd.var;
					break;
				}
			}
			if (pEnv == null) {
				Kiev.reportError(this, "Cannot find "+namePEnv);
				return false;
			}
			Struct s = ((LocalStructDecl)((BlockStat)rm.body).stats[0]).clazz;
			Field f = s.resolveField(ident.name);
			replaceWithNode(new IFldExpr(pos, new LVarExpr(pos, pEnv), (NameRef)~ident, f));
		}
		return true;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		// Check if we try to access this var from local inner/anonymouse class
		if( pctx.clazz.isLocal() ) {
			if( getVar().pctx.clazz != this.pctx.clazz ) {
				var.setNeedProxy(true);
				setAsField(true);
				// Now we need to add this var as a fields to
				// local class and to initializer of this class
				Field vf;
				if( (vf = pctx.clazz.resolveField(ident.name,false)) == null ) {
					// Add field
					vf = pctx.clazz.addField(new Field(ident.name,var.type,ACC_PUBLIC));
					vf.setNeedProxy(true);
					vf.init = (ENode)this.copy();
				}
			}
		}
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		return dmp.space();
	}
}

@node
public final class SFldExpr extends AccessExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class SFldExprImpl extends AccessExprImpl {		
		@ref public Field		var;

		public SFldExprImpl() {}
		public SFldExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static final view SFldExprView of SFldExprImpl extends AccessExprView {
		public Field		var;
	}
	
	@ref public abstract virtual Field			var;
	
	public NodeView			getNodeView()			{ return new SFldExprView((SFldExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new SFldExprView((SFldExprImpl)this.$v_impl); }
	public LvalueExprView	getLvalueExprView()		{ return new SFldExprView((SFldExprImpl)this.$v_impl); }
	public AccessExprView	getAccessExprView()		{ return new SFldExprView((SFldExprImpl)this.$v_impl); }
	public SFldExprView		getSFldExprView()		{ return new SFldExprView((SFldExprImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			{ return new JSFldExprView((SFldExprImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			{ return new JSFldExprView((SFldExprImpl)this.$v_impl); }
	public JLvalueExprView	getJLvalueExprView()	{ return new JSFldExprView((SFldExprImpl)this.$v_impl); }
	public JAccessExprView	getJAccessExprView()	{ return new JSFldExprView((SFldExprImpl)this.$v_impl); }
	public JSFldExprView	getJSFldExprView()		{ return new JSFldExprView((SFldExprImpl)this.$v_impl); }

	@getter public Field		get$var()			{ return this.getSFldExprView().var; }
	@setter public void 		set$var(Field val)	{ this.getSFldExprView().var = val; }


	public SFldExpr() {
		super(new SFldExprImpl());
	}

	public SFldExpr(int pos, Field var) {
		super(new SFldExprImpl(pos));
		this.obj = new TypeRef(pos,((Struct)var.parent).type);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
	}

	public SFldExpr(int pos, Field var, boolean direct_access) {
		super(new SFldExprImpl(pos));
		this.obj = new TypeRef(pos,((Struct)var.parent).type);
		this.ident = new NameRef(pos,var.name.name);
		this.var = var;
		if (direct_access) setAsField(true);
	}

	public String toString() { return var.toString(); }

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.isConstantExpr();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				return true;
			}
		}
		return false;
	}
	public Object	getConstValue() {
		var.acc.verifyReadAccess(this,var);
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.getConstValue();
			else if( var.isStatic() && var.getAttr(attrConstantValue)!=null ) {
				ConstantValueAttr cva = (ConstantValueAttr)var.getAttr(attrConstantValue);
				return cva.value;
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
		if( pctx.method != null )
			pctx.method.addViolatedField(var);
		setResolved(true);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) {
		Struct cl = (Struct)var.parent;
		ClazzName cln = cl.type.getClazzName();
		return dmp.space().append(cln).append('.').append(var.name).space();
	}

}

@node
public final class OuterThisAccessExpr extends AccessExpr {
	
	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class OuterThisAccessExprImpl extends AccessExprImpl {		
		@ref public Struct			outer;
		@ref public NArr<Field>		outer_refs;

		public OuterThisAccessExprImpl() {}
		public OuterThisAccessExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static final view OuterThisAccessExprView of OuterThisAccessExprImpl extends AccessExprView {
		public				Struct			outer;
		public access:ro	NArr<Field>		outer_refs;
	}
	
	@ref public abstract virtual			Struct			outer;
	@ref public abstract virtual access:ro	NArr<Field>		outer_refs;
	
	public NodeView						getNodeView()					{ return new OuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public ENodeView					getENodeView()					{ return new OuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public LvalueExprView				getLvalueExprView()				{ return new OuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public AccessExprView				getAccessExprView()				{ return new OuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public OuterThisAccessExprView		getOuterThisAccessExprView()	{ return new OuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public JNodeView					getJNodeView()					{ return new JOuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public JENodeView					getJENodeView()					{ return new JOuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public JLvalueExprView				getJLvalueExprView()			{ return new JOuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public JAccessExprView				getJAccessExprView()			{ return new JOuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }
	public JOuterThisAccessExprView		getJOuterThisAccessExprView()	{ return new JOuterThisAccessExprView((OuterThisAccessExprImpl)this.$v_impl); }

	@getter public Struct		get$outer()				{ return this.getOuterThisAccessExprView().outer; }
	@getter public NArr<Field>	get$outer_refs()		{ return this.getOuterThisAccessExprView().outer_refs; }
	@setter public void 		set$outer(Struct val)	{ this.getOuterThisAccessExprView().outer = val; }


	public OuterThisAccessExpr() {
		super(new OuterThisAccessExprImpl());
	}

	public OuterThisAccessExpr(int pos, Struct outer) {
		super(new OuterThisAccessExprImpl(pos));
		this.obj = new TypeRef(pos,outer.type);
		this.ident = new NameRef(pos,nameThis);
		this.outer = outer;
	}

	public String toString() { return outer.name.toString()+".this"; }

	public Type getType() {
		return outer.type;
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
		Field ou_ref = outerOf(pctx.clazz);
		if( ou_ref == null )
			throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+pctx.clazz);
		do {
			trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
			outer_refs.append(ou_ref);
			if( ou_ref.type.isInstanceOf(outer.type) ) break;
			ou_ref = outerOf(ou_ref.type.getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].type.isInstanceOf(outer.type) )
			throw new RuntimeException("Outer class "+outer+" not found for inner class "+pctx.clazz);
		if( Kiev.debugResolve ) {
			StringBuffer sb = new StringBuffer("Outer 'this' resolved as this");
			for(int i=0; i < outer_refs.length; i++)
				sb.append("->").append(outer_refs[i].name);
			System.out.println(sb.toString());
		}
		if( pctx.method.isStatic() && !pctx.method.isVirtualStatic() ) {
			throw new RuntimeException("Access to 'this' in static method "+pctx.method);
		}
		setResolved(true);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}


