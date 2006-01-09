package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JLoopStatView;
import kiev.be.java.JLabelView;
import kiev.be.java.JWhileStatView;
import kiev.be.java.JDoWhileStatView;
import kiev.be.java.JForInitView;
import kiev.be.java.JForStatView;
import kiev.be.java.JForEachStatView;

import kiev.be.java.CodeLabel;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public abstract class LoopStat extends ENode implements BreakTarget, ContinueTarget {
	@virtual typedef NImpl = LoopStatImpl;
	@virtual typedef VView = LoopStatView;
	@virtual typedef JView = JLoopStatView;

	@node
	public static abstract class LoopStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = LoopStat;
		@att(copyable=false)	public Label		lblcnt;
		@att(copyable=false)	public Label		lblbrk;
		public LoopStatImpl() {	}
		public LoopStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static abstract view LoopStatView of LoopStatImpl extends ENodeView {
		public access:ro	Label					lblcnt;
		public access:ro	Label					lblbrk;
	}

	protected LoopStat(LoopStatImpl $view) {
		super($view);
		$view.lblcnt = new Label();
		$view.lblbrk = new Label();
		setBreakTarget(true);
	}
}


public final class Label extends DNode {
	
	@dflow(out="this:out()") private static class DFI {}

	@virtual typedef NImpl = LabelImpl;
	@virtual typedef VView = LabelView;
	@virtual typedef JView = JLabelView;

	@node
	public final static class LabelImpl extends DNodeImpl {
		@virtual typedef ImplOf = Label;
		LabelImpl() {}
		@ref(copyable=false)	public List<ASTNode>	links = List.Nil;
								public CodeLabel		label;

		public final void callbackRootChanged() {
			ASTNode root = this._self.ctx_root;
			links = links.filter(fun (ASTNode n)->boolean { return n.ctx_root == root; });
			super.callbackRootChanged();
		}	
	}
	@nodeview
	public final static view LabelView of LabelImpl extends DNodeView {
		public List<ASTNode>		links;
	}
	
	public VView getVView() { return new VView(this.$v_impl); }
	public JView getJView() { return new JView(this.$v_impl); }

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
			Label node = (Label)dfi.node_impl.getNode();
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
}

