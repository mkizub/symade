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
import kiev.Kiev.Ext;
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
@dflow(in="root()")
public class RuleMethod extends Method {

	@att public final NArr<Var>		localvars;
	public int						base = 1;
	public int						max_depth = 0;
	public int						state_depth = 0;
	public int						max_vars;
	public int						index;		// index counter for RuleNode.idx

	public RuleMethod() {
	}

	public RuleMethod(NameRef id, TypeCallRef t_ref, int fl) {
		super(id.name,t_ref,(TypeCallRef)t_ref.copy(),fl | ACC_RULEMETHOD);
		pos = id.pos;
	}
	public RuleMethod(KString name, MethodType type, int fl) {
		super(name,type,type,fl | ACC_RULEMETHOD);
	}

	public int allocNewBase(int n) {
		int b = base;
		base += n;
		return b;
	}

	public int push() {
		state_depth++;
		if( state_depth > max_depth )
			max_depth = state_depth;
		return state_depth-1;
	}

	public int set_depth(int i) {
		state_depth = i;
		if( state_depth > max_depth )
			max_depth = state_depth;
		return state_depth;
	}

	public int add_iterator_var() {
		return max_vars++;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
		Var@ var;
	{
		var @= localvars,
		var.name.equals(name),
		node ?= var
	;
		inlined_by_dispatcher,$cut,false
	;
		!this.isStatic(),
		name.equals(nameThis),
		node ?= getThisPar()
	;
		var @= params,
		var.name.equals(name),
		node ?= var
	;
		!this.isStatic() && path.isForwardsAllowed(),
		var ?= getThisPar(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	}

    public ASTNode pass3() {
		if !( parent instanceof Struct )
			throw new CompilerException(this,"Method must be declared on class level only");
		Struct clazz = (Struct)parent;
		// TODO: check flags for fields
		if( clazz.isPackage() ) setStatic(true);
		if( (flags & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic(true);
			if( pbody == null ) setAbstract(true);
		}
		params.insert(0, new FormPar(pos,namePEnv,Type.tpRule,ACC_FORWARD));
		// push the method, because formal parameters may refer method's type args
		foreach (FormPar fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.meta != null)
				fp.meta.verify();
		}
		if( isVarArgs() ) {
			FormPar va = new FormPar(pos,nameVarArgs,Type.newArrayType(Type.tpObject),0);
			params.append(va);
		}
		foreach (Var lv; localvars)
			lv.setLocalRuleVar(true);
		trace(Kiev.debugMultiMethod,"Rule "+this+" has java type "+this.jtype);
		foreach(ASTAlias al; aliases) al.attach(this);

		foreach(WBCCondition cond; conditions)
			cond.definer = this;

		return this;
    }

	static class RuleMethodDFFunc extends DFFunc {
		final int res_idx;
		RuleMethodDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			RuleMethod m = (RuleMethod)dfi.node;
			DFState in = DFState.makeNewState();
			if (!m.isStatic()) {
				Var p = m.getThisPar();
				in = in.declNode(p);
			}
			for(int i=0; i < m.params.length; i++) {
				Var p = m.params[i];
				in = in.declNode(p);
			}
			for(int i=0; i < m.localvars.length; i++) {
				in = in.declNode(m.localvars[i]);
			}
			res = in;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new RuleMethodDFFunc(dfi);
	}

	public boolean preGenerate() {
		Var penv = params[0];
		assert(penv.name.name == namePEnv && penv.getType() == Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
		if( body instanceof RuleBlock ) {
			body.preGenerate();
			Kiev.runProcessorsOn(body);
			body.cleanDFlow();
		}
		return true;
	}
	
	public void resolveDecl() {
		trace(Kiev.debugResolve,"Resolving rule "+this);
		try {
			Var penv = params[0];
			assert(penv.name.name == namePEnv && penv.getType() == Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
			if( body != null ) {
				if( type.ret == Type.tpVoid ) body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret == Type.tpVoid ) {
					((BlockStat)body).stats.append(new ReturnStat(pos,null));
					body.setAbrupted(true);
				} else {
					Kiev.reportError(body,"Return requared");
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		this.cleanDFlow();
	}


	public boolean compare(KString name, MethodType mt, Type tp, ResInfo info, boolean exact) {
		if( !this.name.equals(name) ) return false;
		int type_len = this.type.args.length - 1;
		int args_len = mt.args.length;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in number of params: "+type_len+" != "+args_len);
				return false;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" not match in number of params: "+type_len+" != "+args_len);
				return false;
			}
		}
		trace(Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,mt));
		MethodType rt = (MethodType)Type.getRealType(tp,this.type);
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if( exact && !mt.args[i].equals(rt.args[i+1]) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+rt.args[i+1]+" != "+mt.args[i]);
				return false;
			}
			else if( !exact && !mt.args[i].isAutoCastableTo(rt.args[i+1]) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+mt.args[i]+" not auto-castable to "+rt.args[i+1]);
				return false;
			}
		}
		boolean match = false;
		if( mt.ret == Type.tpAny )
			match = true;
		else if( exact &&  rt.ret.equals(mt.ret) )
			match = true;
		else if( !exact && rt.ret.isAutoCastableTo(mt.ret) )
			match = true;
		else
			match = false;
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+(match?" match":" do not match"));
		if (info != null && match)
			info.mt = rt;
		return match;
	}

}

