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

public interface BreakTarget {
	public JLabel getBrkLabel();
}

public interface ContinueTarget {
	public JLabel getCntLabel();
}

public final class JLabel extends JDNode {
	public CodeLabel code_label;

	public static JLabel attachJLabel(Label impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JLabel)jn;
		return new JLabel(impl);
	}
	
	protected JLabel(Label impl) {
		super(impl);
	}

	public CodeLabel getCodeLabel(Code code) {
		if (code_label == null || code_label.code != code)
			code_label = code.newLabel();
		return code_label;
	}

	public void generate(Code code, Type reqType) {
		code.addInstr(Instr.set_label,getCodeLabel(code));
	}

	public void backendCleanup() {
		code_label = null;
		super.backendCleanup();
	}
}

public abstract class JLoopStat extends JENode implements BreakTarget, ContinueTarget {

	@virtual typedef VT  ≤ LoopStat;

	public final JLabel lblcnt;
	public final JLabel lblbrk;

	public static JLoopStat attach(LoopStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JLoopStat)jn;
		if (impl instanceof WhileStat)
			return JWhileStat.attach((WhileStat)impl);
		if (impl instanceof DoWhileStat)
			return JDoWhileStat.attach((DoWhileStat)impl);
		if (impl instanceof ForStat)
			return JForStat.attach((ForStat)impl);
		if (impl instanceof ForEachStat)
			return JForEachStat.attach((ForEachStat)impl);
		return new JLoopStat(impl);
	}
	
	protected JLoopStat(LoopStat impl) {
		super(impl);
		lblcnt = (JLabel)impl.lblcnt;
		lblbrk = (JLabel)impl.lblbrk;
	}
	
	public final JLabel getCntLabel() { return lblcnt; }
	public final JLabel getBrkLabel() { return lblbrk; }
}

public final class JWhileStat extends JLoopStat {

	@virtual typedef VT  ≤ WhileStat;

