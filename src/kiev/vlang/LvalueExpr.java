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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Instr.*;
import kiev.vlang.Operator.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
@dflow(out="obj")
public class AccessExpr extends LvalueExpr {

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

	@att
	@dflow(in="this:in")
	public ENode		obj;
	
	@ref
	public Field		var;

	public AccessExpr() {
	}

	public AccessExpr(int pos, ENode obj, Field var) {
		super(pos);
		this.obj = obj;
		this.var = var;
		assert(obj != null && var != null);
	}

	public AccessExpr(int pos, ENode obj, Field var, boolean direct_access) {
		super(pos);
		this.obj = obj;
		this.var = var;
		assert(obj != null && var != null);
		if (direct_access) setAsField(true);
	}

	public AccessExpr(int pos, ENode obj, Field var, int flags) {
		super(pos);
		this.obj = obj;
		this.var = var;
		setFlags(flags);
		assert(obj != null && var != null);
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

	public DNode[] getAccessPath() {
		if (obj instanceof VarAccessExpr) {
			VarAccessExpr va = (VarAccessExpr)obj;
			if (va.var.isFinal() && va.var.isForward())
				return new DNode[]{va.var, this.var};
			return null;
		}
		if (obj instanceof AccessExpr) {
			AccessExpr ae = (AccessExpr)obj;
			if !(ae.var.isFinal() || ae.var.isForward())
				return null;
			DNode[] path = ae.getAccessPath();
			if (path == null)
				return null;
			return (DNode[])Arrays.append(path, var);
		}
		return null;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		try {
			obj.resolve(null);

			// Set violation of the field
			if( PassInfo.method != null
			 && obj instanceof VarAccessExpr && ((VarAccessExpr)obj).var.name.equals(nameThis)
			)
				PassInfo.method.addViolatedField(var);

			setResolved(true);
		} finally { PassInfo.pop(this); }
	}

	public void generateCheckCastIfNeeded() {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf((Struct)var.parent) )
			Code.addInstr(Instr.op_checkcast,((Struct)var.parent).type);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			if( var.isVirtual() && !isAsField() )
				Kiev.reportError(pos, "AccessExpr: Generating virtual field "+var+" directly");
			if( var.isPackedField() )
				Kiev.reportError(pos, "AccessExpr: Generating packed field "+var+" directly");
			Field f = (Field)var;
			var.acc.verifyReadAccess(var);
			obj.generate(null);
			generateCheckCastIfNeeded();
			Code.addInstr(op_getfield,f,obj.getType());
			if( Kiev.verify && f.type.isArgument() && getType().isReference() )
				Code.addInstr(op_checkcast,getType());
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			if( var.isVirtual() && !isAsField() )
				Kiev.reportError(pos, "AccessExpr: Generating virtual field "+var+" directly");
			if( var.isPackedField() )
				Kiev.reportError(pos, "AccessExpr: Generating packed field "+var+" directly");
			Field f = (Field)var;
			var.acc.verifyReadAccess(var);
			obj.generate(null);
			generateCheckCastIfNeeded();
			Code.addInstr(op_dup);
			Code.addInstr(op_getfield,f,obj.getType());
			if( Kiev.verify && f.type.isArgument() && getType().isReference() )
				Code.addInstr(op_checkcast,getType());
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			if( var.isVirtual() && !isAsField() )
				Kiev.reportError(pos, "AccessExpr: Generating virtual field "+var+" directly");
			obj.generate(null);
			generateCheckCastIfNeeded();
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			if( var.isVirtual() && !isAsField() )
				Kiev.reportError(pos, "AccessExpr: Generating virtual field "+var+" directly");
			if( var.isPackedField() )
				Kiev.reportError(pos, "AccessExpr: Generating packed field "+var+" directly");
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_putfield,var,obj.getType());
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			if( var.isVirtual() && !isAsField() )
				Kiev.reportError(pos, "AccessExpr: Generating virtual field "+var+" directly");
			if( var.isPackedField() )
				Kiev.reportError(pos, "AccessExpr: Generating packed field "+var+" directly");
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_dup_x);
			Code.addInstr(op_putfield,var,obj.getType());
		} finally { PassInfo.pop(this); }
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
		var.acc.verifyReadAccess(var);
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
@dflow(out="index")
public class ContainerAccessExpr extends LvalueExpr {

