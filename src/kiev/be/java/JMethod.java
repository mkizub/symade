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
		
	public Var	getRetVar() {
		if( this.$view.retvar == null )
			this.$view.retvar = new Var(pos,nameResultVar,type_ref.ret.getType(),ACC_FINAL);
		return this.$view.retvar;
	}


	public access:ro	Access				acc;
	public access:ro	NodeName			name;
	public access:ro	TypeCallRef			type_ref;
	public access:ro	TypeCallRef			dtype_ref;
	public access:ro	NArr<FormPar>		params;
	public access:ro	NArr<ASTAlias>		aliases;
	public				Var					retvar;
	public				BlockStat			body;
	public				Attr[]				attrs;
	public access:ro	NArr<WBCCondition>	conditions;
	public access:ro	NArr<Field>			violated_fields;
	public access:ro	MetaValue			annotation_default;
	public access:ro	boolean				inlined_by_dispatcher;

	@getter public final MethodType				get$type()	{ return type_ref.getMType(); }
	@getter public final MethodType				get$dtype()	{ return dtype_ref.getMType(); }
	@getter public final MethodType				get$jtype()	{ return (MethodType)dtype.getJavaType(); }

	public final boolean isMultiMethod()		{ return this.$view.is_mth_multimethod; }
	public final boolean isVirtualStatic()		{ return this.$view.is_mth_virtual_static; }
	public final boolean isVarArgs()			{ return this.$view.is_mth_varargs; }
	public final boolean isRuleMethod()		{ return this.$view.is_mth_rule; }
	public final boolean isOperatorMethod()	{ return this.$view.is_mth_operator; }
	public final boolean isNeedFieldInits()	{ return this.$view.is_mth_need_fields_init; }
	public final boolean isInvariantMethod()	{ return this.$view.is_mth_invariant; }
	public final boolean isLocalMethod()		{ return this.$view.is_mth_local; }

	@getter public JMethodView get$child_jctx_method() { return this; }

	public FormPar getOuterThisParam() { return getMethod().getOuterThisParam(); }
	public FormPar getTypeInfoParam() { return getMethod().getTypeInfoParam(); }
	public FormPar getVarArgParam() { return getMethod().getVarArgParam(); }
	
	public CodeLabel getBreakLabel() {
		return ((BlockStat)body).getJBlockStatView().getBreakLabel();
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
		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			foreach(Field f; violated_fields; jctx_clazz.getStruct().instanceOf((Struct)f.parent) ) {
				foreach(Method inv; f.invs; jctx_clazz.getStruct().instanceOf((Struct)inv.parent) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(name.name.startsWith(nameSet) || name.name.startsWith(nameGet)) )
						conditions.addUniq(inv.conditions[0]);
				}
			}
		}
		foreach(WBCCondition cond; conditions; cond.cond != WBCType.CondInvariant )
			cond.getJWBCConditionView().generate(constPool,Type.tpVoid);
		if( !isAbstract() && body != null ) {
			Code code = new Code(jctx_clazz.getStruct(), this.getMethod(), constPool);
			code.generation = true;
			try {
				if( !isBad() ) {
					FormPar thisPar = null;
					if( !isStatic() ) {
						thisPar = new FormPar(pos,Constants.nameThis,jctx_clazz.type,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD);
						code.addVar(thisPar);
					}
					if( params.length > 0 ) code.addVars(params.toArray());
					if( Kiev.verify /*&& jtype != null*/ )
						generateArgumentCheck(code);
					if( Kiev.debugOutputC ) {
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire )
							code.importCode(cond.code_attr);
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondInvariant ) {
							assert( cond.parent instanceof Method && ((Method)cond.parent).isInvariantMethod() );
							if( !name.name.equals(nameInit) && !name.name.equals(nameClassInit) ) {
								if( !((DNode)cond.parent).isStatic() )
									code.addInstrLoadThis();
								code.addInstr(Instr.op_call,(Method)cond.parent,false);
							}
							code.need_to_gen_post_cond = true;
						}
						if( !code.need_to_gen_post_cond ) {
							foreach(WBCCondition cond; conditions; cond.cond != WBCType.CondRequire ) {
								code.need_to_gen_post_cond = true;
								break;
							}
						}
					}
					body.getJENodeView().generate(code,Type.tpVoid);
					if( Kiev.debugOutputC && code.need_to_gen_post_cond ) {
						if( type.ret != Type.tpVoid ) {
							code.addVar(getRetVar());
							code.addInstr(Instr.op_store,getRetVar().getJVarView());
						}
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondInvariant ) {
							if( !((DNode)cond.parent).isStatic() )
								code.addInstrLoadThis();
							code.addInstr(Instr.op_call,(Method)cond.parent,false);
							code.need_to_gen_post_cond = true;
						}
						foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondEnsure )
							code.importCode(cond.code_attr);
						if( type.ret != Type.tpVoid ) {
							code.addInstr(Instr.op_load,getRetVar().getJVarView());
							code.addInstr(Instr.op_return);
							code.removeVar(getRetVar());
						} else {
							code.addInstr(Instr.op_return);
						}
					}
					if( params.length > 0 ) code.removeVars(params.toArray());
					if( thisPar != null ) code.removeVar(thisPar);
				} else {
					code.addInstr(Instr.op_new,Type.tpError);
					code.addInstr(Instr.op_dup);
					KString msg = KString.from("Compiled with errors");
					constPool.addStringCP(msg);
					code.addConst(msg);
					Method func = Type.tpError.clazz.resolveMethod(nameInit,KString.from("(Ljava/lang/String;)V"));
					code.addInstr(Instr.op_call,func,false);
					code.addInstr(Instr.op_throw);
				}
				code.generateCode();
			} catch(Exception e) {
				Kiev.reportError(this,e);
			}
		}
		MetaThrows throwns = getMethod().getMetaThrows();
        if( throwns != null ) {
			ASTNode[] mthrs = throwns.getThrowns();
        	Type[] thrs = new Type[mthrs.length];
			for (int i=0; i < mthrs.length; i++)
				thrs[i] = mthrs[i].getType();
        	ExceptionsAttr athr = new ExceptionsAttr();
        	athr.exceptions = thrs;
			this.addAttr(athr);
        }
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params.length; i++) {
			Type tp1 = jtype.args[i];
			Type tp2 = params[i].type;
			if !(tp2.getJavaType().isInstanceOf(tp1)) {
				code.addInstr(Instr.op_load,params[i].getJVarView());
				code.addInstr(Instr.op_checkcast,tp1);
				code.addInstr(Instr.op_store,params[i].getJVarView());
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
	public				CodeAttr			code_attr;

	public void generate(ConstPool constPool, Type reqType) {
		Code code = new Code(jctx_clazz.getStruct(), jctx_method.getMethod(), constPool);
		code.generation = true;
		code.cond_generation = true;
		if( cond == WBCType.CondInvariant ) {
			body.generate(code,Type.tpVoid);
			code.addInstr(Instr.op_return);
			return;
		}
		if( code_attr == null ) {
			Method m = code.method;
			try {
				FormPar thisPar = null;
				if( !isStatic() ) {
					thisPar = new FormPar(pos,Constants.nameThis,jctx_clazz.type,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD);
					code.addVar(thisPar);
				}
				if( m.params.length > 0 ) code.addVars(m.params.toArray());
				if( cond==WBCType.CondEnsure && m.type.ret != Type.tpVoid ) code.addVar(m.getRetVar());
				body.generate(code,Type.tpVoid);
				if( cond==WBCType.CondEnsure && m.type.ret != Type.tpVoid ) code.removeVar(m.getRetVar());
				if( m.params.length > 0 ) code.removeVars(m.params.toArray());
				if( thisPar != null ) code.removeVar(thisPar);
				code.generateCode((WBCCondition)this.getNode());
			} catch(Exception e) {
				Kiev.reportError(this,e);
			}
			return;
		}
		code_attr.generate(constPool);
	}
}

