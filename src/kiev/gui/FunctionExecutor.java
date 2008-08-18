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

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxFunction;
import kiev.fmt.Draw_SyntaxList;
import kiev.fmt.Draw_SyntaxToken;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.ChooseItemEditor;
import kiev.gui.Editor;
import kiev.gui.NewElemHere;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIManager;
import kiev.vlang.ENode;
import kiev.vtree.ANode;

public final class FunctionExecutor implements IPopupMenuListener, Runnable {

	IPopupMenuPeer						menu;
	final java.util.Vector<IMenuItem>	actions;
	final Editor						editor;

	FunctionExecutor(Editor editor) {
		this.editor = editor;
		actions = new java.util.Vector<IMenuItem>();
	}
	
	public void run() {
		menu = UIManager.newPopupMenu(editor, this);
		for (IMenuItem act: actions)
			menu.addItem(act);
		GfxDrawTermLayoutInfo cur_dtli = editor.getCur_elem().dr.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		menu.showAt(x, y);
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
			if (dr.drnode != context.node)
				return null;
			Draw_SyntaxFunction[] sfs_funcs = dr.syntax.funcs;
			FunctionExecutor fe = new FunctionExecutor(editor);
			if (sfs_funcs != null && sfs_funcs.length > 0) {
				for (Draw_SyntaxFunction sf: sfs_funcs) 
					if(sf.act != null) {
					try {
						dr = editor.getFunctionTarget(sf);
						if (dr == null)
							continue;
						if ("kiev.gui.FuncNewElemOfEmptyList".equals(sf.act)) {
							if (dr.syntax instanceof Draw_SyntaxList) {
								Draw_SyntaxList slst = (Draw_SyntaxList)dr.syntax;
								if (((Object[])dr.drnode.getVal(slst.name)).length == 0)
									fe.actions.add(fe.new NewElemAction(sf.title, dr.drnode, slst, dr.text_syntax));
							}
						}
						else if ("kiev.gui.FuncNewElemOfNull".equals(sf.act)) {
							if (dr.syntax instanceof Draw_SyntaxAttr) {
								Draw_SyntaxAttr satr = (Draw_SyntaxAttr)dr.syntax;
								if (dr.drnode.getVal(satr.name) == null)
									fe.actions.add(fe.new NewElemAction(sf.title, dr.drnode, satr, dr.text_syntax));
							}
						}
						else if ("kiev.gui.FuncChooseOperator".equals(sf.act)) {
							if (dr.syntax instanceof Draw_SyntaxToken) {
								if (dr.drnode instanceof ENode)
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
								Runnable r = af.getAction(new UIActionViewContext(editor.parent_window, null, editor, dr));
								if (r != null)
									fe.actions.add(fe.new RunFuncAction(sf.title, r));
							} catch (Throwable t) {}
						}
					} catch (Throwable t) {}
				}
			}
			for (UIActionFactory af: UIManager.getUIActions(context.ui).getAllActions()) {
				if(af.isForPopupMenu()) {
					try {
						Runnable r = af.getAction(new UIActionViewContext(editor.parent_window, null, editor, dr));
						if (r != null)
							fe.actions.add(fe.new RunFuncAction(af.getDescr(), r));
					} catch (Throwable t) {}
				}
			}
			if (fe.actions.size() > 0)
				return fe;
			return null;
		}
	}
	
	public void popupMenuCanceled() {
		menu.remove();
		editor.stopItemEditor(true);
	}

	public void popupMenuExecuted(IMenuItem item) {
		Runnable act = (Runnable)item;
		menu.remove();
		try {
			act.run();
			act = null;
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			editor.stopItemEditor(act != null);
		}
	}

	class NewElemAction implements IMenuItem, Runnable {
		private String				text;
		private ANode				node;
		private Draw_SyntaxAttr		stx;
		private Draw_ATextSyntax	tstx;
		NewElemAction(String text, ANode node, Draw_SyntaxAttr stx, Draw_ATextSyntax tstx) {
			this.text = text;
			this.node = node;
			this.stx = stx;
			this.tstx = tstx;
		}
		public String getText() {
			return text;
		}
		public void run() {
			NewElemHere neh = new NewElemHere(editor);
			neh.makeMenu(text, node, stx, tstx);
		}
	}

	class EditElemAction implements IMenuItem, Runnable {
		private String				text;
		private Drawable			dr;
		EditElemAction(String text, Drawable dr) {
			this.text = text;
			this.dr = dr;
		}
		public String getText() {
			return text;
		}
		public void run() {
			Runnable r = new ChooseItemEditor().getAction(new UIActionViewContext(editor.parent_window, null, editor, dr));
			if (r != null)
				r.run();
		}
	}

	class RunFuncAction implements IMenuItem, Runnable {
		private String				text;
		private Runnable			r;
		RunFuncAction(String text, Runnable r) {
			this.text = text;
			this.r = r;
		}
		public String getText() {
			return text;
		}
		public void run() {
			r.run();
		}
	}
}

