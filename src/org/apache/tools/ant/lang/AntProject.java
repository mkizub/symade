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

@ThisIsANode
public class AntProject extends AntContainer {

	@SymbolRefAutoComplete @SymbolRefAutoResolve
	@nodeAttr public AntTarget⇑ dflt;
	@nodeAttr public String basedir;

	public boolean preResolveIn() {
		super.preResolveIn();
		foreach (AntAttribute attr; this.attributes; attr.name != null) {
			if (attr.name.equals("name")) {
				this.sname = attr.text.toText();
				attr.detach();
			}
			else if (attr.name.equals("default")) {
				this.dflt = (AntTarget⇑)new SymbolRef<AntTarget>(attr.text.toText());
				attr.detach();
			}
			else if (attr.name.equals("basedir")) {
				this.basedir = attr.text.toText();
				attr.detach();
			}
		}
		foreach (XMLElement el; members) {
			if (el.name.eq("target")) {
				AntTarget tgt = new AntTarget();
				foreach (XMLAttribute attr; el.attributes)
					tgt.attributes += new AntAttribute(attr.name, attr.text.text);
				tgt.members.addAll(el.elements.delToArray());
				el.replaceWithNode(tgt);
				Kiev.runProcessorsOn(tgt, true);
			}
		}
		return true;
	}

	public boolean mainResolveIn() {
		foreach (XMLElement el; members)
			AntProject.resolveXMLElement(el);
		super.mainResolveIn();
		return true;
	}

	public boolean preVerify() {
		foreach (AntAttribute attr; this.attributes)
			Kiev.reportWarning(attr,"Unknown attribute "+attr);
		foreach (XMLElement el; members)
			Kiev.reportWarning(el,"Unresolved task "+el);
		return true;
	}

	private static boolean hasAttr(XMLAttribute[] attrs, String name) {
			foreach (XMLAttribute attr; attrs; attr.name.eq(name))
				return true;
			return false;
	}
	private static String extractText(XMLAttribute attr) {
		attr.detach();
		return attr.text.text;
	}
	
	static void resolveXMLElement(XMLElement el) {
		if (el.name.eq("property")) {
			if (hasAttr(el.attributes, "name")) {
				if (hasAttr(el.attributes, "value")) {
					AntValueProperty p = new AntValueProperty();
					foreach (XMLAttribute attr; el.attributes) {
						if (attr.name.eq("name")) p.sname = extractText(attr);
						else if (attr.name.eq("value")) p.value = new AntText(extractText(attr));
						else p.attributes += new AntAttribute(attr.name, extractText(attr));
					}
					p.members.addAll(el.elements.delToArray());
					el.replaceWithNode(p);
					Kiev.runProcessorsOn(p, true);
					return;
				}
				if (hasAttr(el.attributes, "location")) {
					AntLocationProperty p = new AntLocationProperty();
					foreach (XMLAttribute attr; el.attributes) {
						if (attr.name.eq("name")) p.sname = extractText(attr);
						else if (attr.name.eq("location")) p.location = new AntText(extractText(attr));
						else p.attributes += new AntAttribute(attr.name, extractText(attr));
					}
					p.members.addAll(el.elements.delToArray());
					el.replaceWithNode(p);
					Kiev.runProcessorsOn(p, true);
					return;
				}
			}
		}
		AntNode an = null;
		if (el.name.prefix != null && el.name.prefix.length() > 0) {
			ResInfo<AntLibRef> info = new ResInfo<AntLibRef>(el, el.name.prefix.intern());
			if (AntProject.resolveAntNameR(el, info)) {
				AntLibRef lib = info.resolvedDNode();
				ResInfo<AntLibDef> info = new ResInfo<AntLibDef>(el, el.name.local.intern());
				if (lib.resolveNameR(info)) {
					AntLibDef td = info.resolvedDNode();
					if (td instanceof AntTypeDef) {
						an = new AntStdData();
						an.lib.symbol = lib.symbol;
						an.tdef.symbol = td.symbol;
					} else {
						an = new AntTask();
						an.lib.symbol = lib.symbol;
						an.tdef.symbol = td.symbol;
					}
				}
			}
		} else {
			ResInfo<AntLibDef> info = new ResInfo<AntLibDef>(el, el.name.local.intern());
			if (AntProject.resolveAntNameR(el, info)) {
				AntLibDef td = info.resolvedDNode();
				if (td instanceof AntTypeDef) {
					an = new AntStdData();
					an.tdef.symbol = td.symbol;
				}
				else if (td instanceof AntMacroElement) {
					an = new AntMacroData();
					an.tdef.symbol = td.symbol;
				}
				else {
					an = new AntTask();
					an.tdef.symbol = td.symbol;
				}
			}
		}
		if (an != null) {
			foreach (XMLAttribute xa; el.attributes)
				an.attributes += new AntAttribute(xa.name, xa.text.text);
			an.members.addAll(el.elements.delToArray());
			el.replaceWithNode(an);
			Kiev.runProcessorsOn(an, true);
			return;
		}
	}

	public static rule resolveAntNameR(ANode from, ResInfo path)
		ASTNode@ p;
		ParentEnumerator pe;
		AntContainer@ prj;
	{
		pe = new ParentEnumerator(from, false),
		p @= pe,
		{
			p instanceof AntTarget,
			$cut,
			{
				resolveAntNameInTargetR((AntTarget)p, path)
			;
				p.parent() instanceof AntContainer,
				((AntContainer)p.parent()).resolveNameR(path)
			}
		;
			p instanceof AntContainer,
			$cut,
			((AntContainer)p).resolveNameR(path)
		;
			p instanceof ScopeOfNames,
			((ScopeOfNames)p).resolveNameR(path)
		}
	}

	private static rule resolveAntNameInTargetR(AntTarget tgt, ResInfo path)
		AntTarget⇑@ dep;
	{
		tgt == null || path.isProcessedNS(tgt),
		$cut,
		false
	;
		path.addProcessedNS(tgt),
		tgt.resolveNameR(path)
	;
		dep @= tgt.depends,
		resolveAntNameInTargetR(dep.dnode, path)
	}

	static AntTarget getNodeTarget(ANode from) {
		for (ANode p = from; p != null; p = p.parent()) {
			if (p instanceof AntTarget)
				return (AntTarget)p;
			if (p instanceof AntProject || p instanceof AntLib)
				return null;
		}
		return null;
	}

	static AntContainer getNodeContainer(ANode from) {
		for (ANode p = from; p != null; p = p.parent()) {
			if (p instanceof AntContainer)
				return (AntContainer)p;
		}
		return null;
	}

	static AntMacroDef getNodeMacro(ANode from) {
		for (ANode p = from; p != null; p = p.parent()) {
			if (p instanceof AntMacroDef)
				return (AntMacroDef)p;
			if (p instanceof AntTarget || p instanceof AntProject || p instanceof AntLib)
				return null;
		}
		return null;
	}

}

