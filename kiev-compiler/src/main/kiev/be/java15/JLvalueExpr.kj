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

public class JLvalueExpr extends JENode {

	@virtual typedef VT  ≤ LvalueExpr;

	public static JLvalueExpr attach(LvalueExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JLvalueExpr)jn;
		if (impl instanceof IFldExpr)
			return JIFldExpr.attach((IFldExpr)impl);
		if (impl instanceof ThisExpr)
			return JThisExpr.attach((ThisExpr)impl);
		if (impl instanceof ContainerAccessExpr)
			return JContainerAccessExpr.attach((ContainerAccessExpr)impl);
		if (impl instanceof LVarExpr)
			return JLVarExpr.attach((LVarExpr)impl);
		if (impl instanceof SFldExpr)
			return JSFldExpr.attach((SFldExpr)impl);
		if (impl instanceof ReinterpExpr)
			return JReinterpExpr.attach((ReinterpExpr)impl);
		return new JLvalueExpr(impl);
	}
	
	protected JLvalueExpr(LvalueExpr impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		generateLoad(code);
		if( reqType ≡ code.tenv.tpVoid )
			code.addInstr(Instr.op_pop);
	}

	/** Just load value referenced by lvalue */
	public void generateLoad(Code code) { throw new RuntimeException("JLvalueExpr.generateLoad()"); }

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public void generateLoadDup(Code code) { throw new RuntimeException("JLvalueExpr.generateLoadDup()"); }

	/** Load info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public void generateAccess(Code code) { throw new RuntimeException("JLvalueExpr.generateAccess()"); }

	/** Stores value using previously duped info */
	public void generateStore(Code code) { throw new RuntimeException("JLvalueExpr.generateStore()"); }

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue(Code code) { throw new RuntimeException("JLvalueExpr.generateStoreDupValue()"); }
}

public final class JIFldExpr extends JLvalueExpr {

	@virtual typedef VT  ≤ IFldExpr;

	public final JField var;

