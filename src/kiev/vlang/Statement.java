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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Statement.java,v 1.6.2.1.2.2 1999/05/29 21:03:12 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.2 $
 *
 */

@node
@cfnode
public class ShadowStat extends Statement {
	@ref public Statement stat;
	
	public ShadowStat() {
	}
	public ShadowStat(Statement stat) {
		super(0,null);
		this.stat = stat;
	}
	public Type getType() { return stat.getType(); }
	public void cleanup() {
		parent = null;
		stat   = null;
	}
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
@cfnode
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

	public InlineMethodStat(int pos, ASTNode parent, Method m, Method in) {
		super(pos, parent);
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

	public void resolve(Type reqType) {
		PassInfo.push(this);
		Type[] types = new Type[params_redir.length];
		for (int i=0; i < params_redir.length; i++) {
			types[i] = params_redir[i].new_var.type;
			params_redir[i].new_var.vtype.lnk = method.params[i].type;
		}
		NodeInfoPass.pushState();
		try {
			for(int i=0; i < params_redir.length; i++) {
				NodeInfoPass.setNodeType(params_redir[i].new_var,method.params[i].type);
			}
			method.resolveDecl();
			if( method.body.isAbrupted() ) setAbrupted(true);
			if( method.body.isMethodAbrupted() ) setMethodAbrupted(true);
		} finally {
			NodeInfoPass.popState();
			for (int i=0; i < params_redir.length; i++)
				params_redir[i].new_var.vtype.lnk = types[i];
			PassInfo.pop(this);
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating InlineMethodStat");
		PassInfo.push(this);
		try {
			if( Kiev.verify )
				generateArgumentCheck();
			((Statement)method.body).generate(reqType);
		} finally { PassInfo.pop(this); }
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

	public void cleanup() {
		parent=null;
		method = null;
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
@cfnode
public class BlockStat extends Statement implements ScopeOfNames, ScopeOfMethods {

	@att public final NArr<ENode>		stats;
	
	private int resolve_pos;

	protected CodeLabel	break_label = null;

	public BlockStat() {
	}

	public BlockStat(int pos, ASTNode parent) {
		super(pos, parent);
	}

	public BlockStat(int pos, ASTNode parent, NArr<ENode> sts) {
		super(pos, parent);
		foreach (ENode st; sts) {
			this.stats.append(st);
		}
	}

	public BlockStat(int pos, ASTNode parent, ENode[] sts) {
		super(pos, parent);
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
				Kiev.reportError(decl.pos,"Symbol "+sym.getName()+" already declared in this scope");
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
				Kiev.reportError(decl.pos,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(decl,idx);
	}
	
	public rule resolveNameR(ASTNode@ node, ResInfo info, KString name)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this,this.stats),
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
		n @= new SymbolIterator(this,this.stats),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this,this.stats),
		n instanceof VarDecl && ((VarDecl)n).var.isForward(),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		((VarDecl)n).var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		assert (!isResolved());
		setResolved(true);
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			resolveBlockStats(this, stats);
		} finally {
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode n; stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
			ScopeNodeInfoVector nip_state = NodeInfoPass.popState();
			nip_state = NodeInfoPass.cleanInfoForVars(nip_state,vars.toArray());
			NodeInfoPass.addInfo(nip_state);
			PassInfo.pop(this);
		}
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
					Kiev.reportWarning(stats[i].pos,"Possible unreachable statement");
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
				Kiev.reportError(stats[i].pos,e);
			}
		}
	}


	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockStat");
		PassInfo.push(this);
		try {
			break_label = Code.newLabel();
			//Code.addVars(vars);
			for(int i=0; i < stats.length; i++) {
				try {
					stats[i].generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(stats[i].getPos(),e);
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
		} finally { PassInfo.pop(this); }
	}

	public CodeLabel getBreakLabel() throws RuntimeException {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}

	public void cleanup() {
		parent=null;
		stats.cleanup();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		for(int i=0; i < stats.length; i++) {
			ASTNode n = stats[i];
			if (n instanceof VarDecl)
				((VarDecl)n).var.toJavaDecl(dmp);
			else if (n instanceof LocalStructDecl)
				((LocalStructDecl)n).clazz.toJavaDecl(dmp);
			else
				n.toJava(dmp);
		}
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

@node
@cfnode
public class Initializer extends BlockStat implements SetBody, PreScanneable {
	/** Meta-information (annotations) of this initializer */
	@att public MetaSet					meta;
	@att public PrescannedBody			pbody;
	@att public final NArr<Statement>	addstats;

	public Initializer() {
	}

	public Initializer(int pos, int flags) {
		super(pos, null);
		setFlags(flags);
	}

	public void resolve(Type reqType) {
		super.resolve(reqType);
		for(int i=0; i < addstats.length; i++) {
			stats.insert(addstats[i],i);
			trace(Kiev.debugResolve,"Statement added to block: "+addstats[i]);
		}
		addstats.delAll();
	}

	public boolean setBody(Statement body) {
		trace(Kiev.debugMultiMethod,"Setting body of initializer "+this);
		if (this.stats.length == 0) {
			this.addStatement(body);
		}
		else {
			throw new RuntimeException("Added body to initializer "+this+" which already have statements");
		}
		return true;
	}

	public void cleanup() {
		super.cleanup();
		addstats.cleanup();
		addstats = null;
	}

}

@node
@cfnode
public class EmptyStat extends Statement {

	public EmptyStat() {}

	public EmptyStat(int pos, ASTNode parent) { super(pos, parent); }

	public void resolve(Type reqType) {
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating EmptyStat");
//		Code.setPos(pos);
//		Code.addInstr(Instr.op_nop);
	}

	public void cleanup() {
		parent=null;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(';');
	}
}

@node
@cfnode
public class ExprStat extends Statement {

	@att public ENode		expr;

	public ExprStat() {
	}

	public ExprStat(ENode expr) {
		this.expr = expr;
	}

	public ExprStat(int pos, ASTNode parent, ENode expr) {
		super(pos, parent);
		this.expr = expr;
	}

	public String toString() {
		return "stat "+expr;
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			expr.resolve(Type.tpVoid);
			expr.setGenVoidExpr(true);
		} catch(Exception e ) {
			Kiev.reportError(expr.getPos(),e);
		} finally { PassInfo.pop(this); }
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ExprStat");
		PassInfo.push(this);
		try {
			expr.generate(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(expr.getPos(),e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public Dumper toJava(Dumper dmp) {
		if( isHidden() ) dmp.append("/* ");
		expr.toJava(dmp).append(';');
		if( isHidden() ) dmp.append(" */");
		return dmp;
	}
}

//@node
//@cfnode
//public class DeclStat extends Statement {
//
//	@att public Var		var;
//	@att public Expr	init;
//
//	public DeclStat() {
//	}
//
//	public DeclStat(int pos, ASTNode parent, Var var) {
//		super(pos, parent);
//		this.var = var;
//	}
//
//	public DeclStat(int pos, ASTNode parent, Var var, Expr init) {
//		this(pos,parent,var);
//		this.init = init;
//	}
//
//	public void resolve(Type reqType) throws RuntimeException {
//		if( isResolved() ) return this;
//		PassInfo.push(this);
//		try {
//			if( init != null ) {
//				try {
//					init = init.resolveExpr(var.type);
//					Type it = init.getType();
//					if( it != var.type ) {
//						init = new CastExpr(init.pos,var.type,init);
//						init = init.resolveExpr(var.type);
//					}
//				} catch(Exception e ) {
//					Kiev.reportError(pos,e);
//				}
//			}
//			ASTNode p = parent;
//			while( p != null && !(p instanceof BlockStat || p instanceof BlockExpr) ) p = p.parent;
//			if( p != null ) {
//				if (p instanceof BlockStat)
//					((BlockStat)p).addVar(var);
//				else
//					((BlockExpr)p).addVar(var);
//				NodeInfoPass.setNodeType(var,var.type);
//				if( init != null )
//					NodeInfoPass.setNodeValue(var,init);
//			} else {
//				Kiev.reportError(pos,"Can't find scope for var "+var);
//			}
//		} finally { PassInfo.pop(this); }
//		setResolved(true);
//		return this;
//	}
//
//	public void generate(Type reqType) {
//		trace(Kiev.debugStatGen,"\tgenerating DeclStat");
//		PassInfo.push(this);
//		try {
//			if( init != null ) {
//				if( !var.isNeedRefProxy() ) {
//					init.generate(var.type);
//					Code.addVar(var);
//					Code.addInstr(Instr.op_store,var);
//				} else {
//					Type prt = Type.getProxyType(var.type);
//					Code.addInstr(Instr.op_new,prt);
//					Code.addInstr(Instr.op_dup);
//					init.generate(var.type);
//					MethodType mt = MethodType.newMethodType(null,new Type[]{init.getType()},Type.tpVoid);
//					Method@ in;
//					PassInfo.resolveBestMethodR(prt,in,new ResInfo(ResInfo.noForwards),nameInit,mt);
//					Code.addInstr(Instr.op_call,in,false);
//					Code.addVar(var);
//					Code.addInstr(Instr.op_store,var);
//				}
//			} else {
//				Code.addVar(var);
//			}
//		} catch(Exception e ) {
//			Kiev.reportError(pos,e);
//		} finally { PassInfo.pop(this); }
//	}
//
//	public void cleanup() {
//		parent=null;
//		var.cleanup();
//		var = null;
//		if( init != null ) {
//			init.cleanup();
//			init = null;
//		}
//	}
//
//	public Dumper toJava(Dumper dmp) {
//		var.toJavaDecl(dmp);
//		if( init != null )
//			dmp.space().append("=").space();
//		if( var.isNeedRefProxy() )
//			dmp.append("new").forsed_space().append(Type.getProxyType(var.type))
//			.append('(').append(init).append(')');
//		else
//			dmp.append(init);
//		return dmp.append(';');
//	}
//}

//@node
//@cfnode
//public class TypeDeclStat extends Statement/*defaults*/ {
//
//	@att public Struct		struct;
//
//	public TypeDeclStat() {
//	}
//	
//	public TypeDeclStat(int pos, ASTNode parent) {
//		super(pos, parent);
//	}
//	
//	public void resolve(Type reqType) throws RuntimeException {
//		PassInfo.push(this);
//		try {
//			try {
//				struct = (Struct)struct.resolve(Type.tpVoid);
//			} catch(Exception e ) {
//				Kiev.reportError(pos,e);
//			}
//		} finally { PassInfo.pop(this); }
//		return this;
//	}
//
//	public void generate(Type reqType) {
//		trace(Kiev.debugStatGen,"\tgenerating TypeDeclStat");
//		PassInfo.push(this);
//		try {
////			struct.generate();
//		} catch(Exception e ) {
//			Kiev.reportError(pos,e);
//		} finally { PassInfo.pop(this); }
//	}
//
//	public void cleanup() {
//		parent=null;
//		struct.cleanup();
//		struct = null;
//	}
//
//	public Dumper toJava(Dumper dmp) {
//		struct.toJavaDecl(dmp);
//		return dmp.append(';');
//	}
//}

@node
@cfnode
public class ReturnStat extends Statement/*defaults*/ {

	@att public ENode		expr;

	public ReturnStat() {
	}

	public ReturnStat(int pos, ASTNode parent, ENode expr) {
		super(pos, parent);
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public ReturnStat(int pos, ENode expr) {
		this(pos,null,expr);
	}

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		PassInfo.push(this);
		try {
			if( expr != null ) {
				try {
					expr.resolve(PassInfo.method.type.ret);
				} catch(Exception e ) {
					Kiev.reportError(expr.pos,e);
				}
			}
			if( PassInfo.method.type.ret == Type.tpVoid ) {
				if( expr != null ) throw new RuntimeException("Can't return value in void method");
				expr = null;
			} else {
				if( expr == null ) {
					throw new RuntimeException("Return must return a value in non-void method");
				}
			}
		} finally { PassInfo.pop(this); }
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ReturnStat");
		PassInfo.push(this);
		try {
			if( expr != null )
				expr.generate(PassInfo.method.type.ret);
			generateReturn();
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		if( expr != null ) {
			expr.cleanup();
			expr = null;
		}
	}

	public static void generateReturn() {
		Var tmp_var = null;
		for(int i=PassInfo.pathTop-1; i >= 0; i-- ) {
			if( PassInfo.path[i] instanceof Method ) break;
			else if( PassInfo.path[i] instanceof FinallyInfo ) {
				assert (PassInfo.path[i-1] instanceof TryStat && PassInfo.path[i].parent == PassInfo.path[i-1]);
				i--;	// skip TryStat that is parent of FinallyInfo
				continue;
			}
			else if( PassInfo.path[i] instanceof TryStat ) {
				TryStat ts = (TryStat)PassInfo.path[i];
				if( ts.finally_catcher != null ) {
					if( tmp_var==null /*&& Kiev.verify*/ && PassInfo.method.type.ret != Type.tpVoid ) {
						tmp_var = new Var(0,KString.Empty,PassInfo.method.type.ret,0);
						Code.addVar(tmp_var);
						Code.addInstr(Instr.op_store,tmp_var);
					}
					Code.addInstr(Instr.op_jsr,((FinallyInfo)ts.finally_catcher).subr_label);
				}
			}
			else if( PassInfo.path[i] instanceof SynchronizedStat ) {
				SynchronizedStat ts = (SynchronizedStat)PassInfo.path[i];
				if( tmp_var==null /*&& Kiev.verify*/ && PassInfo.method.type.ret != Type.tpVoid ) {
					tmp_var = new Var(0,KString.Empty,PassInfo.method.type.ret,0);
					Code.addVar(tmp_var);
					Code.addInstr(Instr.op_store,tmp_var);
				}
				Code.addInstr(Instr.op_load,ts.expr_var);
				Code.addInstr(Instr.op_monitorexit);
			}
		}
		if( tmp_var != null ) {
			Code.addInstr(Instr.op_load,tmp_var);
			Code.removeVar(tmp_var);
		}
		if( PassInfo.method.isGenPostCond() ) {
			Code.addInstr(Instr.op_goto,PassInfo.method.getBreakLabel());
			if( PassInfo.method.type.ret != Type.tpVoid )
				Code.stack_pop();
		} else
			Code.addInstr(Instr.op_return);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("return");
		if( expr != null )
			dmp.space().append(expr);
		return dmp.append(';');
	}
}

@node
@cfnode
public class ThrowStat extends Statement/*defaults*/ {

	@att public ENode		expr;

	public ThrowStat() {
	}

	public ThrowStat(int pos, ASTNode parent, Expr expr) {
		super(pos, parent);
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public ThrowStat(int pos, Expr expr) {
		this(pos,null,expr);
	}

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		PassInfo.push(this);
		try {
			try {
				expr.resolve(Type.tpThrowable);
			} catch(Exception e ) {
				Kiev.reportError(expr.pos,e);
			}
			Type exc = expr.getType();
			if( !PassInfo.checkException(exc) )
				Kiev.reportWarning(pos,"Exception "+exc+" must be caught or declared to be thrown");
		} finally { PassInfo.pop(this); }
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ThrowStat");
		PassInfo.push(this);
		try {
			expr.generate(null);
			Code.addInstr(Instr.op_throw);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("throw").space().append(expr).append(';');
	}
}

@node
@cfnode
public class IfElseStat extends Statement {

	@att public ENode		cond;
	@att public Statement	thenSt;
	@att public Statement	elseSt;

	public IfElseStat() {
	}
	
	public IfElseStat(int pos, ASTNode parent, Expr cond, Statement thenSt, Statement elseSt) {
		super(pos,parent);
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public IfElseStat(int pos, Expr cond, Statement thenSt, Statement elseSt) {
		this(pos,null,cond,thenSt,elseSt);
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		ScopeNodeInfoVector result_state = null;
		try {
			ScopeNodeInfoVector then_state = null;
			ScopeNodeInfoVector else_state = null;
			NodeInfoPass.pushState();
			try {
				try {
					cond.resolve(Type.tpBoolean);
					BoolExpr.checkBool(cond);
				} catch(Exception e ) {
					Kiev.reportError(cond.pos,e);
				}
			
				NodeInfoPass.pushState();
				try {
					if( cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
					else if( cond instanceof BinaryBooleanAndExpr ) {
						BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
						if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
						if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
					}
					try {
						thenSt.resolve(Type.tpVoid);
					} catch(Exception e ) {
						Kiev.reportError(thenSt.pos,e);
					}
				} finally { then_state = NodeInfoPass.popState(); }
			
			} finally { NodeInfoPass.popState(); }
			
			NodeInfoPass.pushState();
			try {
				if( cond instanceof BooleanNotExpr ) {
					BooleanNotExpr bne = (BooleanNotExpr)cond;
					if( bne.expr instanceof InstanceofExpr ) ((InstanceofExpr)bne.expr).setNodeTypeInfo();
					else if( bne.expr instanceof BinaryBooleanAndExpr ) {
						BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)bne.expr;
						if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
						if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
					}
				}
				if( elseSt != null ) {
					try {
						elseSt.resolve(Type.tpVoid);
					} catch(Exception e ) {
						Kiev.reportError(elseSt.pos,e);
					}
				}
			} finally { else_state = NodeInfoPass.popState(); }

			if (!(cond instanceof Expr) || !((Expr)cond).isConstantExpr()) {
				if( thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() ) setAbrupted(true);
				if( thenSt.isMethodAbrupted() && elseSt!=null && elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
			}
			else if (((ConstBoolExpr)cond).value) {
				if( thenSt.isAbrupted() ) setAbrupted(true);
				if( thenSt.isMethodAbrupted() ) setMethodAbrupted(true);
			}
			else if (elseSt != null){
				if( elseSt.isAbrupted() ) setAbrupted(true);
				if( elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
			}

			if( thenSt.isAbrupted() && (elseSt==null || elseSt.isAbrupted()) )
				result_state = null;
			else if( thenSt.isAbrupted() && elseSt!=null && !elseSt.isAbrupted() )
				result_state = else_state;
			else if( !thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() )
				result_state = then_state;
			else
				result_state = NodeInfoPass.joinInfo(then_state,else_state);
		} finally {
			PassInfo.pop(this);
			if( result_state != null ) NodeInfoPass.addInfo(result_state);
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating IfElseStat");
		PassInfo.push(this);
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
							ReturnStat.generateReturn();
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
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		cond.cleanup();
		cond = null;
		thenSt.cleanup();
		thenSt = null;
		if( elseSt != null ) {
			elseSt.cleanup();
			elseSt = null;
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
@cfnode
public class CondStat extends Statement {

	@att public ENode			cond;
	@att public ENode			message;

	public CondStat() {
	}

	public CondStat(int pos, ASTNode parent, ENode cond, ENode message) {
		super(pos,parent);
		this.cond = cond;
		this.message = message;
	}

	public CondStat(int pos, ENode cond, ENode message) {
		this(pos,null,cond,message);
	}

	public void resolve(Type reqType) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			try {
				cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(cond);
			} catch(Exception e ) {
				Kiev.reportError(cond.pos,e);
			}
			NodeInfoPass.pushState();
			if( cond instanceof BooleanNotExpr ) {
				BooleanNotExpr bne = (BooleanNotExpr)cond;
				if( bne.expr instanceof InstanceofExpr ) ((InstanceofExpr)bne.expr).setNodeTypeInfo();
				else if( bne.expr instanceof BinaryBooleanAndExpr ) {
					BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)bne.expr;
					if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
					if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
				}
			}
			try {
				message.resolve(Type.tpString);
			} catch(Exception e ) {
				Kiev.reportError(message.pos,e);
			}
			NodeInfoPass.popState();
		} finally {
			ScopeNodeInfoVector result_state = NodeInfoPass.popState();
			NodeInfoPass.addInfo(result_state);
			PassInfo.pop(this);
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
		PassInfo.push(this);
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
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		cond.cleanup();
		cond = null;
		message.cleanup();
		message = null;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if( !(").append(cond)
			.append(") ) throw new kiev.stdlib.AssertionFailedException(")
			.append(message).append(");").newLine();
		return dmp;
	}
}

@node
@cfnode
public class LabeledStat extends Statement/*defaults*/ implements Named {

	public static LabeledStat[]	emptyArray = new LabeledStat[0];

	@att public ASTIdentifier	ident;
	@att public Statement		stat;

	protected CodeLabel	tag_label = null;

	public LabeledStat() {
	}
	
	public NodeName getName() { return new NodeName(ident.name); }

	public void preResolve() {
		PassInfo.push(this);
		try {
			// don't pre-resolve 'ident'
			stat.preResolve();
		} finally { PassInfo.pop(this); }
	}
	
	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			stat.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat.pos,e);
		} finally { PassInfo.pop(this); }
		if( stat.isAbrupted() ) setAbrupted(true);
		if( stat.isMethodAbrupted() ) setMethodAbrupted(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating LabeledStat");
		PassInfo.push(this);
		try {
			Code.addInstr(Instr.set_label,getLabel());
			stat.generate(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat.getPos(),e);
		} finally { PassInfo.pop(this); }
	}

	public CodeLabel getLabel() {
		if( tag_label == null ) tag_label = Code.newLabel();
		return tag_label;
	}

	public void cleanup() {
		parent=null;
		stat.cleanup();
		stat = null;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.newLine(-1).append(ident).append(':').newLine(1).append(stat);
	}
}

@node
@cfnode
public class BreakStat extends Statement {

	@att public ASTIdentifier	ident;

	public BreakStat() {
	}
	
//	public BreakStat(int pos, ASTNode parent, KString name) {
//		super(pos, parent);
//		this.name = name;
//		setAbrupted(true);
//	}

	public void preResolve() {
		// don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		ASTNode p;
		if( ident == null ) {
			for(p=parent; !(
				p instanceof BreakTarget
			 || p instanceof Method
			 || (p instanceof BlockStat && p.isBreakTarget())
			 				); p = p.parent );
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
		}
		if( p instanceof Method )
			Kiev.reportError(pos,"Break not within loop/switch statement");
		p.setBreaked(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BreakStat");
		PassInfo.push(this);
		try {
			Object[] lb = PassInfo.resolveBreakLabel(ident==null?null:ident.name);
			int i=0;
			for(; i < lb.length-1; i++)
				if( lb[i] instanceof CodeLabel )
					Code.addInstr(Instr.op_jsr,(CodeLabel)lb[i]);
				else {
					Code.addInstr(Instr.op_load,(Var)lb[i]);
					Code.addInstr(Instr.op_monitorexit);
				}
			if( isAutoReturnable() )
				ReturnStat.generateReturn();
			else
				Code.addInstr(Instr.op_goto,(CodeLabel)lb[i]);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
			throw new RuntimeException(e.getMessage());
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("break");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';');
	}
}

@node
@cfnode
public class ContinueStat extends Statement/*defaults*/ {

	@att public ASTIdentifier	ident;

	public ContinueStat() {
	}
	
//	public ContinueStat(int pos, ASTNode parent, KString name) {
//		super(pos, parent);
//		this.name = name;
//		setAbrupted(true);
//	}

	public void preResolve() {
		// don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		// TODO: check label or loop statement available
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ContinueStat");
		PassInfo.push(this);
		try {
			Object[] lb = PassInfo.resolveContinueLabel(ident==null?null:ident.name);
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
			Kiev.reportError(pos,e);
			throw new RuntimeException(e.getMessage());
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("continue");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';');
	}
}

@node
@cfnode
public class GotoStat extends Statement/*defaults*/ {

	@att public ASTIdentifier	ident;

	public GotoStat() {
	}
	
//	public GotoStat(int pos, ASTNode parent, KString name) {
//		super(pos, parent);
//		this.name = name;
//		setAbrupted(true);
//	}

	public void preResolve() {
		// don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
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
			Kiev.reportWarning(st.pos,"Unknown statement in label lookup: "+st.getClass());
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
		cl3[cl3.length-1] = stat.getLabel();
		return cl3;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoStat");
		LabeledStat[] stats = resolveStat(ident.name,(Statement)PassInfo.method.body, LabeledStat.emptyArray);
		if( stats.length == 0 )
			throw new CompilerException(pos,"Label "+ident+" unresolved");
		if( stats.length > 1 )
			throw new CompilerException(pos,"Umbigouse label "+ident+" in goto statement");
		LabeledStat stat = stats[0];
		if( stat == null )
			throw new CompilerException(pos,"Label "+ident+" unresolved");
		PassInfo.push(this);
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
			Kiev.reportError(pos,e);
			throw new RuntimeException(e.getMessage());
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("goto").space().append(ident).append(';');
	}
}

@node
@cfnode
public class GotoCaseStat extends Statement/*defaults*/ {

	@att public ENode		expr;
	@ref public SwitchStat	sw;

	public GotoCaseStat() {
	}
	
//	public GotoCaseStat(int pos, ASTNode parent, Expr expr) {
//		super(pos, parent);
//		this.expr = expr;
//		setAbrupted(true);
//	}

	public void resolve(Type reqType) {
		setAbrupted(true);
		for(int i=PassInfo.pathTop-1; i >= 0; i-- ) {
			if( PassInfo.path[i] instanceof SwitchStat ) {
				sw = (SwitchStat)PassInfo.path[i];
				break;
			}
			if( PassInfo.path[i] instanceof Method ) break;
		}
		if( sw == null )
			throw new CompilerException(pos,"goto case statement not within a switch statement");
		if( expr != null ) {
			if( sw.mode == SwitchStat.TYPE_SWITCH ) {
				expr = new AssignExpr(pos,AssignOperator.Assign,
					new VarAccessExpr(pos,sw.tmpvar),expr);
				expr.resolve(Type.tpVoid);
				expr.setGenVoidExpr(true);
			} else {
				expr.resolve(sw.sel.getType());
			}
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoCaseStat");
		PassInfo.push(this);
		try {
			if( expr instanceof Expr && !((Expr)expr).isConstantExpr() ) {
				if( sw.mode == SwitchStat.TYPE_SWITCH )
					expr.generate(Type.tpVoid);
				else
					expr.generate(null);
			}

			Var tmp_var = null;
			for(int i=PassInfo.pathTop-1; i >= 0; i-- ) {
				if( PassInfo.path[i] == sw ) break;
				if( PassInfo.path[i] instanceof FinallyInfo ) {
					i--;
					continue;
				}
				if( PassInfo.path[i] instanceof TryStat ) {
					TryStat ts = (TryStat)PassInfo.path[i];
					if( ts.finally_catcher != null ) {
						if( tmp_var==null && Kiev.verify && expr instanceof Expr && !((Expr)expr).isConstantExpr() ) {
							tmp_var = new Var(0,KString.Empty,expr.getType(),0);
							Code.addVar(tmp_var);
							Code.addInstr(Instr.op_store,tmp_var);
						}
						Code.addInstr(Instr.op_jsr,((FinallyInfo)ts.finally_catcher).subr_label);
					}
				}
				else if( PassInfo.path[i] instanceof SynchronizedStat ) {
					Code.addInstr(Instr.op_load,((SynchronizedStat)PassInfo.path[i]).expr_var);
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
					lb = sw.getBreakLabel();
			}
			else if( !((Expr)expr).isConstantExpr() )
				lb = sw.getContinueLabel();
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
					Kiev.reportWarning(pos,"'goto case "+expr+"' not found, replaced by "+(sw.defCase!=null?"'goto default'":"'break"));
					if( sw.defCase != null )
						lb = ((CaseLabel)sw.defCase).getLabel();
					else
						lb = sw.getBreakLabel();
				}
			}
			Code.addInstr(Instr.op_goto,lb);
			if( expr instanceof Expr && !((Expr)expr).isConstantExpr() && sw.mode != SwitchStat.TYPE_SWITCH )
				Code.stack_pop();
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("goto");
		if( expr != null )
			dmp.append(" case ").append(expr);
		else
			dmp.space().append("default");
		return dmp.append(';');
	}
}