public class WhileStat extends LoopStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="body")		Label		lblcnt;
	@dflow(in="lblcnt")						ENode		cond;
	@dflow(in="cond:true")					ENode		body;
	@dflow(in="cond:false")					Label		lblbrk;
	}

	@virtual typedef NImpl = WhileStatImpl;
	@virtual typedef VView = WhileStatView;
	@virtual typedef JView = JWhileStatView;

	@node
	public static final class WhileStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = WhileStat;
		@att public ENode		cond;
		@att public ENode		body;
		public WhileStatImpl() {}
		public WhileStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view WhileStatView of WhileStatImpl extends LoopStatView {
		public ENode		cond;
		public ENode		body;
	}

	public VView getVView() { return new VView(this.$v_impl); }
	public JView getJView() { return new JView(this.$v_impl); }

	public WhileStat() {
		super(new WhileStatImpl());
	}

	public WhileStat(int pos, ENode cond, ENode body) {
		super(new WhileStatImpl(pos));
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
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="cond:true")	ENode		body;
	@dflow(in="body")							Label		lblcnt;
	@dflow(in="lblcnt")							ENode		cond;
	@dflow(in="cond:false")						Label		lblbrk;
	}

	@virtual typedef NImpl = DoWhileStatImpl;
	@virtual typedef VView = DoWhileStatView;
	@virtual typedef JView = JDoWhileStatView;

	@node
	public static final class DoWhileStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = DoWhileStat;
		@att public ENode		cond;
		@att public ENode		body;
		public DoWhileStatImpl() {}
		public DoWhileStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view DoWhileStatView of DoWhileStatImpl extends LoopStatView {
		public ENode		cond;
		public ENode		body;
	}

	public VView getVView() { return new VView(this.$v_impl); }
	public JView getJView() { return new JView(this.$v_impl); }

	public DoWhileStat() {
		super(new DoWhileStatImpl());
	}

	public DoWhileStat(int pos, ENode cond, ENode body) {
		super(new DoWhileStatImpl(pos));
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

public class ForInit extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	Var[]		decls;
	}

	@virtual typedef NImpl = ForInitImpl;
	@virtual typedef VView = ForInitView;
	@virtual typedef JView = JForInitView;

	@node
	public static final class ForInitImpl extends ENodeImpl {
		@virtual typedef ImplOf = ForInit;
		@att public final NArr<Var>		decls;
		public ForInitImpl() {}
		public ForInitImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ForInitView of ForInitImpl extends ENodeView {
		public access:ro	NArr<Var>		decls;
	}

	public VView getVView() { return new VView(this.$v_impl); }
	public JView getJView() { return new JView(this.$v_impl); }


	public ForInit() {
		super(new ForInitImpl());
	}

	public ForInit(int pos) {
		super(new ForInitImpl(pos));
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

public class ForStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")				ENode		init;
	@dflow(in="init", links="iter")		ENode		cond;
	@dflow(in="cond:true")				ENode		body;
	@dflow(in="body")					Label		lblcnt;
	@dflow(in="lblcnt")					ENode		iter;
	@dflow(in="cond:false")				Label		lblbrk;
	}
	
	@virtual typedef NImpl = ForStatImpl;
	@virtual typedef VView = ForStatView;
	@virtual typedef JView = JForStatView;

	@node
	public static final class ForStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = ForStat;
		@att public ENode		init;
		@att public ENode		cond;
		@att public ENode		body;
		@att public ENode		iter;
		public ForStatImpl() {}
		public ForStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ForStatView of ForStatImpl extends LoopStatView {
		public ENode		init;
		public ENode		cond;
		public ENode		body;
		public ENode		iter;
	}

	public VView getVView() { return new VView(this.$v_impl); }
	public JView getJView() { return new JView(this.$v_impl); }

	public ForStat() {
		super(new ForStatImpl());
	}
	
	public ForStat(int pos, ENode init, ENode cond, ENode iter, ENode body) {
		super(new ForStatImpl(pos));
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

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;
	public static final int	RULE  = 4;

	@virtual typedef NImpl = ForEachStatImpl;
	@virtual typedef VView = ForEachStatView;
	@virtual typedef JView = JForEachStatView;

	@node
	public static final class ForEachStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = ForEachStat;
		@att public int			mode;
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
		public ForEachStatImpl() {}
		public ForEachStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ForEachStatView of ForEachStatImpl extends LoopStatView {
		public int			mode;
		public ENode		container;
		public Var			var;
		public Var			iter;
		public Var			iter_array;
		public ENode		iter_init;
		public ENode		iter_cond;
		public ENode		var_init;
		public ENode		cond;
		public ENode		body;
		public ENode		iter_incr;
	}

	public VView getVView() { return new VView(this.$v_impl); }
	public JView getJView() { return new JView(this.$v_impl); }

	public ForEachStat() {
		super(new ForEachStatImpl());
	}
	
	public ForEachStat(int pos, Var var, ENode container, ENode cond, ENode body) {
		super(new ForEachStatImpl(pos));
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
				nameElements,new MethodType(Type.emptyArray,Type.tpAny))
		) {
			itype = Type.getRealType(ctype,elems.type.ret);
			mode = ELEMS;
		} else if( ctype ≡ Type.tpRule &&
			(
			   ( container instanceof CallExpr && ((CallExpr)container).func.type.ret ≡ Type.tpRule )
			|| ( container instanceof ClosureCallExpr && ((ClosureCallExpr)container).getType() ≡ Type.tpRule )
			)
		  ) {
			itype = Type.tpRule;
			mode = RULE;
		} else {
			throw new CompilerException(container,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+ctype+" found");
		}
		if( itype ≡ Type.tpRule ) {
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
				nameHasMoreElements,new MethodType(Type.emptyArray,Type.tpAny)) )
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
				nameNextElement,new MethodType(Type.emptyArray,Type.tpAny)) )
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

