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
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public abstract class LoopStat extends Statement implements BreakTarget, ContinueTarget {

	protected	CodeLabel	continue_label = null;
	protected	CodeLabel	break_label = null;
	
	protected LoopStat() {
	}

	protected LoopStat(int pos, ASTNode parent) {
		super(pos, parent);
		setBreakTarget(true);
	}

	public CodeLabel getContinueLabel() {
		if( continue_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'continue' label");
		return continue_label;
	}

	public CodeLabel getBreakLabel() {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}

	public abstract Label getDFlowCntLabel();
	public abstract Label getDFlowBrkLabel();
}


@node
@dflow(out="lblbrk")
public class WhileStat extends LoopStat {

	@att(copyable=false)
	@dflow(in="")
	public Label		lblcnt;

	@att
	@dflow(in="lblcnt")
	public ENode		cond;
	
	@att
	@dflow(in="cond:true")
	public Statement	body;
	
	@att(copyable=false)
	@dflow(in="cond:false")
	public Label		lblbrk;

	public WhileStat() {
		this.lblcnt = new Label();
		this.lblbrk = new Label();
	}

	public WhileStat(int pos, ASTNode parent, ENode cond, Statement body) {
		super(pos, parent);
		this.lblcnt = new Label();
		this.lblbrk = new Label();
		this.cond = cond;
		this.body = body;
	}

	public Label getDFlowCntLabel() { return lblcnt; }
	public Label getDFlowBrkLabel() { return lblbrk; }
	
	public boolean preResolve() {
		lblcnt.addLink(body);
		return true;
	}
	
	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			try {
				cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(cond);
			} catch(Exception e ) { Kiev.reportError(cond.pos,e); }
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) { Kiev.reportError(body.pos,e); }
			if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
				setMethodAbrupted(true);
			}
		} finally { PassInfo.pop(this); }
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
				BoolExpr.gen_iftrue(cond, body_label);
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

@node
@dflow(out="lblbrk")
public class DoWhileStat extends LoopStat {

	@att(copyable=false)
	@dflow(in="")
	public Label		lbltmp1;

	@att
	@dflow(in="lbltmp1")
	public Statement	body;

	@att(copyable=false)
	@dflow(in="body")
	public Label		lblcnt;

	@att
	@dflow(in="lblcnt")
	public ENode		cond;
	
	@att(copyable=false)
	@dflow(in="cond:true")
	public Label		lbltmp2;

	@att(copyable=false)
	@dflow(in="cond:false")
	public Label		lblbrk;

	public DoWhileStat() {
		this.lbltmp1 = new Label();
		this.lbltmp2 = new Label();
		this.lblcnt = new Label();
		this.lblbrk = new Label();
	}

	public DoWhileStat(int pos, ASTNode parent, ENode cond, Statement body) {
		super(pos,parent);
		this.lbltmp1 = new Label();
		this.lbltmp2 = new Label();
		this.lblcnt = new Label();
		this.lblbrk = new Label();
		this.cond = cond;
		this.body = body;
	}

	public Label getDFlowCntLabel() { return lblcnt; }
	public Label getDFlowBrkLabel() { return lblbrk; }
	
	public boolean preResolve() {
		lbltmp1.addLink(lbltmp2);
		return true;
	}
	
	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(body.pos,e);
			}
			try {
				cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(cond);
			} catch(Exception e ) {
				Kiev.reportError(cond.pos,e);
			}
			if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
				setMethodAbrupted(true);
			}
		} finally { PassInfo.pop(this); }
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
				BoolExpr.gen_iftrue(cond, body_label);
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

@node
public class ForInit extends ENode implements ScopeOfNames, ScopeOfMethods {

	@att public final NArr<Var>		decls;

	public ForInit() {
	}

	public ForInit(int pos) {
		super(pos);
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info, KString name)
		Var@ var;
	{
		var @= decls,
		var.name.equals(name),
		node ?= var
	;	var @= decls,
		var.isForward(),
		info.enterForward(var) : info.leaveForward(var),
		var.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		Var@ var;
	{
		var @= decls,
		var.isForward(),
		info.enterForward(var) : info.leaveForward(var),
		var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		foreach (Var v; decls)
			v.resolveDecl();
	}
	
	public Dumper toJava(Dumper dmp) {
		for(int i=0; i < decls.length; i++) {
			decls[i].toJava(dmp);
			if( i < decls.length-1 ) dmp.append(',').space();
		}
		return dmp;
	}
}

@node
@dflow(out="lblbrk")
public class ForStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {

