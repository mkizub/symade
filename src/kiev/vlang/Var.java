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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Var.java,v 1.5.2.1 1999/02/12 18:47:10 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1 $
 *
 */

@node
public class Var extends ASTNode implements Named, Typed {

	public static Var[]	emptyArray = new Var[0];

	public NodeName			name;
	@att public TypeRef		vtype;
	@att public MetaSet		meta;
	@att public Expr		init;
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

	public ASTNode resolve(Type reqType) {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			if( init == null && !type.isArray() && type.isWrapper() && !this.isInitWrapper())
				init = new NewExpr(pos,type,Expr.emptyArray);
			if( init != null ) {
				if (type.isWrapper() && !this.isInitWrapper())
					init = new NewExpr(init.pos,type,new Expr[]{init});
				try {
					init = init.resolveExpr(this.type);
					Type it = init.getType();
					if( it != this.type ) {
						init = new CastExpr(init.pos,this.type,init);
						init = init.resolveExpr(this.type);
					}
				} catch(Exception e ) {
					Kiev.reportError(pos,e);
				}
			}
			NodeInfoPass.setNodeType(this,this.type);
			if( init != null && init.getType() != Type.tpVoid )
				NodeInfoPass.setNodeValue(this,init);
			ASTNode p = parent;
			while( p != null && !(p instanceof BlockStat || p instanceof BlockExpr) ) p = p.parent;
			if( p == null ) {
				Kiev.reportWarning(pos,"Can't find scope for var "+this);
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void cleanup() {
		parent = null;
		name   = null;
		vtype  = null;
		init = null;
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
	public boolean		guarded = false;
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

	public static ScopeNodeInfo getNodeInThisScope(ASTNode v) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = getNodeInfo(v);
		if( sni != null ) {
			if( sni.state != state ) {
				trace( Kiev.debugNodeTypes, "types: clone "+sni+" to current scope "+getDepth());
				sni = (ScopeNodeInfo)sni.clone();
				sni.state = state;
				state.append(sni);
			} else {
				trace( Kiev.debugNodeTypes, "types: existed "+sni+" in current scope "+getDepth());
			}
		} else {
			sni = new ScopeNodeInfo(v,state);
			state.append(sni);
			trace( Kiev.debugNodeTypes, "types: add "+sni+" to current scope "+getDepth());
		}
		return sni;
	}

	public static ScopeNodeInfo setNodeType(ASTNode v, Type type) {
		return setNodeTypes(v,new Type[]{type});
	}

	public static ScopeNodeInfo setNodeTypes(ASTNode v, Type[] types) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = getNodeInThisScope(v);

		sni.types = types;
		trace( Kiev.debugNodeTypes, "types: set types to "+sni);
		return sni;
	}

	public static ScopeNodeInfo setNodeValue(ASTNode v, Expr expr) {
		Type tp = expr.getType();
		ScopeNodeInfo sni = getNodeInThisScope(v);

		sni.initialized = true;
		if( expr instanceof ConstExpr )
			sni.value = (ConstExpr)expr;
		else
			sni.value = null;
		if( expr.getType() != Type.tpNull )
			sni.types = expr.getAccessTypes();
		trace( Kiev.debugNodeTypes, "types: set expr "+expr+" for node "+sni);
		return sni;
	}

	public static ScopeNodeInfo setNodeInitialized(ASTNode v, boolean initialized) {
		ScopeNodeInfo sni = getNodeInThisScope(v);

		trace( Kiev.debugNodeTypes, "types: set initialized = "+initialized+" for node "+sni.var);
		sni.initialized = initialized;
		return sni;
	}

	static ScopeNodeInfo addInfo(ScopeNodeInfo sni_new) {
		ScopeNodeInfoVector state = states.peek();
		ScopeNodeInfo sni = getNodeInThisScope(sni_new.var);

		sni.types = sni_new.types;
		sni.initialized = sni_new.initialized;
		sni.value = sni_new.value;
		trace( Kiev.debugNodeTypes, "types: set info to "+sni);
		return sni;
	}

	public static ScopeNodeInfoVector pushState() {
		ScopeNodeInfoVector v = new ScopeNodeInfoVector();
		states.push(v);
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

	public static ScopeNodeInfo getNodeInfo(ASTNode v) {
		int i = states.length();
		boolean guarded = false;
		foreach(ScopeNodeInfoVector state; states) {
			foreach(ScopeNodeInfo sni; state; sni.var == v) {
				trace( Kiev.debugNodeTypes, "types: getinfo for node "+sni.var+" is "+sni+" in scope "+ --i);
				if( guarded ) {
					sni = (ScopeNodeInfo)sni.clone();
					sni.types = new Type[]{getDeclType(sni.var)};
					sni.value = null;
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
			if( t1.isInterface() ) newtypes = (Type[])Arrays.append(newtypes,t1);
			else if( newtypes[0].isInterface() ) newtypes = (Type[])Arrays.insert(newtypes,t1,0);
			else newtypes[0] = t1;
		}
		trace( Kiev.debugNodeTypes, "types: add type yeilds "+Arrays.toString(newtypes));
		return newtypes;
	}

	public static Type[] removeAccessType(Type[] types, Type type) {
		if( type == null || type == Type.tpVoid || type == Type.tpNull ) return types;
		if( types == null || !type.isReference() ) return types;
		Type[] newtypes = Type.emptyArray;
	next_type:
		foreach(Type t1; types) {
			if( t1.isInstanceOf(type) ) continue;
			if( t1.isInterface() ) newtypes = (Type[])Arrays.append(newtypes,t1);
			else if( newtypes.length==0 || newtypes[0].isInterface() ) newtypes = (Type[])Arrays.insert(newtypes,t1,0);
			else newtypes[0] = t1;
		}
		if( newtypes.length == 0 )
			throw new RuntimeException("Null type list while removing type "+type+" from "+Arrays.toString(types));
		return newtypes;
	}

	static ScopeNodeInfoVector cleanInfoForVars(ScopeNodeInfoVector nip_state, Var[] vars) {
		foreach(Var v; vars) {
			foreach(ScopeNodeInfo sni; nip_state; sni.var == v) {
				nip_state.removeElement(sni);
				break;
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
			foreach(ScopeNodeInfo sni2; nip_state2; sni2.var == sni1.var ) {
				trace( Kiev.debugNodeTypes, "types: joining "+sni1+" and "+ sni2);
				ScopeNodeInfo sni = new ScopeNodeInfo(sni1.var,nip_state);
				sni.initialized = sni1.initialized && sni2.initialized;
				if( sni1.value != null && sni1.value.equals(sni2.value) ) sni.value = sni1.value;
				if( sni1.types != null && sni2.types != null ) {
					Type[] types = new Type[]{getDeclType(sni1.var)};
					if( sni1.types.length > 0 && sni2.types.length > 0
					 && sni1.types[0].isClazz() && sni2.types[0].isClazz() ) {
						types = addAccessType(types,Type.leastCommonType(sni1.types[0],sni2.types[0]));
					}
					foreach(Type t1; sni1.types; t1 != null && t1 != Type.tpVoid && t1 != Type.tpNull && t1.isInterface() ) {
						foreach(Type t2; sni2.types; t2 == t1 )
							types = addAccessType(types,t1);
					}
					sni.types = types;
				}
				nip_state.append(sni);
			}
		}
		trace( Kiev.debugNodeTypes, "types: joined to "+nip_state);
		return nip_state;
	}

	public static Type getDeclType(ASTNode n) {
		switch(n) {
		case Var: return ((Var)n).type;
		case Field: return ((Field)n).type;
		}
		throw new RuntimeException("Unknown node of type "+n.getClass());
	}

}

public class ScopeNodeInfo implements Cloneable {

	/** Var or Field */
	public ASTNode					var;
	public ScopeNodeInfoVector		state;
	public Type[]					types;
	public boolean					initialized;
	public ConstExpr				value;

	public ScopeNodeInfo(ASTNode var, ScopeNodeInfoVector state ) {
		this.var = var;
		this.state = state;
	}

	public String toString() {
		return "sni:{"+var+","+Arrays.toString(types)+","+initialized+","+value+"}";
	}

	public Object clone() {
		ScopeNodeInfo newsni = (ScopeNodeInfo)super.clone();
		if( newsni.types != null )
			newsni.types = (Type[])newsni.types.clone();
		return newsni;
	}
}


