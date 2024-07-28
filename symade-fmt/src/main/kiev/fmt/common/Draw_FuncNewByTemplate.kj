package kiev.fmt.common;

import java.util.Vector;

import kiev.gui.IMenu;
import kiev.gui.IMenuItem;
import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.INode;
import kiev.vtree.SpaceAttrSlot;
import kiev.vtree.Copier;

public final class Draw_FuncNewByTemplate extends Draw_SyntaxFunc implements Draw_FuncNewNode {
	private static final long serialVersionUID = 4454170115562457744L;

	public ExpectedTypeInfo[]	subtypes;

	final static class NewElemAction implements IMenuItem {
		private final String title;
		private final kiev.stdlib.TypeInfo typeinfo;
		private final INode node;
		private final String attr;
		private final int index;
		private final INode template;
		NewElemAction(String title, kiev.stdlib.TypeInfo typeinfo, INode node, String attr, int index, INode template) {
			this.title = title;
			this.typeinfo = typeinfo;
			this.node = node;
			this.attr = attr;
			this.index = index;
			this.template = template;
		}
		public String getText() { return title; }
		public void exec() {
			for (AttrSlot a : node.values()) {
				if (a.name == attr) {
					makeNewInstance(a);
					return;
				}
			}
		}
		private void makeNewInstance(AttrSlot a) {
			INode obj = null;
			if (template != null)
				obj = new Copier().copyFull(template);
			else
				obj = (INode)typeinfo.newInstance();
			if (a instanceof ASpaceAttrSlot) {
				int idx = index;
				if (idx < 0) idx = 0; 
				else if (idx > ((ASpaceAttrSlot)a).length(node))
					idx = ((ASpaceAttrSlot)a).length(node);
				node.insVal(a,idx,obj);
			} 
			else
				node.setVal(a, obj);
		}
	}
	private void collectTemplates(Draw_ATextSyntax tstx, String cname, Vector<Draw_SyntaxNodeTemplate> templates) {
		try {
			if (tstx != null) {
				if (tstx.node_templates != null) 
					for (Draw_SyntaxNodeTemplate templ : tstx.node_templates) {
						INode tnode = templ.getTemplateNode();
						if (tnode != null && cname.equals(tnode.getClass().getName())) 
							templates.add(templ);
					}
				collectTemplates(tstx.parent_syntax, cname, templates);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	private void addItems(Menu menu, ExpectedTypeInfo[] expected_types, INode n, int index, Draw_ATextSyntax tstx) {
		if (expected_types == null) return;
		for (ExpectedTypeInfo eti : expected_types) {
			if (eti.getTypeInfo() != null) {
				String title = eti.title;
				if (title == null) title = eti.getTypeInfo().clazz.getName();
				Vector<Draw_SyntaxNodeTemplate> templates = new Vector<Draw_SyntaxNodeTemplate>();
				collectTemplates(tstx, eti.getTypeInfo().clazz.getName(), templates);
				if (templates.size() > 0) {
					Menu sub_menu = new Menu(title);
					sub_menu.append(new NewElemAction("Empty "+title, eti.getTypeInfo(), n, attr, index, null));
					for (Draw_SyntaxNodeTemplate templ : templates)
						sub_menu.append(new NewElemAction(templ.name, eti.getTypeInfo(), n, attr, index, templ.getTemplateNode()));
					menu.append(sub_menu);
				} else {
					menu.append(new NewElemAction(title, eti.getTypeInfo(), n, attr, index, null));
				}
			}
			else if (eti.subtypes != null && eti.subtypes.length > 0) { 
				if (eti.title == null || eti.title.length() == 0) {
					addItems(menu, eti.subtypes, n, index, tstx);
				} else {
					Menu sub_menu = new Menu(eti.title);
					menu.append(sub_menu);
					addItems(sub_menu, eti.subtypes, n, index, tstx);
				}
			}
		}
	}

	/** Make menu for creating new element for the specified node
	 * in the specified attribute at position idx
	 */
	public IMenu makeMenu(INode node, int idx, Draw_ATextSyntax tstx) {
		if (subtypes == null || subtypes.length == 0)
			return null;
		Menu m = new Menu(title);
		addItems(m, subtypes, node, idx, tstx);
		return m;
	}

	public boolean checkApplicable(String attr) {
		if (subtypes == null || subtypes.length == 0)
			return false;
		if (this.attr != attr)
			return false;
		return true;
	}
		
}

