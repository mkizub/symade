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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Rules.java,v 1.4.2.1.2.1 1999/02/15 21:45:14 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.1 $
 *
 */

public class RuleMethod extends Method {

	public Var[]	localvars = Var.emptyArray;
	public int		base = 0;
	public int		max_states = 5;
	public int		state_depth = 5;
	public int		max_vars;

	public RuleMethod(ASTNode clazz, KString name, MethodType type, int acc) {
		super(clazz,name,type,acc | ACC_RULEMETHOD);
	}

	public int allocNewBase() {
		int b = base;
		base += 5;
		return b;
	}

	public void push_state() {
		state_depth++;
		if( state_depth > max_states )
			max_states = state_depth;
	}

	public void push_state(int i) {
		state_depth += i;
		if( state_depth > max_states )
			max_states = state_depth;
	}

	public void set_depth(int i) {
		state_depth = i;
		if( state_depth > max_states )
			max_states = state_depth;
	}

	public int add_iterator_var() {
		return max_vars++;
	}

	public void cleanup() {
    	localvars = null;
        super.cleanup();
	}

	rule public resolveNameR(pvar ASTNode node, pvar List<ASTNode> path, KString name, Type tp, int resfl)
	{
		node @= localvars, ((Var)node.$var).name.equals(name)
	;	inlined_by_dispatcher,$cut,false
	;	node @= params, ((Var)node.$var).name.equals(name)
//		trace(Kiev.debugResolve,"Name "+name+" not found in method's parameters in method "+this);
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
				for(int i=0; i < params.length; i++) {
					PassInfo.addResolvedNode(params[i],this);
					NodeInfoPass.setNodeType(params[i],params[i].type);
					NodeInfoPass.setNodeInitialized(params[i],true);
				}
			}
			for(int i=0; i < localvars.length; i++) {
				PassInfo.addResolvedNode(localvars[i],this);
				NodeInfoPass.setNodeType(localvars[i],localvars[i].type);
//				NodeInfoPass.setNodeInitialized(localvars[i],true);
			}
			max_vars = 0;
			for( int i=0; i < localvars.length; i++)
				if( !localvars[i].isLocalPrologForVar() ) max_vars++;
			if( body != null ) {
				body.parent = this;
				if( type.ret == Type.tpVoid ) body.setAutoReturnable(true);
				if( body instanceof ASTRuleBlock ) {
					body.parent = this;
					body = ((ASTRuleBlock)body).resolve(Type.tpVoid);
					body.parent = this;
					body = ((ASTBlock)body).resolve(Type.tpVoid);
					body.parent = this;
				} else {
					//body.parent = this;
					//((Struct)parent).makeDispatch(this);
					body.parent = this;
					body = ((BlockStat)body).resolve(Type.tpVoid);
				}
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret == Type.tpVoid ) {
					((BlockStat)body).stats = (ASTNode[])Arrays.append(((BlockStat)body).stats,new ReturnStat(pos,body,null));
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


	public boolean compare(KString name, Expr[] args, Type ret, Type type, boolean exact) throws RuntimeException {
		if( !this.name.equals(name) ) return false;
		int type_len = this.type.args.length - 1;
		int args_len = args==null? 0 : args.length;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" differ in number of params: "+type_len+" != "+args_len);
				return false;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" not match in number of params: "+type_len+" != "+args_len);
				return false;
			}
		}
		trace(Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,args,ret));
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if( exact && !args[i].getType().equals(Type.getRealType(type,this.type.args[i+1])) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" differ in param # "+i+": "+Type.getRealType(type,this.type.args[i+1])+" != "+args[i].getType());
				return false;
			}
			else if( !exact && !args[i].getType().isAutoCastableTo(Type.getRealType(type,this.type.args[i+1])) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,args,ret)
					+" differ in param # "+i+": "+args[i].getType()+" not auto-castable to "+Type.getRealType(type,this.type.args[i+1]));
				return false;
			}
		}
		boolean match = false;
		if( ret == null )
			match = true;
		else if( exact &&  Type.getRealType(type,this.type.ret).equals(Type.getRealType(type,ret)) )
			match = true;
		else if( !exact && Type.getRealType(type,this.type.ret).isAutoCastableTo(Type.getRealType(type,ret)) )
			match = true;
		else
			match = false;
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,args,ret)+(match?" match":" do not match"));
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

