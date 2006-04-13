package kiev.be.java15;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.NArr.JArr;

import static kiev.be.java15.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public abstract view JLoopStat of LoopStat extends JENode implements BreakTarget, ContinueTarget {
	public:ro	JLabel				lblcnt;
	public:ro	JLabel				lblbrk;

	public final JLabel getCntLabel() { return lblcnt; }
	public final JLabel getBrkLabel() { return lblbrk; }
}

@nodeview
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

@nodeview
public final view JWhileStat of WhileStat extends JLoopStat {
	public:ro	JENode		cond;
	public:ro	JENode		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating WhileStat");
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

@nodeview
public final view JDoWhileStat of DoWhileStat extends JLoopStat {
	public:ro	JENode		cond;
	public:ro	JENode		body;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating DoWhileStat");
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

@nodeview
public final view JForInit of ForInit extends JENode {
	public:ro	JArr<JVar>	decls;
}

@nodeview
public final view JForStat of ForStat extends JLoopStat {
	public:ro	JENode		init;
	public:ro	JENode		cond;
	public:ro	JENode		body;
	public:ro	JENode		iter;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ForStat");
		lblcnt.label = code.newLabel();
		lblbrk.label = code.newLabel();
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this);
		try {
			if( init != null ) {
				if( init instanceof JForInit ) {
					JForInit fi = (JForInit)init;
					foreach (JVar var; fi.decls) {
						var.generate(code,Type.tpVoid);
					}
				} else {
					init.generate(code,Type.tpVoid);
				}
			}

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

			if( init != null && init instanceof JForInit ) {
				JForInit fi = (JForInit)init;
				JVar[] decls = fi.decls.toArray();
				for(int i=decls.length-1; i >= 0; i--) {
					code.removeVar(decls[i]);
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@nodeview
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
		trace(Kiev.debugStatGen,"\tgenerating ForEachStat");
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

