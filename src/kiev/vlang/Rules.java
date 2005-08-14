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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Rules.java,v 1.4.2.1.2.1 1999/02/15 21:45:14 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.1 $
 *
 */

@node
public class RuleMethod extends Method {

	@att public final NArr<Var>		localvars;
	public int						base = 1;
	public int						max_depth = 0;
	public int						state_depth = 0;
	public int						max_vars;
	public int						index;		// index counter for RuleNode.idx

	public RuleMethod() {
	}

	public RuleMethod(ASTNode clazz, KString name, MethodType type, int acc) {
		super(clazz,name,type,acc | ACC_RULEMETHOD);
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

	public void cleanup() {
    	localvars = null;
        super.cleanup();
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
		node ?= this_par
	;
		var @= params,
		var.name.equals(name),
		node ?= var
	;
		!this.isStatic(),
		var ?= this_par,
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	;
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	}

	public ASTNode resolve(Type reqType) {
		trace(Kiev.debugResolve,"Resolving rule "+this);
		PassInfo.push(this);
		try {
			if (!inlined_by_dispatcher)
				NodeInfoPass.init();
			ScopeNodeInfoVector state = NodeInfoPass.pushState();
			state.guarded = true;
			if (!inlined_by_dispatcher) {
				if (!isStatic()) {
					Var p = this_par;
					NodeInfoPass.setNodeType(p,p.type);
					NodeInfoPass.setNodeInitialized(p,true);
				}
				for(int i=0; i < params.length; i++) {
					NodeInfoPass.setNodeType(params[i],params[i].type);
					NodeInfoPass.setNodeInitialized(params[i],true);
				}
			}
			Var penv = params[0];
			assert(penv.name.name == namePEnv && penv.getType() == Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
			for(int i=0; i < localvars.length; i++) {
				NodeInfoPass.setNodeType(localvars[i],localvars[i].type);
//				NodeInfoPass.setNodeInitialized(localvars[i],true);
			}
			if( body != null ) {
				if( type.ret == Type.tpVoid ) body.setAutoReturnable(true);
				if( body instanceof ASTRuleBlock ) {
					body = ((ASTRuleBlock)body).resolve(Type.tpVoid);
					boolean[] exts = Kiev.getExtSet();
					try {
						Kiev.enable(Ext.GotoCase);
						Kiev.enable(Ext.Goto);
						body = ((ASTBlock)body).resolve(Type.tpVoid);
					} finally { Kiev.setExtSet(exts); }
				} else {
					body = ((BlockStat)body).resolve(Type.tpVoid);
				}
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret == Type.tpVoid ) {
					((BlockStat)body).stats.append(new ReturnStat(pos,body,null));
					body.setAbrupted(true);
				} else {
					Kiev.reportError(pos,"Return requared");
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(0/*body.getPos()*/,e);
		} finally {
			PassInfo.pop(this);
			if (!inlined_by_dispatcher)
				NodeInfoPass.close();
		}

		return this;
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

	public void cleanup() {
    	next_check = null;
        next_back = null;
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
public abstract class ASTRuleNode extends ASTNode {
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

	public void cleanup() {
		jn.cleanup();
		jn = null;
	}

	@getter public int get$base() { return base; }
	@setter public void set$base(int b) { base = b; }

	@getter public int get$idx() { return idx; }
	@setter public void set$idx(int i) { idx = i; }

	public ASTNode resolve(Type tp) {
		throw new CompilerException(pos,"Resolving of ASTRuleNode");
	}

	public String createTextUnification(Var var) {
		return "if( "+createTextVarAccess(var)+".$is_bound ) goto bound$"+idx+";\n";
	}

	public String createTextBacktrack(boolean load) {
		if (!jn.more_back)
			return "return null;\n";	// return false - no more solutions
		assert( ((RuleMethod)PassInfo.method).base != 1 || load==false);
		if (jn.next_back!=null && jn.jump_to_back) {
			if (load) return "bt$ = $env.bt$"+depth+"; goto enter$"+jn.next_back.idx+";\n";
			return "goto enter$"+jn.next_back.idx+";\n";
		}
		if (load)
			return "bt$ = $env.bt$"+depth+"; goto case bt$;\n"; // backtrack to saved address
		if (((RuleMethod)PassInfo.method).base == 1)
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

	public String createTextVarAccess(Var v) {
		if( !v.isLocalRuleVar() ) return v.name.toString();
		return "$env."+v;
	}

}


@node
public final class RuleBlock extends ASTNode implements ScopeOfNames {

	@att public ASTRuleNode	node;
	@att public final NArr<ASTNode>		stats;
	public StringBuffer	fields_buf;

	public RuleBlock() {
	}

	public RuleBlock(int pos, ASTNode parent, ASTRuleNode n) {
		super(pos,parent);
		node = n;
	}

	public RuleBlock(int pos, ASTNode parent, ASTRuleNode n, NArr<ASTNode> stats) {
		this(pos,parent,n);
		foreach (ASTNode st; stats)
			this.stats.add(st);
	}

	public RuleBlock(int pos, ASTNode parent, ASTRuleNode n, ASTNode[] stats) {
		this(pos,parent,n);
		foreach (ASTNode st; stats)
			this.stats.add(st);
	}

	public void cleanup() {
		node.cleanup();
		node = null;
		foreach(ASTNode n; stats; n!=null) n.cleanup();
		stats = null;
		super.cleanup();
	}

	public ASTNode resolve(Type tp) {
		PassInfo.push(this);
		NodeInfoPass.pushState();
		try {
			node = (ASTRuleNode)node.resolve(Type.tpVoid);
			fields_buf = new StringBuffer();
			node.resolve1(new JumpNodes(false,null,false,null,false));
			StringBuffer sb = new StringBuffer(256);
			sb.append("{ ");
			// Declare private method frame class
			String tmpClassName = "frame$$";
			sb.append("static class ").append(tmpClassName).append(" extends rule{\n");
			sb.append("int bt$;\n");
			RuleMethod rule_method = (RuleMethod)PassInfo.method;
			// Backtrace holders
			for (int i=0; i < rule_method.max_depth; i++)
				sb.append("int bt$").append(i).append(";\n");
			// Local variables
			foreach(Var v; rule_method.localvars) {
				String tp = Kiev.parserAddr(v.type);
				if( v.type.isWrapper() )
					sb.append("#t"+tp+' '+v.name.name+" := new #t"+tp+"();\n");
				else
					sb.append("#t"+tp+' '+v.name.name+";\n");
			}
			// tmp variables inserted here
			sb.append(fields_buf.toString());
			fields_buf = null;
			sb.append("}\n");
			// Create new method frame or hash values from
			// existing one
			sb.append("int bt$;\n");
			sb.append("if($env==null) {\n");
			sb.append(" $env=new ").append(tmpClassName).append("(); bt$=0;\n");
			sb.append(" goto enter$1;\n");
			sb.append("}\n");
			if (rule_method.base != 1) {
				sb.append("else{\n");
				sb.append(" $env=($cast ").append(tmpClassName).append(")$env;\n");
				sb.append(" bt$=$env.bt$;\n");
				sb.append("}\n");
				sb.append("switch(bt$) {\ncase 0:\n");
			} else {
				// BUG!!!
				sb.append("else{\n$env=($cast ").append(tmpClassName).append(")$env;}\n");
			}
			sb.append("return null;\n");
			node.createText(sb);
			// Close method
			if (rule_method.base != 1)
				sb.append("}\nreturn null;\n");
			sb.append("}\n");
			trace(Kiev.debugRules,"Rule text generated:\n"+sb);
			ASTBlock mbody = (ASTBlock)Kiev.parseBlock(sb,getPosLine(),getPosColumn());
			PassInfo.method.body = mbody;
			mbody.stats.addAll(stats);
			//PassInfo.clazz.makeDispatch(PassInfo.method);
			return PassInfo.method.body;
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
		}
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info, KString name)
		ASTNode@ stat;
	{
		stat @= stats,
		stat instanceof DeclStat,
		((DeclStat)stat).var.name.equals(name),
		node ?= ((DeclStat)stat).var
	}

}


@node
public final class RuleOrExpr extends ASTRuleNode {

	@att public final NArr<ASTRuleNode>	rules;

	public int get$base() {	return rules[0].get$base(); }
	public void set$base(int b) {}

	public int get$idx() {	return rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleOrExpr() {
	}

	public RuleOrExpr(int pos, ASTRuleNode[] rules) {
		super(pos);
		this.rules.addAll(rules);
	}

	public void cleanup() {
		foreach(ASTNode n; rules; n!=null) n.cleanup();
		rules = null;
		super.cleanup();
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public ASTNode resolve(Type reqType) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i] = (ASTRuleNode)rules[i].resolve(reqType);
    	}
    	return this;
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		int depth = ((RuleMethod)PassInfo.method).state_depth;
		int max_depth = depth;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				j = new JumpNodes(jn.more_check, jn.next_check, true, rules[i+1], true);
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, jn.more_back, jn.next_back, jn.jump_to_back);
			}
			((RuleMethod)PassInfo.method).set_depth(depth);
			rules[i].resolve1(j);
			max_depth = Math.max(max_depth,((RuleMethod)PassInfo.method).state_depth);
		}
		((RuleMethod)PassInfo.method).set_depth(max_depth);
	}
}

@node
public final class RuleAndExpr extends ASTRuleNode {

	@att public final NArr<ASTRuleNode>	rules;

	public int get$base() {	return rules[0].get$base();	}
	public void set$base(int b) {}

	public int get$idx() {	return rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleAndExpr() {
	}

	public RuleAndExpr(int pos, ASTRuleNode[] rules) {
		super(pos);
		this.rules.addAll(rules);
	}

	public void cleanup() {
		foreach(ASTNode n; rules; n!=null) n.cleanup();
		rules = null;
		super.cleanup();
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public ASTNode resolve(Type reqType) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i] = (ASTRuleNode)rules[i].resolve(reqType);
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
    		RuleExpr e = new RuleExpr(new BinaryBooleanAndExpr(e1.pos,e1.expr,e2.expr));
    		rules[i] = e;
			rules.del(i+1);
    		i--;
    	}
    	if (rules.length == 1)
    		return rules[0];
    	return this;
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
public final class RuleIstheExpr extends ASTRuleNode {

	@att public Var		var;		// variable of type PVar<...>
	@att public Expr	expr;		// expression to check/unify

	public RuleIstheExpr() {
	}

	public RuleIstheExpr(int pos, Var var, Expr expr) {
		super(pos);
		this.var = var;
		this.expr = expr;
	}

	public void cleanup() {
		var = null;
		expr.cleanup();
		expr = null;
		super.cleanup();
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)PassInfo.method).index;
		base = ((RuleMethod)PassInfo.method).allocNewBase(1);
		depth = ((RuleMethod)PassInfo.method).push();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				createTextUnification(var)+

			// Unbound
				createTextVarAccess(var)+".$bind(#e"+Kiev.parserAddr(expr.copy())+");\n"+
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
				"if( !"+createTextVarAccess(var)+".equals(#e"+Kiev.parserAddr(expr.copy())+") ) {\n"+	// check
					createTextBacktrack(false)+					// backtrack, bt$ already loaded
				"}\n"+
				createTextMoreCheck(false)							// check next
		);
	}
}

@node
public final class RuleIsoneofExpr extends ASTRuleNode {