	@att
	@dflow(in="")
	public ENode		init;

	@att(copyable=false)
	@dflow(in="init")
	public Label		lbltmp;

	@att
	@dflow(in="lbltmp")
	public ENode		cond;
	
	@att
	@dflow(in="cond:true")
	public Statement	body;

	@att(copyable=false)
	@dflow(in="body")
	public Label		lblcnt;

	@att
	@dflow(in="lblcnt")
	public ENode		iter;
	
	@att(copyable=false)
	@dflow(in="cond:false")
	public Label		lblbrk;

	public ForStat() {
		this.lbltmp = new Label();
		this.lblcnt = new Label();
		this.lblbrk = new Label();
	}
	
	public ForStat(int pos, ASTNode parent, ENode init, Expr cond, Expr iter, Statement body) {
		super(pos, parent);
		this.lbltmp = new Label();
		this.lblcnt = new Label();
		this.lblbrk = new Label();
		this.init = init;
		this.cond = cond;
		this.iter = iter;
		this.body = body;
	}

	public Label getDFlowCntLabel() { return lblcnt; }
	public Label getDFlowBrkLabel() { return lblbrk; }
	
	public boolean preResolve() {
		if (iter != null)
			lbltmp.addLink(iter);
		else
			lbltmp.addLink(lblcnt);
		return true;
	}
	
	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			if( init != null ) {
				try {
					if( init instanceof Statement )
						((Statement)init).resolve(Type.tpVoid);
					else if( init instanceof ForInit )
						((ForInit)init).resolve(Type.tpVoid);
					else if( init instanceof Expr )
						init.resolve(Type.tpVoid);
					else
						throw new RuntimeException("Unknown type of for-init node "+init);
					if (init instanceof Expr)
						init.setGenVoidExpr(true);
				} catch(Exception e ) {
					Kiev.reportError(init.pos,e);
				}
			}
			if( cond != null ) {
				try {
					cond.resolve(Type.tpBoolean);
					BoolExpr.checkBool(cond);
				} catch(Exception e ) {
					Kiev.reportError(cond.pos,e);
				}
			}
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(body.pos,e);
			}
			if( iter != null ) {
				try {
					iter.resolve(Type.tpVoid);
					iter.setGenVoidExpr(true);
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
		} finally { PassInfo.pop(this); }
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
	{
		init instanceof ForInit,
		((ForInit)init).resolveNameR(node,path,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		init instanceof ForInit,
		((ForInit)init).resolveMethodR(node,info,name,mt)
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
					foreach (Var var; fi.decls) {
						var.generate(Type.tpVoid);
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
				else BoolExpr.gen_iftrue(cond, body_label);
			} else {
				Code.addInstr(Instr.op_goto,body_label);
			}
			Code.addInstr(Instr.set_label,break_label);

			if( init != null && init instanceof ForInit ) {
				ForInit fi = (ForInit)init;
				for(int i=fi.decls.length-1; i >= 0; i--) {
					Code.removeVar(fi.decls[i]);
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

@node
public class ForEachStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {

	@att public Var			var;
	@att public Var			iter;
	@att public Var			iter_array;
	@att public ENode		iter_init;
	@att public ENode		iter_cond;
	@att public ENode		iter_incr;
	@att public ENode		var_init;
	@att public ENode		container;
	@att public ENode		cond;
	@att public Statement	body;

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;
	public static final int	RULE  = 4;

	public int			mode;

	public ForEachStat() {
	}
	
	public ForEachStat(int pos, ASTNode parent, Var var, ENode container, ENode cond, Statement body) {
		super(pos, parent);
		this.var = var;
		this.container = container;
		this.cond = cond;
		this.body = body;
	}

	public Label getDFlowCntLabel() { return null; }
	public Label getDFlowBrkLabel() { return null; }
	
	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			// foreach( type x; container; cond) statement
			// is equivalent to
			// for(iter-type x$iter = container.elements(); x$iter.hasMoreElements(); ) {
			//		type x = container.nextElement();
			//		if( !cond ) continue;
			//		...
			//	}
			//	or if container is an array:
			//	for(int x$iter=0, x$arr=container; x$iter < x$arr.length; x$iter++) {
			//		type x = x$arr[ x$iter ];
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

			container.resolve(null);

			Type itype;
			Type ctype = container.getType();
			Method@ elems;
			Method@ nextelem;
			Method@ moreelem;
			if (ctype.isWrapper()) {
				container = ctype.makeWrappedAccess(container);
				container.resolve(null);
				ctype = container.getType();
			}
			if( ctype.isArray() ) {
				itype = Type.tpInt;
				mode = ARRAY;
			} else if( ctype.isInstanceOf( Type.tpKievEnumeration) ) {
				itype = ctype;
				mode = KENUM;
			} else if( ctype.isInstanceOf( Type.tpJavaEnumeration) ) {
				itype = ctype;
				mode = JENUM;
			} else if( PassInfo.resolveBestMethodR(ctype,elems,new ResInfo(ResInfo.noStatic|ResInfo.noImports),
					nameElements,MethodType.newMethodType(null,Type.emptyArray,Type.tpAny))
			) {
				itype = Type.getRealType(ctype,elems.type.ret);
				mode = ELEMS;
			} else if( ctype == Type.tpRule &&
				(
				   ( container instanceof CallExpr && ((CallExpr)container).func.type.ret == Type.tpRule )
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
				iter = new Var(pos,KString.from("$env"),itype,0);
			}
			else if( var != null ) {
				iter = new Var(var.pos,KString.from(var.name.name+"$iter"),itype,0);
				if (mode == ARRAY) {
					iter_array = new Var(container.pos,KString.from(var.name.name+"$arr"),container.getType(),0);
				}
			}
			else {
				iter = null;
			}

			// Initialize iterator
			switch( mode ) {
			case ARRAY:
				/* iter = 0; arr = container;*/
				iter_init = new CommaExpr();
				((CommaExpr)iter_init).exprs.add(
					new AssignExpr(iter.pos,AssignOperator.Assign,
						new VarAccessExpr(container.pos,iter_array),
						new ShadowExpr(container)
					));
				((CommaExpr)iter_init).exprs.add(
					new AssignExpr(iter.pos,AssignOperator.Assign,
						new VarAccessExpr(iter.pos,iter),
						new ConstIntExpr(0)
					));
				iter_init.resolve(Type.tpInt);
				break;
			case KENUM:
				/* iter = container; */
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter), new ShadowExpr(container)
					);
				iter_init.resolve(iter.type);
				break;
			case JENUM:
				/* iter = container; */
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter), new ShadowExpr(container)
					);
				iter_init.resolve(iter.type);
				break;
			case ELEMS:
				/* iter = container.elements(); */
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter),
					new CallExpr(container.pos,(Expr)container.copy(),elems,Expr.emptyArray)
					);
				iter_init.resolve(iter.type);
				break;
			case RULE:
				/* iter = rule(iter,...); */
				{
				iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
					new VarAccessExpr(iter.pos,iter), new ConstNullExpr()
					);
				iter_init.resolve(Type.tpVoid);
				// Also, patch the rule argument
				NArr<ENode> args = null;
				if( container instanceof CallExpr ) {
					args = ((CallExpr)container).args;
				}
				else if( container instanceof ClosureCallExpr ) {
					args = ((ClosureCallExpr)container).args;
				}
				else
					Debug.assert("Unknown type of rule - "+container.getClass());
				args[0] = new VarAccessExpr(container.pos,iter);
				args[0].resolve(Type.tpRule);
				}
				break;
			}
			iter_init.setGenVoidExpr(true);

