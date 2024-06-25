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
package org.apache.tools.ant.lang;
import syntax kiev.Syntax;

import kiev.vdom.*;
import java.io.InputStream;

@ThisIsANode
public class AntContainer extends DNode implements ScopeOfNames, GlobalDNode, ExportXMLDump, CompilationUnit {
	@nodeAttr public AntLibRef∅ libs;
	@nodeAttr public AntAttribute∅ attributes;
	@nodeAttr public ASTNode∅ members;
	@nodeAttr public ASTNode∅ predefines;

	public String qname() {
		return this.symbol.qname();
	}

	public String exportFactory() {
		return "org.apache.tools.ant.lang.AntTextProcessor";
	}

	public rule resolveNameR(ResInfo path)
		ASTNode@ n;
	{
		path ?= this
	;
		{
			n @= members
		;	n @= libs
		;	n @= predefines
		},
		{
			path ?= n
		;	n instanceof AntNamesProvider && !path.isProcessedNS(n),
			path.addProcessedNS(n),
			((AntNamesProvider)n).resolveNameR(path)
		}
	}

	public boolean preResolveIn() {
		super.preResolveIn();
		foreach (AntAttribute attr; this.attributes) {
			if (attr.prefix != null && attr.prefix.equals("xmlns") && attr.text.toText().startsWith("antlib:")) {
				attr.detach();
				String qname = attr.text.toText().substring(7);
				AntLibRef ref = new AntLibRef(attr.name, qname);
				this.libs += ref;
				ref.preLoad();
			}
		}
		foreach (XMLElement el; members)
			AntProject.resolveXMLElement(el)
		return true;
	}

	public boolean preVerify() {
		foreach (AntAttribute attr; this.attributes)
			Kiev.reportWarning(attr,"Unknown attribute "+attr);
		foreach (XMLElement el; members)
			Kiev.reportWarning(el,"Unresolved task "+el);
		return true;
	}

}

@ThisIsANode
public class AntLibRef extends DNode implements AntNamesProvider {
	@nodeAttr public String qname;
	@nodeData public AntLib lib;
	
	public AntLibRef() {}
	public AntLibRef(String prefix, String qname) {
		this.sname = prefix;
		this.qname = qname;
	}

	public String getAntXMLName() { "xmlns" }
	public String getAntXMLPrefix() { sname }
	public String getAntXMLNameSpace() { "antlib:"+qname }

	public rule resolveNameR(ResInfo path)
	{
		path ?= this
	;	lib != null,
		lib.resolveNameR(path)
	}

	public boolean preResolveIn() {
		super.preResolveIn();
		preLoad();
		return true;
	}
	
	public void preLoad() {
		if (lib == null) {
			try {
				lib = AntLib.preLoad(Env.getRoot(), qname);
			} catch (Exception e) {
				Kiev.reportWarning(this, "Cannot load antlib "+qname);
			}
		}
	}
}

@ThisIsANode
public class AntLib extends AntContainer {

	public static AntLib preLoad(Env env, String path) {
		path = path.replace('.','·');
		AntLib al = (AntLib)env.resolveGlobalDNode(path+'·'+"antlib");
		if (al == null) {
			String resource = path.replace('·','/') + "/antlib.xml";
			InputStream inp = null;
			try {
				inp = AntLib.class.getClassLoader().getResourceAsStream(resource);
				ANode[] nodes = new AntTextProcessor().parse(inp, env);
				al = (AntLib)nodes[0];
				al.sname = "antlib";
				KievPackage pkg = env.newPackage(path);
				pkg.pkg_members += al;
				Project prj = env.proj;
				if (prj.compilationUnits.indexOf(al) < 0)
					prj.compilationUnits += al;
				Kiev.runProcessorsOn(al, true);
			} finally {
				if (inp != null) inp.close();
			}
		}
		return al;
	}

	public boolean mainResolveIn() {
		return super.mainResolveIn();
	}
	public void mainResolveOut() {
		foreach (AntAttribute attr; attributes) {
			Kiev.reportWarning("Unknown antlib attribute "+attr);
		}
		super.mainResolveOut();
	}
}

