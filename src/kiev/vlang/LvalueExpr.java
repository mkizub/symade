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
import kiev.transf.*;
import kiev.parser.*;
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
		if (obj instanceof VarExpr) {
			VarExpr va = (VarExpr)obj;
			if (va.getVar().isFinal() && va.getVar().isForward())
				return new DNode[]{va.getVar(), this.var};
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
		obj.resolve(null);

		// Set violation of the field
		if( pctx.method != null
		 && obj instanceof VarExpr && ((VarExpr)obj).name.equals(nameThis)
		)
			pctx.method.addViolatedField(var);

		setResolved(true);
	}

	public void generateCheckCastIfNeeded() {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf((Struct)var.parent) )
			Code.addInstr(Instr.op_checkcast,((Struct)var.parent).type);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - load only: "+this);
		Code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "AccessExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "AccessExpr: Generating packed field "+var+" directly");
		Field f = (Field)var;
		var.acc.verifyReadAccess(this,var);
		obj.generate(null);
		generateCheckCastIfNeeded();
		Code.addInstr(op_getfield,f,obj.getType());
		if( Kiev.verify && f.type.isArgument() && getType().isReference() )
			Code.addInstr(op_checkcast,getType());
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - load & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "AccessExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "AccessExpr: Generating packed field "+var+" directly");
		Field f = (Field)var;
		var.acc.verifyReadAccess(this,var);
		obj.generate(null);
		generateCheckCastIfNeeded();
		Code.addInstr(op_dup);
		Code.addInstr(op_getfield,f,obj.getType());
		if( Kiev.verify && f.type.isArgument() && getType().isReference() )
			Code.addInstr(op_checkcast,getType());
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - access only: "+this);
		Code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "AccessExpr: Generating virtual field "+var+" directly");
		obj.generate(null);
		generateCheckCastIfNeeded();
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - store only: "+this);
		Code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "AccessExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "AccessExpr: Generating packed field "+var+" directly");
		var.acc.verifyWriteAccess(this,var);
		Code.addInstr(op_putfield,var,obj.getType());
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - store & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "AccessExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "AccessExpr: Generating packed field "+var+" directly");
		var.acc.verifyWriteAccess(this,var);
		Code.addInstr(op_dup_x);
		Code.addInstr(op_putfield,var,obj.getType());
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
		setResolved(true);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load only: "+this);
		Code.setLinePos(this.getPosLine());
		if( obj.getType().isArray() ) {
			obj.generate(null);
			index.generate(null);
			Code.addInstr(Instr.op_arr_load);
		} else {
			// Resolve overloaded access method
			ASTNode@ v;
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt));
			obj.generate(null);
			index.generate(null);
			Method func = (Method)v;
			Code.addInstr(Instr.op_call,func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret.isReference()
			 && ( !getType().isStructInstanceOf(func.type.ret.clazz) || getType().isArray() ) )
				Code.addInstr(op_checkcast,getType());
		}
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if( obj.getType().isArray() ) {
			obj.generate(null);
			index.generate(null);
			Code.addInstr(op_dup2);
			Code.addInstr(Instr.op_arr_load);
		} else {
			throw new CompilerException(this,"Too complex expression for overloaded operator '[]'");
		}
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - access only: "+this);
		Code.setLinePos(this.getPosLine());
		obj.generate(null);
		index.generate(null);
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store only: "+this);
		Code.setLinePos(this.getPosLine());
		Type objType = obj.getType();
		if( objType.isArray() ) {
			Code.addInstr(Instr.op_arr_store);
		} else {
			// Resolve overloaded set method
			ASTNode@ v;
			// We need to get the type of object in stack
			Type t = Code.stack_at(0);
			Expr o = new VarExpr(pos,new Var(pos,KString.Empty,t,0));
			Struct s = objType.clazz;
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(objType,v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+objType);
			Code.addInstr(Instr.op_call,(Method)v,false,objType);
			// Pop return value
			Code.addInstr(Instr.op_pop);
		}
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if( obj.getType().isArray() ) {
			Code.addInstr(op_dup_x2);
			Code.addInstr(Instr.op_arr_store);
		} else {
			// Resolve overloaded set method
			ASTNode@ v;
			// We need to get the type of object in stack
			Type t = Code.stack_at(0);
			if( !(Code.stack_at(1).isIntegerInCode() || Code.stack_at(0).isReference()) )
				throw new CompilerException(this,"Index of '[]' can't be of type double or long");
			Expr o = new VarExpr(pos,new Var(pos,KString.Empty,t,0));
			Struct s = obj.getType().clazz;
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt));
			// The method must return the value to duplicate
			Method func = (Method)v;
			Code.addInstr(Instr.op_call,func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret.isReference()
			 && ( !getType().isStructInstanceOf(func.type.ret.clazz) || getType().isArray() ) )
				Code.addInstr(op_checkcast,getType());
		}
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

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
		Code.setLinePos(this.getPosLine());
		if (!Code.method.isStatic())
			Code.addInstr(op_load,Code.method.getThisPar());
		else if (Code.method.isStatic() && Code.method.isVirtualStatic())
			Code.addInstr(op_load,Code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			Code.addNullConst();
		}
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if (!Code.method.isStatic())
			Code.addInstr(op_load,Code.method.getThisPar());
		else if (Code.method.isStatic() && Code.method.isVirtualStatic())
			Code.addInstr(op_load,Code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			Code.addNullConst();
		}
		Code.addInstr(op_dup);
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - access only: "+this);
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store only: "+this);
		Code.setLinePos(this.getPosLine());
		if (!Code.method.isStatic())
			Code.addInstr(op_store,Code.method.getThisPar());
		else if (Code.method.isStatic() && Code.method.isVirtualStatic())
			Code.addInstr(op_store,Code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			Code.addInstr(op_pop);
		}
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store & dup: "+this);
		Code.setLinePos(this.getPosLine());
		Code.addInstr(op_dup);
		if (!Code.method.isStatic())
			Code.addInstr(op_store,Code.method.getThisPar());
		else if (Code.method.isStatic() && Code.method.isVirtualStatic())
			Code.addInstr(op_store,Code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			Code.addInstr(op_pop);
		}
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(toString()).space();
	}
}

