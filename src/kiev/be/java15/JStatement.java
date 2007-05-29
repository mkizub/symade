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

import kiev.vlang.InlineMethodStat.ParamRedir;

import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

public final view JInlineMethodStat of InlineMethodStat extends JENode {
	public:ro	JMethod			method;
	public:ro	ParamRedir[]	params_redir;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		code.setLinePos(this);
		if( Kiev.verify )
			generateArgumentCheck(code);
		foreach (ParamRedir redir; params_redir)
			((JVar)redir.old_var).bcpos = ((JVar)redir.new_var).bcpos;
		method.body.generate(code,reqType);
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params_redir.length; i++) {
			ParamRedir redir = params_redir[i];
			if( !redir.new_var.getType().equals(method.params[i].type) ) {
				code.addInstr(Instr.op_load,(JVar)redir.new_var);
				code.addInstr(Instr.op_checkcast,method.params[i].type);
				code.addInstr(Instr.op_store,(JVar)redir.new_var);
			}
		}
	}
}

public final final view JExprStat of ExprStat extends JENode {
	public:ro	JENode		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ExprStat");
		try {
			if (expr != null)
				expr.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

public final view JReturnStat of ReturnStat extends JENode {
	public:ro	JENode		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ReturnStat");
		code.setLinePos(this);
		try {
			if( expr != null ) {
				expr.generate(code,code.method.type.ret());
				if( !expr.getType().getJType().isInstanceOf(code.method.type.ret().getJType()) ) {
					trace( Kiev.debug && Kiev.debugNodeTypes, "Need checkcast for return");
					code.addInstr(Instr.op_checkcast,code.method.type.ret());
				}
			}
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
						tmp_var = (JVar)new LVar(0,"",code.method.type.ret(),Var.VAR_LOCAL,0);
						code.addVar(tmp_var);
						code.addInstr(Instr.op_store,tmp_var);
					}
					code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
				}
			}
			else if (node instanceof JSynchronizedStat) {
				if( tmp_var==null && code.method.type.ret() ≢ Type.tpVoid ) {
					tmp_var = (JVar)new LVar(0,"",code.method.type.ret(),Var.VAR_LOCAL,0);
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

public final view JThrowStat of ThrowStat extends JENode {
	public:ro	JENode		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ThrowStat");
		code.setLinePos(this);
		try {
			expr.generate(code,null);
			code.addInstr(Instr.op_throw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}
}

public final view JIfElseStat of IfElseStat extends JENode {
	public:ro	JENode		cond;
	public:ro	JENode		thenSt;
	public:ro	JENode		elseSt;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating IfElseStat");
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

public final view JCondStat of CondStat extends JENode {
	public:ro	JENode		enabled;
	public:ro	JENode		cond;
	public:ro	JENode		message;

	public:n,n,n,rw void generateAssertName(Code code) {
		JWBCCondition wbc = (JWBCCondition)jparent.jparent;
		if (wbc.u_name == null) return;
		code.addConst(KString.from(wbc.u_name));
	}

	public:n,n,n,rw JMethod getAssertMethod() {
		String fname;
		JWBCCondition wbc = (JWBCCondition)jparent.jparent;
		switch( wbc.cond ) {
		case WBCType.CondRequire:	fname = nameAssertRequireMethod;
		case WBCType.CondEnsure:	fname = nameAssertEnsureMethod;
		case WBCType.CondInvariant:	fname = nameAssertInvariantMethod;
		default: fname = nameAssertMethod;
		}
		Method func;
		if (wbc.sname == null || wbc.u_name == null)
			func = Type.tpDebug.tdecl.resolveMethod(fname,Type.tpVoid,Type.tpString);
		else
			func = Type.tpDebug.tdecl.resolveMethod(fname,Type.tpVoid,Type.tpString,Type.tpString);
		return (JMethod)func;
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating CondStat");
		if (!Kiev.debugOutputA)
			return;
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
				JBoolExpr.gen_iffalse(code, enabled, else_label);
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

public final view JLabeledStat of LabeledStat extends JENode {
	public:ro	JLabel		lbl;
	public:ro	JENode		stat;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating LabeledStat");
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

public final view JBreakStat of BreakStat extends JENode {
	public:ro	JLabel		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating BreakStat");
		code.setLinePos(this);
		try {
			Object[] lb = resolveBreakLabel(code);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel ) {
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				}
				else {
					code.addInstr(Instr.op_load,(JVar)(Var)lb[i]);
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
	public:n,n,n,rw Object[] resolveBreakLabel(Code code) {
		String name = this.ident;
		Object[] cl = new Object[0];
		if( name == null || name == "" ) {
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
				if( node instanceof JLabeledStat && ((JLabeledStat)node).lbl.sname == name ) {
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

public final view JContinueStat of ContinueStat extends JENode {
	public:ro	JLabel		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ContinueStat");
		code.setLinePos(this);
		try {
			Object[] lb = resolveContinueLabel(code);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					code.addInstr(Instr.op_load,(JVar)(Var)lb[i]);
					code.addInstr(Instr.op_monitorexit);
				}
			code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	public:n,n,n,rw Object[] resolveContinueLabel(Code code) {
		String name = this.ident;
		Object[] cl = new Object[0];
		if( name == null || name == "" ) {
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
				if( node instanceof JLabeledStat && ((JLabeledStat)node).lbl.sname == name ) {
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

public final view JGotoStat of GotoStat extends JENode {
	public:ro	JLabel		dest;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating GotoStat");
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
					code.addInstr(Instr.op_load,(JVar)(Var)lb[i]);
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

	public:n,n,n,rw JNode getAsJNode(JNode jn) { return jn; }
	
	public Object[] resolveLabelStat(Code code, JLabeledStat stat) {
		Object[] cl1 = new CodeLabel[0];
		Object[] cl2 = new CodeLabel[0];
		JNode st = getAsJNode(stat);
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
		st = getAsJNode(this);
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

public final view JGotoCaseStat of GotoCaseStat extends JENode {
	public:ro	JENode			expr;
	public:ro	JSwitchStat		sw;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating GotoCaseStat");
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
				if ((ASTNode)node == (ASTNode)sw)
					break;
				if (node instanceof JFinallyInfo) {
					node = node.jparent; // skip calling jsr if we are in it
					continue;
				}
				if (node instanceof JTryStat) {
					if( node.finally_catcher != null ) {
						if( tmp_var==null && Kiev.verify && !expr.isConstantExpr() ) {
							tmp_var = (JVar)new LVar(0,"",expr.getType(),Var.VAR_LOCAL,0);
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

