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
package kiev.vlang;
import syntax kiev.Syntax;

import java.util.StringTokenizer;

/**
 * @author Maxim Kizub
 * @version $Revision: 296 $
 *
 */

public final class RewriteContext {
	public final Env env;
	public final ASTNode root;
	public final Hashtable<String,Object> args;

	private static Type getMacroParamType(Env env, Class arg) {
		if      (((Class)ASTNode.class).isAssignableFrom(arg))
			return new ASTNodeType(arg);
		else if (arg.isArray())
			return new ArrayType(getMacroParamType(env, arg.getComponentType()));
		else if (arg == Boolean.class)			return env.tenv.tpBoolean;
		else if (arg == Character.class)		return env.tenv.tpChar;
		else if (arg == Byte.class)				return env.tenv.tpByte;
		else if (arg == Short.class)			return env.tenv.tpShort;
		else if (arg == Integer.class)			return env.tenv.tpInt;
		else if (arg == Long.class)				return env.tenv.tpLong;
		else if (arg == Float.class)			return env.tenv.tpFloat;
		else if (arg == Double.class)			return env.tenv.tpDouble;
		return new ASTNodeType(arg);
	}
	
	public static INode rewriteByMacro(Env env, SpacePtr space, String tdecl_name, String macro_name, Object... args) {
		INode res = rewriteByMacro(env, tdecl_name, macro_name, args);
		if (res instanceof BlockRewr) {
			BlockRewr bl = (BlockRewr)res;
			foreach (ASTNode n; bl.stats.delToArray())
				space += n;
		}
		else if (res instanceof ASTNode) {
			space += (ASTNode)res;
		}
		return res;
	}
	
	public static INode rewriteByMacro(Env env, String tdecl_name, String macro_name, Object... args) {
		TypeDecl tdecl = (TypeDecl)env.loadAnyDecl(tdecl_name);
		if (tdecl == null)
			return null;
		Type[] types = new Type[args.length];
		for (int i=0; i < args.length; i++)
			types[i] = getMacroParamType(env, args[i].getClass());
		Method m = tdecl.resolveMethod(env, macro_name, env.tenv.tpVoid, types);
		if (m == null || m.body == null)
			return null;
		Hashtable<String,Object> params = new Hashtable<String,Object>();
		Var[] mparams = m.params;
		for (int i=0; i < mparams.length; i++)
			params.put(mparams[i].sname, args[i]);
		RewriteContext rctx = new RewriteContext(env, m.body, params);
		return m.body.doRewrite(rctx);
	}
	
	public RewriteContext(Env env, ASTNode root, Hashtable<String,Object> args) {
		this.env = env;
		this.root = root;
		this.args = args;
	}

