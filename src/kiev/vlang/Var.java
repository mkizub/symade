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
@dflow(out="this:out()")
public class Var extends DNode implements Named, Typed {

	public static Var[]	emptyArray = new Var[0];

	public NodeName			name;
	
	@att
	public TypeRef			vtype;
	
	@att
	@dflow(in="this:in")
	public ENode			init;
	
	private int				bcpos = -1;

	@ref public abstract virtual access:ro Type	type;
	
	public Var() {
	}

	public Var(int pos, KString name, Type type, int flags) {
		super(pos,flags);
		this.name = new NodeName(name);
		this.vtype = new TypeRef(type);
	}

	public Var(NameRef id, TypeRef vtype, int flags) {
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
		DFState out;
		if (init != null)
			out = init.getDFlow().out();
		else
			out = getDFlow().in();
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
					init = new NewExpr(init.pos,type,new ENode[]{(ENode)~init});
				try {
					init.resolve(this.type);
					Type it = init.getType();
					if( !it.isInstanceOf(this.type) ) {
						init = new CastExpr(init.pos,this.type,(ENode)~init);
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
@dflow(out="this:out()")
public class FormPar extends Var {
	@att public TypeRef		stype;

	public FormPar() {
	}

	public FormPar(int pos, KString name, Type type, int flags) {
		super(pos,name,type,flags);
	}

	public FormPar(NameRef id, TypeRef vtype, TypeRef stype, int flags) {
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
	private final int abrupted;

	private DFState(List<ScopeNodeInfo> states, int abrupted) {
		this.states = states;
		this.abrupted = abrupted;
	}
	
	public static DFState makeNewState() {
		List<ScopeNodeInfo> states = List.Nil;
		return new DFState(states,0);
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
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states),this.abrupted);
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
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states),this.abrupted);
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
		DFState dfs = new DFState(new List.Cons<ScopeNodeInfo>(sni,states),this.abrupted);
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
		assert (state1.abrupted == state2.abrupted);
		DFState dfs = new DFState(states,state1.abrupted);
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

public interface DataFlowSlots {
	public final int IN  = 0;
	public final int OUT = 1;
	public final int TRU = 2;
	public final int FLS = 3;
	public final int JMP = 4;
	
	public final boolean ASSERT_MORE = true;
}

public abstract class DFSocket implements DataFlowSlots {
	final DataFlowInfo owner_dfi;
	final String pslot_name;
	final DFFunc func_in;
	
	DFSocket(DataFlowInfo dfi, String pslot_name, String func_in, String[] link_in) {
		if (ASSERT_MORE) assert(dfi.children.get(pslot_name) == null);
		dfi.children.put(pslot_name,this);
		this.owner_dfi = dfi;
		this.pslot_name = pslot_name;
		if (link_in == null || link_in.length == 0)
			this.func_in = dfi.make(func_in);
		else
			this.func_in = new DFFuncLabel(dfi.make(func_in),dfi.make(link_in));
	}
	
	public abstract void attach(ASTNode child);
	public abstract DFState calc(int slot);
	public abstract boolean isSeqSpace();
}

public class DFSocketChild extends DFSocket {

	// plugged in data flow info of a sub-node
	DataFlowInfo dfi;
	
	public DFSocketChild(DataFlowInfo dfi, String pslot_name, String func_in, String[] link_in) {
		super(dfi, pslot_name, func_in, link_in);
	}

	public void attach(ASTNode child) {
		if (ASSERT_MORE) assert (dfi == null);
		DataFlowInfo dfi = child.getDFlow();
		if (dfi instanceof DataFlowNodeInfo) {
			if (ASSERT_MORE) assert (dfi.dfs == null);
			this.dfi = dfi;
			dfi.dfs = this;
		}
	}
	
	public final DFState calc(int slot) {
		switch (slot) {
		case 0: // in
			return func_in.calc();
		case 1: // out
		case 2: // tru
		case 3: // fls
		case 4: // jmp
			if (dfi == null) {
				Object obj = owner_dfi.node.getVal(pslot_name);
				if (obj instanceof ASTNode) {
					DataFlowInfo tmp = ((ASTNode)obj).getDFlow();
					if (ASSERT_MORE) assert (tmp == this.dfi || tmp instanceof DataFlowRootInfo);
				}
			}
			return (dfi != null) ? dfi.calc(slot) : calc(IN);
		}
		throw new RuntimeException("Bad data flow child socket slot "+slot);
	}
	
	public boolean isSeqSpace() { return false; }
}

public class DFSocketSpace extends DFSocket {
	
	final NArr<ASTNode> space;
	final boolean is_seq;
	
	public DFSocketSpace(DataFlowInfo dfi, NArr<ASTNode> space, String func_in, String[] link_in, boolean is_seq) {
		super(dfi, space.getPSlot().name, func_in, link_in);
		this.space = space;
		this.is_seq	 = is_seq;
	}

	public void attach(ASTNode child) {
		DataFlowInfo dfi = child.getDFlow();
		if (dfi instanceof DataFlowNodeInfo) {
			if (ASSERT_MORE) assert (dfi.dfs == null);
			dfi.dfs = this;
		}
	}
	
	public final DFState calc(int slot) {
		switch (slot) {
		case 0: // in
			return func_in.calc();
		case 1: // out
			if (space.size() == 0)
				return calc(IN);
			return space[space.size()-1].getDFlow().calc(OUT);
		}
		throw new RuntimeException("Bad data flow space socket slot "+slot);
	}

	public boolean isSeqSpace() { return is_seq; }
}

public abstract class DataFlowInfo extends NodeData implements DataFlowSlots {
	public static final KString ID = KString.from("data flow info");

	// will be a set of fields (DataFlow nodes for children) in code-generation 
	Hashtable<String,DFSocket> children = new Hashtable<String,DFSocket>();
	
	// the owner node
	final ASTNode node;
	
	DataFlowInfo(ASTNode node) {
		super(ID);
		this.node = node;
	}

	public final DFState in()  { return calc(IN); }
	public final DFState out() { return calc(OUT); }
	public final DFState tru() { return calc(TRU); }
	public final DFState fls() { return calc(FLS); }
	public final DFState jmp() { return calc(JMP); }

	public abstract DFState calc(int slot);

	private java.lang.reflect.Field getDeclaredField(String name) {
		java.lang.Class cls = node.getClass();
		while (cls != null) {
			try {
				return cls.getDeclaredField(name);
			} catch (NoSuchFieldException e) {}
			cls = cls.getSuperclass();
		}
		throw new RuntimeException("Internal error: no field "+name+" in "+getClass());
	}
	
	// build data flow for a child node
	final DFSocket getSocket(String name) {
		DFSocket df = children.get(name);
		if (df == null) {
			java.lang.reflect.Field jf = getDeclaredField(name);
			kiev.vlang.dflow dfd = (kiev.vlang.dflow)jf.getAnnotation(kiev.vlang.dflow.class);
			String fin = "";
			String[] flnk = null;
			String seq = "";
			if (ASSERT_MORE) assert (dfd != null);
			if (dfd != null) {
				fin = dfd.in().intern();
				flnk = dfd.links();
				seq = dfd.seq().intern();
				if (ASSERT_MORE) assert (seq=="true" || seq=="false" || seq=="");
			}
			if (seq != "") {
				df = new DFSocketSpace(this,(NArr<ASTNode>)node.getVal(name), fin, flnk, seq=="true");
			} else {
				df = new DFSocketChild(this, name, fin, flnk);
			}
		}
		return df;
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
			return new DFFuncJoin(make(m.group(1)), make(m.group(2)));
			
		func = func.intern();
		if (func == "" || func == "this")
			return new DFFuncThis(this,IN);
		int p = func.indexOf(':');
		if (p < 0)
			return new DFFuncChild(getSocket(func),OUT);
		String port = func.substring(p+1).intern();
		func = func.substring(0,p).intern();
		if (func == "" || func == "this") {
			if (port == "true" || port == "tru")
				return new DFFuncThis(this,TRU);
			else if (port == "false" || port == "fls")
				return new DFFuncThis(this,FLS);
			else if (port == "in")
				return new DFFuncThis(this,IN);
			else if (port == "out")
				return new DFFuncThis(this,OUT);
			else if (port == "true()" || port == "tru()")
				return new DFFuncCalcTru(node);
			else if (port == "false()" || port == "fls()")
				return new DFFuncCalcFls(node);
			else if (port == "out()")
				return new DFFuncCalcOut(node);
			throw new RuntimeException("Internal error: DFFunc.make("+func+":"+port+")");
		}
		else {
			if (port == "true" || port == "tru")
				return new DFFuncChild(getSocket(func),TRU);
			else if (port == "false" || port == "fls")
				return new DFFuncChild(getSocket(func),FLS);
			else if (port == "out")
				return new DFFuncChild(getSocket(func),OUT);
			throw new RuntimeException("Internal error: DFFunc.make("+func+":"+port+")");
		}
	}
	
}

public final class DataFlowRootInfo extends DataFlowInfo {

	final DFFunc func_in;
	
	DataFlowRootInfo(ASTNode node, DFFunc func_in) {
		super(node);
		this.func_in = func_in;
	}

	public DFState calc(int slot) {
		switch (slot) {
		case 0: // in
			return func_in.calc();
		}
		throw new RuntimeException("Bad data flow space socket slot "+slot);
	}

}

public final class DataFlowNodeInfo extends DataFlowInfo {

	// a socket of the parent node this data flow is plugged in
	DFSocket dfs;

	private DFFunc func_out;
	private DFFunc func_tru;
	private DFFunc func_fls;
	private DFFunc func_jmp;

	DataFlowNodeInfo(ASTNode node) {
		super(node);
		kiev.vlang.dflow dfd = (kiev.vlang.dflow)node.getClass().getAnnotation(kiev.vlang.dflow.class);
		String fout="", ftru="", ffls="", fjmp="";
		assert (dfd != null);
		if (dfd != null) {
			fout = dfd.out().intern();
			fjmp = dfd.jmp().intern();
			ftru = dfd.tru().intern();
			ffls = dfd.fls().intern();
		}
		if (ftru != "" || ffls != "") {
			assert (fout == "" && fjmp == "");
			this.func_tru = make(ftru);
			this.func_fls = make(ffls);
			this.func_out = new DFFuncJoin(this.func_tru,this.func_fls);
		}
		else if (fjmp != "") {
			assert (fout == "");
			this.func_jmp = make(fjmp);
			this.func_out = new DFFuncAbrupt(this.func_jmp);
		}
		else {
			assert (fjmp == "" && ftru == "" && ffls == "");
			this.func_out = make(fout);
		}
	}

	public final DFState calc(int slot) {
		switch (slot) {
		case 0: // in
			if (dfs.isSeqSpace() && node.pprev != null)
				return node.pprev.getDFlow().calc(OUT);
			return dfs.calc(IN);
		case 1: // out
			return func_out.calc();
		case 2: // tru
			return func_tru == null ? calc(OUT) : func_tru.calc();
		case 3: // fls
			return func_fls == null ? calc(OUT) : func_fls.calc();
		case 4: // jmp
			return func_jmp == null ? calc(OUT) : func_jmp.calc();
		}
		throw new RuntimeException("Bad data flow info slot "+slot);
	}

	public void nodeAttached(ASTNode n) {
		if (ASSERT_MORE) assert(this.node == n);
		if (ASSERT_MORE) assert(this.dfs == null);
		DFSocket dfs = node.parent.getDFlow().getSocket(node.pslot.name);
		if (dfs instanceof DFSocketChild) {
			if (ASSERT_MORE) assert (dfs.dfi == null);
			this.dfs = dfs;
			dfs.dfi = this;
		} else {
			if (ASSERT_MORE) assert (dfs instanceof DFSocketSpace);
			this.dfs = dfs;
		}
	}
	public void dataAttached(ASTNode n) {
		if (n.parent != null)
			nodeAttached(n);
	}
	
	public void nodeDetached(ASTNode n) {
		if (ASSERT_MORE) assert(this.node == n);
		DFSocket dfs = this.dfs;
		if (dfs != null) {
			if (ASSERT_MORE) assert (dfs.owner_dfi.node == this.node.parent);
			if (dfs instanceof DFSocketChild) {
				if (ASSERT_MORE) assert (dfs.dfi == this);
				this.dfs = null;
				dfs.dfi = null;
			} else {
				if (ASSERT_MORE) assert (dfs instanceof DFSocketSpace);
				this.dfs = null;
			}
		}
	}
	public void dataDetached(ASTNode n) {
		nodeDetached(n);
	}
	
}

public abstract class DFFunc implements DataFlowSlots {
	
	private static DFFunc[] emptyArray = new DFFunc[0];
	
	abstract DFState calc();
	
	void invalidate() {}
	
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
}

class DFFuncThis extends DFFunc {
	final DataFlowInfo dfi;
	final int slot;
	DFFuncThis(DataFlowInfo dfi, int slot) {
		this.dfi  = dfi;
		this.slot = slot;
	}
	DFState calc() {
		if (ASSERT_MORE) assert(checkNode(dfi.node,null));
		return dfi.calc(slot);
	}
}
class DFFuncChild extends DFFunc {
	final DFSocket dfs;
	final int slot;
	DFFuncChild(DFSocket dfs, int slot) {
		this.dfs = dfs;
		this.slot = slot;
	}
	DFState calc() {
		if (ASSERT_MORE) assert(checkNode(dfs.owner_dfi.node,null));
		return dfs.calc(slot);
	}
}
class DFFuncCalcOut extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcOut(ASTNode node) { this.node = node; }
	DFState calc() {
		if (ASSERT_MORE) assert(checkNode(node,null));
		if (res != null) return res;
		return res = node.calcDFlowOut();
	}
	void invalidate() { res = null; }
}
class DFFuncCalcTru extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcTru(ASTNode node) { this.node = node; }
	DFState calc() {
		if (ASSERT_MORE) assert(checkNode(node,null));
		if (res != null) return res;
		return res = node.calcDFlowTru();
	}
	void invalidate() { res = null; }
}
class DFFuncCalcFls extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcFls(ASTNode node) { this.node = node; }
	DFState calc() {
		if (ASSERT_MORE) assert(checkNode(node,null));
		if (res != null) return res;
		return res = node.calcDFlowFls();
	}
	void invalidate() { res = null; }
}

