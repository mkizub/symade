package kiev.vlang;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JLvalDNodeView;
import kiev.be.java.JVarView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class Var extends LvalDNode implements Named, Typed {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	@node
	public static class VarImpl extends LvalDNodeImpl {
		public VarImpl() {}
		public VarImpl(int pos) { super(pos); }
		public VarImpl(int pos, int fl) { super(pos, fl); }

		public final Var getVar() { return (Var)this._self; }
		
		     public NodeName	name;
		@att public TypeRef		vtype;
		@att public ENode		init;
		     public int			bcpos = -1;

		public void callbackChildChanged(AttrSlot attr) {
			if (parent != null && pslot != null) {
				if      (attr.name == "vtype")
					parent.callbackChildChanged(pslot);
				else if (attr.name == "meta")
					parent.callbackChildChanged(pslot);
			}
		}	
	}
	@nodeview
	public static view VarView of VarImpl extends LvalDNodeView {
		public	NodeName	name;
		public	TypeRef		vtype;
		public	ENode		init;
		public	int			bcpos;

		@getter public final Type get$type() {
			if (this.$view.vtype == null)
				return Type.tpVoid;
			return this.$view.vtype.getType();
		}
		
		// is a local var in a rule 
		public final boolean isLocalRuleVar() {
			return this.$view.is_var_local_rule_var;
		}
		public final void setLocalRuleVar(boolean on) {
			if (this.$view.is_var_local_rule_var != on) {
				this.$view.is_var_local_rule_var = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// closure proxy
		public final boolean isClosureProxy() {
			return this.$view.is_var_closure_proxy;
		}
		public final void setClosureProxy(boolean on) {
			if (this.$view.is_var_closure_proxy != on) {
				this.$view.is_var_closure_proxy = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// "this" var
		public final boolean isVarThis() {
			return this.$view.is_var_this;
		}
		public final void setVarThis(boolean on) {
			if (this.$view.is_var_this != on) {
				this.$view.is_var_this = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// "super" var
		public final boolean isVarSuper() {
			return this.$view.is_var_super;
		}
		public final void setVarSuper(boolean on) {
			if (this.$view.is_var_super != on) {
				this.$view.is_var_super = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		alias operator(210,fy,$cast) { return new VarView((VarImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		alias operator(210,fy,$cast) { return new VarView((VarImpl)this.$v_impl); }
	public LvalDNodeView	getLvalDNodeView()	alias operator(210,fy,$cast) { return new VarView((VarImpl)this.$v_impl); }
	public VarView			getVarView()		alias operator(210,fy,$cast) { return new VarView((VarImpl)this.$v_impl); }
	
	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JVarView((VarImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JVarView((VarImpl)this.$v_impl); }
	public JLvalDNodeView	getJLvalDNodeView()	alias operator(210,fy,$cast) { return new JVarView((VarImpl)this.$v_impl); }
	public JVarView			getJVarView()		alias operator(210,fy,$cast) { return new JVarView((VarImpl)this.$v_impl); }

	@getter public NodeName				get$name()			{ return this.getVarView().name; }
	@getter public TypeRef				get$vtype()			{ return this.getVarView().vtype; }
	@getter public ENode				get$init()			{ return this.getVarView().init; }
	@getter        int					get$bcpos()			{ return this.getVarView().bcpos; }

	@getter public Type					get$type()			{ return this.getVarView().type; }
	
	@setter public void set$name(NodeName val)				{ this.getVarView().name = val; }
	@setter public void set$vtype(TypeRef val)				{ this.getVarView().vtype = val; }
	@setter public void set$init(ENode val)				{ this.getVarView().init = val; }
	@setter        void set$bcpos(int val)					{ this.getVarView().bcpos = val; }
	
	public static Var[]	emptyArray = new Var[0];

	     public abstract virtual			NodeName		name;
	@att public abstract virtual			TypeRef			vtype;
	@att public abstract virtual			ENode			init;
	            abstract virtual			int				bcpos;
	@ref public abstract virtual access:ro	Type			type;
	
	public Var() { super(new VarImpl()); }
	public Var(VarImpl impl) { super(impl); }

	public Var(int pos, KString name, Type type, int flags) {
		this(new VarImpl(pos,flags));
		assert(type != null);
		this.name = new NodeName(name);
		this.vtype = new TypeRef(type);
	}

	public Var(NameRef id, TypeRef vtype, int flags) {
		this(new VarImpl(id.pos,flags));
		this.name = new NodeName(id.name);
		this.vtype = vtype;
	}

	public Var(VarImpl impl, KString name, Type type) {
		this(impl);
		assert(type != null);
		this.name = new NodeName(name);
		this.vtype = new TypeRef(type);
	}

	public Var(VarImpl impl, NameRef id, TypeRef vtype) {
		this(impl);
		this.name = new NodeName(id.name);
		this.vtype = vtype;
	}

//	@getter public Type get$type() {
//		if (vtype == null)
//			return Type.tpVoid;
//		return vtype.getType();
//	}
	
	// is a local var in a rule 
	public boolean isLocalRuleVar() { return this.getVarView().isLocalRuleVar(); }
	public void setLocalRuleVar(boolean on) { this.getVarView().setLocalRuleVar(on); }
	// closure proxy
	public boolean isClosureProxy() { return this.getVarView().isClosureProxy(); }
	public void setClosureProxy(boolean on) { this.getVarView().setClosureProxy(on); }
	// "this" var
	public boolean isVarThis() { return this.getVarView().isVarThis(); }
	public void setVarThis(boolean on) { this.getVarView().setVarThis(on); }
	// "super" var
	public boolean isVarSuper() { return this.getVarView().isVarSuper(); }
	public void setVarSuper(boolean on) { this.getVarView().setVarSuper(on); }

	public String toString() {
		return name.toString()/*+":="+type*/;
	}

	public int hashCode() {
		return name.hashCode();
	}

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	static class VarDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		VarDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("init"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			Var node = (Var)dfi.node;
			DFState out = DFFunc.calc(f, dfi);
			out = out.declNode(node);
			if( node.init != null && node.init.getType() ≢ Type.tpVoid )
				out = out.setNodeValue(new LvalDNode[]{node},node.init);
			res = out;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new VarDFFunc(dfi);
	}

	public void resolveDecl() {
		if( isResolved() ) return;
		if( init == null && !type.isArray() && type.isWrapper() && !this.isInitWrapper())
			init = new NewExpr(pos,type,ENode.emptyArray);
		if( init != null ) {
			if (init instanceof TypeRef)
				((TypeRef)init).toExpr(this.getType());
			if (type.isWrapper() && !this.isInitWrapper())
				init = new NewExpr(init.pos,type,new ENode[]{(ENode)~init});
			try {
				init.resolve(this.type);
				Type it = init.getType();
				if( !it.isInstanceOf(this.type) ) {
					init = new CastExpr(init.pos,this.type,(ENode)~init);
					init.resolve(this.type);
				}
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
		}
		getDFlow().out();
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name);
	}

	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		dmp.append(type).forsed_space().append(name);
		if (init != null)
			dmp.space().append('=').append(init);
		dmp.append(';').newLine();
		return dmp;
	}

	public Dumper toJavaDecl(Dumper dmp, Type etype) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		return dmp.append(etype).forsed_space().append(name);
	}

}

@node
public final class FormPar extends Var {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}
	
	@node
	public static final class FormParImpl extends VarImpl {
		public FormParImpl() {}
		public FormParImpl(int pos) { super(pos); }
		public FormParImpl(int pos, int fl) { super(pos, fl); }

		@att TypeRef		stype;
		     int			kind;

		public void callbackChildChanged(AttrSlot attr) {
			if (parent != null && pslot != null) {
				if (attr.name == "stype")
					parent.callbackChildChanged(pslot);
				else
					super.callbackChildChanged(attr);
			}
		}
	}
	@nodeview
	public static final view FormParView of FormParImpl extends VarView {
		public TypeRef		stype;
		public int			kind;

		@getter public final Type get$dtype() {
			if (this.$view.stype == null)
				return get$type();
			return this.$view.stype.getType();
		}
		
	}
	public NodeView			getNodeView()		alias operator(210,fy,$cast) { return new FormParView((FormParImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		alias operator(210,fy,$cast) { return new FormParView((FormParImpl)this.$v_impl); }
	public LvalDNodeView	getLvalDNodeView()	alias operator(210,fy,$cast) { return new FormParView((FormParImpl)this.$v_impl); }
	public VarView			getVarView()		alias operator(210,fy,$cast) { return new FormParView((FormParImpl)this.$v_impl); }
	public FormParView		getFormParView()	alias operator(210,fy,$cast) { return new FormParView((FormParImpl)this.$v_impl); }

	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JVarView((FormParImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JVarView((FormParImpl)this.$v_impl); }
	public JLvalDNodeView	getJLvalDNodeView()	alias operator(210,fy,$cast) { return new JVarView((FormParImpl)this.$v_impl); }
	public JVarView			getJVarView()		alias operator(210,fy,$cast) { return new JVarView((FormParImpl)this.$v_impl); }
	
	@getter public TypeRef				get$stype()			{ return this.getFormParView().stype; }
	@getter        int					get$kind()			{ return this.getFormParView().kind; }

	@getter public Type					get$dtype()			{ return this.getFormParView().dtype; }
	
	@setter public void set$stype(TypeRef val)				{ this.getFormParView().stype = val; }
	@setter        void set$kind(int val)					{ this.getFormParView().kind = val; }
	
	public static final int PARAM_NORMAL       = 0;
	public static final int PARAM_THIS         = 1;
	public static final int PARAM_OUTER_THIS   = 2;
	public static final int PARAM_RULE_ENV     = 3;
	public static final int PARAM_TYPEINFO     = 4;
	public static final int PARAM_VARARGS      = 6;
	public static final int PARAM_LVAR_PROXY   = 7;
	
	@att public abstract virtual			TypeRef		stype;
	     public abstract virtual			int			kind;
	@ref public abstract virtual access:ro	Type		dtype;

	public FormPar() {
		super(new FormParImpl());
	}

	public FormPar(int pos, KString name, Type type, int kind, int flags) {
		super(new FormParImpl(pos,flags),name,type);
		this.kind = kind;
		this.stype = new TypeRef(type);
	}

	public FormPar(NameRef id, TypeRef vtype, TypeRef stype, int kind, int flags) {
		super(new FormParImpl(id.pos,flags),id,vtype);
		this.kind = kind;
		this.stype = stype;
	}
	
}

public class DFState {
	
	public static final DFState[] emptyArray = new DFState[0];

	private final List<ScopeNodeInfo> states;
	private final int abrupted;

	private DFState(List<ScopeNodeInfo> states, int abrupted) {
		this.states = states;
		this.abrupted = abrupted;
	}
	
	public static DFState makeNewState() {
		List<ScopeNodeInfo> states = List.Nil;
		return new DFState(states,0);
	}
	
	public ScopeNodeInfo getNodeInfo(LvalDNode[] path) {
		foreach(ScopeNodeInfo sni; states; sni.match(path)) {
			trace( Kiev.debugNodeTypes, "types: getinfo for node "+Arrays.toString(path)+" is "+sni);
			return sni;
		}
		return null;
	}

	private static ScopeNodeInfo makeNode(LvalDNode[] path) {
		ScopeNodeInfo sni;
		if (path.length == 1) {
			if (path[0] instanceof Var)
				sni = new ScopeVarInfo((Var)path[0]);
			else if (path[0] instanceof Field && path[0].isStatic())
				sni = new ScopeStaticFieldInfo((Field)path[0]);
			else
				return null;
		}
		else if (ScopeForwardFieldInfo.checkForwards(path)) {
			sni = new ScopeForwardFieldInfo(path);
		}
		else
			return null;
		trace( Kiev.debugNodeTypes, "types: add "+sni);
		return sni;
	}

	private static ScopeNodeInfo makeNode(Var var) {
		ScopeNodeInfo sni = new ScopeVarInfo(var);
		trace( Kiev.debugNodeTypes, "types: add "+sni);
		return sni;
	}

	public DFState declNode(Var var) {
		ScopeNodeInfo sni = makeNode(var);
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states),this.abrupted);
		trace( Kiev.debugNodeTypes, "types: (decl) var "+sni);
		return dfs;
	}

	public DFState addNodeType(LvalDNode[] path, Type type) {
		ScopeNodeInfo sni = getNodeInfo(path);
		if (sni == null) sni = makeNode(path);
		if (sni == null) return this;
		Type[] snits = sni.getTypes();
		Type[] types = addAccessType(snits, type);
		if (types.length == snits.length) {
			for (int i=0; i < types.length; i++) {
				if (types[i] ≉ snits[i])
					goto changed;
			}
			return this;
		}
changed:;
		sni = sni.makeWithTypes(types);
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states),this.abrupted);
		trace( Kiev.debugNodeTypes, "types: (set types to) "+sni);
		return dfs;
	}

	public DFState setNodeValue(LvalDNode[] path, ENode expr) {
		Type tp = expr.getType();
		if( tp ≡ Type.tpNull && tp ≡ Type.tpVoid )
			return this;
		ScopeNodeInfo sni = makeNode(path);
		if (sni == null) return this;
		sni = sni.makeWithTypes(addAccessType(sni.getTypes(), tp));
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states),this.abrupted);
		trace( Kiev.debugNodeTypes, "types: (set value to) "+sni);
		return dfs;
	}

	/** Add access type to access type array
	 *  Due to the Java allows only one class and many interfaces,
	 *  the access type array must has first element to be
	 *  the class type (if exists), and others to be interface types
	 */
	static Type[] addAccessType(Type[] types, Type type) {
		if( type ≡ null || type ≡ Type.tpVoid || type ≡ Type.tpNull ) return types;
		if( types == null || !type.isReference() ) {
			return new Type[]{type};
		}
		trace( Kiev.debugNodeTypes, "types: add type "+type+" to "+Arrays.toString(types));
		Type[] newtypes = new Type[]{type};
	next_type:
		foreach(Type t1; types; t1 ≢ null && t1 ≢ Type.tpVoid && t1 ≢ Type.tpNull ) {
			for( int i=0; i < newtypes.length; i++) {
				Type t2 = newtypes[i];
				if (t2.isInstanceOf(t1))
					continue next_type;
			}
			for( int i=0; i < newtypes.length; i++) {
				Type t2 = newtypes[i];
				if (t1.isInstanceOf(t2)) {
					newtypes[i] = t1;
					continue next_type;
				}
			}
			if( t1.isInterface() ) {
				newtypes = (Type[])Arrays.append(newtypes,t1);
			} else {
				for (int i=0; i < newtypes.length; i++) {
					if (!newtypes[i].isInterface())
						continue;
					newtypes = (Type[])Arrays.insert(newtypes,t1,i);
					break;
				}
			}
		}
		trace( Kiev.debugNodeTypes, "types: add type yeilds "+Arrays.toString(newtypes));
		return newtypes;
	}

	public DFState cleanInfoForVars(Var[] vars) {
		if (vars == null || vars.length == 0)
			return this;
		List<ScopeNodeInfo> states = this.states.filter(fun (ScopeNodeInfo sni)->boolean {
			foreach (Var v; vars) {
				if (sni instanceof ScopeVarInfo && ((ScopeVarInfo)sni).var == v)
					return false;
				if (sni instanceof ScopeForwardFieldInfo && ((ScopeForwardFieldInfo)sni).path[0] == v)
					return false;
			}
			return true;
		});
		trace( Kiev.debugNodeTypes, "types: vars "+Arrays.toString(vars)+" cleared to "+states);
		if (states == this.states)
			return this;
		DFState dfs = new DFState(states,this.abrupted);
		return dfs;
	}

	public DFState setAbrupted() {
		return new DFState(states,this.abrupted+1);
	}
	
	/** Joins two vectors by AND rule. I.e. initialized = 1.initialized && 2.initialized
	 *  this used for then/else statements of 'if' and || boolean operator
	 */
	public static DFState join(DFState state1, DFState state2) {
		if (state1.abrupted > state2.abrupted)
			return state2;
		if (state2.abrupted > state1.abrupted)
			return state1;
		List<ScopeNodeInfo> diff = List.Nil;
		List<ScopeNodeInfo> base_states;
		{
			int len1 = state1.states.length();
			int len2 = state2.states.length();
			int min = Math.min(len1,len2);
			List<ScopeNodeInfo> s1 = state1.states;
			List<ScopeNodeInfo> s2 = state2.states;
			for (int i=len1-min; i > 0; i--) s1 = s1.tail();
			for (int i=len2-min; i > 0; i--) s2 = s2.tail();
			while (s1 != s2) {
				s1 = s1.tail();
				s2 = s2.tail();
			}
			base_states = s1;
			min = base_states.length();
			for (s1=state1.states; s1 != base_states; s1=s1.tail()) {
				ScopeNodeInfo sni = s1.head();
				if !(diff.contains(sni))
					diff = new List.Cons<ScopeNodeInfo>(sni,diff);
			}
			for (s2=state2.states; s2 != base_states; s2=s2.tail()) {
				ScopeNodeInfo sni = s2.head();
				if !(diff.contains(sni))
					diff = new List.Cons<ScopeNodeInfo>(sni,diff);
			}
		}
		List<ScopeNodeInfo> states = base_states;
		foreach(ScopeNodeInfo sni; diff) {
			ScopeNodeInfo sni1 = state1.getNodeInfo(sni.getPath());
			ScopeNodeInfo sni2 = state2.getNodeInfo(sni.getPath());
			if (sni1 == null || sni2 == null)
				continue;
			sni = sni1.makeJoin(sni2);
			states = new List.Cons<ScopeNodeInfo>(sni, states);
			trace( Kiev.debugNodeTypes, "types: joining "+sni1+" and "+ sni2+" => "+sni);
		}
		trace( Kiev.debugNodeTypes, "types: joined to "+states);
		assert (state1.abrupted == state2.abrupted);
		DFState dfs = new DFState(states,state1.abrupted);
		return dfs;
	}

}

public abstract class ScopeNodeInfo implements Cloneable {
	private Type[]	types;
	private ScopeNodeInfo j1, j2;
	public abstract Type getDeclType();
	public abstract boolean match(LvalDNode[] path);
	public abstract LvalDNode[] getPath();
	
	protected final void setupDeclType() {
		types = new Type[]{getDeclType()};
	}
	
	public final ScopeNodeInfo makeWithTypes(Type[] types) {
		ScopeNodeInfo newsni = (ScopeNodeInfo)super.clone();
		newsni.types = types;
		newsni.j1 = null;
		newsni.j2 = null;
		return newsni;
	}
	public final ScopeNodeInfo makeJoin(ScopeNodeInfo sni) {
		ScopeNodeInfo newsni = (ScopeNodeInfo)super.clone();
		newsni.types = null;
		newsni.j1 = this;
		newsni.j2 = sni;
		return newsni;
	}
	public final Type[] getTypes() {
		if (types != null)
			return types;
		Type[] types = new Type[]{this.getDeclType()};
		foreach(Type t1; j1.getTypes(); t1 ≢ null && t1 ≢ Type.tpVoid && t1 ≢ Type.tpNull) {
			foreach(Type t2; j2.getTypes(); t2 ≢ null && t2 ≢ Type.tpVoid && t2 ≢ Type.tpNull )
				types = DFState.addAccessType(types,Type.leastCommonType(t1,t2));
		}
		this.types = types;
		return types;
	}
}

public class ScopeVarInfo extends ScopeNodeInfo {

	public Var		var;

	public ScopeVarInfo(Var var) {
		this.var = var;
		setupDeclType();
	}

	public Type getDeclType() {
		return var.type;
	}
	
	public String toString() {
		return "sni:{var "+var+","+Arrays.toString(getTypes())+"}";
	}

	public LvalDNode[] getPath() {
		return new LvalDNode[]{var};
	}
	
	public boolean equals(Object obj) {
		if !(obj instanceof ScopeVarInfo)
			return false;
		return var == ((ScopeVarInfo)obj).var;
	}
	
	public boolean match(LvalDNode[] path) {
		return path.length==1 && path[0] == var;
	}
}

public class ScopeStaticFieldInfo extends ScopeNodeInfo {

	public Field	fld;

	public ScopeStaticFieldInfo(Field fld) {
		assert(fld.isStatic());
		this.fld = fld;
		setupDeclType();
	}

	public Type getDeclType() {
		return fld.type;
	}
	
	public String toString() {
		return "sni:{static fld "+fld+","+Arrays.toString(getTypes())+"}";
	}

	public LvalDNode[] getPath() {
		return new LvalDNode[]{fld};
	}
	
	public boolean equals(Object obj) {
		if !(obj instanceof ScopeStaticFieldInfo)
			return false;
		return fld == ((ScopeStaticFieldInfo)obj).fld;
	}
	
	public boolean match(LvalDNode[] path) {
		return path.length==1 && path[0] == fld;
	}
}

public class ScopeForwardFieldInfo extends ScopeNodeInfo {

	public LvalDNode[] path;

	public ScopeForwardFieldInfo(LvalDNode[] path) {
		assert(path.length > 1);
		assert(checkForwards(path));
		this.path = path;
		setupDeclType();
	}
	
	public static boolean checkForwards(LvalDNode[] path) {
		if !(path[0] instanceof Var)
			return false;
		if !(path[0].isForward())
			return false;
		for(int i=1; i < path.length-1; i++) {
			if !(path[i] instanceof Field)
				return false;
			if !(path[i].isForward())
				return false;
			if (path[i].isStatic())
				return false;
		}
		if (path[path.length-1].isStatic())
			return false;
		return true;
	}
	
	public Type getDeclType() {
		Type tp = ((Var)path[0]).type;
		for(int i=1; i < path.length; i++)
			tp = Type.getRealType(tp, ((Field)path[i]).type);
		return tp;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < path.length; i++) {
			if (i < path.length-1)
				sb.append('.');
			sb.append(path[i]);
		}
		return "sni:{forward fld "+sb.toString()+","+Arrays.toString(getTypes())+"}";
	}

	public LvalDNode[] getPath() {
		return path;
	}
	
	public boolean equals(Object obj) {
		if !(obj instanceof ScopeForwardFieldInfo)
			return false;
		ScopeForwardFieldInfo ffi = (ScopeForwardFieldInfo)obj;
		return match(ffi.path);
	}

	public boolean match(LvalDNode[] path) {
		if (this.path.length != path.length)
			return false;
		for(int i=0; i < path.length; i++) {
			if (this.path[i] != path[i])
				return false;
		}
		return true;
	}
}

public interface DataFlowSlots {
	public final int IN  = 0;
	public final int OUT = 1;
	public final int TRU = 2;
	public final int FLS = 3;
	public final int JMP = 4;
	
	public final boolean ASSERT_MORE = false;
}

public final class DataFlowInfo extends NodeData implements DataFlowSlots {
	public static final KString ID = KString.from("data flow info");
	
	private static final Hashtable<Class, DataFlowInfo> data_flows = new Hashtable<Class, DataFlowInfo>(128);

	// will be a set of fields (DataFlow nodes for children) in code-generation 
	final DFSocket[] children;
	
	// attached (to DFSocketChild) DataFlowInfo nodes
	final DataFlowInfo[] attached;
	
	// hashed set of DFState for functions
	private final DFState[] results;
	
	// anti-loop locks for Label(bit 0) and DFFuncLabel
	int locks;
	
	// is a dflow root
	final boolean is_root;
	
	// a socket of the parent node this data flow is plugged in
	access:no,no,ro,rw DFSocket			parent_dfs;
	// DataFlowInfo of the parent node this data flow is plugged in
	access:no,no,ro,rw DataFlowInfo		parent_dfi;

	final DFFunc func_in;
	final DFFunc func_out;
	final DFFunc func_tru;
	final DFFunc func_fls;
	final DFFunc func_jmp;

	public static DataFlowInfo newDataFlowInfo(ASTNode node) {
		DataFlowInfo template = data_flows.get(node.getClass());
		if (template == null) {
			template = new DataFlowInfo((ASTNode)node.getClass().newInstance());
			data_flows.put(node.getClass(), template);
		}
		return new DataFlowInfo(node, template);
	}
	private DataFlowInfo(ASTNode node, DataFlowInfo template) {
		super(ID, node);
		this.children = template.children;
		if (template.results != null)
			this.results = new DFState[template.results.length];
		if (template.attached != null)
			this.attached = new DataFlowInfo[template.attached.length];
		this.is_root = template.is_root;
		this.func_in = template.func_in;
		this.func_out = template.func_out;
		this.func_tru = template.func_tru;
		this.func_fls = template.func_fls;
		this.func_jmp = template.func_jmp;
	}
	private DataFlowInfo(ASTNode node) {
		super(ID, node);
		Vector<DFSocket> chl_dfs = new Vector<DFSocket>();
		Hashtable<String,kiev.vlang.dflow> dflows = new Hashtable<String,kiev.vlang.dflow>();
		int attach_idx = 0;
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			kiev.vlang.dflow dfd = getFieldAnnotation(attr.name);
			if (dfd != null) {
				String seq = dfd.seq().intern();
				if (ASSERT_MORE) assert (seq=="true" || seq=="false" || seq=="");
				DFSocket df;
				if (seq == "true")
					df = new DFSocketSpaceSeq(attr.name);
				else if (seq == "false")
					df = new DFSocketSpaceUnknown(attr.name);
				else
					df = new DFSocketChild(attr.name, attach_idx++);
				if (ASSERT_MORE) assert (df.pslot_name == attr.name);
				chl_dfs.append(df);
				dflows.put(attr.name,dfd);
			}
		}
		if (attach_idx > 0)
			this.attached = new DataFlowInfo[attach_idx];
		children = chl_dfs.toArray();
		int lock_idx = 0;
		foreach (String name; dflows.keys()) {
			kiev.vlang.dflow dfd = dflows.get(name);
			DFSocket df = getSocket(name);
			String fin = dfd.in().intern();
			String[] flnk = dfd.links();
			if (flnk == null || flnk.length == 0) {
				df.func_in = make(fin);
			} else {
				lock_idx ++;
				df.func_in = new DFFunc.DFFuncLabel(make(fin),make(flnk), 1 << lock_idx, allocResult());
			}
		}
		{
			kiev.vlang.dflow dfd = getClassAnnotation();
			String fin = dfd.in().intern();
			String fout = dfd.out().intern();
			String fjmp = dfd.jmp().intern();
			String ftru = dfd.tru().intern();
			String ffls = dfd.fls().intern();
			if (fin != "") {
				assert (fin == "this:in()" || fin == "root()");
				this.func_in = node.newDFFuncIn(this);
				if (fin == "root()")
					is_root = true;
			}
			if (ftru != "" || ffls != "") {
				assert (fout == "" && fjmp == "");
				this.func_tru = make(ftru);
				this.func_fls = make(ffls);
				this.func_out = new DFFunc.DFFuncJoin(this.func_tru,this.func_fls,allocResult());
			}
			else if (fjmp != "") {
				assert (fout == "");
				this.func_jmp = make(fjmp);
				this.func_out = new DFFunc.DFFuncAbrupt(this.func_jmp,allocResult());
				this.func_tru = this.func_out;
				this.func_fls = this.func_out;
			}
			else {
				assert (fjmp == "" && ftru == "" && ffls == "");
				this.func_out = make(fout);
				this.func_tru = this.func_out;
				this.func_fls = this.func_out;
			}
		}
		if (this.locks > 0)
			results = new DFState[this.locks];
		//this.node = null;
	}
	
	public final int allocResult() { return locks++; }
	public final DFState getResult(int idx) { return results[idx]; }
	public final void setResult(int idx, DFState res) { results[idx] = res; }

	public final DFState in()  { return calc(IN); }
	public final DFState out() { return calc(OUT); }
	public final DFState tru() { return calc(TRU); }
	public final DFState fls() { return calc(FLS); }
	public final DFState jmp() { return calc(JMP); }

	public final DFState calc(int slot) {
		switch (slot) {
		case IN:
			if (func_in != null)
				return func_in.calc(this);
			if (parent_dfs.isSeqSpace() && node.pprev != null)
				return node.pprev.getDFlow().calc(OUT);
			assert(parent_dfi == node.parent.getDFlow());
			return DFFunc.calc(parent_dfs.func_in, parent_dfi);
		case OUT:
			return func_out.calc(this);
		case TRU:
			return func_tru.calc(this);
		case FLS:
			return func_fls.calc(this);
		case JMP:
			return func_jmp.calc(this);
		}
		throw new RuntimeException("Bad data flow info slot "+slot);
	}

	private kiev.vlang.dflow getClassAnnotation() {
		try {
			java.lang.Class cls = Class.forName(node.getClass().getName()+"$DFI");
			kiev.vlang.dflow dfd = (kiev.vlang.dflow)cls.getAnnotation(kiev.vlang.dflow.class);
			if (dfd == null)
				throw new Error("Internal error: no @dflow in "+node.getClass()+"$DFI");
			return dfd;
		} catch (ClassNotFoundException e) {
			throw new Error("Internal error: no class "+node.getClass()+"$DFI");
		}
		
	}

	private kiev.vlang.dflow getFieldAnnotation(String name) {
		try {
			java.lang.Class cls = Class.forName(node.getClass().getName()+"$DFI");
			java.lang.reflect.Field jf = cls.getDeclaredField(name);
			return (kiev.vlang.dflow)jf.getAnnotation(kiev.vlang.dflow.class);
		} catch (NoSuchFieldException e) {
			return null;
			//throw new Error("Internal error: no field "+name+" in "+getClass()+"$DFI");
		} catch (ClassNotFoundException e) {
			throw new Error("Internal error: no class "+node.getClass()+"$DFI");
		}
		
	}
	
	// build data flow for a child node
	final DFSocket getSocket(String name) {
		for (int i=0; i < children.length; i++) {
			if (children[i].pslot_name == name)
				return children[i];
		}
		throw new RuntimeException("Internal error: no dflow socket "+name+" in "+node.getClass());
	}
	
	private static java.util.regex.Pattern join_pattern = java.util.regex.Pattern.compile("join ([\\:a-zA-Z_0-9\\(\\)]+) ([\\:a-zA-Z_0-9\\(\\)]+)");
	
	public DFFunc[] make(String[] funcs) {
		DFFunc[] dffuncs = new DFFunc[funcs.length];
		for (int i=0; i < funcs.length; i++)
			dffuncs[i] = make(funcs[i]);
		return dffuncs;
	}
	public DFFunc make(String func) {
		java.util.regex.Matcher m = join_pattern.matcher(func);
		if (m.matches())
			return new DFFunc.DFFuncJoin(make(m.group(1)), make(m.group(2)), allocResult());
			
		func = func.intern();
		if (func == "" || func == "this")
			return new DFFunc.DFFuncThisIn();
		int p = func.indexOf(':');
		if (p < 0)
			return new DFFunc.DFFuncChildOut(getSocket(func));
		String port = func.substring(p+1).intern();
		func = func.substring(0,p).intern();
		if (func == "" || func == "this") {
			if (port == "true" || port == "tru")
				return new DFFunc.DFFuncThisTru();
			else if (port == "false" || port == "fls")
				return new DFFunc.DFFuncThisFls();
			else if (port == "in")
				return new DFFunc.DFFuncThisIn();
			else if (port == "out")
				return new DFFunc.DFFuncThisOut();
			else if (port == "true()" || port == "tru()")
				return node.newDFFuncTru(this);
			else if (port == "false()" || port == "fls()")
				return node.newDFFuncFls(this);
			else if (port == "out()")
				return node.newDFFuncOut(this);
			throw new RuntimeException("Internal error: DFFunc.make("+func+":"+port+")");
		}
		else {
			if (port == "true" || port == "tru")
				return new DFFunc.DFFuncChildTru(getSocket(func));
			else if (port == "false" || port == "fls")
				return new DFFunc.DFFuncChildFls(getSocket(func));
			else if (port == "out")
				return new DFFunc.DFFuncChildOut(getSocket(func));
			throw new RuntimeException("Internal error: DFFunc.make("+func+":"+port+")");
		}
	}
	
	public void nodeAttached() {
		if (!is_root) {
			if (ASSERT_MORE) assert(this.parent_dfi == null);
			if (ASSERT_MORE) assert(this.parent_dfs == null);
			parent_dfi = node.parent.getDFlow();
			parent_dfs = parent_dfi.getSocket(node.pslot.name);
			if (parent_dfs instanceof DFSocketChild) {
				int socket_idx = ((DFSocketChild)parent_dfs).socket_idx;
				if (ASSERT_MORE) assert (parent_dfi.attached[socket_idx] == null);
				parent_dfi.attached[socket_idx] = this;
			} else {
				if (ASSERT_MORE) assert (parent_dfs instanceof DFSocketSpace);
			}
		}
	}
	public void dataAttached() {
		if (node.parent != null)
			nodeAttached();
	}
	
	public void nodeDetached() {
		if (parent_dfs != null) {
			assert(parent_dfi != null);
			if (parent_dfs instanceof DFSocketChild) {
				int socket_idx = ((DFSocketChild)parent_dfs).socket_idx;
				if (ASSERT_MORE) assert (parent_dfi.attached[socket_idx] == this);
				parent_dfi.attached[socket_idx] = null;
			} else {
				if (ASSERT_MORE) assert (parent_dfs instanceof DFSocketSpace);
			}
			this.parent_dfi = null;
			this.parent_dfs = null;
		} else {
			assert(parent_dfi == null);
		}
	}
	public void dataDetached() {
		nodeDetached();
	}
	
}

public abstract class DFSocket implements DataFlowSlots, Cloneable {
	final String pslot_name;
	DFFunc func_in;
	
	DFSocket(String pslot_name) {
		this.pslot_name = pslot_name;
	}
	public boolean isSeqSpace() { return false; }
	public Object clone() { return super.clone(); }
}

public class DFSocketChild extends DFSocket {

	// plugged in data flow info of a sub-node
	final int socket_idx;
	
	public DFSocketChild(String pslot_name, int socket_idx) {
		super(pslot_name);
		this.socket_idx = socket_idx;
	}

	public final DataFlowInfo getAttached(DataFlowInfo owner_dfi) {
		DataFlowInfo dfi = owner_dfi.attached[socket_idx];
		if (dfi == null) {
			Object obj = owner_dfi.node.getVal(pslot_name);
			if (obj instanceof ASTNode) {
				dfi = ((ASTNode)obj).getDFlow();
				if (ASSERT_MORE) assert (dfi == owner_dfi.attached[socket_idx] || dfi.is_root);
			}
		}
		return dfi;
	}
}

public abstract class DFSocketSpace extends DFSocket {
	public DFSocketSpace(String pslot_name) {
		super(pslot_name);
	}
}
public class DFSocketSpaceSeq extends DFSocketSpace {
	public DFSocketSpaceSeq(String pslot_name) {
		super(pslot_name);
	}
	public boolean isSeqSpace() { return true; }
}
public class DFSocketSpaceUnknown extends DFSocketSpace {
	public DFSocketSpaceUnknown(String pslot_name) {
		super(pslot_name);
	}
}

public abstract class DFFunc implements DataFlowSlots {
	
	private static DFFunc[] emptyArray = new DFFunc[0];
	
	public case DFFuncThisIn();
	public case DFFuncThisOut();
	public case DFFuncThisTru();
	public case DFFuncThisFls();
	public case DFFuncChildIn(final DFSocket dfs);
	public case DFFuncChildOut(final DFSocket dfs);
	public case DFFuncChildTru(final DFSocket dfs);
	public case DFFuncChildFls(final DFSocket dfs);
	public case DFFuncJoin(final DFFunc f1, final DFFunc f2, final int res_idx);
	public case DFFuncAbrupt(final DFFunc f1, final int res_idx);
	public case DFFuncLabel(final DFFunc func_in, final DFFunc[] link_in, final int lock_mask, final int res_idx);
	
	static boolean checkNode(ASTNode node, Vector<ASTNode> lst) {
		if (node instanceof FileUnit || node instanceof Method)
			return true;
		if (lst == null) lst = new Vector<ASTNode>();
		assert(!lst.contains(node));
		assert(node.isAttached());
		if (node.pslot.is_space)
			assert(((NArr<ASTNode>)node.parent.getVal(node.pslot.name)).contains(node));
		else
			assert(node.parent.getVal(node.pslot.name) == node);
		lst.append(node);
		checkNode(node.parent, lst);
		return true;
	}

	public DFState calc(DataFlowInfo dfi) {
		return DFFunc.calc(this, dfi);
	}
	
	public static DFState calc(DFFunc f, DataFlowInfo dfi) {
		for(;;) {
		switch(f) {
		case DFFuncThisIn():
			if (ASSERT_MORE) assert(checkNode(dfi.node,null));
			if (dfi.func_in != null) {
				f = dfi.func_in;
			}
			else if (dfi.parent_dfs.isSeqSpace() && dfi.node.pprev != null) {
				dfi = dfi.node.pprev.getDFlow();
				f = dfi.func_out;
			}
			else {
				if (ASSERT_MORE) assert(dfi.parent_dfi == dfi.node.parent.getDFlow());
				f = dfi.parent_dfs.func_in;
				dfi = dfi.parent_dfi;
			}
			break;
		case DFFuncThisOut():
			if (ASSERT_MORE) assert(checkNode(dfi.node,null));
			f = dfi.func_out;
			break;
		case DFFuncThisTru():
			if (ASSERT_MORE) assert(checkNode(dfi.node,null));
			f = dfi.func_tru;
			break;
		case DFFuncThisFls():
			if (ASSERT_MORE) assert(checkNode(dfi.node,null));
			f = dfi.func_fls;
			break;
		case DFFuncChildIn(DFSocket dfs):
			f = dfs.func_in;
			break;
		case DFFuncChildOut(DFSocket dfs):
			if (dfs instanceof DFSocketChild) {
				DataFlowInfo attached = dfs.getAttached(dfi);
				if (attached != null) {
					dfi = attached;
					f = dfi.func_out;
				} else {
					f = dfs.func_in;
				}
			}
			else if (dfs instanceof DFSocketSpace) {
				NArr<ASTNode> space = (NArr<ASTNode>)dfi.node.getVal(dfs.pslot_name);
				if (space.size() == 0) {
					f = dfs.func_in;
				} else {
					dfi = space[space.size()-1].getDFlow();
					f = dfi.func_out;
				}
			}
			break;
		case DFFuncChildTru(DFSocket dfs):
			if (dfs instanceof DFSocketChild) {
				DataFlowInfo attached = dfs.getAttached(dfi);
				if (attached != null) {
					dfi = attached;
					f = dfi.func_tru;
				} else {
					f = dfs.func_in;
				}
			}
			break;
		case DFFuncChildFls(DFSocket dfs):
			if (dfs instanceof DFSocketChild) {
				DataFlowInfo attached = dfs.getAttached(dfi);
				if (attached != null) {
					dfi = attached;
					f = dfi.func_fls;
				} else {
					f = dfs.func_in;
				}
			}
			break;
		case DFFuncJoin(DFFunc f1, DFFunc f2, int res_idx):
		{
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = DFState.join(calc(f1,dfi),calc(f2,dfi));
			dfi.setResult(res_idx, res);
			return res;
		}
		case DFFuncAbrupt(DFFunc f1, int res_idx):
		{
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = calc(f1,dfi).setAbrupted();
			dfi.setResult(res_idx, res);
			return res;
		}
		case DFFuncLabel(DFFunc func_in, DFFunc[] link_in, int lock_mask, int res_idx):
		{
			DFState res = dfi.getResult(res_idx);
			if (res == null) {
				DFState tmp = calc(func_in, dfi);
				if ((dfi.locks & lock_mask) != 0)
					throw new DFLoopException(dfi.node);
				dfi.locks |= lock_mask;
				try {
					foreach (DFFunc lnk; link_in) {
						try {
							DFState s = calc(lnk, dfi);
							tmp = DFState.join(s,tmp);
						} catch (DFLoopException e) {
							if (e.label != dfi.node) throw e;
						}
					}
				} finally { dfi.locks &= ~lock_mask; }
				res = tmp;
			}
			dfi.setResult(res_idx, res);
			return res;
		}
		default:
			return f.calc(dfi);
		}
		}
	}
	static int depth;

}

class DFLoopException extends RuntimeException {
	Object label;
	DFLoopException(Object l) { this.label = l; }
}