	public String replace(String s) {
		if (s == null)
			return null;
		//foreach (String k; args.keys(); s.indexOf('\''+k+'\'') >= 0) {
		//	Object val = args.get(k);
		//	if (val instanceof ISymbol)
		//		s = s.replace('\'' + k + '\'', String.valueOf(val.sname));
		//}
		for(;;) {
			int pS = s.indexOf('{');
			if (pS < 0)
				break;
			int pE = s.indexOf('}', pS);
			if (pE <= 0)
				break;
			s = s.substring(0,pS) + subst(s.substring(pS+1,pE)) + s.substring(pE+1);
		}
		return s;
	}
	private String subst(String qname) {
		int pS = qname.indexOf('>');
		if (pS < 0)
			return String.valueOf(args.get(qname));
		Object o = args.get(qname.substring(0,pS));
		while (pS > 0) {
			if (o == null)
				return qname.substring(0,pS)+"=>null";
			int pE = qname.indexOf('>',pS+1);
			if (pE < 0) {
				o = ((ASTNode)o).getVal(((ASTNode)o).getAttrSlot(qname.substring(pS+1).intern()));
				break;
			} else {
				o = ((ASTNode)o).getVal(((ASTNode)o).getAttrSlot(qname.substring(pS+1,pE).intern()));
				pS = pE;
			}
		}
		return String.valueOf(o);
	}
	public INode toINode(Object o) {
		if (o == null)				return null;
		if (o instanceof ANode)		return (ANode)o;
		if (o instanceof Boolean)	return new ConstBoolExpr(o.booleanValue()); 
		if (o instanceof String)	return new ConstStringExpr((String)o);
		if (o instanceof Integer)	return new ConstIntExpr(o.intValue()); 
		if (o instanceof Byte)		return new ConstByteExpr(o.byteValue()); 
		if (o instanceof Short)		return new ConstShortExpr(o.shortValue()); 
		if (o instanceof Float)		return new ConstFloatExpr(o.floatValue()); 
		if (o instanceof Double)	return new ConstDoubleExpr(o.doubleValue()); 
		if (o.getClass().isEnum())	return new ConstEnumExpr((Enum)o);
		if (o instanceof ASTNode[]) {
			BlockRewr bl = new BlockRewr();
			CopyContext cc = new Copier();
			foreach (ASTNode n; (ASTNode[])o) {
				if (n.isAttached())
					bl.stats += cc.copyRoot(n);
				else
					bl.stats += n;
			}
			cc.updateLinks();
			return bl;
		}
		throw new ClassCastException("Cannot convert to ANode value "+o);
	}
	public Object fixup(AttrSlot attr, Object o) {
		if (o instanceof ConstStringExpr) {
			String s = replace(o.value);
			if (attr.typeinfo.clazz == String.class) {
				o = s;
			}
			else if (attr.typeinfo.clazz == SymbolRef.class) {
				o = new SymbolRef<DNode>(s);
			}
			else if (attr.typeinfo.clazz == Symbol.class) {
				o = new Symbol(s);
			}
			else if (attr.typeinfo.clazz == Operator.class) {
				Operator op = Operator.getOperatorByName(s);
				o = op;
			}
		}
		else if (o instanceof SymbolRef && attr.typeinfo.clazz == Symbol.class) {
			o = ((SymbolRef)o).symbol;
		}
		else if (o instanceof ISymbol && attr.typeinfo.clazz == Symbol.class) {
			o = ((ISymbol)o).symbol;
		}
		else if (o instanceof TypeDecl) {
			if (attr.typeinfo.clazz == TypeRef.class)
				o = new TypeRef(o.getType(env));
			else if (attr.typeinfo.clazz == ENode.class)
				o = new TypeRef(o.getType(env));
		}
		else if (o instanceof ASTNode[]) {
			BlockRewr bl = new BlockRewr();
			CopyContext cc = new Copier();
			foreach (ASTNode n; (ASTNode[])o) {
				if (n.isAttached())
					bl.stats += cc.copyRoot(n);
				else
					bl.stats += n;
			}
			cc.updateLinks();
			o = bl;
		}
		if (o instanceof INode && attr.isAttr())
			o = new Copier().copyFull((INode)o);
		return o;
	}
}

@ThisIsANode(lang=MacroLang)
public final class RewriteMatch extends ENode {

	@DataFlowDefinition(out="this:in") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	RewriteCase[]		cases;
	}

	@nodeAttr public RewriteCase∅		cases;

	public RewriteMatch() {}

	public INode doRewrite(RewriteContext ctx) {
		foreach (RewriteCase rc; cases) {
			if (rc.var.match(ctx.root))
				return rc.doRewrite(ctx);
		}
		throw new CompilerException(ctx.root, "Cannot rewrite");
	}

	public ENode matchCase(ASTNode root) {
		foreach (RewriteCase rc; cases; rc.var.match(root))
			return rc;
		throw new CompilerException(root, "Cannot rewrite");
	}

}

@ThisIsANode(lang=MacroLang)
public final class RewritePattern extends ENode {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final RewritePattern[] emptyArray = new RewritePattern[0];
	
	@nodeAttr public Var						var;
	@nodeAttr public RewritePattern∅		vars;
	
	public RewritePattern() {
		this.var = new LVar("_",Env.getEnv().tenv.tpVoid);
	}

	public Type	getType(Env env) { return new ASTNodeType(this); }

