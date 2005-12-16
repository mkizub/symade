package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.vlang.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.CaseLabel.CaseLabelImpl;
import kiev.vlang.SwitchStat.SwitchStatImpl;
import kiev.vlang.CatchInfo.CatchInfoImpl;
import kiev.vlang.FinallyInfo.FinallyInfoImpl;
import kiev.vlang.TryStat.TryStatImpl;
import kiev.vlang.SynchronizedStat.SynchronizedStatImpl;
import kiev.vlang.WithStat.WithStatImpl;

@nodeview
public final view JCaseLabelView of CaseLabelImpl extends JENodeView {
	public access:ro	JENodeView		val;
	public access:ro	Type			type;
	public				CodeLabel		case_label;
	@getter public final JVarView[]		get$pattern()	{ return (JVarView[])this.$view.pattern.toJViewArray(JVarView.class); }
	@getter public final JENodeView[]	get$stats()		{ return (JENodeView[])this.$view.stats.toJViewArray(JENodeView.class); }

	public CodeLabel getLabel(Code code) {
		if (case_label == null || case_label.code != code) case_label = code.newLabel();
		return case_label;
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		case_label = getLabel(code);
		CodeSwitch cosw = ((SwitchStat)this.parent).getJSwitchStatView().cosw;
		try {
			code.addInstr(Instr.set_label,case_label);
			if( val == null ) cosw.addDefault(case_label);
			else {
				Object v = val.getConstValue();
				if( v instanceof Number )
					cosw.addCase( ((Number)v).intValue(), case_label);
				else if( v instanceof java.lang.Character )
					cosw.addCase( (int)((java.lang.Character)v).charValue(), case_label);
				else
					throw new RuntimeException("Case label "+v+" must be of integer type");
			}
		} catch(Exception e ) { Kiev.reportError(this,e); }
		Vector<JVarView> vars = null;
		if (pattern.length > 0) {
			vars = new Vector<JVarView>();
			foreach (JVarView p; pattern; p.vtype != null && p.name != nameUnderscore) {
				vars.append(p);
				p.generate(code,Type.tpVoid);
			}
		}
		for(int i=0; i < stats.length; i++) {
			try {
				stats[i].generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		if (vars != null)
			code.removeVars(vars.toArray());
	}
}

@nodeview
public view JSwitchStatView of SwitchStatImpl extends JENodeView implements BreakTarget {
	public access:ro	int						mode;
	public access:ro	JENodeView				sel;
	public access:ro	NArr<CaseLabel>			cases;
	public access:ro	JLVarExprView			tmpvar;
	public access:ro	JCaseLabelView			defCase;
	public access:ro	JFieldView				typehash; // needed for re-resolving
	public				JLabelView				lblcnt;
	public				JLabelView				lblbrk;
	public				CodeSwitch				cosw;

	public JLabelView getCntLabel() { return lblcnt; }
	public JLabelView getBrkLabel() { return lblbrk; }

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		int lo = Integer.MAX_VALUE;
		int hi = Integer.MIN_VALUE;

		int ntags = defCase==null? cases.length : cases.length-1;
		int[] tags = new int[ntags];

		for (int i=0, j=0; i < cases.length; i++) {
			if (((CaseLabel)cases[i]).val != null) {
				int val;
				Object v = cases[i].val.getConstValue();
				if( v instanceof Number )
					val = ((Number)v).intValue();
				else if( v instanceof java.lang.Character )
					val = (int)((java.lang.Character)v).charValue();
				else
					throw new RuntimeException("Case label "+v+" must be of integer type");
				tags[j++] = val;
				if (val < lo) lo = val;
				if (hi < val) hi = val;
			}
		}

		long table_space_cost = (long)4 + (hi - lo + 1); // words
		long table_time_cost = 3; // comparisons
		long lookup_space_cost = (long)3 + 2 * ntags;
		long lookup_time_cost = ntags;
		boolean tabswitch =
			table_space_cost + 3 * table_time_cost <=
			lookup_space_cost + 3 * lookup_time_cost;

		try {
			if( mode == SwitchStat.TYPE_SWITCH ) {
				lblcnt.generate(code,null);
				sel.generate(code,null);
			} else {
				sel.generate(code,null);
				lblcnt.generate(code,null);
			}
			if( tabswitch ) {
				cosw = code.newTableSwitch(lo,hi);
				code.addInstr(Instr.op_tableswitch,cosw);
			} else {
				qsort(tags,0,tags.length-1);
				cosw = code.newLookupSwitch(tags);
				code.addInstr(Instr.op_lookupswitch,cosw);
			}
			
			for(int i=0; i < cases.length; i++) {
				if( isAutoReturnable() )
					cases[i].setAutoReturnable(true);
				((CaseLabel)cases[i]).getJCaseLabelView().generate(code,Type.tpVoid);
			}
			Vector<Var> vars = new Vector<Var>();
			for(int i=0; i < cases.length; i++) {
				foreach (ENode n; cases[i].stats; n instanceof VarDecl)
					vars.append(((VarDecl)n).var);
			}
			code.removeVars(vars.toArray());

			lblbrk.generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	/** sort (int) arrays of keys and values
	 */
	static void qsort(int[] keys, int lo, int hi) {
		int i = lo;
		int j = hi;
		int pivot = keys[(i+j)/2];
		do {
			while (keys[i] < pivot) i++;
			while (pivot < keys[j]) j--;
			if (i <= j) {
				int temp = keys[i];
				keys[i] = keys[j];
				keys[j] = temp;
				i++;
				j--;
			}
		} while (i <= j);
		if (lo < j) qsort(keys, lo, j);
		if (i < hi) qsort(keys, i, hi);
	}
}


@nodeview
public view JCatchInfoView of CatchInfoImpl extends JENodeView {
	public access:ro	JVarView		arg;
	public access:ro	JENodeView		body;
	public				CodeLabel		handler;
	public				CodeCatchInfo	code_catcher;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		code.addVar(arg);
		JTryStatView tr = ((TryStat)parent).getJTryStatView();
		try {
			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.enter_catch_handler,code_catcher);
			code.addInstr(Instr.op_store,arg);
			body.generate(code,Type.tpVoid);
			if( !body.isMethodAbrupted() ) {
				if( ((TryStat)parent).finally_catcher != null ) {
					code.addInstr(Instr.op_jsr, tr.finally_catcher.subr_label);
				}
				if( isAutoReturnable() )
					JReturnStatView.generateReturn(code,this);
				else
					code.addInstr(Instr.op_goto, tr.end_label);
			}
			code.addInstr(Instr.exit_catch_handler,code_catcher);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		} finally {
			code.removeVar(arg);
		}
	}
}

@nodeview
public view JFinallyInfoView of FinallyInfoImpl extends JCatchInfoView {
	public access:ro	JVarView	ret_arg;
	public				CodeLabel	subr_label;

	public void generate(Code code, Type reqType) {
		try {
			CodeCatchInfo null_ci = null;
			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.set_label,handler);
			code.addInstr(Instr.enter_catch_handler,null_ci);
			code.addVar(arg);
			code.addInstr(Instr.op_store,arg);
			code.addInstr(Instr.op_jsr,subr_label);
			code.addInstr(Instr.op_load,arg);
			code.addInstr(Instr.op_throw);
			code.addInstr(Instr.exit_catch_handler,null_ci);

			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.set_label,subr_label);
			code.addInstr(Instr.enter_catch_handler,null_ci);
			code.addInstr(Instr.op_store,ret_arg);

			body.generate(code,Type.tpVoid);
			code.addInstr(Instr.op_ret,ret_arg);
		} catch(Exception e ) { Kiev.reportError(this,e);
		} finally { code.removeVar(arg); }
	}
}

