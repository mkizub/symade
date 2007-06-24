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
package kiev.vlang;

import kiev.ir.java15.RNode;
import kiev.be.java15.JNode;
import kiev.ir.java15.RENode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RInlineMethodStat;
import kiev.be.java15.JInlineMethodStat;
import kiev.ir.java15.RBlock;
import kiev.be.java15.JBlock;
import kiev.ir.java15.RExprStat;
import kiev.be.java15.JExprStat;
import kiev.ir.java15.RReturnStat;
import kiev.be.java15.JReturnStat;
import kiev.ir.java15.RThrowStat;
import kiev.be.java15.JThrowStat;
import kiev.ir.java15.RIfElseStat;
import kiev.be.java15.JIfElseStat;
import kiev.ir.java15.RCondStat;
import kiev.be.java15.JCondStat;
import kiev.ir.java15.RLabeledStat;
import kiev.be.java15.JLabeledStat;
import kiev.ir.java15.RBreakStat;
import kiev.be.java15.JBreakStat;
import kiev.ir.java15.RContinueStat;
import kiev.be.java15.JContinueStat;
import kiev.ir.java15.RGotoStat;
import kiev.be.java15.JGotoStat;
import kiev.ir.java15.RGotoCaseStat;
import kiev.be.java15.JGotoCaseStat;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class InlineMethodStat extends ENode implements ScopeOfNames {
	
	@dflow(in="root()", out="this:out()") private static class DFI {}

	public static final class ParamRedir {
		public Var		old_var;
		public Var		new_var;
		public ParamRedir(Var o, Var n) { old_var=o; new_var=n; }
	};

	@virtual typedef This  = InlineMethodStat;
	@virtual typedef JView = JInlineMethodStat;
	@virtual typedef RView = RInlineMethodStat;

	@att public Method			method;
	@ref public ParamRedir[]	params_redir;

	public InlineMethodStat() {}

	public InlineMethodStat(int pos, Method m, Method in) {
		this.pos = pos;
		this.method = m;
		assert(m.params.length == in.params.length);
		params_redir = new ParamRedir[m.params.length];
		for (int i=0; i < m.params.length; i++) {
			params_redir[i] = new ParamRedir(m.params[i],in.params[i]);
		}
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		ParamRedir@	redir;
	{
		redir @= params_redir,
		path.checkNodeName(redir.old_var),
		node ?= redir.new_var
	}

	static class InlineMethodStatDFFuncIn extends DFFunc {
		final int res_idx;
		InlineMethodStatDFFuncIn(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			InlineMethodStat node = (InlineMethodStat)dfi.node_impl;
			DFState in = DFState.makeNewState();
			for(int i=0; i < node.params_redir.length; i++) {
				in = in.declNode(node.params_redir[i].new_var);
				in = in.addNodeType(new Var[]{node.params_redir[i].new_var},node.method.params[i].type);
			}
			res = in;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new InlineMethodStatDFFuncIn(dfi);
	}

	static class InlineMethodStatDFFuncOut extends DFFunc {
		final int res_idx;
		InlineMethodStatDFFuncOut(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			InlineMethodStat node = (InlineMethodStat)dfi.node_impl;
			DataFlowInfo pdfi = DataFlowInfo.getDFlow((ASTNode)node.parent());
			res = DFFunc.calc(pdfi.getSocket(node.pslot().name).func_in, pdfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new InlineMethodStatDFFuncOut(dfi);
	}
}

@node(name="ExprSt")
public class ExprStat extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ExprStat;
	@virtual typedef JView = JExprStat;
	@virtual typedef RView = RExprStat;

	@att public ENode	expr;

	public ExprStat() {}

	public ExprStat(ENode expr) {
		this.expr = expr;
		if (expr != null)
			this.pos = expr.pos;
	}

	public ExprStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
	}

	public String toString() {
		if (expr != null)
			return expr+";";
		else
			return ";";
	}
}

@node(name="Return")
public class ReturnStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ReturnStat;
	@virtual typedef JView = JReturnStat;
	@virtual typedef RView = RReturnStat;

	@att public ENode	expr;

	public ReturnStat() {}

	public ReturnStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public static void autoReturn(Type reqType, RENode expr) {
		if (expr.parent() instanceof ReturnStat)
			return;
		expr.setAutoReturnable(false);
		expr.replaceWithResolve(reqType, fun ()->ENode { return new ReturnStat(expr.pos, ((ENode)expr).detach()); });
	}

	public static void autoReturn(Type reqType, ENode expr) {
		if (expr.parent() instanceof ReturnStat)
			return;
		expr.setAutoReturnable(false);
		expr.replaceWithResolve(reqType, fun ()->ENode { return new ReturnStat(expr.pos, ~expr); });
	}
}

@node(name="Throw")
public class ThrowStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ThrowStat;
	@virtual typedef JView = JThrowStat;
	@virtual typedef RView = RThrowStat;

	@att public ENode	expr;

	public ThrowStat() {}

	public ThrowStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}
}

