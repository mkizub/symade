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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/LvalueExpr.java,v 1.6.2.1.2.5 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.5 $
 *
 */

public class AccessExpr extends LvalueExpr {

	public Expr		obj;
	public Field	var;
	public Method	fset;		// for virtual fields
	public Method	fget;		// for virtual fields

	public AccessExpr(int pos, Expr obj, Field var) {
		super(pos);
		this.obj = obj;
		this.obj.parent = this;
		this.var = var;
		assert(obj != null && var != null);
	}

	public AccessExpr(int pos, ASTNode par, Expr obj, Field var) {
		super(pos,par);
		this.obj = obj;
		this.obj.parent = this;
		this.var = var;
		assert(obj != null && var != null);
	}

	public AccessExpr(int pos, Expr obj, Field var, int flags) {
		super(pos);
		this.obj = obj;
		this.obj.parent = this;
		this.var = var;
		setFlags(flags);
		assert(obj != null && var != null);
	}

	public AccessExpr(int pos, ASTNode par, Expr obj, Field var, int flags) {
		super(pos,par);
		this.obj = obj;
		this.obj.parent = this;
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

	public int getPriority() { return opAccessPriority; }

	public void cleanup() {
		parent=null;
		obj.cleanup();
		obj = null;
		var = null;
		fset = null;
		fget = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;

		PassInfo.push(this);
		try {
			obj = (Expr)obj.resolve(null);
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				KString get_name = new KStringBuffer(nameGet.length()+var.name.name.length()).
					append_fast(nameGet).append_fast(var.name.name).toKString();
				KString set_name = new KStringBuffer(nameSet.length()+var.name.name.length()).
					append_fast(nameSet).append_fast(var.name.name).toKString();

				if( PassInfo.method.name.equals(get_name)
				 || PassInfo.method.name.equals(set_name) ) {
				 	setAsField(true);
				} else {
					// We return get$ method. set$ method must be checked by AssignExpr
					PVar<Method> fsg;
					PassInfo.resolveBestMethodR(((Struct)var.parent),fsg,new ResInfo(),set_name,new Expr[]{this},Type.tpVoid,obj.getType(),ResolveFlags.NoForwards);
					fset = fsg;
					fsg = null;
					PassInfo.resolveBestMethodR(((Struct)var.parent),fsg,new ResInfo(),get_name,Expr.emptyArray,getType(),obj.getType(),ResolveFlags.NoForwards);
					fget = fsg;
				}
			}

			// Set violation of the field
			if( PassInfo.method != null /*&& PassInfo.method.isInvariantMethod()*/
			 && obj instanceof VarAccessExpr && ((VarAccessExpr)obj).var.name.equals(nameThis)
			)
				PassInfo.method.addViolatedField(var);

			setResolved(true);
			Type tp = getType();
			if( !var.getType().equals(tp) ) {
				return new CastExpr(pos,tp,this).resolve(null);
			}
		} finally { PassInfo.pop(this); }
		return this;
	}

