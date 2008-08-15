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
package kiev.transf;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public class XPathME_PreGenerate extends BackendProcessor implements Constants {

	private static final String PROP_BASE				= "symade.transf.xpath";
	public static final String nmXPathUtils			= getPropS(PROP_BASE,"baseView","kiev·vdom·XPathUtils");
	public static final String mnXPathExpr				= getPropS(PROP_BASE,"xpathExpr","kiev·vdom·XPathExpr");
	public static final String mnXPathNSMap			= getPropS(PROP_BASE,"xpathNSMap","kiev·vdom·XPathNSMap");
	public static final String nmQNameValue			= getPropS(PROP_BASE,"qnameValue","kiev·vdom·QNameValue");
	public static final String nmNamespaceMap			= getPropS(PROP_BASE,"nsMap","kiev·vdom·NamespaceMap");
	public static final String nmW3CDomNode			= getPropS(PROP_BASE,"w3cDomNode","org·w3c·dom·Node");
	public static final String nmW3CDomNodeList		= getPropS(PROP_BASE,"w3cDomNodeList","org·w3c·dom·NodeList");
	public static final String nmVDomNode				= getPropS(PROP_BASE,"vDomNode","kiev·vdom·ADomNode");
	
	static Type tpXPathUtils;
	static Type tpQNameValue;
	static Type tpNamespaceMap;
	static Type tpW3CDomNode;
	static Type tpW3CDomNodeList;
	static Type tpVDomNode;

	private XPathME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "View xpath functions pre-generation" }
	public boolean isEnabled() {
		return Kiev.enabled(KievExt.XPath);
	}

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"ViewME_PreGenerate");
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		if (tpXPathUtils == null) {
			tpXPathUtils = Env.getRoot().loadTypeDecl(nmXPathUtils, true).xtype;
			tpQNameValue = Env.getRoot().loadTypeDecl(nmQNameValue, true).xtype;
			tpNamespaceMap = Env.getRoot().loadTypeDecl(nmNamespaceMap, true).xtype;
			tpW3CDomNode = Env.getRoot().loadTypeDecl(nmW3CDomNode, true).xtype;
			tpW3CDomNodeList = Env.getRoot().loadTypeDecl(nmW3CDomNodeList, true).xtype;
			tpVDomNode = Env.getRoot().loadTypeDecl(nmVDomNode, true).xtype;
		}
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}

	public void doProcess(Struct:ASTNode clazz) {
		// generate XPath methods
		foreach (Method m; clazz.members; !m.isAbstract() && m.body == null) {
			UserMeta xpe = (UserMeta)m.getMeta(mnXPathExpr);
			if (xpe == null)
				continue;
			String xpath_expr = xpe.getS("value");
			m.body = new Block(m.pos);
			Type rettp = m.mtype.ret();
			String callName = null;
			TypeRef[] targs = null;
			if (rettp.isInstanceOf(tpVDomNode)) {
				callName = "evalVDomNode";
				targs = new TypeRef[]{new TypeRef(rettp)};
			}
			else if (rettp.isInstanceOf(tpW3CDomNode)) {
				callName = "evalNode";
			}
			else if (rettp.isInstanceOf(tpW3CDomNodeList)) {
				callName = "evalNodeList";
			}
			else if (rettp.isInstanceOf(StdTypes.tpString)) {
				callName = "evalText";
			}
			else if (rettp.isArray()) {
				Type elemtp = rettp.resolve(StdTypes.tpArrayArg);
				if (elemtp.isInstanceOf(StdTypes.tpString)) {
					callName = "evalTextList";
				}
				else if (elemtp.isInstanceOf(tpVDomNode)) {
					callName = "evalVDomNodeList";
					targs = new TypeRef[]{new TypeRef(elemtp)};
				}
			}
			if (callName == null) {
				Kiev.reportError(m,"Unrecognized return type of the method");
				m.block.stats.append(new ConstNullExpr());
				return;
			}
			
			NewInitializedArrayExpr nsctx = new NewInitializedArrayExpr(m.pos, new TypeExpr(tpNamespaceMap,Operator.PostTypeArray), ENode.emptyArray);
			foreach (UserMeta nsmap; ((MetaValueArray)xpe.get("nsmap")).values) {
				nsctx.args += new NewExpr(m.pos, tpNamespaceMap, new ENode[]{new ConstStringExpr(nsmap.getS("prefix")),new ConstStringExpr(nsmap.getS("uri"))});
			}
			
			NewInitializedArrayExpr evars = new NewInitializedArrayExpr(m.pos, new TypeExpr(tpQNameValue,Operator.PostTypeArray), ENode.emptyArray);
			foreach (Var par; m.params; par.kind == Var.PARAM_NORMAL) {
				evars.args += new NewExpr(m.pos, tpQNameValue, new ENode[]{new ConstStringExpr(par.sname),new LVarExpr(0,par)});
			}
			
			ENode ret = new CallExpr(m.pos,new TypeRef(tpXPathUtils),new SymbolRef<Method>(callName),targs,new ENode[]{new ThisExpr(),new ConstStringExpr(xpath_expr), nsctx, evars});
			
			m.block.stats.append(new ReturnStat(m.pos, ret));

			Kiev.runProcessorsOn(m.body);
		}
	}
	
}