@node(name="If")
public class IfElseStat extends ENode {
	
	@dflow(out="join thenSt elseSt") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		thenSt;
	@dflow(in="cond:false")	ENode		elseSt;
	}

	@virtual typedef This  = IfElseStat;
	@virtual typedef JView = JIfElseStat;
	@virtual typedef RView = RIfElseStat;

	@att public ENode			cond;
	@att public ENode			thenSt;
	@att public ENode			elseSt;

	public IfElseStat() {}
	
	public IfElseStat(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		this.pos = pos;
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}
	
	public Type getType() {
		if (thenSt == null || elseSt == null)
			return StdTypes.tpVoid;
		Type t1 = thenSt.getType();
		Type t2 = elseSt.getType();
		if (t1 ≡ StdTypes.tpVoid || t2 ≡ StdTypes.tpVoid)
			return StdTypes.tpVoid;
		if( t1.isReference() && t2.isReference() ) {
			if( t1 ≡ t2 ) return t1;
			if( t1 ≡ Type.tpNull ) return t2;
			if( t2 ≡ Type.tpNull ) return t1;
			return Type.leastCommonType(t1,t2);
		}
		if( t1.isNumber() && t2.isNumber() ) {
			if( t1 ≡ t2 ) return t1;
			return CoreType.upperCastNumbers(t1,t2);
		}
		return t1;
	}
}

@node(name="CondSt")
public class CondStat extends ENode {
	
	@dflow(out="cond:true") private static class DFI {
	@dflow(in="this:in")		ENode		cond;
	@dflow(in="cond:false")		ENode		message;
	}

	@virtual typedef This  = CondStat;
	@virtual typedef JView = JCondStat;
	@virtual typedef RView = RCondStat;

	@att public ENode		enabled;
	@att public ENode		cond;
	@att public ENode		message;

	public CondStat() {}

	public CondStat(int pos, ENode cond, ENode message) {
		this.pos = pos;
		this.cond = cond;
		this.message = message;
	}
}

@node(name="LblSt")
public class LabeledStat extends ENode {
	
	@dflow(out="stat") private static class DFI {
	@dflow(in="this:in")	Label			lbl;
	@dflow(in="lbl")		ENode			stat;
	}

	public static final LabeledStat[]	emptyArray = new LabeledStat[0];

	@virtual typedef This  = LabeledStat;
	@virtual typedef JView = JLabeledStat;
	@virtual typedef RView = RLabeledStat;

	@att public Label			lbl;
	@att public ENode			stat;

	public LabeledStat() {
		this.lbl = new Label();
	}
}

