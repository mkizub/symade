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

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode
public final class WorkflowState extends ADomElement {
	
	public WorkflowTransition[] getTransitions() {
		Vector<WorkflowTransition> transitions = new Vector<WorkflowTransition>();
		foreach (WorkflowTransition t; elements)
			transitions.append(t);
		return transitions.toArray();
	}

}

@ThisIsANode
public final class WorkflowTransition extends ADomElement {
	
	public WorkflowFunction getFunction(String name) {
		foreach (WorkflowFunction f; elements; name.equals(f.getLocalName()))
			return f;
		return null;
	}
	public String getTarget() {
		foreach (ADomElement e; elements; "target".equals(e.getNodeName())) {
			foreach (DomText t; e.elements) {
				return t.getData();
			}
		}
		return null;
	}
}

@ThisIsANode
public final class WorkflowFunction extends ADomElement {
	
	public String getFunc() {
		foreach (ADomElement e; elements; "func".equals(e.getNodeName())) {
			foreach (DomText t; e.elements) {
				return t.getData();
			}
		}
		return null;
	}
	public String[] getArgs() {
		Vector<String> args = new Vector<String>();
		foreach (ADomElement e; elements; "arg".equals(e.getNodeName())) {
			foreach (DomText t; e.elements) {
				args.append(t.getData());
				break;
			}
		}
		return args.toArray();
	}
}


public final class WorkflowInterpreter implements Runnable {
	
	private final ADomDocument doc;
	
	static class ReturnExit extends Error {
		final int exit_code;
		ReturnExit(int exit_code) {
			super("exit with code "+exit_code);
			this.exit_code = exit_code;
		}
	}
	
	public WorkflowInterpreter(org.w3c.dom.Document doc) {
		this.doc = (ADomDocument)doc;
	}
	public void run() {
		try {
			WorkflowState wfst = getState("begin");
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
		WorkflowTransition[] transitions = st.getTransitions();
		foreach (WorkflowTransition t; transitions) {
			WorkflowFunction f = t.getFunction("default");
			if (f != null)
				exec(f);
			String tgt = t.getTarget();
			if (tgt != null) {
				WorkflowState next = getState(tgt);
				if (next == null)
					throw new RuntimeException("Workflow state '"+tgt+"' not found");
				exec(next);
				return;
			}
		}
	}
	
	private WorkflowState getState(String name) {
		foreach (WorkflowState st; doc.element.elements; name.equals(st.getLocalName()))
			return st;
		return null;
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