@node
@dflow(out="this:in")
public class VarExpr extends LvalueExpr {

	static final KString namePEnv = KString.from("$env");

	@att public KString		name;
	@ref private Var		var;

	public VarExpr() {
	}
	public VarExpr(int pos, Var var) {
		super(pos);
		this.var = var;
		this.name = var.name.name;
	}
	public VarExpr(int pos, KString name) {
		super(pos);
		this.name = name;
	}
	public VarExpr(KString name) {
		this.name = name;
	}

	public void set(Token t) {
		if (t.image.startsWith("ID#"))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-1));
		else
			this.name = KString.from(t.image);
        pos = t.getPos();
	}
	
	public String toString() {
		if (var == null)
			return name.toString();
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
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new DNode[]{getVar()});
		if( sni == null || sni.getTypes().length == 0 )
			return new Type[]{var.type};
		return (Type[])sni.getTypes().clone();
	}

	public Var getVar() {
		if (var != null)
			return var;
		ASTNode@ v;
		ResInfo info = new ResInfo(this);
		if( !PassInfo.resolveNameR(this,v,info,name) )
			throw new CompilerException(this,"Unresolved var "+name);
		if !(v instanceof Var)
			throw new CompilerException(this,"Expected "+name+" to be a var");
		var = (Var)v;
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
			Field f = s.resolveField(name);
			assert(f != null);
			replaceWithNode(new AccessExpr(pos, new VarExpr(pos, pEnv), f));
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
				if( (vf = (Field)pctx.clazz.resolveName(name)) == null ) {
					// Add field
					vf = pctx.clazz.addField(new Field(name,var.type,ACC_PUBLIC));
					vf.setNeedProxy(true);
					vf.init = (Expr)this.copy();
				}
			}
		}
		setResolved(true);
	}

	public Field resolveProxyVar() {
		Field proxy_var = (Field)Code.clazz.resolveName(name);
		if( proxy_var == null && Code.method.isStatic() && !Code.method.isVirtualStatic() )
			throw new CompilerException(this,"Proxyed var cannot be referenced from static context");
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
			if( getVar().parent != Code.method ) {
				assert( var.parent instanceof Method, "Non-parametrs var in condition" );
				if( name==nameResultVar ) var = Code.method.getRetVar();
				else for(int i=0; i < Code.method.params.length; i++) {
					Var v = Code.method.params[i];
					if( !v.name.equals(var.name) ) continue;
					assert( var.type.equals(v.type), "Type of vars in overriden methods missmatch" );
					var = v;
					break;
				}
				trace(Kiev.debugStatGen,"Var "+var+" substituted for condition");
			}
			assert( var.parent == Code.method, "Can't find var for condition" );
//			assert( var.name==nameResultVar && var == Code.method.getRetVar()
//			 || var == Code.method.params[var.getBCpos()], "Missplaced var "+var );
		}
	}

	public void generateVerifyCheckCast() {
		if( !Kiev.verify ) return;
		if( !var.type.isReference() || var.type.isArray() ) return;
		Type chtp = null;
		if( getVar().parent instanceof Method ) {
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
		trace(Kiev.debugStatGen,"\t\tgenerating VarExpr - load only: "+this);
		Code.setLinePos(this.getPosLine());
		if( Code.cond_generation ) resolveVarForConditions();
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( Code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
			Code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				Code.addInstr(op_load,Code.method.getThisPar());
				Code.addInstr(op_getfield,resolveProxyVar(),Code.clazz.type);
			} else {
				Code.addInstr(op_load,var);
			}
			if( var.isNeedRefProxy() ) {
				Code.addInstr(op_getfield,resolveVarVal(),Code.clazz.type);
			}
		}
		generateVerifyCheckCast();
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarExpr - load & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if( Code.cond_generation ) resolveVarForConditions();
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( Code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
			Code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				Code.addInstr(op_load,Code.method.getThisPar());
				if( var.isNeedRefProxy() ) {
					Code.addInstr(op_getfield,resolveProxyVar(),Code.clazz.type);
					Code.addInstr(op_dup);
				} else {
					Code.addInstr(op_dup);
					Code.addInstr(op_getfield,resolveProxyVar(),Code.clazz.type);
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
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarExpr - access only: "+this);
		Code.setLinePos(this.getPosLine());
		if( Code.cond_generation ) resolveVarForConditions();
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
		} else {
			if( isAsField() ) {
				Code.addInstr(op_load,Code.method.getThisPar());
				if( var.isNeedRefProxy() ) {
					Code.addInstr(op_getfield,resolveProxyVar(),Code.clazz.type);
					Code.addInstr(op_dup);
				} else {
					Code.addInstr(op_dup);
				}
			} else {
				if( var.isNeedRefProxy() )
					Code.addInstr(op_load,var);
			}
		}
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarExpr - store only: "+this);
		Code.setLinePos(this.getPosLine());
		if( Code.cond_generation ) resolveVarForConditions();
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( Code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
			Code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				if( !var.isNeedRefProxy() ) {
					Code.addInstr(op_putfield,resolveProxyVar(),Code.clazz.type);
				} else {
					Code.addInstr(op_putfield,resolveVarVal(),Code.clazz.type);
				}
			} else {
				if( !var.isNeedRefProxy() ) {
					Code.addInstr(op_store,var);
				} else {
					Code.addInstr(op_putfield,resolveVarVal(),Code.clazz.type);
				}
			}
		}
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating VarExpr - store & dup: "+this);
		Code.setLinePos(this.getPosLine());
		if( Code.cond_generation ) resolveVarForConditions();
		if( !getVar().isNeedProxy() || isUseNoProxy() ) {
			if( Code.vars[var.getBCpos()] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.getBCpos()+" but Code.var["+var.getBCpos()+"] == null");
			Code.addInstr(op_dup);
			Code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				Code.addInstr(op_dup_x);
				if( !var.isNeedRefProxy() ) {
					Code.addInstr(op_putfield,resolveProxyVar(),Code.clazz.type);
				} else {
					Code.addInstr(op_putfield,resolveVarVal(),Code.clazz.type);
				}
			} else {
				if( !var.isNeedRefProxy() ) {
					Code.addInstr(op_dup);
					Code.addInstr(op_store,var);
				} else {
					Code.addInstr(op_dup_x);
					Code.addInstr(op_putfield,resolveVarVal(),Code.clazz.type);
				}
			}
		}
		generateVerifyCheckCast();
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
		ScopeNodeInfo sni = getDFlow().out().getNodeInfo(new DNode[]{var});
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

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - load only: "+this);
		Code.setLinePos(this.getPosLine());
		var.acc.verifyReadAccess(this,var);
		Code.addInstr(op_getstatic,var,Code.clazz.type);
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - load & dup: "+this);
		Code.setLinePos(this.getPosLine());
		var.acc.verifyReadAccess(this,var);
		Code.addInstr(op_getstatic,var,Code.clazz.type);
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - access only: "+this);
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - store only: "+this);
		Code.setLinePos(this.getPosLine());
		var.acc.verifyWriteAccess(this,var);
		Code.addInstr(op_putstatic,var,Code.clazz.type);
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - store & dup: "+this);
		Code.setLinePos(this.getPosLine());
		var.acc.verifyWriteAccess(this,var);
		Code.addInstr(op_dup);
		Code.addInstr(op_putstatic,var,Code.clazz.type);
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
		outer_refs = Field.emptyArray;
		trace(Kiev.debugResolve,"Resolving "+this);
		Field ou_ref = outerOf(pctx.clazz);
		if( ou_ref == null )
			throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+pctx.clazz);
		do {
			trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
			outer_refs = (Field[])Arrays.append(outer_refs,ou_ref);
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

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load only: "+this);
		Code.setLinePos(this.getPosLine());
		Code.addInstr(op_load,Code.method.getThisPar());
		for(int i=0; i < outer_refs.length; i++)
			Code.addInstr(op_getfield,outer_refs[i],Code.clazz.type);
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load & dup: "+this);
		Code.setLinePos(this.getPosLine());
		Code.addInstr(op_load,Code.method.getThisPar());
		for(int i=0; i < outer_refs.length; i++) {
			if( i == outer_refs.length-1 ) Code.addInstr(op_dup);
			Code.addInstr(op_getfield,outer_refs[i],Code.clazz.type);
		}
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - access only: "+this);
		Code.setLinePos(this.getPosLine());
		Code.addInstr(op_load,Code.method.getThisPar());
		for(int i=0; i < outer_refs.length-1; i++) {
			Code.addInstr(op_getfield,outer_refs[i],Code.clazz.type);
		}
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - store only: "+this);
		Code.addInstr(op_putfield,outer_refs[outer_refs.length-1],Code.clazz.type);
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating SimpleAccessExpr - store & dup: "+this);
		Code.addInstr(op_dup_x);
		Code.addInstr(op_putfield,outer_refs[outer_refs.length-1],Code.clazz.type);
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}


