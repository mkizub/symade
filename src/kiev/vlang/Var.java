/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
@dflow(out="this:?out")
public class Var extends DNode implements Named, Typed {

	public static Var[]	emptyArray = new Var[0];

	public NodeName			name;
	@att public TypeRef		vtype;
	@att public ENode		init;
	     int				bcpos = -1;

	@ref public abstract virtual access:ro Type	type;
	
	public Var() {
	}

	public Var(int pos, KString name, Type type, int flags) {
		super(pos,flags);
		this.name = new NodeName(name);
		this.vtype = new TypeRef(type);
	}

	public Var(ASTIdentifier id, TypeRef vtype, int flags) {
		super(id.pos,flags);
		this.name = new NodeName(id.name);
		this.vtype = vtype;
	}

	@getter public Type get$type() {
		return vtype.getType();
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if      (attr.name == "vtype")
				parent.callbackChildChanged(pslot);
			else if (attr.name == "meta")
				parent.callbackChildChanged(pslot);
		}
	}
	
	public String toString() {
		return name.toString()/*+":="+type*/;
	}

	public int hashCode() {
		return name.hashCode();
	}

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	public DFState calcDFlowOut() {
		DFState out = getDFlow().in();
		if (init != null)
			out = init.getDFlow().out();
		out = out.declNode(this);
		if( init != null && init.getType() != Type.tpVoid )
			out = out.setNodeValue(new DNode[]{this},init);
		return out;
	}
	
	public void resolveDecl() {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			if( init == null && !type.isArray() && type.isWrapper() && !this.isInitWrapper())
				init = new NewExpr(pos,type,Expr.emptyArray);
			if( init != null ) {
				if (init instanceof TypeRef)
					((TypeRef)init).toExpr(this.getType());
				if (type.isWrapper() && !this.isInitWrapper())
					init = new NewExpr(init.pos,type,new ENode[]{init});
				try {
					init.resolve(this.type);
					Type it = init.getType();
					if( !it.isInstanceOf(this.type) ) {
						init = new CastExpr(init.pos,this.type,init);
						init.resolve(this.type);
					}
				} catch(Exception e ) {
					Kiev.reportError(pos,e);
				}
			}
			getDFlow().out();
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Var declaration");
		//assert (parent instanceof BlockStat || parent instanceof ExprStat || parent instanceof ForInit);
		PassInfo.push(this);
		try {
			if( init != null ) {
				if( !this.isNeedRefProxy() ) {
					init.generate(this.type);
					Code.addVar(this);
					Code.addInstr(Instr.op_store,this);
				} else {
					Type prt = Type.getProxyType(this.type);
					Code.addInstr(Instr.op_new,prt);
					Code.addInstr(Instr.op_dup);
					init.generate(this.type);
					MethodType mt = MethodType.newMethodType(null,new Type[]{init.getType()},Type.tpVoid);
					Method@ in;
					PassInfo.resolveBestMethodR(prt,in,new ResInfo(ResInfo.noForwards),nameInit,mt);
					Code.addInstr(Instr.op_call,in,false);
					Code.addVar(this);
					Code.addInstr(Instr.op_store,this);
				}
			} else {
				Code.addVar(this);
			}
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name);
	}

	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
//		if (isFinal()) dmp.append("final").forsed_space();
//		if (isForward()) dmp.append("forward").forsed_space();
		if( isNeedRefProxy() )
			dmp.append(Type.getProxyType(type));
		else
			dmp.append(type);
		dmp.forsed_space().append(name);
		if (init != null)
			dmp.space().append('=').append(init);
		dmp.append(';').newLine();
		return dmp;
	}

	public Dumper toJavaDecl(Dumper dmp, Type jtype) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( isNeedRefProxy() )
			dmp.append(Type.getProxyType(jtype));
		else
			dmp.append(jtype);
		return dmp.forsed_space().append(name);
	}

	public void setBCpos(int pos) {
		if( pos < 0 || pos > 255)
			throw new RuntimeException("Bad bytecode position specified: "+pos);
		bcpos = pos;
	}

	public int getBCpos() {
		return bcpos;
	}

}

@node
public class FormPar extends Var {
	@att public TypeRef		stype;

	public FormPar() {
	}

	public FormPar(int pos, KString name, Type type, int flags) {
		super(pos,name,type,flags);
	}

	public FormPar(ASTIdentifier id, TypeRef vtype, TypeRef stype, int flags) {
		super(id,vtype,flags);
		this.stype = stype;
	}

