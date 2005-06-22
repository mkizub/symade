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
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Statement.java,v 1.6.2.1.2.2 1999/05/29 21:03:12 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.2 $
 *
 */

public class InlineMethodStat extends Statement implements Scope {

	static class ParamRedir {
		Var		old_var;
		Var		new_var;
		ParamRedir(Var o, Var n) { old_var=o; new_var=n; }
	};


	public Method		method;
	public ParamRedir[]	params_redir;

	public InlineMethodStat(int pos, Node parent, Method m, Method in) {
		super(pos, parent);
		method = m;
		method.inlined_by_dispatcher = true;
		assert(m.params.length == in.params.length);
		params_redir = new ParamRedir[m.params.length];
		for (int i=0; i < m.params.length; i++) {
			params_redir[i] = new ParamRedir(m.params[i],in.params[i]);
		}
	}

	rule public resolveNameR(Node@ node, ResInfo path, KString name, Type tp, int resfl)
		ParamRedir@	redir;
	{
		redir @= params_redir,
		redir.old_var.name.equals(name),
		$cut,
		node ?= redir.new_var
	}

	rule public resolveMethodR(Node@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type, int resfl)
	{
		false
	}

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		Type[] types = new Type[params_redir.length];
		for (int i=0; i < params_redir.length; i++) {
			types[i] = params_redir[i].new_var.type;
			params_redir[i].new_var.type = method.params[i].type;
		}
		NodeInfoPass.pushState();
		try {
			for(int i=0; i < params_redir.length; i++) {
				NodeInfoPass.setNodeType(params_redir[i].new_var,method.params[i].type);
			}
			method.body.parent = this;
			method = (Method)method.resolve(reqType);
			if( method.body.isAbrupted() ) setAbrupted(true);
			if( method.body.isMethodAbrupted() ) setMethodAbrupted(true);
		} finally {
			PassInfo.pop(this);
			for (int i=0; i < params_redir.length; i++)
				params_redir[i].new_var.type = types[i];
		}
		return this;
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

public class BlockStat extends Statement implements Scope {

	public Node[]	stats = Statement.emptyArray;
	public Var[]		vars = Var.emptyArray;
	public Node∏		members;
	public Statement[]	addstats = Statement.emptyArray;

	protected CodeLabel	break_label = null;

	public BlockStat(int pos, Node parent) {
		super(pos, parent);
		members = new Node∏(this);
	}

	public BlockStat(int pos, Node parent, Node[] stats) {
		super(pos,parent);
		this.stats = stats;
		for(int i=0; i < stats.length; i++) stats[i].parent = this;
	}

	public Statement addStatement(Statement st) {
		stats = (Node[])Arrays.append(stats,st);
		st.parent = this;
		return st;
	}

	public Var addVar(Var var) {
		foreach(Var v; vars; v.name.equals(var.name) ) {
			Kiev.reportWarning(pos,"Variable "+var.name+" already declared in this scope");
		}
		vars = (Var[])Arrays.append(vars,var);
		return var;
	}

	rule public resolveNameR(Node@ node, ResInfo info, KString name, Type tp, int resfl)
		Node@ n;
	{
		n @= vars,
		{
			((Var)n).name.equals(name),
			node ?= n
		;	n.isForward(),
			info.enterForward(n) : info.leaveForward(n),
			Type.getRealType(tp,n.getType()).clazz.resolveNameR(node,info,name,tp,resfl | ResolveFlags.NoImports)
		}
	;	n @= members,
		{	n instanceof Struct,
			name.equals(((Struct)n).name.short_name),
			node ?= n
		;	n instanceof Typedef,
			name.equals(((Typedef)n).name),
			node ?= ((Typedef)n).type
		}
	}

