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

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import kiev.fmt.*;
import kiev.gui.event.ElementEvent;
import kiev.gui.swing.Canvas;
import kiev.gui.swing.Window;
import kiev.vlang.DNode;
import kiev.vlang.FileUnit;
import kiev.vtree.*;
import kiev.gui.swing.ExprEditActions;
import kiev.gui.swing.FunctionExecutor;

/**
 * @author mkizub
 */

public class Editor extends InfoView implements KeyListener {
	
	/** Symbols used by editor */
	
	/** Current editor mode */
	protected KeyListener	item_editor;
	public boolean		insert_mode;
	/** Current x position for scrolling up/down */
	int						cur_x;
	/** Current item */
	public final CurElem	cur_elem;
	/** The object in clipboard */
	public final Clipboard	clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	
	public java.util.Stack<Transaction>		changes = new java.util.Stack<Transaction>();
	
	protected final java.util.Hashtable<InputEventInfo,String[]> keyActionMap;

	{
		//final int SHIFT = KeyEvent.SHIFT_DOWN_MASK;
		final int CTRL  = KeyEvent.CTRL_DOWN_MASK;
		final int ALT   = KeyEvent.ALT_DOWN_MASK;

		this.naviMap.put(new InputEventInfo(ALT,				KeyEvent.VK_X),				new ExprEditActions.Flatten());

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_UP),			new NavigateView.LineUp());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_DOWN),			new NavigateView.LineDn());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_PAGE_UP),		new NavigateView.PageUp());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_PAGE_DOWN),		new NavigateView.PageDn());

		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_LEFT),			new NavigateEditor.GoPrev());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_RIGHT),			new NavigateEditor.GoNext());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_UP),			new NavigateEditor.GoLineUp());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_DOWN),			new NavigateEditor.GoLineDn());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_HOME),			new NavigateEditor.GoLineHome());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_END),			new NavigateEditor.GoLineEnd());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_PAGE_UP),		new NavigateEditor.GoPageUp());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_PAGE_DOWN),		new NavigateEditor.GoPageDn());

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_Z),				new EditActions.Undo());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_C),				new EditActions.Copy());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_X),				new EditActions.Cut());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_DELETE),		new EditActions.Del());

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_F),				new FunctionExecutor.Factory());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_F),				new FunctionExecutor.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_O),				new FolderTrigger.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_N),				new NewElemHere.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_A),				new NewElemNext.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_V),				new PasteElemHere.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_B),				new PasteElemNext.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_E),				new ChooseItemEditor());
		
		this.keyActionMap = new java.util.Hashtable<InputEventInfo,String[]>();
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_E), new String[]{"kiev.gui.TextEditor$Factory",
			"kiev.gui.IntEditor$Factory","kiev.gui.EnumEditor$Factory","kiev.gui.AccessEditor$Factory","kiev.gui.ChooseItemEditor"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_O), new String[]{"kiev.gui.FolderTrigger$Factory"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_N), new String[]{"kiev.gui.NewElemHere$Factory"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_A), new String[]{"kiev.gui.NewElemNext$Factory"});
	}
	
	public Editor(Window window, Draw_ATextSyntax syntax, Canvas view_canvas) {
		super(window, syntax, view_canvas);
		this.show_placeholders = true;
		cur_elem = new CurElem();
	}
	
	public void setRoot(ANode root) {
		super.setRoot(root);
		cur_elem.set(view_root.getFirstLeaf());
		
	}
	
	public void setSyntax(Draw_ATextSyntax syntax) {
		super.setSyntax(syntax);
		cur_elem.restore();
	}

	public void formatAndPaint(boolean full) {
		if (cur_elem == null)
			return;
		if (insert_mode && view_canvas.cursor_offset < 0)
			view_canvas.cursor_offset = 0;
		else if (!insert_mode && item_editor == null && view_canvas.cursor_offset >= 0)
			view_canvas.cursor_offset = -1;
		cur_elem.restore();
		view_canvas.current = cur_elem.dr;
		view_canvas.current_node = cur_elem.node;
		if (full) {
			formatter.setWidth(view_canvas.getImgWidth());
			formatter.setShowAutoGenerated(show_auto_generated);
			formatter.setShowPlaceholders(show_placeholders);
			formatter.setHintEscapes(show_hint_escapes);
			view_canvas.setDlb_root(null);
			if (the_root != null && full) {
				formatter.format(the_root, view_root, getSyntax());
				view_root = formatter.getRootDrawable();
				view_canvas.setDlb_root(formatter.getRootDrawLayoutBlock());
			}
			cur_elem.restore();
		}
		view_canvas.repaint();
	}
	
	public ActionPoint getActionPoint(boolean next) {
		Drawable dr = cur_elem.dr;
		while (dr != null) {
			Drawable p = (Drawable)dr.parent();
			AttrSlot slot = null;
			if (p instanceof DrawNonTermList) {
				slot = ((DrawNonTermList)p).slst_attr;
				if (slot instanceof SpaceAttrSlot)
					return new ActionPoint(p,(SpaceAttrSlot)slot,((DrawNonTermList)p).getInsertIndex(dr, next));
				else if (slot instanceof ExtSpaceAttrSlot)
					return new ActionPoint(p,(ExtSpaceAttrSlot)slot,((DrawNonTermList)p).getInsertIndex(dr, next));
			}
			else if (p instanceof DrawListWrapper) {
				slot = ((DrawListWrapper)p).slst_attr;
				if (slot instanceof SpaceAttrSlot)
					return new ActionPoint(p,(SpaceAttrSlot)slot,((DrawListWrapper)p).getInsertIndex(dr, next));
				else if (slot instanceof ExtSpaceAttrSlot)
					return new ActionPoint(p,(ExtSpaceAttrSlot)slot,((DrawListWrapper)p).getInsertIndex(dr, next));
			}
			dr = p;
		}
		return null;
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	
	public void keyPressed(KeyEvent evt) {
		if (item_editor != null) {
			item_editor.keyPressed(evt);
			return;
		}
		//System.out.println(evt);
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		{
			UIActionFactory af = naviMap.get(new InputEventInfo(mask, code));
			Runnable r = (af == null) ? null : af.getAction(new UIActionViewContext(this.parent_window, this));
			if (r != null) {
				evt.consume();
				r.run();
				return;
			}
			String[] actions = keyActionMap.get(new InputEventInfo(mask, code));
			if (actions != null && cur_elem.dr != null && cur_elem.dr.syntax.funcs != null) {
				Drawable dr;
				for (Draw_SyntaxFunction f: cur_elem.dr.syntax.funcs) 
					if((dr=getFunctionTarget(f)) != null) {
					for (String act: actions) 
						if (act != null && act.equals(f.act)) {
						try {
							Class<?> c = Class.forName(f.act);
							af = (UIActionFactory)c.newInstance();
							r = af.getAction(new UIActionViewContext(this.parent_window, this, dr));
							if (r != null) {
								evt.consume();
								r.run();
								return;
							}
						} catch (Throwable t) {}
					}
				}
			}
			if (mask == 0) {
				if (!(code==KeyEvent.VK_SHIFT || code==KeyEvent.VK_ALT || code==KeyEvent.VK_ALT_GRAPH || code==KeyEvent.VK_CONTROL || code==KeyEvent.VK_CAPS_LOCK))
					java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
		}
	}
	
	public Drawable getFunctionTarget(Draw_SyntaxFunction sf) {
		Drawable dr = this.cur_elem.dr;
		if (sf.attr == null)
			return dr;
		String[] attrs = sf.attr.split("\\.");
		next_attr:
		for(String attr: attrs) {
			while (dr.parent() instanceof DrawCtrl)
				dr = (Drawable)dr.parent();
			for (Drawable d: dr.getChildren()) {
				Drawable x = checkFunctionTarget(attr, d);
				if (x != null) {
					dr = x;
					continue next_attr;
				}
			}
			return null;
		}
		return dr;
	}
	private Drawable checkFunctionTarget(String attr, Drawable dr) {
		if (dr == null)
			return null;
		Draw_SyntaxElem stx0 = dr.syntax;
		Drawable x;
		if (stx0 instanceof Draw_SyntaxAttr && attr.equals(((Draw_SyntaxAttr)stx0).name))
			return dr;
		if (stx0 instanceof Draw_SyntaxSet && ((Draw_SyntaxSet)stx0).nested_function_lookup) {
			for (Drawable d: dr.getChildren()) 
				if( (x=checkFunctionTarget(attr, d)) != null)
					return x;
		}
		if (dr instanceof DrawOptional && (x=checkFunctionTarget(attr, ((DrawOptional)dr).getArg())) != null)
			return x;
		return null;
	}

	public void startItemEditor(KeyListener item_editor) {
		assert (this.item_editor == null);
		this.item_editor = item_editor;
		changes.push(Transaction.open("Editor.java:startItemEditor"));
		view_canvas.repaint();
	}

	public void stopItemEditor(boolean revert) {
		if (item_editor == null)
			return;
		item_editor = null;
		if (revert) {
			Transaction tr = changes.pop();
			tr.close();
			tr.rollback(false);
		} else {
			changes.peek().close();
		}
		this.formatAndPaint(true);
	}
	
	public void mousePressed(MouseEvent e) {
		view_canvas.requestFocus();
		int x = e.getX();
		int y = e.getY() + view_canvas.translated_y;
		DrawTerm dr_vis = view_canvas.first_visible;
		GfxDrawTermLayoutInfo dr = dr_vis == null ? null : dr_vis.getGfxFmtInfo();
		for (; dr != null; dr = dr.getNext()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.x < x && dr.y < y && dr.x+w >= x && dr.y+h >= y) {
				break;
			}
			if (dr.dterm == view_canvas.last_visible)
				return;
		}
		if (dr == null)
			return;
		e.consume();
		cur_elem.set(dr.dterm);
		cur_x = dr.x;
		formatAndPaint(false);
	}
	
	public void mouseClicked(MouseEvent e) {
		view_canvas.requestFocus();
		int x = e.getX();
		int y = e.getY() + view_canvas.translated_y;
		DrawTerm dr_vis = view_canvas.first_visible;
		GfxDrawTermLayoutInfo dr = dr_vis == null ? null : dr_vis.getGfxFmtInfo();
		for (; dr != null; dr = dr.getNext()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.x < x && dr.y < y && dr.x+w >= x && dr.y+h >= y) {
				break;
			}
			if (dr.dterm == view_canvas.last_visible)
				return;
		}
		if (dr == null)
			return;
		if (e.getButton() == MouseEvent.BUTTON3) {
			UIActionFactory af = new FunctionExecutor.Factory();
			Runnable r = af.getAction(new UIActionViewContext(this.parent_window, this));
			if (r != null)
				r.run();
			return;
		}
	}
	
	public void goToPath(ANode[] path) {
		if (view_root == null)
			return;
		try {
			for (final ANode node: path) {
				view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if (n instanceof Drawable) {
							if (n instanceof DrawNodeTerm && ((DrawNodeTerm)n).getAttrObject() == node || ((Drawable)n).get$drnode() == node) {
								DrawTerm dr = ((DrawNodeTerm)n).getFirstLeaf();
								cur_elem.set(dr);
								cur_x = cur_elem.dr.getGfxFmtInfo().getX();
								makeCurrentVisible();
								formatAndPaint(false);
								throw new RuntimeException();
							}
						}
						return true; 
					}
				});
			}
		} catch (Throwable t) {}
	}

	void makeCurrentVisible() {
		try {
			int top_lineno = view_canvas.first_visible.getGfxFmtInfo().getLineNo();
			int bot_lineno = view_canvas.last_visible.getGfxFmtInfo().getLineNo();
			int cur_lineno = cur_elem.dr.getGfxFmtInfo().getLineNo();
			int height = bot_lineno - top_lineno;
			int first_line = view_canvas.first_line;
			
			if (top_lineno > 0 && cur_lineno <= top_lineno)
				first_line = cur_lineno -1;
			if (bot_lineno < view_canvas.num_lines && cur_lineno >= bot_lineno)
				first_line = cur_lineno - height + 1;
			view_canvas.setFirstLine(first_line);
		} catch (NullPointerException e) {}
	}

	public final class CurElem {
		public DrawTerm		dr;
		public ANode			node, oldNode;
		int				x;
		int				y;
		Drawable[]		path = Drawable.emptyArray;
	
		public void set(DrawTerm dr) {
			this.dr = dr;
			oldNode = node;
			setNode(dr == null ? null : dr.get$drnode());
			Editor.this.view_canvas.current = dr;
			Editor.this.view_canvas.current_node = node;
			if (dr != null) {
				GfxDrawTermLayoutInfo dtli = dr.getGfxFmtInfo();
				int w = dtli.getWidth();
				this.x = dtli.getX() + w / 2;
				this.y = dtli.getY();
				java.util.Vector<Drawable> v = new java.util.Vector<Drawable>();
				Drawable d = dr;
				while (d != null) {
					v.add(d);
					d = (Drawable)d.parent();
				}
				path = v.toArray(new Drawable[v.size()]);
			} else {
				path = Drawable.emptyArray;
			}
		}
		
		void restore() {
			Drawable dr = this.dr;
			Drawable root = Editor.this.view_root;
			if (root == null) {
				set(null);
				return;
			}
			if (dr == null) {
				set(root.getFirstLeaf());
				return;
			}
			if (dr.get$ctx_root() == root)
				return;
			if (path.length == 0) {
				set(root.getFirstLeaf());
				return;
			}
			Drawable last = path[path.length-1];
			Drawable bad = null;
			for (int i=path.length-1; i >= 0 && bad == null; i--) {
				if (path[i].get$ctx_root() == root && path[i].getFirstLeaf() != null)
					last = path[i];
				else
					bad = path[i];
			}
			set(last.getFirstLeaf());
			return;
		}
		void nodeUp() {
			if (node != null && node.parent() != null) {
				setNode(node.parent());
				Editor.this.view_canvas.current_node = node;
			}	
		}

		/**
		 * @param node the node to set
		 */
		private void setNode(ANode node) {
			this.node = node;
//			if (node != null && !node.equals(oldNode))
			if (node != null && node.equals(oldNode)) 
				return;
			if (oldNode != null && oldNode.equals(node)) 
				return;
			if (node == null && oldNode == null)
				return;		
			parent_window.fireElementChanged(new ElementEvent(Editor.this, ElementEvent.ELEMENT_CHANGED));	
		}
	}
	
	@Override
	public void elementChanged(ElementEvent e) {}

}