	public void callbackChildChanged(AttrSlot attr) {
		if (parent != null && pslot != null) {
			if (attr.name == "stype")
				parent.callbackChildChanged(pslot);
			else
				super.callbackChildChanged(attr);
		}
	}
	
}

public class CodeVar {

	public Var			var;
	public int		stack_pos = -1;
	public int		start_pc = -1;
	public int		end_pc = -1;
	public int		index = 0;

	public CodeVar(Var var) {
		this.var = var;
		stack_pos = var.getBCpos();
	}

	public String toString() {
		return "("+stack_pos+","+index+","+start_pc+","+end_pc+")";
	}

}

public class DFState {
	
	public static final DFState[] emptyArray = new DFState[0];

	private final List<ScopeNodeInfo> states;

	private DFState(List<ScopeNodeInfo> states) {
		this.states = states;
	}
	
	public static DFState makeNewState() {
		List<ScopeNodeInfo> states = List.Nil;
		return new DFState(states);
	}
	
	public ScopeNodeInfo getNodeInfo(DNode[] path) {
		foreach(ScopeNodeInfo sni; states; sni.match(path)) {
			trace( Kiev.debugNodeTypes, "types: getinfo for node "+Arrays.toString(path)+" is "+sni);
			return sni;
		}
		return null;
	}

	private static ScopeNodeInfo makeNode(DNode[] path) {
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
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states));
		trace( Kiev.debugNodeTypes, "types: (decl) var "+sni);
		return dfs;
	}

	public DFState addNodeType(DNode[] path, Type type) {
		ScopeNodeInfo sni = getNodeInfo(path);
		if (sni == null) sni = makeNode(path);
		if (sni == null) return this;
		Type[] types = addAccessType(sni.types, type);
		if (types.length == sni.types.length) {
			for (int i=0; i < types.length; i++) {
				if (types[i] != sni.types[i])
					goto changed;
			}
			return this;
		}
changed:;
		sni = (ScopeNodeInfo)sni.clone();
		sni.types = types;
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states));
		trace( Kiev.debugNodeTypes, "types: (set types to) "+sni);
		return dfs;
	}

	public DFState setNodeValue(DNode[] path, ENode expr) {
		Type tp = expr.getType();
		if( tp == Type.tpNull && tp == Type.tpVoid )
			return this;
		ScopeNodeInfo sni = makeNode(path);
		if (sni == null) return this;
		Type[] types = addAccessType(sni.types, tp);
		sni.types = types;
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states));
		trace( Kiev.debugNodeTypes, "types: (set value to) "+sni);
		return dfs;
	}

	/** Add access type to access type array
	 *  Due to the Java allows only one class and many interfaces,
	 *  the access type array must has first element to be
	 *  the class type (if exists), and others to be interface types
	 */
	private static Type[] addAccessType(Type[] types, Type type) {
		if( type == null || type == Type.tpVoid || type == Type.tpNull ) return types;
		if( types == null || !type.isReference() ) {
			return new Type[]{type};
		}
		trace( Kiev.debugNodeTypes, "types: add type "+type+" to "+Arrays.toString(types));
		Type[] newtypes = new Type[]{type};
	next_type:
		foreach(Type t1; types; t1 != null && t1 != Type.tpVoid && t1 != Type.tpNull ) {
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
		DFState dfs = new DFState(states);
		return dfs;
	}

	/** Joins two vectors by AND rule. I.e. initialized = 1.initialized && 2.initialized
	 *  this used for then/else statements of 'if' and || boolean operator
	 */
	public static DFState join(DFState state1, DFState state2) {
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
			sni = (ScopeNodeInfo)sni.clone();
			sni.setupDeclType();
			Type[] types = sni.types;
			foreach(Type t1; sni1.types; t1 != null && t1 != Type.tpVoid && t1 != Type.tpNull) {
				foreach(Type t2; sni2.types; t2 != null && t2 != Type.tpVoid && t2 != Type.tpNull )
					types = addAccessType(types,Type.leastCommonType(t1,t2));
			}
			sni.types = types;
			states = new List.Cons<ScopeNodeInfo>(sni, states);
			trace( Kiev.debugNodeTypes, "types: joining "+sni1+" and "+ sni2+" => "+sni);
		}
		trace( Kiev.debugNodeTypes, "types: joined to "+states);
		DFState dfs = new DFState(states);
		return dfs;
	}

}

public abstract class ScopeNodeInfo implements Cloneable {
	public Type[]	types = Type.emptyArray;
	public abstract void setupDeclType();
	public abstract boolean match(DNode[] path);
	public abstract DNode[] getPath();
	public Object clone() { return super.clone(); }
}

public class ScopeVarInfo extends ScopeNodeInfo {

	public Var		var;

