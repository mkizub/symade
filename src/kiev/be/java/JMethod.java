package kiev.be.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.*;

import kiev.be.java.JMethodView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static class JMethodView extends JDNodeView {
	final Method.MethodImpl impl;
	public JMethodView(Method.MethodImpl impl) {
		super(impl);
		this.impl = impl;
	}

	final Method getMethod() { return this.impl.getMethod(); }
		
	public Var	getRetVar() {
		if( this.impl.retvar == null )
			this.impl.retvar = new Var(pos,nameResultVar,type_ref.ret.getType(),ACC_FINAL);
		return this.impl.retvar;
	}


	@getter public final Access					get$acc()					{ return this.impl.acc; }
	@getter public final NodeName				get$name()					{ return this.impl.name; }
	@getter public final TypeCallRef			get$type_ref()				{ return this.impl.type_ref; }
	@getter public final TypeCallRef			get$dtype_ref()				{ return this.impl.dtype_ref; }
	@getter public final NArr<FormPar>			get$params()				{ return this.impl.params; }
	@getter public final NArr<ASTAlias>		get$aliases()				{ return this.impl.aliases; }
	@getter public final Var					get$retvar()				{ return this.impl.retvar; }
	@getter public final BlockStat				get$body()					{ return this.impl.body; }
	@getter public final Attr[]					get$attrs()					{ return this.impl.attrs; }
	@getter public final NArr<WBCCondition>	get$conditions()			{ return this.impl.conditions; }
	@getter public final NArr<Field>			get$violated_fields()		{ return this.impl.violated_fields; }
	@getter public final MetaValue				get$annotation_default()	{ return this.impl.annotation_default; }
	@getter public final boolean				get$inlined_by_dispatcher()	{ return this.impl.inlined_by_dispatcher; }

	@getter public final MethodType				get$type()	{ return type_ref.getMType(); }
	@getter public final MethodType				get$dtype()	{ return dtype_ref.getMType(); }
	@getter public final MethodType				get$jtype()	{ return (MethodType)dtype.getJavaType(); }

	@setter public final void set$retvar(Var val)							{ this.impl.retvar = val; }
	@setter public final void set$body(BlockStat val)						{ this.impl.body = val; }
	@setter public final void set$attrs(Attr[] val)						{ this.impl.attrs = val; }

	public final boolean isMultiMethod() { return this.impl.is_mth_multimethod; }
	public final boolean isVirtualStatic() { return this.impl.is_mth_virtual_static; }
	public final boolean isVarArgs() { return this.impl.is_mth_varargs; }
	public final boolean isRuleMethod() { return this.impl.is_mth_rule; }
	public final boolean isOperatorMethod() { return this.impl.is_mth_operator; }
	public final boolean isNeedFieldInits() { return this.impl.is_mth_need_fields_init; }
	public final boolean isInvariantMethod() { return this.impl.is_mth_invariant; }
	public final boolean isLocalMethod() { return this.impl.is_mth_local; }

	public void generate(ConstPool constPool) {
		if( Kiev.debug ) System.out.println("\tgenerating Method "+this);
		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			foreach(Field f; violated_fields; pctx.clazz.instanceOf((Struct)f.parent) ) {
				foreach(Method inv; f.invs; pctx.clazz.instanceOf((Struct)inv.parent) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(name.name.startsWith(nameSet) || name.name.startsWith(nameGet)) )
						conditions.addUniq(inv.conditions[0]);
				}
			}
		}
		foreach(WBCCondition cond; conditions; cond.cond != WBCType.CondInvariant )
			cond.generate(constPool,Type.tpVoid);
		if( !isAbstract() && body != null ) {
			Code code = new Code(pctx.clazz, this.getMethod(), constPool);
			code.generation = true;
			try {
				if( !isBad() ) {
					FormPar thisPar = null;
					if( !isStatic() ) {
						thisPar = new FormPar(pos,Constants.nameThis,pctx.clazz.type,FormPar.PARAM_THIS,ACC_FINAL|ACC_FORWARD);
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
					body.generate(code,Type.tpVoid);
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
