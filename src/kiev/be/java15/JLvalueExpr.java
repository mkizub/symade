/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.be.java15;
import syntax kiev.Syntax;

import static kiev.be.java15.Instr.*;

@ViewOf(vcast=true, iface=true)
public abstract view JLvalueExpr of LvalueExpr extends JENode {

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		generateLoad(code);
		if( reqType ≡ Type.tpVoid )
			code.addInstr(Instr.op_pop);
	}

	/** Just load value referenced by lvalue */
	public abstract void generateLoad(Code code);

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateLoadDup(Code code);

	/** Load info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateAccess(Code code);

	/** Stores value using previously duped info */
	public abstract void generateStore(Code code);

	/** Stores value using previously duped info, and put stored value in stack */
	public abstract void generateStoreDupValue(Code code);
}

@ViewOf(vcast=true, iface=true)
public final view JAccessExpr of AccessExpr extends JLvalueExpr {
	public:ro	JENode		obj;

	public void generateLoad(Code code) { throw new RuntimeException("JAccessExpr.generateLoad()"); }
	public void generateLoadDup(Code code) { throw new RuntimeException("JAccessExpr.generateLoadDup()"); }
	public void generateAccess(Code code) { throw new RuntimeException("JAccessExpr.generateAccess()"); }
	public void generateStore(Code code) { throw new RuntimeException("JAccessExpr.generateStore()"); }
	public void generateStoreDupValue(Code code) { throw new RuntimeException("JAccessExpr.generateStoreDupValue()"); }
}

