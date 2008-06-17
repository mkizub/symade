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

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true, iface=true)
public final view JMethod of Method extends JDNode {

	public:ro	JVar[]					params;
	public:ro	JENode					body;
	public:ro	JWBCCondition[]			conditions;

	public:ro	CallType				mtype;
	public:ro	CallType				dtype;
	public:ro	CallType				etype;

	public:ro	JBlock					block;

	public MetaThrows getMetaThrows();
	
	public JVar	getRetVar() { return (JVar)((Method)this).getRetVar(); }
	
	public final boolean isVirtualStatic();
	public final boolean isVarArgs();
	public final boolean isRuleMethod();
	public final boolean isOperatorMethod();
	public final boolean isNeedFieldInits();
	public final boolean isInvariantMethod();
	public final boolean isInlinedByDispatcherMethod();

	@getter public JMethod get$child_jctx_method() { return this; }
	
	public boolean isConstructor() {
		return ((Method)this) instanceof Constructor;
	}

	public JVar getOuterThisParam() { return (JVar) ((Method)this).getOuterThisParam(); }
	public JVar getTypeInfoParam(int kind) { return (JVar) ((Method)this).getTypeInfoParam(kind); }
	public JVar getVarArgParam() { return (JVar) ((Method)this).getVarArgParam(); }
	
	public JLabel getBrkLabel() {
		return block.getBrkLabel();
	}

	public void generate(ConstPool constPool) {
		if( Kiev.debug ) System.out.println("\tgenerating Method "+this);
		foreach(JWBCCondition cond; conditions; cond.cond != WBCType.CondInvariant )
			cond.generate(constPool,Type.tpVoid);
		if( !isAbstract() && body != null && !(body instanceof MetaValue)) {
			Code code = new Code((JStruct)jctx_tdecl, this, constPool);
			code.generation = true;
			try {
				JVar thisPar = null;
				if (!isStatic()) {
					thisPar = (JVar)new LVar(pos,Constants.nameThis,jctx_tdecl.xtype,Var.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
					code.addVar(thisPar);
				}
				code.addVars(params);
				if( !isBad() && !isMacro() ) {
					if( Kiev.verify )
						generateArgumentCheck(code);
					if( Kiev.debugOutputC ) {
						foreach(JWBCCondition cond; conditions; cond.cond == WBCType.CondRequire )
							code.importCode(cond.code_attr);
						foreach(JWBCCondition cond; conditions; cond.cond == WBCType.CondInvariant ) {
							assert( cond.jparent instanceof JMethod && ((JMethod)cond.jparent).isInvariantMethod() );
							if( !isConstructor() ) {
								if( !((JDNode)cond.jparent).isStatic() )
									code.addInstrLoadThis();
								code.addInstr(Instr.op_call,cond.jctx_method,false);
							}
							code.need_to_gen_post_cond = true;
						}
						if( !code.need_to_gen_post_cond ) {
							foreach(JWBCCondition cond; conditions; cond.cond != WBCType.CondRequire ) {
								code.need_to_gen_post_cond = true;
								break;
							}
						}
					}
					body.generate(code,Type.tpVoid);
					if( Kiev.debugOutputC && code.need_to_gen_post_cond ) {
						if( mtype.ret() ≢ Type.tpVoid ) {
							code.addVar(getRetVar());
							code.addInstr(Instr.op_store,getRetVar());
						}
						foreach(JWBCCondition cond; conditions; cond.cond == WBCType.CondInvariant ) {
							if( !((JDNode)cond.jparent).isStatic() )
								code.addInstrLoadThis();
							code.addInstr(Instr.op_call,cond.jctx_method,false);
							code.need_to_gen_post_cond = true;
						}
						foreach(JWBCCondition cond; conditions; cond.cond == WBCType.CondEnsure )
							code.importCode(cond.code_attr);
						if( mtype.ret() ≢ Type.tpVoid ) {
							code.addInstr(Instr.op_load,getRetVar());
							code.addInstr(Instr.op_return);
							code.removeVar(getRetVar());
						} else {
							code.addInstr(Instr.op_return);
						}
					}
				} else {
					code.addInstr(Instr.op_new,Type.tpError);
					code.addInstr(Instr.op_dup);
					KString msg;
					if (isMacro())
						msg = KString.from("Macro method invocation");
					else
						msg = KString.from("Compiled with errors");
					constPool.addStringCP(msg);
					code.addConst(msg);
					JMethod func = Type.tpError.getJType().getJStruct().resolveMethod(null,KString.from("(Ljava/lang/String;)V"));
					code.addInstr(Instr.op_call,func,true);
					code.addInstr(Instr.op_throw);
				}
				code.removeVars(params);
				if( thisPar != null ) code.removeVar(thisPar);
				code.generateCode();
			} catch(Exception e) {
				Kiev.reportError(this,e);
			}
		}
		MetaThrows throwns = getMetaThrows();
        if( throwns != null ) {
			ASTNode[] mthrs = throwns.getThrowns();
        	KString[] thrs = new KString[mthrs.length];
			for (int i=0; i < mthrs.length; i++)
				thrs[i] = mthrs[i].getType().getJType().java_signature;
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			this.addAttr(athr);
        }
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params.length; i++) {
			Type tp1 = etype.arg(i);
			Type tp2 = params[i].vtype;
			if !(tp2.getErasedType().isInstanceOf(tp1)) {
				code.addInstr(Instr.op_load,params[i]);
				code.addInstr(Instr.op_checkcast,tp1);
				code.addInstr(Instr.op_store,params[i]);
			}
		}
	}
}

@ViewOf(vcast=true, iface=true)
public final view JInitializer of Initializer extends JDNode {
	public:ro	JENode		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating Initializer");
		code.setLinePos(this);
		body.generate(code,reqType);
	}
}

@ViewOf(vcast=true, iface=true)
public final final view JWBCCondition of WBCCondition extends JDNode {
	public:ro	WBCType				cond;
	public:ro	JENode				body;
	public:ro	JMethod				definer;
	public		CodeAttr			code_attr;

	public void generate(ConstPool constPool, Type reqType) {
		Code code = new Code((JStruct)jctx_tdecl, jctx_method, constPool);
		code.generation = true;
		code.cond_generation = true;
		if( cond == WBCType.CondInvariant ) {
			body.generate(code,Type.tpVoid);
			code.addInstr(Instr.op_return);
			return;
		}
		if( code_attr == null ) {
			JMethod m = code.method;
			try {
				JVar thisPar = null;
				if( !isStatic() ) {
					thisPar = (JVar)new LVar(pos,Constants.nameThis,jctx_tdecl.xtype,Var.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
					code.addVar(thisPar);
				}
				code.addVars(m.params);
				if( cond==WBCType.CondEnsure && m.mtype.ret() ≢ Type.tpVoid ) code.addVar(m.getRetVar());
				body.generate(code,Type.tpVoid);
				if( cond==WBCType.CondEnsure && m.mtype.ret() ≢ Type.tpVoid ) code.removeVar(m.getRetVar());
				code.removeVars(m.params);
				if( thisPar != null ) code.removeVar(thisPar);
				code.generateCode(this);
			} catch(Exception e) {
				Kiev.reportError(this,e);
			}
			return;
		}
		code_attr.generate(constPool);
	}
}