@nodeview
public final view JTryStatView of TryStatImpl extends JENodeView {
	public access:ro	JENodeView			body;
	public access:ro	JFinallyInfoView	finally_catcher;
	public				CodeLabel			end_label;
	@getter public final JCatchInfoView[]	get$catchers()	{ return (JCatchInfoView[])this.$view.catchers.toJViewArray(JCatchInfoView.class); }

	public void generate(Code code, Type reqType) {
		// Generate labels for handlers
		if(finally_catcher != null) {
			code.addVar(finally_catcher.ret_arg);
			finally_catcher.handler = code.newLabel();
			finally_catcher.subr_label = code.newLabel();
			finally_catcher.subr_label.check = false;
			finally_catcher.code_catcher = code.newCatcher(finally_catcher.handler,null);
			code.addInstr(Instr.start_catcher,finally_catcher.code_catcher);
		}
		for(int i= catchers.length-1; i >= 0 ; i--) {
			catchers[i].handler = code.newLabel();
			catchers[i].code_catcher = code.newCatcher(catchers[i].handler,catchers[i].arg.type);
			code.addInstr(Instr.start_catcher,catchers[i].code_catcher);
		}
		end_label = code.newLabel();

		try {
			try {
				if( isAutoReturnable() )
					body.getENode().setAutoReturnable(true);
				body.generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() ) {
					JReturnStatView.generateReturn(code,this);
				} else {
					if( finally_catcher != null )
						code.addInstr(Instr.op_jsr,finally_catcher.subr_label);
					code.addInstr(Instr.op_goto,end_label);
				}
			}
			for(int i=0; i < catchers.length; i++) {
				code.addInstr(Instr.stop_catcher,catchers[i].code_catcher);
			}

			for(int i=0; i < catchers.length; i++) {
				if( isAutoReturnable() )
					catchers[i].getENode().setAutoReturnable(true);
				try {
					catchers[i].generate(code,Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(catchers[i],e);
				}
			}
			if(finally_catcher != null) {
				try {
					code.addInstr(Instr.stop_catcher,finally_catcher.code_catcher);
					finally_catcher.generate(code,Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(finally_catcher,e);
				}
			}
			code.addInstr(Instr.set_label,end_label);
		} finally {
			if(finally_catcher != null)
				code.removeVar(finally_catcher.ret_arg);
		}
	}
}

@nodeview
public final view JSynchronizedStatView of SynchronizedStatImpl extends JENodeView {
	public access:ro	JENodeView		expr;
	public access:ro	JVarView		expr_var;
	public access:ro	JENodeView		body;
	public				CodeLabel		handler;
	public				CodeCatchInfo	code_catcher;
	public				CodeLabel		end_label;

	public void generate(Code code, Type reqType) {
		expr.generate(code,null);
		try {
			code.addVar(expr_var);
			code.addInstr(Instr.op_dup);
			code.addInstr(Instr.op_store,expr_var);
			code.addInstr(Instr.op_monitorenter);
			handler = code.newLabel();
			end_label = code.newLabel();
			code_catcher = code.newCatcher(handler,null);
			code.addInstr(Instr.start_catcher,code_catcher);
			try {
				if( isAutoReturnable() )
					body.getENode().setAutoReturnable(true);
				body.generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
			code.addInstr(Instr.stop_catcher,code_catcher);
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() )
					JReturnStatView.generateReturn(code,this);
				else {
					code.addInstr(Instr.op_load,expr_var);
					code.addInstr(Instr.op_monitorexit);
					code.addInstr(Instr.op_goto,end_label);
				}
			}

			code.addInstr(Instr.set_label,handler);
			code.stack_push(Type.tpThrowable);
			code.addInstr(Instr.op_load,expr_var);
			code.addInstr(Instr.op_monitorexit);
			code.addInstr(Instr.op_throw);

			code.addInstr(Instr.set_label,end_label);
		} finally {
			code.removeVar(expr_var);
		}
	}
}

@nodeview
public final view JWithStatView of WithStatImpl extends JENodeView {
	public access:ro	JENodeView		expr;
	public access:ro	JENodeView		body;
	public access:ro	JLvalDNodeView	var_or_field;
	public				CodeLabel		end_label;

	public void generate(Code code, Type reqType) {
		end_label = code.newLabel();
		try {
			if (expr instanceof JAssignExprView)
				expr.generate(code,Type.tpVoid);
			if( isAutoReturnable() )
				body.getENode().setAutoReturnable(true);
			body.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		if( !body.isMethodAbrupted() ) {
			if( isAutoReturnable() )
				JReturnStatView.generateReturn(code,this);
		}

		code.addInstr(Instr.set_label,end_label);
	}
}

