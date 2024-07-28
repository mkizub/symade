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
package kiev.fmt.proj;
import syntax kiev.Syntax;

import kiev.fmt.proj.KievProjectorFactory.ExprInfo;

import java.util.regex.Pattern;

/**
 * @author Maxim Kizub
 *
 */

public class KievProjectorFactory extends DefaultNodeProjectorFactory {
	public static class ExprInfo {
		public final Class				clazz;
		public final ScalarAttrSlot[]	arg_slots;
		public ExprInfo(Class clazz, ScalarAttrSlot[] arg_slots) {
			this.clazz = clazz;
			this.arg_slots = arg_slots;
		}
	};
	static final Class[] exprClasses = new Class[] {
		TypeExpr.class, EToken.class,
		RuleIsoneofExpr.class, RuleIstheExpr.class,
		IncrementExpr.class, UnaryExpr.class, BinaryExpr.class, AssignExpr.class, ModifyExpr.class,      
		BooleanNotExpr.class, BinaryBoolExpr.class, BinaryBooleanAndExpr.class, BinaryBooleanOrExpr.class,
		InstanceofExpr.class, CastExpr.class, ReinterpExpr.class, ConditionalExpr.class,
		AccessExpr.class,IFldExpr.class,SFldExpr.class,ContainerAccessExpr.class,LVarExpr.class,
		ConstByteExpr.class, ConstShortExpr.class, ConstIntExpr.class, ConstLongExpr.class,
		ConstFloatExpr.class, ConstDoubleExpr.class, ConstBoolExpr.class, ConstNullExpr.class, 
		ConstCharExpr.class, ConstStringExpr.class, ConstEnumExpr.class,
		ThisExpr.class, SuperExpr.class, OuterThisAccessExpr.class,
		CallExpr.class, CtorCallExpr.class, ClosureCallExpr.class,
		TypeNameRef.class, TypeNameArgsRef.class, TypeInnerNameRef.class, TypeExpr.class, 
	};
	
	public final Env env;
	public final KievSyntax kiev_stx;

	public KievProjectorFactory(Env env) {
		super(new ProjectionContext());
		this.env = env;
		this.kiev_stx = (KievSyntax) env.resolveGlobalDNode("kiev·Syntax");
	}

	public NodeProjector getProjector(INode src, INode dst0) {
		NodeProjector np = null;
		if (src != null) {
			np = allProjectors.get(src);
			if (np != null)
				return np;
			INode dst = null;
			if (Arrays.contains(exprClasses, src.getClass())) {
				dst = new KievExprNode();
				np = new KievExprProjector(this, (ENode)src, (KievExprNode)dst);
			}
			if (np != null) {
				allProjectors.put(src, np);
				allProjectors.put(dst, np);
				return np;
			}
			return super.getProjector(src, dst0);
		}
		if (dst0 != null) {
			return super.getProjector(src, dst0);
		}
		throw new RuntimeException("Cannot get projector for null");
	}
}

public class KievExprProjector implements NodeProjector, ChangeListener {
	
	protected final KievProjectorFactory    npfactory;
	protected       ENode                   src_node;
	protected       KievExprNode            dst_node;
	private IgnoredNodeChangeInfo   ignore_notification;

	public KievExprProjector(KievProjectorFactory npfactory, ENode src_node, KievExprNode dst_node) {
		this.npfactory = npfactory;
		this.src_node = src_node;
		this.dst_node = dst_node;
		src_node.addListener(this);
		dst_node.addListener(this);
		dst_node.projector = this;
	}

	public void project() {
		addExpr(src_node);
	}
	
