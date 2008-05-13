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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class QNameValue extends QName {
	public final Object value;
	public QNameValue(String name, Object value) {
		super(name);
		this.value = value;
	}
}

public final class XPathVarResolveHelper implements XPathVariableResolver {
	private final QNameValue[] vars;
	public XPathVarResolveHelper(QNameValue... vars) {
		this.vars = vars;
	}
	public Object resolveVariable(QName qName) {
		foreach (QNameValue var; vars; qName.equals(var))
			return var.value;
		return null;
	}
}
	
@ViewOf(vcast=false, iface=false)
public abstract view WorkflowWrapper of Node {
	
	private final static XPath xPath = XPathFactory.newInstance().newXPath();
	static {
		xPath.setNamespaceContext(new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				if (prefix == null)
					throw new IllegalArgumentException();
				if (prefix.equals("xml")) return "http://www.w3.org/XML/1998/namespace";
				if (prefix.equals("xmlns")) return "http://www.w3.org/2000/xmlns/";
				if ("wflow".equals(prefix)) return "map:kiev.vdom.Workflow";
				if ("wfstate".equals(prefix)) return "map:kiev.vdom.WorkflowState";
				if ("wftrans".equals(prefix)) return "map:kiev.vdom.WorkflowTransition";
				if ("wffunc".equals(prefix)) return "map:kiev.vdom.WorkflowFunction";
				return "";
			} 
			public String getPrefix(String namespaceURI) {
				if (namespaceURI == null)
					throw new IllegalArgumentException();
				if ("http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) return "xml";
				if ("http://www.w3.org/2000/xmlns/".equals(namespaceURI)) return "xmlns";
				if ("map:kiev.vdom.Workflow".equals(namespaceURI)) return "wflow";
				if ("map:kiev.vdom.WorkflowState".equals(namespaceURI)) return "wfstate";
				if ("map:kiev.vdom.WorkflowTransition".equals(namespaceURI)) return "wftrans";
				if ("map:kiev.vdom.WorkflowFunction".equals(namespaceURI)) return "wffunc";
				return "";
			}
			public java.util.Iterator getPrefixes(String namespaceURI) { return null; }
		});
	}
	
	public static XPath getXPath(QNameValue... vars) {
		WorkflowWrapper.xPath.setXPathVariableResolver(new XPathVarResolveHelper(vars));
		return WorkflowWrapper.xPath;
	}

	public String getMapURI() { "" }

	{
		assert (this.getMapURI().equals(this.getNamespaceURI()));
	}

	public final String getNodeName();
	public final String getNamespaceURI();
	
	public final NodeList evalNodeList(String expr, QNameValue... args) {
		return (NodeList)getXPath(args).evaluate(expr,(Node)this,XPathConstants.NODESET);
	}
	public final Node evalNode(String expr, QNameValue... args) {
		return (Node)getXPath(args).evaluate(expr,(Node)this,XPathConstants.NODE);
	}
	public final String evalText(String expr, QNameValue... args) {
		return (String)getXPath(args).evaluate(expr,(Node)this,XPathConstants.STRING);
	}
	public final String[] evalTextList(String expr, QNameValue... args) {
		NodeList lst = (NodeList)getXPath(args).evaluate(expr,(Node)this,XPathConstants.NODESET);
		String[] strs = new String[lst.getLength()];
		for (int i=0; i < strs.length; i++)
			strs[i] = lst.item(i).getNodeValue();
		return strs;
	}
	
}

@ViewOf(vcast=false, iface=false)
public final view Workflow of Node extends WorkflowWrapper {
	
	public String getMapURI() { "map:kiev.vdom.Workflow" }

	public WorkflowState getState(String name) {
		WorkflowState.makeView(evalNode("wfstate:*[local-name()=$name]",new QNameValue("name",name)))
	}
}

@ViewOf(vcast=false, iface=false)
public final view WorkflowState of Node extends WorkflowWrapper {
	
	public String getMapURI() { "map:kiev.vdom.WorkflowState" }

	public WorkflowTransition[] getTransitions() {
		NodeList lst = evalNodeList("*[substring-before(name(),':')='wftrans']");
		WorkflowTransition[] wtrs = new WorkflowTransition[lst.getLength()];
		for (int i=0; i < wtrs.length; i++)
			wtrs[i] = WorkflowTransition.makeView(lst.item(i));
		return wtrs;
	}

}

@ViewOf(vcast=false, iface=false)
public final view WorkflowTransition of Node extends WorkflowWrapper {
	
	public String getMapURI() { "map:kiev.vdom.WorkflowTransition" }

	public WorkflowFunction getFunction(String name) {
		WorkflowFunction.makeView(evalNode("wffunc:*[local-name()=$name]",new QNameValue("name",name)))
	}
	public String getTarget() { evalText("target/text()") }
}

@ViewOf(vcast=false, iface=false)
public final view WorkflowFunction of Node extends WorkflowWrapper {
	
	public String getMapURI() { "map:kiev.vdom.WorkflowFunction" }

	public String getFunc() { evalText("func/text()") }
	public String[] getArgs() { evalTextList("func/text()") }
}


public final class WorkflowInterpreter implements Runnable {
	
	private final org.w3c.dom.Document doc;
	private final Workflow wf;
	
	static class ReturnExit extends Error {
		final int exit_code;
		ReturnExit(int exit_code) {
			super("exit with code "+exit_code);
			this.exit_code = exit_code;
		}
	}
	
	public WorkflowInterpreter(org.w3c.dom.Document doc) {
		this.doc = doc;
		this.wf = Workflow.makeView(this.doc.getDocumentElement());
	}
	
	public void run() {
		try {
			WorkflowState wfst = wf.getState("begin");
			if (wfst == null)
				throw new RuntimeException("Workflow state 'begin' not found");
			exec(wfst);
		} catch (ReturnExit r) {
			System.out.println("exit with code "+r.exit_code);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	private void exec(WorkflowState st) {
		System.out.println("Executing state "+st.getNodeName());
		WorkflowTransition[] transitions = st.getTransitions();
		foreach (WorkflowTransition t; transitions) {
			System.out.println("Executing transition "+t.getNodeName());
			WorkflowFunction f = t.getFunction("default");
			if (f != null)
				exec(f);
			else
				System.out.println("Workflow function 'default' not found");
			String tgt = t.getTarget();
			if (tgt != null) {
				WorkflowState next = wf.getState(tgt);
				if (next == null)
					throw new RuntimeException("Workflow state '"+tgt+"' not found");
				exec(next);
				return;
			}
		}
	}
	
	private void exec(WorkflowFunction f) {
		String name = f.getFunc();
		if ("print".equals(name)) {
			String[] args = f.getArgs();
			foreach (String a; args)
				System.out.print(a);
			System.out.println();
			return;
		}
		if ("exit".equals(name)) {
			String[] args = f.getArgs();
			int code = 0;
			if (args != null && args.length > 0)
				code = Integer.parseInt(args[0]);
			throw new ReturnExit(code);
		}
		throw new RuntimeException("Uknown function '"+name+"'");
	}
}
