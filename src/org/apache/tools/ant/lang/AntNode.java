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
import kiev.gui.IMenu;
import kiev.gui.IMenuItem;
import kiev.gui.UIAction;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.fmt.common.Draw_FuncNewNode;
import kiev.fmt.common.Draw_SyntaxFunc;

import org.apache.tools.ant.taskdefs.UpToDate;

@ThisIsANode//(lang=AntLang)
public abstract class AntQName {
	@abstract
	@nodeAttr public String name;
	@abstract
	@nodeAttr public String uri;
	@abstract
	@nodeAttr public String prefix;
}

@ThisIsANode//(lang=AntLang)
public abstract class AntNode extends DNode {
	
	protected static final Hashtable<String,AntClassMapInfo> GLOBAL_ANT_NODE_MAP;
	static {
		GLOBAL_ANT_NODE_MAP = new Hashtable<String,AntClassMapInfo>();
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.Typedef", new AntClassMapInfo(AntTypeDef.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.Taskdef", new AntClassMapInfo(AntTaskDef.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.PreSetDef", new AntClassMapInfo(AntPresetDef.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.MacroDef", new AntClassMapInfo(AntMacroDef.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.MacroDef$Attribute", new AntClassMapInfo(AntMacroAttribute.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.MacroDef$TemplateElement", new AntClassMapInfo(AntMacroElement.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.MacroDef$Text", new AntClassMapInfo(AntMacroText.class));
		GLOBAL_ANT_NODE_MAP.put("org.apache.tools.ant.taskdefs.UpToDate", new AntClassMapInfo(AntUpToDate.class));
	}

	@nodeAttr public AntAttribute∅ attributes;
	@nodeAttr public ASTNode∅ members;
	
	public AntType getAntType() {
		AntJavaClass jc = this.getClass().getAnnotation(AntJavaClass.class);
		if (jc == null)
			return new AntTypeEmpty();
		return new AntTypeOfClass(jc.value());
	}
	public String getAntXMLName() {
		AntXMLQName qn = this.getClass().getAnnotation(AntXMLQName.class);
		if (qn == null)
			return "";
		return qn.value();
	}
	public String getAntXMLPrefix() {
		AntXMLQName qn = this.getClass().getAnnotation(AntXMLQName.class);
		if (qn == null || qn.prefix().length() == 0)
			return null;
		return qn.prefix();
	}
	public String getAntXMLNameSpace() {
		AntXMLQName qn = this.getClass().getAnnotation(AntXMLQName.class);
		if (qn == null || qn.uri().length() == 0)
			return null;
		return qn.uri();
	}

	// convert AntAttribute into node attributes
	public void processAttributes() {
		if (attributes.length == 0 || this.getClass().getAnnotation(AntXMLAttributes.class) == null)
			return;
		AntXMLAttributes xattrs = this.getClass().getAnnotation(AntXMLAttributes.class);
		foreach (AntAttribute a; attributes; a.prefix == null && a.name != null) {
			foreach (AntXMLAttribute xa; xattrs.value(); xa.value().equalsIgnoreCase(a.name)) {
				String aname = xa.attr();
				if (aname.length() == 0) aname = xa.value();
				foreach (ScalarAttrSlot attr; this.values(); attr.name.equals(aname)) {
					if (attr.typeinfo.clazz == AntText.class) {
						this.setVal(attr,~a.text);
					}
					else if (attr.typeinfo.clazz == String.class) {
						this.setVal(attr,a.text.toText());
					}
					else if (attr.typeinfo.clazz == Boolean.class || attr.typeinfo.clazz == Boolean.TYPE) {
						String val = a.text.toText();
						if ("true".equals(val) || "yes".equals(val) || "on".equals(val))
							this.setVal(attr,Boolean.TRUE);
						else if ("false".equals(val) || "no".equals(val) || "off".equals(val))
							this.setVal(attr,Boolean.FALSE);
						else
							continue;
					}
					else
						continue;
					a.detach();
					break;
				}
			}
		}
	}

	// convert XMLElement into AntNode
	public void processElements() {
		if (members.length == 0)
			return;
		NameAndType[] knownElems = getAntType().getElements();
		foreach (XMLElement el; members; el.name.prefix == null || el.name.prefix.length() == 0) {
			// check it's explicitely defined by 'add' method
			foreach (NameAndType nat; knownElems; nat.name.equalsIgnoreCase(el.name.local) && nat.atype instanceof AntTypeOfClass) {
				AntNode ne = null;
				try {
					String ant_clazz = ((AntTypeOfClass)nat.atype).clazz.getName();
					AntClassMapInfo map = GLOBAL_ANT_NODE_MAP.get(ant_clazz);
					if (map != null && map.ast_clazz != this.getClass())
						ne = (AntNode)map.ast_clazz.newInstance();
				} catch (Exception e) { e.printStackTrace(); }
				if (ne == null)
					ne = new AntSubData(el.name.local, nat.atype);
				foreach (XMLAttribute xa; el.attributes)
					ne.attributes += new AntAttribute(xa.name, xa.text.text);
				ne.members.addAll(el.elements.delToArray());
				el.replaceWithNode(ne);
				Kiev.runProcessorsOn(ne, true);
			}
		}
	}
	
	// convert XMLText into AntText
	public void processText() {
		if (members.length == 0)
			return;
		foreach (XMLText xt; members) {
			AntText tn = new AntText();
			String text = xt.text;
			int p = 0;
			for (int i=0; i < text.length(); i++) {
				char ch = text.charAt(i);
				String ln = null;
				if (ch == '\n')
					ln = text.substring(p,i)
				else if (ch == '\r') {
					if (i+1 < text.length() && text.charAt(i+1) == '\n')
						ln = text.substring(p,i++);
					else
						ln = text.substring(p,i);
				}
				if (ln != null) {
					if (ln.length() == 0)
						tn.elems += new TextBrk();
					else
						tn.elems += new TextLine(ln);
					p = i+1;
				}
			}
			if (p < text.length())
				tn.elems += new TextElem(text.substring(p));
			xt.replaceWithNode(tn);
			Kiev.runProcessorsOn(tn, true);
		}
	}

	public boolean preResolveIn() {
		super.preResolveIn();
		// convert AntAttribute into node attributes
		processAttributes();
		// convert XMLElement into node's known elements
		processElements();
		// convert XMLText into AntText
		processText();
		return true;
	}

	public boolean mainResolveIn() {
		super.mainResolveIn();
		// convert XMLElement into node's known elements
		foreach (XMLElement el; members)
			AntProject.resolveXMLElement(el);
		return true;
	}

	public boolean preVerify() {
		NameAndType[] knownAttrs = getAntType().getAttributes();
		for (int i=0; i < attributes.length; i++) {
			AntAttribute attr = attributes[i];
			// check known
			boolean known = false;
			foreach (NameAndType nat; knownAttrs; nat.name.equalsIgnoreCase(attr.name)) {
				known = true;
				break;
			}
			if (!known)
				Kiev.reportWarning(attr, "Unknown attribute '"+attr.name+"' in "+this);
			// check duplicated
			for (int j=i+1; j < attributes.length; j++) {
				AntAttribute a2 = attributes[j];
				if (attr.name.equals(a2.name)) {
					Kiev.reportWarning(a2, "Duplicated attribute '"+a2.name+"' in "+this);
					break;
				}
			}
		}

		boolean is_task_container = getAntType().isTaskContainer();
		NameAndType[] knownElems = getAntType().getElements();
		next_element:
		for (int i=0; i < members.length; i++) {
			ASTNode elem = members[i];
			if (is_task_container) {
				if !(elem instanceof AntTask)
					Kiev.reportWarning(elem, "Unknown element '"+elem+"' in task container "+this);
				continue;
			}
			if (elem instanceof AntSubData) {
				AntSubData sn = (AntSubData)elem;
				foreach (NameAndType nat; knownElems) {
					if (nat.name.equalsIgnoreCase(sn.name))
						continue next_element;
				}
				Kiev.reportWarning(elem, "Unknown element '"+sn.name+"' in "+this);
			}
			else if (elem instanceof AntStdData) {
				AntStdData sn = (AntStdData)elem;
				foreach (NameAndType nat; knownElems; nat.name.length() == 0) {
					if (nat.atype instanceof AntTypeOfClass) {
						try {
							Class set_clazz = ((AntTypeOfClass)nat.atype).clazz;
							Class dat_clazz = Class.forName(sn.tdef.dnode.classname);
							if (set_clazz.isAssignableFrom(dat_clazz))
								continue next_element;
						} catch (Exception e) {}
					}
				}
				Kiev.reportWarning(elem, "Unknown element '"+sn+"' in "+this);
			}
			else if (elem instanceof AntTask) {
				AntTask sn = (AntTask)elem;
				if (sn.tdef.dnode instanceof AntMacroElement)
					continue next_element;
				foreach (NameAndType nat; knownElems) {
					if (nat.name.equalsIgnoreCase(sn.tdef.name))
						continue next_element;
				}
				Kiev.reportWarning(elem, "Unknown element '"+sn+"' in "+this);
			}
			else if (elem instanceof AntMacroData) {
				AntMacroDef md = AntProject.getNodeMacro(elem);
				if (md != null) {
					foreach (AntMacroElement me; md.members; me.data == elem)
						continue next_element;
				}
				Kiev.reportWarning(elem, "Unknown macro element '"+elem+"' in "+this+" (macro: "+md+")");
			}
			else if (elem instanceof AntNode) {
				AntNode an = (AntNode)elem;
				String name = an.getAntXMLName();
				foreach (NameAndType nat; knownElems) {
					if (nat.name.equalsIgnoreCase(name))
						continue next_element;
				}
				Kiev.reportWarning(elem, "Unknown node '"+name+"' in "+this);
			}
			else if (elem instanceof Text) {
				if (!getAntType().allowsText())
					Kiev.reportWarning(elem, "Text data aren't accepted by "+this);
			}
			else {
				Kiev.reportWarning(elem, "Unknown element '"+elem+"' in "+this);
			}
		}
		return true;
	}
}

@ThisIsANode//(lang=AntLang)
public final class AntSubData extends AntNode {
	final String elname;
	final AntType atype;
	@abstract
	@nodeAttr public:ro String name;

	public AntSubData(String elname, AntType atype) {
		this.elname = elname;
		this.atype = atype;
	}

	public String getAntXMLName() { elname }
	public String getAntXMLPrefix() { null }
	public String getAntXMLNameSpace() { null }

	public AntType getAntType() {
		return atype;
	}

	public String toString() {
		return "ant element "+elname+"/"+atype;
	}

	@getter public String get$name() { elname }

	public Object copy(CopyContext cc) {
		return this.copyTo(new AntSubData(this.elname,this.atype), cc);
	}
}

@ThisIsANode//(lang=AntLang)
public final class AntMacroData extends AntNode {
	@final @SymbolRefAutoComplete @SymbolRefAutoResolve(sever=SeverError.Warning)
	@nodeAttr public AntMacroElement⇑ tdef;

	public void preResolveOut() {
		super.preResolveOut();
		AntMacroElement e = tdef.dnode;
		if (e != null)
			e.data = this;
	}
	public AntType getAntType() {
		return new AntTypeEmpty();
	}

	public String getAntXMLName() { tdef.name }
	public String getAntXMLPrefix() { null }
	public String getAntXMLNameSpace() { null }

	public String toString() {
		return "ant macro element "+tdef;
	}
}

@ThisIsANode//(lang=AntLang)
public final class AntStdData extends AntNode {
	@final @SymbolRefAutoComplete(scopes={})
	@nodeAttr public AntLibRef⇑ lib;
	@final
	@nodeAttr public AntTypeDef⇑ tdef;

	public String getAntXMLName() { tdef.name }
	public String getAntXMLPrefix() { lib.name }
	public String getAntXMLNameSpace() { return lib.dnode == null ? "antlib:?" : lib.dnode.getAntXMLNameSpace(); }

	public AntType getAntType() {
		AntTypeDef td = tdef.dnode;
		if (td == null || td.classname == null) return new AntTypeEmpty();
		return new AntTypeOfClass(td.classname);
	}

	public boolean preResolveIn() {
		if (Env.needResolving(lib))
			lib.resolveSymbol(SeverError.Warning);
		if (Env.needResolving(tdef)) {
			if (lib.name != null && lib.name != "") {
				// shell resolve in lib
				AntLibRef lr = lib.dnode;
				if (lr != null)
					tdef.resolveSymbol(SeverError.Warning, lr);
			} else {
				tdef.resolveSymbol(SeverError.Warning);
			}
		}
		return super.preResolveIn();
	}

	public void preResolveOut() {
		AntTypeDef td = tdef.dnode;
		if (td instanceof AntTypeDef && GLOBAL_ANT_NODE_MAP.get(td.classname) != null) {
			AntNode an = null;
			try {
				AntClassMapInfo map = GLOBAL_ANT_NODE_MAP.get(td.classname);
				if (map != null) {
					an = (AntNode)map.ast_clazz.newInstance();
				}
			} catch (Exception e) { e.printStackTrace(); }
			if (an != null) {
				an.attributes.addAll(this.attributes.delToArray());
				an.members.addAll(this.members.delToArray());
				this.replaceWithNode(an);
				Kiev.runProcessorsOn(an, true);
				return;
			}
		}
		super.preResolveOut();
	}

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "tdef") {
			AutoCompleteResult result = new AutoCompleteResult(false);
			ResInfo<AntTypeDef> info = new ResInfo<AntTypeDef>(tdef, str, ResInfo.noEquals);
			if (lib.name != null && lib.name != "") {
				AntLibRef lr = lib.dnode;
				if (lr == null)
					return null;
				foreach (lr.resolveNameR(info)) {
					Symbol sym = info.resolvedSymbol();
					if (!result.containsData(sym))
						result.append(sym);
				}
				return result;
			} else {
				foreach (AntProject.resolveAntNameR(tdef,info)) {
					Symbol sym = info.resolvedSymbol();
					if (!result.containsData(sym))
						result.append(sym);
				}
			}
			return result;
		}
		return super.resolveAutoComplete(str,slot);
	}

	public String toString() {
		if (lib.name != null)
			return "ant data "+lib+":"+tdef;
		else
			return "ant data "+tdef;
	}
}

@ThisIsANode//(lang=AntLang)
public final class AntTask extends AntNode {
	@final @SymbolRefAutoComplete(scopes={})
	@nodeAttr public AntLibRef⇑ lib;
	@final
	@nodeAttr public AntLibDef⇑ tdef;