	private void addExpr(INode node) {
		switch (node) {
		case EToken:
			dst_node.add(new KievETokenNode(((EToken)node).ident));
			return;
		case ConstBoolExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstNullExpr:
			dst_node.add(new KievETokenNode("null"));
			return;
		case ConstByteExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstShortExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstIntExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstLongExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstFloatExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstDoubleExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstCharExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstStringExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ConstEnumExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case ThisExpr:
			dst_node.add(new KievETokenNode("this"));
			return;
		case SuperExpr:
			dst_node.add(new KievETokenNode("super"));
			return;
		case OuterThisAccessExpr: {
			OuterThisAccessExpr e = (OuterThisAccessExpr)node;
			dst_node.add(npfactory.projectNode(e.outer));
			dst_node.add(new KievETokenNode("."));
			dst_node.add(new KievETokenNode("this"));
			}
			return;
		case LVarExpr:
			dst_node.add(new KievETokenNode(node.toString()));
			return;
		case IFldExpr: {
			IFldExpr e = (IFldExpr)node;
			dst_node.add(npfactory.projectNode(e.obj));
			dst_node.add(new KievETokenNode("."));
			dst_node.add(new KievETokenNode(e.ident));
			}
			return;
		case SFldExpr: {
			SFldExpr e = (SFldExpr)node;
			dst_node.add(npfactory.projectNode(e.obj));
			dst_node.add(new KievETokenNode("."));
			dst_node.add(new KievETokenNode(e.ident));
			}
			return;
		case AccessExpr: {
			AccessExpr e = (AccessExpr)node;
			dst_node.add(npfactory.projectNode(e.obj));
			dst_node.add(new KievETokenNode("."));
			dst_node.add(new KievETokenNode(e.ident));
			}
			return;
		case CallExpr: {
			CallExpr e = (CallExpr)node;
			if (e.obj != null) {
				dst_node.add(npfactory.projectNode(e.obj));
				dst_node.add(new KievETokenNode("."));
			}
			Enumeration<TypeRef> targs = (Enumeration<TypeRef>)e.targs.elements();
			if (targs.hasMoreElements()) {
				dst_node.add(new KievETokenNode("<"));
				boolean comma = false;
				foreach (TypeRef tr; targs) {
					if (comma) dst_node.add(new KievETokenNode(","));
					dst_node.add(npfactory.projectNode(tr));
				}
				dst_node.add(new KievETokenNode(">"));
			}
			dst_node.add(new KievETokenNode(e.ident));
			dst_node.add(new KievETokenNode("("));
			boolean comma = false;
			foreach (ENode a; e.args) {
				if (comma) dst_node.add(new KievETokenNode(","));
				addExpr(a);
				comma = true;
			}
			dst_node.add(new KievETokenNode(")"));
			}
			return;
		case CtorCallExpr: {
			CtorCallExpr e = (CtorCallExpr)node;
			addExpr(e.obj);
			dst_node.add(new KievETokenNode("("));
			boolean comma = false;
			foreach (ENode a; e.args) {
				if (comma) dst_node.add(new KievETokenNode(","));
				addExpr(a);
				comma = true;
			}
			dst_node.add(new KievETokenNode(")"));
			}
			return;
		case ClosureCallExpr: {
			ClosureCallExpr e = (ClosureCallExpr)node;
			addExpr(e.expr);
			dst_node.add(new KievETokenNode("("));
			boolean comma = false;
			foreach (ENode a; e.args) {
				if (comma) dst_node.add(new KievETokenNode(","));
				addExpr(a);
				comma = true;
			}
			dst_node.add(new KievETokenNode(")"));
			}
			return;
		case TypeNameRef: {
			TypeNameRef e = (TypeNameRef)node;
			dst_node.add(new KievETokenNode(e.ident));
			}
			return;
		case TypeNameArgsRef: {
			TypeNameArgsRef e = (TypeNameArgsRef)node;
			dst_node.add(new KievETokenNode(e.ident));
			dst_node.add(new KievETokenNode("<"));
			boolean comma = false;
			foreach (TypeRef tr; e.args) {
				if (comma) dst_node.add(new KievETokenNode(","));
				dst_node.add(npfactory.projectNode(tr));
			}
			dst_node.add(new KievETokenNode(">"));
			}
			return;
		case TypeInnerNameRef: {
			TypeInnerNameRef e = (TypeInnerNameRef)node;
			dst_node.add(npfactory.projectNode(e.outer));
			dst_node.add(new KievETokenNode("."));
			dst_node.add(new KievETokenNode(e.ident));
			Enumeration<TypeRef> args = (Enumeration<TypeRef>)e.args.elements();
			if (args.hasMoreElements()) {
				dst_node.add(new KievETokenNode("<"));
				boolean comma = false;
				foreach (TypeRef tr; e.args) {
					if (comma) dst_node.add(new KievETokenNode(","));
					dst_node.add(npfactory.projectNode(tr));
				}
				dst_node.add(new KievETokenNode(">"));
			}
			}
			return;
		case TypeExpr: {
			TypeExpr e = (TypeExpr)node;
			dst_node.add(npfactory.projectNode(e.arg));
			String op_name = e.op_name;
			if (op_name != null && op_name.length() > 2)
				op_name = op_name.substring(2);
			dst_node.add(new KievETokenNode(op_name));
			}
			return;
		}
		
		if (!Arrays.contains(npfactory.exprClasses, node.getClass())) {
			dst_node.add(npfactory.projectNode(node));
			return;
		}
		ENode e = (ENode)node;
		Opdef opdef = e.getFakeOpdef(npfactory.env);
		if (opdef == null) {
			DNode dn = e.dnode;
			if (dn == null)
				dn = e.getOperation(npfactory.env);
			if (dn instanceof Method)
				opdef = npfactory.kiev_stx.getOpdefForMethod(npfactory.env, (Method)dn);
		}
		if (opdef == null) {
			dst_node.add(new KievETokenNode("("));
			dst_node.add(new KievETokenNode("<op>"));
			foreach (ENode n; e.getEArgs())
				addExpr(n);
			dst_node.add(new KievETokenNode(")"));
			return;
		}
		int eidx = 0;
		foreach (OpArgument arg; opdef.args) {
			if (arg instanceof OpArgEXPR) {
				ENode a = (ENode)e.getVal(e.getAttrSlot(arg.attr_name));
				if (a == null || a.getPriority(npfactory.env) < arg.getPriority()) {
					dst_node.add(new KievETokenNode("("));
					addExpr(a);
					dst_node.add(new KievETokenNode(")"));
				} else {
					addExpr(a);
				}
				continue;
			}
			if (arg instanceof OpArgTYPE) {
				ENode a = (ENode)e.getVal(e.getAttrSlot(arg.attr_name));
				addExpr(a);
				continue;
			}
			if (arg instanceof OpArgIDNT) {
				String id;
				if (arg.attr_name != null && arg.attr_name != "")
					id = (String)e.getVal(e.getAttrSlot(arg.attr_name));
				else
					id = e.ident;
				dst_node.add(new KievETokenNode(id));
				continue;
			}
			if (arg instanceof OpArgOPER) {
				dst_node.add(new KievETokenNode(arg.symbol.sname));
				continue;
			}
			if (arg instanceof OpArgLIST) {
				Object arr = e.getVal(e.getAttrSlot(arg.attr_name));
				boolean separator = false;
				if (arr instanceof ENode[]) {
					foreach (ENode a; (ENode[])arr) {
						if (separator)
							dst_node.add(new KievETokenNode(arg.sep.symbol.sname));
						addExpr(a);
						separator = true;
					}
				}
				else if (arr instanceof ExtSpaceIterator) {
					foreach (ENode a; (ExtSpaceIterator)arr) {
						if (separator)
							dst_node.add(new KievETokenNode(arg.sep.symbol.sname));
						addExpr(a);
						separator = true;
					}
				}
				continue;
			}
		}
	}

