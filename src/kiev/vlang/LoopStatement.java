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
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public abstract class LoopStat extends ENode implements BreakTarget, ContinueTarget {

	@att(copyable=false)	public Label		lblcnt = new Label();
	@att(copyable=false)	public Label		lblbrk = new Label();

	protected LoopStat() {
	}

	protected LoopStat(int pos) {
		super(pos);
		setBreakTarget(true);
	}

	public final Label getCntLabel() { return lblcnt; }
	public final Label getBrkLabel() { return lblbrk; }
}


@node
public class Label extends DNode {
	
	@dflow(out="this:out()") private static class DFI {}

	@node
	static class LabelImpl extends DNodeImpl {
		LabelImpl() {}
		@ref(copyable=false)	List<ASTNode>	links = List.Nil;
								CodeLabel		label;

		public void callbackRootChanged() {
			ASTNode root = this.pctx.root;
			links = links.filter(fun (ASTNode n)->boolean { return n.pctx.root == root; });
			super.callbackRootChanged();
		}	
	}
	@nodeview
	static class LabelView extends DNodeView {
		final LabelImpl impl;
		LabelView(LabelImpl impl) {
			super(impl);
			this.impl = impl;
		}
		@getter public final List<ASTNode>			get$links()		{ return this.impl.links; }
		@getter public final CodeLabel				get$label()		{ return this.impl.label; }
		@setter public final void set$links(List<ASTNode> val)		{ this.impl.links =val; }
		@setter public final void set$label(CodeLabel val)			{ this.impl.label =val; }
	}
	public NodeView			getNodeView()		{ return new LabelView((LabelImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new LabelView((LabelImpl)this.$v_impl); }
	public LabelView		getLabelView()		{ return new LabelView((LabelImpl)this.$v_impl); }

	@ref(copyable=false) public abstract virtual List<ASTNode>		links;
	                     public abstract virtual CodeLabel			label;

	@getter public List<ASTNode>			get$links()		{ return this.getLabelView().links; }
	@getter public CodeLabel				get$label()		{ return this.getLabelView().label; }
	@setter public void set$links(List<ASTNode> val)		{ this.getLabelView().links = val; }
	@setter public void set$label(CodeLabel val)			{ this.getLabelView().label = val; }
	
	public Label() {
		super(new LabelImpl());
	}
	
	public void addLink(ASTNode lnk) {
		if (links.contains(lnk))
			return;
		links = new List.Cons<ASTNode>(lnk, links);
	}

	public void delLink(ASTNode lnk) {
		links = links.diff(lnk);
	}

	static class LabelDFFunc extends DFFunc {
		final int res_idx;
		LabelDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			if ((dfi.locks & 1) != 0)
				throw new DFLoopException(this);
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			Label node = (Label)dfi.node;
			DFState tmp = node.getDFlow().in();
			dfi.locks |= 1;
			try {
				foreach (ASTNode lnk; node.links) {
					try {
						DFState s = lnk.getDFlow().jmp();
						tmp = DFState.join(s,tmp);
					} catch (DFLoopException e) {
						if (e.label != this) throw e;
					}
				}
			} finally { dfi.locks &= ~1; }
			res = tmp;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new LabelDFFunc(dfi);
	}

	public CodeLabel getCodeLabel(Code code) {
		if( label == null  || label.code != code) label = code.newLabel();
		return label;
	}
	public void generate(Code code, Type reqType) {
		code.addInstr(Instr.set_label,getCodeLabel(code));
	}
}

@node
public class WhileStat extends LoopStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="body")		Label		lblcnt;
	@dflow(in="lblcnt")						ENode		cond;
	@dflow(in="cond:true")					ENode		body;
	@dflow(in="cond:false")					Label		lblbrk;
	}

	@att public ENode		cond;
	
	@att public ENode		body;
	
	public WhileStat() {
	}

	public WhileStat(int pos, ENode cond, ENode body) {
		super(pos);
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) { Kiev.reportError(cond,e); }
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) { Kiev.reportError(body,e); }
		if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
			setMethodAbrupted(true);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating WhileStat");
		code.setLinePos(this.getPosLine());
		try {
			lblcnt.label = code.newLabel();
			lblbrk.label = code.newLabel();
			CodeLabel body_label = code.newLabel();

			code.addInstr(Instr.op_goto,lblcnt.label);
			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);
			lblcnt.generate(code,Type.tpVoid);

			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				BoolExpr.gen_iftrue(code, cond, body_label);
			}
			lblbrk.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
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
public class DoWhileStat extends LoopStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="cond:true")	ENode		body;
	@dflow(in="body")							Label		lblcnt;
	@dflow(in="lblcnt")							ENode		cond;
	@dflow(in="cond:false")						Label		lblbrk;
	}

	@att public ENode		body;

	@att public ENode		cond;
	
	public DoWhileStat() {
	}

	public DoWhileStat(int pos, ENode cond, ENode body) {
		super(pos);
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
			setMethodAbrupted(true);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating DoWhileStat");
		code.setLinePos(this.getPosLine());
		try {
			lblcnt.label = code.newLabel();
			lblbrk.label = code.newLabel();
			CodeLabel body_label = code.newLabel();

// Differ from WhileStat in this:	code.addInstr(Instr.op_goto,continue_label);
			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);
			lblcnt.generate(code,Type.tpVoid);

			if( cond.isConstantExpr() ) {
				if( ((Boolean)cond.getConstValue()).booleanValue() ) {
					code.addInstr(Instr.op_goto,body_label);
				}
			} else {
				BoolExpr.gen_iftrue(code, cond, body_label);
			}
			lblbrk.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
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
	
	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	Var[]		decls;
	}

	@att public final NArr<Var>		decls;

	public ForInit() {
	}

	public ForInit(int pos) {
		super(pos);
	}

	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
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

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
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
public class ForStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")				ENode		init;
	@dflow(in="init", links="iter")		ENode		cond;
	@dflow(in="cond:true")				ENode		body;
	@dflow(in="body")					Label		lblcnt;
	@dflow(in="lblcnt")					ENode		iter;
	@dflow(in="cond:false")				Label		lblbrk;
	}

	@att public ENode		init;

	@att public ENode		cond;
	
	@att public ENode		body;

	@att public ENode		iter;
	
	public ForStat() {
	}
	
	public ForStat(int pos, ENode init, ENode cond, ENode iter, ENode body) {
		super(pos);
		this.init = init;
		this.cond = cond;
		this.iter = iter;
		this.body = body;
	}

	public void resolve(Type reqType) {
		if( init != null ) {
			try {
				init.resolve(Type.tpVoid);
				init.setGenVoidExpr(true);
			} catch(Exception e ) {
				Kiev.reportError(init,e);
			}
		}
		if( cond != null ) {
			try {
				cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(cond);
			} catch(Exception e ) {
				Kiev.reportError(cond,e);
			}
		}
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		if( iter != null ) {
			try {
				iter.resolve(Type.tpVoid);
				iter.setGenVoidExpr(true);
			} catch(Exception e ) {
				Kiev.reportError(iter,e);
			}
		}
		if( ( cond==null
			|| (cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue())
			)
			&& !isBreaked()
		) {
			setMethodAbrupted(true);
		}
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
	{
		init instanceof ForInit,
		((ForInit)init).resolveNameR(node,path,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		init instanceof ForInit,
		((ForInit)init).resolveMethodR(node,info,name,mt)
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ForStat");
		lblcnt.label = code.newLabel();
		lblbrk.label = code.newLabel();
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this.getPosLine());
		try {
			if( init != null ) {
				if( init instanceof ForInit ) {
					ForInit fi = (ForInit)init;
					foreach (Var var; fi.decls) {
						var.generate(code,Type.tpVoid);
					}
				} else {
					init.generate(code,Type.tpVoid);
				}
			}

			if( cond != null ) {
				code.addInstr(Instr.op_goto,check_label);
			}

			code.addInstr(Instr.set_label,body_label);
			if( isAutoReturnable() )
				body.setAutoReturnable(true);
			body.generate(code,Type.tpVoid);

			lblcnt.generate(code,Type.tpVoid);
			if( iter != null )
				iter.generate(code,Type.tpVoid);

			code.addInstr(Instr.set_label,check_label);
			if( cond != null ) {
				if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() )
					code.addInstr(Instr.op_goto,body_label);
				else if( cond.isConstantExpr() && !((Boolean)cond.getConstValue()).booleanValue() );
				else BoolExpr.gen_iftrue(code, cond, body_label);
			} else {
				code.addInstr(Instr.op_goto,body_label);
			}
			lblbrk.generate(code,Type.tpVoid);

			if( init != null && init instanceof ForInit ) {
				ForInit fi = (ForInit)init;
				for(int i=fi.decls.length-1; i >= 0; i--) {
					code.removeVar(fi.decls[i]);
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("for").space().append('(');
		if( init != null && init instanceof ENode ) dmp.append(init);
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
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")						ENode		container;
	@dflow(in="this:in")						Var			var;
	@dflow(in="var")							Var			iter;
	@dflow(in="iter")							Var			iter_array;
	@dflow(in="iter_array")						ENode		iter_init;
	@dflow(in="iter_init", links="iter_incr")	ENode		iter_cond;
	@dflow(in="iter_cond:true")					ENode		var_init;
	@dflow(in="var_init")						ENode		cond;
	@dflow(in="cond:true")						ENode		body;
	@dflow(in="body", links="cond:false")		Label		lblcnt;
	@dflow(in="lblcnt")							ENode		iter_incr;
	@dflow(in="iter_cond:false")				Label		lblbrk;
	}

	@att public ENode		container;
	@att public Var			var;
	@att public Var			iter;
	@att public Var			iter_array;
	
	@att public ENode		iter_init;
	
	@att public ENode		iter_cond;
	
	@att public ENode		var_init;
	@att public ENode		cond;
	@att public ENode		body;

	@att public ENode		iter_incr;

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;
	public static final int	RULE  = 4;

	public int			mode;

	public ForEachStat() {
	}
	
	public ForEachStat(int pos, Var var, ENode container, ENode cond, ENode body) {
		super(pos);
		this.var = var;
		this.container = container;
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
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
		} else if( PassInfo.resolveBestMethodR(ctype,elems,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
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
			throw new CompilerException(container,"Container must be an array or an Enumeration "+
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
					new LVarExpr(container.pos,iter_array),
					(ENode)container.copy()
				));
			((CommaExpr)iter_init).exprs.add(
				new AssignExpr(iter.pos,AssignOperator.Assign,
					new LVarExpr(iter.pos,iter),
					new ConstIntExpr(0)
				));
			iter_init.resolve(Type.tpInt);
			break;
		case KENUM:
			/* iter = container; */
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter), (ENode)container.copy()
				);
			iter_init.resolve(iter.type);
			break;
		case JENUM:
			/* iter = container; */
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter), (ENode)container.copy()
				);
			iter_init.resolve(iter.type);
			break;
		case ELEMS:
			/* iter = container.elements(); */
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter),
				new CallExpr(container.pos,(ENode)container.copy(),elems,ENode.emptyArray)
				);
			iter_init.resolve(iter.type);
			break;
		case RULE:
			/* iter = rule(iter/hidden,...); */
			{
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter), new ConstNullExpr()
				);
			iter_init.resolve(Type.tpVoid);
