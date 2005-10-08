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

	public DFState calcDFlowOut(DFFunc flnk) {
		DFState out;
		if (init != null)
			out = init.getDFlow().out(flnk);
		else
			out = getDFlow().in(flnk);
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
			getDFlow().out(null);
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
	private final boolean abrupted;

	private DFState(List<ScopeNodeInfo> states, boolean abrupted) {
		this.states = states;
		this.abrupted = abrupted;
	}
	
	public static DFState makeNewState() {
		List<ScopeNodeInfo> states = List.Nil;
		return new DFState(states,false);
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
		if (this.abrupted)
			return this;
		return new DFState(states,true);
	}
	
	/** Joins two vectors by AND rule. I.e. initialized = 1.initialized && 2.initialized
	 *  this used for then/else statements of 'if' and || boolean operator
	 */
	public static DFState join(DFState state1, DFState state2) {
		if (state1.abrupted && !state2.abrupted)
			return state2;
		if (state2.abrupted && !state1.abrupted)
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
		DFState dfs = new DFState(states,state1.abrupted && state2.abrupted);
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
	
	final DFFunc func_in;
	DFFunc func_out;
	DFFunc func_tru;
	DFFunc func_fls;
	DFFunc func_jmp;
	private boolean attached;

	// will be a set of fields (DataFlow nodes for children) in code-generation 
	Hashtable<String,DataFlow> children = new Hashtable<String,DataFlow>();
	
	public DataFlow(DFFunc func_in) {
		super(ID);
		this.func_in = func_in;
		this.func_out = func_in;
		this.func_tru = func_in;
		this.func_fls = func_in;
		this.func_jmp = func_in;
	}
	
	public boolean isAttached() { return attached; }
	
	public void nodeAttached(ASTNode n) {
		assert(false);
		n.delNodeData(ID);
	}
	
	public void dataAttached(ASTNode n) {
		assert(!attached);
		kiev.vlang.dflow dfd = (kiev.vlang.dflow)n.getClass().getAnnotation(kiev.vlang.dflow.class);
		String fout="", ftru="", ffls="", fjmp="";
		if (dfd != null) {
			fout = dfd.out().intern();
			fjmp = dfd.jmp().intern();
			ftru = dfd.tru().intern();
			ffls = dfd.fls().intern();
		}
		if (ftru != "" || ffls != "") {
			assert (fout == "" && fjmp == "");
			this.func_tru = DFFunc.make(n,ftru);
			this.func_fls = DFFunc.make(n,ffls);
			this.func_out = new DFFuncJoin(this.func_tru,this.func_fls);
			this.func_jmp = this.func_out;
		}
		else if (fjmp != "") {
			assert (fout == "");
			this.func_jmp = DFFunc.make(n,fjmp);
			this.func_out = new DFFuncAbrupt(this.func_jmp);
			this.func_tru = this.func_out;
			this.func_fls = this.func_out;
		}
		else {
			assert (fjmp == "" && ftru == "" && ffls == "");
			this.func_out = DFFunc.make(n,fout);
			this.func_tru = this.func_out;
			this.func_fls = this.func_out;
			this.func_jmp = this.func_out;
		}
		attached = true;
	}
	
	public void nodeDetached(ASTNode n) {
		n.delNodeData(ID);
	}
	
	public void dataDetached(ASTNode n) {
		this.func_in.unlink_in();
		this.func_in.unlink_out();
		this.func_out.unlink_out();
		this.func_tru.unlink_out();
		this.func_fls.unlink_out();
		this.func_jmp.unlink_out();

		this.func_out = func_in;
		this.func_tru = func_in;
		this.func_fls = func_in;
		this.func_jmp = func_in;
		
		children.clear();
		attached = false;
	}
	
	public void invalidate() {
		func_tru.invalidate();
		func_fls.invalidate();
		func_out.invalidate();
		func_jmp.invalidate();
	}
	
	public final DFState in(DFFunc flnk) {
		return func_in.calc(flnk);
	}
	public final DFState out(DFFunc flnk) {
		return fout().calc(flnk);
	}
	public final DFState tru(DFFunc flnk) {
		return ftru().calc(flnk);
	}
	public final DFState fls(DFFunc flnk) {
		return ffls().calc(flnk);
	}
	public final DFState jmp(DFFunc flnk) {
		return fjmp().calc(flnk);
	}
	
	public DFFunc fin()  { return func_in;  }
	public DFFunc fout() { return func_out; }
	public DFFunc ftru() { return func_tru; }
	public DFFunc ffls() { return func_fls; }
	public DFFunc fjmp() { return func_jmp; }

}

public class DataFlowSpace extends DataFlow {
	public final boolean is_seq;
	private final NArr<ASTNode> space;

	public DataFlowSpace(NArr<ASTNode> space, String func_in, boolean is_seq) {
		super(DFFunc.make(space.getParent(),func_in));
		this.space = space;
		this.is_seq = is_seq;
	}
	public DFFunc fout() {
		if (space.size() == 0)
			return fin();
		return space[space.size()-1].getDFlow().fout();
	}
	public boolean isAttached() { return true; }
}

public abstract class DFFunc {
	
	private static DFFunc[] emptyArray = new DFFunc[0];
	
	private static java.util.regex.Pattern join_pattern = java.util.regex.Pattern.compile("join ([\\:a-zA-Z_0-9\\(\\)]+) ([\\:a-zA-Z_0-9\\(\\)]+)");
	
	
	public static DFFunc[] make(ASTNode node, String[] funcs) {
		assert(checkNode(node,null));
		DFFunc[] dffuncs = new DFFunc[funcs.length];
		for (int i=0; i < funcs.length; i++)
			dffuncs[i] = DFFunc.make(node,funcs[i]);
		return dffuncs;
	}
	public static DFFunc make(ASTNode node, String func) {
		assert(checkNode(node,null));
		java.util.regex.Matcher m = join_pattern.matcher(func);
		if (m.matches())
			return new DFFuncJoin(make(node,m.group(1)), make(node,m.group(2)));
			
		func = func.intern();
		if (func == "" || func == "this")
			return new DFFuncThisIn(node);
		int p = func.indexOf(':');
		if (p < 0)
			return new DFFuncChildOut(node,func);
		String port = func.substring(p+1).intern();
		func = func.substring(0,p).intern();
		if (func == "" || func == "this") {
			if (port == "true" || port == "tru")
				return new DFFuncThisTru(node);
			else if (port == "false" || port == "fls")
				return new DFFuncThisFls(node);
			else if (port == "in")
				return new DFFuncThisIn(node);
			else if (port == "out")
				return new DFFuncThisOut(node);
			else if (port == "true()" || port == "tru()")
				return new DFFuncCalcTru(node);
			else if (port == "false()" || port == "fls()")
				return new DFFuncCalcFls(node);
			else if (port == "in()")
				return new DFFuncCalcIn(node);
			else if (port == "out()")
				return new DFFuncCalcOut(node);
			throw new RuntimeException("Internal error: DFFunc.make("+func+":"+port+")");
		}
		else {
			if (port == "true" || port == "tru")
				return new DFFuncChildTru(node,func);
			else if (port == "false" || port == "fls")
				return new DFFuncChildFls(node,func);
			else if (port == "in")
				return new DFFuncChildIn(node,func);
			else if (port == "out")
				return new DFFuncChildOut(node,func);
			throw new RuntimeException("Internal error: DFFunc.make("+func+":"+port+")");
		}
	}
	
	DFFunc link_in;
	DFFunc[] link_out = DFFunc.emptyArray;
	
	abstract DFState calc(DFFunc flnk);
	
	void invalidate() {}
	
	void link_in(DFFunc flnk) {
		if (flnk == null)
			return;
		if (link_in == null) {
			assert(link_in == flnk);
			link_in = flnk;
			link_in.link_out(this);
		}
	}
	void link_out(DFFunc flnk) {
		if (flnk == null)
			return;
		if (link_out == DFFunc.emptyArray)
			link_out = new DFFunc[]{flnk};
		else
			link_out = (DFFunc[])Arrays.appendUniq(link_out,flnk);
	}
	void unlink_in() {
		DFFunc flnk = link_in;
		if (flnk != null) {
			link_in = null;
			flnk.unlink_out();
			invalidate();
		}
	}
	void unlink_out() {
		DFFunc[] flnk = link_out;
		if (flnk != DFFunc.emptyArray) {
			link_out = DFFunc.emptyArray;
			for (int i=0; i < flnk.length; i++)
				flnk[i].unlink_in();
		}
	}
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

class DFFuncThisIn extends DFFunc {
	ASTNode node;
	DFFuncThisIn(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlow().fin();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}
class DFFuncThisOut extends DFFunc {
	ASTNode node;
	DFFuncThisOut(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlow().fout();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}

class DFFuncThisTru extends DFFunc {
	ASTNode node;
	DFFuncThisTru(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlow().ftru();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}
class DFFuncThisFls extends DFFunc {
	ASTNode node;
	DFFuncThisFls(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlow().ffls();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}
class DFFuncCalcIn extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcIn(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (res != null) return res;
		return res = node.calcDFlowIn(this);
	}
	void invalidate() { res = null; }
}
class DFFuncCalcOut extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcOut(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (res != null) return res;
		return res = node.calcDFlowOut(this);
	}
	void invalidate() { res = null; }
}
class DFFuncCalcTru extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcTru(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (res != null) return res;
		return res = node.calcDFlowTru(this);
	}
	void invalidate() { res = null; }
}
class DFFuncCalcFls extends DFFunc {
	ASTNode node;
	DFState res;
	DFFuncCalcFls(ASTNode node) { this.node = node; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (res != null) return res;
		return res = node.calcDFlowFls(this);
	}
	void invalidate() { res = null; }
}

class DFFuncChildIn extends DFFunc {
	ASTNode node;
	String child;
	DFFuncChildIn(ASTNode node, String child) { this.node = node; this.child = child; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlowFor(child).fin();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}
class DFFuncChildOut extends DFFunc {
	ASTNode node;
	String child;
	DFFuncChildOut(ASTNode node, String child) { this.node = node; this.child = child; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlowFor(child).fout();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}
class DFFuncChildTru extends DFFunc {
	ASTNode node;
	String child;
	DFFuncChildTru(ASTNode node, String child) { this.node = node; this.child = child; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlowFor(child).ftru();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}
class DFFuncChildFls extends DFFunc {
	ASTNode node;
	String child;
	DFFuncChildFls(ASTNode node, String child) { this.node = node; this.child = child; }
	DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			link_in = node.getDFlowFor(child).ffls();
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}

class DFFuncJoin extends DFFunc {
	DFFunc f1, f2;
	DFState res;
	DFFuncJoin(DFFunc f1, DFFunc f2) {
		this.f1 = f1;
		this.f2 = f2;
	}
	DFState calc(DFFunc flnk) {
		link_out(flnk);
		if (res != null) return res;
		return res = DFState.join(f1.calc(this),f2.calc(this));
	}
	void invalidate() { res = null; }
	void unlink_in() {
		f1.unlink_in();
		f2.unlink_in();
		invalidate();
	}
}
class DFFuncAbrupt extends DFFunc {
	DFFunc f;
	DFState res;
	DFFuncAbrupt(DFFunc f) {
		this.f = f;
		this.f.link_out(this);
	}
	DFState calc(DFFunc flnk) {
		link_out(flnk);
		if (res != null) return res;
		return res = f.calc(this).setAbrupted();
	}
	void invalidate() { res = null; }
}
class DFFuncFixedState extends DFFunc {
	final DFState state_in;
	DFFuncFixedState(DFState state_in) {
		this.state_in = state_in;
	}
	DFState calc(DFFunc flnk) {
		link_out(flnk);
		return state_in;
	}
}
class DFFuncFixedFunc extends DFFunc {
	final DFFunc func_in;
	DFFuncFixedFunc(DFFunc func_in) {
		this.func_in = func_in;
	}
	DFState calc(DFFunc flnk) {
		link_out(flnk);
		return func_in.calc(this);
	}
	void unlink_in() {
		func_in.unlink_in();
		invalidate();
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
	DFState calc(DFFunc flnk) {
		link_out(flnk);
		if (res == null) {
			DFState tmp = func_in.calc(this);
			if (lock)
				throw new DFLoopException(this);
			lock = true;
			try {
				foreach (DFFunc lnk; link_in) {
					try {
						lnk.link_out(this);
						DFState s = lnk.calc(this);
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
	void unlink_in() {
		func_in.unlink_in();
		foreach (DFFunc f; link_in)
			f.unlink_in();
		invalidate();
	}
	void invalidate() { res = null; }
}
class DFFuncSpaceSeqNodeIn extends DFFunc {
	public ASTNode node;
	DFFunc space_in;
	public DFFuncSpaceSeqNodeIn(ASTNode node, DFFunc space_in) {
		this.node = node;
		this.space_in = space_in;
	}
	public DFState calc(DFFunc flnk) {
		assert(checkNode(node,null));
		link_out(flnk);
		if (link_in == null) {
			if (node.pprev != null)
				link_in = node.pprev.getDFlow().fout();
			else
				link_in = space_in;
			link_in.link_out(this);
		}
		return link_in.calc(this);
	}
}


class DFLoopException extends RuntimeException {
	Object label;
	DFLoopException(Object l) { this.label = l; }
}