	public static JWhileStat attach(WhileStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JWhileStat)jn;
		return new JWhileStat(impl);
	}
	
	protected JWhileStat(WhileStat impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating WhileStat");
		code.setLinePos(this);
		WhileStat vn = vn();
		JENode cond = (JENode)vn.cond;
		JENode body = (JENode)vn.body;
		try {
			lblcnt.getCodeLabel(code);
			lblbrk.getCodeLabel(code);
			CodeLabel body_label = code.newLabel();

			code.addInstr(Instr.op_goto,lblcnt.getCodeLabel(code));
			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,code.tenv.tpVoid);
			lblcnt.generate(code,code.tenv.tpVoid);

			if( cond.isConstantExpr(code.env) ) {
				if( ((Boolean)cond.getConstValue(code.env)).booleanValue() ) {
					code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				JBoolExpr.gen_iftrue(code, cond, body_label);
			}
			lblbrk.generate(code,code.tenv.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JDoWhileStat extends JLoopStat {

	@virtual typedef VT  ≤ DoWhileStat;

	public static JDoWhileStat attach(DoWhileStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JDoWhileStat)jn;
		return new JDoWhileStat(impl);
	}
	
	protected JDoWhileStat(DoWhileStat impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating DoWhileStat");
		code.setLinePos(this);
		DoWhileStat vn = vn();
		JENode cond = (JENode)vn.cond;
		JENode body = (JENode)vn.body;
		try {
			lblcnt.getCodeLabel(code);
			lblbrk.getCodeLabel(code);
			CodeLabel body_label = code.newLabel();

// Differ from WhileStat in this:	code.addInstr(Instr.op_goto,continue_label);
			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,code.tenv.tpVoid);
			lblcnt.generate(code,code.tenv.tpVoid);

			if( cond.isConstantExpr(code.env) ) {
				if( ((Boolean)cond.getConstValue(code.env)).booleanValue() ) {
					code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				JBoolExpr.gen_iftrue(code, cond, body_label);
			}
			lblbrk.generate(code,code.tenv.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JForStat extends JLoopStat {

	@virtual typedef VT  ≤ ForStat;

	public static JForStat attach(ForStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JForStat)jn;
		return new JForStat(impl);
	}
	
	protected JForStat(ForStat impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ForStat");
		lblcnt.getCodeLabel(code);
		lblbrk.getCodeLabel(code);
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this);
		ForStat vn = vn();
		JENode cond = (JENode)vn.cond;
		JENode body = (JENode)vn.body;
		JENode iter = (JENode)vn.iter;
		try {
			JNode[] inits = JNode.toJArray<JNode>(vn.inits);
			for (int i=0; i < inits.length; i++) {
				JNode jn = inits[i];
				if (jn instanceof JVar)
					((JVar)jn).generate(code,code.tenv.tpVoid);
				else if (jn instanceof JENode)
					((JENode)jn).generate(code,code.tenv.tpVoid);
			}

			if( cond != null ) {
				code.addInstr(Instr.op_goto,check_label);
			}

			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,code.tenv.tpVoid);

			lblcnt.generate(code,code.tenv.tpVoid);
			if( iter != null )
				iter.generate(code,code.tenv.tpVoid);

			code.addInstr(Instr.set_label,check_label);
			if( cond != null ) {
				if( cond.isConstantExpr(code.env) && ((Boolean)cond.getConstValue(code.env)).booleanValue() )
					code.addInstr(Instr.op_goto,body_label);
				else if( cond.isConstantExpr(code.env) && !((Boolean)cond.getConstValue(code.env)).booleanValue() );
				else JBoolExpr.gen_iftrue(code, cond, body_label);
			} else {
				code.addInstr(Instr.op_goto,body_label);
			}
			lblbrk.generate(code,code.tenv.tpVoid);

			for (int i=inits.length-1; i >= 0; i--) {
				JNode jn = inits[i];
				if (jn instanceof JVar)
					((JVar)jn).removeVar(code);
			}
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JForEachStat extends JLoopStat {

	@virtual typedef VT  ≤ ForEachStat;

	public static JForEachStat attach(ForEachStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JForEachStat)jn;
		return new JForEachStat(impl);
	}
	
	protected JForEachStat(ForEachStat impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ForEachStat");
		lblcnt.getCodeLabel(code);
		lblbrk.getCodeLabel(code);
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this);
		ForEachStat vn = vn();
		JVar var = (JVar)vn.var;
		JVar iter = (JVar)vn.iter;
		JVar iter_array = (JVar)vn.iter_array;
		JENode iter_init = (JENode)vn.iter_init;
		JENode iter_cond = (JENode)vn.iter_cond;
		JENode var_init = (JENode)vn.var_init;
		JENode cond = (JENode)vn.cond;
		JENode body = (JENode)vn.body;
		JENode iter_incr = (JENode)vn.iter_incr;
		try {
			if( iter != null )
				code.addVar(iter);
			if( var != null )
				code.addVar(var);
			if( iter_array != null )
				code.addVar(iter_array);

			// Init iterator
			iter_init.generate(code,code.tenv.tpVoid);

			// Goto check
			code.addInstr(Instr.op_goto,check_label);

			// Start body - set var, check cond, do body
			code.addInstr(Instr.set_label,body_label);

			if( var_init != null)
				var_init.generate(code,code.tenv.tpVoid);
			if( cond != null )
				JBoolExpr.gen_iffalse(code, cond, lblcnt.getCodeLabel(code));

			body.generate(code,code.tenv.tpVoid);

			// Continue - iterate iterator and check iterator condition
			lblcnt.generate(code,code.tenv.tpVoid);
			if( iter_incr != null )
				iter_incr.generate(code,code.tenv.tpVoid);

			// Just check iterator condition
			code.addInstr(Instr.set_label,check_label);
			if( iter_cond != null )
				JBoolExpr.gen_iftrue(code, iter_cond, body_label);

			if( iter_array != null )
				code.removeVar(iter_array);
			if( var != null )
				code.removeVar(var);
			if( iter != null )
				code.removeVar(iter);

			lblbrk.generate(code,code.tenv.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}

}

