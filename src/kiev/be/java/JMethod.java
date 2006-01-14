package kiev.be.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.Method.MethodImpl;
import kiev.vlang.Initializer.InitializerImpl;
import kiev.vlang.WBCCondition.WBCConditionImpl;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view JMethodView of MethodImpl extends JDNodeView {

	public final Method getMethod() { return (Method)this.getNode(); }
		
	public MetaThrows getMetaThrows() {
		return (MetaThrows)this.$view.getNodeData(MetaThrows.ID);
	}

	public JVarView	getRetVar() {
		if( this.$view.retvar == null )
			this.$view.retvar = new Var(pos,nameResultVar,type.ret,ACC_FINAL);
		return this.$view.retvar.getJVarView();
	}


	public access:ro	Access				acc;
	public access:ro	KString				name;
	public access:ro	JVarView[]			params;
	public access:ro	JBlockStatView		body;
	public				Attr[]				attrs;
	public access:ro	JWBCConditionView[]	conditions;
	public access:ro	JFieldView[]		violated_fields;
	public access:ro	MetaValue			annotation_default;
	public access:ro	boolean				inlined_by_dispatcher;

	@getter public final MethodType				get$type()		{ return this.$view.type; }
	@getter public final MethodType				get$dtype()		{ return this.$view.dtype; }
	@getter public final MethodType				get$etype()		{ return (MethodType)dtype.getErasedType(); }
	@getter public final JVarView[]				get$params()	{ return (JVarView[])this.$view.params.toJViewArray(JVarView.class); }
	@getter public final JFieldView[]			get$violated_fields()	{ return (JFieldView[])this.$view.violated_fields.toJViewArray(JFieldView.class); }
	@getter public final JWBCConditionView[]	get$conditions()		{ return (JWBCConditionView[])this.$view.conditions.toJViewArray(JWBCConditionView.class); }

	public final boolean isVirtualStatic()		{ return this.$view.is_mth_virtual_static; }
	public final boolean isVarArgs()			{ return this.$view.is_mth_varargs; }
	public final boolean isRuleMethod()		{ return this.$view instanceof RuleMethod.RuleMethodImpl; }
	public final boolean isOperatorMethod()	{ return this.$view.is_mth_operator; }
	public final boolean isNeedFieldInits()	{ return this.$view.is_mth_need_fields_init; }
	public final boolean isInvariantMethod()	{ return this.$view.is_mth_invariant; }
	public final boolean isLocalMethod()		{ return this.$view.is_mth_local; }

	@getter public JMethodView get$child_jctx_method() { return this; }

	public JVarView getOuterThisParam() { return (JVarView)this.getMethod().getOuterThisParam(); }
	public JVarView getTypeInfoParam(int kind) { return (JVarView)this.getMethod().getTypeInfoParam(kind); }
	public JVarView getVarArgParam() { return (JVarView)this.getMethod().getVarArgParam(); }
	
	public CodeLabel getBreakLabel() {
		return body.getBreakLabel();
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
		foreach(JWBCConditionView cond; conditions; cond.cond != WBCType.CondInvariant )
			cond.generate(constPool,Type.tpVoid);
		if( !isAbstract() && body != null ) {
			Code code = new Code(jctx_clazz, this, constPool);
			code.generation = true;
			try {
				if( !isBad() ) {
					JVarView thisPar = null;
					if (!isStatic()) {
						thisPar = new FormPar(pos,Constants.nameThis,jctx_clazz.concr_type,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD).getJVarView();
						code.addVar(thisPar);
					}
					code.addVars(params);
					if( Kiev.verify )
						generateArgumentCheck(code);
					if( Kiev.debugOutputC ) {
						foreach(JWBCConditionView cond; conditions; cond.cond == WBCType.CondRequire )
							code.importCode(cond.code_attr);
						foreach(JWBCConditionView cond; conditions; cond.cond == WBCType.CondInvariant ) {
							assert( cond.jparent instanceof JMethodView && ((JMethodView)cond.jparent).isInvariantMethod() );
							if( !name.equals(nameInit) && !name.equals(nameClassInit) ) {
								if( !((JDNodeView)cond.jparent).isStatic() )
									code.addInstrLoadThis();
								code.addInstr(Instr.op_call,cond.jctx_method,false);
							}
							code.need_to_gen_post_cond = true;
						}
						if( !code.need_to_gen_post_cond ) {
							foreach(JWBCConditionView cond; conditions; cond.cond != WBCType.CondRequire ) {
								code.need_to_gen_post_cond = true;
								break;
							}
						}
					}
					body.generate(code,Type.tpVoid);
					if( Kiev.debugOutputC && code.need_to_gen_post_cond ) {
						if( type.ret ≢ Type.tpVoid ) {
							code.addVar(getRetVar());
							code.addInstr(Instr.op_store,getRetVar());
						}
						foreach(JWBCConditionView cond; conditions; cond.cond == WBCType.CondInvariant ) {
							if( !((JDNodeView)cond.jparent).isStatic() )
								code.addInstrLoadThis();
							code.addInstr(Instr.op_call,cond.jctx_method,false);
							code.need_to_gen_post_cond = true;
						}
						foreach(JWBCConditionView cond; conditions; cond.cond == WBCType.CondEnsure )
							code.importCode(cond.code_attr);
						if( type.ret ≢ Type.tpVoid ) {
							code.addInstr(Instr.op_load,getRetVar());
							code.addInstr(Instr.op_return);
							code.removeVar(getRetVar());
						} else {
							code.addInstr(Instr.op_return);
						}
					}
					code.removeVars(params);
					if( thisPar != null ) code.removeVar(thisPar);
				} else {
					code.addInstr(Instr.op_new,Type.tpError);
					code.addInstr(Instr.op_dup);
					KString msg = KString.from("Compiled with errors");
					constPool.addStringCP(msg);
					code.addConst(msg);
					JMethodView func = Type.tpError.getJStruct().resolveMethod(nameInit,KString.from("(Ljava/lang/String;)V"));
					code.addInstr(Instr.op_call,func,false);
					code.addInstr(Instr.op_throw);
				}
				code.generateCode();
			} catch(Exception e) {
				Kiev.reportError(this,e);
			}
		}
		MetaThrows throwns = getMetaThrows();
        if( throwns != null ) {
			ASTNode[] mthrs = throwns.getThrowns();
        	JStructView[] thrs = new JStructView[mthrs.length];
			for (int i=0; i < mthrs.length; i++)
				thrs[i] = mthrs[i].getType().getStruct().getJStructView();
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			this.addAttr(athr);
        }
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params.length; i++) {
			Type tp1 = etype.args[i];
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
public final view JInitializerView of InitializerImpl extends JDNodeView {
	public access:ro	JENodeView		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Initializer");
		code.setLinePos(this);
		body.generate(code,reqType);
	}
}

@nodeview
public final final view JWBCConditionView of WBCConditionImpl extends JDNodeView {
	public access:ro	WBCType				cond;
	public access:ro	KString				name;
	public access:ro	JENodeView			body;
	public access:ro	JMethodView			definer;
	public				CodeAttr			code_attr;

	public void generate(ConstPool constPool, Type reqType) {
		Code code = new Code(jctx_clazz, jctx_method, constPool);
		code.generation = true;
		code.cond_generation = true;
		if( cond == WBCType.CondInvariant ) {
			body.generate(code,Type.tpVoid);
			code.addInstr(Instr.op_return);
			return;
		}
		if( code_attr == null ) {
			JMethodView m = code.method;
			try {
				JVarView thisPar = null;
				if( !isStatic() ) {
					thisPar = new FormPar(pos,Constants.nameThis,jctx_clazz.concr_type,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD).getJVarView();
					code.addVar(thisPar);
				}
				code.addVars(m.params);
				if( cond==WBCType.CondEnsure && m.type.ret ≢ Type.tpVoid ) code.addVar(m.getRetVar());
				body.generate(code,Type.tpVoid);
				if( cond==WBCType.CondEnsure && m.type.ret ≢ Type.tpVoid ) code.removeVar(m.getRetVar());
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

