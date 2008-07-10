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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import kiev.fmt.DrawJavaAccess;
import kiev.fmt.DrawTerm;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.MetaAccess;

public class AccessEditor 
	implements ItemEditor, PopupMenuListener, Runnable, ActionListener {
	private final Editor			editor;
	private final DrawJavaAccess	cur_elem;
	private final JPopupMenu		menu;
	private JMenuItem				done;
	AccessEditor(Editor editor, DrawJavaAccess cur_elem) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.menu = new JPopupMenu();
	}
	
	final static class Factory implements UIActionFactory {
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
			if (dt.get$drnode() != context.node)
				return null;
			return new AccessEditor(editor, (DrawJavaAccess)dt);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		menu.add(done=new JMenuItem("Done")); done.addActionListener(this);
		JMenuItem b;
		ButtonGroup group = new ButtonGroup();
		menu.add(b=new SetSimpleMenuItem("@public",    "public"));		group.add(b); b.addActionListener(this);
		menu.add(b=new SetSimpleMenuItem("@protected", "protected"));	group.add(b); b.addActionListener(this);
		menu.add(b=new SetSimpleMenuItem("@access",    ""));			group.add(b); b.addActionListener(this);
		menu.add(b=new SetSimpleMenuItem("@private",   "private"));	group.add(b); b.addActionListener(this);
		int flags = ((MetaAccess)cur_elem.get$drnode()).getFlags();
		menu.add(b=new JCheckBoxMenuItem("Access bits")); b.setSelected(flags != -1); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("public read",     1<<7, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("public write",    1<<6, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("protected read",  1<<5, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("protected write", 1<<4, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("package read",    1<<3, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("package write",   1<<2, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("private read",    1<<1, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("private write",   1<<0, flags)); b.addActionListener(this);
		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		menu.addPopupMenuListener(this);
		menu.show((Component)editor.getView_canvas(), x, y);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	
	public void popupMenuCanceled(PopupMenuEvent e) {
		editor.getView_canvas().remove(menu);
		editor.stopItemEditor(true);
	}
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

	private void setMenuVisible() {
		done.setText("Save as: "+cur_elem.buildText());
		menu.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		try {
			if (e.getSource() instanceof SetSimpleMenuItem) {
				SetSimpleMenuItem mi = (SetSimpleMenuItem)e.getSource();
				((MetaAccess)cur_elem.get$drnode()).setSimple(mi.val);
				setMenuVisible();
			}
			else if (e.getSource() instanceof SetFlagsMenuItem) {
				SetFlagsMenuItem mi = (SetFlagsMenuItem)e.getSource();
				MetaAccess ma = (MetaAccess)cur_elem.get$drnode();
				if (mi.isSelected())
					ma.setFlags(ma.getFlags() | mi.val);
				else
					ma.setFlags(ma.getFlags() & ~mi.val);
				setMenuVisible();
			}
			else if (e.getSource() instanceof JCheckBoxMenuItem) {
				JCheckBoxMenuItem mi = (JCheckBoxMenuItem)e.getSource();
				if (mi.isSelected()) {
					int flags = ((MetaAccess)cur_elem.get$drnode()).getFlags();
					for (SetFlagsMenuItem sf: (SetFlagsMenuItem[])menu.getSubElements()) {
						sf.setEnabled(true);
						sf.setSelected((flags & sf.val) != 0);
					}
				} else {
					for (SetFlagsMenuItem sf: (SetFlagsMenuItem[])menu.getSubElements()) {
						sf.setEnabled(false);
						sf.setSelected(false);
					}
					((MetaAccess)cur_elem.get$drnode()).setFlags(-1);
				}
				setMenuVisible();
			}
			else {
				editor.getView_canvas().remove(menu);
				editor.stopItemEditor(false);
			}
		} catch (Throwable t) {
			editor.getView_canvas().remove(menu);
			editor.stopItemEditor(true);
		}
	}

	class SetSimpleMenuItem extends JRadioButtonMenuItem {
		private static final long serialVersionUID = -1402816227449376371L;
		final String val;
		SetSimpleMenuItem(String text, String val) {
			super(text);
			this.val = val;
			MetaAccess ma = (MetaAccess)cur_elem.get$drnode();
			setSelected((ma.getSimple() == val));
		}
	}
	class SetFlagsMenuItem extends JCheckBoxMenuItem {
		private static final long serialVersionUID = 3217051199174588720L;
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
}
