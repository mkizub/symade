/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.parser;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 271 $
 *
 */

@ThisIsANode(lang=CoreLang)
public class Opdef extends DNode {
	
	@AttrXMLDumpInfo(attr=true, name="priority")
	@nodeAttr public int				prior;
	@AttrXMLDumpInfo(attr=true, name="istype")
	@nodeAttr public boolean			type_operator;
	@AttrXMLDumpInfo(attr=true, name="as-node")
	@nodeAttr public String				as_node;
	@nodeAttr public OpArgument∅		args;
	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr public Method⇑∅			methods;

	public Operator			resolved;

	public Operator makeOperator() {
		if (args.length == 0)
			return null;
		String name = "";
		foreach (OpArgument arg; args)
			name = name + arg.toOpName() + ' ';
		if (resolved != null)
			return resolved;
		this.resolved = new Operator(name.trim().intern());
		return this.resolved;
	}
	
	public COpdef[] compile() {
		Vector<COpArgument> args = new Vector<COpArgument>();
		foreach (OpArgument arg; this.args)
			args.append(arg.compile());
		Vector<Vector<COpArgument>> vargs = new Vector<Vector<COpArgument>>();
		vargs.append(args);
	outer_loop:
		for (int v=0; v < vargs.length; v++) {
			args = vargs[v];
			for (int i=0; i < args.length; i++) {
				COpArgument a = args[i];
				if (a instanceof COpArgOPTIONAL) {
					args.removeElementAt(i);
					Vector<COpArgument> args1 = (Vector<COpArgument>)args.clone();
					for (int j=0; j < a.args.length; j++)
						args1.insertElementAt(i+j, a.args[j]);
					vargs.insertElementAt(v+1, args1);
					v -= 1;
					continue outer_loop;
				}
				if (a instanceof COpArgALTR) {
					if (a.args.length == 0) {
						args.removeElementAt(i);
					} else {
						vargs.removeElementAt(v);
						for (int j=0; j < a.args.length; j++) {
							Vector<COpArgument> args1 = (Vector<COpArgument>)args.clone();
							args1[i] = a.args[j];
							vargs.insertElementAt(v+j, args1);
						}
					}
					v -= 1;
					continue outer_loop;
				}
			}
		}
		COpdef[] copdefs = new COpdef[vargs.length];
		for (int i=0; i < copdefs.length; i++)
			copdefs[i] = new COpdef(this, prior, vargs[i].toArray(), type_operator);
		return copdefs;
	}
	
	public Opdef() {}
	
	public String toString() {
		return "operator "+sname;
	}
	
	public OpArgument[] getOpdefArgs() { return args; }

	public boolean isLeftRecursive() {
		if (type_operator)
			return args[0] instanceof OpArgTYPE;
		else
			return args[0] instanceof OpArgEXPR;
	}
	
	public String toString(ENode e) {
		ENode[] exprs = e.getEArgs();
		StringBuffer sb = new StringBuffer();
		int eidx = 0;
		foreach (OpArgument arg; this.args) {
			if (arg instanceof OpArgEXPR) {
				if (eidx < exprs.length) {
					ENode e = exprs[eidx++];
					if (e == null || e.getPriority(Env.getEnv()) < arg.getPriority())
						sb.append('(').append(e).append(')');
					else
						sb.append(e).append(' ');
				} else {
					sb.append("(???)");
				}
				continue;
			}
			else if (arg instanceof OpArgTYPE) {
				if (eidx < exprs.length) {
					ENode e = exprs[eidx++];
					sb.append(e).append(' ');
				} else {
					sb.append("(???)");
				}
				continue;
			}
			else if (arg instanceof OpArgOPER) {
				sb.append(' ').append(arg.symbol.sname).append(' ');
				continue;
			}
		}
		return sb.toString().trim();
	}
	
	public void addMethod(Method m) {
		assert (m.hasName(this.sname));
		for(int i=0; i < methods.length; i++)
			if (methods[i].dnode == m)
				return;
		methods += (Method⇑)new SymbolRef<Method>(m);
	}