	public ScopeVarInfo(Var var) {
		this.var = var;
		setupDeclType();
	}

	public void setupDeclType() {
		this.types = new Type[]{var.type};
	}
	
	public String toString() {
		return "sni:{var "+var+","+Arrays.toString(types)+"}";
	}

	public Object clone() {
		ScopeVarInfo newsni = (ScopeVarInfo)super.clone();
		if( newsni.types.length > 0)
			newsni.types = (Type[])newsni.types.clone();
		return newsni;
	}
	
	public DNode[] getPath() {
		return new DNode[]{var};
	}
	
	public boolean equals(Object obj) {
		if !(obj instanceof ScopeVarInfo)
			return false;
		return var == ((ScopeVarInfo)obj).var;
	}
	
	public boolean match(DNode[] path) {
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

	public void setupDeclType() {
		this.types = new Type[]{fld.type};
	}
	
	public String toString() {
		return "sni:{static fld "+fld+","+Arrays.toString(types)+"}";
	}

	public Object clone() {
		ScopeStaticFieldInfo newsni = (ScopeStaticFieldInfo)super.clone();
		if( newsni.types.length > 0)
			newsni.types = (Type[])newsni.types.clone();
		return newsni;
	}
	
	public DNode[] getPath() {
		return new DNode[]{fld};
	}
	
	public boolean equals(Object obj) {
		if !(obj instanceof ScopeStaticFieldInfo)
			return false;
		return fld == ((ScopeStaticFieldInfo)obj).fld;
	}
	
	public boolean match(DNode[] path) {
		return path.length==1 && path[0] == fld;
	}
}

public class ScopeForwardFieldInfo extends ScopeNodeInfo {

	public DNode[] path;

	public ScopeForwardFieldInfo(DNode[] path) {
		assert(path.length > 1);
		assert(checkForwards(path));
		this.path = path;
		setupDeclType();
	}
	
	public static boolean checkForwards(DNode[] path) {
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
	
	public void setupDeclType() {
		Type tp = ((Var)path[0]).type;
		for(int i=1; i < path.length; i++)
			tp = Type.getRealType(tp, ((Field)path[i]).type);
		this.types = new Type[]{tp};
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < path.length; i++) {
			if (i < path.length-1)
				sb.append('.');
			sb.append(path[i]);
		}
		return "sni:{forward fld "+sb.toString()+","+Arrays.toString(types)+"}";
	}

	public DNode[] getPath() {
		return path;
	}
	
	public Object clone() {
		ScopeStaticFieldInfo newsni = (ScopeStaticFieldInfo)super.clone();
		if( newsni.types.length > 0)
			newsni.types = (Type[])newsni.types.clone();
		return newsni;
	}

	
	public boolean equals(Object obj) {
		if !(obj instanceof ScopeForwardFieldInfo)
			return false;
		ScopeForwardFieldInfo ffi = (ScopeForwardFieldInfo)obj;
		return match(ffi.path);
	}

	public boolean match(DNode[] path) {
		if (this.path.length != path.length)
			return false;
		for(int i=0; i < path.length; i++) {
			if (this.path[i] != path[i])
				return false;
		}
		return true;
	}
}

public class DataFlow extends NodeData {
	public static final KString ID = KString.from("data flow");

	DataFlowIn  df_in;
	DataFlowOut df_out;

	public DataFlow(DataFlowIn df_in) {
		super(ID);
		assert(df_in != null);
		this.df_in = df_in;
	}
	
	public void nodeAttached(ASTNode n) {
		n.delNodeData(ID);
	}
	
	public void dataAttached(ASTNode n) {
		assert(df_out == null);
		kiev.vlang.dflow dfd = (kiev.vlang.dflow)n.getClass().getAnnotation(kiev.vlang.dflow.class);
		String fout="", ftru="", ffls="";
		if (dfd != null) {
			fout = dfd.out().intern();
			ftru = dfd.tru().intern();
			ffls = dfd.fls().intern();
		}
		df_out = new DataFlowOutFunc(n, fout, ftru, ffls);
	}
	
	public void nodeDetached(ASTNode n) {
		n.delNodeData(ID);
	}
	
	public void dataDetached(ASTNode n) {
		df_out = null;
	}
	
