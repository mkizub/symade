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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/LoopStatement.java,v 1.5.2.1 1999/02/12 18:47:07 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1 $
 *
 */

public abstract class LoopStat extends Statement implements BreakTarget, ContinueTarget {

	protected	CodeLabel	continue_label = null;
	protected	CodeLabel	break_label = null;

	protected LoopStat(int pos, ASTNode parent) {
		super(pos, parent);
		setBreakTarget(true);
	}

	public CodeLabel getContinueLabel() throws RuntimeException {
		if( continue_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'continue' label");
		return continue_label;
	}

	public CodeLabel getBreakLabel() throws RuntimeException {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}
}


public class WhileStat extends LoopStat {

	public BooleanExpr	cond;
	public Statement	body;

	public WhileStat(int pos, ASTNode parent, BooleanExpr cond, Statement body) {
		super(pos, parent);
		this.cond = cond;
		this.cond.parent = this;
		this.body = body;
		this.body.parent = this;
	}

	public void cleanup() {
		parent=null;
		cond.cleanup();
		cond = null;
		body.cleanup();
		body = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		try {
			try {
				cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
			} catch(Exception e ) { Kiev.reportError(cond.pos,e); }
			if( cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
			else if( cond instanceof BinaryBooleanAndExpr ) {
				BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
				if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
				if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
			}
			try {
				body = (Statement)body.resolve(Type.tpVoid);
			} catch(Exception e ) { Kiev.reportError(body.pos,e); }
			if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
				setMethodAbrupted(true);
			}
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
		}
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating WhileStat");
		PassInfo.push(this);
		try {
			continue_label = Code.newLabel();
			break_label = Code.newLabel();
			CodeLabel body_label = Code.newLabel();

			Code.addInstr(Instr.op_goto,continue_label);
			Code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(Type.tpVoid);
			Code.addInstr(Instr.set_label,continue_label);

			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					Code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				((BooleanExpr)cond).generate_iftrue(body_label);
			}
			Code.addInstr(Instr.set_label,break_label);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("while").space().append('(').space().append(cond)
			.space().append(')');
		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(body);
		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.newLine();
		else dmp.newLine(-1);
		return dmp;
	}
}

public class DoWhileStat extends LoopStat {

	public BooleanExpr	cond;
	public Statement	body;

	public DoWhileStat(int pos, ASTNode parent, BooleanExpr cond, Statement body) {
		super(pos,parent);
		this.cond = cond;
		this.cond.parent = this;
		this.body = body;
		this.body.parent = this;
	}

	public void cleanup() {
		parent=null;
		cond.cleanup();
		cond = null;
		body.cleanup();
		body = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		try {
			try {   body = (Statement)body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(body.pos,e);
			}
			try {
				cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
				if( !(cond instanceof BooleanExpr) )
					cond = (BooleanExpr)new BooleanWrapperExpr(cond.pos,cond).resolve(Type.tpBoolean);
			} catch(Exception e ) {
				Kiev.reportError(cond.pos,e);
			}
			if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
				setMethodAbrupted(true);
			}
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
		}
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating DoWhileStat");
		PassInfo.push(this);
		try {
			continue_label = Code.newLabel();
			break_label = Code.newLabel();
			CodeLabel body_label = Code.newLabel();

// Differ from WhileStat in this:	Code.addInstr(Instr.op_goto,continue_label);
			Code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(Type.tpVoid);
			Code.addInstr(Instr.set_label,continue_label);

			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					Code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				((BooleanExpr)cond).generate_iftrue(body_label);
			}
			Code.addInstr(Instr.set_label,break_label);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("do");

		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(body);
		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.newLine();
		else dmp.newLine(-1);

		dmp.append("while").space().append('(').space().append(cond).space().append(");").newLine();
		return dmp;
	}
}

public class ForInit extends ASTNode implements ScopeOfNames {

	public Type	type;
	public Var[]	vars;
	public Expr[]	inits;

	public ForInit(int pos, Type type, Var[] vars, Expr[] inits) {
		super(pos);
		this.type = type;
		this.vars = vars;
		this.inits = inits;
	}

	public void cleanup() {
		parent=null;
		type = null;
		vars = null;
		foreach(ASTNode n; inits; n!=null) n.cleanup();
		inits = null;
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	rule public resolveNameR(ASTNode@ node, List<ASTNode>@ path, KString name, Type tp, int resfl)
	{
		node @= vars, ((Var)node).name.equals(name)
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append(type).space();
		for(int i=0; i < vars.length; i++) {
			dmp.append(vars[i].name);
			for(Type tp=vars[i].type; tp.isArray(); tp = tp.args[0])
				dmp.append("[]");
			if( inits[i] != null ) dmp.space().append('=').space().append(inits[i]);
			if( i < vars.length-1 ) dmp.append(',').space();
		}
		return dmp;
	}
}

public class ForStat extends LoopStat implements ScopeOfNames {