	public String getAntXMLName() { tdef.name }
	public String getAntXMLPrefix() { lib.name }
	public String getAntXMLNameSpace() { return lib.dnode == null ? "antlib:?" : lib.dnode.getAntXMLNameSpace(); }

	public boolean preResolveIn() {
		if (Env.needResolving(lib))
			lib.resolveSymbol(SeverError.Warning);
		if (Env.needResolving(tdef)) {
			if (lib.name != null && lib.name != "") {
				// shell resolve in lib
				AntLibRef lr = lib.dnode;
				if (lr != null)
					tdef.resolveSymbol(SeverError.Warning, lr);
			} else {
				tdef.resolveSymbol(SeverError.Warning);
			}
		}
		return super.preResolveIn();
	}

	public void preResolveOut() {
		AntLibDef td = tdef.dnode;
		if (td instanceof AntTaskDef && GLOBAL_ANT_NODE_MAP.get(td.classname) != null) {
			AntNode an = null;
			try {
				AntClassMapInfo map = GLOBAL_ANT_NODE_MAP.get(td.classname);
				if (map != null) {
					an = (AntNode)map.ast_clazz.newInstance();
				}
			} catch (Exception e) { e.printStackTrace(); }
			if (an != null) {
				an.attributes.addAll(this.attributes.delToArray());
				an.members.addAll(this.members.delToArray());
				this.replaceWithNode(an);
				Kiev.runProcessorsOn(an, true);
				return;
			}
		}
		super.preResolveOut();
	}

	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "tdef") {
			AutoCompleteResult result = new AutoCompleteResult(false);
			ResInfo<AntLibDef> info = new ResInfo<AntLibDef>(tdef, str, ResInfo.noEquals);
			if (lib.name != null && lib.name != "") {
				AntLibRef lr = lib.dnode;
				if (lr == null)
					return null;
				foreach (lr.resolveNameR(info)) {
					if (!(info.resolvedDNode() instanceof AntTypeDef) && !result.containsData(info.resolvedSymbol()))
						result.append(info.resolvedSymbol());
				}
				return result;
			} else {
				foreach (AntProject.resolveAntNameR(tdef,info)) {
					if (!(info.resolvedDNode() instanceof AntTypeDef) && !result.containsData(info.resolvedSymbol()))
						result.append(info.resolvedSymbol());
				}
			}
			return result;
		}
		return super.resolveAutoComplete(str,slot);
	}

	public AntType getAntType() {
		AntLibDef td = tdef.dnode;
		if (td == null) return new AntTypeEmpty();
		if (td instanceof AntTaskDef)
			return new AntTypeOfClass(td.classname);
		if (td instanceof AntPresetDef) {
			AntTask task = td.getAntTask();
			if (task == null)
				return new AntTypeEmpty();
			return task.getAntType();
		}
		if (td instanceof AntMacroDef) {
			return new AntTypeOfMacro((AntMacroDef)td);
		}
		return new AntTypeEmpty();
	}

	public String toString() {
		if (lib.name != null)
			return "ant task "+lib+":"+tdef;
		else
			return "ant task "+tdef;
	}
}

