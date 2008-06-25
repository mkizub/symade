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

import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

@ViewOf(vcast=true, iface=true)
public final view JCaseLabel of CaseLabel extends JENode {

	public static final class ExtRefAttrSlot_case_label extends ExtRefAttrSlot {
		ExtRefAttrSlot_case_label() { super("case-label", TypeInfo.newTypeInfo(CodeLabel.class,null)); }
		public CodeLabel getLabel(JNode parent) { return (CodeLabel)get((ASTNode)parent); }
	}
	public static final ExtRefAttrSlot_case_label CASE_LABEL_ATTR = new ExtRefAttrSlot_case_label();
	

	public:ro	JENode			val;
	public:ro	Type			ctype;
	public:ro	JVar[]			pattern;

	public CodeLabel getLabel(Code code) {
		CodeLabel label = JCaseLabel.CASE_LABEL_ATTR.getLabel(this);
		if (label == null || label.code != code) {
			label = code.newLabel();
			JCaseLabel.CASE_LABEL_ATTR.set((CaseLabel)this,label);
		}
		return label;
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		CodeLabel case_label = getLabel(code);
		CodeSwitch cosw = JSwitchStat.CODE_SWITCH_ATTR.getCodeSwitch((JSwitchStat)this.jparent);
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
		if (this.jparent instanceof JMatchStat) {
			foreach (JVar p; pattern; p.vtype != null && p.sname != nameUnderscore)
				p.generate(code,Type.tpVoid);
		}
	}
	public void removeVars(Code code) {
		if (pattern.length > 0) {
			Vector<JVar> vars = new Vector<JVar>();
			foreach (JVar p; pattern; p.vtype != null && p.sname != nameUnderscore)
				vars.append(p);
			code.removeVars(vars.toArray());
		}
	}

	public void backendCleanup() {
		JCaseLabel.CASE_LABEL_ATTR.clear((CaseLabel)this);
	}

}

class SwitchInfo {
	int[] tags;
	boolean tabswitch;
	int lo = Integer.MAX_VALUE;
	int hi = Integer.MIN_VALUE;
}