	@ref public final NArr<Var>		vars;		// variable of type PVar<...>
	@att public final NArr<Expr>	exprs;		// expression to check/unify
	public int[]	iter_vars;	// iterator var

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;

	public Type[]		itypes;
	public int[]		modes;

	public RuleIsoneofExpr() {
	}

	public RuleIsoneofExpr(int pos, Var[] vars, Expr[] exprs) {
		super(pos);
		this.vars.addAll(vars);
		this.exprs.addAll(exprs);
	}

	public void cleanup() {
		foreach(ASTNode n; exprs; n!=null) n.cleanup();
		exprs = null;
		vars = null;
		super.cleanup();
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)PassInfo.method).index;
		base = ((RuleMethod)PassInfo.method).allocNewBase(2);
		depth = ((RuleMethod)PassInfo.method).push();
		iter_vars = new int[vars.length];
		itypes = new Type[vars.length];
		modes = new int[vars.length];
		for(int i=0; i < vars.length; i++) {
			exprs[i] = (Expr)exprs[i].resolve(null);
			Type ctype = exprs[i].getType();
			Method@ elems;
			if( ctype.isArray() ) {
				itypes[i] = Type.newRefType(Env.getStruct(KString.from("kiev.stdlib.ArrayEnumerator")),new Type[]{ctype.args[0]});
				modes[i] = ARRAY;
			} else if( ctype.isInstanceOf( Type.tpKievEnumeration) ) {
				itypes[i] = ctype;
				modes[i] = KENUM;
			} else if( ctype.isInstanceOf( Type.tpJavaEnumeration) ) {
				itypes[i] = ctype;
				modes[i] = JENUM;
			} else if( PassInfo.resolveBestMethodR(ctype,elems,new ResInfo(ResInfo.noStatic|ResInfo.noImports),
					nameElements,MethodType.newMethodType(null,Type.emptyArray,Type.tpAny))
			) {
				itypes[i] = Type.getRealType(ctype,elems.type.ret);
				modes[i] = ELEMS;
			} else {
				throw new CompilerException(exprs[i].pos,"Container must be an array or an Enumeration "+
					"or a class that implements 'Enumeration elements()' method, but "+ctype+" found");
			}
			iter_vars[i] = ((RuleMethod)PassInfo.method).add_iterator_var();
			ASTNode rb = this.parent;
			while( rb!=null && !(rb instanceof RuleBlock)) {
				Debug.assert(rb.parent != null, "Parent of "+rb.getClass()+":"+rb+" is null");
				rb = rb.parent;
			}
			Debug.assert(rb != null);
			Debug.assert(rb instanceof RuleBlock);
			((RuleBlock)rb).fields_buf.append(itypes[i])
				.append(' ').append("$iter$").append(iter_vars[i]).append(";\n");
		}
	}

	private String createTextCheckUnbinded() {
		String s = "";
		for(int i=0; i < vars.length; i++ )
			s = s + "("+createTextVarAccess(vars[i])+".$is_bound)"+
				( i < vars.length-1 ? " || " : "" );
		return s;
	}

	private String createTextUnification() {
		if( vars.length > 1 ) {
			return
				"if( "+createTextCheckUnbinded()+" ) {\n"+
					"throw new RuntimeException(\"All vars must be unbinded\");\n"+
				"}\n"
			;
		} else {
			return createTextUnification(vars[0]);
		}
	}

	private String createTextNewIterator(int i) {
		switch( modes[i] ) {
		case ARRAY:
			return "new "+itypes[i]+"(#e"+Kiev.parserAddr(exprs[i].copy())+")";
		case KENUM:
			return "#e"+Kiev.parserAddr(exprs[i].copy());
		case JENUM:
			return "#e"+Kiev.parserAddr(exprs[i].copy());
		case ELEMS:
			return "(#e"+Kiev.parserAddr(exprs[i].copy())+").elements()";
		default:
			throw new RuntimeException("Unknown mode of iterator "+modes[i]);
		}
	}

	private String createTextNewIterators() {
		String s = "";
		for(int i=0; i < vars.length; i++ )
			s += "$env.$iter$"+iter_vars[i]+"="+createTextNewIterator(i)+";\n";
		return s;
	}

	private String createTextUnbindVars() {
		String s = "";
		for(int i=0; i < vars.length; i++ ) {
			s = s + "$env.$iter$"+iter_vars[i]+"=null;\n";
			s = s + createTextVarAccess(vars[i])+".$unbind();\n";
		}
		return s;
	}

	private String createTextCheckNext() {
		String s = "";
		for(int i=0; i < vars.length; i++ )
			s = s + "($env.$iter$"+iter_vars[i]+".hasMoreElements()"+
				" && "+createTextVarAccess(vars[i])+".$rebind_chk($env.$iter$"+iter_vars[i]+".nextElement()))"+
				( i < vars.length-1 ? " && " : "" );
		return s;
	}

	private String createTextContaince(int i) {
		switch( modes[i] ) {
		case ARRAY:
			return "kiev.stdlib.ArrayEnumerator.contains(#e"+Kiev.parserAddr(exprs[i].copy())+","+vars[i].name+".$var)";
		case KENUM:
			return "kiev.stdlib.PEnv.contains(#e"+Kiev.parserAddr(exprs[i].copy())+","+vars[i].name+".$var)";
		case JENUM:
			return "kiev.stdlib.PEnv.jcontains(#e"+Kiev.parserAddr(exprs[i].copy())+","+vars[i].name+".$var)";
		case ELEMS:
			return "#e"+Kiev.parserAddr(exprs[i].copy())+".contains("+vars[i].name+".$var)";
		default:
			throw new RuntimeException("Unknown mode of iterator "+modes[i]);
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
				"if( "+createTextContaince(0)+" ) {\n"+			// check
					createTextMoreCheck(true)+
				"}\n"+
			"case "+(base+1)+":\n"+
				createTextBacktrack(true)						// backtrack, bt$ may needs to be loaded
		);
	}
}