class DFFuncJoin extends DFFunc {
	DFFunc f1, f2;
	DFState res;
	DFFuncJoin(DFFunc f1, DFFunc f2) {
		this.f1 = f1;
		this.f2 = f2;
	}
	DFState calc() {
		if (res != null) return res;
		return res = DFState.join(f1.calc(),f2.calc());
	}
	void invalidate() { res = null; }
}

class DFFuncAbrupt extends DFFunc {
	DFFunc f;
	DFState res;
	DFFuncAbrupt(DFFunc f) {
		this.f = f;
	}
	DFState calc() {
		if (res != null) return res;
		return res = f.calc().setAbrupted();
	}
	void invalidate() { res = null; }
}

class DFFuncFixedState extends DFFunc {
	final DFState state_in;
	DFFuncFixedState(DFState state_in) {
		this.state_in = state_in;
	}
	DFState calc() {
		return state_in;
	}
}

class DFFuncLabel extends DFFunc {
	final DFFunc    func_in;
	final DFFunc[]  link_in;
	private boolean lock;
	DFState res;
	DFFuncLabel(DFFunc func_in, DFFunc[] link_in) {
		this.func_in = func_in;
		this.link_in = link_in;
	}
	DFState calc() {
		if (res == null) {
			DFState tmp = func_in.calc();
			if (lock)
				throw new DFLoopException(this);
			lock = true;
			try {
				foreach (DFFunc lnk; link_in) {
					try {
						DFState s = lnk.calc();
						tmp = DFState.join(s,tmp);
					} catch (DFLoopException e) {
						if (e.label != this) throw e;
					}
				}
			} finally { lock = false; }
			res = tmp;
		}
		return res;
	}
}

class DFLoopException extends RuntimeException {
	Object label;
	DFLoopException(Object l) { this.label = l; }
}