@ViewOf(vcast=true, iface=true)
public view JSwitchStat of SwitchStat extends JBlock implements BreakTarget {

	public static final class ExtRefAttrSlot_code_switch extends ExtRefAttrSlot {
		ExtRefAttrSlot_code_switch() { super("code-switch", TypeInfo.newTypeInfo(CodeSwitch.class,null)); }
		public CodeSwitch getCodeSwitch(JSwitchStat parent) { return (CodeSwitch)get((SwitchStat)parent); }
	}
	public static final ExtRefAttrSlot_code_switch CODE_SWITCH_ATTR = new ExtRefAttrSlot_code_switch();
	
	public:ro	JENode				sel;
	public:ro	JCaseLabel[]		cases;
	public:ro	JCaseLabel			defCase;
	public:ro	JENode				sel_to_int;
	public:ro	JLabel				lblcnt;

	public JLabel getCntLabel() {
		if( lblcnt == null )
			((SwitchStat)this).lblcnt = new Label();
		return lblcnt;
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchInfo si = makeSwitchInfo(this);
		
		try {
			sel.generate(code,null);
			getCntLabel().generate(code,null);

			CodeSwitch cosw;
			if( si.tabswitch ) {
				cosw = code.newTableSwitch(si.lo,si.hi);
				code.addInstr(Instr.op_tableswitch,cosw);
			} else {
				cosw = code.newLookupSwitch(si.tags);
				code.addInstr(Instr.op_lookupswitch,cosw);
			}
			CODE_SWITCH_ATTR.set((SwitchStat)this,cosw);

			generateStats(code,Type.tpVoid);

			getBrkLabel().generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	static SwitchInfo makeSwitchInfo(JSwitchStat sw) {
		SwitchInfo si = new SwitchInfo();

		int ntags = sw.defCase==null? sw.cases.length : sw.cases.length-1;
		si.tags = new int[ntags];

		for (int i=0, j=0; i < sw.cases.length; i++) {
			if (sw.cases[i].val != null) {
				int val;
				Object v = sw.cases[i].val.getConstValue();
				if( v instanceof Number )
					val = ((Number)v).intValue();
				else if( v instanceof java.lang.Character )
					val = (int)((java.lang.Character)v).charValue();
				else
					throw new RuntimeException("Case label "+v+" must be of integer type");
				si.tags[j++] = val;
				if (val < si.lo) si.lo = val;
				if (val > si.hi) si.hi = val;
			}
		}
		long table_space_cost = (long)4 + (si.hi - si.lo + 1); // words
		long table_time_cost = 3; // comparisons
		long lookup_space_cost = (long)3 + 2 * ntags;
		long lookup_time_cost = ntags;
		si.tabswitch =
			table_space_cost + 3 * table_time_cost <=
			lookup_space_cost + 3 * lookup_time_cost;

		if (!si.tabswitch)
			qsort(si.tags,0,si.tags.length-1);

		return si;
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

	public void backendCleanup() {
		CODE_SWITCH_ATTR.clear((SwitchStat)this);
		((SwitchStat)this).lblbrk = null;
		((SwitchStat)this).lblcnt = null;
	}
}

@ViewOf(vcast=true, iface=true)
public view JSwitchEnumStat of SwitchEnumStat extends JSwitchStat {

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchInfo si = makeSwitchInfo(this);
		
		sel.generate(code,null);
		getCntLabel().generate(code,null);
		sel_to_int.generate(code,null);

		CodeSwitch cosw;
		if( si.tabswitch ) {
			cosw = code.newTableSwitch(si.lo,si.hi);
			code.addInstr(Instr.op_tableswitch,cosw);
		} else {
			cosw = code.newLookupSwitch(si.tags);
			code.addInstr(Instr.op_lookupswitch,cosw);
		}
		CODE_SWITCH_ATTR.set((SwitchStat)this,cosw);

		generateStats(code,Type.tpVoid);

		getBrkLabel().generate(code,null);
		code.addInstr(Instr.switch_close,cosw);
	}
}

@ViewOf(vcast=true, iface=true)
public view JSwitchTypeStat of SwitchTypeStat extends JSwitchStat {

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchInfo si = makeSwitchInfo(this);
		
		try {
			sel.generate(code,null);
			getCntLabel().generate(code,null);
			sel_to_int.generate(code,null);

			CodeSwitch cosw;
			cosw = code.newTableSwitch(si.lo,si.hi);
			code.addInstr(Instr.op_tableswitch,cosw);
			CODE_SWITCH_ATTR.set((SwitchStat)this,cosw);

			generateStats(code,Type.tpVoid);

			getBrkLabel().generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@ViewOf(vcast=true, iface=true)
public view JMatchStat of MatchStat extends JSwitchStat {

	public:ro	JVar				tmp_var;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchInfo si = makeSwitchInfo(this);
		
		try {
			sel.generate(code,null);
			getCntLabel().generate(code,null);
			code.addInstr(Instr.op_dup);
			code.addVar(tmp_var);
			code.addInstr(Instr.op_store,tmp_var);
			sel_to_int.generate(code,null);

			CodeSwitch cosw;
			if( si.tabswitch ) {
				cosw = code.newTableSwitch(si.lo,si.hi);
				code.addInstr(Instr.op_tableswitch,cosw);
			} else {
				cosw = code.newLookupSwitch(si.tags);
				code.addInstr(Instr.op_lookupswitch,cosw);
			}
			CODE_SWITCH_ATTR.set((SwitchStat)this,cosw);

			generateStats(code,Type.tpVoid);

			getBrkLabel().generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
			code.removeVar(tmp_var);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}


@ViewOf(vcast=true, iface=true)
public view JCatchInfo of CatchInfo extends JENode {

	public:ro	JVar			arg;
	public:ro	JENode			body;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		code.addVar(arg);
		JTryStat tr = (JTryStat)this.jparent;
		try {
			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.enter_catch_handler,JTryStat.CODE_CATCHER_ATTR.getCatcher(this));
			code.addInstr(Instr.op_store,arg);
			body.generate(code,Type.tpVoid);
			if( !body.isAbrupted() ) {
				if( tr.finally_catcher != null ) {
					code.addInstr(Instr.op_jsr, JTryStat.SUBR_LABEL_ATTR.getLabel(tr.finally_catcher));
				}
				if( isAutoReturnable() )
					JReturnStat.generateReturn(code,this);
				else
					code.addInstr(Instr.op_goto, JTryStat.END_LABEL_ATTR.getLabel(tr));
			}
			code.addInstr(Instr.exit_catch_handler,JTryStat.CODE_CATCHER_ATTR.getCatcher(this));
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		} finally {
			code.removeVar(arg);
		}
	}

	public void backendCleanup() {
		JTryStat.HANDLER_ATTR.clear((CatchInfo)this);
		JTryStat.CODE_CATCHER_ATTR.clear((CatchInfo)this);
	}
}

@ViewOf(vcast=true, iface=true)
public view JFinallyInfo of FinallyInfo extends JENode {
	public:ro	JVar			ret_arg;
	public:ro	JENode			body;

	public void generate(Code code, Type reqType) {
		JVar arg = (JVar)new LVar(pos,"",Type.tpThrowable,Var.VAR_LOCAL,0);
		try {
			CodeCatchInfo null_ci = null;
			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.set_label,JTryStat.HANDLER_ATTR.getLabel(this));
			code.addInstr(Instr.enter_catch_handler,null_ci);
			code.addVar(arg);
			code.addInstr(Instr.op_store,arg);
			code.addInstr(Instr.op_jsr,JTryStat.SUBR_LABEL_ATTR.getLabel(this));
			code.addInstr(Instr.op_load,arg);
			code.addInstr(Instr.op_throw);
			code.addInstr(Instr.exit_catch_handler,null_ci);

			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.set_label,JTryStat.SUBR_LABEL_ATTR.getLabel(this));
			code.addInstr(Instr.enter_catch_handler,null_ci);
			code.addInstr(Instr.op_store,ret_arg);

			body.generate(code,Type.tpVoid);
			code.addInstr(Instr.op_ret,ret_arg);
		} catch(Exception e ) { Kiev.reportError(this,e);
		} finally { code.removeVar(arg); }
	}

	public void backendCleanup() {
		JTryStat.HANDLER_ATTR.clear((FinallyInfo)this);
		JTryStat.SUBR_LABEL_ATTR.clear((FinallyInfo)this);
		JTryStat.CODE_CATCHER_ATTR.clear((FinallyInfo)this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JTryStat of TryStat extends JENode {

	public static final class ExtRefAttrSlot_handler extends ExtRefAttrSlot {
		ExtRefAttrSlot_handler() { super("handler-label", TypeInfo.newTypeInfo(CodeLabel.class,null)); }
		public CodeLabel getLabel(JNode parent) { return (CodeLabel)get((ASTNode)parent); }
	}
	public static final ExtRefAttrSlot_handler HANDLER_ATTR = new ExtRefAttrSlot_handler();
	
	public static final class ExtRefAttrSlot_end_label extends ExtRefAttrSlot {
		ExtRefAttrSlot_end_label() { super("end-label", TypeInfo.newTypeInfo(CodeLabel.class,null)); }
		public CodeLabel getLabel(JNode parent) { return (CodeLabel)get((ASTNode)parent); }
	}
	public static final ExtRefAttrSlot_end_label END_LABEL_ATTR = new ExtRefAttrSlot_end_label();

	public static final class ExtRefAttrSlot_subr_label extends ExtRefAttrSlot {
		ExtRefAttrSlot_subr_label() { super("end-label", TypeInfo.newTypeInfo(CodeLabel.class,null)); }
		public CodeLabel getLabel(JNode parent) { return (CodeLabel)get((ASTNode)parent); }
	}
	public static final ExtRefAttrSlot_subr_label SUBR_LABEL_ATTR = new ExtRefAttrSlot_subr_label();

	public static final class ExtRefAttrSlot_code_catcher extends ExtRefAttrSlot {
		ExtRefAttrSlot_code_catcher() { super("end-label", TypeInfo.newTypeInfo(CodeLabel.class,null)); }
		public CodeCatchInfo getCatcher(JNode parent) { return (CodeCatchInfo)get((ASTNode)parent); }
	}
	public static final ExtRefAttrSlot_code_catcher CODE_CATCHER_ATTR = new ExtRefAttrSlot_code_catcher();

	public:ro	JENode				body;
	public:ro	JCatchInfo[]		catchers;
	public:ro	JFinallyInfo		finally_catcher;

	public void generate(Code code, Type reqType) {
		// Generate labels for handlers
		if(finally_catcher != null) {
			code.addVar(finally_catcher.ret_arg);
			CodeLabel handler = code.newLabel();
			HANDLER_ATTR.set((FinallyInfo)finally_catcher, handler);
			CodeLabel subr_label = code.newLabel();
			subr_label.check = false;
			SUBR_LABEL_ATTR.set((FinallyInfo)finally_catcher, subr_label);
			CodeCatchInfo code_catcher = code.newCatcher(handler,null);
			CODE_CATCHER_ATTR.set((FinallyInfo)finally_catcher,code_catcher);
			code.addInstr(Instr.start_catcher,code_catcher);
		}
		for(int i= catchers.length-1; i >= 0 ; i--) {
			CodeLabel handler = code.newLabel();
			HANDLER_ATTR.set((CatchInfo)catchers[i], handler);
			CodeCatchInfo code_catcher = code.newCatcher(handler,catchers[i].arg.vtype.getJType());
			CODE_CATCHER_ATTR.set((CatchInfo)catchers[i],code_catcher);
			code.addInstr(Instr.start_catcher,code_catcher);
		}
		CodeLabel end_label = code.newLabel();
		END_LABEL_ATTR.set((TryStat)this,end_label);

		try {
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() ) {
					JReturnStat.generateReturn(code,this);
				} else {
					if( finally_catcher != null )
						code.addInstr(Instr.op_jsr,SUBR_LABEL_ATTR.getLabel(finally_catcher));
					code.addInstr(Instr.op_goto,end_label);
				}
			}
			for(int i=0; i < catchers.length; i++) {
				code.addInstr(Instr.stop_catcher,CODE_CATCHER_ATTR.getCatcher(catchers[i]));
			}

			for(int i=0; i < catchers.length; i++) {
				if( isAutoReturnable() )
					catchers[i].setAutoReturnable(true);
				try {
					catchers[i].generate(code,Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(catchers[i],e);
				}
			}
			if(finally_catcher != null) {
				try {
					code.addInstr(Instr.stop_catcher,CODE_CATCHER_ATTR.getCatcher(finally_catcher));
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

	public void backendCleanup() {
		END_LABEL_ATTR.clear((TryStat)this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JSynchronizedStat of SynchronizedStat extends JENode {
	public:ro	JENode			expr;
	public:ro	JVar			expr_var;
	public:ro	JENode			body;

	public void generate(Code code, Type reqType) {
		expr.generate(code,null);
		try {
			code.addVar(expr_var);
			code.addInstr(Instr.op_dup);
			code.addInstr(Instr.op_store,expr_var);
			code.addInstr(Instr.op_monitorenter);
			CodeLabel handler = code.newLabel();
			JTryStat.HANDLER_ATTR.set((SynchronizedStat)this, handler);
			CodeLabel end_label = code.newLabel();
			CodeCatchInfo code_catcher = code.newCatcher(handler,null);
			JTryStat.CODE_CATCHER_ATTR.set((SynchronizedStat)this,code_catcher);
			code.addInstr(Instr.start_catcher,code_catcher);
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(this,e);
			}
			code.addInstr(Instr.stop_catcher,code_catcher);
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() )
					JReturnStat.generateReturn(code,this);
				else {
					code.addInstr(Instr.op_load,expr_var);
					code.addInstr(Instr.op_monitorexit);
					code.addInstr(Instr.op_goto,end_label);
				}
			}

			code.addInstr(Instr.set_label,handler);
			code.stack_push(JType.tpThrowable);
			code.addInstr(Instr.op_load,expr_var);
			code.addInstr(Instr.op_monitorexit);
			code.addInstr(Instr.op_throw);

			code.addInstr(Instr.set_label,end_label);
		} finally {
			code.removeVar(expr_var);
		}
	}

	public void backendCleanup() {
		JTryStat.HANDLER_ATTR.clear((SynchronizedStat)this);
		JTryStat.CODE_CATCHER_ATTR.clear((SynchronizedStat)this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JWithStat of WithStat extends JENode {
	public:ro	JENode		expr;
	public:ro	JENode		body;
	public:ro	JVar		var_or_field;

	public void generate(Code code, Type reqType) {
		CodeLabel end_label = code.newLabel();
		try {
			if (expr instanceof JAssignExpr)
				expr.generate(code,Type.tpVoid);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		if( !body.isMethodAbrupted() ) {
			if( isAutoReturnable() )
				JReturnStat.generateReturn(code,this);
		}

		code.addInstr(Instr.set_label,end_label);
	}
}