@node
public final class RuleCutExpr extends ASTRuleNode {

	public RuleCutExpr() {
	}

	public RuleCutExpr(int pos) {
		super(pos);
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)PassInfo.method).index;
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
public final class RuleCallExpr extends ASTRuleNode {

	@att public Expr				obj;
	public Named					func;
	@att public final NArr<Expr>	args;
	public boolean					super_flag;
	public int						env_var;

	public RuleCallExpr() {
	}

	public RuleCallExpr(CallExpr expr) {
		super(expr.pos);
		this.obj = null;
		this.func = expr.func;
		foreach(Expr e; expr.args) this.args.append(e);
		this.super_flag = expr.super_flag;
	}

	public RuleCallExpr(CallAccessExpr expr) {
		super(expr.pos);
		this.obj = expr.obj;
		this.func = expr.func;
		foreach(Expr e; expr.args) this.args.append(e);
		this.super_flag = expr.super_flag;
	}

	public RuleCallExpr(ClosureCallExpr expr) {
		super(expr.pos);
		this.obj = expr.expr;
		if( expr.func instanceof VarAccessExpr )
			this.func = ((VarAccessExpr)expr.func).var;
		else if( expr.func instanceof StaticFieldAccessExpr )
			this.func = ((StaticFieldAccessExpr)expr.func).var;
		else if( expr.func instanceof AccessExpr ) {
			this.func = ((AccessExpr)expr.func).var;
			this.obj = ((AccessExpr)expr.func).obj;
		}
		foreach(Expr e; expr.args) this.args.append(e);
		this.args.insert(0,expr.env_access);
	}