	public static JIFldExpr attach(IFldExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JIFldExpr)jn;
		return new JIFldExpr(impl);
	}
	
	protected JIFldExpr(IFldExpr impl) {
		super(impl);
		this.var = (JField)impl.var;
	}

	public boolean	isConstantExpr(Env env) { return var.isConstantExpr(env); }
	public Object	getConstValue(Env env) { return var.getConstValue(env); }

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		IFldExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		Type ot = obj.getType();
		if( !code.jtenv.getJType(ot).isInstanceOf(var.jctx_tdecl.getJType(code.jtenv)) )
			code.addInstr(Instr.op_checkcast,var.jctx_tdecl.getType());
	}

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - load only: "+this);
		code.setLinePos(this);
		IFldExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(vn, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyRead(vn,var.vn());
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		if (var.isNative()) {
			Field var = this.var.vn();
			assert(var.isMacro());
			Field arr_length = code.jenv.getFldArrLength();
			if (var == arr_length) {
				code.addInstr(Instr.op_arrlength);
			} else {
				Kiev.reportError(vn, "IFldExpr: Unknown native macro field "+var);
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
		IFldExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(vn, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyRead(vn,var.vn());
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
		code.addInstr(op_dup);
		if (var.isNative()) {
			Field var = this.var.vn();
			assert(var.isMacro());
			Field arr_length = code.jenv.getFldArrLength();
			if (var == arr_length) {
				code.addInstr(Instr.op_arrlength);
			} else {
				Kiev.reportError(vn, "IFldExpr: Unknown native macro field "+var);
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
		IFldExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		if( var.isVirtual() && !isAsField() )
			Kiev.reportError(vn, "IFldExpr: Generating virtual field "+var+" directly");
		obj.generate(code,null);
		generateCheckCastIfNeeded(code);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - store only: "+this);
		code.setLinePos(this);
		IFldExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(vn, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyWrite(vn,var.vn());
		if (var.isNative()) {
			assert(var.isMacro());
			Kiev.reportError(vn, "IFldExpr: Unknown native macro field "+var);
			code.addInstr(op_pop);
			code.addInstr(op_pop);
		} else {
			code.addInstr(op_putfield,var,obj.getType());
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IFldExpr - store & dup: "+this);
		code.setLinePos(this);
		IFldExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		if (var.isVirtual() && !isAsField())
			Kiev.reportError(vn, "IFldExpr: Generating virtual field "+var+" directly");
		MetaAccess.verifyWrite(vn,var.vn());
		code.addInstr(op_dup_x);
		if (var.isNative()) {
			assert(var.isMacro());
			Kiev.reportError(vn, "IFldExpr: Unknown native macro field "+var);
			code.addInstr(op_pop);
			code.addInstr(op_pop);
		} else {
			code.addInstr(op_putfield,var,obj.getType());
		}
	}

}


public final class JContainerAccessExpr extends JLvalueExpr {

	@virtual typedef VT  ≤ ContainerAccessExpr;

	public static JContainerAccessExpr attach(ContainerAccessExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JContainerAccessExpr)jn;
		return new JContainerAccessExpr(impl);
	}
	
	protected JContainerAccessExpr(ContainerAccessExpr impl) {
		super(impl);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load only: "+this);
		code.setLinePos(this);
		ContainerAccessExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		JENode index = (JENode)vn.index;
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(Instr.op_arr_load);
		} else {
			throw new CompilerException(vn,"Overloaded arr[value] at generation phase");
		}
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - load & dup: "+this);
		code.setLinePos(this);
		ContainerAccessExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		JENode index = (JENode)vn.index;
		if( obj.getType().isArray() ) {
			obj.generate(code,null);
			index.generate(code,null);
			code.addInstr(op_dup2);
			code.addInstr(Instr.op_arr_load);
		} else {
			throw new CompilerException(vn,"Too complex expression for overloaded operator '[]'");
		}
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - access only: "+this);
		ContainerAccessExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		JENode index = (JENode)vn.index;
		if( obj.getType().isArray() ) {
			code.setLinePos(this);
			obj.generate(code,null);
			index.generate(code,null);
		} else {
			throw new CompilerException(vn,"Too complex expression for overloaded operator '[]'");
		}
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store only: "+this);
		code.setLinePos(this);
		ContainerAccessExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		JENode index = (JENode)vn.index;
		Type objType = obj.getType();
		if( objType.isArray() ) {
			code.addInstr(Instr.op_arr_store);
		} else {
			throw new CompilerException(vn,"Overloaded arr[value] at generation phase");
		}
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ContainerAccessExpr - store & dup: "+this);
		code.setLinePos(this);
		ContainerAccessExpr vn = vn();
		JENode obj = (JENode)vn.obj;
		JENode index = (JENode)vn.index;
		if( obj.getType().isArray() ) {
			code.addInstr(op_dup_x2);
			code.addInstr(Instr.op_arr_store);
		} else {
			throw new CompilerException(vn,"Overloaded arr[value] at generation phase");
		}
	}

}


public final class JThisExpr extends JLvalueExpr {

	@virtual typedef VT  ≤ ThisExpr;

	public static JThisExpr attach(ThisExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JThisExpr)jn;
		return new JThisExpr(impl);
	}
	
	protected JThisExpr(ThisExpr impl) {
		super(impl);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ThisExpr - load only: "+this);
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(vn(),"Access '"+toString()+"' in static context");
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
			Kiev.reportError(vn(),"Access '"+toString()+"' in static context");
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
			Kiev.reportError(vn(),"Access '"+toString()+"' in static context");
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
			Kiev.reportError(vn(),"Access '"+toString()+"' in static context");
			code.addInstr(op_pop);
		}
	}

}

public final class JSuperExpr extends JENode {

	@virtual typedef VT  ≤ SuperExpr;

	public static JSuperExpr attach(SuperExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JSuperExpr)jn;
		return new JSuperExpr(impl);
	}
	
	protected JSuperExpr(SuperExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SuperExpr");
		code.setLinePos(this);
		if (!code.method.isStatic())
			code.addInstrLoadThis();
		else if (code.method.isStatic() && code.method.isVirtualStatic())
			code.addInstr(op_load,code.method.params[0]);
		else {
			Kiev.reportError(vn(),"Access '"+toString()+"' in static context");
			code.addNullConst();
		}
	}
}

public final class JLVarExpr extends JLvalueExpr {

	@virtual typedef VT  ≤ LVarExpr;

	public final JVar var;

	public static JLVarExpr attach(LVarExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JLVarExpr)jn;
		return new JLVarExpr(impl);
	}
	
	protected JLVarExpr(LVarExpr impl) {
		super(impl);
		this.var = (JVar)impl.var;
	}

	public JField resolveProxyVar(Code code) {
		JField proxy_var = code.clazz.resolveField(code.jenv,this.getIdent(),true);
		if( proxy_var == null && code.method.isStatic() && !code.method.isVirtualStatic() )
			throw new CompilerException(vn(),"Proxyed var cannot be referenced from static context");
		return proxy_var;
	}

	public JField resolveVarVal(JEnv jenv) {
		CompaundType prt = Type.getProxyType(var.getType());
		JField var_valf = ((JStruct)(Struct)prt.tdecl).resolveField(jenv,nameCellVal);
		return var_valf;
	}

	public JVar resolveVarForConditions(Code code) {
		assert( code.cond_generation );
		JVar var = this.var;
		// Bind the correct var
		if( !var.jparent.equals(code.method) ) {
			assert( var.jparent instanceof JMethod, "Non-parametrs var in condition" );
			if (this.getIdent() == nameResultVar) {
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
		if( !code.jtenv.getJType(var.getType()).isInstanceOf(code.jtenv.getJType(chtp)) ) {
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
				throw new CompilerException(vn(), "Var "+var+" not exists in the code");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.getType());
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
				throw new CompilerException(vn(), "Var "+var+" not exists in the code");
			code.addInstr(op_load,var);
		} else {
			if( isAsField() ) {
				code.addInstrLoadThis();
				code.addInstr(op_dup);
				code.addInstr(op_getfield,resolveProxyVar(code),code.clazz.getType());
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
				throw new CompilerException(vn(), "Var "+var+" not exists in the code");
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_putfield,resolveProxyVar(code),code.clazz.getType());
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
				throw new CompilerException(vn(), "Var "+var+" not exists in the code");
			code.addInstr(op_dup);
			code.addInstr(op_store,var);
		} else {
			if( isAsField() ) {
				code.addInstr(op_dup_x);
				code.addInstr(op_putfield,resolveVarVal(code.jenv),code.clazz.getType());
			} else {
				code.addInstr(op_dup_x);
				code.addInstr(op_putfield,resolveVarVal(code.jenv),code.clazz.getType());
			}
		}
		generateVerifyCheckCast(code);
	}

}

public final class JSFldExpr extends JLvalueExpr {

	@virtual typedef VT  ≤ SFldExpr;

	public final JField var;

	public static JSFldExpr attach(SFldExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JSFldExpr)jn;
		return new JSFldExpr(impl);
	}
	
	protected JSFldExpr(SFldExpr impl) {
		super(impl);
		this.var = (JField)impl.var;
	}

	public boolean	isConstantExpr(Env env) { return var.isConstantExpr(env); }
	public Object	getConstValue(Env env) { return var.getConstValue(env); }

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - load only: "+this);
		code.setLinePos(this);
		MetaAccess.verifyRead(vn(),var.vn());
		code.addInstr(op_getstatic,var,code.clazz.getType());
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - load & dup: "+this);
		code.setLinePos(this);
		MetaAccess.verifyRead(vn(),var.vn());
		code.addInstr(op_getstatic,var,code.clazz.getType());
	}

	public void generateAccess(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - access only: "+this);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - store only: "+this);
		code.setLinePos(this);
		MetaAccess.verifyWrite(vn(),var.vn());
		code.addInstr(op_putstatic,var,code.clazz.getType());
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating SFldExpr - store & dup: "+this);
		code.setLinePos(this);
		MetaAccess.verifyWrite(vn(),var.vn());
		code.addInstr(op_dup);
		code.addInstr(op_putstatic,var,code.clazz.getType());
	}

}

