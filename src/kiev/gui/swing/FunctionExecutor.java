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
package kiev.gui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.TextAction;

import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxFunction;
import kiev.fmt.Draw_SyntaxList;
import kiev.fmt.Draw_SyntaxToken;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.ChooseItemEditor;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.ENode;
import kiev.vtree.ANode;

public final class FunctionExecutor implements Runnable {

	public JPopupMenu			menu;
	final java.util.Vector<TextAction>	actions;

	private final Editor editor;
	FunctionExecutor(Editor editor) {
		this.editor = editor;
		actions = new java.util.Vector<TextAction>();
	}
	
	public void run() {
		menu = new JPopupMenu();
		for (TextAction act: actions)
			menu.add(new JMenuItem(act));
		GfxDrawTermLayoutInfo cur_dtli = editor.getCur_elem().dr.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		menu.show((Component)editor.getView_canvas(), x, y);
	}

	public static Factory newFactory(){
		return new Factory();
	}

	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Popup list of functions for a current element"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			if (dr == null)
				return null;
			if (dr.get$drnode() != context.node)
				return null;
			Draw_SyntaxFunction[] sfs_funcs = dr.syntax.funcs;
			if (sfs_funcs == null || sfs_funcs.length == 0)
				return null;
			FunctionExecutor fe = new FunctionExecutor(editor);
			for (Draw_SyntaxFunction sf: sfs_funcs) 
				if(sf.act != null) {
				try {
					dr = editor.getFunctionTarget(sf);
					if (dr == null)
						continue;
					if ("kiev.gui.FuncNewElemOfEmptyList".equals(sf.act)) {
						if (dr.syntax instanceof Draw_SyntaxList) {
							Draw_SyntaxList slst = (Draw_SyntaxList)dr.syntax;
							if (((Object[])dr.get$drnode().getVal(slst.name)).length == 0)
								fe.actions.add(fe.new NewElemAction(sf.title, dr.get$drnode(), slst));
						}
					}
					else if ("kiev.gui.FuncNewElemOfNull".equals(sf.act)) {
						if (dr.syntax instanceof Draw_SyntaxAttr) {
							Draw_SyntaxAttr satr = (Draw_SyntaxAttr)dr.syntax;
							if (dr.get$drnode().getVal(satr.name) == null)
								fe.actions.add(fe.new NewElemAction(sf.title, dr.get$drnode(), satr));
						}
					}
					else if ("kiev.gui.FuncChooseOperator".equals(sf.act)) {
						if (dr.syntax instanceof Draw_SyntaxToken) {
							if (dr.get$drnode() instanceof ENode)
								fe.actions.add(fe.new EditElemAction(sf.title, dr));
						}
					}
					else if ("kiev.gui.ChooseItemEditor".equals(sf.act)) {
						if (dr.syntax instanceof Draw_SyntaxAttr) {
							//Draw_SyntaxAttr satr = (Draw_SyntaxAttr)dr.syntax;
							fe.actions.add(fe.new EditElemAction(sf.title, dr));
						}
					}
					else {
						try {
							Class<?> c = Class.forName(sf.act);
							UIActionFactory af = (UIActionFactory)c.newInstance();
							if (!af.isForPopupMenu())
								continue;
							Runnable r = af.getAction(new UIActionViewContext(editor.parent_window, editor, dr));
							if (r != null)
								fe.actions.add(fe.new RunFuncAction(sf.title, r));
						} catch (Throwable t) {}
					}
				} catch (Throwable t) {}
			}
			for (UIActionFactory af: editor.naviMap.values()) if(af.isForPopupMenu()) {
				try {
					Runnable r = af.getAction(new UIActionViewContext(editor.parent_window, editor, dr));
					if (r != null)
						fe.actions.add(fe.new RunFuncAction(af.getDescr(), r));
				} catch (Throwable t) {}
			}
			if (fe.actions.size() > 0)
				return fe;
			return null;
		}
	}
	
	class NewElemAction extends TextAction {
		private static final long serialVersionUID = -6884163900635124561L;
		private String				text;
		private ANode				node;
		private Draw_SyntaxAttr		stx;
		NewElemAction(String text, ANode node, Draw_SyntaxAttr stx) {
			super(text);
			this.text = text;
			this.node = node;
			this.stx = stx;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null) {
				((Canvas)editor.getView_canvas()).remove(menu);
				menu = null;
			}
			NewElemHere neh = new NewElemHere(editor);
			neh.makeMenu(text, node, stx);
			//neh.run();
		}
	}

	class EditElemAction extends TextAction {
		private static final long serialVersionUID = -2651552143135646233L;
		private Drawable	dr;
		EditElemAction(String text, Drawable dr) {
			super(text);
			this.dr = dr;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null) {
				((Canvas)editor.getView_canvas()).remove(menu);
				menu = null;
			}
			Runnable r = new ChooseItemEditor().getAction(new UIActionViewContext(editor.parent_window, editor, dr));
			if (r != null)
				r.run();
		}
	}

	class RunFuncAction extends TextAction {
		private static final long serialVersionUID = -1481340712427352335L;
		private Runnable	r;
		RunFuncAction(String text, Runnable r) {
			super(text);
			this.r = r;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null) {
				((Canvas)editor.getView_canvas()).remove(menu);
				menu = null;
			}
			r.run();
		}
	}
}

