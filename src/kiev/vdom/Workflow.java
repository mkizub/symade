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

import java.lang.annotation.*;
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

@ThisIsANode
public final class Workflow extends ADomElement {
	
	@XPathExpr(value="wfstate:*[local-name()=$name]", nsmap={@XPathNSMap(prefix="wfstate",uri="map:kiev.vdom.WorkflowState")})
	public WorkflowState getState(String name);
}


@ThisIsANode
public final class WorkflowState extends ADomElement {
	
	@XPathExpr(value="wftrans:*",nsmap={@XPathNSMap(prefix="wftrans",uri="map:kiev.vdom.WorkflowTransition")})
	public WorkflowTransition[] getTransitions();
}

@ThisIsANode
public final class WorkflowTransition extends ADomElement {
	
	@XPathExpr(value="wffunc:*[local-name()=$name]",nsmap={@XPathNSMap(prefix="wffunc",uri="map:kiev.vdom.WorkflowFunction")})
	public WorkflowFunction getFunction(String name);
	
	@XPathExpr("target/text()")
	public String getTarget();
}

@ThisIsANode
public final class WorkflowFunction extends ADomElement {
	
	@XPathExpr("func/text()")
	public String getFunc();
	
	@XPathExpr("arg/text()")
	public String[] getArgs();
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
		this.wf = (Workflow)this.doc.getDocumentElement();
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
