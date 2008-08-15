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
import java.io.*;
import java.util.zip.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;

/**
 * @author Maxim Kizub
 *
 */

@singleton
public final class JobLang extends LangBase {
	static {
		defaultEditorSyntaxName = "stx-fmt·syntax-for-vdom";
		defaultInfoSyntaxName = "stx-fmt·syntax-for-vdom";
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

	@XPathExpr(value="workflow/wf:Workflow", nsmap={@XPathNSMap(prefix="wf",uri=WorkflowBase.NAMESPACE_URI)})
	public WorkflowWorkflow getWorkflow();
}

@ThisIsANode(lang=JobLang, name="File")
public final class JobFile extends JobBase {
	
	@AttrXMLDumpInfo(attr=true, name="arch")
	@nodeAttr
	public String		is_archive;
	
	public JobFile() {}
	
	public JobFile(String name) {
		ADomElement name_el = new GenDomElement();
		name_el.localName = "name";
		name_el.elements += new DomText(name);
		this.elements += name_el;
	}
	
	public boolean isArchive() {
		is_archive != null && Boolean.valueOf(is_archive).booleanValue()
	}
	
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
		defaultEditorSyntaxName = "stx-fmt·syntax-for-vdom";
		defaultInfoSyntaxName = "stx-fmt·syntax-for-vdom";
	}
	public String getName() { "wf" }
	public Class[] getSuperLanguages() { superLanguages }
	public Class[] getNodeClasses() { nodeClasses }

	private static Class[] superLanguages = {};
	private static Class[] nodeClasses = {
		WorkflowBase.class,
			WorkflowWorkflow.class,
			WorkflowState.class,
			WorkflowTransition.class,
			WorkflowFunction.class,
			WorkflowParam.class
	};
}

@ThisIsANode(lang=WorkflowLang)
public abstract class WorkflowBase extends ADomElement {
	
	public static final String NAMESPACE_URI = "map:kiev.vdom.Workflow";

	public final String getNamespaceURI() { NAMESPACE_URI }

	public String getPrefix() { getCompilerLang().getName() }

	public String getLocalName() { getCompilerNodeName() }
}


@ThisIsANode(lang=WorkflowLang, name="Workflow")
public final class WorkflowWorkflow extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public String		name;
	
	@XPathExpr(value="wf:State[@name=$name]", nsmap={@XPathNSMap(prefix="wf",uri=WorkflowBase.NAMESPACE_URI)})
	public WorkflowState getState(String name);

	public String getName() { name }
}


@ThisIsANode(lang=WorkflowLang, name="State")
public final class WorkflowState extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public String		name;
	
	@XPathExpr(value="wf:Transition",nsmap={@XPathNSMap(prefix="wf",uri=WorkflowBase.NAMESPACE_URI)})
	public WorkflowTransition[] getTransitions();
	
	public String toString() {
		return this.getNodeName()+":"+name;
	}
}

@ThisIsANode(lang=WorkflowLang, name="Transition")
public final class WorkflowTransition extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true, name="succ")
	@nodeAttr
	public String		on_success;
	
	@AttrXMLDumpInfo(attr=true, name="fail")
	@nodeAttr
	public String		on_fail;
	
	@XPathExpr(value="wf:Function",nsmap={@XPathNSMap(prefix="wf",uri=WorkflowBase.NAMESPACE_URI)})
	public WorkflowFunction[] getFunctions();
	
	@XPathExpr("@succ")
	public String getSuccessState();
	@XPathExpr("@fail")
	public String getFailState();
}

@ThisIsANode(lang=WorkflowLang, name="Function")
public final class WorkflowFunction extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true, name="call")
	@nodeAttr
	public String		func;
	
	public String getFunc() { func }
	
	@XPathExpr(value="wf:Param[@name=$name]",nsmap={@XPathNSMap(prefix="wf",uri=WorkflowBase.NAMESPACE_URI)})
	public WorkflowParam getParam(String name);

	@XPathExpr(value="wf:Param",nsmap={@XPathNSMap(prefix="wf",uri=WorkflowBase.NAMESPACE_URI)})
	public WorkflowParam[] getParams();
}

@ThisIsANode(lang=WorkflowLang, name="Param")
public final class WorkflowParam extends WorkflowBase {
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public String		name;
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr
	public String		value;
	
	public String getName() { name }
	public String getValue() { value }
}


public final class WorkflowInterpreter implements Runnable {
	
	private final org.w3c.dom.Document doc;
	private final JobJob job;
	private final WorkflowWorkflow wf;
	private int dump_version = 0;
	
	static class ReturnExit extends Error {
		final int exit_code;
		ReturnExit(int exit_code) {
			super("exit with code "+exit_code);
			this.exit_code = exit_code;
		}
	}
	
	public WorkflowInterpreter(org.w3c.dom.Document doc) {
		this.doc = doc;
		this.job = (JobJob)this.doc.getDocumentElement();
		this.wf = this.job.getWorkflow();
	}
	
	public void run() {
		dumpXmlFile();
		try {
			if (this.wf == null)
				throw new RuntimeException("Workflow not found in the job");
			WorkflowState wfst = this.wf.getState("begin");
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
			boolean succ = true;
			try {
				foreach (WorkflowFunction f; funcs)
					exec(f);
			} catch (Exception e) {
				succ = false;
				System.err.println(e);
			}
			String tgt = succ ? t.getSuccessState() : t.getFailState();
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
			WorkflowParam[] params = f.getParams();
			foreach (WorkflowParam p; params)
				System.out.print(p.getValue());
			System.out.println();
			return;
		}
		if ("exit".equals(name)) {
			int code = 0; 
			WorkflowParam pcode = f.getParam("code");
			if (pcode != null) {
				String val = pcode.getValue();
				if (val != null)
					code = Integer.parseInt(val);
			}
			throw new ReturnExit(code);
		}
		if ("archive-files".equals(name)) {
			JobFile[] jfiles = this.job.getFiles();
			Vector<JobFile> vect = new Vector<JobFile>();
			foreach (JobFile jf; jfiles; !jf.isArchive())
				vect.append(jf);
			if (vect.size() > 0) {
				File tmpf = File.createTempFile("wfarch", ".zip", new File("."));
				JobFile archjf = new JobFile(tmpf.getName());
				archjf.is_archive = "true";
				ADomElement archived = new GenDomElement();
				archived.localName = "archived";
				archjf.elements += archived;
				((ADomElement)vect.elementAt(0).parent()).elements += archjf;
				ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tmpf));
				byte[] buf = new byte[2048];
				foreach (JobFile jf; vect) {
					String jfname = jf.getName();
					FileInputStream fin = new FileInputStream(jfname);
					zout.putNextEntry(new ZipEntry(jfname));
					int sz;
					while ( (sz=fin.read(buf)) > 0 )
						zout.write(buf,0,sz);
					zout.closeEntry();
					jf.detach();
					archived.elements.append(jf);
				}
				zout.close();
			}
			dumpXmlFile();
			return;
		}
		throw new RuntimeException("Unknown function '"+name+"'");
	}

	private void dumpXmlFile() throws Exception {
		// Prepare the DOM document for writing
		javax.xml.transform.Source source = new javax.xml.transform.dom.DOMSource(this.doc);
		
		// Prepare the output file
		File file = new File("dump"+this.dump_version+".xml");
		javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(file);
		
		// Write the DOM document to the file
		javax.xml.transform.Transformer xformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
		xformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
		xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		xformer.transform(source, result);
		this.dump_version += 1;
	}
	
}
