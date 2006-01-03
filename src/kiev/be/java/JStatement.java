package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.InlineMethodStat.InlineMethodStatImpl;
import kiev.vlang.InlineMethodStat.ParamRedir;
import kiev.vlang.BlockStat.BlockStatImpl;
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
public final view JInlineMethodStatView of InlineMethodStatImpl extends JENodeView {
	public access:ro	JMethodView		method;
	public access:ro	ParamRedir[]	params_redir;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		code.setLinePos(this);
		if( Kiev.verify )
			generateArgumentCheck(code);
		foreach (ParamRedir redir; params_redir)
			redir.old_var.getJVarView().bcpos = redir.new_var.getJVarView().bcpos;
		method.body.generate(code,reqType);
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params_redir.length; i++) {
			ParamRedir redir = params_redir[i];
			if( !redir.new_var.type.equals(method.params[i].type) ) {
				code.addInstr(Instr.op_load,redir.new_var.getJVarView());
				code.addInstr(Instr.op_checkcast,method.params[i].type);
				code.addInstr(Instr.op_store,redir.new_var.getJVarView());
			}
		}
	}
}

@nodeview
public final view JBlockStatView of BlockStatImpl extends JENodeView {
	@getter public final JENodeView[]	get$stats()	{ return (JENodeView[])this.$view.stats.toJViewArray(JENodeView.class); }
	public				CodeLabel		break_label;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockStat");
		code.setLinePos(this);
		JENodeView[] stats = this.stats;
		break_label = code.newLabel();
		for(int i=0; i < stats.length; i++) {
			try {
				stats[i].generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		Vector<JVarView> vars = new Vector<JVarView>();
		foreach (JENodeView n; stats) {
			if (n instanceof JVarDeclView)
				vars.append(n.var);
		}
		code.removeVars(vars.toArray());
		JNodeView p = this.jparent;
		if( p instanceof JMethodView && Kiev.debugOutputC
		 && code.need_to_gen_post_cond && ((JMethodView)p).type.ret ≢ Type.tpVoid) {
			code.stack_push(((JMethodView)p).etype.ret.getJType());
		}
		code.addInstr(Instr.set_label,break_label);
	}

	public CodeLabel getBreakLabel() throws RuntimeException {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}

}

@nodeview
public final view JEmptyStatView of EmptyStatImpl extends JENodeView {
	public JEmptyStatView(EmptyStatImpl $view) { super($view); }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating EmptyStat");
//		code.setLinePos(this);
//		code.addInstr(Instr.op_nop);
	}
}