	public void cleanup() {
		if( obj != null ) {
			obj.cleanup();
			obj = null;
		}
		func = null;
		foreach(ASTNode n; args; n!=null) n.cleanup();
		args = null;
		super.cleanup();
	}

	public ASTNode resolve(Type reqType) {
		return this;
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)PassInfo.method).index;
		base = ((RuleMethod)PassInfo.method).allocNewBase(1);
		depth = ((RuleMethod)PassInfo.method).push();
		env_var = ((RuleMethod)PassInfo.method).add_iterator_var();
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
				sb.append("#e").append(Kiev.parserAddr(obj.copy())).append('.');
			}
		}
		else if (super_flag) {
			sb.append("super.");
		}
		sb.append(func.getName()).append('(');
		for(int i=1; i < args.length; i++) {
			String pa = Kiev.parserAddr(args[i].copy());
			sb.append("#e"+pa);
			trace(Kiev.debugRules,"#e"+pa+" is "+args[i]);
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
public abstract class RuleExprBase extends ASTRuleNode {
	@att public Expr		expr;
	@att public Expr		bt_expr;

	public RuleExprBase() {
	}

	public RuleExprBase(Expr expr) {
		super(expr.pos);
		this.expr = expr;
	}

	public RuleExprBase(Expr expr, Expr bt_expr) {
		super(expr.pos);
		this.expr = expr;
		this.bt_expr = bt_expr;
	}

	public void cleanup() {
		expr.cleanup();
		expr = null;
		super.cleanup();
	}

	public ASTNode resolve(Type reqType) {
		expr = (Expr)expr.resolve(null);

		if( expr instanceof CallExpr ) {
			CallExpr e = (CallExpr)expr;
			if( e.func.type.ret == Type.tpRule )
				return new RuleCallExpr(e).resolve(reqType);
		}
		else if( expr instanceof CallAccessExpr ) {
			CallAccessExpr e = (CallAccessExpr)expr;
			if( e.func.type.ret == Type.tpRule )
				return new RuleCallExpr(e).resolve(reqType);
		}
		else if( expr instanceof ClosureCallExpr ) {
			ClosureCallExpr e = (ClosureCallExpr)expr;
			Type tp = e.getType();
			if( tp == Type.tpRule || (tp instanceof ClosureType && ((ClosureType)tp).ret == Type.tpRule && tp.args.length == 0) )
				return new RuleCallExpr(e).resolve(reqType);
		}

		return this;
	}
}

@node
public final class RuleWhileExpr extends RuleExprBase {

	public RuleWhileExpr() {
	}

	public RuleWhileExpr(Expr expr) {
		super(expr);
	}

	public RuleWhileExpr(Expr expr, Expr bt_expr) {
		super(expr, bt_expr);
	}

	public ASTNode resolve(Type reqType) {
		ASTNode n = super.resolve(reqType);
		if (n != this) return n;
		if (!expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(expr.pos,"Boolean expression is requared");
		if (bt_expr != null)
			bt_expr = (Expr)bt_expr.resolve(null);
		return this;
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)PassInfo.method).index;
		base = ((RuleMethod)PassInfo.method).allocNewBase(1);
		depth = ((RuleMethod)PassInfo.method).push();
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
				:	"#e"+Kiev.parserAddr(bt_expr.copy())+";\n"
				)+
				"if ( !#e"+Kiev.parserAddr(expr.copy())+" ) {\n"+
					createTextBacktrack(true)+						// backtrack, bt$ may needs to be loaded
				"}\n"+
				createTextMoreCheck(false)
		);
	}
}

