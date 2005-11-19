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
@dflow(out="this:in")
public class ShadowStat extends Statement {
	@ref public Statement stat;
	
	public ShadowStat() {
	}
	public ShadowStat(Statement stat) {
		super(0);
		this.stat = stat;
	}
	public Type getType() { return stat.getType(); }

	public void resolve(Type reqType) {
		stat.resolve(reqType);
		setResolved(true);
	}

	public void generate(Type reqType) {
		stat.generate(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		return stat.toJava(dmp);
	}

}

@node
@dflow(in="root()", out="this:out()")
public class InlineMethodStat extends Statement implements ScopeOfNames {

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

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
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
				in = in.addNodeType(new DNode[]{node.params_redir[i].new_var},node.method.params[i].type);
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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		Code.setLinePos(this.getPosLine());
		if( Kiev.verify )
			generateArgumentCheck();
		foreach (ParamRedir redir; params_redir)
			redir.old_var.setBCpos(redir.new_var.getBCpos());
		((Statement)method.body).generate(reqType);
	}

	public void generateArgumentCheck() {
		for(int i=0; i < params_redir.length; i++) {
			ParamRedir redir = params_redir[i];
			if( !redir.new_var.type.equals(method.params[i].type) ) {
				Code.addInstr(Instr.op_load,redir.new_var);
				Code.addInstr(Instr.op_checkcast,method.params[i].type);
				Code.addInstr(Instr.op_store,redir.new_var);
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
@dflow(out="this:out()")
public class BlockStat extends Statement implements ScopeOfNames, ScopeOfMethods {

	@dflow(in="", seq="true")
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
	
	public rule resolveNameR(ASTNode@ node, ResInfo info, KString name)
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
		;	n instanceof Typedef,
			name.equals(((Typedef)n).name),
			node ?= ((Typedef)n).type
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
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
				if( stats[i] instanceof Statement ) {
					Statement st = (Statement)stats[i];
					st.resolve(Type.tpVoid);
					st = (Statement)stats[i];
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


	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockStat");
		Code.setLinePos(this.getPosLine());
		break_label = Code.newLabel();
		//Code.addVars(vars);
		for(int i=0; i < stats.length; i++) {
			try {
				stats[i].generate(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		Vector<Var> vars = new Vector<Var>();
		foreach (ASTNode n; stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
		Code.removeVars(vars.toArray());
		if( parent instanceof Method && Kiev.debugOutputC
		 && parent.isGenPostCond() && ((Method)parent).type.ret != Type.tpVoid) {
			Code.stack_push(((Method)parent).type.ret);
		}
		Code.addInstr(Instr.set_label,break_label);
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
@dflow(out="this:in")
public class EmptyStat extends Statement {

	public EmptyStat() {}

	public EmptyStat(int pos) { super(pos); }

	public void resolve(Type reqType) {
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating EmptyStat");
//		Code.setPos(pos);
//		Code.addInstr(Instr.op_nop);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(';').newLine();
	}
}

@node
@dflow(out="expr")
public class ExprStat extends Statement {

	@dflow
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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ExprStat");
		try {
			expr.generate(Type.tpVoid);
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
@dflow(jmp="expr")
public class ReturnStat extends Statement/*defaults*/ {

	@att
	@dflow(in="this:in")
	public ENode		expr;

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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ReturnStat");
		Code.setLinePos(this.getPosLine());
		try {
			if( expr != null )
				expr.generate(Code.method.type.ret);
			generateReturn(this);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public static void generateReturn(ASTNode from) {
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
					if( tmp_var==null /*&& Kiev.verify*/ && Code.method.type.ret != Type.tpVoid ) {
						tmp_var = new Var(0,KString.Empty,Code.method.type.ret,0);
						Code.addVar(tmp_var);
						Code.addInstr(Instr.op_store,tmp_var);
					}
					Code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
				}
			}
			else if (node instanceof SynchronizedStat) {
				if( tmp_var==null /*&& Kiev.verify*/ && Code.method.type.ret != Type.tpVoid ) {
					tmp_var = new Var(0,KString.Empty,Code.method.type.ret,0);
					Code.addVar(tmp_var);
					Code.addInstr(Instr.op_store,tmp_var);
				}
				Code.addInstr(Instr.op_load,node.expr_var);
				Code.addInstr(Instr.op_monitorexit);
			}
		}
		if( tmp_var != null ) {
			Code.addInstr(Instr.op_load,tmp_var);
			Code.removeVar(tmp_var);
		}
		if( Code.method.isGenPostCond() ) {
			Code.addInstr(Instr.op_goto,Code.method.getBreakLabel());
			if( Code.method.type.ret != Type.tpVoid )
				Code.stack_pop();
		} else
			Code.addInstr(Instr.op_return);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("return");
		if( expr != null )
			dmp.space().append(expr);
		return dmp.append(';').newLine();
	}
}

@node
@dflow(jmp="expr")
public class ThrowStat extends Statement/*defaults*/ {

	@att
	@dflow(in="")
	public ENode		expr;

	public ThrowStat() {
	}

	public ThrowStat(int pos, Expr expr) {
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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ThrowStat");
		Code.setLinePos(this.getPosLine());
		try {
			expr.generate(null);
			Code.addInstr(Instr.op_throw);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("throw").space().append(expr).append(';').newLine();
	}
}

@node
@dflow(out="join thenSt elseSt")
public class IfElseStat extends Statement {

	@att
	@dflow
	public ENode		cond;
	
	@att
	@dflow(in="cond:true")
	public Statement	thenSt;
	
	@att
	@dflow(in="cond:false")
	public Statement	elseSt;

	public IfElseStat() {
	}
	
	public IfElseStat(int pos, Expr cond, Statement thenSt, Statement elseSt) {
		super(pos);
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void resolve(Type reqType) {
		Code.setLinePos(this.getPosLine());
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

		if (!(cond instanceof Expr) || !((Expr)cond).isConstantExpr()) {
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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating IfElseStat");
		Code.setLinePos(this.getPosLine());
		try {
			if( cond instanceof Expr && ((Expr)cond).isConstantExpr() ) {
				Expr cond = (Expr)this.cond;
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					if( isAutoReturnable() )
						thenSt.setAutoReturnable(true);
					thenSt.generate(Type.tpVoid);
				}
				else if( elseSt != null ) {
					if( isAutoReturnable() )
						elseSt.setAutoReturnable(true);
					elseSt.generate(Type.tpVoid);
				}
			} else {
				CodeLabel else_label = Code.newLabel();
				BoolExpr.gen_iffalse(cond, else_label);
				thenSt.generate(Type.tpVoid);
				if( elseSt != null ) {
					CodeLabel end_label = Code.newLabel();
					if( !thenSt.isMethodAbrupted() ) {
						if( isAutoReturnable() )
							ReturnStat.generateReturn(this);
						else if (!thenSt.isAbrupted())
							Code.addInstr(Instr.op_goto,end_label);
					}
					Code.addInstr(Instr.set_label,else_label);
					elseSt.generate(Type.tpVoid);
					Code.addInstr(Instr.set_label,end_label);
				} else {
					Code.addInstr(Instr.set_label,else_label);
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
@dflow(out="cond:true")
public class CondStat extends Statement {

	@att
	@dflow(in="")
	public ENode			cond;
	
	@att
	@dflow(in="cond:false")
	public ENode			message;

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

	private void generateAssertName() {
		WBCCondition wbc = (WBCCondition)parent.parent;
		if( wbc.name == null ) return;
		Code.addConst((KString)wbc.name.name);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating CondStat");
		Code.setLinePos(this.getPosLine());
		try {
			if(cond instanceof Expr && ((Expr)cond).isConstantExpr() ) {
				Expr cond = (Expr)this.cond;
				if( ((Boolean)cond.getConstValue()).booleanValue() );
				else {
					generateAssertName();
					message.generate(Type.tpString);
					Method func = Type.tpDebug.resolveMethod(
						getAssertMethodName(),
						getAssertMethodSignature());
					Code.addInstr(Instr.op_call,func,false);
				}
			} else {
				CodeLabel else_label = Code.newLabel();
				BoolExpr.gen_iftrue(cond, else_label);
				generateAssertName();
				message.generate(Type.tpString);
				Method func = Type.tpDebug.resolveMethod(
					getAssertMethodName(),
					getAssertMethodSignature());
				Code.addInstr(Instr.op_call,func,false);
				Code.addInstr(Instr.set_label,else_label);
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
@dflow(out="stat")
public class LabeledStat extends Statement/*defaults*/ implements Named {

	public static LabeledStat[]	emptyArray = new LabeledStat[0];

	@att
	public NameRef			ident;
	
	@att(copyable=false)
	@dflow(in="")
	public Label			lbl;

	@att
	@dflow(in="lbl")
	public Statement		stat;

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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating LabeledStat");
		Code.setLinePos(this.getPosLine());
		try {
			lbl.generate(Type.tpVoid);
			stat.generate(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat,e);
		}
	}

	public CodeLabel getCodeLabel() {
		return lbl.getCodeLabel();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.newLine(-1).append(ident).append(':').newLine(1).append(stat);
	}
}

@node
@dflow(jmp="this:in")
public class BreakStat extends Statement {

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
			 || (p instanceof BlockStat && p.isBreakTarget())
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
			 || (p instanceof BlockStat && p.isBreakTarget())
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
		p.setBreaked(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BreakStat");
		Code.setLinePos(this.getPosLine());
		try {
			Object[] lb = resolveBreakLabel();
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel ) {
					Code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				}
				else {
					Code.addInstr(Instr.op_load,(Var)lb[i]);
					Code.addInstr(Instr.op_monitorexit);
				}
			if( isAutoReturnable() )
				ReturnStat.generateReturn(this);
			else
				Code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	private Object[] resolveBreakLabel() {
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
					return (Object[])Arrays.append(cl,t.getBrkLabel().getCodeLabel());
				}
				else if( node instanceof BlockStat && node.isBreakTarget() ){
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
					Statement st = node.stat;
					if( st instanceof BreakTarget )
						return (Object[])Arrays.append(cl,st.getBrkLabel().getCodeLabel());
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
@dflow(jmp="this:in")
public class ContinueStat extends Statement/*defaults*/ {

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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ContinueStat");
		Code.setLinePos(this.getPosLine());
		try {
			Object[] lb = resolveContinueLabel();
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					Code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					Code.addInstr(Instr.op_load,(Var)lb[i]);
					Code.addInstr(Instr.op_monitorexit);
				}
			Code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			throw new RuntimeException(e.getMessage());
		}
	}

	/** Returns array of CodeLabel (to op_jsr) or Var (to op_monitorexit) */
	private Object[] resolveContinueLabel() {
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
					return (Object[])Arrays.append(cl,node.getCntLabel().getCodeLabel());
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
					Statement st = node.stat;
					if( st instanceof ContinueTarget )
						return (Object[])Arrays.append(cl,st.getCntLabel().getCodeLabel());
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
@dflow(jmp="this:in")
public class GotoStat extends Statement/*defaults*/ {

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

	public Object[] resolveLabelStat(LabeledStat stat) {
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
		cl3[cl3.length-1] = stat.getCodeLabel();
		return cl3;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoStat");
		LabeledStat[] stats = resolveStat(ident.name,Code.method.body, LabeledStat.emptyArray);
		if( stats.length == 0 )
			throw new CompilerException(this,"Label "+ident+" unresolved");
		if( stats.length > 1 )
			throw new CompilerException(this,"Umbigouse label "+ident+" in goto statement");
		LabeledStat stat = stats[0];
		if( stat == null )
			throw new CompilerException(this,"Label "+ident+" unresolved");
		Code.setLinePos(this.getPosLine());
		try {
			Object[] lb = resolveLabelStat(stat);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					Code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					Code.addInstr(Instr.op_load,(Var)lb[i]);
					Code.addInstr(Instr.op_monitorexit);
				}
			Code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
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
@dflow(jmp="expr")
public class GotoCaseStat extends Statement/*defaults*/ {

	@att
	@dflow(in="")
	public ENode		expr;
	
	@ref
	public SwitchStat	sw;

	public GotoCaseStat() {
	}
	
//	public GotoCaseStat(int pos, ASTNode parent, Expr expr) {
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

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoCaseStat");
		Code.setLinePos(this.getPosLine());
		try {
			if( expr instanceof Expr && !((Expr)expr).isConstantExpr() ) {
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					expr.generate(Type.tpVoid);
				else
					expr.generate(null);
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
						if( tmp_var==null && Kiev.verify && expr instanceof Expr && !((Expr)expr).isConstantExpr() ) {
							tmp_var = new Var(0,KString.Empty,expr.getType(),0);
							Code.addVar(tmp_var);
							Code.addInstr(Instr.op_store,tmp_var);
						}
						Code.addInstr(Instr.op_jsr,node.finally_catcher.subr_label);
					}
				}
				else if (node instanceof SynchronizedStat) {
					Code.addInstr(Instr.op_load,node.expr_var);
					Code.addInstr(Instr.op_monitorexit);
				}
			}
			if( tmp_var != null ) {
				Code.addInstr(Instr.op_load,tmp_var);
				Code.removeVar(tmp_var);
			}
			CodeLabel lb = null;
			if !( expr instanceof Expr ) {
				if( sw.defCase != null )
					lb = ((CaseLabel)sw.defCase).getLabel();
				else
					lb = sw.getBrkLabel().getCodeLabel();
			}
			else if( !((Expr)expr).isConstantExpr() )
				lb = sw.getCntLabel().getCodeLabel();
			else {
				int goto_value = ((Number)((ConstExpr)expr).getConstValue()).intValue();
				foreach(ASTNode an; sw.cases) {
					CaseLabel cl = (CaseLabel)an;
					int case_value = ((Number)((ConstExpr)cl.val).getConstValue()).intValue();
					if( goto_value == case_value ) {
						lb = cl.getLabel();
						break;
					}
				}
				if( lb == null ) {
					Kiev.reportWarning(this,"'goto case "+expr+"' not found, replaced by "+(sw.defCase!=null?"'goto default'":"'break"));
					if( sw.defCase != null )
						lb = ((CaseLabel)sw.defCase).getLabel();
					else
						lb = sw.getBrkLabel().getCodeLabel();
				}
			}
			Code.addInstr(Instr.op_goto,lb);
			if( expr instanceof Expr && !((Expr)expr).isConstantExpr() && sw.mode != SwitchStat.TYPE_SWITCH )
				Code.stack_pop();
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