public abstract class ASTRuleNode extends ASTNode {
	public static ASTRuleNode[]	emptyArray = new ASTRuleNode[0];

	public JumpNodes		jn;

	public abstract 		void	createText(StringBuffer sb);
	public abstract 		void	resolve1(JumpNodes jn);
	public virtual int		base = -100000;

	public ASTRuleNode(int pos) {
		super(pos);
	}

	public void cleanup() {
		jn.cleanup();
		jn = null;
	}

	public int get$base() { return base; }
	public void set$base(int b) { base = b; }

	public ASTNode resolve(Type tp) {
		throw new CompilerException(pos,"Resolving of ASTRuleNode");
	}

	public String createTextMakeLocalVars() {
/*		Var[] lvs = ((RuleMethod)PassInfo.method).localvars;
		String s = "";
		for(int i=0; i < lvs.length; i++) {
			if( !lvs[i].isLocalPrologForVar() )
				s = s + "$env.setVar("+i+",new "+lvs[i].type+"().self);";
		}
		return s;
*/		return "";
	}

	public String createTextUnification(Var var) {
		return
			"case "+base+":\n"+
				( base==0? createTextMakeLocalVars(): "\n")+
				"if( "+createTextVarAccess(var)+".$is_bound ) {\n"+
					"$states[$state_pc] = "+(base+3)+";"+"goto case "+(base+3)+";\n"+
				"} else {\n"+
					"$states[$state_pc] = "+(base+1)+";"+"goto case "+(base+1)+";\n"+
				"}\n"
			;
	}

	public String createTextBacktrack() {
		return
				( jn.more_back?
					( jn.next_back!=null ?
						( jn.jump_to_back ?
							"$states[$state_pc] = "+jn.next_back.base+";"+"goto case "+jn.next_back.base+";\n"
							:
							"goto case $states[--$state_pc];\n"		// pop backtrack
						)
						:
						"goto case $states[--$state_pc];\n"		// pop method's state
					)
					:
					"return null;\n"		// return false
				)
			;
	}


	public String createTextMoreCheck(int backtrack_to) {
		return
				( backtrack_to > 0 ?
					"$states[$state_pc] = "+(base+backtrack_to)+";\n"		// set backtrack state
					:
					""
				)+
				( jn.more_check?
					"$states[++$state_pc]="+jn.next_check.base+";\n"+		// jump to new check
					"goto case "+jn.next_check.base+";\n"
					:
					"$env.$state_pc=$state_pc; return $env;\n"
				)
			;
	}

	public String createTextMoreCheckPush(int backtrack_to) {
		return
				"$states[$state_pc]="+(base+backtrack_to)+";\n"+		// set backtrack state
				( jn.more_check?
					"$states[++$state_pc]="+jn.next_check.base+";\n"+		// jump to new check
					"goto case "+jn.next_check.base+";\n"
					:
					"$env.$state_pc=$state_pc; return $env;\n"
				)
			;
	}

	public String createTextVarAccess(Var v) {
		if( !v.isLocalPrologVar() ) return v.name.toString();
/*		int offs = -1;
		Var[] lvars = ((RuleMethod)PassInfo.method).localvars;
		for(int i=0; i < lvars.length; i++)
			if( v.name.equals(lvars[i].name) )
				{ offs = i; break; }
		if( offs < 0 )
			throw new RuntimeException("Var "+v+" not found in rule's local vars");
		return "(($cast "+v.getType()+")$env.getVar("+offs+"))";
*/		return "$env."+v;
	}

}


public class RuleBlock extends ASTNode implements ScopeOfNames {

	public ASTRuleNode	node;
	public ASTNode[]	stats;
	public StringBuffer	fields_buf;

	public RuleBlock(int pos, ASTNode parent, ASTRuleNode n) {
		super(pos,parent);
		node = n;
		node.parent = this;
		stats = ASTNode.emptyArray;
	}