@ViewOf(vcast=true, iface=true)
public final view JIFldExpr of IFldExpr extends JLvalueExpr {
	
	private static final Field arr_length = Type.tpArray.resolveField("length");
	
	public:ro JENode		obj;
	public:ro JField		var;

	public boolean	isConstantExpr() { return var.isConstantExpr(); }
	public Object	getConstValue() { return var.getConstValue(); }

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.getJType().isInstanceOf(var.jctx_tdecl.jtype) )
			code.addInstr(Instr.op_checkcast,var.jctx_tdecl.xtype);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - load only: "+this);
		code.setLinePos(this);
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyRead(this,var);
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		if (var.isNative()) {
			Field var = (Field)this.var;
			assert(var.isMacro());
			if (var == arr_length) {
				code.addInstr(Instr.op_arrlength);
			} else {
				Kiev.reportError(this, "IFldExpr: Unknown native macro field "+var);
				JConstExpr.generateConst(null, code, getType());
			}
		} else {
			code.addInstr(op_getfield,var,obj.getType());
			if( Kiev.verify && var.getType() instanceof ArgType && getType().isReference() )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - load & dup: "+this);
		code.setLinePos(this);
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyRead(this,var);
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		code.addInstr(op_dup);
		if (var.isNative()) {
			Field var = (Field)this.var;
			assert(var.isMacro());
			if (var == arr_length) {
				code.addInstr(Instr.op_arrlength);
			} else {
				Kiev.reportError(this, "IFldExpr: Unknown native macro field "+var);
				JConstExpr.generateConst(null, code, getType());
			}
		} else {
			code.addInstr(op_getfield,var,obj.getType());
			if( Kiev.verify && var.getType() instanceof ArgType && getType().isReference() )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - access only: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - store only: "+this);
		code.setLinePos(this);
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyWrite(this,var);
		if (var.isNative()) {
			assert(var.isMacro());
			Kiev.reportError(this, "IFldExpr: Unknown native macro field "+var);
			code.addInstr(op_pop);
			code.addInstr(op_pop);
		} else {
			code.addInstr(op_putfield,var,obj.getType());
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - store & dup: "+this);
		code.setLinePos(this);
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyWrite(this,var);
		code.addInstr(op_dup_x);
		if (var.isNative()) {
			assert(var.isMacro());
			Kiev.reportError(this, "IFldExpr: Unknown native macro field "+var);
			code.addInstr(op_pop);
			code.addInstr(op_pop);
		} else {
			code.addInstr(op_putfield,var,obj.getType());
		}
	}

}


@ViewOf(vcast=true, iface=true)
public final view JContainerAccessExpr of ContainerAccessExpr extends JLvalueExpr {
	public:ro	JENode		obj;
	public:ro	JENode		index;

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load only: "+this);
		code.setLinePos(this);
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(Instr.op_arr_load);
		} else {
			// Resolve overloaded access method
			Method@ v;
			CallType mt = new CallType(obj.getType(),null,new Type[]{index.getType()},Type.tpAny,false);
			ResInfo info = new ResInfo((ASTNode)this,nameArrayGetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayGetOp,mt));
			obj.generate(code,null);
			index.generate(code,null);
			Method func = (Method)v;
			code.addInstr(Instr.op_call,(JMethod)func,false,obj.getType());
			if( Kiev.verify
			 && func.mtype.ret().isReference()
			 && ( !func.mtype.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load & dup: "+this);
		code.setLinePos(this);
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(op_dup2);
			code.addInstr(Instr.op_arr_load);
		} else {
			throw new CompilerException(this,"Too complex expression for overloaded operator '[]'");
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - access only: "+this);
		code.setLinePos(this);
		obj.generate(code,null);
		index.generate(code,null);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store only: "+this);
		code.setLinePos(this);
		Type objType = obj.getType();
		if( objType.isArray() ) {
			code.addInstr(Instr.op_arr_store);
		} else {
			// Resolve overloaded set method
			Method@ v;
			// We need to get the type of object in stack
			JType jt = code.stack_at(0);
			Type t = Signature.getType(jt.java_signature);
			ENode o = new LVarExpr(pos,new LVar(pos,"",t,Var.VAR_LOCAL,0));
			Struct s = objType.getStruct();
			CallType mt = new CallType(objType,null,new Type[]{index.getType(),o.getType()},Type.tpAny,false);
			ResInfo info = new ResInfo((ASTNode)this,nameArraySetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(objType,v,info,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArraySetOp,mt)+" in "+objType);
			Method func = (Method)v;
			code.addInstr(Instr.op_call,(JMethod)func,false,objType);
			// Pop return value
			code.addInstr(Instr.op_pop);
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store & dup: "+this);
		code.setLinePos(this);
		if( obj.getType().isArray() ) {
			code.addInstr(op_dup_x2);
			code.addInstr(Instr.op_arr_store);
		} else {
			// Resolve overloaded set method
			Method@ v;
			// We need to get the type of object in stack
			JType jt = code.stack_at(0);
			Type t = Signature.getType(jt.java_signature);
			if( !(code.stack_at(1).isIntegerInCode() || code.stack_at(0).isReference()) )
				throw new CompilerException(this,"Index of '[]' can't be of type double or long");
			ENode o = new LVarExpr(pos,new LVar(pos,"",t,Var.VAR_LOCAL,0));
			Struct s = obj.getType().getStruct();
			CallType mt = new CallType(obj.getType(),null,new Type[]{index.getType(),o.getType()},Type.tpAny,false);
			ResInfo info = new ResInfo((ASTNode)this,nameArraySetOp,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArraySetOp,mt));
			// The method must return the value to duplicate
			Method func = (Method)v;
			code.addInstr(Instr.op_call,(JMethod)func,false,obj.getType());
			if( Kiev.verify
			 && func.mtype.ret().isReference()
			 && ( !func.mtype.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

}


@ViewOf(vcast=true, iface=true)
public final view JThisExpr of ThisExpr extends JLvalueExpr {

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addNullConst();
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ThisExpr - load & dup: "+this);
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addNullConst();
		}
		code.addInstr(op_dup);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ThisExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ThisExpr - store only: "+this);
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrStoreThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_store,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addInstr(op_pop);
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ThisExpr - store & dup: "+this);
		code.setLinePos(this);
		code.addInstr(op_dup);
		if (!code.method.isStatic())
			code.addInstrStoreThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_store,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addInstr(op_pop);
		}
	}

}

@ViewOf(vcast=true, iface=true)
public final view JSuperExpr of SuperExpr extends JENode {

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SuperExpr");
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addNullConst();
		}
	}
}