class ActionPoint {
	public final Drawable	dr;
	public final ANode		node;
	public final AttrSlot	slot;
	public final int		index;
	public final int		length;
	public ActionPoint(Drawable dr, AttrSlot slot) {
		this.dr = dr;
		this.node = dr.get$drnode();
		this.slot = slot;
		if (slot instanceof SpaceAttrSlot) {
			this.index = 0;
			this.length = ((SpaceAttrSlot)slot).getArray(node).length;
		} else {
			this.index = -1;
			this.length = -1;
		}
	}
	public ActionPoint(Drawable dr, SpaceAttrSlot slot, int idx) {
		this.dr = dr;
		this.node = dr.get$drnode();
		this.slot = slot;
		this.length = slot.getArray(node).length;
		if (idx <= 0) {
			this.index = 0;
		} else {
			if (idx >= this.length)
				this.index = this.length;
			else
				this.index = idx;
		}
	}
	public ActionPoint(Drawable dr, ExtSpaceAttrSlot slot, int idx) {
		this.dr = dr;
		this.node = dr.get$drnode();
		this.slot = slot;
		int length = 0;
		kiev.stdlib.Enumeration en = slot.iterate(node);
		while (en.hasMoreElements()) {
			en.nextElement();
			length++;
		}
		this.length = length;
		if (idx <= 0) {
			this.index = 0;
		} else {
			if (idx >= this.length)
				this.index = this.length;
			else
				this.index = idx;
		}
	}
}