	public RuleBlock(int pos, ASTNode parent, ASTRuleNode n, ASTNode[] stats) {
		super(pos,parent);
		node = n;
		node.parent = this;
		this.stats = stats;
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
			node.parent = this;
			node = (ASTRuleNode)node.resolve(Type.tpVoid);
			fields_buf = new StringBuffer();
			node.parent = this;
			node.resolve1(new JumpNodes(false,null,false,null,false));
			node.parent = this;
			StringBuffer sb = new StringBuffer(256);
			sb.append("{ ");
/*			sb.append("$env.enter(");
			if( Kiev.debugOutputR )
				sb.append("\"Entered method ").append(PassInfo.method.name.toString())
				.append('/').append(PassInfo.method.type.args.length-1).append('"').append(',');
			sb.append( ((RuleMethod)PassInfo.method).max_states ).append(',')
				.append( ((RuleMethod)PassInfo.method).max_vars+1 );
			sb.append("); ");
*/
			// Declare private method frame class
			String tmpClassName = "frame$$";
			sb.append("static class ").append(tmpClassName).append(" extends rule{\n");
			sb.append("int[] $states; int $state_pc;\n");
			// Local variables
			foreach(Var v; ((RuleMethod)PassInfo.method).localvars) {
				if( v.isPrologVar() )
					sb.append("pvar ").append(v.type.args[0]).append(' ').append(v.name.name).append(";\n");
				else if( v.type.clazz.isWrapper() )
					sb.append(v.type).append(' ').append(v.name.name)
					  .append(" := new ").append(v.type).append("();\n");
				else
					sb.append(v.type).append(' ').append(v.name.name).append(";\n");
			}
			// tmp variables inserted here
			sb.append(fields_buf.toString());
			fields_buf = null;
			sb.append("}\n");
			// Create new method frame or hash values from
			// existing one
			sb.append("int $states[],$state_pc;\n");
			sb.append("if($env==null) {\n");
			sb.append(" $env=new ").append(tmpClassName).append("();$env.$states=$states=new int[50];$state_pc=0;\n");
			sb.append("}else{\n");
			sb.append(" $env=($cast ").append(tmpClassName).append(")$env;\n");
			sb.append(" $states=$env.$states;$state_pc=$env.$state_pc;\n");
			sb.append("}\nswitch($states[$state_pc]) {\n");
			node.createText(sb);
			// Close method
			sb.append("} return null; }\n");
			trace(Kiev.debugRules,"Rule text generated:\n"+sb);
			PassInfo.method.body = Kiev.parseBlock(sb,getPosLine(),getPosColumn());
			if( stats != null && stats.length > 0 ) {
				ASTNode[] bstats = new ASTNode[stats.length+((ASTBlock)PassInfo.method.body).stats.length];
				int i=0;
				for(; i < stats.length; i++) { bstats[i] = stats[i]; bstats[i].parent = PassInfo.method.body; }
				for(int j=0; i < bstats.length; i++, j++) bstats[i] = ((ASTBlock)PassInfo.method.body).stats[j];
				((ASTBlock)PassInfo.method.body).stats = bstats;
			}
			//PassInfo.clazz.makeDispatch(PassInfo.method);
			return PassInfo.method.body;
		} finally {
			PassInfo.pop(this);
			NodeInfoPass.popState();
		}
	}

	rule public resolveNameR(pvar ASTNode node, pvar List<ASTNode> path, KString name, Type tp, int resfl)
		pvar ASTNode stat;
	{
		stat @= stats,
		stat.$var instanceof DeclStat,
		((DeclStat)stat.$var).var.name.equals(name),
		node ?= ((DeclStat)stat.$var).var
	}


}


public class RuleOrExpr extends ASTRuleNode {

	public ASTRuleNode[]	rules;

	public int get$base() {
		return rules[0].get$base();
	}
	public void set$base(int b) {}