@node
public final class RuleExpr extends RuleExprBase {

	public RuleExpr() {
	}

	public RuleExpr(Expr expr) {
		super(expr);
	}

	public RuleExpr(Expr expr, Expr bt_expr) {
		super(expr, bt_expr);
	}

	public void cleanup() {
		if (bt_expr != null) {
			bt_expr.cleanup();
			bt_expr = null;
		}
		super.cleanup();
	}

	public ASTNode resolve(Type reqType) {
		ASTNode n = super.resolve(reqType);
		if (n != this) {
			if (bt_expr != null)
				throw new CompilerException(bt_expr.pos,"Backtrace expression ignored for rule-call");
			return n;
		}
		if (bt_expr != null && expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(bt_expr.pos,"Backtrace expression in boolean rule");
		if (bt_expr != null)
			bt_expr = (Expr)bt_expr.resolve(null);

		return this;
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)PassInfo.method).index;
		if (bt_expr != null) {
			base = ((RuleMethod)PassInfo.method).allocNewBase(1);
			depth = ((RuleMethod)PassInfo.method).push();
		}
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				( expr.getType().equals(Type.tpBoolean) ?
					"if ( ! #e"+Kiev.parserAddr(expr.copy())+" ) {\n"+
						createTextBacktrack(false)+					// backtrack, bt$ already loaded
					"}\n"+
					createTextMoreCheck(false)
				: bt_expr == null ?
					"#e"+Kiev.parserAddr(expr.copy())+";\n"+
					createTextMoreCheck(false)
				:
					"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
					"bt$ = "+base+";\n"+							// set new backtrack state to point itself
					"#e"+Kiev.parserAddr(expr.copy())+";\n"+
					createTextMoreCheck(true)+
			"case "+base+":\n"+
					"#e"+Kiev.parserAddr(bt_expr.copy())+";\n"+
					createTextBacktrack(true)						// backtrack, bt$ needs to be loaded
				)
		);
	}
}