class TransferableANode implements Transferable, ClipboardOwner {
	static DataFlavor transferableANodeFlavor;
	static {
		try {
			transferableANodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=kiev.vtree.ANode");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public final ANode node;
	public TransferableANode(ANode node) {
		this.node = node;
	}
    public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {
			DataFlavor.stringFlavor,
			transferableANodeFlavor
		};
	}
    public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor df: getTransferDataFlavors()) 
			if( df.equals(flavor))
				return true;
		return false;
	}
    public Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException
	{
		if (transferableANodeFlavor.equals(flavor))
			return node;
		if (DataFlavor.stringFlavor.equals(flavor))
			return String.valueOf(node);
		throw new UnsupportedFlavorException(flavor);
	}
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}
}


interface KeyHandler {
	public void process();
}

final class FolderTrigger implements Runnable {
	private final Editor editor;
	private final DrawFolded df;
	FolderTrigger(Editor editor, DrawFolded df) {
		this.editor = editor;
		this.df = df;
	}
	public void run() {
		df.setDrawFolded(!df.getDrawFolded());
		editor.formatAndPaint(true);
	}

	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Toggle folding"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			for (Drawable dr = editor.cur_elem.dr; dr != null; dr = (Drawable)dr.parent()) {
				if (dr instanceof DrawFolded)
					return new FolderTrigger(editor, (DrawFolded)dr);
			}
			return null;
		}
	}
}

