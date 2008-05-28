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

@singleton
public final class JobLang extends LangBase {
	static {
		defaultEditorSyntaxName = "stx-fmt\u001fsyntax-for-vdom";
		defaultInfoSyntaxName = "stx-fmt\u001fsyntax-for-vdom";
	}
	public String getName() { "job" }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		JobBase.class,
			JobJob.class,
			JobFile.class,
			JobProperty.class,
			JobState.class
	};
}

@ThisIsANode(lang=JobLang)
public abstract class JobBase extends ADomElement {
	
	public static final String NAMESPACE_URI = "map:kiev.vdom.Job";

	public final String getNamespaceURI() { NAMESPACE_URI }

	public String getPrefix() { getCompilerLang().getName() }

	public String getLocalName() { getCompilerNodeName() }
}

@ThisIsANode(lang=JobLang, name="Job")
public final class JobJob extends JobBase {
	
	@XPathExpr(value="files/job:File", nsmap={@XPathNSMap(prefix="job",uri=JobBase.NAMESPACE_URI)})
	public JobFile[] getFiles();

	@XPathExpr(value="job:Property", nsmap={@XPathNSMap(prefix="job",uri=JobBase.NAMESPACE_URI)})
	public JobProperty[] getProperties();

	@XPathExpr(value="job:State", nsmap={@XPathNSMap(prefix="job",uri=JobBase.NAMESPACE_URI)})
	public JobState getJobState();
}

@ThisIsANode(lang=JobLang, name="File")
public final class JobFile extends JobBase {
	
	@XPathExpr(value="name")
	public String getName();

	@XPathExpr(value="hash")
	public String getHash();

	@XPathExpr(value="archived/job:File", nsmap={@XPathNSMap(prefix="job",uri=JobBase.NAMESPACE_URI)})
	public JobFile[] getArchivedFiles();
}

@ThisIsANode(lang=JobLang, name="Property")
public final class JobProperty extends JobBase {
	
	@XPathExpr(value="name")
	public String getName();

	@XPathExpr(value="value")
	public String getValue();
}

@ThisIsANode(lang=JobLang, name="State")
public final class JobState extends JobBase {
	
	@AttrXMLDumpInfo(attr=true, name="at")
	@nodeAttr
	public String		curWorkflowState;
	
	public String getCurWorkflowState() { curWorkflowState }

	@XPathExpr(value="job:Property", nsmap={@XPathNSMap(prefix="job",uri=JobBase.NAMESPACE_URI)})
	public JobProperty[] getProperties();

	@XPathExpr(value="job:State", nsmap={@XPathNSMap(prefix="job",uri=JobBase.NAMESPACE_URI)})
	public JobState[] getSubStates();
}



@singleton
public final class WorkflowLang extends LangBase {
	static {
		defaultEditorSyntaxName = "stx-fmt\u001fsyntax-for-vdom";
		defaultInfoSyntaxName = "stx-fmt\u001fsyntax-for-vdom";
	}
	public String getName() { "wf" }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		WorkflowBase.class,
			WorkflowDocument.class,
			WorkflowState.class,
			WorkflowTransition.class,
			WorkflowFunction.class
	};
}

@ThisIsANode(lang=WorkflowLang)
public abstract class WorkflowBase extends ADomElement {
	
	public static final String WORKFLOW_NAMESPACE_URI = "map:kiev.vdom.Workflow";

	public final String getNamespaceURI() { WORKFLOW_NAMESPACE_URI }

	public String getPrefix() { getCompilerLang().getName() }

	public String getLocalName() { getCompilerNodeName() }
}


@ThisIsANode(lang=WorkflowLang, name="Document")
public final class WorkflowDocument extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public String		name;
	
	@XPathExpr(value="wf:State[@name=$name]", nsmap={@XPathNSMap(prefix="wf",uri=WORKFLOW_NAMESPACE_URI)})
	public WorkflowState getState(String name);

	public String getName() { name }
}


@ThisIsANode(lang=WorkflowLang, name="State")
public final class WorkflowState extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public String		name;
	
	@XPathExpr(value="wf:Transition",nsmap={@XPathNSMap(prefix="wf",uri=WORKFLOW_NAMESPACE_URI)})
	public WorkflowTransition[] getTransitions();
	
	public String toString() {
		return this.getNodeName()+":"+name;
	}
}

@ThisIsANode(lang=WorkflowLang, name="Transition")
public final class WorkflowTransition extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true, name="from")
	@nodeAttr
	public String		source;
	
	@AttrXMLDumpInfo(attr=true, name="to")
	@nodeAttr
	public String		target;
	
	@XPathExpr(value="wf:Function",nsmap={@XPathNSMap(prefix="wf",uri=WORKFLOW_NAMESPACE_URI)})
	public WorkflowFunction[] getFunctions();
	
	@XPathExpr("@from")
	public String getSource();
	@XPathExpr("@to")
	public String getTarget();
}

@ThisIsANode(lang=WorkflowLang, name="Function")
public final class WorkflowFunction extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true, name="call")
	@nodeAttr
	public String		func;
	
	public String getFunc() { func }
	
	@XPathExpr("arg/text()")
	public String[] getArgs();
}


public final class WorkflowInterpreter implements Runnable {
	
	private final org.w3c.dom.Document doc;
	private final WorkflowDocument wf;
	
	static class ReturnExit extends Error {
		final int exit_code;
		ReturnExit(int exit_code) {
			super("exit with code "+exit_code);
			this.exit_code = exit_code;
		}
	}
	
	public WorkflowInterpreter(org.w3c.dom.Document doc) {
		this.doc = doc;
		this.wf = (WorkflowDocument)this.doc.getDocumentElement();
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
		System.out.println("Executing state "+st);
		WorkflowTransition[] transitions = st.getTransitions();
		foreach (WorkflowTransition t; transitions) {
			System.out.println("Executing transition "+t.getNodeName());
			WorkflowFunction[] funcs = t.getFunctions();
			foreach (WorkflowFunction f; funcs) {
				exec(f);
			}
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