			// Check iterator condition

			switch( mode ) {
			case ARRAY:
				/* iter < container.length */
				iter_cond = new BinaryBoolExpr(iter.pos,BinaryOperator.LessThen,
					new VarAccessExpr(iter.pos,iter),
					new ArrayLengthAccessExpr(iter.pos,new VarAccessExpr(0,iter_array))
					);
				break;
			case KENUM:
			case JENUM:
			case ELEMS:
				/* iter.hasMoreElements() */
				if( !PassInfo.resolveBestMethodR(itype,moreelem,new ResInfo(ResInfo.noStatic|ResInfo.noImports),
					nameHasMoreElements,MethodType.newMethodType(null,Type.emptyArray,Type.tpAny)) )
					throw new CompilerException(pos,"Can't find method "+nameHasMoreElements);
				iter_cond = new CallExpr(	iter.pos,
						new VarAccessExpr(iter.pos,this,iter),
						moreelem,
						Expr.emptyArray
					);
				break;
			case RULE:
				/* (iter = rule(iter, ...)) != null */
				iter_cond = new BinaryBoolExpr(
					container.pos,
					BinaryOperator.NotEquals,
					new AssignExpr(container.pos,AssignOperator.Assign,
						new VarAccessExpr(container.pos,iter),
						(Expr)container.copy()),
					new ConstNullExpr()
					);
				break;
			}
			if( iter_cond != null ) {
				iter_cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(iter_cond);
			}