	public ASTNode		init;
	public BooleanExpr	cond;
	public Expr			iter;
	public Statement	body;

	public ForStat(int pos, ASTNode parent, ASTNode init, BooleanExpr cond, Expr iter, Statement body) {
		super(pos, parent);
		if( init != null ) {
			this.init = init;
			this.init.parent = this;
		}
		if( cond != null ) {
			this.cond = cond;
			this.cond.parent = this;
		}
		if( iter != null ) {
			this.iter = iter;
			this.iter.parent = this;
		}
		this.body = body;
		this.body.parent = this;
	}

	public void cleanup() {
		parent=null;
		if( init != null ) {
			init.cleanup();
			init = null;
			}
		if( cond != null ) {
			cond.cleanup();
			cond = null;
			}
		if( iter != null ) {
			iter.cleanup();
			iter = null;
			}
		body.cleanup();
		body = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		try {
			if( init != null ) {
				try {
					if( init instanceof Statement )
						init = ((Statement)init).resolve(Type.tpVoid);
					else if( init instanceof Expr )
						init = ((Expr)init).resolve(Type.tpVoid);
					else if( init instanceof ASTVarDecls ) {
						ASTVarDecls vdecls = (ASTVarDecls)init;
						int flags = 0;
						Type type = ((ASTType)vdecls.type).pass2();
						int dim = 0;
						while( type.isArray() ) { dim++; type = type.args[0]; }
						Var[] vars = new Var[vdecls.vars.length];
						Expr[] inits = new Expr[vdecls.vars.length];
						for(int j=0; j < vdecls.vars.length; j++) {
							ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
							KString vname = vdecl.name;
							Type tp = type;
							for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
							for(int k=0; k < dim; k++) tp = Type.newArrayType(tp);
							vars[j] = new Var(vdecl.pos,this,vname,tp,flags);
							if( vdecl.init != null )
								inits[j] = vdecl.init.resolveExpr(vars[j].type);
							PassInfo.addResolvedNode(vars[j],this);
						}
						init = new ForInit(init.pos,type,vars,inits);
					}
					else
						throw new RuntimeException("Unknown type of for-init node "+init);
				} catch(Exception e ) {
					Kiev.reportError(init.pos,e);
				}
			}
			if(  cond != null && cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
			else if(  cond != null && cond instanceof BinaryBooleanAndExpr ) {
				BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
				if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
				if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
			}
			if( cond != null ) {
				try {
					cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
				} catch(Exception e ) {
					Kiev.reportError(cond.pos,e);
				}
			}
			try {   body = (Statement)body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(body.pos,e);
			}
			if( iter != null ) {
				try {	iter = (Expr)iter.resolve(Type.tpVoid);
				} catch(Exception e ) {
					Kiev.reportError(iter.pos,e);
				}
			}
			if( ( cond==null
				|| (cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue())
				)
				&& !isBreaked()
			) {
				setMethodAbrupted(true);
			}
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
		}
		return this;
	}

	rule public resolveNameR(ASTNode@ node, List<ASTNode>@ path, KString name, Type tp, int resfl)
	{
		init instanceof ForInit, ((ForInit)init).resolveNameR(node,path,name,tp,resfl)
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ForStat");
		continue_label = Code.newLabel();
		break_label = Code.newLabel();
		CodeLabel body_label = Code.newLabel();
		CodeLabel check_label = Code.newLabel();

		PassInfo.push(this);
		try {
			if( init != null ) {
				if( init instanceof Statement )
					((Statement)init).generate(Type.tpVoid);
				else if( init instanceof Expr )
					((Expr)init).generate(Type.tpVoid);
				else if( init instanceof ForInit ) {
					ForInit fi = (ForInit)init;
					for(int i=0; i < fi.vars.length; i++) {
						Code.addVar(fi.vars[i]);
						if( fi.inits[i] != null ) {
							fi.inits[i].generate(fi.vars[i].type);
							Code.addInstr(Instr.op_store,fi.vars[i]);
						}
					}
				}
			}

			if( cond != null ) {
				Code.addInstr(Instr.op_goto,check_label);
			}

			Code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(Type.tpVoid);

			Code.addInstr(Instr.set_label,continue_label);
			if( iter != null )
				iter.generate(Type.tpVoid);

			Code.addInstr(Instr.set_label,check_label);
			if( cond != null ) {
				if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() )
					Code.addInstr(Instr.op_goto,body_label);
				else if( cond.isConstantExpr() && !((Boolean)cond.getConstValue()).booleanValue() );
				else ((BooleanExpr)cond).generate_iftrue(body_label);
			} else {
				Code.addInstr(Instr.op_goto,body_label);
			}
			Code.addInstr(Instr.set_label,break_label);

			if( init != null && init instanceof ForInit ) {
				ForInit fi = (ForInit)init;
				for(int i=fi.vars.length-1; i >= 0; i--) {
					Code.removeVar(fi.vars[i]);
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("for").space().append('(');
		if( init != null && init instanceof Statement ) dmp.append(init);
		else if( init != null ) {
			dmp.append(init).append(';');
		} else {
			dmp.append(';');
		}

		if( cond != null )
			dmp.append(cond);
		dmp.append(';');

		if( iter != null )
			dmp.append(iter);
		dmp.space().append(')').space();

		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(body);
		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.newLine();
		else dmp.newLine(-1);

		return dmp;
	}
}

public class ForEachStat extends LoopStat implements ScopeOfNames {

	public Var			var;
	public Var			iter;
	public Expr			iter_init;
	public BooleanExpr	iter_cond;
	public Expr			iter_incr;
	public Expr			var_init;
	public Expr			container;
	public BooleanExpr	cond;
	public Statement	body;

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;
	public static final int	RULE  = 4;

	public int			mode;

	public ForEachStat(int pos, ASTNode parent, Var var, Expr container, BooleanExpr cond, Statement body) {
		super(pos, parent);
		this.var = var;
		if( this.var != null )
			this.var.parent = this;
		this.container = container;
		this.container.parent = this;
		if( cond != null ) {
			this.cond = cond;
			this.cond.parent = this;
		}
		this.body = body;
		this.body.parent = this;
	}

	public void cleanup() {
		parent=null;
		var = null;
		iter = null;
		if( iter_init != null ) {
			iter_init.cleanup();
			iter_init = null;
			}
		if( iter_cond != null ) {
			iter_cond.cleanup();
			iter_cond = null;
			}
		if( iter_incr != null ) {
			iter_incr.cleanup();
			iter_incr = null;
			}
		if( var_init != null ) {
			var_init.cleanup();
			var_init = null;
			}
		if( container != null ) {
			container.cleanup();
			container = null;
			}
		if( cond != null ) {
			cond.cleanup();
			cond = null;
			}
		body.cleanup();
		body = null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		PassInfo.push(this);
		ScopeNodeInfoVector state = NodeInfoPass.pushState();
		state.guarded = true;
		try {
			// foreach( type x; container; cond) statement
			// is equivalent to
			// for(iter-type x$iter = container.elements(); x$iter.hasMoreElements(); ) {
			//		type x = container.nextElement();
			//		if( !cond ) continue;
			//		...
			//	}
			//	or if container is an array:
			//	for(int x$iter=0; x$iter < container.length; x$iter++) {
			//		type x = container[ x$iter ];
			//
			//		if( !cond ) continue;
			//		...
			//	}
			//	or if container is a rule:
			//	for(rule $env=null; ($env=rule($env,...)) != null; ) {
			//		if( !cond ) continue;
			//		...
			//	}
			//

			container = (Expr)container.resolve(null);

			Type itype;
			Type ctype = container.getType();
			PVar<Method> elems = new PVar<Method>();
			PVar<Method> nextelem = new PVar<Method>();
			PVar<Method> moreelem = new PVar<Method>();
			if (ctype.clazz.isWrapper()) {
				container = (Expr)new AccessExpr(container.pos,container,ctype.clazz.wrapped_field).resolve(null);
				ctype = container.getType();
			}
			if( ctype.isArray() ) {
				itype = Type.tpInt;
				mode = ARRAY;
			} else if( ctype.clazz.instanceOf( Type.tpKievEnumeration.clazz) ) {
				itype = ctype;
				mode = KENUM;
			} else if( ctype.isInstanceOf( Type.tpJavaEnumeration) ) {
				itype = ctype;
				mode = JENUM;
			} else if( PassInfo.resolveBestMethodR(ctype.clazz,elems,new PVar<List<ASTNode>>(List.Nil),nameElements,Expr.emptyArray,null,ctype,0) ) {
				itype = Type.getRealType(ctype,elems.type.ret);
				mode = ELEMS;
			} else if( ctype == Type.tpRule &&
				(
				   ( container instanceof CallExpr && ((CallExpr)container).func.type.ret == Type.tpRule )
				|| ( container instanceof CallAccessExpr && ((CallAccessExpr)container).func.type.ret == Type.tpRule )
				|| ( container instanceof ClosureCallExpr && ((ClosureCallExpr)container).getType() == Type.tpRule )
				)
			  ) {
				itype = Type.tpRule;
				mode = RULE;
			} else {
				throw new CompilerException(container.pos,"Container must be an array or an Enumeration "+
					"or a class that implements 'Enumeration elements()' method, but "+ctype+" found");
			}
			if( itype == Type.tpRule ) {
				iter = new Var(pos,this,KString.from("$env"),itype,0);
			}
			else if( var != null ) {
				iter = new Var(var.pos,this,KString.from(var.name.name+"$iter"),itype,0);
			}
			else {
				iter = null;
			}

			// Initialize iterator
			switch( mode ) {
			case ARRAY:
				/* iter = 0; */
				iter_init = new AssignExpr(iter.pos,AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter),
						new ConstExpr(iter.pos,Kiev.newInteger(0))
					);
				iter_init.parent = this;
				iter_init = (Expr)iter_init.resolve(Type.tpInt);
				break;
			case KENUM:
				/* iter = container; */
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter),	container
					);
				iter_init.parent = this;
				iter_init = (Expr)iter_init.resolve(iter.type);
				break;
			case JENUM:
				/* iter = container; */
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter),	container
					);
				iter_init.parent = this;
				iter_init = (Expr)iter_init.resolve(iter.type);
				break;
			case ELEMS:
				/* iter = container.elements(); */
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter),
					new CallAccessExpr(container.pos,container,elems,Expr.emptyArray)
					);
				iter_init.parent = this;
				iter_init = (Expr)iter_init.resolve(iter.type);
				break;
			case RULE:
				/* iter = rule(iter,...); */
				{
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter), new ConstExpr(iter.pos,null)
					);
				iter_init.parent = this;
				iter_init = (Expr)iter_init.resolve(Type.tpVoid);
				// Also, patch the rule argument
				Expr[] args = null;
				if( container instanceof CallExpr ) {
					args = ((CallExpr)container).args;
				}
				else if( container instanceof CallAccessExpr ) {
					args = ((CallAccessExpr)container).args;
				}
				else if( container instanceof ClosureCallExpr ) {
					args = ((ClosureCallExpr)container).args;
				}
				else
					Debug.assert("Unknown type of rule - "+container.getClass());
				args[0] = (Expr)new VarAccessExpr(container.pos,iter).resolve(Type.tpRule);
				}
				break;
			}

			// Check iterator condition

			switch( mode ) {
			case ARRAY:
				/* iter < container.length */
				iter_cond = new BinaryBooleanExpr(iter.pos,BinaryOperator.LessThen,
					new VarAccessExpr(iter.pos,iter),
					new ArrayLengthAccessExpr(iter.pos,container)
					);
				break;
			case KENUM:
			case JENUM:
			case ELEMS:
				/* iter.hasMoreElements() */
				if( !PassInfo.resolveBestMethodR(itype.clazz,moreelem,
					new PVar<List<ASTNode>>(List.Nil),nameHasMoreElements,Expr.emptyArray,null,ctype,0) )
					throw new CompilerException(pos,"Can't find method "+nameHasMoreElements);
				iter_cond = new BooleanWrapperExpr(iter.pos,
					new CallAccessExpr(iter.pos, new VarAccessExpr(iter.pos,this,iter),
						moreelem,
						Expr.emptyArray
					));
				break;
			case RULE:
				/* (iter = rule(iter, ...)) != null */
				iter_cond = new BinaryBooleanExpr(
					container.pos,
					BinaryOperator.NotEquals,
					new AssignExpr(container.pos,AssignOperator.Assign,
						new VarAccessExpr(container.pos,iter),
						container),
					new ConstExpr(container.pos,null)
					);
				break;
			}
			if( iter_cond != null ) {
				iter_cond.parent = this;
				iter_cond = (BooleanExpr)iter_cond.resolve(Type.tpBoolean);
			}

			// Initialize value
			switch( mode ) {
			case ARRAY:
				/* var = container[iter] */
				var_init = new AssignExpr(var.pos,AssignOperator.Assign,
					new VarAccessExpr(var.pos,var),
					new ContainerAccessExpr(container.pos,container,new VarAccessExpr(iter.pos,iter))
					);
				break;
			case KENUM:
			case JENUM:
			case ELEMS:
				/* var = iter.nextElement() */
				if( !PassInfo.resolveBestMethodR(itype.clazz,nextelem,
					new PVar<List<ASTNode>>(List.Nil),nameNextElement,Expr.emptyArray,null,ctype,0) )
					throw new CompilerException(pos,"Can't find method "+nameHasMoreElements);
					var_init = new CallAccessExpr(iter.pos,
						new VarAccessExpr(iter.pos,iter),
						nextelem,
						Expr.emptyArray
					);
				if (!nextelem.type.ret.isInstanceOf(var.type))
					var_init = new CastExpr(pos,var.type,var_init);
				var_init = new AssignExpr(var.pos,AssignOperator.Assign,
					new VarAccessExpr(var.pos,var),
					var_init
				);
				break;
			case RULE:
				/* iter = rule(...); */
				var_init = null;
				break;
			}
			if( var_init != null ) {
				var_init.parent = this;
				var_init = (Expr)var_init.resolve(var.getType());
			}

			// Check condition, if any
			if( cond != null ) {
				cond = (BooleanExpr)cond.resolve(Type.tpBoolean);
			}

			if( cond != null && cond instanceof InstanceofExpr ) ((InstanceofExpr)cond).setNodeTypeInfo();
			else if( cond != null &&  cond instanceof BinaryBooleanAndExpr ) {
				BinaryBooleanAndExpr bbae = (BinaryBooleanAndExpr)cond;
				if( bbae.expr1 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr1).setNodeTypeInfo();
				if( bbae.expr2 instanceof InstanceofExpr ) ((InstanceofExpr)bbae.expr2).setNodeTypeInfo();
			}

			// Process body
			try {   body = (Statement)body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(body.pos,e);
			}

			// Increment iterator
			if( mode == ARRAY ) {
				/* iter++ */
				iter_incr = new IncrementExpr(iter.pos,PostfixOperator.PostIncr,
					new VarAccessExpr(iter.pos,iter)
					);
				iter_incr.parent = this;
				iter_incr = (Expr)iter_incr.resolve(Type.tpVoid);
			} else {
				iter_incr = null;
			}
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
		}

		return this;
	}

	rule public resolveNameR(ASTNode@ node, List<ASTNode>@ path, KString name, Type tp, int resfl)
	{
		{	node ?= var
		;	node ?= iter
		}, ((Var)node).name.equals(name)
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ForEachStat");
		continue_label = Code.newLabel();
		break_label = Code.newLabel();
		CodeLabel body_label = Code.newLabel();
		CodeLabel check_label = Code.newLabel();

		PassInfo.push(this);
		try {
			if( iter != null )
				Code.addVar(iter);
			if( var != null )
				Code.addVar(var);

			// Init iterator
			iter_init.generate(Type.tpVoid);

			// Goto check
			Code.addInstr(Instr.op_goto,check_label);

			// Start body - set var, check cond, do body
			Code.addInstr(Instr.set_label,body_label);

			if( var_init != null)
				var_init.generate(Type.tpVoid);
			if( cond != null )
				cond.generate_iffalse(continue_label);

			body.generate(Type.tpVoid);

			// Continue - iterate iterator and check iterator condition
			Code.addInstr(Instr.set_label,continue_label);
			if( iter_incr != null )
				iter_incr.generate(Type.tpVoid);

			// Just check iterator condition
			Code.addInstr(Instr.set_label,check_label);
			if( iter_cond != null )
				iter_cond.generate_iftrue(body_label);

			if( var != null )
				Code.removeVar(var);
			if( iter != null )
				Code.removeVar(iter);

			Code.addInstr(Instr.set_label,break_label);
		} catch(Exception e ) {
			Kiev.reportError(pos,e);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("for").space().append('(');
		if( iter_init != null )
			dmp.append(iter_init).append(';');
		if( iter_cond != null )
			dmp.append(iter_cond).append(';');
		if( iter_incr != null )
			dmp.append(iter_incr);
		dmp.append(')').space();

		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.forsed_space();
		else dmp.newLine(1);
		if( var_init != null )
			dmp.append(var_init).newLine();
		if( cond != null )
			dmp.append("if(").append(cond).append(") break;").newLine();

		dmp.append(body);
		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.newLine();
		else dmp.newLine(-1);

		return dmp;
	}
}