final class PasteElemHere implements Runnable {
	final ANode       paste_node;
	final Editor      editor;
	final ANode       into_node;
	final AttrSlot    attr_slot;
	final ActionPoint ap;
	PasteElemHere(ANode paste_node, Editor editor, ANode into_node, AttrSlot attr_slot) {
		this.paste_node = paste_node;
		this.editor = editor;
		this.into_node = into_node;
		this.attr_slot = attr_slot;
		this.ap = null;
	}
	PasteElemHere(ANode paste_node, Editor editor, ActionPoint ap) {
		this.paste_node = paste_node;
		this.editor = editor;
		this.into_node = null;
		this.attr_slot = null;
		this.ap = ap;
	}
	public void run() {
		ANode paste_node = this.paste_node;
		editor.changes.push(Transaction.open("Editor.java:PasteElemHere"));
		try {
			if (paste_node.isAttached())
				paste_node = paste_node.ncopy();
			if (attr_slot != null) {
				if (attr_slot instanceof SpaceAttrSlot)
					((SpaceAttrSlot)attr_slot).insert(into_node, 0, paste_node);
				else if (attr_slot instanceof ExtSpaceAttrSlot)
					((ExtSpaceAttrSlot)attr_slot).add(into_node, paste_node);
				else if (attr_slot instanceof ScalarAttrSlot)
					((ScalarAttrSlot)attr_slot).set(into_node, paste_node);
			}
			else if (ap != null)
				((SpaceAttrSlot)ap.slot).insert(ap.node,ap.index,paste_node);
		} finally {
			editor.changes.peek().close();
		}
		editor.formatAndPaint(true);
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Paste an element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			Transferable content = editor.clipboard.getContents(null);
			if (!content.isDataFlavorSupported(TransferableANode.transferableANodeFlavor))
				return null;
			ANode node = null;
			try {
				node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			// try paste as a node into null
			if (dr instanceof DrawNodeTerm) {
				DrawNodeTerm dt = (DrawNodeTerm)dr;
				ScalarPtr pattr = dt.getScalarPtr();
				if (pattr.get() == null && pattr.slot.typeinfo.$instanceof(node))
					return new PasteElemHere(node, editor, pattr.node, pattr.slot);
			}
			// try paste as a node into placeholder
			if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
				Draw_SyntaxPlaceHolder dsph = (Draw_SyntaxPlaceHolder)dr.syntax;
				ANode drnode = dr.get$drnode();
				for (AttrSlot attr: drnode.values()) {
					if (attr.name != dsph.attr_name)
						continue;
					if (attr instanceof ScalarAttrSlot && ((ScalarAttrSlot)attr).get(drnode) == null && attr.typeinfo.$instanceof(node))
						return new PasteElemHere(node, editor, drnode, attr);
					else if (attr instanceof SpaceAttrSlot && ((SpaceAttrSlot)attr).getArray(drnode).length == 0 && attr.typeinfo.$instanceof(node))
						return new PasteElemHere(node, editor, drnode, attr);
				}
			}
			// try paste as an element of list
			ActionPoint ap = editor.getActionPoint(false);
			if (ap != null && ap.length >= 0 && ap.slot.typeinfo.$instanceof(node)) {
				return new PasteElemHere(node, editor, ap);
			}
			return null;
		}
	}
}

