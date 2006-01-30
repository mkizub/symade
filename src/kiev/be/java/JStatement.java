package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.NArr.JArr;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.InlineMethodStat.InlineMethodStatImpl;
import kiev.vlang.InlineMethodStat.ParamRedir;
import kiev.vlang.Block.BlockImpl;
import kiev.vlang.EmptyStat.EmptyStatImpl;
import kiev.vlang.ExprStat.ExprStatImpl;
import kiev.vlang.ReturnStat.ReturnStatImpl;
import kiev.vlang.ThrowStat.ThrowStatImpl;
import kiev.vlang.IfElseStat.IfElseStatImpl;
import kiev.vlang.CondStat.CondStatImpl;
import kiev.vlang.LabeledStat.LabeledStatImpl;
import kiev.vlang.BreakStat.BreakStatImpl;
import kiev.vlang.ContinueStat.ContinueStatImpl;
import kiev.vlang.GotoStat.GotoStatImpl;
import kiev.vlang.GotoCaseStat.GotoCaseStatImpl;

@nodeview
public final view JInlineMethodStat of InlineMethodStatImpl extends JENode {
	public access:ro	JMethod		method;
	public access:ro	ParamRedir[]	params_redir;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		code.setLinePos(this);
		if( Kiev.verify )
			generateArgumentCheck(code);
		foreach (ParamRedir redir; params_redir)
			redir.old_var.getJView().bcpos = redir.new_var.getJView().bcpos;
		method.body.generate(code,reqType);
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params_redir.length; i++) {
			ParamRedir redir = params_redir[i];
			if( !redir.new_var.type.equals(method.params[i].type) ) {
				code.addInstr(Instr.op_load,redir.new_var.getJView());
				code.addInstr(Instr.op_checkcast,method.params[i].type);
				code.addInstr(Instr.op_store,redir.new_var.getJView());
			}
		}
	}
}

@nodeview
public final view JEmptyStat of EmptyStatImpl extends JENode {
	public JEmptyStat(EmptyStatImpl $view) { super($view); }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating EmptyStat");
//		code.setLinePos(this);
//		code.addInstr(Instr.op_nop);
	}
}

@nodeview
public final final view JExprStat of ExprStatImpl extends JENode {
	public access:ro	JENode		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ExprStat");
		try {
			expr.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
	}
}