	rule public resolveMethodR(Node@ node, ResInfo info, KString name, Expr[] args, Type ret, Type type, int resfl)
		Var@ n;
	{
		n @= vars,
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		Type.getRealType(type,n.getType()).clazz.resolveMethodR(node,info,name,args,ret,type,resfl | ResolveFlags.NoImports)
	}

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			resolveBlockStats();
			if( addstats.length > 0 ) {
				for(int i=0; i < addstats.length; i++) {
					stats = (Node[])Arrays.insert(stats,addstats[i],i);
					trace(Kiev.debugResolve,"Statement added to block: "+addstats[i]);
				}
				addstats = Statement.emptyArray;
			}
		} finally {
			ScopeNodeInfoVector nip_state = NodeInfoPass.popState();
			nip_state = NodeInfoPass.cleanInfoForVars(nip_state,vars);
			NodeInfoPass.addInfo(nip_state);
			PassInfo.pop(this);
		}
		return this;
	}

	public void resolveBlockStats() {
		for(int i=0; i < stats.length; i++) {
			try {
				if( (i == stats.length-1) && isAutoReturnable() )
					stats[i].setAutoReturnable(true);
				stats[i].parent = this;
				if( isAbrupted() &&
					(stats[i] instanceof LabeledStat || stats[i] instanceof ASTLabeledStatement) ) {
					setAbrupted(false);
				}
				if( isAbrupted() ) {
					Kiev.reportWarning(stats[i].pos,"Possible unreachable statement");
//					stats[i].setHidden(true);
//					continue;
				}
				if( stats[i] instanceof Statement ) {
					stats[i] = (Statement)((Statement)stats[i]).resolve(Type.tpVoid);
					if( stats[i].isAbrupted() && !isBreaked() ) setAbrupted(true);
					if( stats[i].isMethodAbrupted() && !isBreaked() ) setMethodAbrupted(true);
				}
				else if( stats[i] instanceof ASTVarDecls ) {
					ASTVarDecls vdecls = (ASTVarDecls)stats[i];
					int flags = 0;
					// TODO: check flags for fields
					for(int j=0; j < vdecls.modifier.length; j++)
						flags |= ((ASTModifier)vdecls.modifier[j]).flag();
					Type type = ((ASTType)vdecls.type).pass2();
//					if( (flags & ACC_PROLOGVAR) != 0 ) {
//            			Kiev.reportWarning(stats[i].pos,"Modifier 'pvar' is deprecated. Replace 'pvar Type' with 'Type@', please");
//						type = Type.newRefType(Type.tpPrologVar.clazz,new Type[]{type});
//					}
					Node[] vstats = new Node[0];
					for(int j=0; j < vdecls.vars.length; j++) {
						ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
						KString vname = vdecl.name;
						Type tp = type;
						for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
						DeclStat vstat;
						if( vdecl.init != null ) {
							if (!type.clazz.isWrapper() || vdecl.of_wrapper)
								vstat = (Statement)new DeclStat(
									vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags),vdecl.init);
							else
								vstat = (Statement)new DeclStat(
									vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags),
									new NewExpr(vdecl.init.pos,type,new Expr[]{vdecl.init}));
						}
//						else if( (flags & ACC_PROLOGVAR) != 0 && !vdecl.of_wrapper)
//							vstat = (Statement)new DeclStat(vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags)
//								,new NewExpr(vdecl.pos,type,Expr.emptyArray));
						else if( vdecl.dim == 0 && type.clazz.isWrapper() && !vdecl.of_wrapper)
							vstat = (Statement)new DeclStat(vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags)
								,new NewExpr(vdecl.pos,type,Expr.emptyArray));
						else
							vstat = (Statement)new DeclStat(vdecl.pos,this,new Var(vdecl.pos,vname,tp,flags));
						vstat.parent = this;
						vstat = (DeclStat)vstat.resolve(Type.tpVoid);
						vstats = (Node[])Arrays.append(vstats,vstat);
//						vars = (Var[])Arrays.append(vars,vstat.var);
					}
					stats[i] = vstats[0];
					for(int j=1; j < vstats.length; j++, i++) {
						stats = (Node[])Arrays.insert(stats,vstats[j],i+1);
					}
				}
				else if( stats[i] instanceof ASTTypeDeclaration ) {
					ASTTypeDeclaration decl = (ASTTypeDeclaration)stats[i];
					Struct cl;
					if( PassInfo.method==null || PassInfo.method.isStatic())
						decl.modifier = (Node[])Arrays.append(decl.modifier,ASTModifier.modSTATIC);
					cl = (Struct)decl.pass1();
					cl.setLocal(true);
					cl = (Struct)decl.pass2();
					cl = (Struct)decl.pass2_2();
					ASTTypeDeclaration.pass3(cl,decl.members);
					cl.autoProxyMethods();
					cl.resolveFinalFields(false);
					stats[i] = new TypeDeclStat(decl.pos,this,cl).resolve(null);
					members = (Node[])Arrays.append(members,cl);
				}
				else
					Kiev.reportError(stats[i].pos,"Unknown kind of statement/declaration "+stats[i].getClass());
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
					((Statement)stats[i]).generate(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(stats[i].getPos(),e);
				}
			}
			Code.removeVars(vars);
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
		foreach(Node n; stats; n!=null) n.cleanup();
		stats = null;
		foreach(Node n; vars; n!=null) n.cleanup();
		vars = null;
		foreach(Node n; addstats; n!=null) n.cleanup();
		addstats = null;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).newLine();
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

