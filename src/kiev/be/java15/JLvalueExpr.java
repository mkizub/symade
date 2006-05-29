package kiev.be.java15;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java15.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
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

@nodeview
public abstract view JAccessExpr of AccessExpr extends JLvalueExpr {
	public:ro	JENode		obj;
}

@nodeview
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
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - load only: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		Access.verifyRead(this,var);
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
			if( Kiev.verify && var.type instanceof ArgType && getType().isReference() )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - load & dup: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		Access.verifyRead(this,var);
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
			if( Kiev.verify && var.type instanceof ArgType && getType().isReference() )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - access only: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - store only: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		Access.verifyWrite(this,var);
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
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - store & dup: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		Access.verifyWrite(this,var);
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


@nodeview
public final view JContainerAccessExpr of ContainerAccessExpr extends JLvalueExpr {
	public:ro	JENode		obj;
	public:ro	JENode		index;

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load only: "+this);
		code.setLinePos(this);
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(Instr.op_arr_load);
		} else {
			// Resolve overloaded access method
			Method@ v;
			CallType mt = new CallType(new Type[]{index.getType()},Type.tpAny);
			ResInfo info = new ResInfo((ASTNode)this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayGetOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayGetOp,mt));
			obj.generate(code,null);
			index.generate(code,null);
			Method func = (Method)v;
			code.addInstr(Instr.op_call,(JMethod)func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret().isReference()
			 && ( !func.type.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load & dup: "+this);
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
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - access only: "+this);
		code.setLinePos(this);
		obj.generate(code,null);
		index.generate(code,null);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store only: "+this);
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
			ENode o = new LVarExpr(pos,new Var(pos,"",t,0));
			Struct s = objType.getStruct();
			CallType mt = new CallType(new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo((ASTNode)this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(objType,v,info,nameArraySetOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArraySetOp,mt)+" in "+objType);
			Method func = (Method)v;
			code.addInstr(Instr.op_call,(JMethod)func,false,objType);
			// Pop return value
			code.addInstr(Instr.op_pop);
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store & dup: "+this);
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
			ENode o = new LVarExpr(pos,new Var(pos,"",t,0));
			Struct s = obj.getType().getStruct();
			CallType mt = new CallType(new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo((ASTNode)this,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArraySetOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArraySetOp,mt));
			// The method must return the value to duplicate
			Method func = (Method)v;
			code.addInstr(Instr.op_call,(JMethod)func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret().isReference()
			 && ( !func.type.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

}


@nodeview
public final view JThisExpr of ThisExpr extends JLvalueExpr {

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
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
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load & dup: "+this);
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
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store only: "+this);
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
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - store & dup: "+this);
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

@nodeview
public final view JLVarExpr of LVarExpr extends JLvalueExpr {
	public:ro	JVar		var;

	public JField resolveProxyVar(Code code) {
		JField proxy_var = code.clazz.resolveField(this.ident.name,true);
		if( proxy_var == null && code.method.isStatic() && !code.method.isVirtualStatic() )
			throw new CompilerException(this,"Proxyed var cannot be referenced from static context");
		return proxy_var;
	}

	public JField resolveVarVal() {
		CompaundType prt = Type.getProxyType(var.type);
		JField var_valf = ((JStruct)prt.clazz).resolveField(nameCellVal);
		return var_valf;
	}

	public JVar resolveVarForConditions(Code code) {
		assert( code.cond_generation );
		JVar var = this.var;
		// Bind the correct var
		if( !var.jparent.equals(code.method) ) {
			assert( var.jparent instanceof JMethod, "Non-parametrs var in condition" );
			if (this.ident.name == nameResultVar) {
				var = code.method.getRetVar();
			} else {
				for(int i=0; i < code.method.params.length; i++) {
					JVar v = code.method.params[i];
					if (v.id.uname != var.id.uname) continue;
					assert( var.type.equals(v.type), "Type of vars in overriden methods missmatch" );
					var = v;
					break;
				}
			}
			trace(Kiev.debugStatGen,"Var "+var+" substituted for condition");
		}
		assert( var.jparent.equals(code.method), "Can't find var for condition" );
		return var;
	}

	public void generateVerifyCheckCast(Code code) {
		if( !Kiev.verify ) return;
		if( !var.type.isReference() || var.type.isArray() ) return;
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
			chtp = var.type.getErasedType();
		if( !var.type.getJType().isInstanceOf(chtp.getJType()) ) {
			code.addInstr(op_checkcast,var.type);
			return;
		}
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - load only: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
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
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - load & dup: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
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
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - access only: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_dup);
			}
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - store only: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
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
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - store & dup: "+this);
		code.setLinePos(this);
		JVar var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
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

@nodeview
public final view JSFldExpr of SFldExpr extends JLvalueExpr {
	public:ro JENode		obj;
	public:ro JField		var;
	
	public boolean	isConstantExpr() { return var.isConstantExpr(); }
	public Object	getConstValue() { return var.getConstValue(); }

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - load only: "+this);
		code.setLinePos(this);
		Access.verifyRead(this,var);
		code.addInstr(op_getstatic,var,code.clazz.xtype);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - load & dup: "+this);
		code.setLinePos(this);
		Access.verifyRead(this,var);
		code.addInstr(op_getstatic,var,code.clazz.xtype);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - store only: "+this);
		code.setLinePos(this);
		Access.verifyWrite(this,var);
		code.addInstr(op_putstatic,var,code.clazz.xtype);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - store & dup: "+this);
		code.setLinePos(this);
		Access.verifyWrite(this,var);
		code.addInstr(op_dup);
		code.addInstr(op_putstatic,var,code.clazz.xtype);
	}

}

@nodeview
public final view JOuterThisAccessExpr of OuterThisAccessExpr extends JLvalueExpr {
	public:ro	Struct			outer;
	public:ro	JField[]		outer_refs;

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load only: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++)
			code.addInstr(op_getfield,outer_refs[i],code.clazz.xtype);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load & dup: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++) {
			if( i == outer_refs.length-1 ) code.addInstr(op_dup);
			code.addInstr(op_getfield,outer_refs[i],code.clazz.xtype);
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - access only: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length-1; i++) {
			code.addInstr(op_getfield,outer_refs[i],code.clazz.xtype);
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - store only: "+this);
		code.addInstr(op_putfield,outer_refs[outer_refs.length-1],code.clazz.xtype);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - store & dup: "+this);
		code.addInstr(op_dup_x);
		code.addInstr(op_putfield,outer_refs[outer_refs.length-1],code.clazz.xtype);
	}

}

@nodeview
public final view JReinterpExpr of ReinterpExpr extends JLvalueExpr {
	public:ro	JENode		expr;

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - load only: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateLoad(code);
		else
			expr.generate(code, null);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - load & dup: "+this);
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
		trace(Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - access only: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateAccess(code);
		else
			expr.generate(code, null);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - store only: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateStore(code);
		else
			throw new CompilerException(this,"Cannot generate store for non-lvalue "+expr);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - store & dup: "+this);
		code.setLinePos(this);
		JENode expr = this.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateStoreDupValue(code);
		else
			throw new CompilerException(this,"Cannot generate store+dup value for non-lvalue "+expr);
	}

}