	public boolean match(ASTNode node) {
		if ( ((ASTNodeMetaType)((ASTNodeType)getType(Env.getEnv())).meta_type).clazz.equals(node.getClass()) )
			return true;
		return false;
	}

}

@ThisIsANode(lang=MacroLang)
public final class RewriteCase extends ENode implements ScopeOfNames {

	@DataFlowDefinition(out="this:in") private static class DFI {
	@DataFlowDefinition(in="this:in")			RewritePattern		var;
	@DataFlowDefinition(in="var", seq="true")	ASTNode[]			stats;
	}

	public static final RewriteCase[] emptyArray = new RewriteCase[0];
	
	@nodeAttr public RewritePattern		var;
	@nodeAttr public ASTNode∅				stats;

	public RewriteCase() {}

	public rule resolveNameR(ResInfo info)
	{
		info ?= var.var
	;
		info.isForwardsAllowed(),
		var.var.isForward(),
		info.enterForward(var.var) : info.leaveForward(var.var),
		var.getType(info.env).resolveNameAccessR(info)
	}

	public INode doRewrite(RewriteContext ctx) {
		ctx.args.put(var.var.sname, ctx.root);
		INode res = null;
		foreach (ASTNode stat; stats)
			res = stat.doRewrite(ctx);
		return res;
	}

}

@ThisIsANode(lang=MacroLang)
public final class RewriteNodeFactory extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	RewriteNodeArg[]	args;
	}
	
	@nodeAttr public RewriteNodeArg∅		args;
	          private Class					node_class;

	public RewriteNodeFactory() {}
	public RewriteNodeFactory(String ident) {
		this.ident = ident;
		setupClass();
	}
	
	private void setupClass() {
		node_class = ASTNodeMetaType.allNodes.get(ident);
		if (node_class != null)
			return;
		try {
			if (ident.indexOf('.') < 0)
				node_class = Class.forName("kiev.vlang."+ident);
			else
				node_class = Class.forName(ident);
			if (node_class != null) {
				Hashtable<String,Class> allNodes = ASTNodeMetaType.allNodes;
				foreach (String key; allNodes.keys(); node_class.equals(allNodes.get(key))) {
					this.ident = key;
					return;
				}
			}
		} catch (Throwable t) {}
	}

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		if (node_class == null && ident != null) {
			setupClass();
			if (node_class == null)
				Kiev.reportError(this,"Compiler node '"+this.ident+"' does not exists");
		}
	}

	public INode doRewrite(RewriteContext ctx) {
		ASTNode res = (ASTNode)node_class.newInstance();
		foreach (RewriteNodeArg rn; args) {
			INode r = rn.doRewrite(ctx);
			AttrSlot attr = null;
			foreach (AttrSlot a; res.values(); a.name == rn.attr) {
				attr = a;
				break;
			}
			if (attr == null)
				throw new CompilerException(ctx.root, "Cannot find attribute "+rn.attr+" in node "+res.getClass().getName());
			if (attr instanceof ASpaceAttrSlot) {
				if (r instanceof RewriteNodeArgArray) {
					foreach (ASTNode o; ((RewriteNodeArgArray)r).args)
						attr.add(res, (ASTNode)ctx.fixup(attr,o));
				} else {
					attr.add(res, (ASTNode)ctx.fixup(attr,r));
				}
			}
			else if (attr instanceof ScalarAttrSlot) {
				attr.set(res, ctx.fixup(attr,r));
			}
		}
		return res;
	}
}

@ThisIsANode(lang=MacroLang)
public final class RewriteNodeArg extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode				anode;
	}
	
	public static final RewriteNodeArg[] emptyArray = new RewriteNodeArg[0];
	
	@nodeAttr public ENode		anode;
	@nodeAttr public String		attr;

	@setter
	public final void set$attr(String value) {
		this.attr = (value != null) ? value.intern() : null;
	}
	
	public RewriteNodeArg() {}
	public RewriteNodeArg(String attr, ENode node) {
		this.attr = attr;
		this.anode = node;
		assert( attr != null && node != null );
	}

	public INode doRewrite(RewriteContext ctx) {
		return anode.doRewrite(ctx);
	}
}

