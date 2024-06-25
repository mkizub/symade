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
public class AntAttribute extends ASTNode {
	@nodeAttr public String prefix;
	@nodeAttr public String name;
	@nodeAttr public String uri;
	@nodeAttr public AntText  text;
	
	public AntAttribute() {
		this.text = new AntText();
	}
	public AntAttribute(XMLQName name, String text) {
		this.prefix = name.prefix;
		this.name = name.local;
		this.uri = name.uri;
		this.text = new AntText(text);
	}
	public AntAttribute(String name, String text) {
		this.name = name;
		this.text = new AntText(text);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name);
		sb.append('=');
		sb.append('\'');
		sb.append(text.toText());
		sb.append('\'');
		return sb.toString();
	}
}

@ThisIsANode
public class AntText extends Text {
	
	public AntText() {}
	public AntText(String text) {
		elems += new TextElem(text);
	}
	
	public boolean mainResolveIn() {
		super.preResolveIn();
		// join texts
		for (int i=0; i < elems.length; i++) {
			if (elems[i] instanceof TextElem && (i+1) < elems.length && elems[i+1] instanceof TextElem) {
				TextElem te1 = (TextElem)elems[i];
				TextElem te2 = (TextElem)elems[i+1];
				if (te2.text == null)
					;
				else if (te1.text == null)
					te1.text = te2.text;
				else
					te1.text += te2.text;
				te2.detach();
				i -= 1;
			}
		}
		// extract macro attributes refs
		if (AntProject.getNodeMacro(this) != null) {
			for (int i=0; i < elems.length; i++) {
				if (elems[i] instanceof TextElem) {
					String text = ((TextElem)elems[i]).text;
					int s = 0;
					int e;
				next_pos_at:;
					if ( (s=text.indexOf('@', s)) < 0)
						continue;
					if (s == (text.length()-1))
						continue;
					if (text.charAt(s+1) != '{' || (e=text.indexOf('}',s)) < 0) {
						s = s + 2;
						goto next_pos_at;
					}
					// found a property inside, split into text,prop,text
					elems[i].detach();
					int j = i;
					if (s > 0)
						elems.insert(j++,new TextElem(text.substring(0,s)));
					elems.insert(j++,new AntTextMacroAttrRef(text.substring(s+2,e)));
					if (e < (text.length()-1))
						elems.insert(j++,new TextElem(text.substring(e+1)));
					i--;
				}
			}
		}
		// extract property refs 
		if (AntProject.getNodeContainer(this) != null) {
			for (int i=0; i < elems.length; i++) {
				if (elems[i] instanceof TextElem) {
					String text = ((TextElem)elems[i]).text;
					int s = 0;
					int e;
				next_pos_dollar:;
					if ( (s=text.indexOf('$', s)) < 0)
						continue;
					if (s == (text.length()-1))
						continue;
					if (text.charAt(s+1) != '{' || (e=text.indexOf('}',s)) < 0) {
						s = s + 2;
						goto next_pos_dollar;
					}
					// found a property inside, split into text,prop,text
					elems[i].detach();
					int j = i;
					if (s > 0)
						elems.insert(j++,new TextElem(text.substring(0,s)));
					elems.insert(j++,new AntTextPropRef(text.substring(s+2,e)));
					if (e < (text.length()-1))
						elems.insert(j++,new TextElem(text.substring(e+1)));
					i--;
				}
			}
		}
		return true;
	}
}

@ThisIsANode
public class AntTextPropRef extends ATextNode {
	@final @SymbolRefAutoComplete(scopes={}) @SymbolRefAutoResolve(sever=SeverError.Warning)
	@nodeAttr public AntProperty⇑ prop;
	
	public AntTextPropRef() {}
	public AntTextPropRef(String name) {
		prop.name = name;
	}
	
	public String toText() { return "${"+prop+"}"; }
}

@ThisIsANode
public class AntTextMacroAttrRef extends ATextNode {
	@final @SymbolRefAutoComplete(scopes={}) @SymbolRefAutoResolve(sever=SeverError.Warning)
	@nodeAttr public AntMacroAttribute⇑ attr;
	
	public AntTextMacroAttrRef() {}
	public AntTextMacroAttrRef(String name) {
		attr.name = name;
	}
	
	public String toText() { return "@{"+attr+"}"; }
}