/*
	if true:
		if exists next check node
			if no more variants ( isthe )
				set new state and go there
			if more variants possible ( isoneof or call )
				push new state and jump there
		if no next check node
			if no more variants ( isthe )
				set always fail state and return true
			if more variants possible ( isoneof or call )
				return true
	if false
		if exists next backtrack node
			if top of stack
				set new state and go there
			if not top
				pop state and go there
		if no next backtrack node
			pop state (pop out method's state) and return false
*/

public /*immutable*/ class JumpNodes implements Cloneable {
	public /*final*/ boolean		more_check;
	public /*final*/ ASTRuleNode	next_check;
	public /*final*/ boolean		more_back;
	public /*final*/ ASTRuleNode	next_back;
	public /*final*/ boolean		jump_to_back;

	public JumpNodes(boolean mu, ASTRuleNode nu, boolean mbt, ASTRuleNode nbt, boolean jtb) {
		more_check = mu;
		next_check = nu;
		more_back = mbt;
		next_back = nbt;
		jump_to_back = jtb;
	}

	public Object clone() {
		return super.clone();
	}
}

/*
New scheme for prolog engine:

a) each rule method is passed with first argument - method frame;
b) method frame class is created for each rule, and
holds rule arguments, rule local vars, rule state stack
and rule temporary vars (for example, to implement restore
values on backtracking of assign operation);
c) initially the method frame pointer is null. If it's
null - rule creates new instanse and fills it with
arguments and initial values
d) if rule is successive, it returns it's own frame
object, if fails - returns null.
*/

@node
public abstract class ASTRuleNode extends ENode {
	public static ASTRuleNode[]	emptyArray = new ASTRuleNode[0];

	public JumpNodes		jn;

	public abstract 		void	createText(StringBuffer sb);
	public abstract 		void	resolve1(JumpNodes jn);
	@virtual
	public virtual int		base;
	@virtual
	public virtual int		idx;
	public int				depth = -1;

	public ASTRuleNode() {
	}

	public ASTRuleNode(int pos) {
		super(pos);
	}

	@getter public int get$base() { return base; }
	@setter public void set$base(int b) { base = b; }

	@getter public int get$idx() { return idx; }
	@setter public void set$idx(int i) { idx = i; }

	public boolean preGenerate() {
		throw new CompilerException(this,"preGenerate of ASTRuleNode");
	}
	public void resolve(Type tp) {
		throw new CompilerException(this,"Resolving of ASTRuleNode");
	}

	public String createTextUnification(VarExpr var) {
		return "if( "+createTextVarAccess(var)+".$is_bound ) goto bound$"+idx+";\n";
	}