@ThisIsANode(lang=MacroLang)
public final class RewriteNodeArgArray extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		args;
	}

	@nodeAttr public ASTNode∅				args;

	public RewriteNodeArgArray() {}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		for(int i=0; i < args.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(args[i]);
		}
		sb.append('}');
		return sb.toString();
	}
}

@ThisIsANode(name="BlockRewr", lang=MacroLang)
public class BlockRewr extends Block {
	
	@DataFlowDefinition(out="this:out()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		stats;
	}

	public BlockRewr() {}

	public BlockRewr(ENode[] stats) {
		this.stats.addAll(stats);
	}

}

@ThisIsANode(lang=MacroLang)
public class IfElseRewr extends ENode {
	
	@DataFlowDefinition(out="join thenSt elseSt") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		cond;
	@DataFlowDefinition(in="cond:true")	ENode		thenSt;
	@DataFlowDefinition(in="cond:false")	ENode		elseSt;
	}

	@nodeAttr public ENode			cond;
	@nodeAttr public ENode			thenSt;
	@nodeAttr public ENode			elseSt;

	public IfElseRewr() {}
	
	public IfElseRewr(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		this.pos = pos;
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void postVerify(Env env, INode parent, AttrSlot slot) {
		if (thenSt == null || elseSt == null)
			throw new RuntimeException("Missed then or else part of 'if#'");
	}

	public INode doRewrite(RewriteContext ctx) {
		ENode cond = this.cond;
		if (!cond.isConstantExpr(ctx.env)) {
			cond = (ENode)this.cond.doRewrite(ctx);
			if (!cond.isConstantExpr(ctx.env)) {
				assert (cond.isConstantExpr(ctx.env));
				throw new RuntimeException("Non-constant condition in 'if#'");
			}
		}
		Boolean b = (Boolean)cond.getConstValue(ctx.env);
		if (b.booleanValue())
			return thenSt.doRewrite(ctx);
		else
			return elseSt.doRewrite(ctx);
	}

}

@ThisIsANode(name="SwitchRewr", lang=MacroLang)
public class SwitchRewr extends SwitchStat {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode			sel;
	@DataFlowDefinition(in="sel", seq="true")	ENode[]			stats;
	@DataFlowDefinition(in="stats")				Label			lblcnt;
	@DataFlowDefinition(in="stats")				Label			lblbrk;
	}
	

	public SwitchRewr() {}
	
	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {
		Type tp = sel.getType(env).getErasedType();
		if (tp instanceof ASTNodeType) {
			if (!((ASTNodeMetaType)tp.meta_type).clazz.isEnum())
				Kiev.reportError(this, "Type of switch# selector must be primitive type or enum");
		}
		else if!(tp instanceof CoreType)
			Kiev.reportError(this, "Type of switch# selector must be primitive type or enum");
	}

	public INode doRewrite(RewriteContext ctx) {
		Object sel = this.sel;
		if (!sel.isConstantExpr(ctx.env))
			sel = sel.doRewrite(ctx);
		CaseLabel cl = null;
		if (sel instanceof ConstEnumExpr)
			cl = findEnumCase((ConstEnumExpr)sel);
		else
			Kiev.reportError(this, "Unsupported switch# selector "+sel);
		if (cl == null)
			return null;
		BlockRewr bl = new BlockRewr();
		ASTNode st = (ASTNode)Env.getNextNode(cl);
		while (st != null && !(st instanceof CaseLabel)) {
			bl.stats += (ASTNode)st.doRewrite(ctx);
			st = (ASTNode)Env.getNextNode(st);
		}
		if (bl.stats.length == 0)
			return null;
		if (bl.stats.length == 1)
			return ~bl.stats[0];
		return bl;
	}
	
	private CaseLabel findEnumCase(ConstEnumExpr e) {
		int tag = e.value.ordinal();
		String name = e.value.name().intern();
		String type_name = e.value.getClass().getName().replace('.','·').replace('$','·');
		foreach (CaseLabel cl; stats) {
			ENode val = cl.val;
			if (val instanceof ConstIntExpr && val.value == tag)
				return cl;
			if (val instanceof ConstStringExpr && val.value.equals(name))
				return cl;
			if (val instanceof ConstEnumExpr && val.value == e.value)
				return cl;
			if (val instanceof SFldExpr) {
				if (type_name.equals(val.obj.getTypeDecl(Env.getEnv()).qname()) && name == val.ident)
					return cl;
			}
		}
		return null;
	}

}

