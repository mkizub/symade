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

public final class JCaseLabel extends JENode {

	@virtual typedef VT  ≤ CaseLabel;

	private CodeLabel case_label;
	public final int case_value;
	public final boolean has_value;

	public static JCaseLabel attach(CaseLabel impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCaseLabel)jn;
		return new JCaseLabel(impl);
	}
	
	protected JCaseLabel(CaseLabel impl) {
		super(impl);
		if (impl.val != null) {
			has_value = true;
			Object v = impl.val.getConstValue(Env.getEnv());
			if (v instanceof Number)
				case_value = ((Number)v).intValue();
			else if (v instanceof Character)
				case_value = (int)((Character)v).charValue();
		}
	}
	
	public CodeLabel getLabel(Code code) {
		if (case_label == null || case_label.code != code)
			case_label = code.newLabel();
		return case_label;
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		CaseLabel vn = vn();
		CodeLabel case_label = getLabel(code);
		CodeSwitch cosw = ((JSwitchStat)this.jparent).code_switch;
		code.addInstr(Instr.set_label,case_label);
		if (!has_value)
			cosw.addDefault(case_label);
		else
			cosw.addCase(case_value, case_label);
		if (vn.parent() instanceof MatchStat) {
				JVar[] pattern = JNode.toJArray<JVar>(vn.pattern);
				foreach (JVar p; pattern; p.vtype != null && p.sname != nameUnderscore)
					p.generate(code,code.tenv.tpVoid);
		}
	}
	public void removeVars(Code code) {
		CaseLabel vn = vn();
		if (vn.parent() instanceof MatchStat) {
			JVar[] pattern = JNode.toJArray<JVar>(vn.pattern);
			if (pattern.length > 0) {
				Vector<JVar> vars = new Vector<JVar>();
				foreach (JVar p; pattern; p.vtype != null && p.sname != nameUnderscore)
					vars.append(p);
				code.removeVars(vars.toArray());
			}
		}
	}

	public void backendCleanup() {
		case_label = null;
		super.backendCleanup();
	}

}

class SwitchInfo {
	int[] tags;
	boolean tabswitch;
	int lo = Integer.MAX_VALUE;
	int hi = Integer.MIN_VALUE;
}

public class JSwitchStat extends JBlock implements BreakTarget {

	@virtual typedef VT  ≤ SwitchStat;

