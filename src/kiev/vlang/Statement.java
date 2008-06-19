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

import kiev.ir.java15.RENode;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(lang=CoreLang)
public class InlineMethodStat extends ENode implements ScopeOfNames {
	
	@DataFlowDefinition(in="root()", out="this:out()") private static class DFI {}

	@nodeAttr public Method					dispatched;
	@nodeData public Method					dispatcher;
	@nodeAttr public SymbolRef∅			old_vars;
	@nodeAttr public SymbolRef∅			new_vars;

	public InlineMethodStat() {}

	public InlineMethodStat(int pos, Method dispatched, Method dispatcher) {
		this.pos = pos;
		this.dispatched = dispatched;
		this.dispatcher = dispatcher;
		assert(dispatched.params.length == dispatcher.params.length);
		for (int i=0; i < dispatched.params.length; i++) {
			old_vars += new SymbolRef<Var>(dispatcher.params[i]);
			new_vars += new SymbolRef<Var>(dispatched.params[i]);
		}
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		SymbolRef@	sr;
	{
		sr @= old_vars,
		path.checkNodeName((Var)sr.dnode),
		node ?= getNewVar((Var)sr.dnode)
	}
	
	private Var getNewVar(Var old) {
		for (int i=0; i < old_vars.length; i++) {
			if (old_vars[i].dnode == old)
				return (Var)old_vars[i].dnode;
		}
		throw new CompilerException(this, "Cannot find a matched new var for old var "+old);
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
			for(int i=0; i < node.new_vars.length; i++) {
				in = in.declNode((Var)node.new_vars[i].dnode);
				in = in.addNodeType(new Var[]{(Var)node.new_vars[i].dnode},node.dispatched.params[i].getType());
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

@ThisIsANode(name="ExprSt", lang=CoreLang)
public class ExprStat extends ENode {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	}

	@nodeAttr public ENode	expr;

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

@ThisIsANode(name="Return", lang=CoreLang)
public class ReturnStat extends ENode {
	
	@DataFlowDefinition(jmp="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	}

	@nodeAttr public ENode	expr;

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

@ThisIsANode(name="Throw", lang=CoreLang)
public class ThrowStat extends ENode {
	
	@DataFlowDefinition(jmp="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	}

	@nodeAttr public ENode	expr;

	public ThrowStat() {}

	public ThrowStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}
}

@ThisIsANode(name="If", lang=CoreLang)
public class IfElseStat extends ENode {
	
	@DataFlowDefinition(out="join thenSt elseSt") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		cond;
	@DataFlowDefinition(in="cond:true")	ENode		thenSt;
	@DataFlowDefinition(in="cond:false")	ENode		elseSt;
	}

	@nodeAttr public ENode			cond;
	@nodeAttr public ENode			thenSt;
	@nodeAttr public ENode			elseSt;

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

@ThisIsANode(name="CondSt", lang=CoreLang)
public class CondStat extends ENode {
	
	@DataFlowDefinition(out="cond:true") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode		cond;
	@DataFlowDefinition(in="cond:false")		ENode		message;
	}

	@nodeAttr public ENode		enabled;
	@nodeAttr public ENode		cond;
	@nodeAttr public ENode		message;

	public CondStat() {}

	public CondStat(int pos, ENode cond, ENode message) {
		this.pos = pos;
		this.cond = cond;
		this.message = message;
	}
}

@ThisIsANode(name="LblSt", lang=CoreLang)
public class LabeledStat extends ENode {
	
	@DataFlowDefinition(out="stat") private static class DFI {
	@DataFlowDefinition(in="this:in")	Label			lbl;
	@DataFlowDefinition(in="lbl")		ENode			stat;
	}

	public static final LabeledStat[]	emptyArray = new LabeledStat[0];

	@nodeAttr public Label			lbl;
	@nodeAttr public ENode			stat;

	public LabeledStat() {
		this.lbl = new Label();
	}
}

@ThisIsANode(name="Break", lang=CoreLang)
public class BreakStat extends ENode {
	
	@DataFlowDefinition(jmp="this:in") private static class DFI {}

	@nodeData(copyable=false) public Label		dest;

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

@ThisIsANode(name="Continue", lang=CoreLang)
public class ContinueStat extends ENode {
	
	@DataFlowDefinition(jmp="this:in") private static class DFI {}

	@nodeData(copyable=false) public Label		dest;

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

@ThisIsANode(name="Goto", lang=CoreLang)
public class GotoStat extends ENode {
	
	@DataFlowDefinition(jmp="this:in") private static class DFI {}

	@nodeData(copyable=false) public Label		dest;

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

@ThisIsANode(name="GotoCase", lang=CoreLang)
public class GotoCaseStat extends ENode {
	
	@DataFlowDefinition(jmp="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	}

	@nodeAttr public ENode		expr;
	@nodeData public SwitchStat	sw;

	public GotoCaseStat() {}
}