@ThisIsANode(lang=MacroLang)
public class ForEachRewr extends ENode implements ScopeOfNames {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")	Var			var;
	@DataFlowDefinition(in="var")		ASTNode		container;
	@DataFlowDefinition(in="container")	ASTNode		cond;
	@DataFlowDefinition(in="cond")		ASTNode		body;
	}

	@nodeAttr public Var			var;
	@nodeAttr public ASTNode		container;
	@nodeAttr public ASTNode		cond;
	@nodeAttr public ASTNode		body;

	public ForEachRewr() {}
	
	public INode doRewrite(RewriteContext ctx) {
		BlockRewr cont = (BlockRewr)this.container.doRewrite(ctx);
		BlockRewr bl = new BlockRewr();
		foreach (ASTNode n; cont.stats) {
			Type tp = var.getType(ctx.env);
			if (tp instanceof ASTNodeType) {
				if (!((ASTNodeMetaType)tp.meta_type).clazz.isAssignableFrom(n.getClass()))
					continue;
			}
			RewriteContext rc = new RewriteContext(ctx.env, this, (Hashtable<String,Object>)ctx.args.clone());
			rc.args.put(var.sname, n);
			if (this.cond != null) {
				INode c = this.cond.doRewrite(rc);
				if!(c instanceof ENode)
					continue;
				ENode e = (ENode)c;
				if (!e.isConstantExpr(ctx.env))
					continue;
				if (e.getConstValue(ctx.env) != Boolean.TRUE)
					continue;
			}
			ASTNode r = (ASTNode)body.doRewrite(rc);
			if (r instanceof BlockRewr) {
				foreach (ASTNode st; r.stats.delToArray())
					bl.stats += st;
			} else {
				bl.stats += r;
			}
		}
		return bl;
	}

	public rule resolveNameR(ResInfo path)
	{
		path ?= var
	}

}

@ThisIsANode(lang=MacroLang)
public class MacroListIntExpr extends ENode {
	
	@DataFlowDefinition(out="end") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode	start;
	@DataFlowDefinition(in="start")		ENode	end;
	}

	@nodeAttr public ENode			start;
	@nodeAttr public ENode			end;

	public MacroListIntExpr() {}
	
	public INode doRewrite(RewriteContext ctx) {
		int si = 0;
		if (start != null) {
			INode s = start.doRewrite(ctx);
			if (s instanceof Number)
				si = s.intValue();
			else if (s instanceof ENode && s.isConstantExpr(ctx.env))
				si = ((Number)s.getConstValue(ctx.env)).intValue();
			else
				return null;
		}
		int ei = 1;
		if (end != null) {
			INode e = end.doRewrite(ctx);
			if (e instanceof Number)
				ei = e.intValue();
			else if (e instanceof ENode && e.isConstantExpr(ctx.env))
				ei = ((Number)e.getConstValue(ctx.env)).intValue();
			else
				return null;
		}
		BlockRewr b = new BlockRewr();
		for (int i=si; i <= ei; i++)
			b.stats += new ConstIntExpr(i);
		return b;
	}

}

@ThisIsANode(name="Self", lang=MacroLang)
public final class MacroSelfExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	public MacroSelfExpr() {}

	public Type getType(Env env) {
		try {
			ComplexTypeDecl td = Env.ctxTDecl(this);
			if (td == null)
				return env.tenv.tpVoid;
			if (td.sname == nameIFaceImpl)
				return Env.ctxTDecl(td).getType(env);
			return td.getType(env);
		} catch(Exception e) {
			Kiev.reportError(this,e);
			return env.tenv.tpVoid;
		}
	}

	public String toString() { return "self"; }

	public INode doRewrite(RewriteContext ctx) {
		return ((CallExpr)ctx.root).obj;
	}
}