public class FuncNewAntAttr implements Draw_FuncNewNode {
	
	final static class NewAttrAction implements IMenuItem {
		private final String title;
		private final String qname;
		private final AntNode node;
		private final int index;
		NewAttrAction(String title, String qname, AntNode node, int index) {
			this.title = title;
			this.qname = qname;
			this.node = node;
			this.index = index;
		}
		public String getText() { return title; }
		public void exec() {
			int idx = this.index;
			if (idx < 0)
				idx = 0;
			if (idx > node.attributes.length)
				idx = node.attributes.length;
			node.attributes.insert(idx, new AntAttribute(qname, null));
		}
	}

	public IMenu makeMenu(INode inode, int idx, Draw_ATextSyntax tstx, WorkerThreadGroup wthg) {
		if !(inode instanceof AntNode)
			return null;
		AntNode node = (AntNode)inode;
		Draw_SyntaxFunc.Menu menu = new Draw_SyntaxFunc.Menu("Add attribute");
		NameAndType[] attrs = node.getAntType().getAttributes();
		next_attr:
		foreach (NameAndType attr; attrs) {
			// check this attribute is not set yet
			foreach (AntAttribute a; node.attributes; attr.name.equalsIgnoreCase(a.name))
				continue next_attr;
			NewAttrAction naa = new NewAttrAction(attr.name, attr.name, node, idx);
			menu.append(naa);
		}
		menu.append(new NewAttrAction("raw attribute", null, node, idx));
		return menu;
	}
	
