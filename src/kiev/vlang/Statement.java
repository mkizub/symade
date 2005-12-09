/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ShadowStat extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@ref public ENode stat;
	
	public ShadowStat() {
	}
	public ShadowStat(ENode stat) {
		super(0);
		this.stat = stat;
	}
	public Type getType() { return stat.getType(); }

	public void resolve(Type reqType) {
		stat.resolve(reqType);
		setResolved(true);
	}

	public void generate(Code code, Type reqType) {
		stat.generate(code,reqType);
	}

	public Dumper toJava(Dumper dmp) {
		return stat.toJava(dmp);
	}

}

@node
public class InlineMethodStat extends ENode implements ScopeOfNames {
	
	@dflow(in="root()", out="this:out()") private static class DFI {}

	static class ParamRedir {
		FormPar		old_var;
		FormPar		new_var;
		ParamRedir(FormPar o, FormPar n) { old_var=o; new_var=n; }
	};


	@att public Method		method;
	public ParamRedir[]		params_redir;

	public InlineMethodStat() {
	}

	public InlineMethodStat(int pos, Method m, Method in) {
		super(pos);
		method = m;
		method.inlined_by_dispatcher = true;
		assert(m.params.length == in.params.length);
		params_redir = new ParamRedir[m.params.length];
		for (int i=0; i < m.params.length; i++) {
			params_redir[i] = new ParamRedir(m.params[i],in.params[i]);
		}
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		ParamRedir@	redir;
	{
		redir @= params_redir,
		redir.old_var.name.equals(name),
		$cut,
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
			InlineMethodStat node = (InlineMethodStat)dfi.node;
			DFState in = DFState.makeNewState();
			for(int i=0; i < node.params_redir.length; i++) {
				in = in.declNode(node.params_redir[i].new_var);
				in = in.addNodeType(new LvalDNode[]{node.params_redir[i].new_var},node.method.params[i].type);
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
			InlineMethodStat node = (InlineMethodStat)dfi.node;
			DataFlowInfo pdfi = node.parent.getDFlow();
			res = DFFunc.calc(pdfi.getSocket(node.pslot.name).func_in, pdfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new InlineMethodStatDFFuncOut(dfi);
	}

	public void resolve(Type reqType) {
		Type[] types = new Type[params_redir.length];
		for (int i=0; i < params_redir.length; i++) {
			types[i] = params_redir[i].new_var.type;
			params_redir[i].new_var.vtype.lnk = method.params[i].type;
		}
		try {
			method.resolveDecl();
			if( method.body.isAbrupted() ) setAbrupted(true);
			if( method.body.isMethodAbrupted() ) setMethodAbrupted(true);
		} finally {
			for (int i=0; i < params_redir.length; i++)
				params_redir[i].new_var.vtype.lnk = types[i];
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		code.setLinePos(this.getPosLine());
		if( Kiev.verify )
			generateArgumentCheck(code);
		foreach (ParamRedir redir; params_redir)
			redir.old_var.setBCpos(redir.new_var.getBCpos());
		method.body.generate(code,reqType);
	}

	public void generateArgumentCheck(Code code) {
		for(int i=0; i < params_redir.length; i++) {
			ParamRedir redir = params_redir[i];
			if( !redir.new_var.type.equals(method.params[i].type) ) {
				code.addInstr(Instr.op_load,redir.new_var);
				code.addInstr(Instr.op_checkcast,method.params[i].type);
				code.addInstr(Instr.op_store,redir.new_var);
			}
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		foreach (ParamRedir redir; params_redir)
			dmp.append("/* ")
			.append(redir.old_var.type.toString()).space().append(redir.old_var)
			.append('=').append(redir.new_var)
			.append(';').append(" */").newLine();
		dmp.append("/* Body of method "+method+" */").newLine();
		if (method.body == null)
			dmp.append(';');
		else
			dmp.append(method.body);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

@node
public class BlockStat extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		stats;
	}

	@att public final NArr<ENode>		stats;
	
	private int resolve_pos;

	protected CodeLabel	break_label = null;

	public BlockStat() {
	}

	public BlockStat(int pos) {
		super(pos);
	}

	public BlockStat(int pos, NArr<ENode> sts) {
		super(pos);
		foreach (ENode st; sts) {
			this.stats.append(st);
		}
	}

	public BlockStat(int pos, ENode[] sts) {
		super(pos);
		foreach (ENode st; sts) {
			this.stats.append(st);
		}
	}

	public ENode addStatement(ENode st) {
		stats.append(st);
		return st;
	}

	public void addSymbol(Named sym) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ENode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.append(decl);
	}

	public void insertSymbol(Named sym, int idx) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ASTNode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(decl,idx);
	}
	
	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof VarDecl,
			((VarDecl)n).var.name.equals(name),
			node ?= ((VarDecl)n).var
		;	n instanceof LocalStructDecl,
			name.equals(((LocalStructDecl)n).clazz.name.short_name),
			node ?= ((LocalStructDecl)n).clazz
		;	n instanceof TypeDefOp,
			name.equals(((TypeDefOp)n).name),
			node ?= ((TypeDefOp)n)
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		info.space_prev != null && info.space_prev.pslot.name == "stats",
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward(),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		((VarDecl)n).var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		assert (!isResolved());
		setResolved(true);
		resolveBlockStats(this, stats);
	}

	static class BlockStatDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		BlockStatDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("stats"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			BlockStat node = (BlockStat)dfi.node;
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode n; node.stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
			if (vars.length > 0)
				res = DFFunc.calc(f, dfi).cleanInfoForVars(vars.toArray());
			else
				res = DFFunc.calc(f, dfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new BlockStatDFFunc(dfi);
	}

	public static void resolveBlockStats(ENode self, NArr<ENode> stats) {
		for(int i=0; i < stats.length; i++) {
			try {
				if( (i == stats.length-1) && self.isAutoReturnable() )
					stats[i].setAutoReturnable(true);
				if( self.isAbrupted() && (stats[i] instanceof LabeledStat) ) {
					self.setAbrupted(false);
				}
				if( self.isAbrupted() ) {
					//Kiev.reportWarning(stats[i].pos,"Possible unreachable statement");
				}
				if( stats[i] instanceof ENode ) {
					ENode st = stats[i];
					st.resolve(Type.tpVoid);
					st = stats[i];
					if( st.isAbrupted() && !self.isBreaked() ) self.setAbrupted(true);
					if( st.isMethodAbrupted() && !self.isBreaked() ) self.setMethodAbrupted(true);
				}
				else {
					stats[i].resolve(Type.tpVoid);
				}
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
	}


	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockStat");
		code.setLinePos(this.getPosLine());
		break_label = code.newLabel();
		//code.addVars(vars);
		for(int i=0; i < stats.length; i++) {
			try {
				stats[i].generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		Vector<Var> vars = new Vector<Var>();
		foreach (ASTNode n; stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
		code.removeVars(vars.toArray());
		if( parent instanceof Method && Kiev.debugOutputC
		 && ((Method)parent).isGenPostCond() && ((Method)parent).type.ret != Type.tpVoid) {
			code.stack_push(((Method)parent).type.ret);
		}
		code.addInstr(Instr.set_label,break_label);
	}

	public CodeLabel getBreakLabel() throws RuntimeException {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		foreach (ENode s; stats)
			s.toJava(dmp);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

@node
public class EmptyStat extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	public EmptyStat() {}

	public EmptyStat(int pos) { super(pos); }

	public void resolve(Type reqType) {
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating EmptyStat");
//		code.setPos(pos);
//		code.addInstr(Instr.op_nop);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(';').newLine();
	}
}

@node
public class ExprStat extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@att public ENode		expr;

	public ExprStat() {
	}

	public ExprStat(ENode expr) {
		this.expr = expr;
	}

	public ExprStat(int pos, ENode expr) {
		super(pos);
		this.expr = expr;
	}

	public String toString() {
		return "stat "+expr;
	}

	public void resolve(Type reqType) {
		try {
			expr.resolve(Type.tpVoid);
			expr.setGenVoidExpr(true);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ExprStat");
		try {
			expr.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
	}

	public Dumper toJava(Dumper dmp) {
		if( isHidden() ) dmp.append("/* ");
		expr.toJava(dmp).append(';');
		if( isHidden() ) dmp.append(" */");
		return dmp.newLine();
	}
}

@node
public class ReturnStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@att public ENode		expr;

	public ReturnStat() {
	}

	public ReturnStat(int pos, ENode expr) {
		super(pos);
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		if( expr != null ) {
			try {
				expr.resolve(pctx.method.type.ret);
			} catch(Exception e ) {
				Kiev.reportError(expr,e);
			}
		}
		if( pctx.method.type.ret == Type.tpVoid ) {
			if( expr != null ) throw new RuntimeException("Can't return value in void method");
			expr = null;
		} else {
			if( expr == null ) {
				throw new RuntimeException("Return must return a value in non-void method");
			}
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ReturnStat");
		code.setLinePos(this.getPosLine());
		try {
			if( expr != null )
				expr.generate(code,code.method.type.ret);
			generateReturn(code,this);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public static void generateReturn(Code code, ASTNode from) {
		Var tmp_var = null;
		for(ASTNode node = from; node != null; node = node.parent) {
			if (node instanceof Method)
				break;
			else if (node instanceof FinallyInfo) {
				assert (node.parent instanceof TryStat);
				node = node.parent; // skip TryStat that is parent of FinallyInfo
				continue;
			}
			else if (node instanceof TryStat) {
				if( node.finally_catcher != null ) {
					if( tmp_var==null /*&& Kiev.verify*/ && code.method.type.ret != Type.tpVoid ) {
						tmp_var = new Var(0,KString.Empty,code.method.type.ret,0);
						code.addVar(tmp_var);
						code.addInstr(Instr.op_store,tmp_var);
					}
					code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
				}
			}
			else if (node instanceof SynchronizedStat) {
				if( tmp_var==null /*&& Kiev.verify*/ && code.method.type.ret != Type.tpVoid ) {
					tmp_var = new Var(0,KString.Empty,code.method.type.ret,0);
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
		if( code.method.isGenPostCond() ) {
			code.addInstr(Instr.op_goto,code.method.getBreakLabel());
			if( code.method.type.ret != Type.tpVoid )
				code.stack_pop();
		} else
			code.addInstr(Instr.op_return);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("return");
		if( expr != null )
			dmp.space().append(expr);
		return dmp.append(';').newLine();
	}
}

@node
public class ThrowStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@att public ENode		expr;

	public ThrowStat() {
	}

	public ThrowStat(int pos, ENode expr) {
		super(pos);
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		try {
			expr.resolve(Type.tpThrowable);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
		Type exc = expr.getType();
		if( !PassInfo.checkException(this,exc) )
			Kiev.reportWarning(this,"Exception "+exc+" must be caught or declared to be thrown");
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ThrowStat");
		code.setLinePos(this.getPosLine());
		try {
			expr.generate(code,null);
			code.addInstr(Instr.op_throw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("throw").space().append(expr).append(';').newLine();
	}
}

@node
public class IfElseStat extends ENode {
	
	@dflow(out="join thenSt elseSt") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		thenSt;
	@dflow(in="cond:false")	ENode		elseSt;
	}

	@att public ENode		cond;
	
	@att public ENode		thenSt;
	
	@att public ENode		elseSt;

	public IfElseStat() {
	}
	
	public IfElseStat(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		super(pos);
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
	
		try {
			thenSt.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(thenSt,e);
		}
		if( elseSt != null ) {
			try {
				elseSt.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(elseSt,e);
			}
		}

		if (!cond.isConstantExpr()) {
			if( thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() && elseSt!=null && elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (cond.getConstValue() instanceof Boolean && ((Boolean)cond.getConstValue()).booleanValue()) {
			if( thenSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (elseSt != null){
			if( elseSt.isAbrupted() ) setAbrupted(true);
			if( elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating IfElseStat");
		code.setLinePos(this.getPosLine());
		try {
			if( cond.isConstantExpr() ) {
				ENode cond = this.cond;
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
				BoolExpr.gen_iffalse(code, cond, else_label);
				thenSt.generate(code,Type.tpVoid);
				if( elseSt != null ) {
					CodeLabel end_label = code.newLabel();
					if( !thenSt.isMethodAbrupted() ) {
						if( isAutoReturnable() )
							ReturnStat.generateReturn(code,this);
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("if(").space().append(cond).space()
			.append(')');
		if( /*thenSt instanceof ExprStat ||*/ thenSt instanceof BlockStat || thenSt instanceof InlineMethodStat) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(thenSt);
		if( /*thenSt instanceof ExprStat ||*/ thenSt instanceof BlockStat || thenSt instanceof InlineMethodStat) dmp.newLine();
		else dmp.newLine(-1);
		if( elseSt != null ) {
			dmp.append("else");
			if( elseSt instanceof IfElseStat || elseSt instanceof BlockStat || elseSt instanceof InlineMethodStat ) dmp.forsed_space();
			else dmp.newLine(1);
			dmp.append(elseSt).newLine();
			if( elseSt instanceof IfElseStat || elseSt instanceof BlockStat || elseSt instanceof InlineMethodStat ) dmp.newLine();
			else dmp.newLine(-1);
		}
		return dmp;
	}
}

@node
public class CondStat extends ENode {
	
	@dflow(out="cond:true") private static class DFI {
	@dflow(in="this:in")		ENode		cond;
	@dflow(in="cond:false")		ENode		message;
	}

	@att public ENode			cond;
	
	@att public ENode			message;

	public CondStat() {
	}

	public CondStat(int pos, ENode cond, ENode message) {
		super(pos);
		this.cond = cond;
		this.message = message;
	}

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		try {
			message.resolve(Type.tpString);
		} catch(Exception e ) {
			Kiev.reportError(message,e);
		}
	}

	private KString getAssertMethodName() {
		WBCCondition wbc = (WBCCondition)parent.parent;
		switch( wbc.cond ) {
		case WBCType.CondRequire:	return nameAssertRequireMethod;
		case WBCType.CondEnsure:	return nameAssertEnsureMethod;
		case WBCType.CondInvariant:	return nameAssertInvariantMethod;
		default: return nameAssertMethod;
		}
	}

	private KString getAssertMethodSignature() {
		WBCCondition wbc = (WBCCondition)parent.parent;
		if( wbc.name == null )
			return nameAssertSignature;
		else
			return nameAssertNameSignature;
	}

	private void generateAssertName(Code code) {
		WBCCondition wbc = (WBCCondition)parent.parent;
		if( wbc.name == null ) return;
		code.addConst((KString)wbc.name.name);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating CondStat");
		code.setLinePos(this.getPosLine());
		try {
			if(cond.isConstantExpr() ) {
				ENode cond = this.cond;
				if( ((Boolean)cond.getConstValue()).booleanValue() );
				else {
					generateAssertName(code);
					message.generate(code,Type.tpString);
					Method func = Type.tpDebug.clazz.resolveMethod(
						getAssertMethodName(),
						getAssertMethodSignature());
					code.addInstr(Instr.op_call,func,false);
				}
			} else {
				CodeLabel else_label = code.newLabel();
				BoolExpr.gen_iftrue(code, cond, else_label);
				generateAssertName(code);
				message.generate(code,Type.tpString);
				Method func = Type.tpDebug.clazz.resolveMethod(
					getAssertMethodName(),
					getAssertMethodSignature());
				code.addInstr(Instr.op_call,func,false);
				code.addInstr(Instr.set_label,else_label);
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if( !(").append(cond)
			.append(") ) throw new kiev.stdlib.AssertionFailedException(")
			.append(message).append(");").newLine();
		return dmp;
	}
}

@node
public class LabeledStat extends ENode implements Named {
	
	@dflow(out="stat") private static class DFI {
	@dflow(in="this:in")	Label			lbl;
	@dflow(in="lbl")		ENode			stat;
	}

	public static LabeledStat[]	emptyArray = new LabeledStat[0];

	@att
	public NameRef			ident;
	
	@att(copyable=false)
	public Label			lbl;

	@att
	public ENode			stat;

	public LabeledStat() {
		lbl = new Label();
	}
	
	public NodeName getName() { return new NodeName(ident.name); }

	public void resolve(Type reqType) {
		try {
			stat.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat,e);
		}
		if( stat.isAbrupted() ) setAbrupted(true);
		if( stat.isMethodAbrupted() ) setMethodAbrupted(true);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating LabeledStat");
		code.setLinePos(this.getPosLine());
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

	public Dumper toJava(Dumper dmp) {
		return dmp.newLine(-1).append(ident).append(':').newLine(1).append(stat);
	}
}

@node
public class BreakStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@att public NameRef			ident;
	
	@ref public Label			dest;

	public BreakStat() {
	}
	
//	public BreakStat(int pos, ASTNode parent, KString name) {
//		super(pos, parent);
//		this.name = name;
//		setAbrupted(true);
//	}

	public void callbackRootChanged() {
		if (dest != null && dest.pctx.root != this.pctx.root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		ASTNode p;
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(
				p instanceof BreakTarget
			 || p instanceof Method
			 || (p instanceof BlockStat && ((BlockStat)p).isBreakTarget())
			 				); p = p.parent );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = ((LoopStat)p).getBrkLabel();
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat &&
					((LabeledStat)p).getName().equals(ident.name) )
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if( !(p instanceof BreakTarget || p instanceof BlockStat ) ) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(ident.name) ) {
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
					Label l = ((LoopStat)p).getBrkLabel();
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		}
		return false; // don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		ASTNode p;
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(
				p instanceof BreakTarget
			 || p instanceof Method
			 || (p instanceof BlockStat && ((BlockStat)p).isBreakTarget())
			 				); p = p.parent );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = ((LoopStat)p).getBrkLabel();
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat &&
					((LabeledStat)p).getName().equals(ident.name) )
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if( !(p instanceof BreakTarget || p instanceof BlockStat ) ) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(ident.name) ) {
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
					Label l = ((LoopStat)p).getBrkLabel();
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		}
		if( p instanceof Method )
			Kiev.reportError(this,"Break not within loop/switch statement");
		((ENode)p).setBreaked(true);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BreakStat");
		code.setLinePos(this.getPosLine());
		try {
			Object[] lb = resolveBreakLabel(code);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel ) {
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				}
				else {
					code.addInstr(Instr.op_load,(Var)lb[i]);
					code.addInstr(Instr.op_monitorexit);
				}
			if( isAutoReturnable() )
				ReturnStat.generateReturn(code,this);
			else
				code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	private Object[] resolveBreakLabel(Code code) {
		KString name = ident==null?null:ident.name;
		Object[] cl = new Object[0];
		if( name == null || name.equals(KString.Empty) ) {
			// Search for loop statements
			for(ASTNode node = this.parent; node != null; node = node.parent) {
				if( node instanceof TryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof Method ) break;
				if( node instanceof BreakTarget || node instanceof BlockStat );
				else continue;
				if( node instanceof BreakTarget ) {
					BreakTarget t = (BreakTarget)node;
					return (Object[])Arrays.append(cl,t.getBrkLabel().getCodeLabel(code));
				}
				else if( node instanceof BlockStat && ((BlockStat)node).isBreakTarget() ){
					BlockStat t = (BlockStat)node;
					return (Object[])Arrays.append(cl,t.getBreakLabel());
				}
			}
			throw new RuntimeException("Break not within loop statement");
		} else {
			// Search for labels with loop/switch statement
			for(ASTNode node = this.parent; node != null; node = node.parent) {
				if( node instanceof TryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof Method ) break;
				if( node instanceof LabeledStat && ((LabeledStat)node).getName().equals(name) ) {
					ENode st = node.stat;
					if( st instanceof BreakTarget )
						return (Object[])Arrays.append(cl,st.getBrkLabel().getCodeLabel(code));
					else if (st instanceof BlockStat)
						return (Object[])Arrays.append(cl,st.getBreakLabel());
					else
						throw new RuntimeException("Label "+name+" does not refer to break target");
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a break target");
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("break");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@node
public class ContinueStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@att public NameRef			ident;

	@ref public Label			dest;

	public ContinueStat() {
	}
	
//	public ContinueStat(int pos, ASTNode parent, KString name) {
//		super(pos, parent);
//		this.name = name;
//		setAbrupted(true);
//	}

	public void callbackRootChanged() {
		if (dest != null && dest.pctx.root != this.pctx.root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		ASTNode p;
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(p instanceof LoopStat || p instanceof Method); p = p.parent );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Continue not within loop statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = ((LoopStat)p).getCntLabel();
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat && ((LabeledStat)p).getName().equals(ident.name) )
					throw new RuntimeException("Label "+ident+" does not refer to continue target");
				if !(p instanceof LoopStat) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(ident.name) ) {
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
					Label l = ((LoopStat)p).getCntLabel();
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		}
		return false; // don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		// TODO: check label or loop statement available
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ContinueStat");
		code.setLinePos(this.getPosLine());
		try {
			Object[] lb = resolveContinueLabel(code);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					code.addInstr(Instr.op_load,(Var)lb[i]);
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
		KString name = ident==null?null:ident.name;
		Object[] cl = new Object[0];
		if( name == null || name.equals(KString.Empty) ) {
			// Search for loop statements
			for(ASTNode node = this.parent; node != null; node = node.parent) {
				if( node instanceof TryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof Method ) break;
				if( node instanceof ContinueTarget )
					return (Object[])Arrays.append(cl,node.getCntLabel().getCodeLabel(code));
			}
			throw new RuntimeException("Continue not within loop statement");
		} else {
			// Search for labels with loop statement
			for(ASTNode node = this.parent; node != null; node = node.parent) {
				if( node instanceof TryStat ) {
					if( node.finally_catcher != null )
						cl = (Object[])Arrays.append(cl,node.finally_catcher.subr_label);
				}
				else if( node instanceof SynchronizedStat ) {
					cl = (Object[])Arrays.append(cl,node.expr_var);
				}
				if( node instanceof Method ) break;
				if( node instanceof LabeledStat && ((LabeledStat)node).getName().equals(name) ) {
					ENode st = node.stat;
					if( st instanceof ContinueTarget )
						return (Object[])Arrays.append(cl,st.getCntLabel().getCodeLabel(code));
					throw new RuntimeException("Label "+name+" does not refer to continue target");
				}
			}
		}
		throw new RuntimeException("Label "+name+" unresolved or isn't a continue target");
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("continue");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@node
public class GotoStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@att public NameRef			ident;

	@ref public Label			dest;

	public GotoStat() {
	}
	
//	public GotoStat(int pos, ASTNode parent, KString name) {
//		super(pos, parent);
//		this.name = name;
//		setAbrupted(true);
//	}

	public void callbackRootChanged() {
		if (dest != null && dest.pctx.root != this.pctx.root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		LabeledStat[] stats = resolveStat(ident.name,pctx.method.body, LabeledStat.emptyArray);
		if( stats.length == 0 ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return false;
		}
		if( stats.length > 1 ) {
			Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
		}
		LabeledStat stat = stats[0];
		if( stat == null ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return false;
		}
		dest = stat.lbl;
		dest.addLink(this);
		return false; // don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		LabeledStat[] stats = resolveStat(ident.name,pctx.method.body, LabeledStat.emptyArray);
		if( stats.length == 0 ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		if( stats.length > 1 ) {
			Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
		}
		LabeledStat stat = stats[0];
		if( stat == null ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		dest = stat.lbl;
		dest.addLink(this);
	}

	public static LabeledStat[] resolveStat(KString name, ASTNode st, LabeledStat[] stats) {
		int i;
		switch( st ) {
		case SwitchStat:
		{
			SwitchStat bst = (SwitchStat)st;
			for(int j=0; j < bst.cases.length; j++ ) {
				CaseLabel cl = (CaseLabel)bst.cases[j];
				for(i=0; i < cl.stats.length; i++ ) {
					stats = resolveStat(name,cl.stats[i],stats);
				}
			}
		}
			break;
		case BlockStat:
		{
			BlockStat bst = (BlockStat)st;
			for(i=0; i < bst.stats.length; i++ ) {
				stats = resolveStat(name,bst.stats[i],stats);
			}
		}
			break;
		case TryStat:
		{
			TryStat tst = (TryStat)st;
			stats = resolveStat(name,tst.body,stats);
			for(i=0; i < tst.catchers.length; i++) {
				stats = resolveStat(name,((CatchInfo)tst.catchers[i]).body,stats);
			}
		}
			break;
		case WhileStat:
		{
			WhileStat wst = (WhileStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case DoWhileStat:
		{
			DoWhileStat wst = (DoWhileStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case ForStat:
		{
			ForStat wst = (ForStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case ForEachStat:
		{
			ForEachStat wst = (ForEachStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case IfElseStat:
		{
			IfElseStat wst = (IfElseStat)st;
			stats = resolveStat(name,wst.thenSt,stats);
			if( wst.elseSt != null )
				stats = resolveStat(name,wst.elseSt,stats);
		}
			break;
		case LabeledStat:
		{
			LabeledStat lst = (LabeledStat)st;
			if( lst.ident.name.equals(name) ) {
				stats = (LabeledStat[])Arrays.appendUniq(stats,lst);
			}
			stats = resolveStat(name,lst.stat,stats);
		}
			break;
		case EmptyStat: 		break;
		case LocalStructDecl:	break;
		case VarDecl:			break;
		case GotoStat:			break;
		case GotoCaseStat:		break;
		case ReturnStat:		break;
		case ThrowStat:			break;
		case ExprStat:			break;
		case BreakStat:			break;
		case ContinueStat:		break;
		default:
			Kiev.reportWarning(st,"Unknown statement in label lookup: "+st.getClass());
		}
		return stats;
	}

	public Object[] resolveLabelStat(Code code, LabeledStat stat) {
		Object[] cl1 = new CodeLabel[0];
		Object[] cl2 = new CodeLabel[0];
		ASTNode st = stat;
		while( !(st instanceof Method) ) {
			if( st instanceof FinallyInfo ) {
				st = st.parent.parent;
				continue;
			}
			else if( st instanceof TryStat ) {
				TryStat ts = (TryStat)st;
				if( ts.finally_catcher != null )
					cl1 = (Object[])Arrays.append(cl1,((FinallyInfo)ts.finally_catcher).subr_label);
			}
			else if( st instanceof SynchronizedStat ) {
				cl1 = (Object[])Arrays.append(cl1,((SynchronizedStat)st).expr_var);
			}
			st = st.parent;
		}
		st = this;
		while( !(st instanceof Method) ) {
			if( st instanceof FinallyInfo ) {
				st = st.parent.parent;
				continue;
			}
			if( st instanceof TryStat ) {
				TryStat ts = (TryStat)st;
				if( ts.finally_catcher != null )
					cl2 = (Object[])Arrays.append(cl2,((FinallyInfo)ts.finally_catcher).subr_label);
			}
			else if( st instanceof SynchronizedStat ) {
				cl2 = (Object[])Arrays.append(cl2,((SynchronizedStat)st).expr_var);
			}
			st = st.parent;
		}
		int i = 0;
		for(; i < cl2.length && i < cl1.length; i++ )
			if( cl1[i] != cl2[i] ) break;
		Object[] cl3 = new Object[ cl2.length - i + 1 ];
		System.arraycopy(cl2,i,cl3,0,cl3.length-1);
		cl3[cl3.length-1] = stat.getCodeLabel(code);
		return cl3;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoStat");
		LabeledStat[] stats = resolveStat(ident.name,code.method.body, LabeledStat.emptyArray);
		if( stats.length == 0 )
			throw new CompilerException(this,"Label "+ident+" unresolved");
		if( stats.length > 1 )
			throw new CompilerException(this,"Umbigouse label "+ident+" in goto statement");
		LabeledStat stat = stats[0];
		if( stat == null )
			throw new CompilerException(this,"Label "+ident+" unresolved");
		code.setLinePos(this.getPosLine());
		try {
			Object[] lb = resolveLabelStat(code,stat);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					code.addInstr(Instr.op_load,(Var)lb[i]);
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

	public Dumper toJava(Dumper dmp) {
		return dmp.append("goto").space().append(ident).append(';').newLine();
	}
}

@node
public class GotoCaseStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@att public ENode		expr;
	
	@ref public SwitchStat	sw;

	public GotoCaseStat() {
	}
	
//	public GotoCaseStat(int pos, ASTNode parent, ENode expr) {
//		super(pos, parent);
//		this.expr = expr;
//		setAbrupted(true);
//	}

	public void resolve(Type reqType) {
		setAbrupted(true);
		for(ASTNode node = this.parent; node != null; node = node.parent) {
			if (node instanceof SwitchStat) {
				this.sw = (SwitchStat)node;
				break;
			}
			if (node instanceof Method)
				break;
		}
		if( this.sw == null )
			throw new CompilerException(this,"goto case statement not within a switch statement");
		if( expr != null ) {
			if( sw.mode == SwitchStat.TYPE_SWITCH ) {
				expr = new AssignExpr(pos,AssignOperator.Assign,
					new LVarExpr(pos,sw.tmpvar.getVar()),(ENode)~expr);
				expr.resolve(Type.tpVoid);
				expr.setGenVoidExpr(true);
			} else {
				expr.resolve(sw.sel.getType());
			}
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoCaseStat");
		code.setLinePos(this.getPosLine());
		try {
			if( !expr.isConstantExpr() ) {
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					expr.generate(code,Type.tpVoid);
				else
					expr.generate(code,null);
			}

			Var tmp_var = null;
			for(ASTNode node = this.parent; node != null; node = node.parent) {
				if (node == sw)
					break;
				if (node instanceof FinallyInfo) {
					node = node.parent; // skip calling jsr if we are in it
					continue;
				}
				if (node instanceof TryStat) {
					if( node.finally_catcher != null ) {
						if( tmp_var==null && Kiev.verify && !expr.isConstantExpr() ) {
							tmp_var = new Var(0,KString.Empty,expr.getType(),0);
							code.addVar(tmp_var);
							code.addInstr(Instr.op_store,tmp_var);
						}
						code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
					}
				}
				else if (node instanceof SynchronizedStat) {
					code.addInstr(Instr.op_load,node.expr_var);
					code.addInstr(Instr.op_monitorexit);
				}
			}
			if( tmp_var != null ) {
				code.addInstr(Instr.op_load,tmp_var);
				code.removeVar(tmp_var);
			}
			CodeLabel lb = null;
			if !( expr instanceof ENode ) {
				if( sw.defCase != null )
					lb = ((CaseLabel)sw.defCase).getLabel(code);
				else
					lb = sw.getBrkLabel().getCodeLabel(code);
			}
			else if( !expr.isConstantExpr() )
				lb = sw.getCntLabel().getCodeLabel(code);
			else {
				int goto_value = ((Number)((ConstExpr)expr).getConstValue()).intValue();
				foreach(ASTNode an; sw.cases) {
					CaseLabel cl = (CaseLabel)an;
					int case_value = ((Number)((ConstExpr)cl.val).getConstValue()).intValue();
					if( goto_value == case_value ) {
						lb = cl.getLabel(code);
						break;
					}
				}
				if( lb == null ) {
					Kiev.reportWarning(this,"'goto case "+expr+"' not found, replaced by "+(sw.defCase!=null?"'goto default'":"'break"));
					if( sw.defCase != null )
						lb = ((CaseLabel)sw.defCase).getLabel(code);
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("goto");
		if( expr != null )
			dmp.append(" case ").append(expr);
		else
			dmp.space().append("default");
		return dmp.append(';').newLine();
	}
}