@node(name="Break")
public class BreakStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = BreakStat;
	@virtual typedef JView = JBreakStat;
	@virtual typedef RView = RBreakStat;

	@ref(copyable=false) public Label		dest;

	public boolean preVerify() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		return super.preVerify();
	}

	public BreakStat() {
		this.ident = "";
	}

	public boolean mainResolveIn() {
		ASTNode p;
		if (dest != null) {
			dest.delLink((BreakStat)this);
			dest = null;
		}
		if (ident == null || ident == "") {
			for(p=(ASTNode)parent(); !(p instanceof Method || p.isBreakTarget()); p = (ASTNode)p.parent() );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = p.lblbrk;
					if (l != null) {
						dest = l;
						l.addLink((BreakStat)this);
					}
				}
			}
		} else {
	label_found:
			for(p=(ASTNode)parent(); !(p instanceof Method) ; p=(ASTNode)p.parent() ) {
				if (p instanceof LabeledStat && p.lbl.sname == this.ident)
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if (!p.isBreakTarget()) continue;
				ASTNode pp = p;
				for(p=(ASTNode)p.parent(); p instanceof LabeledStat; p = (ASTNode)p.parent()) {
					if (p.lbl.sname == this.ident) {
						p = pp;
						break label_found;
					}
				}
				p = pp;
			}
			if( p instanceof Method || p == null) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = p.lblbrk;
					if (l != null) {
						dest = l;
						l.addLink((BreakStat)this);
					}
				}
			}
		}
		return false; // don't pre-resolve
	}
}

@node(name="Continue")
public class ContinueStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = ContinueStat;
	@virtual typedef JView = JContinueStat;
	@virtual typedef RView = RContinueStat;

	@ref(copyable=false) public Label		dest;

	public boolean preVerify() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		return super.preVerify();
	}

	public ContinueStat() {
		this.ident = "";
	}

	public boolean mainResolveIn() {
		ASTNode p;
		if (dest != null) {
			dest.delLink((ContinueStat)this);
			dest = null;
		}
		if (this.ident == null || this.ident == "") {
			for(p=(ASTNode)parent(); !(p instanceof LoopStat || p instanceof Method); p = (ASTNode)p.parent() );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Continue not within loop statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = p.lblcnt;
					if (l != null) {
						dest = l;
						l.addLink((ContinueStat)this);
					}
				}
			}
		} else {
	label_found:
			for(p=(ASTNode)parent(); !(p instanceof Method) ; p=(ASTNode)p.parent() ) {
				if( p instanceof LabeledStat && p.lbl.sname == this.ident )
					throw new RuntimeException("Label "+ident+" does not refer to continue target");
				if !(p instanceof LoopStat) continue;
				ASTNode pp = p;
				for(p=(ASTNode)p.parent(); p instanceof LabeledStat; p = (ASTNode)p.parent()) {
					if( p.lbl.sname == this.ident ) {
						p = pp;
						break label_found;
					}
				}
				p = pp;
			}
			if( p instanceof Method || p == null) {
				Kiev.reportError(this,"Continue not within loop statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = p.lblcnt;
					if (l != null) {
						dest = l;
						l.addLink((ContinueStat)this);
					}
				}
			}
		}
		return false; // don't pre-resolve
	}
}

@node(name="Goto")
public class GotoStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = GotoStat;
	@virtual typedef JView = JGotoStat;
	@virtual typedef RView = RGotoStat;

	@ref(copyable=false) public Label		dest;

	public boolean preVerify() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		return super.preVerify();
	}

	public GotoStat() {
		this.ident = "";
	}
	
	public boolean mainResolveIn() {
		if (dest != null) {
			dest.delLink((GotoStat)this);
			dest = null;
		}
		Label[] labels = resolveStat(this.ident,ctx_method.body);
		if( labels.length == 0 ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return false;
		}
		if( labels.length > 1 ) {
			Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
		}
		Label label = labels[0];
		dest = label;
		dest.addLink((GotoStat)this);
		return false; // don't pre-resolve
	}

	public static Label[] resolveStat(String name, ASTNode st) {
		Vector<Label> labels = new Vector<Label>();
		st.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if (n instanceof Label && n.sname == name) {
					Label l = (Label)n;
					if (!labels.contains(l))
						labels.append(l);
					return true; // can be nested
				}
				if (n instanceof DNode)
					return false; // don't scan declarations, like inner classes
				return true;
			}
		});
		return labels.toArray();
	}
}

@node(name="GotoCase")
public class GotoCaseStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = GotoCaseStat;
	@virtual typedef JView = JGotoCaseStat;
	@virtual typedef RView = RGotoCaseStat;

	@att public ENode		expr;
	@ref public SwitchStat	sw;

	public GotoCaseStat() {}
}