	public RuleOrExpr(int pos, ASTRuleNode[] rules) {
		super(pos);
		this.rules = rules;
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
			rules[i].parent = this;
    		rules[i] = (ASTRuleNode)rules[i].resolve(reqType);
			rules[i].parent = this;
    	}
    	return this;
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		int depth = ((RuleMethod)PassInfo.method).state_depth;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				j = new JumpNodes(jn.more_check, jn.next_check, true, rules[i+1], true);
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, jn.more_back, jn.next_back, jn.jump_to_back);
			}
			((RuleMethod)PassInfo.method).set_depth(depth);
			rules[i].parent = this;
			rules[i].resolve1(j);
			rules[i].parent = this;
		}
		((RuleMethod)PassInfo.method).set_depth(((RuleMethod)PassInfo.method).max_states);
	}
}

public class RuleAndExpr extends ASTRuleNode {

	public ASTRuleNode[]	rules;

	public int get$base() {
		return rules[0].get$base();
	}
	public void set$base(int b) {}

	public RuleAndExpr(int pos, ASTRuleNode[] rules) {
		super(pos);
		this.rules = rules;
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
			rules[i].parent = this;
    		rules[i] = (ASTRuleNode)rules[i].resolve(reqType);
			rules[i].parent = this;
    	}
    	return this;
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				if( i==0 )
					j = new JumpNodes(true, rules[i+1], jn.more_back, jn.next_back, jn.jump_to_back);
				else
					j = new JumpNodes(true, rules[i+1], true, rules[i-1], false);
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, true, rules[i-1], false);
			}
			rules[i].parent = this;
			rules[i].resolve1(j);
			rules[i].parent = this;
		}
	}
}

public class RuleIstheExpr extends ASTRuleNode {

	public Var		var;		// variable of type PVar<...>
	public Expr		expr;		// expression to check/unify

	public RuleIstheExpr(int pos, Var var, Expr expr) {
		super(pos);
		this.var = var;
		this.expr = expr;
		expr.parent = this;
	}

	public void cleanup() {
		var = null;
		expr.cleanup();
		expr = null;
		super.cleanup();
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			createTextUnification(var)+

			// Bind here
			"case "+(base+1)+":\n"+								// unification assign value, no checks
				createTextVarAccess(var)+".$bind(#e"+expr.parserAddr()+");\n"+
				"if( "+createTextVarAccess(var)+".$is_bound ) {\n"+
					createTextMoreCheck(2)+
				"} else {\n"+
					createTextBacktrack()+
				"}\n"+
			"case "+(base+2)+":\n"+								// backtracking, always fail state
				createTextVarAccess(var)+".$unbind();\n"+			// binded here, unbind
				createTextBacktrack()+

			// Already binded
			"case "+(base+3)+":\n"+								// unification, check
				"if( "+createTextVarAccess(var)+".equals(#e"+expr.parserAddr()+") ) {\n"+	// check
					createTextMoreCheck(4)+
				"} else {\n"+
					createTextBacktrack()+
				"}\n"+
			"case "+(base+4)+":\n"+
				createTextBacktrack()
		);
	}
}

public class RuleIsoneofExpr extends ASTRuleNode {

	public Var[]	vars;		// variable of type PVar<...>
	public Expr[]	exprs;		// expression to check/unify
	public int[]	iter_vars;	// iterator var

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;

	public Type[]		itypes;
	public int[]		modes;

	public RuleIsoneofExpr(int pos, Var[] vars, Expr[] exprs) {
		super(pos);
		this.vars = vars;
		this.exprs = exprs;
		foreach(Expr e; exprs) e.parent = this;
	}