	public void putback() {
		throw new RuntimeException("Putback not implemented");
	}

	public INode getSrcNode() {
		return dst_node;
	}

	public INode getDstNode() {
		return dst_node;
	}

	public void dispose() {
		if (src_node != null) {
			src_node.delListener(this);
			src_node = null;
		}
		if (dst_node != null) {
			dst_node.delListener(this);
			dst_node = null;
		}
	}
	
	protected void ignoreSrcCallbackEvent(AttrSlot slot, Object value, int idx) {
		this.ignore_notification = new IgnoredNodeChangeInfo(src_node, slot, value, idx);
	}

	protected void ignoreDstCallbackEvent(AttrSlot slot, Object value, int idx) {
		this.ignore_notification = new IgnoredNodeChangeInfo(dst_node, slot, value, idx);
	}

	// listener interface
	public void callbackNodeChanged(NodeChangeInfo info) {
	}
}

@ThisIsANode
public class KievExprNode extends ANode {
	@nodeAttr
	public ANode∅ nodes;
	
	public boolean unsaved;
	public boolean invalid;
	
	KievExprProjector projector;
	
	public KievExprNode() {
		super(new AHandle(), null);
	}
	
    public String toString() {
    	StringBuffer sb = new StringBuffer();
		sb.append("expr:");
    	foreach(INode n; nodes)
	    	sb.append(' ').append(n);
        return sb.toString();
    }
	