	public String createTextBacktrack(boolean load) {
		if (!jn.more_back)
			return "return null;\n";	// return false - no more solutions
		assert( ((RuleMethod)pctx.method).base != 1 || load==false);
		if (jn.next_back!=null && jn.jump_to_back) {
			if (load) return "bt$ = $env.bt$"+depth+"; goto enter$"+jn.next_back.idx+";\n";
			return "goto enter$"+jn.next_back.idx+";\n";
		}
		if (load)
			return "bt$ = $env.bt$"+depth+"; goto case bt$;\n"; // backtrack to saved address
		if (((RuleMethod)pctx.method).base == 1)
			return "return null;\n";
		return "goto case bt$;\n"; // backtrack to saved address
	}


	public String createTextMoreCheck(boolean force_goto) {
		if (!jn.more_check)
			return "$env.bt$=bt$; return $env;\n";				// return true - we've found a solution
		if (force_goto || jn.next_check.idx != (idx+1))
			return	"goto enter$"+jn.next_check.idx+";\n";		// jump to new check
		return "";
	}

	public String createTextVarAccess(VarExpr v) {
		if( !v.getVar().isLocalRuleVar() ) return v.name.toString();
		return "$env."+v;
	}

}


@node
@dflow(out="node")
public final class RuleBlock extends BlockStat {

	@att
	@dflow(in="this:in")
	public ASTRuleNode	node;
	
	public StringBuffer	fields_buf;

	public RuleBlock() {
	}

	public RuleBlock(int pos, ASTRuleNode n) {
		super(pos);
		node = n;
	}

	public void resolve(Type tp) {
		throw new CompilerException(this,"Resolving of RuleBlock");
	}
	public boolean preGenerate() {
		node.resolve(Type.tpVoid);
		fields_buf = new StringBuffer();
		node.resolve1(new JumpNodes(false,null,false,null,false));
		StringBuffer sb = new StringBuffer(256);
		sb.append("{ ");
		// Declare private method frame class
		String tmpClassName = "frame$$";
		sb.append("static class ").append(tmpClassName).append(" extends rule{\n");
		sb.append("int bt$;\n");
		RuleMethod rule_method = (RuleMethod)pctx.method;
		// Backtrace holders
		for (int i=0; i < rule_method.max_depth; i++)
			sb.append("int bt$").append(i).append(";\n");
		// Local variables
		foreach(Var v; rule_method.localvars) {
			String tp = Kiev.reparseType(v.type);
			if( v.type.isWrapper() )
				sb.append(tp+' '+v.name.name+" := new "+tp+"();\n");
			else
				sb.append(tp+' '+v.name.name+";\n");
		}
		// tmp variables inserted here
		sb.append(fields_buf.toString());
		fields_buf = null;
		sb.append("}\n");
		// Create new method frame or hash values from
		// existing one
		sb.append(tmpClassName).append(" $env;\n");
		sb.append("int bt$;\n");
		sb.append("if("+namePEnv+"==null) {\n");
		sb.append(" $env=new ").append(tmpClassName).append("(); bt$=0;\n");
		sb.append(" goto enter$1;\n");
		sb.append("}\n");
		if (rule_method.base != 1) {
			sb.append("else{\n");
			sb.append(" $env=($cast ").append(tmpClassName).append(")"+namePEnv+";\n");
			sb.append(" bt$=$env.bt$;\n");
			sb.append("}\n");
			sb.append("switch(bt$) {\ncase 0:\n");
		} else {
			// BUG!!!
			sb.append("else{\n$env=($cast ").append(tmpClassName).append(")"+namePEnv+";}\n");
		}
		sb.append("return null;\n");
		node.createText(sb);
		// Close method
		if (rule_method.base != 1)
			sb.append("}\nreturn null;\n");
		sb.append("}\n");
		trace(Kiev.debugRules,"Rule text generated:\n"+sb);
		BlockStat mbody = Kiev.parseBlock(this,sb);
		pctx.method.body = mbody;
		mbody.stats.addAll(stats);
		return false;
	}

}


@node
@dflow(out="rules")
public final class RuleOrExpr extends ASTRuleNode {

	@att
	@dflow(in="", seq="false")
	public final NArr<ASTRuleNode>	rules;

	public int get$base() {	return rules[0].get$base(); }
	public void set$base(int b) {}