	public void cleanup() {
		foreach(ASTNode n; exprs; n!=null) n.cleanup();
		exprs = null;
		vars = null;
		super.cleanup();
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state();
		iter_vars = new int[vars.length];
		itypes = new Type[vars.length];
		modes = new int[vars.length];
		for(int i=0; i < vars.length; i++) {
			exprs[i].parent = this;
			exprs[i] = (Expr)exprs[i].resolve(null);
			exprs[i].parent = this;
			Type ctype = exprs[i].getType();
			PVar<Method> elems = new PVar<Method>();
			if( ctype.isArray() ) {
				itypes[i] = Type.newRefType(Env.getStruct(KString.from("kiev.stdlib.ArrayEnumerator")),new Type[]{ctype.args[0]});
				modes[i] = ARRAY;
			} else if( ctype.clazz.instanceOf( Type.tpKievEnumeration.clazz) ) {
				itypes[i] = ctype;
				modes[i] = KENUM;
			} else if( ctype.isInstanceOf( Type.tpJavaEnumeration) ) {
				itypes[i] = ctype;
				modes[i] = JENUM;
			} else if( ctype.clazz.resolveMethodR(elems,new PVar<List<ASTNode>>(List.Nil),nameElements,Expr.emptyArray,null,ctype,ResolveFlags.NoForwards) ) {
				itypes[i] = Type.getRealType(ctype,elems.$var.type.ret);
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
			"case "+base+":\n"+
				( base==0? createTextMakeLocalVars(): "")+
				"if( "+createTextCheckUnbinded()+" ) {\n"+
					"throw new RuntimeException(\"All vars must be unbinded\");\n"+
				"} else {\n"+
					"$states[$state_pc]="+(base+1)+";"+"goto case "+(base+1)+";\n"+
				"}\n"
			;
		} else {
			return createTextUnification(vars[0]);
		}
	}

	private String createTextNewIterator(int i) {
		switch( modes[i] ) {
		case ARRAY:
			return "new "+itypes[i]+"(#e"+exprs[i].parserAddr()+")";
		case KENUM:
			return "#e"+exprs[i].parserAddr();
		case JENUM:
			return "#e"+exprs[i].parserAddr();
		case ELEMS:
			return "(#e"+exprs[i].parserAddr()+").elements()";
		default:
			throw new RuntimeException("Unknown mode of iterator "+modes[i]);
		}
	}

	private String createTextNewIterators() {
		String s = "";
		for(int i=0; i < vars.length; i++ )
			s = s+"$env.$iter$"+iter_vars[i]+"="+createTextNewIterator(i)+";\n";
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
				" && "+createTextVarAccess(vars[i])+".$bind_chk($env.$iter$"+iter_vars[i]+".nextElement()))"+
				( i < vars.length-1 ? " && " : "" );
		return s;
	}

	private String createTextContaince(int i) {
		switch( modes[i] ) {
		case ARRAY:
			return "kiev.stdlib.ArrayEnumerator.contains(#e"+exprs[i].parserAddr()+","+vars[i].name+".$var)";
		case KENUM:
			return "kiev.stdlib.PEnv.contains(#e"+exprs[i].parserAddr()+","+vars[i].name+".$var)";
		case JENUM:
			return "kiev.stdlib.PEnv.jcontains(#e"+exprs[i].parserAddr()+","+vars[i].name+".$var)";
		case ELEMS:
			return "#e"+exprs[i].parserAddr()+".contains("+vars[i].name+".$var)";
		default:
			throw new RuntimeException("Unknown mode of iterator "+modes[i]);
		}
	}

	public void createText(StringBuffer sb) {
		sb.append(
			createTextUnification()+

			// Bind here
			"case "+(base+1)+":\n"+								// unification assign value, no checks
				"{\n"+createTextNewIterators()+					// create iterators
				"if( "+createTextCheckNext()+" ) {\n"+
					createTextMoreCheck(2)+
				"} else {\n"+
					createTextUnbindVars()+						// binded here, unbind
					createTextBacktrack()+
				"}}\n"+
			"case "+(base+2)+":\n"+								// backtracking, check next element
				"{\n"+
				"if( "+createTextCheckNext()+" ) {\n"+
					createTextMoreCheck(0)+
				"} else {\n"+
					createTextUnbindVars()+						// binded here, unbind
					createTextBacktrack()+
				"}}\n"+

			// Already binded
			"case "+(base+3)+":\n"+								// unification, check
				"if( "+createTextContaince(0)+" ) {\n"+			// check
					createTextMoreCheck(4)+
				"} else {\n"+
					createTextBacktrack()+
				"}\n"+
			"case "+(base+4)+":\n"+
				createTextBacktrack()
		);
	}
}

public class RuleCutExpr extends ASTRuleNode {