@ThisIsANode(name="MacroAccess", lang=MacroLang)
public final class MacroAccessExpr extends ENode {
	
	@DataFlowDefinition(out="obj") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			obj;
	}

	@nodeAttr public ENode			obj;

	public MacroAccessExpr() {}

	public CoreOperation getOperation(Env env) { env.coreFuncs.fMacroAccess.operation }

	public Type getType(Env env) {
		ENode obj = this.obj;
		Type ot;
		if (obj instanceof LVarExpr && obj.getVarSafe().parent() instanceof RewritePattern)
			ot = ((RewritePattern)obj.getVarSafe().parent()).getType(env).getErasedType();
		else
			ot = obj.getType(env).getErasedType();
		if!(ot instanceof ASTNodeType)
			return env.tenv.tpVoid;
		String name = ("attr$"+ident+"$type").intern();
		int n = ot.getArgsLength();
		for (int i=0; i < n; i++) {
			if (ot.getArg(i).name == name)
				return ot.resolveArg(i);
		}
		foreach (AttrSlot a; obj.values(); a.name == this.ident) {
			Class clazz = a.typeinfo.clazz;
			if (clazz == Boolean.class) return env.tenv.tpBoolean;
			if (clazz == String.class) return env.tenv.tpString;
			if (clazz == Integer.class) return env.tenv.tpInt;
			if (clazz == Byte.class) return env.tenv.tpByte;
			if (clazz == Short.class) return env.tenv.tpShort;
			if (clazz == Long.class) return env.tenv.tpLong;
			if (clazz == Float.class) return env.tenv.tpFloat;
			if (clazz == Double.class) return env.tenv.tpDouble;
			if (clazz == Character.class) return env.tenv.tpChar;
			if (clazz.isEnum()) return new ASTNodeType(clazz);
			return new ASTNodeType(clazz);
		}
		return env.tenv.tpVoid;
	}
/*
	public boolean	isConstantExpr(Env env) {
		if( var.isFinal() ) {
			if (var.init != null && var.init.isConstantExpr(env))
				return true;
			else if (var.const_value != null)
				return true;
		}
		return false;
	}
*/
	public Object	getConstValue(Env env) {
		return obj.getVal(obj.getAttrSlot(this.ident));
	}

	public String toString() {
		if (obj == null)
			return String.valueOf(ident);
		if (obj.getPriority(Env.getEnv()) < opAccessPriority)
			return "("+obj.toString()+")>->"+ident;
		else
			return obj.toString()+">->"+ident;
	}

	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {
		ENode obj = this.obj;
		Type tp;
		if (obj instanceof LVarExpr && obj.getVarSafe().parent() instanceof RewritePattern)
			tp = ((RewritePattern)obj.getVarSafe().parent()).getType(env).getErasedType();
		else
			tp = obj.getType(env).getErasedType();
		if!(tp instanceof ASTNodeType)
			throw new CompilerException(this, "Accessor must be an AST node");
		ResInfo<Field> info = new ResInfo<Field>(env,this,ident,ResInfo.noStatic | ResInfo.noSyntaxContext | ResInfo.noForwards);
		if!(tp.resolveNameAccessR(info)) {
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			msg.append("\t").append(tp).append('\n');
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
	}

	public INode doRewrite(RewriteContext ctx) {
		INode obj = this.obj.doRewrite(ctx);
		if (obj == null)
			return null;
		return ctx.toINode(obj.getVal(obj.getAttrSlot(this.ident)));
	}
}


@ThisIsANode(lang=MacroLang)
public class MacroSubstExpr extends ENode {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode	expr;
	}

	@nodeAttr public ENode			expr;

	public MacroSubstExpr() {}
	
	public INode doRewrite(RewriteContext ctx) {
		return expr.doRewrite(ctx);
	}

}