	public int get$idx() {	return rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleOrExpr() {
	}

	public RuleOrExpr(ASTRuleNode first) {
		this.rules.add(first);
	}

	public RuleOrExpr(int pos, ASTRuleNode[] rules) {
		super(pos);
		this.rules.addAll(rules);
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public void resolve(Type reqType) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].resolve(reqType);
    	}
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		int depth = ((RuleMethod)pctx.method).state_depth;
		int max_depth = depth;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				j = new JumpNodes(jn.more_check, jn.next_check, true, rules[i+1], true);
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, jn.more_back, jn.next_back, jn.jump_to_back);
			}
			((RuleMethod)pctx.method).set_depth(depth);
			rules[i].resolve1(j);
			max_depth = Math.max(max_depth,((RuleMethod)pctx.method).state_depth);
		}
		((RuleMethod)pctx.method).set_depth(max_depth);
	}
}

@node
@dflow(out="rules")
public final class RuleAndExpr extends ASTRuleNode {

	@att
	@dflow(in="", seq="true")
	public final NArr<ASTRuleNode>	rules;

	public int get$base() {	return rules[0].get$base();	}
	public void set$base(int b) {}

	public int get$idx() {	return rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleAndExpr() {
	}

	public RuleAndExpr(ASTRuleNode first) {
		this.rules.add(first);
	}

	public RuleAndExpr(int pos, ASTRuleNode[] rules) {
		super(pos);
		this.rules.addAll(rules);
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public void resolve(Type reqType) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].resolve(reqType);
    	}
    	// combine simple boolean expressions
    	for(int i=0; i < (rules.length-1); i++) {
    		ASTRuleNode r1 = rules[i];
    		ASTRuleNode r2 = rules[i+1];
    		if (!(r1 instanceof RuleExpr)) continue;
    		if (!(r2 instanceof RuleExpr)) continue;
    		RuleExpr e1 = (RuleExpr)r1;
    		RuleExpr e2 = (RuleExpr)r2;
    		if (!e1.expr.getType().equals(Type.tpBoolean)) continue;
    		if (!e2.expr.getType().equals(Type.tpBoolean)) continue;
    		if (e1.bt_expr != null) continue;
    		if (e2.bt_expr != null) continue;
    		RuleExpr e = new RuleExpr(new BinaryBooleanAndExpr(e1.pos,(ENode)~e1.expr,(ENode)~e2.expr));
    		rules[i] = e;
			rules.del(i+1);
    		i--;
    	}
    	if (rules.length == 1)
    		replaceWithNode((ENode)~rules[0]);
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		boolean more_back = jn.more_back;
		ASTRuleNode next_back = jn.next_back;
		boolean jump_to_back = jn.jump_to_back;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				j = new JumpNodes(true, rules[i+1], more_back, next_back, jump_to_back);
				if (rules[i] instanceof RuleExpr) {
					RuleExpr re = (RuleExpr)rules[i];
					if (re.bt_expr != null) {
						more_back = true;
						next_back = rules[i];
						jump_to_back = false;
					}
				}
				else if (rules[i] instanceof RuleCutExpr) {
					more_back = false;
					next_back = null;
					jump_to_back = false;
				}
				else {
					more_back = true;
					next_back = rules[i];
					jump_to_back = false;
				}
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, more_back, next_back, jump_to_back);
			}
			rules[i].resolve1(j);
		}
	}
}

@node
@dflow(out="expr")
public final class RuleIstheExpr extends ASTRuleNode {

	@att
	public VarExpr	var;		// variable of type PVar<...>
	
	@att
	@dflow(in="this:in")
	public ENode	expr;		// expression to check/unify

	public RuleIstheExpr() {
	}

	public RuleIstheExpr(int pos, VarExpr var, ENode expr) {
		super(pos);
		this.var = var;
		this.expr = expr;
	}