public class EmptyStat extends Statement {

	public EmptyStat(int pos, Node parent) { super(pos, parent); }

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		PassInfo.pop(this);
		return this;
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

public class ExprStat extends Statement {

	public Expr		expr;

	public ExprStat(int pos, Node parent, Expr expr) {
		super(pos, parent);
		this.expr = expr;
		this.expr.parent = this;
	}

	public String toString() {
		return "stat "+expr;
	}

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		try {
			expr = expr.resolveExpr(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(expr.getPos(),e);
		} finally { PassInfo.pop(this); }
		return this;
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

public class DeclStat extends Statement {

	public Var		var;
	public Expr		init;

	public DeclStat(int pos, Node parent, Var var) {
		super(pos, parent);
		this.var = var;
		this.var.parent = this;
	}

	public DeclStat(int pos, Node parent, Var var, Expr init) {
		this(pos,parent,var);
		this.init = init;
		init.parent = this;
	}

	public Node resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			if( init != null ) {
				try {
					init = init.resolveExpr(var.type);
					Type it = init.getType();
					if( it != var.type ) {
						init = new CastExpr(init.pos,var.type,init);
						init.parent = this;
						init = init.resolveExpr(var.type);
					}
				} catch(Exception e ) {
					Kiev.reportError(pos,e);
				}
			}
			Node p = parent;
			while( p != null && !(p instanceof BlockStat) ) p = p.parent;
			if( p != null ) {
				((BlockStat)p).addVar(var);
				NodeInfoPass.setNodeType(var,var.type);
				if( init != null )
					NodeInfoPass.setNodeValue(var,init);
			} else {
				Kiev.reportError(pos,"Can't find scope for var "+var);
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating DeclStat");
		PassInfo.push(this);
		try {
			if( init != null ) {
				if( !var.isNeedRefProxy() ) {
					init.generate(var.type);
					Code.addVar(var);
					Code.addInstr(Instr.op_store,var);
				} else {
					Type prt = Type.getProxyType(var.type);
					Code.addInstr(Instr.op_new,prt);
					Code.addInstr(Instr.op_dup);
					init.generate(var.type);
					PVar<Method> in = new PVar<Method>();
					PassInfo.resolveBestMethodR(prt.clazz,in,new ResInfo(),
						nameInit,new Expr[]{init},Type.tpVoid,null,ResolveFlags.NoForwards);
					Code.addInstr(Instr.op_call,in,false);
					Code.addVar(var);
					Code.addInstr(Instr.op_store,var);
				}
			} else {
				Code.addVar(var);
			}
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		var.cleanup();
		var = null;
		if( init != null ) {
			init.cleanup();
			init = null;
		}
	}

	public Dumper toJava(Dumper dmp) {
		var.toJavaDecl(dmp);
		if( init != null )
			dmp.space().append("=").space();
		if( var.isNeedRefProxy() )
			dmp.append("new").forsed_space().append(Type.getProxyType(var.type))
			.append('(').append(init).append(')');
		else
			dmp.append(init);
		return dmp.append(';');
	}
}

public class TypeDeclStat extends Statement/*defaults*/ {

	public Struct		struct;

	public TypeDeclStat(int pos, Node parent, Struct struct) {
		super(pos, parent);
		this.struct = struct;
		this.struct.parent = this;
	}

	public Node resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		try {
			try {
				struct = (Struct)struct.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(pos,e);
			}
		} finally { PassInfo.pop(this); }
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating TypeDeclStat");
		PassInfo.push(this);
		try {
//			struct.generate();
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public void cleanup() {
		parent=null;
		struct.cleanup();
		struct = null;
	}

	public Dumper toJava(Dumper dmp) {
		struct.toJavaDecl(dmp);
		return dmp.append(';');
	}
}

public class ReturnStat extends Statement/*defaults*/ {

	public Expr		expr;

	public ReturnStat(int pos, Node parent, Expr expr) {
		super(pos, parent);
		this.expr = expr;
		if( expr != null)
			this.expr.parent = this;
		setMethodAbrupted(true);
	}

	public ReturnStat(int pos, Expr expr) {
		this(pos,null,expr);
	}

	public Node resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		try {
			if( expr != null ) {
				try {
					expr = expr.resolveExpr(PassInfo.method.type.ret);
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
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ReturnStat");
		PassInfo.push(this);
		try {
			if( expr != null )
				expr.generate(Type.getRealType(Kiev.argtype,PassInfo.method.type.ret));
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

public class ThrowStat extends Statement/*defaults*/ {

	public Expr		expr;

	public ThrowStat(int pos, Node parent, Expr expr) {
		super(pos, parent);
		this.expr = expr;
		this.expr.parent = this;
		setMethodAbrupted(true);
	}

	public ThrowStat(int pos, Expr expr) {
		this(pos,null,expr);
	}

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		try {
			try {
				expr = expr.resolveExpr(Type.tpThrowable);
			} catch(Exception e ) {
				Kiev.reportError(expr.pos,e);
			}
			Type exc = expr.getType();
			if( !PassInfo.checkException(exc) )
				Kiev.reportWarning(pos,"Exception "+exc+" must be caught or declared to be thrown");
		} finally { PassInfo.pop(this); }
		return this;
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

public class IfElseStat extends Statement {

	public BooleanExpr	cond;
	public Statement	thenSt;
	public Statement	elseSt;

	public IfElseStat(int pos, Node parent, BooleanExpr cond, Statement thenSt, Statement elseSt) {
		super(pos,parent);
		this.cond = cond;
		this.cond.parent = this;
		this.thenSt = thenSt;
		this.thenSt.parent = this;
		if( elseSt != null ) {
			this.elseSt = elseSt;
			this.elseSt.parent = this;
		}
	}

	public IfElseStat(int pos, BooleanExpr cond, Statement thenSt, Statement elseSt) {
		this(pos,null,cond,thenSt,elseSt);
	}

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		ScopeNodeInfoVector result_state = null;
		try {
			try {
				cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
			} catch(Exception e ) {
				Kiev.reportError(cond.pos,e);
			}
			NodeInfoPass.pushState();
			if( cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
			else if( cond instanceof BinaryBooleanAndExpr ) {
				BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
				if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
				if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
			}
			try {
				thenSt = (Statement)thenSt.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(thenSt.pos,e);
			}
			ScopeNodeInfoVector then_state = NodeInfoPass.popState();
			NodeInfoPass.popState();
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
			if( elseSt != null ) {
				try {
					elseSt = (Statement)elseSt.resolve(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(elseSt.pos,e);
				}
			}
			ScopeNodeInfoVector else_state = NodeInfoPass.popState();

			if( thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() && elseSt!=null && elseSt.isMethodAbrupted() ) setMethodAbrupted(true);

			if( thenSt.isAbrupted() && (elseSt==null || elseSt.isAbrupted()) )
				result_state = null;
			else if( thenSt.isAbrupted() && elseSt!=null && !elseSt.isAbrupted() )
				result_state = else_state;
			else if( !thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() )
				result_state = then_state;
			else
				result_state = NodeInfoPass.joinInfo(then_state,else_state);

			if( !(cond instanceof BooleanExpr) ) {
				throw new RuntimeException("Condition of if-else statement must be a boolean expression, but type "+cond.getType()+" found");
			}
		} finally {
			PassInfo.pop(this);
			if( result_state != null ) NodeInfoPass.addInfo(result_state);
		}
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating IfElseStat");
		PassInfo.push(this);
		try {
			BooleanExpr cond = this.cond;
			if( cond.isGenResolve() ) {
				cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
			}
			if( cond.isConstantExpr() ) {
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
				((BooleanExpr)cond).generate_iffalse(else_label);
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
		if( cond.isGenResolve() ) {
			Kiev.gen_resolve = true;
			try {
				Expr c = (BooleanExpr)cond.resolve(Type.tpBoolean);
				if( c.isConstantExpr() ) {
					if( ((Boolean)c.getConstValue()).booleanValue() )
						dmp.append(thenSt);
					else if( elseSt != null )
						dmp.append(elseSt);
					return dmp;
				}
			} finally { Kiev.gen_resolve = false; }
		}
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

public class CondStat extends Statement {

	public BooleanExpr	cond;
	public Expr			message;

	public CondStat(int pos, Node parent, BooleanExpr cond, Expr message) {
		super(pos,parent);
		this.cond = cond;
		this.cond.parent = this;
		this.message = message;
		this.message.parent = this;
	}

	public CondStat(int pos, BooleanExpr cond, Expr message) {
		this(pos,null,cond,message);
	}

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			try {
				cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
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
				message = (Expr)message.resolve(Type.tpString);
			} catch(Exception e ) {
				Kiev.reportError(message.pos,e);
			}
			NodeInfoPass.popState();

			if( !(cond instanceof BooleanExpr) ) {
				throw new RuntimeException("Condition of if-else statement must be a boolean expression, but type "+cond.getType()+" found");
			}
		} finally {
			PassInfo.pop(this);
			ScopeNodeInfoVector result_state = NodeInfoPass.popState();
			NodeInfoPass.addInfo(result_state);
		}
		return this;
	}

	private KString getAssertMethodName() {
		WorkByContractCondition wbc = (WorkByContractCondition)parent.parent;
		switch( wbc.cond ) {
		case WorkByContractCondition.CondRequire:	return nameAssertRequireMethod;
		case WorkByContractCondition.CondEnsure:	return nameAssertEnsureMethod;
		case WorkByContractCondition.CondInvariant:	return nameAssertInvariantMethod;
		default: return nameAssertMethod;
		}
	}

	private KString getAssertMethodSignature() {
		WorkByContractCondition wbc = (WorkByContractCondition)parent.parent;
		if( wbc.name == null )
			return nameAssertSignature;
		else
			return nameAssertNameSignature;
	}

	private void generateAssertName() {
		WorkByContractCondition wbc = (WorkByContractCondition)parent.parent;
		if( wbc.name == null ) return;
		Code.addConst((KString)wbc.name);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating CondStat");
		PassInfo.push(this);
		try {
			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() );
				else {
					generateAssertName();
					message.generate(Type.tpString);
					Method func = Type.tpDebug.clazz.resolveMethod(
						getAssertMethodName(),
						getAssertMethodSignature());
					Code.addInstr(Instr.op_call,func,false);
				}
			} else {
				CodeLabel else_label = Code.newLabel();
				((BooleanExpr)cond).generate_iftrue(else_label);
				generateAssertName();
				message.generate(Type.tpString);
				Method func = Type.tpDebug.clazz.resolveMethod(
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

public class LabeledStat extends Statement/*defaults*/ implements Named {

	public static LabeledStat[]	emptyArray = new LabeledStat[0];

	public KString		name;
	public Statement	stat;

	protected CodeLabel	tag_label = null;

	public LabeledStat(int pos, Node parent, KString name, Statement stat) {
		super(pos, parent);
		this.name = name;
		this.stat = stat;
		this.stat.parent = this;
	}

	public NodeName getName() { return new NodeName(name); }

	public Node resolve(Type reqType) {
		PassInfo.push(this);
		try {
			stat = (Statement)stat.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat.pos,e);
		} finally { PassInfo.pop(this); }
		if( stat.isAbrupted() ) setAbrupted(true);
		if( stat.isMethodAbrupted() ) setMethodAbrupted(true);
		return this;
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
		return dmp.newLine(-1).append(name).append(':').newLine(1).append(stat);
	}
}

public class BreakStat extends Statement/*defaults*/ {

	public KString		name;

	public BreakStat(int pos, Node parent, KString name) {
		super(pos, parent);
		this.name = name;
		setAbrupted(true);
	}

	public Node resolve(Type reqType) {
		Node p;
		if( name == null ) {
			for(p=parent; !(
				p instanceof BreakTarget
			 || p instanceof Method
			 || (p instanceof BlockStat && p.isBreakTarget())
			 				); p = p.parent );
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat &&
					((LabeledStat)p).getName().equals(name) )
					throw new RuntimeException("Label "+name+" does not refer to break target");
				if( !(p instanceof BreakTarget || p instanceof BlockStat ) ) continue;
				Node pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(name) ) {
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
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BreakStat");
		PassInfo.push(this);
		try {
			Object[] lb = PassInfo.resolveBreakLabel(name);
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
		if( name != null && !name.equals(KString.Empty) )
			dmp.space().append(name);
		return dmp.append(';');
	}
}

public class ContinueStat extends Statement/*defaults*/ {

	public KString		name;

	public ContinueStat(int pos, Node parent, KString name) {
		super(pos, parent);
		this.name = name;
		setAbrupted(true);
	}

	public Node resolve(Type reqType) {
		// TODO: check label or loop statement available
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ContinueStat");
		PassInfo.push(this);
		try {
			Object[] lb = PassInfo.resolveContinueLabel(name);
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
		if( name != null && !name.equals(KString.Empty) )
			dmp.space().append(name);
		return dmp.append(';');
	}
}

public class GotoStat extends Statement/*defaults*/ {

	public KString		name;

	public GotoStat(int pos, Node parent, KString name) {
		super(pos, parent);
		this.name = name;
		setAbrupted(true);
	}

	public Node resolve(Type reqType) {
		return this;
	}

	public static LabeledStat[] resolveStat(KString name, Statement st, LabeledStat[] stats) {
		int i;
		switch( st ) {
		case SwitchStat:
		{
			SwitchStat bst = (SwitchStat)st;
			for(int j=0; j < bst.cases.length; j++ ) {
				CaseLabel cl = (CaseLabel)bst.cases[j];
				stats = resolveStat(name,cl.stats,stats);
			}
		}
			break;
		case BlockStat:
		{
			BlockStat bst = (BlockStat)st;
			for(i=0; i < bst.stats.length; i++ ) {
				stats = resolveStat(name,(Statement)bst.stats[i],stats);
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
			if( lst.name.equals(name) ) {
				stats = (LabeledStat[])Arrays.appendUniq(stats,lst);
			}
			stats = resolveStat(name,lst.stat,stats);
		}
			break;
		case EmptyStat: 	break;
		case TypeDeclStat:	break;
		case DeclStat:	    break;
		case GotoStat:  	break;
		case GotoCaseStat: 	break;
		case ReturnStat:	break;
		case ThrowStat: 	break;
		case ExprStat:	    break;
		case BreakStat:	    break;
		case ContinueStat:	break;
		default:
			Kiev.reportWarning(st.pos,"Unknown statement in label lookup: "+st.getClass());
		}
		return stats;
	}

	public Object[] resolveLabelStat(LabeledStat stat) {
		Object[] cl1 = new CodeLabel[0];
		Object[] cl2 = new CodeLabel[0];
		Node st = stat;
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
		LabeledStat[] stats = resolveStat(name,(Statement)PassInfo.method.body, LabeledStat.emptyArray);
		if( stats.length == 0 )
			throw new CompilerException(pos,"Label "+name+" unresolved");
		if( stats.length > 1 )
			throw new CompilerException(pos,"Umbigouse label "+name+" in goto statement");
		LabeledStat stat = stats[0];
		if( stat == null )
			throw new CompilerException(pos,"Label "+name+" unresolved");
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
		return dmp.append("goto").space().append(name).append(';');
	}
}

public class GotoCaseStat extends Statement/*defaults*/ {

	public Expr			expr;
	public SwitchStat	sw;

	public GotoCaseStat(int pos, Node parent, Expr expr) {
		super(pos, parent);
		this.expr = expr;
		if( expr != null )
			expr.parent = this;
		setAbrupted(true);
	}

	public Node resolve(Type reqType) {
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
				expr = (Expr)new AssignExpr(pos,AssignOperator.Assign,
					new VarAccessExpr(pos,sw.tmpvar),expr).resolve(Type.tpVoid);
			} else {
				expr = expr.resolveExpr(sw.sel.getType());
			}
		}
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating GotoCaseStat");
		PassInfo.push(this);
		try {
			if( expr != null && !expr.isConstantExpr() ) {
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
						if( tmp_var==null && Kiev.verify && expr != null && !expr.isConstantExpr() ) {
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
			if( expr == null ) {
				if( sw.defCase != null )
					lb = ((CaseLabel)sw.defCase).getLabel();
				else
					lb = sw.getBreakLabel();
			}
			else if( !expr.isConstantExpr() )
				lb = sw.getContinueLabel();
			else {
				int goto_value = ((Number)((ConstExpr)expr).getConstValue()).intValue();
				foreach(Node an; sw.cases) {
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
			if( expr != null && !expr.isConstantExpr() && sw.mode != SwitchStat.TYPE_SWITCH )
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

