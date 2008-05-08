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

import org.w3c.dom.NodeList;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class WorkflowWrapper {
	
	public final static XPath xPath = XPathFactory.newInstance().newXPath();
	static {
		xPath.setNamespaceContext(new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				if ("wflow".equals(prefix)) return "map:kiev.vdom.Workflow";
				if ("wfstate".equals(prefix)) return "map:kiev.vdom.WorkflowState";
				if ("wftrans".equals(prefix)) return "map:kiev.vdom.WorkflowTransition";
				if ("wffunc".equals(prefix)) return "map:kiev.vdom.WorkflowFunction";
				return "";
			} 
			public String getPrefix(String namespaceURI) {
				if ("map:kiev.vdom.Workflow".equals(namespaceURI)) return "wflow";
				if ("map:kiev.vdom.WorkflowState".equals(namespaceURI)) return "wfstate";
				if ("map:kiev.vdom.WorkflowTransition".equals(namespaceURI)) return "wftrans";
				if ("map:kiev.vdom.WorkflowFunction".equals(namespaceURI)) return "wffunc";
				return "";
			}
			public java.util.Iterator getPrefixes(String namespaceURI) { return null; }
		});
	}
	
	public final ADomElement elem;
	
	public WorkflowWrapper(ADomElement elem) {
		this.elem = elem;
		assert (getMapURI().equals(elem.nodeNamespaceURI));
	}
	
	public abstract String getMapURI();
	
}

public final class Workflow extends WorkflowWrapper {
	
	public Workflow(ADomElement elem) {
		super(elem);
	}
	
	public String getMapURI() { "map:kiev.vdom.Workflow" }

	public WorkflowState getState(String name) {
		ADomElement el = (ADomElement)xPath.evaluate("wfstate:"+name,this.elem,XPathConstants.NODE);
		if (el == null)
			throw new RuntimeException("Workflow state '"+name+"' not found");
		return new WorkflowState(el);
	}
	

}

public final class WorkflowState extends WorkflowWrapper {
	
	private final static XPathExpression xpath_transitions = xPath.compile("*[substring-before(name(),':')='wftrans']");
	
	public WorkflowState(ADomElement elem) {
		super(elem);
	}
	
	public String getMapURI() { "map:kiev.vdom.WorkflowState" }

	public WorkflowTransition[] getTransitions() {
		NodeList lst = (NodeList)xpath_transitions.evaluate(this.elem,XPathConstants.NODESET);
		WorkflowTransition[] wtrs = new WorkflowTransition[lst.getLength()];
		for (int i=0; i < wtrs.length; i++)
			wtrs[i] = new WorkflowTransition((ADomElement)lst.item(i));
		return wtrs;
	}

}

public final class WorkflowTransition extends WorkflowWrapper {
	
	private final static XPathExpression xpath_function = xPath.compile("*[substring-before(name(),':')='wffunc']");
	private final static XPathExpression xpath_target = xPath.compile("text(child::target)");
	
	public WorkflowTransition(ADomElement elem) {
		super(elem);
	}
	
	public String getMapURI() { "map:kiev.vdom.WorkflowTransition" }

	public WorkflowFunction getFunction(String name) {
		ADomElement n = (ADomElement)xpath_function.evaluate(this.elem,XPathConstants.NODE);
		return new WorkflowFunction(n);
	}
	public String getTarget() {
		return (String)xpath_target.evaluate(this.elem,XPathConstants.STRING);
	}
}

public final class WorkflowFunction extends WorkflowWrapper {
	
	private final static XPathExpression xpath_func = xPath.compile("text(child::func)");
	private final static XPathExpression xpath_args = xPath.compile("arg/text()");

	public WorkflowFunction(ADomElement elem) {
		super(elem);
	}
	
	public String getMapURI() { "map:kiev.vdom.WorkflowFunction" }

	public String getFunc() {
		return (String)xpath_func.evaluate(this.elem,XPathConstants.STRING);
	}
	public String[] getArgs() {
		NodeList lst = (NodeList)xpath_args.evaluate(this.elem,XPathConstants.NODESET);
		String[] args = new String[lst.getLength()];
		for (int i=0; i < args.length; i++)
			args[i] = ((DomText)lst.item(i)).getData();
		return args;
	}
}


public final class WorkflowInterpreter implements Runnable {
	
	private final ADomDocument doc;
	private final Workflow wf;
	
	static class ReturnExit extends Error {
		final int exit_code;
		ReturnExit(int exit_code) {
			super("exit with code "+exit_code);
			this.exit_code = exit_code;
		}
	}
	
	public WorkflowInterpreter(org.w3c.dom.Document doc) {
		this.doc = (ADomDocument)doc;
		this.wf = new Workflow(this.doc.element);
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
		System.out.println("Executing state "+st.elem.getNodeName());
		WorkflowTransition[] transitions = st.getTransitions();
		foreach (WorkflowTransition t; transitions) {
			System.out.println("Executing transition "+t.elem.getNodeName());
			WorkflowFunction f = t.getFunction("default");
			if (f != null)
				exec(f);
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
