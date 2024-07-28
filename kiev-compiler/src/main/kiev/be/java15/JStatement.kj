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

public final class JInlineMethodStat extends JENode {

	@virtual typedef VT  ≤ InlineMethodStat;

	public final JMethod dispatched;
	public final JMethod dispatcher;

	public static JInlineMethodStat attach(InlineMethodStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JInlineMethodStat)jn;
		return new JInlineMethodStat(impl);
	}
	
	protected JInlineMethodStat(InlineMethodStat impl) {
		super(impl);
		this.dispatched = (JMethod)impl.dispatched;
		this.dispatcher = (JMethod)impl.dispatcher;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		code.setLinePos(this);
		InlineMethodStat vn = vn();
		SymbolRef[] old_vars = vn.old_vars;
		SymbolRef[] new_vars = vn.new_vars;
		for (int i=0; i < old_vars.length; i++) {
			Var vold = (Var)old_vars[i].dnode;
			Var vnew = (Var)new_vars[i].dnode;
			code.addVarAlias((JVar)vold, (JVar)vnew);
		}
		try {
			if( Kiev.verify )
				generateArgumentCheck(code);
			dispatched.body.generate(code,reqType);
		} finally {
			for (int i=old_vars.length-1; i >= 0; i--) {
				Var vold = (Var)old_vars[i].dnode;
				Var vnew = (Var)new_vars[i].dnode;
				code.removeVarAlias((JVar)vold, (JVar)vnew);
			}
		}
	}

	public void generateArgumentCheck(Code code) {
		JMethod m = dispatched;
		InlineMethodStat vn = vn();
		SymbolRef[] old_vars = vn.old_vars;
		SymbolRef[] new_vars = vn.new_vars;
		for (int i=0; i < old_vars.length; i++) {
			JVar vold = (JVar)(Var)old_vars[i].dnode;
			JVar vnew = (JVar)(Var)new_vars[i].dnode;
			if( !vnew.equals(m.params[i].vtype) ) {
				code.addInstr(Instr.op_load,vold);
				code.addInstr(Instr.op_checkcast,m.params[i].getType());
				code.addInstr(Instr.op_store,vnew);
			}
		}
	}
}

public final class JExprStat extends JENode {

	@virtual typedef VT  ≤ ExprStat;

