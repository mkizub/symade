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

import kiev.fmt.DrawJavaAccess;
import kiev.fmt.DrawTerm;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.MetaAccess;

public class AccessEditor implements ItemEditor {
	private final Editor			editor;
	private final DrawJavaAccess	cur_elem;
	private IPopupMenuPeer			menu;
	private IMenuItem				done;
	AccessEditor(Editor editor, DrawJavaAccess cur_elem) {
		this.editor = editor;
		this.cur_elem = cur_elem;
	}
	
	public final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit access attribute"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt == null || context.node == null)
				return null;
			if (!(dt instanceof DrawJavaAccess))
				return null;
			if (dt.drnode != context.node)
				return null;
			return new AccessEditor(editor, (DrawJavaAccess)dt);
		}
	}

	public void run() {
//		menu = UIManager.newPopupMenu(editor, this);
//		editor.startItemEditor(this);
//		menu.addItem(done=new JMenuItem("Done")); done.addActionListener(this);
//		JMenuItem b;
//		ButtonGroup group = new ButtonGroup();
//		menu.addItem(b=new SetSimpleMenuItem("@public",    "public"));		group.add(b); b.addActionListener(this);
//		menu.addItem(b=new SetSimpleMenuItem("@protected", "protected"));	group.add(b); b.addActionListener(this);
//		menu.addItem(b=new SetSimpleMenuItem("@access",    ""));			group.add(b); b.addActionListener(this);
//		menu.addItem(b=new SetSimpleMenuItem("@private",   "private"));	group.add(b); b.addActionListener(this);
//		int flags = ((MetaAccess)cur_elem.drnode).getFlags();
//		menu.addItem(b=new JCheckBoxMenuItem("Access bits")); b.setSelected(flags != -1); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("public read",     1<<7, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("public write",    1<<6, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("protected read",  1<<5, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("protected write", 1<<4, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("package read",    1<<3, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("package write",   1<<2, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("private read",    1<<1, flags)); b.addActionListener(this);
//		menu.addItem(b=new SetFlagsMenuItem("private write",   1<<0, flags)); b.addActionListener(this);
//		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
//		int x = cur_dtli.getX();
//		int h = cur_dtli.getHeight();
//		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
//		menu.showAt(x, y);
	}

	public void popupMenuCanceled() {
		menu.remove();
		editor.stopItemEditor(true);
	}

	/*	
	private void setMenuVisible() {
		done.setText("Save as: "+cur_elem.buildText());
		menu.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			if (e.getSource() instanceof SetSimpleMenuItem) {
				SetSimpleMenuItem mi = (SetSimpleMenuItem)e.getSource();
				((MetaAccess)cur_elem.drnode).simple = mi.val;
				setMenuVisible();
			}
			else if (e.getSource() instanceof SetFlagsMenuItem) {
				SetFlagsMenuItem mi = (SetFlagsMenuItem)e.getSource();
				MetaAccess ma = (MetaAccess)cur_elem.drnode;
				if (mi.isSelected())
					ma.setFlags(ma.getFlags() | mi.val);
				else
					ma.setFlags(ma.getFlags() & ~mi.val);
				setMenuVisible();
			}
			else if (e.getSource() instanceof JCheckBoxMenuItem) {
				JCheckBoxMenuItem mi = (JCheckBoxMenuItem)e.getSource();
				if (mi.isSelected()) {
					int flags = ((MetaAccess)cur_elem.drnode).getFlags();
					for (SetFlagsMenuItem sf: (SetFlagsMenuItem[])menu.getSubElements()) {
						sf.setEnabled(true);
						sf.setSelected((flags & sf.val) != 0);
					}
				} else {
					for (SetFlagsMenuItem sf: (SetFlagsMenuItem[])menu.getSubElements()) {
						sf.setEnabled(false);
						sf.setSelected(false);
					}
					((MetaAccess)cur_elem.drnode).setFlags(-1);
				}
				setMenuVisible();
			}
			else {
				((Canvas)editor.getView_canvas()).remove(menu);
				editor.stopItemEditor(false);
			}
		} catch (Throwable t) {
			((Canvas)editor.getView_canvas()).remove(menu);
			editor.stopItemEditor(true);
		}
	}

	class SetSimpleMenuItem extends JRadioButtonMenuItem {
		final String val;
		SetSimpleMenuItem(String text, String val) {
			super(text);
			this.val = val;
			MetaAccess ma = (MetaAccess)cur_elem.drnode;
			setSelected((ma.simple == val));
		}
	}
	class SetFlagsMenuItem extends JCheckBoxMenuItem {
		final int val;
		SetFlagsMenuItem(String text, int val, int flags) {
			super(text);
			this.val = val;
			if (flags != -1)
				setSelected((flags & val) != 0);
			else
				setEnabled(false);
		}
	}
*/
}