	public Symbol resolveMethod(Env env, ENode expr) {
		ENode[] args = expr.getEArgs();
		ResInfo<Method> info = new ResInfo<Method>(env, expr, this.sname, ResInfo.noStatic);
		Type[] tps = new Type[args.length-1];
		for (int i=0; i < tps.length; i++) {
			tps[i] = args[i+1].getType(env);
			tps[i].checkResolved();
		}
		CallType mt = new CallType(args[0].getType(env), null, tps, env.tenv.tpAny, false);
		if (PassInfo.resolveBestMethodR(args[0].getType(env),info,mt)) {
			DNode dn = info.resolvedDNode();
			if (dn instanceof Method && dn.body instanceof CoreExpr) {
				CoreOperation cop = ((CoreExpr)dn.body).getCoreOperation(env);
				if (cop != null)
					return cop.symbol;
			}
			return info.resolvedSymbol();
		}
		info = new ResInfo<Method>(env, expr, this.sname, 0);
		tps = new Type[args.length];
		for (int i=0; i < tps.length; i++)
			tps[i] = args[i].getType(env);
		mt = new CallType(null, null, tps, env.tenv.tpAny, false);
		if (PassInfo.resolveBestMethodR(this,info,mt)) {
			DNode dn = info.resolvedDNode();
			if (dn instanceof Method && dn.body instanceof CoreExpr) {
				CoreOperation cop = ((CoreExpr)dn.body).getCoreOperation(env);
				if (cop != null)
					return cop.symbol;
			}
			return info.resolvedSymbol();
		}
		return null;
	}

	public final rule resolveOperatorMethodR(ResInfo info, CallType mt)
		SymbolRef<Method>@ m;
	{
		m @= this.methods,
		m.dnode != null,
		info ?= m.dnode.equalsByCast(info.getName(),mt,info.env.tenv.tpVoid,info)
	}
	
	//public boolean isForCoreOperation(CoreOperation operation) {
	//	foreach (Method m; methods; m.body instanceof CoreExpr) {
	//		CoreOperation cop = ((CoreExpr)m.body).getCoreOperation();
	//		if (cop == operation)
	//			return true;
	//	}
	//	return false;
	//}
}

@ThisIsANode(lang=CoreLang)
public abstract class OpArgument extends SNode {
	@AttrXMLDumpInfo(attr=true, name="attr")
	@nodeAttr public String attr_name;
	
	public String getAttrName() { return attr_name; }
	
	@setter public final void set$attr_name(String value) {
		this.attr_name = (value == null) ? null : value.intern();
	}
	public final Opdef getOpdef() {
		ANode p = parent();
		while (p instanceof OpArgument)
			p = p.parent();
		if (p instanceof Opdef)
			return (Opdef)p;
		return null;
	}
	
	public abstract String toOpName();

	public abstract COpArgument compile();
}

@ThisIsANode(lang=CoreLang)
public abstract class OpArgEXPR extends OpArgument {
	public abstract int getPriority();
	public String toOpName() { "V" }
	public String toString() { "EXPR/"+getPriority() }

	public final COpArgEXPR compile() {
		return new COpArgEXPR(this, getPriority());
	}
}

@ThisIsANode(lang=CoreLang)
public final class OpArgEXPR_X extends OpArgEXPR {
	public int getPriority() { getOpdef().prior + 1 }
}

@ThisIsANode(lang=CoreLang)
public final class OpArgEXPR_Y extends OpArgEXPR {
	public int getPriority() { getOpdef().prior }
}

@ThisIsANode(lang=CoreLang)
public final class OpArgEXPR_Z extends OpArgEXPR {
	public int getPriority() { 0 }
}

@ThisIsANode(lang=CoreLang)
public final class OpArgEXPR_P extends OpArgEXPR {
	@AttrXMLDumpInfo(attr=true, name="priority")
	@nodeAttr public int prior;
	public int getPriority() { prior }
}

@ThisIsANode(lang=CoreLang)
public final class OpArgTYPE extends OpArgument {
	public String toString() { "TYPE" }
	public String toOpName() { "T" }

	public final COpArgTYPE compile() {
		return new COpArgTYPE(this);
	}
}

@ThisIsANode(lang=CoreLang)
public final class OpArgIDNT extends OpArgument {
	public String toString() { "IDNT" }
	public String toOpName() { "I" }

	public final COpArgIDNT compile() {
		return new COpArgIDNT(this);
	}
}

@ThisIsANode(lang=CoreLang)
public final class OpArgNODE extends OpArgument {

	@AttrXMLDumpInfo(attr=true, name="as-node")
	@nodeAttr public String				as_node;

	public String toString() { "NODE" }
	public String toOpName() { "N" }

