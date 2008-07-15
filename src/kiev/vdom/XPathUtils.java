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
package kiev.vdom;
import syntax kiev.Syntax;

import java.lang.annotation.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;

/**
 * @author Maxim Kizub
 *
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface XPathExpr {
	public String value();
	public XPathNSMap[] nsmap() default {};
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface XPathNSMap {
	public String prefix();
	public String uri();
}

public final class QNameValue extends QName {
	public final Object value;
	public QNameValue(String name, Object value) {
		super(name);
		this.value = value;
	}
}

public final class NamespaceMap {
	public final String prefix;
	public final String uri;
	public NamespaceMap(String prefix, String uri) {
		this.prefix = prefix;
		this.uri = uri;
	}
}

public final class XPathVarResolveHelper implements XPathVariableResolver {
	private final QNameValue[] vars;
	public XPathVarResolveHelper(QNameValue[] vars) {
		this.vars = vars;
	}
	public Object resolveVariable(QName qName) {
		foreach (QNameValue var; vars; qName.equals(var))
			return var.value;
		return null;
	}
}

public final class NamespaceContextHelper implements NamespaceContext {
	private final NamespaceMap[] map;
	public NamespaceContextHelper(NamespaceMap[] map) {
		this.map = map;
	}
	public String getNamespaceURI(String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException();
		if (prefix.equals("xml")) return "http://www.w3.org/XML/1998/namespace";
		if (prefix.equals("xmlns")) return "http://www.w3.org/2000/xmlns/";
		if (this.map != null) {
			foreach (NamespaceMap m; this.map; m.prefix.equals(prefix))
				return m.uri;
		}
		return "";
	} 
	public String getPrefix(String namespaceURI) {
		if (namespaceURI == null)
			throw new IllegalArgumentException();
		if ("http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) return "xml";
		if ("http://www.w3.org/2000/xmlns/".equals(namespaceURI)) return "xmlns";
		if (this.map != null) {
			foreach (NamespaceMap m; this.map; m.uri.equals(namespaceURI))
				return m.prefix;
		}
		return "";
	}
	public java.util.Iterator getPrefixes(String namespaceURI) { return null; }
}
	
public final class XPathUtils {
	
	private final static XPath xPath = XPathFactory.newInstance().newXPath();

	private static XPath getXPath(NamespaceMap[] nsctx, QNameValue[] vars) {
		XPath xPath = XPathUtils.xPath;
		xPath.setNamespaceContext(new NamespaceContextHelper(nsctx));
		xPath.setXPathVariableResolver(new XPathVarResolveHelper(vars));
		return xPath;
	}

	public static Node evalNode(Node node, String expr, NamespaceMap[] nsctx, QNameValue[] args) {
		return (Node)getXPath(nsctx,args).evaluate(expr,node,XPathConstants.NODE);
	}

	@unerasable
	public static <W extends ADomNode> W evalVDomNode(Node node, String expr, NamespaceMap[] nsctx, QNameValue[] args) {
		return (W)evalNode(node,expr,nsctx,args);
	}
	
	public static NodeList evalNodeList(Node node, String expr, NamespaceMap[] nsctx, QNameValue[] args) {
		return (NodeList)getXPath(nsctx,args).evaluate(expr,node,XPathConstants.NODESET);
	}

	@unerasable
	public static <W extends ADomNode> W[] evalVDomNodeList(Node node, String expr, NamespaceMap[] nsctx, QNameValue[] args) {
		NodeList nlst = evalNodeList(node,expr,nsctx,args);
		int sz = nlst.getLength();
		W[] warr = new W[sz];
		for (int i=0; i < sz; i++)
			warr[i] = (W)nlst.item(i);
		return warr;
	}

	public static String evalText(Node node, String expr, NamespaceMap[] nsctx, QNameValue[] args) {
		return (String)getXPath(nsctx,args).evaluate(expr,node,XPathConstants.STRING);
	}

	public static String[] evalTextList(Node node, String expr, NamespaceMap[] nsctx, QNameValue[] args) {
		NodeList lst = (NodeList)getXPath(nsctx,args).evaluate(expr,node,XPathConstants.NODESET);
		String[] strs = new String[lst.getLength()];
		for (int i=0; i < strs.length; i++)
			strs[i] = lst.item(i).getNodeValue();
		return strs;
	}
	
}