	public RuleCutExpr(int pos) {
		super(pos);
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"case "+base+":\n"+
				( base==0? createTextMakeLocalVars(): "")+
			"case "+(base+1)+":\n"+								// unification assign value, no checks
			"case "+(base+3)+":\n"+								// unification, check
				createTextMoreCheck(2)+
			"case "+(base+2)+":\n"+								// backtracking, always fail state
			"case "+(base+4)+":\n"+
				"return null;\n"		// return false
		);
	}
}

public class RuleIfExpr extends ASTRuleNode {

	public Expr			cond;
    public ASTRuleNode	thenSt;
    public ASTRuleNode	elseSt;

	public RuleIfExpr(int pos, Expr cond, ASTRuleNode thenSt, ASTRuleNode elseSt) {
		super(pos);
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void cleanup() {
		cond.cleanup();
		cond = null;
		thenSt.cleanup();
		thenSt = null;
		if( elseSt != null ) {
			elseSt.cleanup();
			elseSt = null;
		}
		super.cleanup();
	}

    public ASTNode resolve(Type reqType) {
		cond.parent = this;
    	cond = (Expr)cond.resolve(Type.tpBoolean);
		cond.parent = this;
		thenSt.parent = this;
    	thenSt = (ASTRuleNode)thenSt.resolve(reqType);
		thenSt.parent = this;
    	if( elseSt != null ) {
			elseSt.parent = this;
	    	elseSt = (ASTRuleNode)elseSt.resolve(reqType);
			elseSt.parent = this;
		}
    	return this;
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;

		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state();

		int depth = ((RuleMethod)PassInfo.method).state_depth;
		JumpNodes j;
		j = new JumpNodes(jn.more_check, jn.next_check, jn.more_back, jn.next_back, jn.jump_to_back);
		thenSt.parent = this;
		thenSt.resolve1(j);
		thenSt.parent = this;

		if( elseSt != null ) {
			((RuleMethod)PassInfo.method).set_depth(depth);
			j = new JumpNodes(jn.more_check, jn.next_check, jn.more_back, jn.next_back, jn.jump_to_back);
			elseSt.parent = this;
			elseSt.resolve1(j);
			elseSt.parent = this;
		}

		((RuleMethod)PassInfo.method).set_depth(((RuleMethod)PassInfo.method).max_states);
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"case "+base+":\n"+
				( base==0? createTextMakeLocalVars(): "")+
			"case "+(base+1)+":\n"+								// unification assign value, no checks
			"case "+(base+3)+":\n"+								// unification, check
				"if ( #e"+cond.parserAddr()+" ) {\n"+
					"$states[$state_pc]="+thenSt.base+";\n"+				// jump to "then"
					"goto case "+thenSt.base+";\n"+
				"} else {\n"+
					( elseSt != null ?
						"$states[$state_pc]="+elseSt.base+";\n"+			// jump to "else"
						"goto case "+elseSt.base+";\n"
						:
						"$states[$state_pc]="+(base+2)+";\n"+				// no else, backtrack to me
						"$env.$state_pc=$state_pc;return $env;\n"
					)+
				"}"+
			"case "+(base+2)+":\n"+								// backtracking, always fail state
			"case "+(base+4)+":\n"+
				createTextBacktrack()
		);
		thenSt.createText(sb);
		if( elseSt != null ) elseSt.createText(sb);
	}
}

public class RuleForExpr extends ASTRuleNode {

	public ASTNode		init;
	public DeclStat		initstats[] = new DeclStat[0];
	public Expr			cond;
	public Expr			iter;
	public ASTRuleNode	body;

	public RuleForExpr(int pos, ASTNode init, Expr cond, Expr iter, ASTRuleNode body) {
		super(pos);
		this.init = init;
		this.cond = cond;
		this.iter = iter;
		this.body = body;
	}