	public final COpArgNODE compile() {
		Class clazz = Class.forName(as_node);
		return new COpArgNODE(this, clazz);
	}
}

@ThisIsANode(lang=CoreLang)
public final class OpArgOPER extends OpArgument {

	@AttrXMLDumpInfo(attr=true, name="symbol")
	@nodeAttr public Symbol			symbol;
	
	@AttrXMLDumpInfo(attr=true, name="as-node")
	@nodeAttr public String				as_node;

	public String getText() { return symbol.sname; }

	public OpArgOPER() {
		this.symbol = new Symbol();
		this.symbol.setUUID(null, "");
	}
	
	public String toString() { "OPER "+symbol.sname }

	public String toOpName() { this.symbol.sname }

	public final COpArgOPER compile() {
		Class clazz = null;
		if (as_node != null && as_node.length() > 0)
			clazz = Class.forName(as_node);
		return new COpArgOPER(this, symbol.sname, clazz);
	}
}

@ThisIsANode(lang=CoreLang)
public class OpArgSEQS extends OpArgument {
	@nodeAttr public OpArgument∅		args;

	public String toString() { "SEQS"+toOpName() }

	public OpArgument[] getSeqArgs() { return args; }
	
	public String toOpName() {
		String name = "{ ";
		foreach (OpArgument arg; args)
			name = name + arg.toOpName() + " ";
		name = name + "}"
		return name;
	}

	public final COpArgSEQS compile() {
		COpArgument[] args = new COpArgument[this.args.length];
		for (int i=0; i < args.length; i++)
			args[i] = this.args[i].compile();
		return new COpArgSEQS(this, args);
	}
}

@ThisIsANode(lang=CoreLang)
public class OpArgOPTIONAL extends OpArgument {
	@nodeAttr public OpArgument∅		args;

	public String toString() { "OPTIONAL"+toOpName() }

	public OpArgument[] getOptArgs() { return args; }
	
	public String toOpName() {
		String name = "{ ";
		foreach (OpArgument arg; args)
			name = name + arg.toOpName() + " ";
		name = name + "}?"
		return name;
	}

	public final COpArgOPTIONAL compile() {
		COpArgument[] args = new COpArgument[this.args.length];
		for (int i=0; i < args.length; i++)
			args[i] = this.args[i].compile();
		return new COpArgOPTIONAL(this, args);
	}
}

@ThisIsANode(lang=CoreLang)
public class OpArgALTR extends OpArgument {
	@nodeAttr public OpArgument∅		args;

	public String toString() { "ALTR"+toOpName() }

	public OpArgument[] getAltArgs() { return args; }
	
	public String toOpName() {
		String name = "{ ";
		foreach (OpArgument arg; args)
			name = name + arg.toOpName() + " | ";
		name = name + "}"
		return name;
	}

	public final COpArgALTR compile() {
		COpArgument[] args = new COpArgument[this.args.length];
		for (int i=0; i < args.length; i++)
			args[i] = this.args[i].compile();
		return new COpArgALTR(this, args);
	}
}

@ThisIsANode(lang=CoreLang)
public abstract class OpArgLIST extends OpArgument {
	@nodeAttr public OpArgument el;
	@nodeAttr public OpArgOPER  sep;

	public abstract int getMinCount();

	public OpArgOPER getSeparator() { return sep; }
	
	public String toOpName() {
		if (sep != null)
			return "{ "+el.toOpName()+" "+sep.toOpName()+" }";
		return "{ "+el.toOpName()+" }";
	}

	public final COpArgLIST compile() {
		if (sep != null)
			return new COpArgLIST(this, el.compile(), sep.compile(), getMinCount());
		return new COpArgLIST(this, el.compile(), null, getMinCount());
	}
}

@ThisIsANode(lang=CoreLang)
public final class OpArgLIST_ANY extends OpArgLIST {
	public String toString() { "LIST{"+el+" / "+sep+"}*" }
	public int getMinCount() { return 0; }
	public String toOpName() { super.toOpName()+"*" }
}

@ThisIsANode(lang=CoreLang)
public final class OpArgLIST_ONE extends OpArgLIST {
	public String toString() { "LIST{"+el+" / "+sep+"}+" }
	public int getMinCount() { return 1; }
	public String toOpName() { super.toOpName()+"+" }
}

@ThisIsANode(lang=CoreLang)
public final class OpArgLIST_NUM extends OpArgLIST {
	@AttrXMLDumpInfo(attr=true, name="mincount")
	@nodeAttr public int min;

