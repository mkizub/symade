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
package kiev.gui;

import java.util.Vector;

import kiev.fmt.*;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.IPopupMenuPeer;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.SpaceAttrSlot;

public abstract class NewElemEditor implements ItemEditor, IPopupMenuListener {

	static class Menu {
		String			title;
		Menu[]			menus;
		IMenuItem[]		actions;
		Menu(String title) {
			this.title = title;
			this.menus = new Menu[0];
			this.actions = new IMenuItem[0];
		}
		void append(Menu m) {
			menus = (Menu[])kiev.stdlib.Arrays.append(menus, m);
		}
		void append(IMenuItem m) {
			actions = (IMenuItem[])kiev.stdlib.Arrays.append(actions, m);
		}
	}
	
	final Editor	editor;
	int				idx;
	IPopupMenuPeer	menu;

	public NewElemEditor(Editor editor) {
		this.editor = editor;
	}

	private void addItems(Menu menu, ExpectedTypeInfo[] expected_types, ANode n, String name, Draw_ATextSyntax tstx) {
		if (expected_types == null)
			return;
		for (ExpectedTypeInfo eti: expected_types) {
			if (eti.typeinfo != null) {
				String title = eti.title;
				if (title == null)
					title = eti.typeinfo.clazz.getName();
				menu.append(new NewElemAction(title, eti.typeinfo, n, name, tstx));
			}
			else if (eti.subtypes != null && eti.subtypes.length > 0) {
				if (eti.title == null || eti.title.length() == 0) {
					addItems(menu, eti.subtypes, n, name, tstx);
				} else {
					Menu sub_menu = new Menu(eti.title);
					menu.append(sub_menu);
					addItems(sub_menu, eti.subtypes, n, name, tstx);
				}
			}
		}
	}
	
	private void makeSubMenu(ISubMenuPeer jm, Menu m) {
		for (Menu sub: m.menus) {
			while (sub.actions.length == 0 && sub.menus.length == 1)
				sub = sub.menus[0];
			if (sub.actions.length == 0 && sub.menus.length == 0)
				continue;
			makeSubMenu(jm.newSubMenu(sub.title), sub);
		}
		for (IMenuItem a: m.actions)
			jm.addItem(a);
	}
	private IPopupMenuPeer makePopupMenu(Menu m) {
		while (m.actions.length == 0 && m.menus.length == 1)
			m = m.menus[0];
		IPopupMenuPeer jp = UIManager.newPopupMenu(editor, this); //m.title;
		for (Menu sub: m.menus) {
			while (sub.actions.length == 0 && sub.menus.length == 1)
				sub = sub.menus[0];
			if (sub.actions.length == 0 && sub.menus.length == 0)
				continue;
			makeSubMenu(jp.newSubMenu(sub.title), sub);
		}
		for (IMenuItem a: m.actions)
			jp.addItem(a);
		return jp;
	}

	public void makeMenu(String title, ANode n, Draw_SyntaxAttr satt, Draw_ATextSyntax tstx) {
		Menu m = new Menu(title);
		addItems(m, satt.getExpectedTypes(), n, satt.name, tstx);
		menu = makePopupMenu(m);
		GfxDrawTermLayoutInfo cur_dtli = editor.getCur_elem().dr.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		this.menu.showAt(x, y);
		editor.startItemEditor(this);
	}

	public void makeMenu(String title, ANode n, Draw_SyntaxPlaceHolder splh, Draw_ATextSyntax tstx) {
		Menu m = new Menu(title);
		addItems(m, splh.getExpectedTypes(), n, splh.attr_name, tstx);
		this.menu = makePopupMenu(m);
		GfxDrawTermLayoutInfo cur_dtli = editor.getCur_elem().dr.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		this.menu.showAt(x, y);
		editor.startItemEditor(this);
	}

	private void makeTemplatesMenu(NewElemAction act, Vector<Draw_SyntaxNodeTemplate> templates) {
		Menu m = new Menu("Choose template");
		m.append(new NewElemAction("Empty", act.typeinfo, act.node, act.attr, (ASTNode)null));
		for (Draw_SyntaxNodeTemplate templ : templates)
			m.append(new NewElemAction(templ.name, act.typeinfo, act.node, act.attr, templ.getTemplateNode()));
		this.menu = makePopupMenu(m);
		GfxDrawTermLayoutInfo cur_dtli = editor.getCur_elem().dr.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		this.menu.showAt(x, y);
	}

	public void popupMenuCanceled() {
		if (menu != null)
			menu.remove();
		editor.stopItemEditor(true);
	}

	public void popupMenuExecuted(IMenuItem item) {
		NewElemAction sa = (NewElemAction)item;
		menu.remove();
		sa.actionPerformed();
	}

	class NewElemAction implements IMenuItem {
		private String					title;
		private kiev.stdlib.TypeInfo	typeinfo;
		private ANode					node;
		private String					attr;
		private Draw_ATextSyntax		tstx;
		private ASTNode					template;
		NewElemAction(String title, kiev.stdlib.TypeInfo typeinfo, ANode node, String attr, Draw_ATextSyntax tstx) {
			this.title = title;
			this.typeinfo = typeinfo;
			this.node = node;
			this.attr = attr;
			this.tstx = tstx;
		}
		NewElemAction(String title, kiev.stdlib.TypeInfo typeinfo, ANode node, String attr, ASTNode template) {
			this.title = title;
			this.typeinfo = typeinfo;
			this.node = node;
			this.attr = attr;
			this.template = template;
		}
		public String getText() {
			return title;
		}
		public void actionPerformed() {
			for (AttrSlot a: node.values()) {
				if (a.name == attr) {
					makeNewInstance(a);
					return;
				}
			}
			editor.stopItemEditor(true);
			return;
		}
		private void collectTemplates(Draw_ATextSyntax tstx, Vector<Draw_SyntaxNodeTemplate> templates) {
			if (tstx != null) {
				if (tstx.node_templates != null) {
					for (Draw_SyntaxNodeTemplate templ : tstx.node_templates) {
						ASTNode tnode = templ.getTemplateNode();
						if (tnode != null && typeinfo.clazz.getName().equals(tnode.getClass().getName()))
							templates.add(templ);
					}
				}
				collectTemplates(tstx.parent_syntax, templates);
			}
		}
		private void makeNewInstance(AttrSlot a) {
			try {
				Vector<Draw_SyntaxNodeTemplate> templates = new Vector<Draw_SyntaxNodeTemplate>();
				collectTemplates(tstx, templates);
				if (templates.size() > 0) {
					makeTemplatesMenu(this, templates);
					return;
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			try {
				ANode obj;
				if (template != null)
					obj = template.ncopy();
				else
					obj = (ANode)typeinfo.newInstance();
				if (a instanceof SpaceAttrSlot) {
					if (idx < 0)
						idx = 0;
					else if (idx > ((SpaceAttrSlot)a).getArray(node).length)
						idx = ((SpaceAttrSlot)a).getArray(node).length;
					((SpaceAttrSlot)a).insert(node,idx,obj);
				}
				else if (a instanceof ExtSpaceAttrSlot) {
					((ExtSpaceAttrSlot)a).add(node, obj);
				}
				else {
					((ScalarAttrSlot)a).set(node, obj);
				}
				editor.stopItemEditor(false);
			} catch (Throwable t) {
				t.printStackTrace();
				editor.stopItemEditor(true);
			}
		}
	}
}