    public void resolve(Type reqType) {
		var.resolve(null);
		expr.resolve(var.getVar().type.args[0]);
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)pctx.method).index;
		base = ((RuleMethod)pctx.method).allocNewBase(1);
		depth = ((RuleMethod)pctx.method).push();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				createTextUnification(var)+

			// Unbound
				createTextVarAccess(var)+".$bind("+Kiev.reparseExpr(expr,true)+");\n"+
				"if( !"+createTextVarAccess(var)+".$is_bound ) {\n"+
					createTextBacktrack(false)+					// backtrack, bt$ already loaded
				"}\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
				createTextMoreCheck(true)+						// check next
			"case "+base+":\n"+									// backtracking, always fail state
				createTextVarAccess(var)+".$unbind();\n"+		// was binded here, unbind
				createTextBacktrack(true)+						// backtrack, bt$ needs to be loaded

			// Already bound
			"bound$"+idx+":;\n"+
				"if( ! "+createTextVarAccess(var)+".equals("+Kiev.reparseExpr(expr,true)+") ) {\n"+	// check
					createTextBacktrack(false)+					// backtrack, bt$ already loaded
				"}\n"+
				createTextMoreCheck(false)							// check next
		);
	}
}

@node
@dflow(out="expr")
public final class RuleIsoneofExpr extends ASTRuleNode {

	@att
	public VarExpr			var;		// variable of type PVar<...>
	
	@att
	@dflow(in="this:in")
	public ENode			expr;		// expression to check/unify
	
	public int				iter_var;	// iterator var

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;

	public Type			itype;
	public int			mode;

	public RuleIsoneofExpr() {
	}

	public RuleIsoneofExpr(int pos, VarExpr var, ENode expr) {
		super(pos);
		this.var = var;
		this.expr = expr;
	}

    public void resolve(Type reqType) {
		var.resolve(null);
		expr.resolve(null);
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)pctx.method).index;
		base = ((RuleMethod)pctx.method).allocNewBase(2);
		depth = ((RuleMethod)pctx.method).push();
		expr.resolve(null);
		Type ctype = expr.getType();
		Method@ elems;
		if( ctype.isArray() ) {
			itype = Type.newRefType(Env.getStruct(KString.from("kiev.stdlib.ArrayEnumerator")),new Type[]{ctype.args[0]});
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
		} else {
			throw new CompilerException(expr,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+ctype+" found");
		}
		iter_var = ((RuleMethod)pctx.method).add_iterator_var();
		ASTNode rb = this.parent;
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.parent != null, "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent;
		}
		Debug.assert(rb != null);
		Debug.assert(rb instanceof RuleBlock);
		((RuleBlock)rb).fields_buf.append(itype)
			.append(' ').append("$iter$").append(iter_var).append(";\n");
	}

	private String createTextCheckUnbinded() {
		return "("+createTextVarAccess(var)+".$is_bound)";
	}

	private String createTextUnification() {
		return createTextUnification(var);
	}

	private String createTextNewIterator() {
		switch( mode ) {
		case ARRAY:
			return "new "+itype+"("+Kiev.reparseExpr(expr,true)+")";
		case KENUM:
			return Kiev.reparseExpr(expr,true);
		case JENUM:
			return Kiev.reparseExpr(expr,true);
		case ELEMS:
			return "("+Kiev.reparseExpr(expr,true)+").elements()";
		default:
			throw new RuntimeException("Unknown mode of iterator "+mode);
		}
	}

	private String createTextNewIterators() {
		return "$env.$iter$"+iter_var+"="+createTextNewIterator()+";\n";
	}

	private String createTextUnbindVars() {
		return "$env.$iter$"+iter_var+"=null;\n"+
				createTextVarAccess(var)+".$unbind();\n";
	}

	private String createTextCheckNext() {
		return "($env.$iter$"+iter_var+".hasMoreElements()"+
				" && "+createTextVarAccess(var)+".$rebind_chk($env.$iter$"+iter_var+".nextElement()))";
	}

	private String createTextContaince() {
		switch( mode ) {
		case ARRAY:
			return "kiev.stdlib.ArrayEnumerator.contains("+Kiev.reparseExpr(expr,true)+","+var.name+".$var)";
		case KENUM:
			return "kiev.stdlib.PEnv.contains("+Kiev.reparseExpr(expr,true)+","+var.name+".$var)";
		case JENUM:
			return "kiev.stdlib.PEnv.jcontains("+Kiev.reparseExpr(expr,true)+","+var.name+".$var)";
		case ELEMS:
			return Kiev.reparseExpr(expr,true)+".contains("+var.name+".$var)";
		default:
			throw new RuntimeException("Unknown mode of iterator "+mode);
		}
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				createTextUnification()+

			// Bind here
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
				createTextNewIterators()+						// create iterators
			"case "+(base+0)+":\n"+								// backtracking, check next element
				"if( "+createTextCheckNext()+" ) {\n"+
					createTextMoreCheck(true)+
				"} else {\n"+
					createTextUnbindVars()+						// binded here, unbind
					createTextBacktrack(true)+					// backtrack, bt$ may needs to be loaded
				"}\n"+

			// Already binded
			"bound$"+idx+":;\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+(base+1)+";\n"+						// set new backtrack state to point itself
				"if( "+createTextContaince()+" ) {\n"+			// check
					createTextMoreCheck(true)+
				"}\n"+
			"case "+(base+1)+":\n"+
				createTextBacktrack(true)						// backtrack, bt$ may needs to be loaded
		);
	}
}

