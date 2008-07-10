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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;

import kiev.fmt.GfxDrawTermLayoutInfo;

import kiev.fmt.DrawIdent;
import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.DNode;
import kiev.vtree.ASTNode;
import kiev.vtree.ScalarPtr;

public class TextEditor implements ItemEditor, ComboBoxEditor, Runnable {
	
	protected final Editor		editor;
	protected final DrawTerm	dr_term;
	protected final ScalarPtr	pattr;
	protected       int			edit_offset;
	protected       boolean		in_combo;
	protected       JComboBox	combo;

	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the attribute as a text"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt == null || context.node == null)
				return null;
			if (!(dt.syntax instanceof Draw_SyntaxAttr))
				return null;
			if (dt.get$drnode() != context.node)
				return null;
			ScalarPtr pattr = dt.get$drnode().getScalarPtr(((Draw_SyntaxAttr)dt.syntax).name);
			return new TextEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		editor.getView_canvas().setCursor_offset(edit_offset);
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.getView_canvas().setCursor_offset(edit_offset + dr_term.getPrefix().length());
		}
		showAutoComplete();
	}

	public TextEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr) {
		this.editor = editor;
		this.dr_term = dr_term;
		this.pattr = pattr;
	}

	String getText() {
		return (String)pattr.get();
	}
	void setText(String text) {
		if (text != null && !text.equals(getText())) {
			if (dr_term instanceof DrawIdent)
				pattr.set(text.replace('.','\u001f'));
			else
				pattr.set(text);
			showAutoComplete();
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask != 0 && mask != KeyEvent.SHIFT_DOWN_MASK)
			return;
		evt.consume();
		String text = this.getText();
		if (text == null) { text = ""; }
		int prefix_offset = dr_term.getPrefix().length();
		if (edit_offset < 0) {
			edit_offset = 0;
			editor.getView_canvas().setCursor_offset(edit_offset+prefix_offset);
		}
		if (edit_offset > text.length()) {
			edit_offset = text.length();
			editor.getView_canvas().setCursor_offset(edit_offset+prefix_offset);
		}
		switch (code) {
		case KeyEvent.VK_DOWN:
			if (in_combo) {
				int count = combo.getItemCount();
				if (count == 0) {
					in_combo = false;
					break;
				}
				int idx = combo.getSelectedIndex();
				idx++;
				if (idx >= count)
					idx = 0;
				combo.setSelectedIndex(idx);
				break;
			}
			else if (combo != null && combo.getItemCount() > 0) {
				in_combo = true;
				if (combo.getSelectedIndex() < 0)
					combo.setSelectedIndex(0);
			}
			break;
		case KeyEvent.VK_UP:
			if (in_combo) {
				int count = combo.getItemCount();
				if (count == 0) {
					in_combo = false;
					break;
				}
				int idx = combo.getSelectedIndex();
				idx--;
				if (idx < 0)
					idx = count-1;
				combo.setSelectedIndex(idx);
				break;
			}
			else if (combo != null && combo.getItemCount() > 0) {
				in_combo = true;
				if (combo.getSelectedIndex() < 0)
					combo.setSelectedIndex(combo.getItemCount()-1);
			}
			break;
		case KeyEvent.VK_HOME:
			edit_offset = 0;
			break;
		case KeyEvent.VK_END:
			edit_offset = text.length();
			break;
		case KeyEvent.VK_LEFT:
			if (edit_offset > 0)
				edit_offset--;
			break;
		case KeyEvent.VK_RIGHT:
			if (edit_offset < text.length())
				edit_offset++;
			break;
		case KeyEvent.VK_DELETE:
			if (edit_offset < text.length()) {
				text = text.substring(0, edit_offset)+text.substring(edit_offset+1);
				this.setText(text);
			}
			break;
		case KeyEvent.VK_BACK_SPACE:
			if (edit_offset > 0) {
				edit_offset--;
				text = text.substring(0, edit_offset)+text.substring(edit_offset+1);
				this.setText(text);
			}
			break;
		case KeyEvent.VK_ENTER:
			if (in_combo) {
				in_combo = false;
				text = (String)combo.getSelectedItem();
				this.setText(text);
				edit_offset = text.length();
				combo.setPopupVisible(false);
				break;
			} else {
				edit_offset = -1;
				editor.getView_canvas().setCursor_offset(edit_offset);
				editor.stopItemEditor(false);
				if (combo != null)
					editor.getView_canvas().remove(combo);
				return;
			}
		case KeyEvent.VK_ESCAPE:
			if (in_combo) {
				in_combo = false;
				combo.setSelectedIndex(-1);
				combo.setPopupVisible(false);
				if (combo.getItemCount() > 0)
					combo.setPopupVisible(true);
				break;
			} else {
				edit_offset = -1;
				editor.getView_canvas().setCursor_offset(edit_offset); 
				editor.stopItemEditor(true);
				if (combo != null)
					editor.getView_canvas().remove(combo);
				return;
			}
		default:
			if (evt.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
				char ch = evt.getKeyChar();
				if (ch == '.' && dr_term instanceof DrawIdent)
					ch = '\u001f';
				text = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
				edit_offset++;
				this.setText(text);
			}
		}
		editor.getView_canvas().setCursor_offset(edit_offset+prefix_offset);
		editor.formatAndPaint(true);
	}

	public void addActionListener(ActionListener l) {}
	public void removeActionListener(ActionListener l) {}
	public Component getEditorComponent() { return null; }
	public Object getItem() { return pattr.get(); }
	public void selectAll() {}
	public void setItem(Object text) {
		if (text != null) {
			setText((String)text);
			editor.formatAndPaint(true);
		}
	}

	void showAutoComplete() {
		if (!(pattr.node instanceof ASTNode))
			return;
		String name = getText();
		if (name == null || name.length() == 0)
			return;
		boolean qualified = name.indexOf('\u001f') > 0;
		DNode[] decls = ((ASTNode)pattr.node).findForResolve(name,pattr.slot,false);
		if (decls == null)
			return;
		if (combo == null) {
			combo = new JComboBox();
			combo.setOpaque(false);
			combo.setEditable(true);
			combo.setEditor(this);
			combo.configureEditor(this, name);
			combo.setMaximumRowCount(10);
			combo.setPopupVisible(false);
			editor.getView_canvas().add(combo);
		} else {
			combo.removeAllItems();
		}
		combo.setPopupVisible(false);
		GfxDrawTermLayoutInfo info = dr_term.getGfxFmtInfo();
		int x = info.getX();
		int y = info.getY() - editor.getView_canvas().getTranslated_y();
		int w = info.getWidth();
		int h = info.getHeight();
		combo.setBounds(x, y, w+100, h);
		boolean popup = false;
		for (DNode dn: decls) {
			combo.addItem(qualified ? dn.get$qname().replace('\u001f','.') : dn.get$sname());
			popup = true;
		}
		if (popup) {
			if (!in_combo)
				combo.setSelectedIndex(-1);
			combo.setPopupVisible(true);
		} else {
			in_combo = false;
		}
	}
	
}