    public ASTNode resolve(Type reqType) {
    	Var v = null;
		initstats = (DeclStat[])Arrays.append(initstats,
			(Statement)new DeclStat(pos,PassInfo.method,
				v=new Var(pos,KString.from("for$"+this.hashCode()),Type.tpBoolean,0),
				new ConstBooleanExpr(pos,true)));
		v.setLocalPrologForVar(true);
		if( init instanceof ASTVarDecls ) {
			ASTVarDecls vdecls = (ASTVarDecls)init;
			int flags = 0;
			Type type = ((ASTType)vdecls.type).pass2();
			for(int j=0; j < vdecls.vars.length; j++) {
				ASTVarDecl vdecl = (ASTVarDecl)vdecls.vars[j];
				KString vname = vdecl.name;
				Type tp = type;
				for(int k=0; k < vdecl.dim; k++) tp = Type.newArrayType(tp);
				Statement vstat;
				if( vdecl.init != null )
					vstat = (Statement)new DeclStat(vdecl.pos,PassInfo.method,
						v=new Var(vdecl.pos,vname,tp,flags),vdecl.init);
				else
					throw new CompilerException(vdecl.pos,"Variable must be initialized");
				v.setLocalPrologForVar(true);
				vstat.parent = PassInfo.method;
				vstat = (Statement)vstat.resolve(Type.tpVoid);
				initstats = (DeclStat[])Arrays.append(initstats,vstat);
			}
		} else {
			throw new CompilerException(init.pos,"Variable declaration excpected");
		}
		for(int i=0; i < initstats.length; i++)
			((RuleMethod)PassInfo.method).localvars =
				(Var[])Arrays.append(((RuleMethod)PassInfo.method).localvars,initstats[i].var);
		cond = (Expr)cond.resolve(Type.tpBoolean);
		iter = (Expr)iter.resolve(Type.tpVoid);
		body = (ASTRuleNode)body.resolve(reqType);
    	return this;
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;

		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state();

		JumpNodes j;
		j = new JumpNodes(true, this, true, this, false);
		body.resolve1(j);
	}

	private String createTextInitializeLocalVars() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < initstats.length; i++) {
			sb.append(initstats[i].var.name.name).append(".$bind(").append("#e"+initstats[i].init.parserAddr()).append(");");
		}
		return sb.toString();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"case "+base+":\n"+									// TODO make check on vars in resolve!
				( base==0? createTextMakeLocalVars(): "")+
				"if("+initstats[0].var.name.name+"){\n"+
					"#e"+iter.parserAddr()+";\n"+
				"}else{\n"+
					createTextInitializeLocalVars()+
				"}\n"+
				"if(!(#e"+cond.parserAddr()+")){\n"+
					createTextMoreCheckPush(2)+
				"}else{\n"+
					"$states[++$state_pc]="+body.base+";\n"+
					"goto case "+body.base+";\n"+
				"}\n"+
			// Bind here
			"case "+(base+1)+":\n"+								// unification assign value, no checks
			"case "+(base+3)+":\n"+								// unification, check
				createTextBacktrack()+							// should never come here!
			"case "+(base+2)+":\n"+								// backtracking
			"case "+(base+4)+":\n"+
				createTextBacktrack()
		);
	}
}

public class RuleCallExpr extends ASTRuleNode {

	public Expr		obj;
	public Named	func;
	public Expr[]	args;
	public boolean	super_flag = false;
	public int		env_var;

	public RuleCallExpr(CallExpr expr) {
		super(expr.pos);
		this.obj = null;
		this.func = expr.func;
		this.args = expr.args;
		foreach(Expr e; args) e.parent = this;
	}

	public RuleCallExpr(CallAccessExpr expr) {
		super(expr.pos);
		this.obj = expr.obj;
		this.obj.parent = this;
		this.func = expr.func;
		this.args = expr.args;
		foreach(Expr e; args) e.parent = this;
	}