@nodeview
public final final view JExprStatView of ExprStatImpl extends JENodeView {
	public access:ro	JENodeView		expr;

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
public final view JReturnStatView of ReturnStatImpl extends JENodeView {
	public access:ro	JENodeView		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ReturnStat");
		code.setLinePos(this);
		try {
			if( expr != null )
				expr.generate(code,code.method.type.ret);
			generateReturn(code,this);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public static void generateReturn(Code code, JNodeView from) {
		JVarView tmp_var = null;
		for(JNodeView node = from; node != null; node = node.jparent) {
			if (node instanceof JMethodView)
				break;
			else if (node instanceof JFinallyInfoView) {
				assert (node.jparent instanceof JTryStatView);
				node = node.jparent; // skip TryStat that is parent of FinallyInfo
				continue;
			}
			else if (node instanceof JTryStatView) {
				if( node.finally_catcher != null ) {
					if( tmp_var==null && code.method.type.ret ≢ Type.tpVoid ) {
						tmp_var = new Var(0,KString.Empty,code.method.type.ret,0).getJVarView();
						code.addVar(tmp_var);
						code.addInstr(Instr.op_store,tmp_var);
					}
					code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
				}
			}
			else if (node instanceof JSynchronizedStatView) {
				if( tmp_var==null && code.method.type.ret ≢ Type.tpVoid ) {
					tmp_var = new Var(0,KString.Empty,code.method.type.ret,0).getJVarView();
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
			if( code.method.type.ret ≢ Type.tpVoid )
				code.stack_pop();
		} else
			code.addInstr(Instr.op_return);
	}
}

@nodeview
public final view JThrowStatView of ThrowStatImpl extends JENodeView {
	public access:ro	JENodeView		expr;

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
public final view JIfElseStatView of IfElseStatImpl extends JENodeView {
	public access:ro	JENodeView		cond;
	public access:ro	JENodeView		thenSt;
	public access:ro	JENodeView		elseSt;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating IfElseStat");
		code.setLinePos(this);
		try {
			if( cond.isConstantExpr() ) {
				JENodeView cond = this.cond;
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
				JBoolExprView.gen_iffalse(code, cond, else_label);
				thenSt.generate(code,Type.tpVoid);
				if( elseSt != null ) {
					CodeLabel end_label = code.newLabel();
					if( !thenSt.isMethodAbrupted() ) {
						if( isAutoReturnable() )
							JReturnStatView.generateReturn(code,this);
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
public final view JCondStatView of CondStatImpl extends JENodeView {
	public access:ro	JENodeView		cond;
	public access:ro	JENodeView		message;

	private void generateAssertName(Code code) {
		JWBCConditionView wbc = (JWBCConditionView)jparent.jparent;
		if( wbc.name == null ) return;
		code.addConst((KString)wbc.name);
	}

	private JMethodView getAssertMethod() {
		KString fname;
		JWBCConditionView wbc = (JWBCConditionView)jparent.jparent;
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
		return func.getJMethodView();
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating CondStat");
		code.setLinePos(this);
		try {
			if(cond.isConstantExpr() ) {
				JENodeView cond = this.cond;
				if( ((Boolean)cond.getConstValue()).booleanValue() );
				else {
					generateAssertName(code);
					message.generate(code,Type.tpString);
					code.addInstr(Instr.op_call,getAssertMethod(),false);
				}
			} else {
				CodeLabel else_label = code.newLabel();
				JBoolExprView.gen_iftrue(code, cond, else_label);
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
public final view JLabeledStatView of LabeledStatImpl extends JENodeView {
	public access:ro	KString			ident;
	public access:ro	JLabelView		lbl;
	public access:ro	JENodeView		stat;

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
public final view JBreakStatView of BreakStatImpl extends JENodeView {
	public access:ro	KString			ident;
	public access:ro	JLabelView		dest;

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
					code.addInstr(Instr.op_load,((Var)lb[i]).getJVarView());
					code.addInstr(Instr.op_monitorexit);
				}
			if( isAutoReturnable() )
				JReturnStatView.generateReturn(code,this);
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
			for(JNodeView node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStatView ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStatView ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethodView ) break;
				if( node instanceof BreakTarget || node instanceof JBlockStatView );
				else continue;
				if( node instanceof BreakTarget ) {
					BreakTarget t = (BreakTarget)node;
					return (Object[])Arrays.append(cl,t.getBrkLabel().getCodeLabel(code));
				}
				else if( node instanceof JBlockStatView && ((JBlockStatView)node).isBreakTarget() ){
					JBlockStatView t = (JBlockStatView)node;
					return (Object[])Arrays.append(cl,t.getBreakLabel());
				}
			}
			throw new RuntimeException("Break not within loop statement");
		} else {
			// Search for labels with loop/switch statement
			for(JNodeView node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStatView ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStatView ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethodView ) break;
				if( node instanceof JLabeledStatView && ((JLabeledStatView)node).ident == ident ) {
					JENodeView st = ((JLabeledStatView)node).stat;
					if( st instanceof BreakTarget )
						return (Object[])Arrays.append(cl,st.getBrkLabel().getCodeLabel(code));
					else if (st instanceof JBlockStatView)
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
public final view JContinueStatView of ContinueStatImpl extends JENodeView {
	public access:ro	KString			ident;
	public access:ro	JLabelView		dest;

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
					code.addInstr(Instr.op_load,((Var)lb[i]).getJVarView());
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
			for(JNodeView node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStatView ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStatView ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethodView ) break;
				if( node instanceof ContinueTarget )
					return (Object[])Arrays.append(cl,node.getCntLabel().getCodeLabel(code));
			}
			throw new RuntimeException("Continue not within loop statement");
		} else {
			// Search for labels with loop statement
			for(JNodeView node = this.jparent; node != null; node = node.jparent) {
				if( node instanceof JTryStatView ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof JSynchronizedStatView ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof JMethodView ) break;
				if( node instanceof JLabeledStatView && ((JLabeledStatView)node).ident == ident ) {
					JENodeView st = ((JLabeledStatView)node).stat;
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
public final view JGotoStatView of GotoStatImpl extends JENodeView {
	public access:ro	KString			ident;
	public access:ro	JLabelView		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoStat");
//		JLabeledStatView[] stats = resolveStat(ident, code.method.body, JLabeledStatView.emptyArray);
//		if( stats.length == 0 )
//			throw new CompilerException(this,"Label "+ident+" unresolved");
//		if( stats.length > 1 )
//			throw new CompilerException(this,"Umbigouse label "+ident+" in goto statement");
//		LabeledStat stat = stats[0];
//		if( stat == null )
//			throw new CompilerException(this,"Label "+ident+" unresolved");
		code.setLinePos(this);
		try {
			Object[] lb = resolveLabelStat(code,(JLabeledStatView)dest.jparent);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					code.addInstr(Instr.op_load,(JVarView)lb[i]);
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

	public Object[] resolveLabelStat(Code code, JLabeledStatView stat) {
		Object[] cl1 = new CodeLabel[0];
		Object[] cl2 = new CodeLabel[0];
		JNodeView st = stat;
		while( !(st instanceof JMethodView) ) {
			if( st instanceof JFinallyInfoView ) {
				st = st.jparent.jparent;
				continue;
			}
			else if( st instanceof JTryStatView ) {
				JTryStatView ts = (JTryStatView)st;
				if( ts.finally_catcher != null )
					cl1 = (Object[])Arrays.append(cl1,ts.finally_catcher.subr_label);
			}
			else if( st instanceof JSynchronizedStatView ) {
				cl1 = (Object[])Arrays.append(cl1,st.expr_var);
			}
			st = st.jparent;
		}
		st = this;
		while( !(st instanceof JMethodView) ) {
			if( st instanceof JFinallyInfoView ) {
				st = st.jparent.jparent;
				continue;
			}
			if( st instanceof JTryStatView ) {
				JTryStatView ts = (JTryStatView)st;
				if( ts.finally_catcher != null )
					cl2 = (Object[])Arrays.append(cl2,ts.finally_catcher.subr_label);
			}
			else if( st instanceof JSynchronizedStatView ) {
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
public final view JGotoCaseStatView of GotoCaseStatImpl extends JENodeView {
	public access:ro	JENodeView			expr;
	public access:ro	JSwitchStatView		sw;

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

			JVarView tmp_var = null;
			for(JNodeView node = this.jparent; node != null; node = node.jparent) {
				if (node == sw)
					break;
				if (node instanceof JFinallyInfoView) {
					node = node.jparent; // skip calling jsr if we are in it
					continue;
				}
				if (node instanceof JTryStatView) {
					if( node.finally_catcher != null ) {
						if( tmp_var==null && Kiev.verify && !expr.isConstantExpr() ) {
							tmp_var = new Var(0,KString.Empty,expr.getType(),0).getJVarView();
							code.addVar(tmp_var);
							code.addInstr(Instr.op_store,tmp_var);
						}
						code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
					}
				}
				else if (node instanceof JSynchronizedStatView) {
					code.addInstr(Instr.op_load,node.expr_var);
					code.addInstr(Instr.op_monitorexit);
				}
			}
			if( tmp_var != null ) {
				code.addInstr(Instr.op_load,tmp_var);
				code.removeVar(tmp_var);
			}
			CodeLabel lb = null;
			if !( expr instanceof JENodeView ) {
				if( sw.defCase != null )
					lb = sw.defCase.getLabel(code);
				else
					lb = sw.getBrkLabel().getCodeLabel(code);
			}
			else if( !expr.isConstantExpr() )
				lb = sw.getCntLabel().getCodeLabel(code);
			else {
				int goto_value = ((Number)((JConstExprView)expr).getConstValue()).intValue();
				foreach(JCaseLabelView cl; sw.cases) {
					int case_value = ((Number)((JConstExprView)cl.val).getConstValue()).intValue();
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

