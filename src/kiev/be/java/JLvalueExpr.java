package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.LvalueExpr.LvalueExprImpl;
import kiev.vlang.AccessExpr.AccessExprImpl;
import kiev.vlang.IFldExpr.IFldExprImpl;
import kiev.vlang.ContainerAccessExpr.ContainerAccessExprImpl;
import kiev.vlang.ThisExpr.ThisExprImpl;
import kiev.vlang.LVarExpr.LVarExprImpl;
import kiev.vlang.SFldExpr.SFldExprImpl;
import kiev.vlang.OuterThisAccessExpr.OuterThisAccessExprImpl;

@nodeview
public abstract view JLvalueExprView of LvalueExprImpl extends JENodeView {
	public JLvalueExprView(LvalueExpr.LvalueExprImpl $view) {
		super($view);
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		generateLoad(code);
		if( reqType == Type.tpVoid )
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
public abstract view JAccessExprView of AccessExprImpl extends JLvalueExprView {
	public access:ro	JENodeView	obj;
	public access:ro	KString		ident;
}

@nodeview
public final view JIFldExprView of IFldExprImpl extends JAccessExprView {
	public access:ro	JFieldView		var;

	public boolean	isConstantExpr() { return var.getField().isConstantExpr(); }
	public Object	getConstValue() { return var.getField().getConstValue(); }

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf((Struct)var.parent) )
			code.addInstr(Instr.op_checkcast,((Struct)var.parent).type);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - load only: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		var.acc.verifyReadAccess(this,var);
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		code.addInstr(op_getfield,var,obj.getType());
		if( Kiev.verify && var.type.isArgument() && getType().isReference() )
			code.addInstr(op_checkcast,getType());
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - load & dup: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		var.acc.verifyReadAccess(this,var);
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		code.addInstr(op_dup);
		code.addInstr(op_getfield,var,obj.getType());
		if( Kiev.verify && var.type.isArgument() && getType().isReference() )
			code.addInstr(op_checkcast,getType());
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
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_putfield,var,obj.getType());
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IFldExpr - store & dup: "+this);
		code.setLinePos(this);
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(this, "IFldExpr: Generating virtual field "+var+" directly");
		if( var.isPackedField() )
			Kiev.reportError(this, "IFldExpr: Generating packed field "+var+" directly");
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_dup_x);
		code.addInstr(op_putfield,var,obj.getType());
	}

}


@nodeview
public final view JContainerAccessExprView of ContainerAccessExprImpl extends JLvalueExprView {
	public access:ro	JENodeView		obj;
	public access:ro	JENodeView		index;

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
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType()},Type.tpAny);
			ResInfo info = new ResInfo(getNode(),ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt));
			obj.generate(code,null);
			index.generate(code,null);
			Method func = (Method)v;
			code.addInstr(Instr.op_call,func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret.isReference()
			 && ( !getType().isStructInstanceOf(func.type.ret.getStruct()) || getType().isArray() ) )
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
			Type t = code.stack_at(0);
			ENode o = new LVarExpr(pos,new Var(pos,KString.Empty,t,0));
			Struct s = objType.getStruct();
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo(getNode(),ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(objType,v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt)+" in "+objType);
			code.addInstr(Instr.op_call,(Method)v,false,objType);
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
			Type t = code.stack_at(0);
			if( !(code.stack_at(1).isIntegerInCode() || code.stack_at(0).isReference()) )
				throw new CompilerException(this,"Index of '[]' can't be of type double or long");
			ENode o = new LVarExpr(pos,new Var(pos,KString.Empty,t,0));
			Struct s = obj.getType().getStruct();
			MethodType mt = MethodType.newMethodType(null,new Type[]{index.getType(),o.getType()},Type.tpAny);
			ResInfo info = new ResInfo(getNode(),ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic);
			if( !PassInfo.resolveBestMethodR(obj.getType(),v,info,nameArrayOp,mt) )
				throw new CompilerException(this,"Can't find method "+Method.toString(nameArrayOp,mt));
			// The method must return the value to duplicate
			Method func = (Method)v;
			code.addInstr(Instr.op_call,func,false,obj.getType());
			if( Kiev.verify
			 && func.type.ret.isReference()
			 && ( !getType().isStructInstanceOf(func.type.ret.getStruct()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

}


@nodeview
public final view JThisExprView of ThisExprImpl extends JLvalueExprView {

	public JThisExprView(ThisExprImpl $view) { super($view); }
	
	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0].getJVarView());
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
			code.addInstr(op_load,code.method.params[0].getJVarView());
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
			code.addInstr(op_store,code.method.params[0].getJVarView());
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
			code.addInstr(op_store,code.method.params[0].getJVarView());
		else {
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
			code.addInstr(op_pop);
		}
	}

}