	public void generateCheckCastIfNeeded() {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.clazz.instanceOf((Struct)var.parent) )
			Code.addInstr(Instr.op_checkcast,((Struct)var.parent).type);
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			Field f = (Field)var;
			var.acc.verifyReadAccess(var);
			obj.generate(null);
			generateCheckCastIfNeeded();
			if( var.isVirtual() && !isAsField() ) {
				assert(fget != null,"methods get$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fget,false,obj.getType());
			} else {
				if( var.isPackedField() )
					Code.addInstr(op_getfield,var.pack.packer,obj.getType());
				else
					Code.addInstr(op_getfield,f,obj.getType());
			}
			if( Kiev.verify && f.type.clazz.isArgument()
			 && Type.getRealType(Kiev.argtype,getType()).isReference() )
				Code.addInstr(op_checkcast,getType());
			if( var.isPackedField() ) {
				int mask = FieldAccessExpr.masks[var.pack.size];
				mask <<= var.pack.offset;
				Code.addConst(mask);
				Code.addInstr(op_and);
				if(var.pack.offset > 0) {
					Code.addConst(var.pack.offset);
					Code.addInstr(op_ushr);
				if( var.pack.size == 8 && var.type == Type.tpByte )
					Code.addInstr(Instr.op_x2y,Type.tpByte);
				else if( var.pack.size == 16 && var.type == Type.tpShort )
					Code.addInstr(Instr.op_x2y,Type.tpShort);
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			Field f = (Field)var;
			var.acc.verifyReadAccess(var);
			obj.generate(null);
			generateCheckCastIfNeeded();
			Code.addInstr(op_dup);
			if( var.isVirtual() && !isAsField() ) {
				assert(fget != null,"methods get$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fget,false,obj.getType());
			} else {
				if( var.isPackedField() )
					Code.addInstr(op_getfield,var.pack.packer,obj.getType());
				else
					Code.addInstr(op_getfield,f,obj.getType());
			}
			if( Kiev.verify && f.type.clazz.isArgument()
			 && Type.getRealType(Kiev.argtype,getType()).isReference() )
				Code.addInstr(op_checkcast,getType());
			if( var.isPackedField() ) {
				int mask = FieldAccessExpr.masks[var.pack.size];
				mask <<= var.pack.offset;
				Code.addConst(mask);
				Code.addInstr(op_and);
				if(var.pack.offset > 0) {
					Code.addConst(var.pack.offset);
					Code.addInstr(op_ushr);
				}
				if( var.pack.size == 8 && var.type == Type.tpByte )
					Code.addInstr(Instr.op_x2y,Type.tpByte);
				else if( var.pack.size == 16 && var.type == Type.tpShort )
					Code.addInstr(Instr.op_x2y,Type.tpShort);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			obj.generate(null);
			generateCheckCastIfNeeded();
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				assert(fset != null,"methods set$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fset,false,obj.getType());
			} else {
				if( var.isPackedField() ) {
					// Correct value
					int mask = FieldAccessExpr.masks[var.pack.size];
					Code.addConst(mask);
					Code.addInstr(op_and);
					if(var.pack.offset > 0) {
						Code.addConst(var.pack.offset);
						Code.addInstr(op_shl);
					}

					// Load old value of packer field
					Code.addInstr(op_swap);
					Code.addInstr(op_dup_x);
					Code.addInstr(op_getfield,var.pack.packer,obj.getType());
					// Clear var's position
					mask = FieldAccessExpr.masks[var.pack.size];
					mask <<= var.pack.offset;
					mask = ~mask;
					Code.addConst(mask);
					Code.addInstr(op_and);

					// Fill with var's value
					Code.addInstr(op_or);

					// Store packer field
					Code.addInstr(op_putfield,var.pack.packer,obj.getType());
				} else {
					Code.addInstr(op_putfield,var,obj.getType());
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating AccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_dup_x);
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				assert(fset != null,"methods set$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fset,false,obj.getType());
			} else {
				if( var.isPackedField() ) {
					// Correct value
					int mask = FieldAccessExpr.masks[var.pack.size];
					Code.addConst(mask);
					Code.addInstr(op_and);
					if(var.pack.offset > 0) {
						Code.addConst(var.pack.offset);
						Code.addInstr(op_shl);
					}

					// Load old value of packer field
					Code.addInstr(op_swap);
					Code.addInstr(op_dup_x);
					Code.addInstr(op_getfield,var.pack.packer,obj.getType());
					// Clear var's position
					mask = FieldAccessExpr.masks[var.pack.size];
					mask <<= var.pack.offset;
					mask = ~mask;
					Code.addConst(mask);
					Code.addInstr(op_and);

					// Fill with var's value
					Code.addInstr(op_or);

					// Store packer field
					Code.addInstr(op_putfield,var.pack.packer,obj.getType());
				} else {
					Code.addInstr(op_putfield,var,obj.getType());
				}
			}
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
		if( isAsField() ) {
			return dmp.space().append(var.name).space();
		} else {
			dmp.append(var.name);
		}
		return dmp;
	}

	public Dumper toCpp(Dumper dmp) {
		if( obj.getPriority() < opAccessPriority )
			dmp.append('(').append(obj).append(")->");
		else
			dmp.append(obj).append("->");
		if( isAsField() )
			return dmp.space().append(var.name).space();
		else
			dmp.append(var.name);
		return dmp;
	}
}

public class ContainerAccessExpr extends LvalueExpr {

	public Expr		obj;
	public Expr		index;

	public ContainerAccessExpr(int pos, Expr obj, Expr index) {
		super(pos);
		this.obj = obj;
		this.obj.parent = this;
		this.index = index;
		this.index.parent = this;
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
				PVar<ASTNode> v;
				Struct s = t.clazz;
				if (s.generated_from != null) s = s.generated_from;
				if( !PassInfo.resolveBestMethodR(s,v,new ResInfo(),nameArrayOp,new Expr[]{index},null,t,ResolveFlags.NoForwards) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,new Expr[]{index})+" in "+t);
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
				foreach(Method m; s.methods; m.name.equals(nameArrayOp))
					return new Type[]{Type.getRealType(t,m.type.ret)};
				if( s.super_clazz != null ) {
					s = s.super_clazz.clazz;
					continue;
				}
				throw new RuntimeException("Resolved object "+obj+" of type "+t+" is not an array and does not overrides '[]' operator");
			}
		}
	}

	public int getPriority() { return opContainerElementPriority; }

	public void cleanup() {
		obj.cleanup();
		obj = null;
		index.cleanup();
		index = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			obj = (Expr)obj.resolve(null);
			if( !obj.getType().isArray() ) {
				// May be an overloaded '[]' operator, ensure overriding
				Struct s = obj.getType().clazz;
				if (s.generated_from != null) s = s.generated_from;
			lookup_op:
				for(;;) {
					s.checkResolved();
					foreach(Method m; s.methods; m.name.equals(nameArrayOp))
						break lookup_op;
					if( s.super_clazz != null ) {
						s = s.super_clazz.clazz;
						continue;
					}
					throw new RuntimeException("Resolved object "+obj+" of type "+obj.getType()+" is not an array and does not overrides '[]' operator");
				}
			}
			index = (Expr)index.resolve(null);
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
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
				PVar<ASTNode> v;
				if( !PassInfo.resolveBestMethodR(obj.getType().clazz,v,new ResInfo(),nameArrayOp,new Expr[]{index},null,obj.getType(),ResolveFlags.NoForwards) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,new Expr[]{index}));
				obj.generate(null);
				index.generate(null);
				Method func = (Method)v;
				Code.addInstr(Instr.op_call,func,false,obj.getType());
				if( Kiev.verify
				 && func.type.ret.isReference()
				 && Type.getRealType(Kiev.argtype,func.type.ret).isReference()
//				 && func.jtype!= null
				 && ( !getType().clazz.equals(func.type.ret.clazz) || getType().isArray() ) )
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
			Type objType = Type.getRealType(Kiev.argtype,obj.getType());
			if( objType.isArray() ) {
				Code.addInstr(Instr.op_arr_store);
			} else {
				// Resolve overloaded set method
				PVar<ASTNode> v;
				// We need to get the type of object in stack
				Type t = Code.stack_at(0);
				Expr o = new VarAccessExpr(pos,this,new Var(pos,this,KString.Empty,t,0));
				Struct s = objType.clazz;
				if (s.generated_from != null) s = s.generated_from;
				if( !PassInfo.resolveBestMethodR(s,v,new ResInfo(),nameArrayOp,new Expr[]{index,o},null,objType,ResolveFlags.NoForwards) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,new Expr[]{index,o})+" in "+objType);
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
				PVar<ASTNode> v;
				// We need to get the type of object in stack
				Type t = Code.stack_at(0);
				if( !(Code.stack_at(1).isIntegerInCode() || Code.stack_at(0).isReference()) )
					throw new CompilerException(pos,"Index of '[]' can't be of type double or long");
				Expr o = new VarAccessExpr(pos,this,new Var(pos,this,KString.Empty,t,0));
				Struct s = obj.getType().clazz;
				if (s.generated_from != null) s = s.generated_from;
				if( !PassInfo.resolveBestMethodR(s,v,new ResInfo(),nameArrayOp,new Expr[]{index,o},null,obj.getType(),ResolveFlags.NoForwards) )
					throw new CompilerException(pos,"Can't find method "+Method.toString(nameArrayOp,new Expr[]{index,o}));
				// The method must return the value to duplicate
				Method func = (Method)v;
				Code.addInstr(Instr.op_call,func,false,obj.getType());
				if( Kiev.verify
				 && Type.getRealType(Kiev.argtype,func.type.ret).isReference()
//				 && func.jtype!= null
				 && ( !getType().clazz.equals(func.type.ret.clazz) || getType().isArray() ) )
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


public class VarAccessExpr extends LvalueExpr {

	public Var		var;

	public VarAccessExpr(int pos, Var var) {
		super(pos);
		if( var == null )
			throw new RuntimeException("Null var");
		this.var = var;
	}
	public VarAccessExpr(int pos, ASTNode par, Var var) {
		super(pos,par);
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
		ScopeNodeInfo sni = NodeInfoPass.getNodeInfo(var);
		if( sni == null || sni.types == null )
			return new Type[]{var.type};
		return sni.types;
	}

	public void cleanup() {
		parent=null;
		var = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			// Check if we try to access this var from local inner/anonymouse class
			if( Kiev.kaffe && PassInfo.method!=null && PassInfo.method.isLocalMethod()) {
				ASTNode p = var.parent;
				while( !(p instanceof Method) ) p = p.parent;
				if( p != PassInfo.method && p.parent == PassInfo.clazz ) {
					// Check if we already have proxyed this var
					boolean already_prox = false;
					for(int i=0; i < PassInfo.method.params.length; i++) {
						if(PassInfo.method.params[i].isClosureProxy() &&
							PassInfo.method.params[i].name.name.equals(var.name.name)
						) {
							var = PassInfo.method.params[i];
							already_prox = true;
							break;
						}
					}
					if( !already_prox ) {
						Type[] types = (Type[])Arrays.insert(PassInfo.method.type.args,var.type,0);
						PassInfo.method.type = MethodType.newMethodType(
							PassInfo.method.type.clazz,
							PassInfo.method.type.fargs,
							types,
							PassInfo.method.type.ret
							);
						Var v = new Var(PassInfo.method.pos,var.name.name,var.type,0);
						v.parent = PassInfo.method;
						v.setClosureProxy(true);
						if( PassInfo.method.isStatic() )
							PassInfo.method.params = (Var[])Arrays.insert(
								PassInfo.method.params,v,0);
						else
							PassInfo.method.params = (Var[])Arrays.insert(
								PassInfo.method.params,v,1);
						var = v;
					}
				}
			}
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
						vf = PassInfo.clazz.addField(new Field(PassInfo.clazz,var.name.name,var.type,ACC_PUBLIC));
						vf.setNeedProxy(true);
						vf.init = this;
					}
				}
			}
/*
			// Check it needs to be initialized
			if( parent instanceof AssignExpr && ((AssignExpr)parent).lval == this );
			else if( parent instanceof kiev.parser.ASTIdentifier
			 && parent.parent instanceof AssignExpr
			 && ((AssignExpr)parent.parent).lval == parent );
			else {
				ScopeNodeInfo sni = NodeInfoPass.getNodeInfo(var);
				if( !sni.initialized ) {
					assert( !(var.parent instanceof Method), "Uninitialized method parametr" );
					if( var.parent instanceof DeclStat ) {
						Kiev.reportWarning(pos,"Access to possibly unitialized variable "+var);
						if( ((DeclStat)var.parent).init == null ) {
							if( var.type.isBoolean() )
								((DeclStat)var.parent).init = new ConstBooleanExpr(var.pos,false);
							else if( var.type.isIntegerInCode() )
								((DeclStat)var.parent).init = new ConstExpr(var.pos,Kiev.newInteger(0));
							else if( var.type == Type.tpLong )
								((DeclStat)var.parent).init = new ConstExpr(var.pos,Kiev.newLong(0));
							else if( var.type == Type.tpFloat )
								((DeclStat)var.parent).init = new ConstExpr(var.pos,Kiev.newFloat(0.f));
							else if( var.type == Type.tpDouble )
								((DeclStat)var.parent).init = new ConstExpr(var.pos,Kiev.newDouble(0.d));
							else
								((DeclStat)var.parent).init = new ConstExpr(var.pos,null);
						}
					} else {
						Kiev.reportError(pos,"Access to possibly unitialized variable "+var);
					}
				}
			}
*/
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
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
			(Field)prt.clazz.resolveName(nameCellVal);
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
		if( !Type.getRealType(Kiev.argtype,var.type).isReference() || var.type.isArray() ) return;
		Type chtp = null;
		if( var.parent instanceof Method ) {
			Method m = (Method)var.parent;
			for(int i=m.isStatic()?0:1; i < m.params.length; i++) {
				if( var == m.params[i] ) {
					// First param is this
//					if( m.isStatic() ) chtp = m.jtype.args[i];
//					else chtp = m.jtype.args[i-1];
					if( m.isStatic() ) chtp = m.type.args[i];
					else chtp = m.type.args[i-1];
					break;
				}
			}
		}
		if( chtp == null )
			chtp = var.type.getJavaType();
		if( !var.type.clazz.equals(chtp.clazz) ) {
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
					Code.addInstr(op_load,PassInfo.method.params[0]);
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
					Code.addInstr(op_load,PassInfo.method.params[0]);
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
					Code.addInstr(op_getfield,resolveVarVal(),PassInfo.clazz.type);
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
					Code.addInstr(op_load,PassInfo.method.params[0]);
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

	public int getPriority() { return opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		if( var.isNeedRefProxy() )
			dmp.append(".val");
		return dmp.space();
	}

	public Dumper toCpp(Dumper dmp) {
		dmp.space();
		dmp.append(var);
		if( var.isNeedRefProxy() )
			dmp.append("->val");
		return dmp.space();
	}

}

public class LocalPrologVarAccessExpr extends LvalueExpr {

	public Var		var;

	public LocalPrologVarAccessExpr(int pos, ASTNode par, Var var) {
		super(pos,par);
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

	public void cleanup() {
		parent=null;
		var = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public Field resolveFieldForLocalPrologVar() {
		RuleMethod rm = (RuleMethod)PassInfo.method;
		TypeDeclStat tds = (TypeDeclStat)((BlockStat)rm.body).stats[0];
		Struct s = tds.struct;
		Field f = s.resolveField(var.name.name);
		assert(f != null);
		return f;
	}

	public Var resolveFrameForLocalPrologVar() {
		RuleMethod rm = (RuleMethod)PassInfo.method;
		if( rm.isStatic() ) {
			assert(rm.params[0].type == Type.tpRule);
			return rm.params[0];
		} else {
			assert(rm.params[1].type == Type.tpRule);
			return rm.params[1];
		}
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

	public int getPriority() { return opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append("$env.").append(var.name.name);
		return dmp.space();
	}
}

public class FieldAccessExpr extends LvalueExpr {

	public Field		var;
	public Method	fset;		// for virtual fields
	public Method	fget;		// for virtual fields

	public FieldAccessExpr(int pos, Field var) {
		super(pos);
		this.var = var;
	}

	public FieldAccessExpr(int pos, Field var, int flags) {
		super(pos);
		this.var = var;
		setFlags(flags);
	}

	public String toString() { return var.toString(); }

	public boolean	isConstantExpr() {
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.isConstantExpr();
		}
		return false;
	}
	public Object	getConstValue() {
		var.acc.verifyReadAccess(var);
		if( var.isFinal() ) {
			if( var.init != null )
				return var.init.getConstValue();
		}
    	throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public Type getType() {
		try {
			Type t = var.getType();
			return Type.getRealType((Type)PassInfo.method.params[0].type,t);
		} catch(Exception e) {
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = NodeInfoPass.getNodeInfo(var);
		if( sni == null || sni.types == null )
			types = new Type[]{var.type};
		else
			types = sni.types;
		for(int i=0; i < types.length; i++) types[i] = Type.getRealType((Type)PassInfo.method.params[0].type,types[i]);
		return types;
	}

	public void cleanup() {
		parent=null;
		var = null;
		fget = null;
		fset = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				KString get_name = new KStringBuffer(nameGet.length()+var.name.name.length()).
					append_fast(nameGet).append_fast(var.name.name).toKString();
				KString set_name = new KStringBuffer(nameSet.length()+var.name.name.length()).
					append_fast(nameSet).append_fast(var.name.name).toKString();

				if( PassInfo.method.name.equals(get_name)
				 || PassInfo.method.name.equals(set_name) ) {
				 	setAsField(true);
				} else {
					// We return get$ method. set$ method must be checked by AssignExpr
					PVar<Method> fsg;
					PassInfo.resolveBestMethodR(((Struct)var.parent),fsg,new ResInfo(),set_name,new Expr[]{this},Type.tpVoid,null,ResolveFlags.NoForwards);
					fset = fsg;
					fsg = null;
					PassInfo.resolveBestMethodR(((Struct)var.parent),fsg,new ResInfo(),get_name,Expr.emptyArray,getType(),null,ResolveFlags.NoForwards);
					fget = fsg;
				}
			}
			if( PassInfo.method.isStatic() && !PassInfo.method.isVirtualStatic() ) {
				throw new RuntimeException("Access to non-static field "+var+" in static method "+PassInfo.method);
			}

			// Set violation of the field
			if( PassInfo.method != null /*&& PassInfo.method.isInvariantMethod()*/ )
				PassInfo.method.addViolatedField(var);

		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
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

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating FieldAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyReadAccess(var);
			Code.addInstr(op_load,PassInfo.method.params[0]);
			if( var.isVirtual() && !isAsField() ) {
				assert(fget != null,"methods get$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fget,false,PassInfo.clazz.type);
			} else {
				if( var.isPackedField() )
					Code.addInstr(op_getfield,var.pack.packer,PassInfo.clazz.type);
				else
					Code.addInstr(op_getfield,var,PassInfo.clazz.type);
			}
			if( Kiev.verify && var.type.clazz.isArgument()
			 && Type.getRealType(Kiev.argtype,getType()).isReference() )
				Code.addInstr(op_checkcast,getType());
			if( var.isPackedField() ) {
				int mask = masks[var.pack.size];
				mask <<= var.pack.offset;
				Code.addConst(mask);
				Code.addInstr(op_and);
				if(var.pack.offset > 0) {
					Code.addConst(var.pack.offset);
					Code.addInstr(op_ushr);
				}
				if( var.pack.size == 8 && var.type == Type.tpByte )
					Code.addInstr(Instr.op_x2y,Type.tpByte);
				else if( var.pack.size == 16 && var.type == Type.tpShort )
					Code.addInstr(Instr.op_x2y,Type.tpShort);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating FieldAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyReadAccess(var);
			Code.addInstr(op_load,PassInfo.method.params[0]);
			Code.addInstr(op_dup);
			if( var.isVirtual() && !isAsField() ) {
				assert(fget != null,"methods get$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fget,false,PassInfo.clazz.type);
			} else {
				if( var.isPackedField() )
					Code.addInstr(op_getfield,var.pack.packer,PassInfo.clazz.type);
				else
					Code.addInstr(op_getfield,var,PassInfo.clazz.type);
			}
			if( Kiev.verify && var.type.clazz.isArgument()
			 && Type.getRealType(Kiev.argtype,getType()).isReference() )
				Code.addInstr(op_checkcast,getType());
			if( var.isPackedField() ) {
				int mask = masks[var.pack.size];
				mask <<= var.pack.offset;
				Code.addConst(mask);
				Code.addInstr(op_and);
				if(var.pack.offset > 0) {
					Code.addConst(var.pack.offset);
					Code.addInstr(op_ushr);
				}
				if( var.pack.size == 8 && var.type == Type.tpByte )
					Code.addInstr(Instr.op_x2y,Type.tpByte);
				else if( var.pack.size == 16 && var.type == Type.tpShort )
					Code.addInstr(Instr.op_x2y,Type.tpShort);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateAccess() {
		trace(Kiev.debugStatGen,"\t\tgenerating FieldAccessExpr - access only: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,PassInfo.method.params[0]);
		} finally { PassInfo.pop(this); }
	}

	public void generateStore() {
		trace(Kiev.debugStatGen,"\t\tgenerating FieldAccessExpr - store only: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				assert(fset != null,"methods set$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fset,false,PassInfo.clazz.type);
			} else {
				if( var.isPackedField() ) {
					// Correct value
					int mask = masks[var.pack.size];
					Code.addConst(mask);
					Code.addInstr(op_and);
					if(var.pack.offset > 0) {
						Code.addConst(var.pack.offset);
						Code.addInstr(op_shl);
					}

					// Load old value of packer field
					Code.addInstr(op_swap);
					Code.addInstr(op_dup_x);
					Code.addInstr(op_getfield,var.pack.packer,PassInfo.clazz.type);
					// Clear var's position
					mask = masks[var.pack.size];
					mask <<= var.pack.offset;
					mask = ~mask;
					Code.addConst(mask);
					Code.addInstr(op_and);

					// Fill with var's value
					Code.addInstr(op_or);

					// Store packer field
					Code.addInstr(op_putfield,var.pack.packer,PassInfo.clazz.type);
				} else {
					Code.addInstr(op_putfield,var,PassInfo.clazz.type);
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating SimpleAccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_dup_x);
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				assert(fset != null,"methods set$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fset,false,PassInfo.clazz.type);
			} else {
				if( var.isPackedField() ) {
					// Correct value
					int mask = masks[var.pack.size];
					Code.addConst(mask);
					Code.addInstr(op_and);
					if(var.pack.offset > 0) {
						Code.addConst(var.pack.offset);
						Code.addInstr(op_shl);
					}

					// Load old value of packer field
					Code.addInstr(op_swap);
					Code.addInstr(op_dup_x);
					Code.addInstr(op_getfield,var.pack.packer,PassInfo.clazz.type);
					// Clear var's position
					mask = masks[var.pack.size];
					mask <<= var.pack.offset;
					mask = ~mask;
					Code.addConst(mask);
					Code.addInstr(op_and);

					// Fill with var's value
					Code.addInstr(op_or);

					// Store packer field
					Code.addInstr(op_putfield,var.pack.packer,PassInfo.clazz.type);
				} else {
					Code.addInstr(op_putfield,var,PassInfo.clazz.type);
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public int getPriority() { return opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		if( isAsField() ) {
			return dmp.space().append("this.").append(var.name).space();
		} else {
			return dmp.space().append("this.").append(var).space();
		}
	}

	public Dumper toCpp(Dumper dmp) {
		if( isAsField() ) {
			return dmp.space().append("this->").append(var.name).space();
		} else {
			return dmp.space().append("this->").append(var).space();
		}
	}
}

public class StaticFieldAccessExpr extends LvalueExpr {

	public Struct		obj;
	public Field		var;
	public Method	fset;		// for virtual fields
	public Method	fget;		// for virtual fields

	public StaticFieldAccessExpr(int pos, Struct obj, Field var) {
		super(pos);
		this.var = var;
		this.obj = obj;
	}

	public StaticFieldAccessExpr(int pos, Struct obj, Field var, int flags) {
		super(pos);
		this.var = var;
		this.obj = obj;
		setFlags(flags);
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
			Type t = var.getType();
			return Type.getRealType(obj.super_clazz,t);
		} catch(Exception e) {
			Kiev.reportError(pos,e);
			return Type.tpVoid;
		}
	}

	public Type[] getAccessTypes() {
		Type[] types;
		ScopeNodeInfo sni = NodeInfoPass.getNodeInfo(var);
		if( sni == null || sni.types == null )
			types = new Type[]{var.type};
		else
			types = sni.types;
		for(int i=0; i < types.length; i++) types[i] = Type.getRealType(obj.super_clazz,types[i]);
		return types;
	}

	public void cleanup() {
		parent=null;
		var = null;
		obj = null;
		fget = null;
		fset = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				KString get_name = new KStringBuffer(nameGet.length()+var.name.name.length()).
					append_fast(nameGet).append_fast(var.name.name).toKString();
				KString set_name = new KStringBuffer(nameSet.length()+var.name.name.length()).
					append_fast(nameSet).append_fast(var.name.name).toKString();

				if( PassInfo.method.name.equals(get_name)
				 || PassInfo.method.name.equals(set_name) ) {
				 	setAsField(true);
				} else {
					// We return get$ method. set$ method must be checked by AssignExpr
					PVar<Method> fsg;
					PassInfo.resolveBestMethodR(((Struct)var.parent),fsg,new ResInfo(),set_name,new Expr[]{this},Type.tpVoid,null,ResolveFlags.NoForwards);
					fset = fsg;
					fsg = null;
					PassInfo.resolveBestMethodR(((Struct)var.parent),fsg,new ResInfo(),get_name,Expr.emptyArray,getType(),null,ResolveFlags.NoForwards);
					fget = fsg;
				}
			}

			// Set violation of the field
			if( PassInfo.method != null /*&& PassInfo.method.isInvariantMethod()*/ )
				PassInfo.method.addViolatedField(var);

		} finally { PassInfo.pop(this); }

		setResolved(true);
		return this;
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyReadAccess(var);
			if( var.isVirtual() && !isAsField() ) {
				assert(fget != null,"methods get$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fget,false,PassInfo.clazz.type);
			}
			else if (var.parent.isPrimitiveEnum()) {
				Code.addConst(((Integer)((ConstExpr)var.init).getConstValue()).intValue());
			}
			else {
				Code.addInstr(op_getstatic,var,PassInfo.clazz.type);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyReadAccess(var);
			if( var.isVirtual() && !isAsField() ) {
				assert(fget != null,"methods get$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fget,false,PassInfo.clazz.type);
			} else {
				Code.addInstr(op_getstatic,var,PassInfo.clazz.type);
			}
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
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				assert(fset != null,"methods set$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fset,false,PassInfo.clazz.type);
			} else {
				Code.addInstr(op_putstatic,var,PassInfo.clazz.type);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generateStoreDupValue() {
		trace(Kiev.debugStatGen,"\t\tgenerating StaticFieldAccessExpr - store & dup: "+this);
		PassInfo.push(this);
		try {
			var.acc.verifyWriteAccess(var);
			Code.addInstr(op_dup);
			if( (var.isVirtual() || var.isExportCpp()) && !isAsField() ) {
				assert(fset != null,"methods set$"+var.name.name+" not preresolved");
				Code.addInstr(op_call,fset,false,PassInfo.clazz.type);
			} else {
				Code.addInstr(op_putstatic,var,PassInfo.clazz.type);
			}
		} finally { PassInfo.pop(this); }
	}

	public int getPriority() { return opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
		Struct cl = (Struct)var.parent;
		cl = Type.getRealType(Kiev.argtype,cl.type).clazz;
		if( isAsField() ) {
			return dmp.space().append(cl.name)
				.append('.').append(var.name).space();
		} else {
			return dmp.space().append(cl.name)
				.append('.').append(var).space();
		}
	}

	public Dumper toCpp(Dumper dmp) {
		Struct cl = (Struct)var.parent;
		cl = Type.getRealType(Kiev.argtype,cl.type).clazz;
		cl.name.make_cpp_name();
		KString cl_cpp_name = cl.name.cpp_name;
		if (cl.isPrimitiveEnum()) {
			cl.package_clazz.name.make_cpp_name();
			cl_cpp_name = cl.package_clazz.name.cpp_name;
		}
		if (!Kiev.gen_cpp_namespace) {
			cl_cpp_name = KString.Empty;
		}
		if( isAsField() ) {
			return dmp.space().append(cl_cpp_name)
				.append("::").append(var.name).space();
		} else {
			return dmp.space().append(cl_cpp_name)
				.append("::").append(var).space();
		}
	}
}

public class OuterThisAccessExpr extends LvalueExpr {

	public Struct		outer;
	public Field[]		outer_refs = Field.emptyArray;

	public OuterThisAccessExpr(int pos, Struct outer) {
		super(pos);
		this.outer = outer;
	}

	public String toString() { return outer.name.toString()+".this"; }

	public Type getType() {
		return outer.type;
	}

	public static Field outerOf(Struct clazz) {
		for(int i=0; i < clazz.fields.length; i++)
			if( clazz.fields[i].name.name.startsWith(nameThisDollar) ) {
				trace(Kiev.debugResolve,"Name of field "+clazz.fields[i]+" starts with this$");
				return clazz.fields[i];
			}
		return null;
	}

	public void cleanup() {
		parent=null;
		outer = null;
		outer_refs = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			trace(Kiev.debugResolve,"Resolving "+this);
			Field ou_ref = outerOf(PassInfo.clazz);
			if( ou_ref == null )
				throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+PassInfo.clazz);
			do {
				trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
				outer_refs = (Field[])Arrays.append(outer_refs,ou_ref);
				if( ou_ref.type.isInstanceOf(outer.type) ) break;
				ou_ref = outerOf(ou_ref.type.clazz);
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
		return this;
	}

	public void generateLoad() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load only: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,PassInfo.method.params[0]);
			for(int i=0; i < outer_refs.length; i++)
				Code.addInstr(op_getfield,outer_refs[i],PassInfo.clazz.type);
		} finally { PassInfo.pop(this); }
	}

	public void generateLoadDup() {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load & dup: "+this);
		PassInfo.push(this);
		try {
			Code.addInstr(op_load,PassInfo.method.params[0]);
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
			Code.addInstr(op_load,PassInfo.method.params[0]);
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

	public int getPriority() { return opAccessPriority; }

	public Dumper toJava(Dumper dmp) { return dmp.space().append(outer.name.name).append(".this").space(); }
}

public class SelfAccessExpr extends LvalueExpr {

	public LvalueExpr		expr;

	public SelfAccessExpr(int pos, LvalueExpr expr) {
		super(pos);
		this.expr = expr;
		this.expr.parent = this;
	}

	public String toString() {
		if( expr.getPriority() < opAccessPriority )
			return "("+expr.toString()+").$self";
		else
			return expr.toString()+".$self";
	}

	public Type getType() {
		return Type.tpObject;
	}

	public int getPriority() { return opAccessPriority; }

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		expr = (LvalueExpr)expr.resolve(reqType);
		setResolved(true);
		// Thanks to AccessExpr, that resolved all things for us
		return this;
	}

	/** Just load value referenced by lvalue */
	public void generateLoad() {
		expr.generateLoad();
	}

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public void generateLoadDup() {
		expr.generateLoadDup();
	}

	/** Load info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public void generateAccess() {
		expr.generateAccess();
	}

	/** Stores value using previously duped info */
	public void generateStore() {
		expr.generateStore();
	}

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue() {
		expr.generateStoreDupValue();
	}

	public Dumper toJava(Dumper dmp) {
		if( expr.getPriority() < opAccessPriority ) {
			dmp.append('(');
			expr.toJava(dmp).append(")").space();
		} else {
			expr.toJava(dmp).space();
		}
		return dmp;
	}

	public Dumper toCpp(Dumper dmp) {
		if( expr.getPriority() < opAccessPriority ) {
			dmp.append('(').append(expr).append(")").space();
		} else {
			dmp.append(expr).space();
		}
		return dmp;
	}
}

