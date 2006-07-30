package kiev.be.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view JMethod of Method extends JDNode {

	public:ro	JVar[]					params;
	public:ro	JENode					body;
	public		Attr[]					attrs;
	public:ro	JWBCCondition[]			conditions;

	public:ro	CallType				type;
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

	public JVar getOuterThisParam() { return (JVar) ((Method)this).getOuterThisParam(); }
	public JVar getTypeInfoParam(int kind) { return (JVar) ((Method)this).getTypeInfoParam(kind); }
	public JVar getVarArgParam() { return (JVar) ((Method)this).getVarArgParam(); }
	
	public CodeLabel getBreakLabel() {
		return block.getBreakLabel();
	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		for(int i=0; i < attrs.length; i++) {
			if(attrs[i].name == a.name) {
				attrs[i] = a;
				return a;
			}
		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		for(int i=0; i < attrs.length; i++)
			if( attrs[i].name.equals(name) )
				return attrs[i];
		return null;
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
					thisPar = (JVar)new FormPar(pos,Constants.nameThis,jctx_tdecl.xtype,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
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
							if( !id.equals(nameInit) && !id.equals(nameClassInit) ) {
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
						if( type.ret() ≢ Type.tpVoid ) {
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
						if( type.ret() ≢ Type.tpVoid ) {
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
					JMethod func = Type.tpError.getJStruct().resolveMethod(nameInit,KString.from("(Ljava/lang/String;)V"));
					code.addInstr(Instr.op_call,func,false);
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
        	JStruct[] thrs = new JStruct[mthrs.length];
			for (int i=0; i < mthrs.length; i++)
				thrs[i] = (JStruct)mthrs[i].getType().getStruct();
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			this.addAttr(athr);
        }
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params.length; i++) {
			Type tp1 = etype.arg(i);
			Type tp2 = params[i].type;
			if !(tp2.getErasedType().isInstanceOf(tp1)) {
				code.addInstr(Instr.op_load,params[i]);
				code.addInstr(Instr.op_checkcast,tp1);
				code.addInstr(Instr.op_store,params[i]);
			}
		}
	}
}

@nodeview
public final view JInitializer of Initializer extends JDNode {
	public:ro	JENode		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Initializer");
		code.setLinePos(this);
		body.generate(code,reqType);
	}
}

@nodeview
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
					thisPar = (JVar)new FormPar(pos,Constants.nameThis,jctx_tdecl.xtype,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD|ACC_SYNTHETIC);
					code.addVar(thisPar);
				}
				code.addVars(m.params);
				if( cond==WBCType.CondEnsure && m.type.ret() ≢ Type.tpVoid ) code.addVar(m.getRetVar());
				body.generate(code,Type.tpVoid);
				if( cond==WBCType.CondEnsure && m.type.ret() ≢ Type.tpVoid ) code.removeVar(m.getRetVar());
				code.removeVars(m.params);
				if( thisPar != null ) code.removeVar(thisPar);
				this.openForEdit();
				code.generateCode(this);
			} catch(Exception e) {
				Kiev.reportError(this,e);
			}
			return;
		}
		code_attr.generate(constPool);
	}
}