@nodeview
public final view JLVarExprView of LVarExprImpl extends JLvalueExprView {
	public access:ro	KString		ident;
	public access:ro	JVarView	var;

	public JFieldView resolveProxyVar(Code code) {
		Field proxy_var = code.clazz.resolveField(this.ident,false);
		if( proxy_var == null && code.method.isStatic() && !code.method.isVirtualStatic() )
			throw new CompilerException(this,"Proxyed var cannot be referenced from static context");
		return proxy_var.getJFieldView();
	}

	public JFieldView resolveVarVal() {
		BaseType prt = Type.getProxyType(var.type);
		Field var_valf = prt.clazz.resolveField(nameCellVal);
		return var_valf.getJFieldView();
	}

	public JVarView resolveVarForConditions(Code code) {
		assert( code.cond_generation );
		Var var = this.var.getVar();
		// Bind the correct var
		if( var.parent != code.method ) {
			assert( var.parent instanceof Method, "Non-parametrs var in condition" );
			if( this.ident==nameResultVar ) {
				var = code.method.getRetVar();
			} else {
				for(int i=0; i < code.method.params.length; i++) {
					Var v = code.method.params[i];
					if( !v.name.equals(var.name) ) continue;
					assert( var.type.equals(v.type), "Type of vars in overriden methods missmatch" );
					var = v;
					break;
				}
			}
			trace(Kiev.debugStatGen,"Var "+var+" substituted for condition");
		}
		assert( var.parent == code.method, "Can't find var for condition" );
		return var.getJVarView();
	}

	public void generateVerifyCheckCast(Code code) {
		if( !Kiev.verify ) return;
		if( !var.type.isReference() || var.type.isArray() ) return;
		Type chtp = null;
		if( var.parent instanceof Method ) {
			Method m = (Method)var.parent;
			for(int i=0; i < m.params.length; i++) {
				if( var == m.params[i] ) {
					chtp = m.jtype.args[i];
					break;
				}
			}
		}
		if( chtp == null )
			chtp = var.type.getJavaType();
		if( !var.type.isStructInstanceOf(chtp.getStruct()) ) {
			code.addInstr(op_checkcast,var.type);
			return;
		}
	}

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - load only: "+this);
		code.setLinePos(this);
		JVarView var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.type);
			} else {
				code.addInstr(op_load,var);
			}
		}
		generateVerifyCheckCast(code);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - load & dup: "+this);
		code.setLinePos(this);
		JVarView var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_dup);
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.type);
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
		JVarView var = this.var;
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
		JVarView var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_putfield,resolveProxyVar(code),code.clazz.type);
			} else {
				code.addInstr(op_store,var);
			}
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating LVarExpr - store & dup: "+this);
		code.setLinePos(this);
		JVarView var = this.var;
		if( code.cond_generation ) var = resolveVarForConditions(code);
		if( !var.isNeedProxy() || isUseNoProxy() ) {
			if( code.vars[var.bcpos] == null )
				throw new CompilerException(this,"Var "+var+" has bytecode pos "+var.bcpos+" but code.var["+var.bcpos+"] == null");
			code.addInstr(op_dup);
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_dup_x);
				code.addInstr(op_putfield,resolveVarVal(),code.clazz.type);
			} else {
				code.addInstr(op_dup_x);
				code.addInstr(op_putfield,resolveVarVal(),code.clazz.type);
			}
		}
		generateVerifyCheckCast(code);
	}

}

@nodeview
public final view JSFldExprView of SFldExprImpl extends JAccessExprView {
	public access:ro	JFieldView		var;
	
	public boolean	isConstantExpr() { return var.getField().isConstantExpr(); }
	public Object	getConstValue() { return var.getField().getConstValue(); }

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - load only: "+this);
		code.setLinePos(this);
		var.acc.verifyReadAccess(this,var);
		code.addInstr(op_getstatic,var,code.clazz.type);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - load & dup: "+this);
		code.setLinePos(this);
		var.acc.verifyReadAccess(this,var);
		code.addInstr(op_getstatic,var,code.clazz.type);
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - store only: "+this);
		code.setLinePos(this);
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_putstatic,var,code.clazz.type);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SFldExpr - store & dup: "+this);
		code.setLinePos(this);
		var.acc.verifyWriteAccess(this,var);
		code.addInstr(op_dup);
		code.addInstr(op_putstatic,var,code.clazz.type);
	}

}

@nodeview
public final view JOuterThisAccessExprView of OuterThisAccessExprImpl extends JAccessExprView {
	public access:ro	Struct			outer;
	public access:ro	NArr<Field>		outer_refs;

	public void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load only: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++)
			code.addInstr(op_getfield,outer_refs[i].getJFieldView(),code.clazz.type);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - load & dup: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length; i++) {
			if( i == outer_refs.length-1 ) code.addInstr(op_dup);
			code.addInstr(op_getfield,outer_refs[i].getJFieldView(),code.clazz.type);
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - access only: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		for(int i=0; i < outer_refs.length-1; i++) {
			code.addInstr(op_getfield,outer_refs[i].getJFieldView(),code.clazz.type);
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr - store only: "+this);
		code.addInstr(op_putfield,outer_refs[outer_refs.length-1].getJFieldView(),code.clazz.type);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating SimpleAccessExpr - store & dup: "+this);
		code.addInstr(op_dup_x);
		code.addInstr(op_putfield,outer_refs[outer_refs.length-1].getJFieldView(),code.clazz.type);
	}

}