	public static JExprStat attach(ExprStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JExprStat)jn;
		return new JExprStat(impl);
	}
	
	protected JExprStat(ExprStat impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ExprStat");
		code.setLinePos(this);
		ExprStat vn = vn();
		JENode expr = (JENode)vn.expr;
		try {
			if (expr != null)
				expr.generate(code,code.tenv.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JReturnStat extends JENode {

	@virtual typedef VT  ≤ ReturnStat;

	public static JReturnStat attach(ReturnStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JReturnStat)jn;
		return new JReturnStat(impl);
	}
	
	protected JReturnStat(ReturnStat impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ReturnStat");
		code.setLinePos(this);
		ReturnStat vn = vn();
		JENode expr = (JENode)vn.expr;
		try {
			if( expr != null ) {
				expr.generate(code,code.method.mtype.ret());
				if( !code.jtenv.getJType(expr.getType()).isInstanceOf(code.jtenv.getJType(code.method.mtype.ret())) ) {
					trace( Kiev.debug && Kiev.debugNodeTypes, "Need checkcast for return");
					code.addInstr(Instr.op_checkcast,code.method.mtype.ret());
				}
			}
			generateReturn(code,this);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
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
					if( tmp_var==null && code.method.mtype.ret() ≢ code.tenv.tpVoid ) {
						tmp_var = (JVar)new LVar(0,"",code.method.mtype.ret(),Var.VAR_LOCAL,0);
						code.addVar(tmp_var);
						code.addInstr(Instr.op_store,tmp_var);
					}
					code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
				}
			}
			else if (node instanceof JSynchronizedStat) {
				if( tmp_var==null && code.method.mtype.ret() ≢ code.tenv.tpVoid ) {
					tmp_var = (JVar)new LVar(0,"",code.method.mtype.ret(),Var.VAR_LOCAL,0);
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
			code.addInstr(Instr.op_goto,code.method.getBrkLabel().getCodeLabel(code));
			if( code.method.mtype.ret() ≢ code.tenv.tpVoid )
				code.stack_pop();
		} else
			code.addInstr(Instr.op_return);
	}
}

public final class JThrowStat extends JENode {

	@virtual typedef VT  ≤ ThrowStat;

	public static JThrowStat attach(ThrowStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JThrowStat)jn;
		return new JThrowStat(impl);
	}
	
	protected JThrowStat(ThrowStat impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating ThrowStat");
		code.setLinePos(this);
		ThrowStat vn = vn();
		JENode expr = (JENode)vn.expr;
		try {
			expr.generate(code,null);
			code.addInstr(Instr.op_throw);
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JIfElseStat extends JENode {

	@virtual typedef VT  ≤ IfElseStat;

	public static JIfElseStat attach(IfElseStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JIfElseStat)jn;
		return new JIfElseStat(impl);
	}
	
	protected JIfElseStat(IfElseStat impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating IfElseStat");
		code.setLinePos(this);
		IfElseStat vn = vn();
		JENode cond = (JENode)vn.cond;
		JENode thenSt = (JENode)vn.thenSt;
		JENode elseSt = (JENode)vn.elseSt;
		Type gen_tp;
		if( reqType ≡ code.tenv.tpVoid )
			gen_tp = code.tenv.tpVoid;
		else
			gen_tp = this.getType(); 
		try {
			if( cond.isConstantExpr(code.env) ) {
				if( ((Boolean)cond.getConstValue(code.env)).booleanValue() ) {
					if( isAutoReturnable() )
						thenSt.setAutoReturnable(true);
					thenSt.generate(code,gen_tp);
				}
				else if( elseSt != null ) {
					if( isAutoReturnable() )
						elseSt.setAutoReturnable(true);
					elseSt.generate(code,gen_tp);
				}
			} else {
				CodeLabel else_label = code.newLabel();
				JBoolExpr.gen_iffalse(code, cond, else_label);
				thenSt.generate(code,gen_tp);
				if( elseSt != null ) {
					CodeLabel end_label = code.newLabel();
					if( !thenSt.isMethodAbrupted() ) {
						if( isAutoReturnable() )
							JReturnStat.generateReturn(code,this);
						else if (!thenSt.isAbrupted())
							code.addInstr(Instr.op_goto,end_label);
					}
					code.addInstr(Instr.set_label,else_label);
					elseSt.generate(code,gen_tp);
					code.addInstr(Instr.set_label,end_label);
				} else {
					code.addInstr(Instr.set_label,else_label);
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JCondStat extends JENode {

	@virtual typedef VT  ≤ CondStat;

	public static JCondStat attach(CondStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCondStat)jn;
		return new JCondStat(impl);
	}
	
	protected JCondStat(CondStat impl) {
		super(impl);
	}

	public void generateAssertName(Code code) {
		JWBCCondition wbc = (JWBCCondition)jparent.jparent;
		if (wbc.sname == null) return;
		code.addConst(wbc.sname);
	}

	public JMethod getAssertMethod(Code code) {
		String fname;
		JWBCCondition wbc = (JWBCCondition)jparent.jparent;
		switch( wbc.cond ) {
		case WBCType.CondRequire:	fname = nameAssertRequireMethod;
		case WBCType.CondEnsure:	fname = nameAssertEnsureMethod;
		case WBCType.CondInvariant:	fname = nameAssertInvariantMethod;
		default: fname = nameAssertMethod;
		}
		Method func;
		if (wbc.sname == null)
			func = code.tenv.tpDebug.tdecl.resolveMethod(code.env,fname,code.tenv.tpVoid,code.tenv.tpString);
		else
			func = code.tenv.tpDebug.tdecl.resolveMethod(code.env,fname,code.tenv.tpVoid,code.tenv.tpString,code.tenv.tpString);
		return (JMethod)func;
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating CondStat");
		if (!Kiev.debugOutputA)
			return;
		code.setLinePos(this);
		CondStat vn = vn();
		JENode enabled = (JENode)vn.enabled;
		JENode cond = (JENode)vn.cond;
		JENode message = (JENode)vn.message;
		try {
			if(cond.isConstantExpr(code.env) ) {
				if( ((Boolean)cond.getConstValue(code.env)).booleanValue() );
				else {
					generateAssertName(code);
					message.generate(code,code.tenv.tpString);
					code.addInstr(Instr.op_call,getAssertMethod(code),false);
				}
			} else {
				CodeLabel else_label = code.newLabel();
				JBoolExpr.gen_iffalse(code, enabled, else_label);
				JBoolExpr.gen_iftrue(code, cond, else_label);
				generateAssertName(code);
				message.generate(code,code.tenv.tpString);
				code.addInstr(Instr.op_call,getAssertMethod(code),false);
				code.addInstr(Instr.set_label,else_label);
			}
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}
}

public final class JLabeledStat extends JENode {

	@virtual typedef VT  ≤ LabeledStat;

	public final JLabel lbl;

	public static JLabeledStat attach(LabeledStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JLabeledStat)jn;
		return new JLabeledStat(impl);
	}
	
	protected JLabeledStat(LabeledStat impl) {
		super(impl);
		this.lbl = (JLabel)impl.lbl;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating LabeledStat");
		code.setLinePos(this);
		LabeledStat vn = vn();
		JENode stat = (JENode)vn.stat;
		try {
			lbl.generate(code,code.tenv.tpVoid);
			stat.generate(code,code.tenv.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat.vn(),e);
		}
	}

	public CodeLabel getCodeLabel(Code code) {
		return lbl.getCodeLabel(code);
	}
}

public final class JBreakStat extends JENode {

	@virtual typedef VT  ≤ BreakStat;

	public final JLabel dest;

	public static JBreakStat attach(BreakStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBreakStat)jn;
		return new JBreakStat(impl);
	}
	
	protected JBreakStat(BreakStat impl) {
		super(impl);
		this.dest = (JLabel)impl.dest;
	}

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
			Kiev.reportError(vn(),e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	public Object[] resolveBreakLabel(Code code) {
		String name = this.getIdent();
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
					return (Object[])Arrays.append(cl,t.getBrkLabel().getCodeLabel(code));
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
					JENode st = (JENode)((LabeledStat)node.vn()).stat;
					if( st instanceof BreakTarget )
						return (Object[])Arrays.append(cl,st.getBrkLabel().getCodeLabel(code));
					else if (st instanceof JBlock)
						return (Object[])Arrays.append(cl,st.getBrkLabel().getCodeLabel(code));
					else
						throw new RuntimeException("Label "+name+" does not refer to break target");
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a break target");
	}
}

public final class JContinueStat extends JENode {

	@virtual typedef VT  ≤ ContinueStat;

	public final JLabel dest;

	public static JContinueStat attach(ContinueStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JContinueStat)jn;
		return new JContinueStat(impl);
	}
	
	protected JContinueStat(ContinueStat impl) {
		super(impl);
		this.dest = (JLabel)impl.dest;
	}

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
			Kiev.reportError(vn(),e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	public Object[] resolveContinueLabel(Code code) {
		String name = this.getIdent();
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
					JENode st = (JENode)((LabeledStat)node.vn()).stat;
					if( st instanceof ContinueTarget )
						return (Object[])Arrays.append(cl,st.getCntLabel().getCodeLabel(code));
					throw new RuntimeException("Label "+name+" does not refer to continue target");
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a continue target");
	}
}

public final class JGotoStat extends JENode {

	@virtual typedef VT  ≤ GotoStat;

	public final JLabel dest;

	public static JGotoStat attach(GotoStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JGotoStat)jn;
		return new JGotoStat(impl);
	}
	
	protected JGotoStat(GotoStat impl) {
		super(impl);
		this.dest = (JLabel)impl.dest;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating GotoStat");
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
			Kiev.reportError(vn(),e);
			throw new RuntimeException(e.getMessage());
		}
	}

	public JNode getAsJNode(JNode jn) { return jn; }
	
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

public final class JGotoCaseStat extends JENode {

	@virtual typedef VT  ≤ GotoCaseStat;

	public static JGotoCaseStat attach(GotoCaseStat impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JGotoCaseStat)jn;
		return new JGotoCaseStat(impl);
	}
	
	protected JGotoCaseStat(GotoCaseStat impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating GotoCaseStat");
		code.setLinePos(this);
		GotoCaseStat vn = vn();
		JENode expr = (JENode)vn.expr;
		SwitchStat swvn = vn.sw;
		JSwitchStat sw = (JSwitchStat)vn.sw;
		try {
			if (!expr.isConstantExpr(code.env))
				expr.generate(code,null);

			JVar tmp_var = null;
			for(JNode node = this.jparent; node != null; node = node.jparent) {
				if (node.vn() == swvn)
					break;
				if (node instanceof JFinallyInfo) {
					node = node.jparent; // skip calling jsr if we are in it
					continue;
				}
				if (node instanceof JTryStat) {
					if( node.finally_catcher != null ) {
						if( tmp_var==null && Kiev.verify && !expr.isConstantExpr(code.env) ) {
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
			JCaseLabel sw_defCase = (JCaseLabel)swvn.defCase;
			if !( expr instanceof JENode ) {
				if( sw_defCase != null )
					lb = sw_defCase.getLabel(code);
				else
					lb = sw.getBrkLabel().getCodeLabel(code);
			}
			else if (!expr.isConstantExpr(code.env)) {
				lb = sw.getCntLabel().getCodeLabel(code);
			}
			else {
				int goto_value = ((Number)((JConstExpr)expr).getConstValue(code.env)).intValue();
				JCaseLabel[] sw_cases = JNode.toJArray<JCaseLabel>(swvn.cases);
				foreach(JCaseLabel cl; sw_cases; cl.has_value && goto_value == cl.case_value) {
					lb = cl.getLabel(code);
					break;
				}
				if( lb == null ) {
					Kiev.reportError(vn,"'goto case "+expr+"' not found, replaced by "+(sw_defCase!=null?"'goto default'":"'break"));
					if( sw_defCase != null )
						lb = sw_defCase.getLabel(code);
					else
						lb = sw.getBrkLabel().getCodeLabel(code);
				}
			}
			code.addInstr(Instr.op_goto,lb);
			if( !expr.isConstantExpr(code.env) && !(sw instanceof JSwitchTypeStat) )
				code.stack_pop();
		} catch(Exception e ) {
			Kiev.reportError(vn,e);
		}
	}

}