@ThisIsANode(lang=MacroLang)
public class MacroSubstTypeRef extends TypeRef {
	
	@DataFlowDefinition(out="mtype") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		mtype;
	@DataFlowDefinition(in="mtype")	ENode		req_type;
	}

	@nodeAttr public ENode			mtype;
	@nodeAttr public TypeRef		req_type;

	public MacroSubstTypeRef() {}
	
	public INode doRewrite(RewriteContext ctx) {
		return mtype.doRewrite(ctx);
	}
	
	public Type getType(Env env) {
		if (req_type != null)
			return req_type.getType(env);
		return env.tenv.tpAny;
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		return true;
	}

	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) {
		return true;
	}

}

@ThisIsANode(name="CmpNode", lang=MacroLang)
public class MacroBinaryBoolExpr extends ENode {
	
	@DataFlowDefinition(out="expr2") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode			expr1;
	@DataFlowDefinition(in="expr1")			ENode			expr2;
	}
	
	@nodeAttr public ENode			expr1;
	@nodeAttr public ENode			expr2;

	public MacroBinaryBoolExpr() {}

	public void initFrom(ENode node, Symbol sym, ENode[] args) {
		this.pos = node.pos;
		this.symbol = sym;
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public ENode[] getEArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { toStringByOpdef() }

	public Type getType(Env env) { return env.tenv.tpBoolean; }

	public INode doRewrite(RewriteContext ctx) {
		INode e1 = expr1.doRewrite(ctx);
		if (e1 instanceof ConstNullExpr) e1 = null;
		INode e2 = expr2.doRewrite(ctx);
		if (e2 instanceof ConstNullExpr) e2 = null;
		Operator op = getOper();
		if (op == Operator.Equals) {
			if (e1 == null && e2 == null)
				return new ConstBoolExpr(true);
			if (e1 == null || e2 == null)
				return new ConstBoolExpr(false);
			return new ConstBoolExpr(e1.equals(e2));
		}
		if (op == Operator.NotEquals) {
			if (e1 == null && e2 == null)
				return new ConstBoolExpr(false);
			if (e1 == null || e2 == null)
				return new ConstBoolExpr(true);
			return new ConstBoolExpr(!e1.equals(e2));
		}
		return new ConstBoolExpr(false);
	}
	
	public boolean isConstantExpr(Env env) {
		if (!expr1.isConstantExpr(env))
			return false;
		if (!expr2.isConstantExpr(env))
			return false;
		DNode m = this.dnode;
		if !(m instanceof Method) {
			Opdef opd = resolveOpdef(env);
			if (opd == null)
				return false;
			Symbol sym = opd.resolveMethod(env,this);
			if (sym != null) {
				this.symbol = sym;
				m = sym.dnode;
			}
		}
		if (m instanceof CoreOperation)
			return true;
		return false;
	}
	public Object getConstValue(Env env) {
		return ((CoreOperation)this.dnode).calc(this).getConstValue(env);
	}
}


@ThisIsANode(name="HasMeta", lang=MacroLang)
public class MacroHasMetaExpr extends ENode {
	
	@DataFlowDefinition(out="meta") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode			expr;
	@DataFlowDefinition(in="expr")			MNode			meta;
	}
	
	@nodeAttr public ENode			expr;
	@nodeAttr public MNode			meta;

	public MacroHasMetaExpr() {}

	public Type getType(Env env) { return env.tenv.tpBoolean; }
	
	public int getPriority(Env env) { return opInstanceOfPriority; }

	public INode doRewrite(RewriteContext ctx) {
		INode expr = this.expr.doRewrite(ctx);
		INode meta = this.meta.doRewrite(ctx);
		if!(expr instanceof DNode)
			return new ConstBoolExpr(false);
		if!(meta instanceof MNode)
			return new ConstBoolExpr(false);
		MNode m = ((DNode)expr).getMeta(meta.qname());
		if (m == null)
			return new ConstBoolExpr(false);
		return new ConstBoolExpr(m.equals(meta));
	}
}