@node
@dflow(out="this:in")
public final class RuleCutExpr extends ASTRuleNode {

	public RuleCutExpr() {
	}

	public RuleCutExpr(int pos) {
		super(pos);
	}

	public void resolve(Type reqType) {
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)pctx.method).index;
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				"bt$ = 0;\n"+								// backtracking, always fail state, state 0 is 'return null'
				createTextMoreCheck(false)
		);
	}
}

@node
@dflow(out="args")
public final class RuleCallExpr extends ASTRuleNode {

	@att
	@dflow(in="this:in")
	public ENode					obj;
	
	public Named					func;
	
	@att
	@dflow(in="obj", seq="true")
	public final NArr<ENode>		args;
	
	public boolean					super_flag;
	public int						env_var;

	public RuleCallExpr() {
	}

	public RuleCallExpr(CallExpr expr) {
		super(expr.pos);
		this.obj = (ENode)~expr.obj;
		this.func = expr.func;
		this.args.addAll(expr.args.delToArray());
		this.super_flag = expr.super_flag;
	}

	public RuleCallExpr(ClosureCallExpr expr) {
		super(expr.pos);
		this.obj = (ENode)~expr.expr;
		if( expr.expr instanceof VarExpr )
			this.func = ((VarExpr)expr.expr).getVar();
		else if( expr.expr instanceof StaticFieldAccessExpr )
			this.func = ((StaticFieldAccessExpr)expr.expr).var;
		else if( expr.expr instanceof AccessExpr ) {
			this.func = ((AccessExpr)expr.expr).var;
			this.obj = (ENode)~((AccessExpr)expr.expr).obj;
		}
		this.args.addAll(expr.args.delToArray());
		this.args.insert(0,expr.env_access);
	}

	public void resolve(Type reqType) {
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)pctx.method).index;
		base = ((RuleMethod)pctx.method).allocNewBase(1);
		depth = ((RuleMethod)pctx.method).push();
		env_var = ((RuleMethod)pctx.method).add_iterator_var();
		ASTNode rb = this.parent;
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.parent != null, "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent;
		}
		Debug.assert(rb != null);
		Debug.assert(rb instanceof RuleBlock);
		((RuleBlock)rb).fields_buf.append("rule $rc$frame$")
			.append(env_var).append(";\n");
	}

	private String createTextCall() {
		StringBuffer sb = new StringBuffer();
		sb.append("($env.$rc$frame$").append(env_var).append("=");
		if( obj != null ) {
			if (super_flag) {
				assert (obj instanceof ThisExpr);
				sb.append("super.");
			} else {
				sb.append(Kiev.reparseExpr(obj,true)).append('.');
			}
		}
		else if (super_flag) {
			sb.append("super.");
		}
		sb.append(func.getName()).append('(');
		for(int i=1; i < args.length; i++) {
			sb.append(Kiev.reparseExpr(args[i],true));
			if( i < args.length-1) sb.append(',');
		}
		sb.append("))");
		return sb.toString();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
			"case "+base+":\n"+
				"if( ! "+createTextCall()+" ) {\n"+
					createTextBacktrack(true)+					// backtrack, bt$ may needs to be loaded
				"}\n"+
				createTextMoreCheck(false)
		);
	}
}