	public static JSwitchStat attach(SwitchStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JSwitchStat)jn;
		if (impl instanceof SwitchEnumStat)
			return JSwitchEnumStat.attach((SwitchEnumStat)impl);
		if (impl instanceof SwitchTypeStat)
			return JSwitchTypeStat.attach((SwitchTypeStat)impl);
		if (impl instanceof MatchStat)
			return JMatchStat.attach((MatchStat)impl);
		return new JSwitchStat(impl);
	}
	
	protected JSwitchStat(SwitchStat impl) {
		super(impl);
	}

	//public:ro	JENode				sel;
	//public:ro	JCaseLabel[]		cases;
	//public:ro	JCaseLabel			defCase;
	//public:ro	JENode				sel_to_int;
	//public:ro	JLabel				lblcnt;
	
	CodeSwitch code_switch;

	public JLabel getBrkLabel() {
		SwitchStat vn = vn();
		Label lblbrk = vn.lblbrk;
		if (lblbrk == null)
			vn.lblbrk = lblbrk = new Label();
		return (JLabel)lblbrk;
	}

	public JLabel getCntLabel() {
		SwitchStat vn = vn();
		Label lblcnt = vn.lblcnt;
		if (lblcnt == null)
			vn.lblcnt = lblcnt = new Label();
		return (JLabel)lblcnt;
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchStat vn = vn();
		SwitchInfo si = makeSwitchInfo(code, this);
		
		try {
			((JENode)vn.sel).generate(code,null);
			getCntLabel().generate(code,null);

			CodeSwitch cosw;
			if( si.tabswitch ) {
				cosw = code.newTableSwitch(si.lo,si.hi);
				code.addInstr(Instr.op_tableswitch,cosw);
			} else {
				cosw = code.newLookupSwitch(si.tags);
				code.addInstr(Instr.op_lookupswitch,cosw);
			}
			code_switch = cosw;

			generateStats(code,code.tenv.tpVoid);

			getBrkLabel().generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}

	static SwitchInfo makeSwitchInfo(Code code, JSwitchStat sw) {
		SwitchInfo si = new SwitchInfo();

		SwitchStat vn = sw.vn();
		JCaseLabel[] sw_cases = JNode.toJArray<JCaseLabel>(vn.cases);
		int ntags = vn.defCase==null? sw_cases.length : sw_cases.length-1;
		si.tags = new int[ntags];
		int j=0;
		foreach (JCaseLabel cl; sw_cases; cl.has_value) {
			int val = cl.case_value;
			si.tags[j++] = val;
			if (val < si.lo) si.lo = val;
			if (val > si.hi) si.hi = val;
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
		code_switch = null;
		SwitchStat vn = vn();
		vn.lblbrk = null;
		vn.lblcnt = null;
		super.backendCleanup();
	}
}


public final class JSwitchEnumStat extends JSwitchStat {

	@virtual typedef VT  ≤ SwitchEnumStat;

	public static JSwitchEnumStat attach(SwitchEnumStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JSwitchEnumStat)jn;
		return new JSwitchEnumStat(impl);
	}
	
	protected JSwitchEnumStat(SwitchEnumStat impl) {
		super(impl);
	}


	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchEnumStat vn = vn();
		SwitchInfo si = makeSwitchInfo(code, this);
		
		((JENode)vn.sel).generate(code,null);
		getCntLabel().generate(code,null);
		((JENode)vn.sel_to_int).generate(code,null);

		CodeSwitch cosw;
		if( si.tabswitch ) {
			cosw = code.newTableSwitch(si.lo,si.hi);
			code.addInstr(Instr.op_tableswitch,cosw);
		} else {
			cosw = code.newLookupSwitch(si.tags);
			code.addInstr(Instr.op_lookupswitch,cosw);
		}
		code_switch = cosw;

		generateStats(code,code.tenv.tpVoid);

		getBrkLabel().generate(code,null);
		code.addInstr(Instr.switch_close,cosw);
	}
}

public final class JSwitchTypeStat extends JSwitchStat {

	@virtual typedef VT  ≤ SwitchTypeStat;

	public static JSwitchTypeStat attach(SwitchTypeStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JSwitchTypeStat)jn;
		return new JSwitchTypeStat(impl);
	}
	
	protected JSwitchTypeStat(SwitchTypeStat impl) {
		super(impl);
	}


	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		SwitchStat vn = vn();
		SwitchInfo si = makeSwitchInfo(code, this);
		
		try {
			((JENode)vn.sel).generate(code,null);
			getCntLabel().generate(code,null);
			((JENode)vn.sel_to_int).generate(code,null);

			CodeSwitch cosw;
			cosw = code.newTableSwitch(si.lo,si.hi);
			code.addInstr(Instr.op_tableswitch,cosw);
			code_switch = cosw;

			generateStats(code,code.tenv.tpVoid);

			getBrkLabel().generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JMatchStat extends JSwitchStat {

	@virtual typedef VT  ≤ MatchStat;

	public static JMatchStat attach(MatchStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JMatchStat)jn;
		return new JMatchStat(impl);
	}
	
	protected JMatchStat(MatchStat impl) {
		super(impl);
	}

	//public:ro	JVar				tmp_var;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);

		MatchStat vn = vn();
		SwitchInfo si = makeSwitchInfo(code, this);
		
		try {
			((JENode)vn.sel).generate(code,null);
			getCntLabel().generate(code,null);
			code.addInstr(Instr.op_dup);
			JVar tmp_var = (JVar)vn.tmp_var;
			code.addVar(tmp_var);
			code.addInstr(Instr.op_store,tmp_var);
			((JENode)vn.sel_to_int).generate(code,null);

			CodeSwitch cosw;
			if( si.tabswitch ) {
				cosw = code.newTableSwitch(si.lo,si.hi);
				code.addInstr(Instr.op_tableswitch,cosw);
			} else {
				cosw = code.newLookupSwitch(si.tags);
				code.addInstr(Instr.op_lookupswitch,cosw);
			}
			code_switch = cosw;

			generateStats(code,code.tenv.tpVoid);

			getBrkLabel().generate(code,null);
			code.addInstr(Instr.switch_close,cosw);
			code.removeVar(tmp_var);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}


public class JCatchInfo extends JENode {

	@virtual typedef VT  ≤ CatchInfo;

	public final JVar arg;
	public CodeLabel handler;
	public CodeCatchInfo code_catcher;

	public static JCatchInfo attach(CatchInfo impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCatchInfo)jn;
		return new JCatchInfo(impl);
	}
	
	protected JCatchInfo(CatchInfo impl) {
		super(impl);
		arg = (JVar)impl.arg;
	}
	
	public void generate(Code code, Type reqType) {
		CatchInfo vn = vn();
		JENode body = (JENode)vn.body;
		code.setLinePos(this);
		code.addVar(arg);
		JTryStat tr = (JTryStat)this.jparent;
		try {
			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.enter_catch_handler,code_catcher);
			code.addInstr(Instr.op_store,arg);
			body.generate(code,code.tenv.tpVoid);
			if( !body.isAbrupted() ) {
				if( tr.finally_catcher != null ) {
					code.addInstr(Instr.op_jsr, tr.finally_catcher.subr_label);
				}
				if( isAutoReturnable() )
					JReturnStat.generateReturn(code,this);
				else
					code.addInstr(Instr.op_goto, tr.end_label);
			}
			code.addInstr(Instr.exit_catch_handler,code_catcher);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		} finally {
			code.removeVar(arg);
		}
	}

	public void backendCleanup() {
		handler = null;
		code_catcher = null;
		super.backendCleanup();
	}
}