@nodeview
public final view JReturnStat of ReturnStatImpl extends JENode {
	public access:ro	JENode		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ReturnStat");
		code.setLinePos(this);
		try {
			if( expr != null )
				expr.generate(code,code.method.type.ret());
			generateReturn(code,this);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public static void generateReturn(Code code, JNode from) {
		JVar tmp_var = null;
		for(JNode node = from; node != null; node = node.jparent) {
			if (node instanceof JMethod)
				break;
			else if (node instanceof JFinallyInfo) {
				assert (node.jparent instanceof JTryStat);
				node = node.jparent; // skip TryStat that is parent of FinallyInfo
				continue;
			}
			else if (node instanceof JTryStat) {
				if( node.finally_catcher != null ) {
					if( tmp_var==null && code.method.type.ret() ≢ Type.tpVoid ) {
						tmp_var = new Var(0,KString.Empty,code.method.type.ret(),0).getJView();
						code.addVar(tmp_var);
						code.addInstr(Instr.op_store,tmp_var);
					}
					code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
				}
			}
			else if (node instanceof JSynchronizedStat) {
				if( tmp_var==null && code.method.type.ret() ≢ Type.tpVoid ) {
					tmp_var = new Var(0,KString.Empty,code.method.type.ret(),0).getJView();
					code.addVar(tmp_var);
					code.addInstr(Instr.op_store,tmp_var);
				}
				code.addInstr(Instr.op_load,node.expr_var);
				code.addInstr(Instr.op_monitorexit);
			}
		}
		if( tmp_var != null ) {
			code.addInstr(Instr.op_load,tmp_var);
			code.removeVar(tmp_var);
		}
		if( code.need_to_gen_post_cond ) {
			code.addInstr(Instr.op_goto,code.method.getBreakLabel());
			if( code.method.type.ret() ≢ Type.tpVoid )
				code.stack_pop();
		} else
			code.addInstr(Instr.op_return);
	}
}

@nodeview
public final view JThrowStat of ThrowStatImpl extends JENode {
	public access:ro	JENode		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ThrowStat");
		code.setLinePos(this);
		try {
			expr.generate(code,null);
			code.addInstr(Instr.op_throw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@nodeview
public final view JIfElseStat of IfElseStatImpl extends JENode {
	public access:ro	JENode		cond;
	public access:ro	JENode		thenSt;
	public access:ro	JENode		elseSt;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating IfElseStat");
		code.setLinePos(this);
		try {
			if( cond.isConstantExpr() ) {
				JENode cond = this.cond;
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					if( isAutoReturnable() )
						thenSt.setAutoReturnable(true);
					thenSt.generate(code,Type.tpVoid);
				}
				else if( elseSt != null ) {
					if( isAutoReturnable() )
						elseSt.setAutoReturnable(true);
					elseSt.generate(code,Type.tpVoid);
				}
			} else {
				CodeLabel else_label = code.newLabel();
				JBoolExpr.gen_iffalse(code, cond, else_label);
				thenSt.generate(code,Type.tpVoid);
				if( elseSt != null ) {
					CodeLabel end_label = code.newLabel();
					if( !thenSt.isMethodAbrupted() ) {
						if( isAutoReturnable() )
							JReturnStat.generateReturn(code,this);
						else if (!thenSt.isAbrupted())
							code.addInstr(Instr.op_goto,end_label);
					}
					code.addInstr(Instr.set_label,else_label);
					elseSt.generate(code,Type.tpVoid);
					code.addInstr(Instr.set_label,end_label);
				} else {
					code.addInstr(Instr.set_label,else_label);
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@nodeview
public final view JCondStat of CondStatImpl extends JENode {
	public access:ro	JENode		cond;
	public access:ro	JENode		message;

	private void generateAssertName(Code code) {
		JWBCCondition wbc = (JWBCCondition)jparent.jparent;
		if( wbc.name == null ) return;
		code.addConst((KString)wbc.name);
	}

	private JMethod getAssertMethod() {
		KString fname;
		JWBCCondition wbc = (JWBCCondition)jparent.jparent;
		switch( wbc.cond ) {
		case WBCType.CondRequire:	fname = nameAssertRequireMethod;
		case WBCType.CondEnsure:	fname = nameAssertEnsureMethod;
		case WBCType.CondInvariant:	fname = nameAssertInvariantMethod;
		default: fname = nameAssertMethod;
		}
		Method func;
		if( wbc.name == null )
			func = Type.tpDebug.clazz.resolveMethod(fname,Type.tpVoid,Type.tpString);
		else
			func = Type.tpDebug.clazz.resolveMethod(fname,Type.tpVoid,Type.tpString,Type.tpString);
		return func.getJView();
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating CondStat");
		code.setLinePos(this);
		try {
			if(cond.isConstantExpr() ) {
				JENode cond = this.cond;
				if( ((Boolean)cond.getConstValue()).booleanValue() );
				else {
					generateAssertName(code);
					message.generate(code,Type.tpString);
					code.addInstr(Instr.op_call,getAssertMethod(),false);
				}
			} else {
				CodeLabel else_label = code.newLabel();
				JBoolExpr.gen_iftrue(code, cond, else_label);
				generateAssertName(code);
				message.generate(code,Type.tpString);
				code.addInstr(Instr.op_call,getAssertMethod(),false);
				code.addInstr(Instr.set_label,else_label);
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

@nodeview
public final view JLabeledStat of LabeledStatImpl extends JENode {
	public access:ro	KString			ident;
	public access:ro	JLabel		lbl;
	public access:ro	JENode		stat;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating LabeledStat");
		code.setLinePos(this);
		try {
			lbl.generate(code,Type.tpVoid);
			stat.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat,e);
		}
	}

	public CodeLabel getCodeLabel(Code code) {
		return lbl.getCodeLabel(code);
	}
}

@nodeview
public final view JBreakStat of BreakStatImpl extends JENode {
	public access:ro	KString			ident;
	public access:ro	JLabel		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BreakStat");
		code.setLinePos(this);
		try {
			Object[] lb = resolveBreakLabel(code);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel ) {
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				}
				else {
					code.addInstr(Instr.op_load,((Var)lb[i]).getJView());
					code.addInstr(Instr.op_monitorexit);
				}
			if( isAutoReturnable() )
				JReturnStat.generateReturn(code,this);
			else
				code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	private Object[] resolveBreakLabel(Code code) {
		KString name = ident==null?null:ident;
		Object[] cl = new Object[0];
		if( name == null || name.equals(KString.Empty) ) {
			// Search for loop statements
			for(JNode node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethod ) break;
				if( node instanceof BreakTarget || node instanceof JBlock );
				else continue;
				if( node instanceof BreakTarget ) {
					BreakTarget t = (BreakTarget)node;
					return (Object[])Arrays.append(cl,t.getBrkLabel().getCodeLabel(code));
				}
				else if( node instanceof JBlock && node.isBreakTarget() ){
					JBlock t = (JBlock)node;
					return (Object[])Arrays.append(cl,t.getBreakLabel());
				}
			}
			throw new RuntimeException("Break not within loop statement");
		} else {
			// Search for labels with loop/switch statement
			for(JNode node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethod ) break;
				if( node instanceof JLabeledStat && ((JLabeledStat)node).ident == ident ) {
					JENode st = ((JLabeledStat)node).stat;
					if( st instanceof BreakTarget )
						return (Object[])Arrays.append(cl,st.getBrkLabel().getCodeLabel(code));
					else if (st instanceof JBlock)
						return (Object[])Arrays.append(cl,st.getBreakLabel());
					else
						throw new RuntimeException("Label "+name+" does not refer to break target");
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a break target");
	}
}

@nodeview
public final view JContinueStat of ContinueStatImpl extends JENode {
	public access:ro	KString			ident;
	public access:ro	JLabel		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ContinueStat");
		code.setLinePos(this);
		try {
			Object[] lb = resolveContinueLabel(code);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					code.addInstr(Instr.op_load,((Var)lb[i]).getJView());
					code.addInstr(Instr.op_monitorexit);
				}
			code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	private Object[] resolveContinueLabel(Code code) {
		KString name = ident==null?null:ident;
		Object[] cl = new Object[0];
		if( name == null || name.equals(KString.Empty) ) {
			// Search for loop statements
			for(JNode node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethod ) break;
				if( node instanceof ContinueTarget )
					return (Object[])Arrays.append(cl,node.getCntLabel().getCodeLabel(code));
			}
			throw new RuntimeException("Continue not within loop statement");
		} else {
			// Search for labels with loop statement
			for(JNode node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethod ) break;
				if( node instanceof JLabeledStat && ((JLabeledStat)node).ident == ident ) {
					JENode st = ((JLabeledStat)node).stat;
					if( st instanceof ContinueTarget )
						return (Object[])Arrays.append(cl,st.getCntLabel().getCodeLabel(code));
					throw new RuntimeException("Label "+name+" does not refer to continue target");
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a continue target");
	}
}

@nodeview
public final view JGotoStat of GotoStatImpl extends JENode {
	public access:ro	KString			ident;
	public access:ro	JLabel		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoStat");
//		JLabeledStat[] stats = resolveStat(ident, code.method.body, JLabeledStat.emptyArray);
//		if( stats.length == 0 )
//			throw new CompilerException(this,"Label "+ident+" unresolved");
//		if( stats.length > 1 )
//			throw new CompilerException(this,"Umbigouse label "+ident+" in goto statement");
//		LabeledStat stat = stats[0];
//		if( stat == null )
//			throw new CompilerException(this,"Label "+ident+" unresolved");
		code.setLinePos(this);
		try {
			Object[] lb = resolveLabelStat(code,(JLabeledStat)dest.jparent);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					code.addInstr(Instr.op_load,(JVar)lb[i]);
					code.addInstr(Instr.op_monitorexit);
				}
			code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public Object[] resolveLabelStat(Code code, JLabeledStat stat) {
		Object[] cl1 = new CodeLabel[0];
		Object[] cl2 = new CodeLabel[0];
		JNode st = stat;
		while( !(st instanceof JMethod) ) {
			if( st instanceof JFinallyInfo ) {
				st = st.jparent.jparent;
				continue;
			}
			else if( st instanceof JTryStat ) {
				JTryStat ts = (JTryStat)st;
				if( ts.finally_catcher != null )
					cl1 = (Object[])Arrays.append(cl1,ts.finally_catcher.subr_label);
			}
			else if( st instanceof JSynchronizedStat ) {
				cl1 = (Object[])Arrays.append(cl1,st.expr_var);
			}
			st = st.jparent;
		}
		st = this;
		while( !(st instanceof JMethod) ) {
			if( st instanceof JFinallyInfo ) {
				st = st.jparent.jparent;
				continue;
			}
			if( st instanceof JTryStat ) {
				JTryStat ts = (JTryStat)st;
				if( ts.finally_catcher != null )
					cl2 = (Object[])Arrays.append(cl2,ts.finally_catcher.subr_label);
			}
			else if( st instanceof JSynchronizedStat ) {
				cl2 = (Object[])Arrays.append(cl2, st.expr_var);
			}
			st = st.jparent;
		}
		int i = 0;
		for(; i < cl2.length && i < cl1.length; i++ )
			if( cl1[i] != cl2[i] ) break;
		Object[] cl3 = new Object[ cl2.length - i + 1 ];
		System.arraycopy(cl2,i,cl3,0,cl3.length-1);
		cl3[cl3.length-1] = stat.getCodeLabel(code);
		return cl3;
	}
}

@nodeview
public final view JGotoCaseStat of GotoCaseStatImpl extends JENode {
	public access:ro	JENode			expr;
	public access:ro	JSwitchStat		sw;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoCaseStat");
		code.setLinePos(this);
		try {
			if( !expr.isConstantExpr() ) {
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					expr.generate(code,Type.tpVoid);
				else
					expr.generate(code,null);
			}

			JVar tmp_var = null;
			for(JNode node = this.jparent; node != null; node = node.jparent) {
				if (node.getNode() == sw.getNode())
					break;
				if (node instanceof JFinallyInfo) {
					node = node.jparent; // skip calling jsr if we are in it
					continue;
				}
				if (node instanceof JTryStat) {
					if( node.finally_catcher != null ) {
						if( tmp_var==null && Kiev.verify && !expr.isConstantExpr() ) {
							tmp_var = new Var(0,KString.Empty,expr.getType(),0).getJView();
							code.addVar(tmp_var);
							code.addInstr(Instr.op_store,tmp_var);
						}
						code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
					}
				}
				else if (node instanceof JSynchronizedStat) {
					code.addInstr(Instr.op_load,node.expr_var);
					code.addInstr(Instr.op_monitorexit);
				}
			}
			if( tmp_var != null ) {
				code.addInstr(Instr.op_load,tmp_var);
				code.removeVar(tmp_var);
			}
			CodeLabel lb = null;
			if !( expr instanceof JENode ) {
				if( sw.defCase != null )
					lb = sw.defCase.getLabel(code);
				else
					lb = sw.getBrkLabel().getCodeLabel(code);
			}
			else if( !expr.isConstantExpr() )
				lb = sw.getCntLabel().getCodeLabel(code);
			else {
				int goto_value = ((Number)((JConstExpr)expr).getConstValue()).intValue();
				foreach(JCaseLabel cl; sw.cases) {
					int case_value = ((Number)((JConstExpr)cl.val).getConstValue()).intValue();
					if( goto_value == case_value ) {
						lb = cl.getLabel(code);
						break;
					}
				}
				if( lb == null ) {
					Kiev.reportError(this,"'goto case "+expr+"' not found, replaced by "+(sw.defCase!=null?"'goto default'":"'break"));
					if( sw.defCase != null )
						lb = sw.defCase.getLabel(code);
					else
						lb = sw.getBrkLabel().getCodeLabel(code);
				}
			}
			code.addInstr(Instr.op_goto,lb);
			if( !expr.isConstantExpr() && sw.mode != SwitchStat.TYPE_SWITCH )
				code.stack_pop();
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

}