	public boolean checkApplicable(String attr) {
		return true;
	}
}

public class FuncNewAntElem implements Draw_FuncNewNode {
	
	final static class NewElemAction implements IMenuItem {
		private final String title;
		private final String qname;
		private final AntType atype;
		private final AntNode node;
		private final int index;
		NewElemAction(String title, String qname, AntType atype, AntNode node, int index) {
			this.title = title;
			this.qname = qname;
			this.atype = atype;
			this.node = node;
			this.index = index;
		}
		public String getText() { return title; }
		public void exec() {
			int idx = this.index;
			if (idx < 0)
				idx = 0;
			if (idx > node.attributes.length)
				idx = node.attributes.length;
			if (qname == null || atype == null) {
				node.members.insert(idx, new XMLElement());
				return;
			}
			if (qname.equals("text") && atype instanceof AntTypeOfClass && ((AntTypeOfClass)atype).clazz == String.class) {
				node.members.insert(idx, new Text());
				return;
			}
			node.members.insert(idx, new AntSubData(qname, atype));
		}
	}

	public IMenu makeMenu(INode inode, int idx, Draw_ATextSyntax tstx, WorkerThreadGroup wthg) {
		if !(inode instanceof AntNode)
			return null;
		AntNode node = (AntNode)inode;
		Draw_SyntaxFunc.Menu menu = new Draw_SyntaxFunc.Menu("Add element");
		NameAndType[] elems = node.getAntType().getElements();
		next_elem:
		foreach (NameAndType elem; elems) {
			// check this attribute is not set yet
			foreach (ASTNode el; node.members) {
				if (el instanceof AntSubData && elem.name.equalsIgnoreCase(el.name))
					continue next_elem;
			}
			NewElemAction naa = new NewElemAction(elem.name, elem.name, elem.atype, node, idx);
			menu.append(naa);
		}
		menu.append(new NewElemAction("raw element", null, null, node, idx));
		return menu;
	}
	
	public boolean checkApplicable(String attr) {
		return true;
	}
}