public class JFinallyInfo extends JENode {

	@virtual typedef VT  ≤ FinallyInfo;

	public final JVar ret_arg;
	public CodeLabel handler;
	public CodeCatchInfo code_catcher;
	public CodeLabel subr_label;
	
	public static JFinallyInfo attach(FinallyInfo impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JFinallyInfo)jn;
		return new JFinallyInfo(impl);
	}
	
	protected JFinallyInfo(FinallyInfo impl) {
		super(impl);
		ret_arg = (JVar)impl.ret_arg;
	}
	
	public void generate(Code code, Type reqType) {
		FinallyInfo vn = vn();
		JENode body = (JENode)vn.body;
		JVar arg = (JVar)new LVar(0,"",code.tenv.tpThrowable,Var.VAR_LOCAL,0);
		try {
			CodeCatchInfo null_ci = null;
			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.set_label,handler);
			code.addInstr(Instr.enter_catch_handler,null_ci);
			code.addVar(arg);
			code.addInstr(Instr.op_store,arg);
			code.addInstr(Instr.op_jsr,this.subr_label);
			code.addInstr(Instr.op_load,arg);
			code.addInstr(Instr.op_throw);
			code.addInstr(Instr.exit_catch_handler,null_ci);

			// This label must be created by TryStat's generate routine;
			code.addInstr(Instr.set_label,this.subr_label);
			code.addInstr(Instr.enter_catch_handler,null_ci);
			code.addInstr(Instr.op_store,ret_arg);

			body.generate(code,code.tenv.tpVoid);
			code.addInstr(Instr.op_ret,ret_arg);
		} catch(Exception e ) { Kiev.reportError(vn,e);
		} finally { code.removeVar(arg); }
	}

	public void backendCleanup() {
		handler = null;
		subr_label = null;
		code_catcher = null;
		super.backendCleanup();
	}
}

public final class JTryStat extends JENode {


	@virtual typedef VT  ≤ TryStat;

	public final JCatchInfo[]	catchers;
	public final JFinallyInfo		finally_catcher;
	public CodeLabel				end_label;