final class PasteElemNext implements Runnable {
	final Editor editor;
	PasteElemNext(Editor editor) {
		this.editor = editor;
	}
	public void run() {
		Transferable content = editor.clipboard.getContents(null);
		ANode node = null;
		try {
			node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ActionPoint ap = editor.getActionPoint(true);
		editor.changes.push(Transaction.open("Editor.java:PasteElemNext"));
		try {
			if (node.isAttached())
				node = node.ncopy();
			((SpaceAttrSlot)ap.slot).insert(ap.node,ap.index,node);
		} finally {
			editor.changes.peek().close();
		}
		editor.formatAndPaint(true);
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Paste an element at next position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Transferable content = editor.clipboard.getContents(null);
			if (!content.isDataFlavorSupported(TransferableANode.transferableANodeFlavor))
				return null;
			ANode node = null;
			try {
				node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			ActionPoint ap = editor.getActionPoint(true);
			if (ap == null || ap.length < 0)
				return null;
			if (!ap.slot.typeinfo.$instanceof(node))
				return null;
			return new PasteElemNext(editor);
		}
	}
}

class TextEditor implements KeyListener, ComboBoxEditor, Runnable {
	
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
		this.editor.view_canvas.cursor_offset = edit_offset;
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.view_canvas.cursor_offset = edit_offset + dr_term.getPrefix().length();
		}
		showAutoComplete();
	}

	TextEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr) {
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
			editor.view_canvas.cursor_offset = edit_offset+prefix_offset;
		}
		if (edit_offset > text.length()) {
			edit_offset = text.length();
			editor.view_canvas.cursor_offset = edit_offset+prefix_offset;
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
				editor.view_canvas.cursor_offset = edit_offset = -1;
				editor.stopItemEditor(false);
				if (combo != null)
					editor.view_canvas.remove(combo);
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
				editor.view_canvas.cursor_offset = edit_offset = -1;
				editor.stopItemEditor(true);
				if (combo != null)
					editor.view_canvas.remove(combo);
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
		editor.view_canvas.cursor_offset = edit_offset+prefix_offset;
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
			editor.view_canvas.add(combo);
		} else {
			combo.removeAllItems();
		}
		combo.setPopupVisible(false);
		GfxDrawTermLayoutInfo dtli = dr_term.getGfxFmtInfo();
		int x = dtli.getX();
		int y = dtli.getY() - editor.view_canvas.translated_y;
		int w = dtli.getWidth();
		int h = dtli.getHeight();
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