@node
@dflow(out="expr")
public abstract class RuleExprBase extends ASTRuleNode {
	@att
	@dflow(in="this:in")
	public ENode		expr;
	
	@att
	@dflow(in="this:in")
	public ENode		bt_expr;

	public RuleExprBase() {
	}

	public RuleExprBase(ENode expr) {
		super(expr.pos);
		this.expr = expr;
	}

	public RuleExprBase(ENode expr, ENode bt_expr) {
		super(expr.pos);
		this.expr = expr;
		this.bt_expr = bt_expr;
	}

	public void resolve(Type reqType) {
		expr.resolve(null);

		if( expr instanceof CallExpr ) {
			CallExpr e = (CallExpr)expr;
			if( e.func.type.ret == Type.tpRule ) {
				replaceWithNodeResolve(reqType, new RuleCallExpr((CallExpr)~e));
				return;
			}
		}
		else if( expr instanceof ClosureCallExpr ) {
			ClosureCallExpr e = (ClosureCallExpr)expr;
			Type tp = e.getType();
			if( tp == Type.tpRule || (tp instanceof ClosureType && ((ClosureType)tp).ret == Type.tpRule && tp.args.length == 0) ) {
				replaceWithNodeResolve(reqType, new RuleCallExpr((ClosureCallExpr)~e));
				return;
			}
		}
	}
}

@node
@dflow(out="expr")
public final class RuleWhileExpr extends RuleExprBase {

	public RuleWhileExpr() {
	}

	public RuleWhileExpr(ENode expr) {
		super(expr);
	}

	public RuleWhileExpr(ENode expr, ENode bt_expr) {
		super(expr, bt_expr);
	}

	public void resolve(Type reqType) {
		super.resolve(reqType);
		if (pslot == null) return; // check we were replaced
		if (!expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(expr,"Boolean expression is requared");
		if (bt_expr != null)
			bt_expr.resolve(null);
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)pctx.method).index;
		base = ((RuleMethod)pctx.method).allocNewBase(1);
		depth = ((RuleMethod)pctx.method).push();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
			"case "+base+":\n"+
				(bt_expr == null ?
					""
				:	Kiev.reparseExpr(bt_expr,true)+";\n"
				)+
				"if ( ! "+Kiev.reparseExpr(expr,true)+" ) {\n"+
					createTextBacktrack(true)+						// backtrack, bt$ may needs to be loaded
				"}\n"+
				createTextMoreCheck(false)
		);
	}
}

@node
@dflow(out="expr")
public final class RuleExpr extends RuleExprBase {

	public RuleExpr() {
	}

	public RuleExpr(ENode expr) {
		super(expr);
	}

	public RuleExpr(ENode expr, ENode bt_expr) {
		super(expr, bt_expr);
	}

	public void resolve(Type reqType) {
		super.resolve(reqType);
		if (pslot == null) {
			if (bt_expr != null)
				throw new CompilerException(bt_expr,"Backtrace expression ignored for rule-call");
			return;
		}
		if (bt_expr != null && expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(bt_expr,"Backtrace expression in boolean rule");
		if (bt_expr != null)
			bt_expr.resolve(null);
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)pctx.method).index;
		if (bt_expr != null) {
			base = ((RuleMethod)pctx.method).allocNewBase(1);
			depth = ((RuleMethod)pctx.method).push();
		}
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				( expr.getType().equals(Type.tpBoolean) ?
					"if ( ! "+Kiev.reparseExpr(expr,true)+" ) {\n"+
						createTextBacktrack(false)+					// backtrack, bt$ already loaded
					"}\n"+
					createTextMoreCheck(false)
				: bt_expr == null ?
					Kiev.reparseExpr(expr,true)+";\n"+
					createTextMoreCheck(false)
				:
					"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
					"bt$ = "+base+";\n"+							// set new backtrack state to point itself
					Kiev.reparseExpr(expr,true)+";\n"+
					createTextMoreCheck(true)+
			"case "+base+":\n"+
					Kiev.reparseExpr(bt_expr,true)+";\n"+
					createTextBacktrack(true)						// backtrack, bt$ needs to be loaded
				)
		);
	}
}