public final class JOuterThisAccessExpr extends JENode {

	@virtual typedef VT  ≤ OuterThisAccessExpr;

	public static JOuterThisAccessExpr attach(OuterThisAccessExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JOuterThisAccessExpr)jn;
		return new JOuterThisAccessExpr(impl);
	}
	
	protected JOuterThisAccessExpr(OuterThisAccessExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating OuterThisAccessExpr: "+this);
		code.setLinePos(this);
		code.addInstrLoadThis();
		JField[] outer_refs = JNode.toJArray<JField>(vn().outer_refs);
		for(int i=0; i < outer_refs.length; i++)
			code.addInstr(op_getfield,outer_refs[i],code.clazz.getType());
	}

}

public final class JReinterpExpr extends JLvalueExpr {

	@virtual typedef VT  ≤ ReinterpExpr;

	public static JReinterpExpr attach(ReinterpExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JReinterpExpr)jn;
		return new JReinterpExpr(impl);
	}
	
	protected JReinterpExpr(ReinterpExpr impl) {
		super(impl);
	}

	public void generateLoad(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - load only: "+this);
		code.setLinePos(this);
		ReinterpExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateLoad(code);
		else
			expr.generate(code, null);
	}

	public void generateLoadDup(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - load & dup: "+this);
		code.setLinePos(this);
		ReinterpExpr vn = vn();
		JENode expr = (JENode)vn.expr;
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
		ReinterpExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateAccess(code);
		else
			expr.generate(code, null);
	}

	public void generateStore(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - store only: "+this);
		code.setLinePos(this);
		ReinterpExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateStore(code);
		else
			throw new CompilerException(vn,"Cannot generate store for non-lvalue "+expr);
	}

	public void generateStoreDupValue(Code code) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ReinterpExpr - store & dup: "+this);
		code.setLinePos(this);
		ReinterpExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		if (expr instanceof JLvalueExpr)
			expr.generateStoreDupValue(code);
		else
			throw new CompilerException(vn,"Cannot generate store+dup value for non-lvalue "+expr);
	}

}