	public final DFState in() {
		return df_in.calcDFStateIn();
	}
	public DFState out() {
		if (df_out == null)
			return df_in.calcDFStateIn();
		return df_out.calcDFStateOut();
	}
	public final DFState tru() {
		if (df_out == null)
			return df_in.calcDFStateIn();
		return df_out.calcDFStateTru();
	}
	public final DFState fls() {
		if (df_out == null)
			return df_in.calcDFStateIn();
		return df_out.calcDFStateFls();
	}
}

// DataFlowIn is owned and configured by a parent node, and calculates
// input DFState for a child node
public abstract class DataFlowIn {
	public DataFlowIn() {
	}
	public abstract DFState calcDFStateIn();
}

public class DataFlowInFunc extends DataFlowIn {
	public ASTNode owner;
	private DFState state_in;
	public final boolean is_seq;
	public final String func_in;

	public DataFlowInFunc(ASTNode owner, String func_in, boolean is_seq) {
		this.owner = owner;
		this.func_in = func_in;
		this.is_seq = is_seq;
	}
	public DFState calcDFStateIn() {
		if (state_in == null)
			state_in = owner.getDFStateOfExpr(func_in);
		return state_in;
	}
}

public class DataFlowInSpace extends DataFlowIn {
	public ASTNode owner;
	DataFlowInFunc space_in;
	public DataFlowInSpace(ASTNode owner, DataFlowIn space_in) {
		this.owner = owner;
		this.space_in = (DataFlowInFunc)space_in;
	}
	public DFState calcDFStateIn() {
		if (space_in.is_seq && owner.pprev != null)
			return owner.pprev.getDFlow().out();
		else
			return space_in.calcDFStateIn();
	}
}

public class DataFlowInFixed extends DataFlowIn {
	private DFState state_in;
	public DataFlowInFixed(DFState state_in) {
		this.state_in = state_in;
	}
	public DFState calcDFStateIn() {
		return state_in;
	}
}

// DataFlowOut is owned and configured by a node, and holds
// DataFlow subnodes for chidlren nodes, and calculates out
// DFState of the node
public abstract class DataFlowOut {

	// will be a set of fields (DataFlow nodes for children) in code-generation 
	Hashtable<String,DataFlow> children = new Hashtable<String,DataFlow>();
	
	public ASTNode owner;
	
	public DataFlowOut(ASTNode owner) {
		this.owner = owner;
	}
	public abstract DFState calcDFStateOut();
	public abstract DFState calcDFStateTru();
	public abstract DFState calcDFStateFls();
	public abstract void reset();
}
public class DataFlowOutSpaceSeq extends DataFlowOut {
	private final NArr<ASTNode> space;
	DataFlowIn space_in;
	public DataFlowOutSpaceSeq(ASTNode owner, NArr<ASTNode> space, DataFlowIn space_in) {
		super(owner);
		this.space = space;
		this.space_in = space_in;
	}
	public DFState calcDFStateOut() {
		if (space.size() == 0)
			return space_in.calcDFStateIn();
		return space[space.size()-1].getDFlow().out();
	}
	public DFState calcDFStateTru() { return calcDFStateOut(); }
	public DFState calcDFStateFls() { return calcDFStateOut(); }
	public void reset() {}
}
public class DataFlowOutFunc extends DataFlowOut {
	public final String func_out;
	public final String func_tru;
	public final String func_fls;

	private DFState state_out;
	private DFState state_tru;
	private DFState state_fls;

	public DataFlowOutFunc(ASTNode owner, String func_out, String func_tru, String func_fls) {
		super(owner);
		this.func_out = func_out;
		this.func_tru = func_tru;
		this.func_fls = func_fls;
	}
	public void reset() {
		state_out = null;
		state_tru = null;
		state_fls = null;
	}
	public DFState calcDFStateOut() {
		if (state_out == null) {
			if (func_tru != "" || func_fls != "") {
				state_tru = owner.getDFStateOfExpr(func_tru);
				state_fls = owner.getDFStateOfExpr(func_fls);
			}
			state_out = owner.getDFStateOfExpr(func_out);
		}
		return state_out;
	}
	public DFState calcDFStateTru() {
		if (state_tru != null)
			return state_tru;
		if (state_out == null) {
			if (func_tru != "" || func_fls != "") {
				state_tru = owner.getDFStateOfExpr(func_tru);
				state_fls = owner.getDFStateOfExpr(func_fls);
			}
			state_out = owner.getDFStateOfExpr(func_out);
		}
		return state_tru==null? state_out : state_tru;
	}
	public DFState calcDFStateFls() {
		if (state_fls != null)
			return state_fls;
		if (state_out == null) {
			if (func_tru != "" || func_fls != "") {
				state_tru = owner.getDFStateOfExpr(func_tru);
				state_fls = owner.getDFStateOfExpr(func_fls);
			}
			state_out = owner.getDFStateOfExpr(func_out);
		}
		return state_fls==null? state_out : state_fls;
	}
}


