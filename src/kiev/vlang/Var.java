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
					if( it != this.type ) {
						init = new CastExpr(init.pos,this.type,init);
						init.resolve(this.type);
					}
				} catch(Exception e ) {
					Kiev.reportError(pos,e);
				}
			}
			NodeInfoPass.setNodeType(new DNode[]{this},this.type);
			if( init != null && init.getType() != Type.tpVoid )
				NodeInfoPass.setNodeValue(new DNode[]{this},init);
			ASTNode p = parent;
			while( p != null && !(p instanceof BlockStat || p instanceof BlockExpr) ) p = p.parent;
			if( p == null ) {
				Kiev.reportWarning(pos,"Can't find scope for var "+this);
			}
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
//		Env.toJavaModifiers(dmp,access);
		if (isFinal()) dmp.append("final").forsed_space();
		if (isForward()) dmp.append("forward").forsed_space();
		if( isNeedRefProxy() )
			dmp.append(Type.getProxyType(type));
		else
			dmp.append(type);
		return dmp.forsed_space().append(name);
	}

	public Dumper toJavaDecl(Dumper dmp, Type jtype) {
//		Env.toJavaModifiers(dmp,access);
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

public class ScopeNodeInfoVector extends Vector<ScopeNodeInfo> {
	public boolean		guarded;
	public void setInfo(ScopeNodeInfo sni) {
		for(int i=length()-1; i >= 0; i--) {
			if (this[i].equals(sni)) {
				this[i] = sni;
				return;
			}
		}
		append(sni);
	}
}

public class NodeInfoPass {

	public static Stack<ScopeNodeInfoVector>	states;
	public static Stack<Stack<ScopeNodeInfoVector>>	init_states =
		new Stack<Stack<ScopeNodeInfoVector>>();

	public static void init() {
		init_states.push(states);
		states = new Stack<ScopeNodeInfoVector>();
	}

	public static void close() {
		states = init_states.pop();
	}
	
	public static int getDepth() {
		if (states == null) return -1;
		return states.length()-1;
	}

	private static ScopeNodeInfo makeNodeInThisScope(DNode[] path) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = getNodeInfo(path);
		if( sni != null ) {
			trace( Kiev.debugNodeTypes, "types: clone "+sni+" to current scope "+getDepth());
			sni = (ScopeNodeInfo)sni.clone();
			state.setInfo(sni);
		} else {
			if (path.length == 1) {
				if (path[0] instanceof Var)
					sni = new ScopeVarInfo((Var)path[0]);
				else if (path[0] instanceof Field && path[0].isStatic())
					sni = new ScopeStaticFieldInfo((Field)path[0]);
			}
			else if (ScopeForwardFieldInfo.checkForwards(path)) {
				sni = new ScopeForwardFieldInfo(path);
			}
			else
				return null;
			state.setInfo(sni);
			trace( Kiev.debugNodeTypes, "types: add "+sni+" to current scope "+getDepth());
		}
		return sni;
	}

	public static ScopeNodeInfo setNodeType(DNode[] path, Type type) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = makeNodeInThisScope(path);

		trace( Kiev.debugNodeTypes, "types: set type to "+sni);
		return sni;
	}

	public static ScopeNodeInfo setNodeTypes(DNode[] path, Type[] types) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = makeNodeInThisScope(path);

		sni.types = types;
		trace( Kiev.debugNodeTypes, "types: set types to "+sni);
		return sni;
	}

	public static ScopeNodeInfo setNodeValue(DNode[] path, ENode expr) {
		Type tp = expr.getType();
		ScopeNodeInfo sni = makeNodeInThisScope(path);

		if( tp != Type.tpNull && tp != Type.tpVoid ) {
			foreach (Type tp; sni.types)
				sni.types = addAccessType(expr.getAccessTypes(), tp);
		}
		trace( Kiev.debugNodeTypes, "types: set type "+tp+" for node "+sni);
		return sni;
	}

	static ScopeNodeInfo addInfo(ScopeNodeInfo sni_new) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = makeNodeInThisScope(sni_new.getPath());

		foreach (Type tp; sni.types)
			sni.types = addAccessType(sni_new.types, tp);
		trace( Kiev.debugNodeTypes, "types: set info to "+sni);
		return sni;
	}

	public static ScopeNodeInfoVector pushState() {
		ScopeNodeInfoVector v = new ScopeNodeInfoVector();
		states.push(v);
		trace( Kiev.debugNodeTypes, "types: push state to level "+getDepth());
		return v;
	}

	public static ScopeNodeInfoVector pushGuardedState() {
		ScopeNodeInfoVector v = new ScopeNodeInfoVector();
		states.push(v);
		v.guarded = true;
		trace( Kiev.debugNodeTypes, "types: push state to level "+getDepth());
		return v;
	}

	public static ScopeNodeInfoVector pushState(ScopeNodeInfoVector v) {
		states.push(v);
		trace( Kiev.debugNodeTypes, "types: push state to level "+getDepth());
		return v;
	}

	public static ScopeNodeInfoVector popState() {
		trace( Kiev.debugNodeTypes, "types: pop state to level "+(getDepth()-1));
		return states.pop();
	}

	public static ScopeNodeInfo getNodeInfo(DNode[] path) {
		int i = states.length();
		boolean guarded = false;
		foreach(ScopeNodeInfoVector state; states) {
			foreach(ScopeNodeInfo sni; state; sni.match(path)) {
				trace( Kiev.debugNodeTypes, "types: getinfo for node "+Arrays.toString(path)+" is "+sni+" in scope "+ --i);
				if( guarded ) {
					sni = (ScopeNodeInfo)sni.clone();
					sni.setupDeclType();
				}
				return sni;
			}
			if( state.guarded ) guarded = true;
		}
		return null;
	}

	/** Add access type to access type array
	 *  Due to the Java allows only one class and many interfaces,
	 *  the access type array must has first element to be
	 *  the class type (if exists), and others to be interface types
	 */
	public static Type[] addAccessType(Type[] types, Type type) {
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

	static ScopeNodeInfoVector cleanInfoForVars(ScopeNodeInfoVector nip_state, Var[] vars) {
		foreach(Var v; vars) {
			foreach(ScopeNodeInfo sni; nip_state) {
				if (sni instanceof ScopeVarInfo && ((ScopeVarInfo)sni).var == v) {
					nip_state.removeElement(sni);
					break;
				}
				if (sni instanceof ScopeForwardFieldInfo && ((ScopeForwardFieldInfo)sni).path[0] == v) {
					nip_state.removeElement(sni);
					break;
				}
			}
		}
		return nip_state;
	}

	static void addInfo(ScopeNodeInfoVector nip_state) {
		foreach(ScopeNodeInfo sni; nip_state)
			addInfo(sni);
	}

	/** Joins two vectors by AND rule. I.e. initialized = 1.initialized && 2.initialized
	 *  this used for then/else statements of 'if' and || boolean operator
	 */
	static ScopeNodeInfoVector joinInfo(ScopeNodeInfoVector nip_state1, ScopeNodeInfoVector nip_state2) {
		ScopeNodeInfoVector nip_state = new ScopeNodeInfoVector();
		foreach(ScopeNodeInfo sni1; nip_state1) {
			foreach(ScopeNodeInfo sni2; nip_state2; sni2.equals(sni1) ) {
				trace( Kiev.debugNodeTypes, "types: joining "+sni1+" and "+ sni2);
				ScopeNodeInfo sni = (ScopeNodeInfo)sni1.clone();
				sni.setupDeclType();
				Type[] types = sni.types;
				foreach(Type t1; sni1.types; t1 != null && t1 != Type.tpVoid && t1 != Type.tpNull) {
					foreach(Type t2; sni2.types; t2 != null && t2 != Type.tpVoid && t2 != Type.tpNull )
						types = addAccessType(types,Type.leastCommonType(t1,t2));
				}
				sni.types = types;
				nip_state.setInfo(sni);
			}
		}
		trace( Kiev.debugNodeTypes, "types: joined to "+nip_state);
		return nip_state;
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