	public RuleCallExpr(ClosureCallExpr expr) {
		super(expr.pos);
		this.obj = expr.expr;
		if( expr.func instanceof VarAccessExpr )
			this.func = ((VarAccessExpr)expr.func).var;
		else if( expr.func instanceof FieldAccessExpr )
			this.func = ((FieldAccessExpr)expr.func).var;
		else if( expr.func instanceof StaticFieldAccessExpr )
			this.func = ((StaticFieldAccessExpr)expr.func).var;
		else if( expr.func instanceof AccessExpr ) {
			this.func = ((AccessExpr)expr.func).var;
			this.obj = ((AccessExpr)expr.func).obj;
		}
		this.args = (Expr[])Arrays.insert(expr.args,expr.env_access,0);
		foreach(Expr e; args) e.parent = this;
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
		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state(5);
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
		if( obj != null ) sb.append("#e").append(obj.parserAddr()).append('.');
		sb.append(func.getName()).append('(');
		for(int i=1; i < args.length; i++) {
			sb.append("#e"+args[i].parserAddr());
			trace(Kiev.debugRules,"#e"+args[i].parserAddr()+" is "+args[i]);
			if( i < args.length-1) sb.append(',');
		}
		sb.append("))");
		return sb.toString();
	}
/*
	private String createTextCheckBinded() {
		if( !Kiev.debugOutputR ) return "";
		StringBuffer sb = new StringBuffer();
		foreach(Expr e; args; e instanceof VarAccessExpr) {
			VarAccessExpr vae = (VarAccessExpr)e;
			if( vae.var.type.clazz.equals(Type.tpPrologVar.clazz) )
				sb.append("#e").append(vae.parserAddr()).append(".$is_bound(\"")
					.append(vae.var.name).append("\");\n");
		}
		return sb.toString();
	}
*/
	public void createText(StringBuffer sb) {
		sb.append(
			"case "+base+":\n"+									// TODO make check on vars in resolve!
				( base==0? createTextMakeLocalVars(): "")+
				"$states[$state_pc]="+(base+1)+";\n"+
				"goto case "+(base+1)+";\n"+

			// Bind here
			"case "+(base+1)+":\n"+								// unification assign value, no checks
			"case "+(base+3)+":\n"+								// unification, check
			"case "+(base+2)+":\n"+								// backtracking
			"case "+(base+4)+":\n"+
				"if( "+createTextCall()+" ) {\n"+
					createTextMoreCheck(2)+
				"} else {\n"+
					createTextBacktrack()+
				"}\n"
		);
	}
}

public class RuleExpr extends ASTRuleNode {

	public Expr		expr;
	public boolean	while_mode;

	public RuleExpr(Expr expr) {
		super(expr.pos);
		this.expr = expr;
		expr.parent = this;
	}

	public RuleExpr(Expr expr, boolean wm) {
		this(expr);
		while_mode = wm;
	}

	public void cleanup() {
		expr.cleanup();
		expr = null;
		super.cleanup();
	}

	public ASTNode resolve(Type reqType) {
//		if( !expr.getType().equals(Type.tpBoolean) )
//			throw new CompilerException(expr.pos,"Boolean expression is requared");
		expr.parent = this;
		expr = (Expr)expr.resolve(null);
		expr.parent = this;
		if( !while_mode ) {
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
				if( tp == Type.tpRule || (tp instanceof MethodType && ((MethodType)tp).ret == Type.tpRule && tp.args.length == 0) )
					return new RuleCallExpr(e).resolve(reqType);
				else
					System.out.println("ClosureCall "+e+" returns "+e.getType());
			}
		}
		return this;
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		base = ((RuleMethod)PassInfo.method).allocNewBase();
		((RuleMethod)PassInfo.method).push_state();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"case "+base+":\n"+
				( base==0? createTextMakeLocalVars(): "")+
			"case "+(base+1)+":\n"+								// unification assign value, no checks
			"case "+(base+3)+":\n"+								// unification, check
			( while_mode ?
				"case "+(base+2)+":\n"+								// backtracking, always fail state
				"case "+(base+4)+":\n"
			:
				""
			)+
				( expr.getType().equals(Type.tpBoolean) ?
					"if ( #e"+expr.parserAddr()+" ) {\n"+
						createTextMoreCheck(2)+
					"} else {\n"+
						createTextBacktrack()+
					"}\n"
				:
					"#e"+expr.parserAddr()+";\n"+
						createTextMoreCheck(2)
				)+
			( while_mode ?
				""
			:
				"case "+(base+2)+":\n"+								// backtracking, always fail state
				"case "+(base+4)+":\n"+
					createTextBacktrack()
			)
		);
	}
}