@ViewOf(vcast=true, iface=true)
public final view JLVarExpr of LVarExpr extends JLvalueExpr {
	public:ro	JVar		var;

	public JField resolveProxyVar(Code code) {
		JField proxy_var = code.clazz.resolveField(this.ident,true);
		if( proxy_var == null && code.method.isStatic() && !code.method.isVirtualStatic() )
			throw new CompilerException(this,"Proxyed var cannot be referenced from static context");
		return proxy_var;
	}

	public JField resolveVarVal() {
		CompaundType prt = Type.getProxyType(var.getType());
		JField var_valf = ((JStruct)(Struct)prt.tdecl).resolveField(nameCellVal);
		return var_valf;
	}

	public JVar resolveVarForConditions(Code code) {
		assert( code.cond_generation );
		JVar var = this.var;
		// Bind the correct var
		if( !var.jparent.equals(code.method) ) {
			assert( var.jparent instanceof JMethod, "Non-parametrs var in condition" );
			if (this.ident == nameResultVar) {
				var = code.method.getRetVar();
			} else {
				for(int i=0; i < code.method.params.length; i++) {
					JVar v = code.method.params[i];
					if (v.sname != var.sname) continue;
					assert( var.getType().equals(v.vtype), "Type of vars in overriden methods missmatch" );
					var = v;
					break;
				}
			}
			trace(Kiev.debug && Kiev.debugStatGen,"Var "+var+" substituted for condition");
		}
		assert( var.jparent.equals(code.method), "Can't find var for condition" );
		return var;
	}

	public void generateVerifyCheckCast(Code code) {
		if( !Kiev.verify ) return;
		if( !var.getType().isReference() || var.getType().isArray() ) return;
		Type chtp = null;
		if( var.jparent instanceof JMethod ) {
			JMethod m = (JMethod)var.jparent;
			JVar[] params = m.params;
			for(int i=0; i < params.length; i++) {
				if( var == params[i] ) {
					chtp = m.etype.arg(i);
					break;
				}
			}
		}
		if( chtp == null )
			chtp = var.getType().getErasedType();
		if( !var.getType().getJType().isInstanceOf(chtp.getJType()) ) {
			code.addInstr(op_checkcast,var.getType());
			return;
		}
	}

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating LVarExpr - load only: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() ) {
			if( code.lookupCodeVar(var) == null )
				throw new CompilerException("Var "+var+" not exists in the code");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.xtype);
			} else {
				code.addInstr(op_load,var);
			}
		}
		generateVerifyCheckCast(code);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating LVarExpr - load & dup: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() ) {
			if( code.lookupCodeVar(var) == null )
				throw new CompilerException("Var "+var+" not exists in the code");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_dup);
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.xtype);
			} else {
				code.addInstr(op_load,var);
				code.addInstr(op_dup);
			}
		}
		generateVerifyCheckCast(code);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating LVarExpr - access only: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() ) {
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_dup);
			}
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating LVarExpr - store only: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() ) {
			if( code.lookupCodeVar(var) == null )
				throw new CompilerException("Var "+var+" not exists in the code");
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_putfield,resolveProxyVar(code),code.clazz.xtype);
			} else {
				code.addInstr(op_store,var);
			}
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating LVarExpr - store & dup: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() ) {
			if( code.lookupCodeVar(var) == null )
				throw new CompilerException("Var "+var+" not exists in the code");
			code.addInstr(op_dup);
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_dup_x);
				code.addInstr(op_putfield,resolveVarVal(),code.clazz.xtype);
			} else {
				code.addInstr(op_dup_x);
				code.addInstr(op_putfield,resolveVarVal(),code.clazz.xtype);
			}
		}
		generateVerifyCheckCast(code);
	}

}

@ViewOf(vcast=true, iface=true)
public final view JSFldExpr of SFldExpr extends JLvalueExpr {
	public:ro JTypeRef		obj;
	public:ro JField		var;
	
	public boolean	isConstantExpr() { return var.isConstantExpr(); }
	public Object	getConstValue() { return var.getConstValue(); }

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - load only: "+this);
		code.setLinePos(this);
		MetaAccess.verifyRead(this,var);
		code.addInstr(op_getstatic,var,code.clazz.xtype);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - load & dup: "+this);
		code.setLinePos(this);
		MetaAccess.verifyRead(this,var);
		code.addInstr(op_getstatic,var,code.clazz.xtype);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - store only: "+this);
		code.setLinePos(this);
		MetaAccess.verifyWrite(this,var);
		code.addInstr(op_putstatic,var,code.clazz.xtype);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - store & dup: "+this);
		code.setLinePos(this);
		MetaAccess.verifyWrite(this,var);
		code.addInstr(op_dup);
		code.addInstr(op_putstatic,var,code.clazz.xtype);
	}

}

@ViewOf(vcast=true, iface=true)
public final view JOuterThisAccessExpr of OuterThisAccessExpr extends JENode {
	public:ro	JField[]		outer_refs;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++)
			code.addInstr(op_getfield,outer_refs[i],code.clazz.xtype);
	}

}

@ViewOf(vcast=true, iface=true)
public final view JReinterpExpr of ReinterpExpr extends JLvalueExpr {
	public:ro	JENode		expr;

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - load only: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateLoad(code);
		else
			expr.generate(code, null);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - load & dup: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr) {
			expr.generateLoadDup(code);
		} else {
			expr.generate(code, null);
			code.addInstr(op_dup);
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - access only: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateAccess(code);
		else
			expr.generate(code, null);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - store only: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateStore(code);
		else
			throw new CompilerException(this,"Cannot generate store for non-lvalue "+expr);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - store & dup: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateStoreDupValue(code);
		else
			throw new CompilerException(this,"Cannot generate store+dup value for non-lvalue "+expr);
	}

}