			// Initialize value
			switch( mode ) {
			case ARRAY:
				/* var = container[iter] */
				var_init = new AssignExpr(var.pos,AssignOperator.Assign2,
					new VarAccessExpr(var.pos,var),
					new ContainerAccessExpr(container.pos,new VarAccessExpr(0,iter_array),new VarAccessExpr(iter.pos,iter))
					);
				break;
			case KENUM:
			case JENUM:
			case ELEMS:
				/* var = iter.nextElement() */
				if( !PassInfo.resolveBestMethodR(itype,nextelem,new ResInfo(ResInfo.noStatic|ResInfo.noImports),
					nameNextElement,MethodType.newMethodType(null,Type.emptyArray,Type.tpAny)) )
					throw new CompilerException(pos,"Can't find method "+nameHasMoreElements);
					var_init = new CallExpr(iter.pos,
						new VarAccessExpr(iter.pos,iter),
						nextelem,
						Expr.emptyArray
					);
				if (!nextelem.type.ret.isInstanceOf(var.type))
					var_init = new CastExpr(pos,var.type,var_init);
				var_init = new AssignExpr(var.pos,AssignOperator.Assign2,
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
				var_init.resolve(var.getType());
				var_init.setGenVoidExpr(true);
			}

			// Check condition, if any
			if( cond != null ) {
				cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(cond);
			}

			// Process body
			try {
				body.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(body.pos,e);
			}

			// Increment iterator
			if( mode == ARRAY ) {
				/* iter++ */
				iter_incr = new IncrementExpr(iter.pos,PostfixOperator.PostIncr,
					new VarAccessExpr(iter.pos,iter)
					);
				iter_incr.resolve(Type.tpVoid);
				iter_incr.setGenVoidExpr(true);
			} else {
				iter_incr = null;
			}
		} finally { PassInfo.pop(this); }
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
	{
		{	node ?= var
		;	node ?= iter
		}, ((Var)node).name.equals(name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		Var@ n;
	{
		{	n ?= var
		;	n ?= iter
		},
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
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
			if( iter_array != null )
				Code.addVar(iter_array);

			// Init iterator
			iter_init.generate(Type.tpVoid);

			// Goto check
			Code.addInstr(Instr.op_goto,check_label);

			// Start body - set var, check cond, do body
			Code.addInstr(Instr.set_label,body_label);

			if( var_init != null)
				var_init.generate(Type.tpVoid);
			if( cond != null )
				BoolExpr.gen_iffalse(cond, continue_label);

			body.generate(Type.tpVoid);

			// Continue - iterate iterator and check iterator condition
			Code.addInstr(Instr.set_label,continue_label);
			if( iter_incr != null )
				iter_incr.generate(Type.tpVoid);

			// Just check iterator condition
			Code.addInstr(Instr.set_label,check_label);
			if( iter_cond != null )
				BoolExpr.gen_iftrue(iter_cond, body_label);

			if( iter_array != null )
				Code.removeVar(iter_array);
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
			dmp.append("if !(").append(cond).append(") continue;").newLine();

		dmp.append(body);
		if( body instanceof ExprStat || body instanceof BlockStat ) dmp.newLine();
		else dmp.newLine(-1);

		return dmp;
	}
}