	public static JTryStat attach(TryStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JTryStat)jn;
		return new JTryStat(impl);
	}
	
	protected JTryStat(TryStat impl) {
		super(impl);
		catchers = JNode.toJArray<JCatchInfo>(impl.catchers);
		finally_catcher = (JFinallyInfo)impl.finally_catcher;
	}
	
	public void generate(Code code, Type reqType) {
		TryStat vn = vn();
		JENode body = (JENode)vn.body;
		// Generate labels for handlers
		if(finally_catcher != null) {
			code.addVar(finally_catcher.ret_arg);
			CodeLabel handler = code.newLabel();
			finally_catcher.handler = handler;
			CodeLabel subr_label = code.newLabel();
			subr_label.check = false;
			finally_catcher.subr_label = subr_label;
			CodeCatchInfo code_catcher = code.newCatcher(handler,null);
			finally_catcher.code_catcher = code_catcher;
			code.addInstr(Instr.start_catcher,code_catcher);
		}
		for(int i= catchers.length-1; i >= 0 ; i--) {
			CodeLabel handler = code.newLabel();
			catchers[i].handler = handler;
			CodeCatchInfo code_catcher = code.newCatcher(handler,code.jtenv.getJType(catchers[i].arg.vtype));
			catchers[i].code_catcher = code_catcher;
			code.addInstr(Instr.start_catcher,code_catcher);
		}
		CodeLabel end_label = code.newLabel();
		this.end_label = end_label;

		try {
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(code,code.tenv.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(vn,e);
			}
			if( !body.isMethodAbrupted() ) {
				if( isAutoReturnable() ) {
					JReturnStat.generateReturn(code,this);
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
					catchers[i].setAutoReturnable(true);
				try {
					catchers[i].generate(code,code.tenv.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(catchers[i].vn(),e);
				}
			}
			if(finally_catcher != null) {
				try {
					code.addInstr(Instr.stop_catcher,finally_catcher.code_catcher);
					finally_catcher.generate(code,code.tenv.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(finally_catcher.vn(),e);
				}
			}
			code.addInstr(Instr.set_label,end_label);
		} finally {
			if(finally_catcher != null)
				code.removeVar(finally_catcher.ret_arg);
		}
	}

	public void backendCleanup() {
		end_label = null;
		super.backendCleanup();
	}
}

public final class JSynchronizedStat extends JENode {

	@virtual typedef VT  ≤ SynchronizedStat;

	public final JVar expr_var;
	public CodeLabel handler;
	public CodeCatchInfo code_catcher;

	public static JSynchronizedStat attach(SynchronizedStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JSynchronizedStat)jn;
		return new JSynchronizedStat(impl);
	}
	
	protected JSynchronizedStat(SynchronizedStat impl) {
		super(impl);
		expr_var = (JVar)impl.expr_var;
	}
	
	public void generate(Code code, Type reqType) {
		SynchronizedStat vn = vn();
		JENode expr = (JENode)vn.expr;
		JENode body = (JENode)vn.body;
		expr.generate(code,null);
		try {
			code.addVar(expr_var);
			code.addInstr(Instr.op_dup);
			code.addInstr(Instr.op_store,expr_var);
			code.addInstr(Instr.op_monitorenter);
			CodeLabel handler = code.newLabel();
			this.handler = handler;
			CodeLabel end_label = code.newLabel();
			CodeCatchInfo code_catcher = code.newCatcher(handler,null);
			this.code_catcher = code_catcher;
			code.addInstr(Instr.start_catcher,code_catcher);
			try {
				if( isAutoReturnable() )
					body.setAutoReturnable(true);
				body.generate(code,code.tenv.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(vn,e);
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
			code.stack_push(code.jenv.getJTypeEnv().tpThrowable);
			code.addInstr(Instr.op_load,expr_var);
			code.addInstr(Instr.op_monitorexit);
			code.addInstr(Instr.op_throw);

			code.addInstr(Instr.set_label,end_label);
		} finally {
			code.removeVar(expr_var);
		}
	}

	public void backendCleanup() {
		handler = null;
		code_catcher = null;
		super.backendCleanup();
	}
}

public final class JWithStat extends JENode {

	@virtual typedef VT  ≤ WithStat;

	public static JWithStat attach(WithStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JWithStat)jn;
		return new JWithStat(impl);
	}
	
	protected JWithStat(WithStat impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		CodeLabel end_label = code.newLabel();
		WithStat vn = vn();
		JENode expr = (JENode)vn.expr;
		JENode body = (JENode)vn.body;
		try {
			if (expr instanceof JAssignExpr)
				expr.generate(code,code.tenv.tpVoid);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,code.tenv.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
		if( !body.isMethodAbrupted() ) {
			if( isAutoReturnable() )
				JReturnStat.generateReturn(code,this);
		}

		code.addInstr(Instr.set_label,end_label);
	}
}

