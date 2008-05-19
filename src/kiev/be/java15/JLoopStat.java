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

public interface BreakTarget {
	public JLabel getBrkLabel();
}

public interface ContinueTarget {
	public JLabel getCntLabel();
}

@ViewOf(vcast=true, iface=true)
public abstract view JLoopStat of LoopStat extends JENode implements BreakTarget, ContinueTarget {
	public:ro	JLabel				lblcnt;
	public:ro	JLabel				lblbrk;

	public final JLabel getCntLabel() { return lblcnt; }
	public final JLabel getBrkLabel() { return lblbrk; }
}

@ViewOf(vcast=true, iface=true)
public final view JLabel of Label extends JDNode {
	public:ro	List<ASTNode>		links;
	public		CodeLabel			label;

	public CodeLabel getCodeLabel(Code code) {
		if( label == null  || label.code != code) label = code.newLabel();
		return label;
	}
	public void generate(Code code, Type reqType) {
		code.addInstr(Instr.set_label,getCodeLabel(code));
	}
}

@ViewOf(vcast=true, iface=true)
public final view JWhileStat of WhileStat extends JLoopStat {
	public:ro	JENode		cond;
	public:ro	JENode		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating WhileStat");
		code.setLinePos(this);
		try {
			lblcnt.label = code.newLabel();
			lblbrk.label = code.newLabel();
			CodeLabel body_label = code.newLabel();

			code.addInstr(Instr.op_goto,lblcnt.label);
			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);
			lblcnt.generate(code,Type.tpVoid);

			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				JBoolExpr.gen_iftrue(code, cond, body_label);
			}
			lblbrk.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@ViewOf(vcast=true, iface=true)
public final view JDoWhileStat of DoWhileStat extends JLoopStat {
	public:ro	JENode		cond;
	public:ro	JENode		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating DoWhileStat");
		code.setLinePos(this);
		try {
			lblcnt.label = code.newLabel();
			lblbrk.label = code.newLabel();
			CodeLabel body_label = code.newLabel();

// Differ from WhileStat in this:	code.addInstr(Instr.op_goto,continue_label);
			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);
			lblcnt.generate(code,Type.tpVoid);

			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				JBoolExpr.gen_iftrue(code, cond, body_label);
			}
			lblbrk.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@ViewOf(vcast=true, iface=true)
public final view JForStat of ForStat extends JLoopStat {
	public:ro	JNode		init;
	public:ro	JENode		cond;
	public:ro	JENode		body;
	public:ro	JENode		iter;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ForStat");
		lblcnt.label = code.newLabel();
		lblbrk.label = code.newLabel();
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this);
		try {
			if( init instanceof JVar )
				((JVar)init).generate(code,Type.tpVoid);
			else if (init instanceof JDeclGroup)
				((JDeclGroup)init).generate(code,Type.tpVoid);
			else if (init instanceof JENode)
				((JENode)init).generate(code,Type.tpVoid);

			if( cond != null ) {
				code.addInstr(Instr.op_goto,check_label);
			}

			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);

			lblcnt.generate(code,Type.tpVoid);
			if( iter != null )
				iter.generate(code,Type.tpVoid);

			code.addInstr(Instr.set_label,check_label);
			if( cond != null ) {
				if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() )
					code.addInstr(Instr.op_goto,body_label);
				else if( cond.isConstantExpr() && !((Boolean)cond.getConstValue()).booleanValue() );
				else JBoolExpr.gen_iftrue(code, cond, body_label);
			} else {
				code.addInstr(Instr.op_goto,body_label);
			}
			lblbrk.generate(code,Type.tpVoid);

			if (init instanceof JVar)
				((JVar)init).removeVar(code);
			else if (init instanceof JDeclGroup)
				((JDeclGroup)init).removeVars(code);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@ViewOf(vcast=true, iface=true)
public final view JForEachStat of ForEachStat extends JLoopStat {
	public:ro	int				mode;
	public:ro	JENode		container;
	public:ro	JVar		var;
	public:ro	JVar		iter;
	public:ro	JVar		iter_array;
	public:ro	JENode		iter_init;
	public:ro	JENode		iter_cond;
	public:ro	JENode		var_init;
	public:ro	JENode		cond;
	public:ro	JENode		body;
	public:ro	JENode		iter_incr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ForEachStat");
		lblcnt.label = code.newLabel();
		lblbrk.label = code.newLabel();
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this);
		try {
			if( iter != null )
				code.addVar(iter);
			if( var != null )
				code.addVar(var);
			if( iter_array != null )
				code.addVar(iter_array);

			// Init iterator
			iter_init.generate(code,Type.tpVoid);

			// Goto check
			code.addInstr(Instr.op_goto,check_label);

			// Start body - set var, check cond, do body
			code.addInstr(Instr.set_label,body_label);

			if( var_init != null)
				var_init.generate(code,Type.tpVoid);
			if( cond != null )
				JBoolExpr.gen_iffalse(code, cond, lblcnt.label);

			body.generate(code,Type.tpVoid);

			// Continue - iterate iterator and check iterator condition
			lblcnt.generate(code,Type.tpVoid);
			if( iter_incr != null )
				iter_incr.generate(code,Type.tpVoid);

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

			lblbrk.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

}

