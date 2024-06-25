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
public final class AntTarget extends DNode implements ScopeOfNames {
	@nodeAttr public String value;
	@SymbolRefAutoComplete @SymbolRefAutoResolve
	@nodeAttr public AntTarget⇑∅ depends;
	@nodeAttr public AntProperty⇑ onlyif;
	@nodeAttr public AntProperty⇑ unless;
	@nodeAttr public String description;
	@nodeAttr public AntAttribute∅ attributes;
	@nodeAttr public ASTNode∅ members;

	public rule resolveNameR(ResInfo path)
		ASTNode@ n;
	{
		path ?= this
	;	n @= members,
		{
			path ?= n
		;	n instanceof AntNamesProvider,
			((AntNamesProvider)n).resolveNameR(path)
		}
	}

	public boolean preResolveIn() {
		foreach (AntAttribute attr; this.attributes; attr.name != null) {
			if (attr.name.equals("name")) {
				this.sname = attr.text.toText();
				attr.detach();
			}
			else if (attr.name.equals("description")) {
				this.description = attr.text.toText();
				attr.detach();
			}
			else if (attr.name.equals("if")) {
				this.onlyif = (AntProperty⇑)new SymbolRef<AntProperty>(attr.text.toText());
				attr.detach();
			}
			else if (attr.name.equals("unless")) {
				this.unless = (AntProperty⇑)new SymbolRef<AntProperty>(attr.text.toText());
				attr.detach();
			}
			else if (attr.name.equals("depends")) {
				foreach (String dep; attr.text.toText().split(",")) {
					dep = dep.trim();
					if (dep.length() == 0)
						continue;
					this.depends += (AntTarget⇑)new SymbolRef<AntTarget>(dep);
				}
				attr.detach();
			}
		}
		super.preResolveIn();
		return true;
	}

	public boolean mainResolveIn() {
		foreach (XMLElement el; members)
			AntProject.resolveXMLElement(el);
		if (onlyif != null && Env.needResolving(onlyif)) {
			ResInfo<AntProperty> info = new ResInfo<AntProperty>(this, onlyif.name);
			if (AntProject.resolveAntNameR(onlyif,info))
				onlyif.symbol = info.resolvedSymbol();
			else
				Kiev.reportWarning(onlyif,"Unresolved property "+onlyif);
		}
		if (unless != null && Env.needResolving(unless)) {
			ResInfo<AntProperty> info = new ResInfo<AntProperty>(this, unless.name);
			if (AntProject.resolveAntNameR(unless,info))
				unless.symbol = info.resolvedSymbol();
			else
				Kiev.reportWarning(unless,"Unresolved property "+unless);
		}
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

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "onlyif") {
			AutoCompleteResult result = new AutoCompleteResult(false);
			ResInfo<AntProperty> info = new ResInfo<AntProperty>(this, str, ResInfo.noEquals);
			foreach (AntProject.resolveAntNameR(onlyif,info)) {
				Symbol sym = info.resolvedSymbol();
				if (!result.containsData(sym))
					result.append(sym);
			}
			return result;
		}
		if (slot.name == "unless") {
			AutoCompleteResult result = new AutoCompleteResult(false);
			ResInfo<AntProperty> info = new ResInfo<AntProperty>(this, str, ResInfo.noEquals);
			foreach (AntProject.resolveAntNameR(unless,info)) {
				Symbol sym = info.resolvedSymbol();
				if (!result.containsData(sym))
					result.append(sym);
			}
			return result;
		}
		return super.resolveAutoComplete(str,slot);
	}
}