	@att
	@dflow(in="this:in")
	public ENode		obj;
	
	@att
	@dflow(in="obj")
	public ENode		index;

	public ContainerAccessExpr() {
	}

	public ContainerAccessExpr(int pos, ENode obj, ENode index) {
		super(pos);
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
				ASTNode@ v;
				MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
				ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(t,v,info,nameArrayOp,mt) )
					return Type.tpVoid; //throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+t);
				return Type.getRealType(t,((Method)v).type.ret);
			}
		} catch(Exception e) {
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type t = obj.getType();
		if( t.isArray() ) {
			return new Type[]{Type.getRealType(t,t.args[0])};
		} else {
			Struct s = t.clazz;
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
		PassInfo.push(this);
		try {
			obj.resolve(null);
			if( !obj.getType().isArray() ) {
				// May be an overloaded '[]' operator, ensure overriding
				Struct s = obj.getType().clazz;
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
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			if( obj.getType().isArray() ) {
				obj.generate(null);
				index.generate(null);
				Code.addInstr(Instr.op_arr_load);
			} else {
				// Resolve overloaded access method
				ASTNode@ v;
				MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
				ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt));
				obj.generate(null);
				index.generate(null);
				Method func = (Method)v;
				Code.addInstr(Instr.op_call,func,false,obj.getType());
				if( Kiev.verify
				 && func.type.ret.isReference()
				 && ( !getType().isStructInstanceOf(func.type.ret.clazz) || getType().isArray() ) )
				 	Code.addInstr(op_checkcast,getType());
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			if( obj.getType().isArray() ) {
				obj.generate(null);
				index.generate(null);
				Code.addInstr(op_dup2);
				Code.addInstr(Instr.op_arr_load);
			} else {
				throw new CompilerException(pos,"Too complex expression for overloaded operator '[]'");
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			obj.generate(null);
			index.generate(null);
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			Type objType = obj.getType();
			if( objType.isArray() ) {
				Code.addInstr(Instr.op_arr_store);
			} else {
				// Resolve overloaded set method
				ASTNode@ v;
				// We need to get the type of object in stack
				Type t = Code.stack_at(0);
				Expr o = new VarAccessExpr(pos,new Var(pos,KString.Empty,t,0));
				Struct s = objType.clazz;
				MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
				ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(objType,v,info,nameArrayOp,mt) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+objType);
				Code.addInstr(Instr.op_call,(Method)v,false,objType);
				// Pop return value
				Code.addInstr(Instr.op_pop);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			if( obj.getType().isArray() ) {
				Code.addInstr(op_dup_x2);
				Code.addInstr(Instr.op_arr_store);
			} else {
				// Resolve overloaded set method
				ASTNode@ v;
				// We need to get the type of object in stack
				Type t = Code.stack_at(0);
				if( !(Code.stack_at(1).isIntegerInCode() || Code.stack_at(0).isReference()) )
					throw new CompilerException(pos,"Index of '[]' can't be of type double or long");
				Expr o = new VarAccessExpr(pos,new Var(pos,KString.Empty,t,0));
				Struct s = obj.getType().clazz;
				MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
				ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
				if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,mt));
				// The method must return the value to duplicate
				Method func = (Method)v;
				Code.addInstr(Instr.op_call,func,false,obj.getType());
				if( Kiev.verify
				 && func.type.ret.isReference()
				 && ( !getType().isStructInstanceOf(func.type.ret.clazz) || getType().isArray() ) )
				 	Code.addInstr(op_checkcast,getType());
			}
		} finally { PassInfo.pop(this); }
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
@dflow(out="this:in")
public class ThisExpr extends LvalueExpr {

	public boolean super_flag;
	
	public ThisExpr() {
	}
	public ThisExpr(int pos) {
		super(pos);
	}
	public ThisExpr(boolean super_flag) {
		this.super_flag = super_flag;
	}

	public String toString() { return super_flag ? "super" : "this"; }

	private Var getVar() {
		if (PassInfo.method == null)
			return null;
		if (PassInfo.method.isStatic())
			return null;
		return PassInfo.method.getThisPar();
	}
	
	public Type getType() {
		try {
			if (PassInfo.clazz == null)
				return Type.tpVoid;
			if (PassInfo.clazz.name.short_name.equals(nameIdefault))
				return PassInfo.clazz.package_clazz.type;
			if (super_flag)
				PassInfo.clazz.type.getSuperType();
			return PassInfo.clazz.type;
		} catch(Exception e) {
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			if (PassInfo.method != null &&
				PassInfo.method.isStatic() &&
				!PassInfo.clazz.name.short_name.equals(nameIdefault)
			)
				Kiev.reportError(pos,"Access '"+toString()+"' in static context");
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
		PassInfo.push(this);
		try {
			if (!PassInfo.method.isStatic())
				Code.addInstr(op_load,PassInfo.method.getThisPar());
			else if (PassInfo.method.isStatic() && PassInfo.method.isVirtualStatic())
				Code.addInstr(op_load,PassInfo.method.params[0]);
			else {
				Kiev.reportError(pos,"Access '"+toString()+"' in static context");
				Code.addNullConst();
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			if (!PassInfo.method.isStatic())
				Code.addInstr(op_load,PassInfo.method.getThisPar());
			else if (PassInfo.method.isStatic() && PassInfo.method.isVirtualStatic())
				Code.addInstr(op_load,PassInfo.method.params[0]);
			else {
				Kiev.reportError(pos,"Access '"+toString()+"' in static context");
				Code.addNullConst();
			}
			Code.addInstr(op_dup);
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - access only: "+this);
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store only: "+this);
		PassInfo.push(this);
		try {
			if (!PassInfo.method.isStatic())
				Code.addInstr(op_store,PassInfo.method.getThisPar());
			else if (PassInfo.method.isStatic() && PassInfo.method.isVirtualStatic())
				Code.addInstr(op_store,PassInfo.method.params[0]);
			else {
				Kiev.reportError(pos,"Access '"+toString()+"' in static context");
				Code.addInstr(op_pop);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_dup);
			if (!PassInfo.method.isStatic())
				Code.addInstr(op_store,PassInfo.method.getThisPar());
			else if (PassInfo.method.isStatic() && PassInfo.method.isVirtualStatic())
				Code.addInstr(op_store,PassInfo.method.params[0]);
			else {
				Kiev.reportError(pos,"Access '"+toString()+"' in static context");
				Code.addInstr(op_pop);
			}
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(toString()).space();
	}
}

@node
@dflow(out="this:in")
public class VarAccessExpr extends LvalueExpr {

	@ref public Var		var;

	public VarAccessExpr() {
	}
	public VarAccessExpr(int pos, Var var) {
		super(pos);
		if( var == null )
			throw new RuntimeException("Null var");
		this.var = var;
	}

	public String toString() { return var.toString(); }

	public Type getType() {
		try {
			return var.type;
		} catch(Exception e) {
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new DNode[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.type};
		return (Type[])sni.getTypes().clone();
	}

	public void resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		try {
			// Check if we try to access this var from local inner/anonymouse class
			if( PassInfo.clazz.isLocal() ) {
				ASTNode p = var.parent;
				while( !(p instanceof Struct) ) p = p.parent;
				if( p != PassInfo.clazz ) {
					var.setNeedProxy(true);
					setAsField(true);
					// Now we need to add this var as a fields to
					// local class and to initializer of this class
					Field vf;
					if( (vf = (Field)PassInfo.clazz.resolveName(var.name.name)) == null ) {
						// Add field
						vf = PassInfo.clazz.addField(new Field(var.name.name,var.type,ACC_PUBLIC));
						vf.setNeedProxy(true);
						vf.init = (Expr)this.copy();
					}
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public Field resolveProxyVar() {
		Field proxy_var =
			(Field)PassInfo.clazz.resolveName(var.name.name);
		if( proxy_var == null && PassInfo.method.isStatic() && !PassInfo.method.isVirtualStatic() )
			throw new CompilerException(pos,"Proxyed var cannot be referenced from static context");
		return proxy_var;
	}

	public Field resolveVarVal() {
		Type prt = Type.getProxyType(var.type);
		Field var_valf =
			(Field)prt.resolveName(nameCellVal);
		return var_valf;
	}

	public void resolveVarForConditions() {
		if( Code.cond_generation ) {
			// Bind the correct var
			if( var.parent != PassInfo.method ) {
				assert( var.parent instanceof Method, "Non-parametrs var in condition" );
				if( var.name==nameResultVar ) var = PassInfo.method.getRetVar();
				else for(int i=0; i < PassInfo.method.params.length; i++) {
					Var v = PassInfo.method.params[i];
					if( !v.name.equals(var.name) ) continue;
					assert( var.type.equals(v.type), "Type of vars in overriden methods missmatch" );
					var = v;
					break;
				}
				trace(Kiev.debugStatGen,"Var "+var+" substituted for condition");
			}
			assert( var.parent == PassInfo.method, "Can't find var for condition" );
//			assert( var.name==nameResultVar && var == PassInfo.method.getRetVar()
//			 || var == PassInfo.method.params[var.getBCpos()], "Missplaced var "+var );
		}
	}

	public void generateVerifyCheckCast() {
		if( !Kiev.verify ) return;
		if( !var.type.isReference() || var.type.isArray() ) return;
		Type chtp = null;
		if( var.parent instanceof Method ) {
			Method m = (Method)var.parent;
			for(int i=0; i < m.params.length; i++) {
				if( var == m.params[i] ) {
//					if( m.isStatic() ) chtp = m.jtype.args[i];
//					else chtp = m.jtype.args[i-1];
					chtp = m.type.args[i];
					break;
				}
			}
		}
		if( chtp == null )
			chtp = var.type.getJavaType();
		if( !var.type.isStructInstanceOf(chtp.getStruct()) ) {
			Code.addInstr(op_checkcast,var.type);
			return;
		}
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			if( Code.cond_generation ) resolveVarForConditions();
			if( !var.isNeedProxy() || isUseNoProxy() ) {
				if( Code.vars[var.getBCpos()] == null )
					throw new CompilerException(pos,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
				Code.addInstr(op_load,var);
			} else {
				if( isAsField() ) {
					Code.addInstr(op_load,PassInfo.method.getThisPar());
					Code.addInstr(op_getfield,resolveProxyVar(),PassInfo.clazz.type);
				} else {
					Code.addInstr(op_load,var);
				}
				if( var.isNeedRefProxy() ) {
					Code.addInstr(op_getfield,resolveVarVal(),PassInfo.clazz.type);
				}
			}
			generateVerifyCheckCast();
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			if( Code.cond_generation ) resolveVarForConditions();
			if( !var.isNeedProxy() || isUseNoProxy() ) {
				if( Code.vars[var.getBCpos()] == null )
					throw new CompilerException(pos,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
				Code.addInstr(op_load,var);
			} else {
				if( isAsField() ) {
					Code.addInstr(op_load,PassInfo.method.getThisPar());
					if( var.isNeedRefProxy() ) {
						Code.addInstr(op_getfield,resolveProxyVar(),PassInfo.clazz.type);
						Code.addInstr(op_dup);
					} else {
						Code.addInstr(op_dup);
						Code.addInstr(op_getfield,resolveProxyVar(),PassInfo.clazz.type);
					}
				} else {
					Code.addInstr(op_load,var);
					Code.addInstr(op_dup);
				}
				if( var.isNeedRefProxy() ) {
					Code.addInstr(op_getfield,resolveVarVal(),resolveProxyVar().getType());
				}
			}
			generateVerifyCheckCast();
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			if( Code.cond_generation ) resolveVarForConditions();
			if( !var.isNeedProxy() || isUseNoProxy() ) {
			} else {
				if( isAsField() ) {
					Code.addInstr(op_load,PassInfo.method.getThisPar());
					if( var.isNeedRefProxy() ) {
						Code.addInstr(op_getfield,resolveProxyVar(),PassInfo.clazz.type);
						Code.addInstr(op_dup);
					} else {
						Code.addInstr(op_dup);
					}
				} else {
					if( var.isNeedRefProxy() )
						Code.addInstr(op_load,var);
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			if( Code.cond_generation ) resolveVarForConditions();
			if( !var.isNeedProxy() || isUseNoProxy() ) {
				if( Code.vars[var.getBCpos()] == null )
					throw new CompilerException(pos,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
				Code.addInstr(op_store,var);
			} else {
				if( isAsField() ) {
					if( !var.isNeedRefProxy() ) {
						Code.addInstr(op_putfield,resolveProxyVar(),PassInfo.clazz.type);
					} else {
						Code.addInstr(op_putfield,resolveVarVal(),PassInfo.clazz.type);
					}
				} else {
					if( !var.isNeedRefProxy() ) {
						Code.addInstr(op_store,var);
					} else {
						Code.addInstr(op_putfield,resolveVarVal(),PassInfo.clazz.type);
					}
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			if( Code.cond_generation ) resolveVarForConditions();
			if( !var.isNeedProxy() || isUseNoProxy() ) {
				if( Code.vars[var.getBCpos()] == null )
					throw new CompilerException(pos,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
				Code.addInstr(op_dup);
				Code.addInstr(op_store,var);
			} else {
				if( isAsField() ) {
					Code.addInstr(op_dup_x);
					if( !var.isNeedRefProxy() ) {
						Code.addInstr(op_putfield,resolveProxyVar(),PassInfo.clazz.type);
					} else {
						Code.addInstr(op_putfield,resolveVarVal(),PassInfo.clazz.type);
					}
				} else {
					if( !var.isNeedRefProxy() ) {
						Code.addInstr(op_dup);
						Code.addInstr(op_store,var);
					} else {
						Code.addInstr(op_dup_x);
						Code.addInstr(op_putfield,resolveVarVal(),PassInfo.clazz.type);
					}
				}
			}
			generateVerifyCheckCast();
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		if( var.isNeedRefProxy() )
			dmp.append(".val");
		return dmp.space();
	}
}

@node
@dflow(out="this:in")
public class LocalPrologVarAccessExpr extends LvalueExpr {

	static final KString namePEnv = KString.from("$env");
	
	@ref public Var		var;

	public LocalPrologVarAccessExpr() {
	}
	
	public LocalPrologVarAccessExpr(int pos, Var var) {
		super(pos);
		this.var = var;
		RuleMethod rm = (RuleMethod)PassInfo.method;
		int i = 0;
		for(; i < rm.localvars.length; i++)
			if( rm.localvars[i].name.equals(var.name) ) break;
		if( i >= rm.localvars.length )
			throw new CompilerException(pos,"Local prolog var "+var+" not found in "+rm);
	}

	public String toString() {
		return "$env."+var.name;
	}

	public Type getType() {
		try {
			return var.type;
//			return Rule.getTypeOfVar(var);
		} catch(Exception e) {
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public void resolve(Type reqType) {
		setResolved(true);
	}

	public Field resolveFieldForLocalPrologVar() {
		RuleMethod rm = (RuleMethod)PassInfo.method;
		Struct s = ((LocalStructDecl)((BlockStat)rm.body).stats[0]).clazz;
		Field f = s.resolveField(var.name.name);
		assert(f != null);
		return f;
	}

	public Var resolveFrameForLocalPrologVar() {
		RuleMethod rm = (RuleMethod)PassInfo.method;
		assert(rm.params[0].type == Type.tpRule);
		foreach (ENode n; rm.body.stats; n instanceof VarDecl) {
			VarDecl vd = (VarDecl)n;
			if (vd.var.name.equals(namePEnv)) {
				assert(vd.var.type.isInstanceOf(Type.tpRule));
				return vd.var;
			}
		}
		Kiev.reportError(pos, "Cannot find "+namePEnv);
		return rm.params[0];
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating LocalPrologVarAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,resolveFrameForLocalPrologVar());
			Code.addInstr(op_getfield,resolveFieldForLocalPrologVar(),var.getType());
			if( Kiev.verify && !var.type.equals(Type.tpObject) )
				Code.addInstr(Instr.op_checkcast,var.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating LocalPrologVarAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,resolveFrameForLocalPrologVar());
			Code.addInstr(op_dup);
			Code.addInstr(op_getfield,resolveFieldForLocalPrologVar(),var.getType());
			if( Kiev.verify && !var.type.equals(Type.tpObject) )
				Code.addInstr(Instr.op_checkcast,var.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,resolveFrameForLocalPrologVar());
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			if( Kiev.verify && !var.type.equals(Type.tpObject) )
				Code.addInstr(Instr.op_checkcast,var.type);
			Code.addInstr(op_putfield,resolveFieldForLocalPrologVar(),var.getType());
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarAccessExpr - store & dup: "+this);
		try {
			if( Kiev.verify && !var.type.equals(Type.tpObject) )
				Code.addInstr(Instr.op_checkcast,var.type);
			Code.addInstr(op_dup_x);
			Code.addInstr(op_putfield,resolveFieldForLocalPrologVar(),var.getType());
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append("$env.").append(var.name.name);
		return dmp.space();
	}
}

@node
@dflow(out="this:in")
public class StaticFieldAccessExpr extends LvalueExpr {

	@ref public Field		var;

	public StaticFieldAccessExpr() {
	}

	public StaticFieldAccessExpr(int pos, Field var) {
		super(pos);
		this.var = var;
	}

	public StaticFieldAccessExpr(int pos, Field var, boolean direct_access) {
		super(pos);
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
		var.acc.verifyReadAccess(var);
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
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new DNode[]{var});
		if( sni == null || sni.getTypes().length == 0 )
			types = new Type[]{var.type};
		else
			types = (Type[])sni.getTypes().clone();
		return types;
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			// Set violation of the field
			if( PassInfo.method != null /*&& PassInfo.method.isInvariantMethod()*/ )
				PassInfo.method.addViolatedField(var);
		} finally { PassInfo.pop(this); }

		setResolved(true);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyReadAccess(var);
			Code.addInstr(op_getstatic,var,PassInfo.clazz.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyReadAccess(var);
			Code.addInstr(op_getstatic,var,PassInfo.clazz.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - access only: "+this);
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_putstatic,var,PassInfo.clazz.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_dup);
			Code.addInstr(op_putstatic,var,PassInfo.clazz.type);
		} finally { PassInfo.pop(this); }
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) {
		Struct cl = (Struct)var.parent;
		ClazzName cln = cl.type.getClazzName();
		return dmp.space().append(cln).append('.').append(var.name).space();
	}

}

@node
@dflow(out="this:in")
public class OuterThisAccessExpr extends LvalueExpr {

	@ref public Struct		outer;
	public Field[]			outer_refs = Field.emptyArray;

	public OuterThisAccessExpr() {
	}

	public OuterThisAccessExpr(int pos, Struct outer) {
		super(pos);
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
		PassInfo.push(this);
		try {
			outer_refs = Field.emptyArray;
			trace(Kiev.debugResolve,"Resolving "+this);
			Field ou_ref = outerOf(PassInfo.clazz);
			if( ou_ref == null )
				throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+PassInfo.clazz);
			do {
				trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
				outer_refs = (Field[])Arrays.append(outer_refs,ou_ref);
				if( ou_ref.type.isInstanceOf(outer.type) ) break;
				ou_ref = outerOf(ou_ref.type.getStruct());
			} while( ou_ref!=null );
			if( !outer_refs[outer_refs.length-1].type.isInstanceOf(outer.type) )
				throw new RuntimeException("Outer class "+outer+" not found for inner class "+PassInfo.clazz);
			if( Kiev.debugResolve ) {
				StringBuffer sb = new StringBuffer("Outer 'this' resolved as this");
				for(int i=0; i < outer_refs.length; i++)
					sb.append("->").append(outer_refs[i].name);
				System.out.println(sb.toString());
			}
			if( PassInfo.method.isStatic() && !PassInfo.method.isVirtualStatic() ) {
				throw new RuntimeException("Access to 'this' in static method "+PassInfo.method);
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,PassInfo.method.getThisPar());
			for(int i=0; i < outer_refs.length; i++)
				Code.addInstr(op_getfield,outer_refs[i],PassInfo.clazz.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,PassInfo.method.getThisPar());
			for(int i=0; i < outer_refs.length; i++) {
				if( i == outer_refs.length-1 ) Code.addInstr(op_dup);
				Code.addInstr(op_getfield,outer_refs[i],PassInfo.clazz.type);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,PassInfo.method.getThisPar());
			for(int i=0; i < outer_refs.length-1; i++) {
				Code.addInstr(op_getfield,outer_refs[i],PassInfo.clazz.type);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - store only: "+this);
		Code.addInstr(op_putfield,outer_refs[outer_refs.length-1],PassInfo.clazz.type);
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating SimpleAccessExpr - store & dup: "+this);
		Code.addInstr(op_dup_x);
		Code.addInstr(op_putfield,outer_refs[outer_refs.length-1],PassInfo.clazz.type);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}