	public String toString() { "LIST{"+el+" / "+sep+" / "+min+"}" }
	public int getMinCount() { return min; }
	public String toOpName() { super.toOpName()+min }
}

public final class COpdef {
	public final Opdef				source;
	public final int				prior;
	public final int				arity;
	public final int				min_args;
	public final COpArgument[]		args;
	public final String				as_node;
	public final boolean			type_operator;
	public final boolean			left_recursive;
	COpdef(Opdef source, int prior, COpArgument[] args, boolean type_operator) {
		this.source = source;
		this.prior = prior;
		this.args = args;
		this.as_node = source.as_node;
		this.type_operator = type_operator;
		if (type_operator)
			this.left_recursive = (args[0] instanceof COpArgTYPE);
		else
			this.left_recursive = (args[0] instanceof COpArgEXPR);
		int arity = 0;
		int min_args = 0;
		foreach (COpArgument arg; args) {
			if (arg instanceof COpArgLIST) {
				int min = arg.min_count;
				if (min > 0)
					min_args += min*2 - 1;
			}
			else if (arg instanceof COpArgOPER) {
				min_args++;
			}
			else if (arg instanceof COpArgOPTIONAL) {
			}
			else {
				arity++;
				min_args++;
			}
		}
		this.arity = arity;
		this.min_args = min_args;
	}
	public String toString() {
		String s = "operator " + (type_operator ? "type" : "expr"+prior) + " { ";
		foreach (COpArgument a; args)
			s = s + a + " ";
		s = s + "}"
		return s;
	}
}
public abstract class COpArgument {
	public final OpArgument			source;
	public final String				attr_name;
	COpArgument(OpArgument source) {
		this.source = source;
		this.attr_name = source.attr_name;
	}
}
public final class COpArgEXPR extends COpArgument {
	public final int				prior;
	COpArgEXPR(OpArgument source, int prior) {
		super(source);
		this.prior = prior;
	}
	public String toString() {
		return "E"+prior;
	}
}
public final class COpArgTYPE extends COpArgument {
	COpArgTYPE(OpArgument source) {
		super(source);
	}
	public String toString() {
		return "T";
	}
}
public final class COpArgIDNT extends COpArgument {
	COpArgIDNT(OpArgument source) {
		super(source);
	}
	public String toString() {
		return "T";
	}
}
public final class COpArgNODE extends COpArgument {
	public final Class				clazz;
	COpArgNODE(OpArgument source, Class clazz) {
		super(source);
		this.clazz = clazz;
	}
	public String toString() {
		return "N"+clazz.getName();
	}
}
public final class COpArgOPER extends COpArgument {
	public final String				text;
	public final Class				clazz;
	COpArgOPER(OpArgument source, String text, Class clazz) {
		super(source);
		this.text = text;
		this.clazz = clazz;
	}
	public String toString() {
		return text;
	}
}
public final class COpArgLIST extends COpArgument {
	public final COpArgument		el;
	public final COpArgOPER			sep;
	public final int				min_count;
	COpArgLIST(OpArgument source, COpArgument el, COpArgOPER sep, int min_count) {
		super(source);
		this.el = el;
		this.sep = sep;
		this.min_count = min_count;
	}
	public String toString() {
		return "{ "+el+" "+sep+" }"+min_count;
	}
}
public class COpArgOPTIONAL extends COpArgument {
	public final COpArgument[]		args;
	COpArgOPTIONAL(OpArgument source, COpArgument[] args) {
		super(source);
		this.args = args;
	}
	public String toString() {
		String s = "{ ";
		foreach (COpArgument a; args)
			s = s + a + " ";
		s = s + "}?"
		return s;
	}
}
public class COpArgALTR extends COpArgument {
	public final COpArgument[]		args;
	COpArgALTR(OpArgument source, COpArgument[] args) {
		super(source);
		this.args = args;
	}
	public String toString() {
		String s = "{ ";
		foreach (COpArgument a; args)
			s = s + a + " | ";
		s = s + "}"
		return s;
	}
}
public class COpArgSEQS extends COpArgument {
	public final COpArgument[]		args;
	COpArgSEQS(OpArgument source, COpArgument[] args) {
		super(source);
		this.args = args;
	}
	public String toString() {
		String s = "{ ";
		foreach (COpArgument a; args)
			s = s + a + " ";
		s = s + "}"
		return s;
	}
}