//			// now is hidden // Also, patch the rule argument
//			NArr<ENode> args = null;
//			if( container instanceof CallExpr ) {
//				args = ((CallExpr)container).args;
//			}
//			else if( container instanceof ClosureCallExpr ) {
//				args = ((ClosureCallExpr)container).args;
//			}
//			else
//				Debug.assert("Unknown type of rule - "+container.getClass());
//			args[0] = new LVarExpr(container.pos,iter);
//			args[0].resolve(Type.tpRule);
			}
			break;
		}
		iter_init.setGenVoidExpr(true);

		// Check iterator condition

		switch( mode ) {
		case ARRAY:
			/* iter < container.length */
			iter_cond = new BinaryBoolExpr(iter.pos,BinaryOperator.LessThen,
				new LVarExpr(iter.pos,iter),
				new ArrayLengthExpr(iter.pos,new LVarExpr(0,iter_array))
				);
			break;
		case KENUM:
		case JENUM:
		case ELEMS:
			/* iter.hasMoreElements() */
			if( !PassInfo.resolveBestMethodR(itype,moreelem,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
				nameHasMoreElements,MethodType.newMethodType(null,Type.emptyArray,Type.tpAny)) )
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
			iter_cond = new CallExpr(	iter.pos,
					new LVarExpr(iter.pos,iter),
					moreelem,
					ENode.emptyArray
				);
			break;
		case RULE:
			/* (iter = rule(iter, ...)) != null */
			iter_cond = new BinaryBoolExpr(
				container.pos,
				BinaryOperator.NotEquals,
				new AssignExpr(container.pos,AssignOperator.Assign,
					new LVarExpr(container.pos,iter),
					(ENode)container.copy()),
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
				new LVarExpr(var.pos,var),
				new ContainerAccessExpr(container.pos,new LVarExpr(0,iter_array),new LVarExpr(iter.pos,iter))
				);
			break;
		case KENUM:
		case JENUM:
		case ELEMS:
			/* var = iter.nextElement() */
			if( !PassInfo.resolveBestMethodR(itype,nextelem,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
				nameNextElement,MethodType.newMethodType(null,Type.emptyArray,Type.tpAny)) )
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
				var_init = new CallExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					nextelem,
					ENode.emptyArray
				);
			if (!nextelem.type.ret.isInstanceOf(var.type))
				var_init = new CastExpr(pos,var.type,(ENode)~var_init);
			var_init = new AssignExpr(var.pos,AssignOperator.Assign2,
				new LVarExpr(var.pos,var),
				(ENode)~var_init
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
			Kiev.reportError(body,e);
		}

		// Increment iterator
		if( mode == ARRAY ) {
			/* iter++ */
			iter_incr = new IncrementExpr(iter.pos,PostfixOperator.PostIncr,
				new LVarExpr(iter.pos,iter)
				);
			iter_incr.resolve(Type.tpVoid);
			iter_incr.setGenVoidExpr(true);
		} else {
			iter_incr = null;
		}
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
	{
		{	node ?= var
		;	node ?= iter
		}, ((Var)node).name.equals(name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
		Var@ n;
	{
		{	n ?= var
		;	n ?= iter
		},
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating ForEachStat");
		lblcnt.label = code.newLabel();
		lblbrk.label = code.newLabel();
		CodeLabel body_label = code.newLabel();
		CodeLabel check_label = code.newLabel();

		code.setLinePos(this.getPosLine());
		try {
			if( iter != null )
				code.addVar(iter);
			if( var != null )
				code.addVar(var);
			if( iter_array != null )
				code.addVar(iter_array);

			// Init iterator
			iter_init.generate(code,Type.tpVoid);

			// Goto check
			code.addInstr(Instr.op_goto,check_label);

			// Start body - set var, check cond, do body
			code.addInstr(Instr.set_label,body_label);

			if( var_init != null)
				var_init.generate(code,Type.tpVoid);
			if( cond != null )
				BoolExpr.gen_iffalse(code, cond, lblcnt.label);

			body.generate(code,Type.tpVoid);

			// Continue - iterate iterator and check iterator condition
			lblcnt.generate(code,Type.tpVoid);
			if( iter_incr != null )
				iter_incr.generate(code,Type.tpVoid);

			// Just check iterator condition
			code.addInstr(Instr.set_label,check_label);
			if( iter_cond != null )
				BoolExpr.gen_iftrue(code, iter_cond, body_label);

			if( iter_array != null )
				code.removeVar(iter_array);
			if( var != null )
				code.removeVar(var);
			if( iter != null )
				code.removeVar(iter);

			lblbrk.generate(code,Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
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