	public void add(INode node) {
		if (node instanceof KievExprNode) {
			foreach (INode n; node.nodes.delToArray())
				add(n);
		} else {
			nodes.append(node.asANode());
		}
	}

	public void parseExpr(boolean save) {
		unsaved = true;
		try {
			KievExprNode expr = new Copier().copyFull(this);
			ASTExprParser pst = new ASTExprParser(Env.getEnv(), Env.ctxSyntaxScope(projector.src_node).getAllOpdefs(), expr.nodes);
			List<ENode> results = pst.parseExpr();
			if (results.length() == 0) {
				StringBuffer msg = new StringBuffer("Expression: '"+this+"' may not be resolved using defined operators");
				foreach(ENode n; results)
					msg.append(n).append("\n");
				System.out.println(msg.toString());
				invalid = true;
				return;
			}
			if (results.length() > 1) {
				StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be resolved as:\n");
				foreach(ENode n; results)
					msg.append(n).append("\n");
				System.out.println(msg.toString());
				invalid = true;
				return;
			}
			ENode res = results.head();
			ENode rc = res.closeBuild();
			System.out.println("Parsed: "+this+"\n=>\t"+res+"\n=>\t"+rc);
			invalid = false;
			if (save)
				projector.src_node.replaceWithNode(rc, projector.src_node.parent(), projector.src_node.pslot());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}

@ThisIsANode
public class KievETokenNode extends ANode implements ASTToken {

	public static final Pattern patternIdent = Pattern.compile("[\\p{Alpha}_$][\\p{Alnum}_$]*");
	public static final Pattern patternOper = Pattern.compile("[\\!\\#\\%\\&\\(\\)\\*-\\/\\:\\;\\<\\=\\>\\?\\[\\\\\\]\\^\\{\\|\\}\\~\\u2190-\\u22F1]+");
	public static final Pattern patternIntConst = Pattern.compile("[\\+\\-]?(?:0[Xx])\\p{Digit}+[Ll]?");
	public static final Pattern patternFloatConst = Pattern.compile("\\p{Digit}+\\.\\p{Digit}*(?:[Ee][\\+\\-]?\\p{Digit}+)?[FfDd]?");

	@nodeAttr public String		text;
	@nodeData public ETokenKind	base_kind;
	@nodeData public INode      value;
	
	public KievETokenNode() {
		super(new AHandle(), null);
		this.base_kind = ETokenKind.UNKNOWN;
	}
	public KievETokenNode(String text) {
		super(new AHandle(), null);
		this.text = text;
		this.base_kind = ETokenKind.UNKNOWN;
	}
	public KievETokenNode(String text, ETokenKind kind) {
		super(new AHandle(), null);
		this.text = text;
		this.base_kind = kind;
	}
	
	public String toString() {
		return text;
	}
	
	public void callbackChanged(NodeChangeInfo info) {
		if (info.content_change) {
			if (info.slot.name == "text" || info.slot.name == "base_kind") {
				INode p = parent();
				if (p instanceof KievExprNode) {
					p.parseExpr(false);
				}
			}
		}
		super.callbackChanged(info);
	}

	@setter public final void set$text(String val) {
		this.value = null;
		this.text = (val == null) ? null : val.intern();
	}
	
	public String getTokenText() {
		return this.text;
	}
	
	public boolean isIdentifier() {
		if (base_kind == ETokenKind.UNKNOWN)
			guessKind();
		return base_kind == ETokenKind.MAYBE_IDENTIFIER || base_kind == ETokenKind.EXPL_IDENTIFIER || base_kind == ETokenKind.TYPE_DECL || base_kind == ETokenKind.SCOPE_DECL;
	}

	public boolean isOperator() {
		if (base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.MAYBE_IDENTIFIER)
			guessKind();
		return base_kind == ETokenKind.MAYBE_OPERATOR || base_kind == ETokenKind.EXPL_OPERATOR;
	}

	public boolean isMaybeOper() {
		if (base_kind == ETokenKind.UNKNOWN)
			guessKind();
		return base_kind == ETokenKind.MAYBE_OPERATOR || base_kind == ETokenKind.EXPL_OPERATOR || base_kind == ETokenKind.MAYBE_IDENTIFIER;
	}

	public TypeRef asType(Env env) {
		if (base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.MAYBE_IDENTIFIER)
			guessKind();
		if (base_kind == ETokenKind.TYPE_DECL && value instanceof TypeDecl)
			return new TypeNameRef(pos, text, ((TypeDecl)value).getType(env));
		return null;
	}
	
	public DNode asScope(Env env) {
		if (base_kind == ETokenKind.UNKNOWN || base_kind == ETokenKind.MAYBE_IDENTIFIER)
			guessKind();
		if ((base_kind == ETokenKind.TYPE_DECL || base_kind == ETokenKind.SCOPE_DECL) && value instanceof DNode)
			return (DNode)value;
		return null;
	}
	
	public ENode asExpr(Env env) {
		guessKind();
		if (value instanceof ConstExpr)
			return (ENode)value;
		return new EToken(0, this.text, this.base_kind);
	}

	private void guessKind() {
		if (base_kind.isExplicit() && value != null)
			return;
		if (value == null)
			value = NopExpr.dummyNode;
		String text = this.text;

		if (text == null || text == "") {
			if (base_kind != ETokenKind.UNKNOWN)
				this.base_kind = ETokenKind.UNKNOWN;
			return;
		}

		SyntaxScope ss = Env.ctxSyntaxScope(this);
		if (ss != null && ss.isOperator(text)) {
			this.base_kind = ETokenKind.MAYBE_OPERATOR;
			return;
		}

		if (text.charAt(0) == '\"' && text.charAt(text.length()-1) == '\"') {
			this.base_kind = ETokenKind.EXPR_STRING;
			this.value = new ConstStringExpr(ConstExpr.source2ascii(text.substring(1,text.length()-1)));
			return;
		}
		if (text.charAt(0) == '\'' && text.charAt(text.length()-1) == '\'') {
			this.base_kind = ETokenKind.EXPR_CHAR;
			char c;
			if( text.length() == 3 )
				c = text.charAt(1);
			else
				c = ConstExpr.source2ascii(text.substring(1,text.length()-1)).charAt(0);
			this.value = new ConstCharExpr(c);
			return;
		}
		if (patternIntConst.matcher(text).matches()) {
			this.base_kind = ETokenKind.EXPR_NUMBER;
			int radix = 10;
			boolean neg = false;
			boolean force_long = false;
			if( text.startsWith("-") ) { text = text.substring(1); neg = true; }
			if( text.endsWith("L") || text.endsWith("l")) { text = text.substring(0,text.length()-2); force_long = true; }
			if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
			else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
			long l = ConstExpr.parseLong(text,radix);
			if (neg)
				l = -l;
			ConstRadixExpr expr;
			if (force_long || l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
				expr = new ConstLongExpr(l);
			else
				expr = new ConstIntExpr((int)l);
			if (radix == 8)
				expr.radix = IntRadix.RADIX_OCT;
			else if (radix == 16)
				expr.radix = IntRadix.RADIX_HEX;
			else
				expr.radix = IntRadix.RADIX_DEC;
			this.value = expr;
			return;
		}
		if (patternFloatConst.matcher(text).matches()) {
			this.base_kind = ETokenKind.EXPR_NUMBER;
			boolean force_float = false;
			if( text.endsWith("F") || text.endsWith("f")) { text = text.substring(0,text.length()-2); force_float = true; }
			double f = Double.parseDouble(text);
			if (force_float)
				this.value = new ConstFloatExpr((float)f);
			else
				this.value = new ConstDoubleExpr(f);
			return;
		}
		
		if (patternIdent.matcher(text).matches())
			this.base_kind = ETokenKind.MAYBE_IDENTIFIER;
		else if (patternOper.matcher(text).matches())
			this.base_kind = ETokenKind.MAYBE_OPERATOR;
		
		// resolve in the path of scopes
		ResInfo info = new ResInfo(Env.getEnv(),this,text);
		if (PassInfo.resolveNameR(this,info)) {
			if (info.resolvedSymbol().parent() instanceof OpArgOPER) {
				this.base_kind = ETokenKind.MAYBE_OPERATOR;
			}
			DNode dn = info.resolvedDNode();
			if (dn instanceof KievPackage) {
				this.base_kind = ETokenKind.SCOPE_DECL;
				value = dn;
			}
			if (dn instanceof TypeDecl) {
				this.base_kind = ETokenKind.TYPE_DECL;
				value = dn;
			}
		}
	}
	
}