final class IntEditor extends TextEditor {
	
	IntEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr) {
		super(editor, dr_term, pattr);
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the attribute as an integer"; }
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
			return new IntEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		this.editor.view_canvas.cursor_offset = edit_offset;
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.view_canvas.cursor_offset = edit_offset + dr_term.getPrefix().length();
		}
	}

	String getText() {
		Object o = pattr.get();
		if (o == null)
			return null;
		return String.valueOf(o);
	}
	void setText(String text) {
		pattr.set(Integer.valueOf(text));
	}
}


final class FindDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -2062602325873536210L;
	private Editor the_view;
	private ANode cur_node;
	private JTextField text;
	private JOptionPane optionPane;
	FindDialog(JFrame parent, Editor the_view) {
		super(parent,"Find",false);
		this.the_view = the_view;
		cur_node = the_view.cur_elem.node;
		this.text = new JTextField();
		JButton bnFind = new JButton("Find");
		JButton bnCancel = new JButton("Cancel");
		bnFind.setActionCommand("find");
		bnFind.addActionListener(this);
		bnCancel.setActionCommand("cancel");
		bnCancel.addActionListener(this);
		this.optionPane = new JOptionPane(
			text,
			JOptionPane.QUESTION_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION,
			null,
			new Object[]{bnFind,bnCancel},
			bnFind
		);
		setContentPane(this.optionPane);
	}
	public void actionPerformed(ActionEvent e) {
		if ("find".equals(e.getActionCommand())) {
			System.out.println("Find: "+text.getText());
			String txt = text.getText();
			if (txt != null && txt.length() > 0)
				lookup(txt);
		}
		else if ("cancel".equals(e.getActionCommand())) {
			this.dispose();
		}
	}
	private void lookup(final String txt) {
		try {
			cur_node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n.getClass().getName().indexOf(txt) >= 0)
						throw new FoundException(n);
					return true;
				}
			});
		} catch (FoundException e) {
			setNode(e.node);
		}
	}
	private void setNode(ANode n) {
		java.util.Vector<ANode> path = new java.util.Vector<ANode>();
		path.add(n);
		while (n.parent() != null) {
			n = n.parent();
			path.add(n);
			if (n instanceof FileUnit)
				break;
		}
		the_view.goToPath(path.toArray(new ANode[path.size()]));
	}
	static class FoundException extends RuntimeException {
		private static final long serialVersionUID = -3393988705244860916L;
		final ANode node;
		FoundException(ANode n) { this.node = n; }
	}
}
