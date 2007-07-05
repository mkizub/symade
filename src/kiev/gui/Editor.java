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

import kiev.Kiev;
import kiev.Compiler;
import kiev.CompilerThread;
import kiev.EditorThread;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import java.util.EnumSet;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.Graphics;

import java.awt.datatransfer.*;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ComboBoxEditor;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.TextAction;

/**
 * @author mkizub
 */
@node(copyable=false)
public class Editor extends InfoView implements KeyListener {
	
	/** Symbols used by editor */
	
	/** Current editor mode */
	protected KeyListener	item_editor;
	protected boolean		insert_mode;
	/** Current x position for scrolling up/down */
	int						cur_x;
	/** Current item */
	public final CurElem	cur_elem;
	/** The object in clipboard */
	public final Clipboard	clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	
	protected Stack<Transaction>		changes = new Stack<Transaction>();
	
	protected final Hashtable<InputEventInfo,String[]> keyActionMap;

	{
		final int SHIFT = KeyEvent.SHIFT_DOWN_MASK;
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

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_F),				new FunctionExecuter.Factory());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_F),				new FunctionExecuter.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_O),				new FolderTrigger.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_N),				new NewElemHere.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_A),				new NewElemNext.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_V),				new PasteElemHere.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_B),				new PasteElemNext.Factory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_E),				new ChooseItemEditor());
		
		this.keyActionMap = new Hashtable<InputEventInfo,String[]>();
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_E), new String[]{"kiev.gui.TextEditor$Factory",
			"kiev.gui.IntEditor$Factory","kiev.gui.EnumEditor$Factory","kiev.gui.AccessEditor$Factory","kiev.gui.ChooseItemEditor"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_O), new String[]{"kiev.gui.FolderTrigger$Factory"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_N), new String[]{"kiev.gui.NewElemHere$Factory"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_A), new String[]{"kiev.gui.NewElemNext$Factory"});
	}
	
	public Editor(Window window, ATextSyntax syntax, Canvas view_canvas) {
		super(window, syntax, view_canvas);
		this.show_placeholders = true;
		cur_elem = new CurElem();
	}
	
	public void setRoot(ANode root) {
		super.setRoot(root);
		cur_elem.set(view_root.getFirstLeaf());
	}
	
	public void setSyntax(ATextSyntax syntax) {
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
			this.formatter.setWidth(view_canvas.imgWidth);
			this.formatter.setShowAutoGenerated(this.show_auto_generated);
			this.formatter.setShowPlaceholders(this.show_placeholders);
			this.formatter.setHintEscapes(this.show_hint_escapes);
			view_canvas.root = null;
			if (the_root != null && full)
				view_canvas.root = view_root = formatter.format(the_root, view_root);
			cur_elem.restore();
		}
		view_canvas.repaint();
		ANode src = cur_elem.node;
		parent_window.info_view.the_root = src;
		parent_window.info_view.formatAndPaint(true);
	}
	
	public ActionPoint getActionPoint(boolean next) {
		Drawable dr = cur_elem.dr;
		while (dr != null) {
			Drawable p = (Drawable)dr.parent();
			if (p instanceof DrawNonTermList)
				return new ActionPoint(p,p.slst_attr,p.getInsertIndex(dr, next));
			if (p instanceof DrawWrapList)
				return new ActionPoint(p,p.slst_attr,p.getInsertIndex(dr, next));
			dr = p;
		}
		return null;
	}

	private boolean isSpaceOrHidden(Drawable dr) {
		return dr.isUnvisible() || isSpace(dr);
	}
	
	private boolean isSpace(Drawable dr) {
		return dr instanceof DrawSpace;
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
				foreach (SyntaxFunction f; cur_elem.dr.syntax.funcs.funcs; (dr=getFunctionTarget(f)) != null) {
					foreach (String act; actions; act != null && act.equals(f.act)) {
						try {
							Class c = Class.forName(f.act);
							UIActionFactory af = (UIActionFactory)c.newInstance();
							Runnable r = af.getAction(new UIActionViewContext(this.parent_window, this, dr));
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
				if !(code==KeyEvent.VK_SHIFT || code==KeyEvent.VK_ALT || code==KeyEvent.VK_ALT_GRAPH || code==KeyEvent.VK_CONTROL || code==KeyEvent.VK_CAPS_LOCK)
					java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			}
		}
	}
	
	public Drawable getFunctionTarget(SyntaxFunction sf) {
		Drawable dr = this.cur_elem.dr;
		if (sf.attr == null)
			return dr;
		String[] attrs = sf.attr.split("\\.");
		next_attr:
		foreach(String attr; attrs) {
			while (dr.parent() instanceof DrawCtrl)
				dr = (Drawable)dr.parent();
			if !(dr.parent() instanceof DrawNonTerm)
				return null;
			foreach (Drawable d; ((DrawNonTerm)dr.parent()).args) {
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
		SyntaxElem stx0 = dr.syntax;
		SyntaxElem stx1 = dr.attr_syntax;
		Drawable x;
		if (stx0 instanceof SyntaxAttr && attr.equals(stx0.name))
			return dr;
		if (stx1 instanceof SyntaxAttr && attr.equals(stx1.name))
			return dr;
		if (stx0 instanceof SyntaxSet && stx0.nested_function_lookup) {
			foreach (Drawable d; ((DrawNonTerm)dr).args; (x=checkFunctionTarget(attr, d)) != null)
				return x;
		}
		if (dr instanceof DrawOptional && (x=checkFunctionTarget(attr, dr.arg)) != null)
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
		DrawTerm dr = view_canvas.first_visible;
		for (; dr != null; dr = dr.getNextLeaf()) {
			if (dr.x < x && dr.y < y && dr.x+dr.w >= x && dr.y+dr.h >= y) {
				break;
			}
			if (dr == view_canvas.last_visible)
				return;
		}
		if (dr == null)
			return;
		e.consume();
		cur_elem.set(dr);
		cur_x = cur_elem.dr.x;
		formatAndPaint(false);
	}
	
	public void mouseClicked(MouseEvent e) {
		view_canvas.requestFocus();
		int x = e.getX();
		int y = e.getY() + view_canvas.translated_y;
		DrawTerm dr = view_canvas.first_visible;
		for (; dr != null; dr = dr.getNextLeaf()) {
			if (dr.x < x && dr.y < y && dr.x+dr.w >= x && dr.y+dr.h >= y) {
				break;
			}
			if (dr == view_canvas.last_visible)
				return;
		}
		if (dr == null)
			return;
		if (e.getButton() == MouseEvent.BUTTON3) {
			UIActionFactory af = new FunctionExecuter.Factory();
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
			foreach (ANode node; path) {
				view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if (n instanceof Drawable) {
							if (n instanceof DrawNodeTerm && n.getAttrPtr().get() == node || n.drnode == node) {
								DrawTerm dr = n.getFirstLeaf();
								cur_elem.set(dr);
								cur_x = cur_elem.dr.x;
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
			int top_lineno = view_canvas.first_visible.lineno;
			int bot_lineno = view_canvas.last_visible.lineno;
			int height = bot_lineno - top_lineno;
			int first_line = view_canvas.first_line;
			
			if (top_lineno > 0 && cur_elem.dr.getFirstLeaf().lineno <= top_lineno)
				first_line = cur_elem.dr.getFirstLeaf().lineno -1;
			if (bot_lineno < view_canvas.num_lines && cur_elem.dr.getFirstLeaf().lineno >= bot_lineno)
				first_line = cur_elem.dr.getFirstLeaf().lineno - height + 1;
			view_canvas.setFirstLine(first_line);
		} catch (NullPointerException e) {}
	}

	final class CurElem {
		DrawTerm		dr;
		ANode			node;
		int				x, y;
		Drawable[]		path = Drawable.emptyArray;
	
		void set(DrawTerm dr) {
			this.dr = dr;
			this.node = (dr == null ? null : dr.drnode);
			Editor.this.view_canvas.current = dr;
			Editor.this.view_canvas.current_node = node;
			if (dr != null) {
				this.x = dr.x + dr.w / 2;
				this.y = dr.y;
				Vector<Drawable> v = new Vector<Drawable>();
				while (dr != null) {
					v.append(dr);
					dr = (Drawable)dr.parent();
				}
				path = v.toArray();
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
			if (dr.ctx_root == root)
				return;
			if (path.length == 0) {
				set(root.getFirstLeaf());
				return;
			}
			Drawable last = path[path.length-1];
			Drawable bad = null;
			for (int i=path.length-1; i >= 0 && bad == null; i--) {
				if (path[i].ctx_root == root && path[i].getFirstLeaf() != null)
					last = path[i];
				else
					bad = path[i];
			}
			set(last.getFirstLeaf());
			return;
		}
		void nodeUp() {
			if (node != null && node.parent() != null) {
				node = node.parent();
				Editor.this.view_canvas.current_node = node;
			}	
		}
	}
}

public class ActionPoint {
	public final Drawable	dr;
	public final ANode		node;
	public final AttrSlot	slot;
	public final int		index, length;
	public ActionPoint(Drawable dr, AttrSlot slot) {
		this.dr = dr;
		this.node = dr.drnode;
		this.slot = slot;
		if (slot instanceof SpaceAttrSlot) {
			this.index = 0;
			this.length = ((ANode[])slot.get(node)).length;
		} else {
			this.index = -1;
			this.length = -1;
		}
	}
	public ActionPoint(Drawable dr, AttrSlot slot, int idx) {
		this.dr = dr;
		this.node = dr.drnode;
		this.slot = slot;
		this.length = ((ANode[])slot.get(node)).length;
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

public class TransferableANode implements Transferable, ClipboardOwner {
	static DataFlavor transferableANodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=kiev.vlang.ANode");
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
		foreach (DataFlavor df; getTransferDataFlavors(); df.equals(flavor))
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


public interface KeyHandler {
	public void process();
}

final class ChooseItemEditor implements UIActionFactory {

	public String getDescr() { "Edit current element" }
	public boolean isForPopupMenu() { false }
	public Runnable getAction(UIActionViewContext context) {
		if (context.editor == null)
			return null;
		Editor editor = context.editor;
		if (context.dt.drnode != context.node)
			return null;
		Drawable dr = context.dr;
		if (dr instanceof DrawNodeTerm) {
			DrawNodeTerm dt = (DrawNodeTerm)dr;
			AttrPtr pattr = dt.getAttrPtr();
			Object obj = pattr.get();
			if (obj instanceof SymbolRef)
				return new TextEditor(editor, dt, ((SymbolRef)obj).getAttrPtr("name"));
			else if (obj instanceof String || obj == null && pattr.slot.typeinfo.clazz == String.class)
				return new TextEditor(editor, dt, pattr);
			else if (obj instanceof Integer)
				return new IntEditor(editor, dt, pattr);
			else if (obj instanceof ConstIntExpr)
				return new IntEditor(editor, dt, obj.getAttrPtr("value"));
			else if (obj instanceof Boolean || Enum.class.isAssignableFrom(pattr.slot.typeinfo.clazz))
				return new EnumEditor(editor, dt, pattr);
		}
		else if (dr instanceof DrawEnumChoice) {
			DrawEnumChoice dec = (DrawEnumChoice)dr;
			SyntaxEnumChoice stx = (SyntaxEnumChoice)dec.syntax;
			DrawTerm dt = dr.getFirstLeaf();
			if (dt == null) {
				dt = editor.cur_elem.dr.getFirstLeaf();
				if (dt == null)
					dt = editor.cur_elem.dr.getNextLeaf();
			}
			return new EnumEditor(editor, dt, dec.drnode.getAttrPtr(stx.name));
		}
		else if (dr.parent() instanceof DrawEnumChoice) {
			DrawEnumChoice dec = (DrawEnumChoice)dr.parent();
			SyntaxEnumChoice stx = (SyntaxEnumChoice)dec.syntax;
			return new EnumEditor(editor, dr.getFirstLeaf(), dec.drnode.getAttrPtr(stx.name));
		}
		else if (dr instanceof DrawToken && dr.drnode instanceof ENode && ((SyntaxToken)dr.syntax).kind == SyntaxToken.TokenKind.OPERATOR) {
			return new OperatorEditor(editor, (DrawToken)dr);
		}
		return null;
	}
}

final class FolderTrigger implements Runnable {
	private final Editor editor;
	private final DrawFolded df;
	FolderTrigger(Editor editor, DrawFolded df) {
		this.editor = editor;
		this.df = df;
	}
	public void run() {
		df.draw_folded = !df.draw_folded;
		editor.formatAndPaint(true);
	}

	final static class Factory implements UIActionFactory {
		public String getDescr() { "Toggle folding" }
		public boolean isForPopupMenu() { true }
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

final class FunctionExecuter implements Runnable {

	public JPopupMenu			menu;
	final Vector<TextAction>	actions;

	private final Editor editor;
	FunctionExecuter(Editor editor) {
		this.editor = editor;
		actions = new Vector<TextAction>();
	}
	
	public void run() {
		menu = new JPopupMenu();
		foreach (TextAction act; actions)
			menu.add(new JMenuItem(act));
		int x = editor.cur_elem.dr.x;
		int y = editor.cur_elem.dr.y + editor.cur_elem.dr.h - editor.view_canvas.translated_y;
		menu.show(editor.view_canvas, x, y);
	}


	final static class Factory implements UIActionFactory {
		public String getDescr() { "Popup list of functions for a current element" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			if (dr == null)
				return null;
			if (dr.drnode != context.node)
				return null;
			SyntaxFunctions sfs = dr.syntax.funcs;
			if (sfs == null || sfs.funcs.length == 0)
				return null;
			FunctionExecuter fe = new FunctionExecuter(editor);
			foreach (SyntaxFunction sf; sfs.funcs; sf.act != null) {
				try {
					dr = editor.getFunctionTarget(sf);
					if (dr == null)
						continue;
					if ("kiev.gui.FuncNewElemOfEmptyList".equals(sf.act)) {
						if (dr.syntax instanceof SyntaxList) {
							SyntaxList slst = (SyntaxList)dr.syntax;
							if (((Object[])dr.drnode.getVal(slst.name)).length == 0)
								fe.actions.append(fe.new NewElemAction(sf.title, dr.drnode, slst));
						}
						else if (dr.attr_syntax instanceof SyntaxList) {
							SyntaxList slst = (SyntaxList)dr.attr_syntax;
							if (((Object[])dr.drnode.getVal(slst.name)).length == 0)
								fe.actions.append(fe.new NewElemAction(sf.title, dr.drnode, slst));
						}
					}
					else if ("kiev.gui.FuncNewElemOfNull".equals(sf.act)) {
						if (dr.syntax instanceof SyntaxAttr) {
							SyntaxAttr satr = (SyntaxAttr)dr.syntax;
							if (dr.drnode.getVal(satr.name) == null)
								fe.actions.append(fe.new NewElemAction(sf.title, dr.drnode, satr));
						}
						else if (dr.attr_syntax instanceof SyntaxAttr) {
							SyntaxAttr satr = (SyntaxAttr)dr.attr_syntax;
							if (dr.drnode.getVal(satr.name) == null)
								fe.actions.append(fe.new NewElemAction(sf.title, dr.drnode, satr));
						}
					}
					else if ("kiev.gui.FuncChooseOperator".equals(sf.act)) {
						if (dr.syntax instanceof SyntaxToken) {
							if (dr.drnode instanceof ENode)
								fe.actions.append(fe.new EditElemAction(sf.title, dr));
						}
					}
					else if ("kiev.gui.ChooseItemEditor".equals(sf.act)) {
						if (dr.syntax instanceof SyntaxAttr) {
							SyntaxAttr satr = (SyntaxAttr)dr.syntax;
							fe.actions.append(fe.new EditElemAction(sf.title, dr));
						}
						else if (dr.attr_syntax instanceof SyntaxAttr) {
							SyntaxAttr satr = (SyntaxAttr)dr.attr_syntax;
							fe.actions.append(fe.new EditElemAction(sf.title, dr));
						}
					}
					else {
						try {
							Class c = Class.forName(sf.act);
							UIActionFactory af = (UIActionFactory)c.newInstance();
							if (!af.isForPopupMenu())
								continue;
							Runnable r = af.getAction(new UIActionViewContext(editor.parent_window, editor, dr));
							if (r != null)
								fe.actions.append(fe.new RunFuncAction(sf.title, r));
						} catch (Throwable t) {}
					}
				} catch (Throwable t) {}
			}
			foreach (UIActionFactory af; editor.naviMap; af.isForPopupMenu()) {
				try {
					Runnable r = af.getAction(new UIActionViewContext(editor.parent_window, editor, dr));
					if (r != null)
						fe.actions.append(fe.new RunFuncAction(af.getDescr(), r));
				} catch (Throwable t) {}
			}
			if (fe.actions.size() > 0)
				return fe;
			return null;
		}
	}
	
	class NewElemAction extends TextAction {
		private String		text;
		private ANode		node;
		private SyntaxAttr	stx;
		NewElemAction(String text, ANode node, SyntaxAttr stx) {
			super(text);
			this.text = text;
			this.node = node;
			this.stx = stx;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null) {
				editor.view_canvas.remove(menu);
				menu = null;
			}
			NewElemHere neh = new NewElemHere(editor);
			neh.makeMenu(text, node, stx);
			//neh.run();
		}
	}

	class EditElemAction extends TextAction {
		private String		text;
		private Drawable	dr;
		EditElemAction(String text, Drawable dr) {
			super(text);
			this.text = text;
			this.dr = dr;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null) {
				editor.view_canvas.remove(menu);
				menu = null;
			}
			Runnable r = new ChooseItemEditor().getAction(new UIActionViewContext(editor.parent_window, editor, dr));
			if (r != null)
				r.run();
		}
	}

	class RunFuncAction extends TextAction {
		private String		text;
		private Runnable	r;
		RunFuncAction(String text, Runnable r) {
			super(text);
			this.text = text;
			this.r = r;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null) {
				editor.view_canvas.remove(menu);
				menu = null;
			}
			r.run();
		}
	}
}

abstract class NewElemEditor implements KeyListener, PopupMenuListener {

	static class Menu {
		String			title;
		Menu[]			menus;
		NewElemAction[]	actions;
		Menu(String title) {
			this.title = title;
			this.menus = new Menu[0];
			this.actions = new NewElemAction[0];
		}
	}
	
	Editor		editor;
	int			idx;
	JPopupMenu	menu;

	NewElemEditor(Editor editor) {
		this.editor = editor;
	}

	private void addItems(Menu menu, SymbolRef[] expected_types, ANode n, String name) {
		foreach (SymbolRef sr; expected_types) {
			if (sr.dnode instanceof Struct) {
				menu.actions = (NewElemAction[])Arrays.append(menu.actions, new NewElemAction((Struct)sr.dnode, n, name));
			} else if (sr.dnode instanceof SyntaxExpectedTemplate) {
				SyntaxExpectedTemplate et = (SyntaxExpectedTemplate)sr.dnode;
				if (et.title == null || et.title.length() == 0) {
					addItems(menu, et.expected_types, n, name);
				} else {
					Menu sub_menu = new Menu(et.title);
					menu.menus = (Menu[])Arrays.append(menu.menus, sub_menu);
					addItems(sub_menu, et.expected_types, n, name);
				}
			}
		}
	}
	
	private JMenu makeSubMenu(Menu m) {
		JMenu jm = new JMenu(m.title);
		foreach (Menu sub; m.menus) {
			while (sub.actions.length == 0 && sub.menus.length == 1)
				sub = sub.menus[0];
			if (sub.actions.length == 0 && sub.menus.length == 0)
				continue;
			jm.add(makeSubMenu(sub));
		}
		foreach (NewElemAction a; m.actions)
			jm.add(a);
		return jm;
	}
	private JPopupMenu makePopupMenu(Menu m) {
		while (m.actions.length == 0 && m.menus.length == 1)
			m = m.menus[0];
		JPopupMenu jp = new JPopupMenu(m.title);
		foreach (Menu sub; m.menus) {
			while (sub.actions.length == 0 && sub.menus.length == 1)
				sub = sub.menus[0];
			if (sub.actions.length == 0 && sub.menus.length == 0)
				continue;
			jp.add(makeSubMenu(sub));
		}
		foreach (NewElemAction a; m.actions)
			jp.add(a);
		return jp;
	}

	public void makeMenu(String title, ANode n, SyntaxAttr satt) {
		Menu m = new Menu(title);
		addItems(m, satt.expected_types, n, satt.name);
		this.menu = makePopupMenu(m);
		this.menu.addPopupMenuListener(this);
		int x = editor.cur_elem.dr.x;
		int y = editor.cur_elem.dr.y + editor.cur_elem.dr.h - editor.view_canvas.translated_y;
		this.menu.show(editor.view_canvas, x, y);
		editor.startItemEditor(this);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	public void popupMenuCanceled(PopupMenuEvent e) {
		if (menu != null)
			editor.view_canvas.remove(menu);
		editor.stopItemEditor(true);
	}
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

	class NewElemAction extends TextAction {
		private Struct	cls;
		private ANode	node;
		private String	attr;
		NewElemAction(Struct cls, ANode node, String attr) {
			super(cls.sname);
			this.cls = cls;
			this.node = node;
			this.attr = attr;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null)
				editor.view_canvas.remove(menu);
			foreach (AttrSlot a; node.values(); a.name == attr) {
				try {
					ANode obj = (ANode)Class.forName(cls.qname().replace('\u001f','.')).newInstance();
					obj.initForEditor();
					if (a.is_space) {
						SpaceAttrSlot<ANode> sas = (SpaceAttrSlot<ANode>)a;
						if (idx < 0)
							idx = 0;
						else if (idx > sas.get(node).length)
							idx = sas.get(node).length;
						sas.insert(node,idx,obj);
					} else {
						a.set(node, obj);
					}
				} catch (Throwable t) {
					t.printStackTrace();
					editor.stopItemEditor(true);
					a = null;
				}
				if (a != null)
					editor.stopItemEditor(false);
				return;
			}
			editor.stopItemEditor(true);
			return;
		}
	}
}

final class NewElemHere extends NewElemEditor implements Runnable {
	NewElemHere(Editor editor) { super(editor); }
	public void run() {
		Drawable dr = editor.cur_elem.dr;
		if (dr instanceof DrawPlaceHolder && ((SyntaxPlaceHolder)dr.syntax).parent() instanceof SyntaxAttr) {
			ANode n = dr.drnode;
			SyntaxAttr satt = (SyntaxAttr)((SyntaxPlaceHolder)dr.syntax).parent();
			makeMenu("Set new item", n, satt);
			return;
		}
		if (dr instanceof DrawNodeTerm && (dr.drnode == null || dr.getAttrPtr().get() == null)) {
			ANode n = dr.drnode;
			while (n == null) {
				dr = (Drawable)dr.parent();
				n = dr.drnode;
			}
			SyntaxAttr satt = (SyntaxAttr)dr.syntax;
			makeMenu("Set new item", n, satt);
			return;
		}
		ActionPoint ap = editor.getActionPoint(false);
		if (ap != null && ap.length >= 0) {
			SyntaxList slst = (SyntaxList)ap.dr.syntax;
			this.idx = ap.index;
			makeMenu("Insert new item", ap.node, slst);
			return;
		}
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { "Create a new element at this position" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			if (dr instanceof DrawPlaceHolder && ((SyntaxPlaceHolder)dr.syntax).parent() instanceof SyntaxAttr) {
				ANode n = dr.drnode;
				SyntaxAttr satt = (SyntaxAttr)((SyntaxPlaceHolder)dr.syntax).parent();
				if (satt.expected_types.length == 0)
					return null;
				return new NewElemHere(editor);
			}
			if (dr instanceof DrawNodeTerm && (dr.drnode == null || dr.getAttrPtr().get() == null)) {
				ANode n = dr.drnode;
				while (n == null) {
					dr = (Drawable)dr.parent();
					n = dr.drnode;
				}
				SyntaxAttr satt = (SyntaxAttr)dr.syntax;
				if (satt.expected_types.length == 0)
					return null;
				return new NewElemHere(editor);
			}
			ActionPoint ap = editor.getActionPoint(false);
			if (ap == null || ap.length < 0)
				return null;
			SyntaxList slst = (SyntaxList)ap.dr.syntax;
			if (slst.expected_types.length == 0)
				return null;
			return new NewElemHere(editor);
		}
	}
}

final class NewElemNext extends NewElemEditor implements Runnable {
	NewElemNext(Editor editor) { super(editor); }
	public void run() {
		ActionPoint ap = editor.getActionPoint(true);
		if (ap != null && ap.length >= 0) {
			SyntaxList slst = (SyntaxList)ap.dr.syntax;
			this.idx = ap.index;
			makeMenu("Append new item", ap.node, slst);
			return;
		}
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { "Create a new element at next position" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			ActionPoint ap = editor.getActionPoint(true);
			if (ap == null || ap.length < 0)
				return null;
			SyntaxList slst = (SyntaxList)ap.dr.syntax;
			if (slst.expected_types.length == 0)
				return null;
			return new NewElemNext(editor);
		}
	}
}

final class PasteElemHere implements Runnable {
	final ANode paste_node;
	final Editor editor;
	final AttrPtr pattr;
	final ActionPoint ap;
	PasteElemHere(ANode paste_node, Editor editor, AttrPtr pattr) {
		this.paste_node = paste_node;
		this.editor = editor;
		this.pattr = pattr;
	}
	PasteElemHere(ANode paste_node, Editor editor, ActionPoint ap) {
		this.paste_node = paste_node;
		this.editor = editor;
		this.ap = ap;
	}
	public void run() {
		ANode node = this.paste_node;
		editor.changes.push(Transaction.open("Editor.java:PasteElemHere"));
		try {
			if (node.isAttached())
				node = node.ncopy();
			if (pattr != null) {
				if (pattr.slot.is_space)
					((SpaceAttrSlot)pattr.slot).insert(pattr.node, 0, node);
				else
					pattr.set(node);
			}
			else if (ap != null)
				((SpaceAttrSlot)ap.slot).insert(ap.node,ap.index,node);
		} finally {
			editor.changes.peek().close();
		}
		editor.formatAndPaint(true);
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { "Paste an element at this position" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			Transferable content = editor.clipboard.getContents(null);
			if (!content.isDataFlavorSupported(TransferableANode.transferableANodeFlavor))
				return null;
			ANode node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
			// try paste as a node into null
			if (dr instanceof DrawNodeTerm) {
				DrawNodeTerm dt = (DrawNodeTerm)dr;
				AttrPtr pattr = dt.getAttrPtr();
				if (pattr.get() == null && pattr.slot.typeinfo.$instanceof(node))
					return new PasteElemHere(node, editor, pattr);
			}
			// try paste as a node into placeholder
			if (dr instanceof DrawPlaceHolder && ((SyntaxPlaceHolder)dr.syntax).parent() instanceof SyntaxAttr) {
				SyntaxAttr sa = (SyntaxAttr)((SyntaxPlaceHolder)dr.syntax).parent();
				AttrPtr pattr = dr.drnode.getAttrPtr(sa.name);
				if (pattr.get() == null && pattr.slot.typeinfo.$instanceof(node))
					return new PasteElemHere(node, editor, pattr);
				else if (pattr.slot.is_space && ((Object[])pattr.get()).length == 0 && pattr.slot.typeinfo.$instanceof(node))
					return new PasteElemHere(node, editor, pattr);
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
		ANode node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
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
		public String getDescr() { "Paste an element at next position" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Transferable content = editor.clipboard.getContents(null);
			if (!content.isDataFlavorSupported(TransferableANode.transferableANodeFlavor))
				return null;
			ANode node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
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
	protected final AttrPtr		pattr;
	protected       int			edit_offset;
	protected       boolean		in_combo;
	protected       JComboBox	combo;

	final static class Factory implements UIActionFactory {
		public String getDescr() { "Edit the attribute as a text" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if !(dt.syntax instanceof SyntaxAttr)
				return null;
			if (dt.drnode != context.node)
				return null;
			AttrPtr pattr = dt.drnode.getAttrPtr(((SyntaxAttr)dt.syntax).name);
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

	TextEditor(Editor editor, DrawTerm dr_term, AttrPtr pattr) {
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
				String text = (String)combo.getSelectedItem();
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
		if !(pattr.node instanceof ASTNode)
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
		int x = dr_term.x;
		int y = dr_term.y - editor.view_canvas.translated_y;
		int w = dr_term.w;
		int h = dr_term.h;
		combo.setBounds(x, y, w+100, h);
		boolean popup = false;
		foreach (DNode dn; decls) {
			combo.addItem(qualified ? dn.qname.replace('\u001f','.') : dn.sname);
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
	
	IntEditor(Editor editor, DrawTerm dr_term, AttrPtr pattr) {
		super(editor, dr_term, pattr);
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { "Edit the attribute as an integer" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if !(dt.syntax instanceof SyntaxAttr)
				return null;
			if (dt.drnode != context.node)
				return null;
			AttrPtr pattr = dt.drnode.getAttrPtr(((SyntaxAttr)dt.syntax).name);
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

class EnumEditor implements KeyListener, PopupMenuListener, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final AttrPtr		pattr;
	private final JPopupMenu	menu;
	EnumEditor(Editor editor, DrawTerm cur_elem, AttrPtr pattr) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.pattr = pattr;
		this.menu = new JPopupMenu();
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { "Edit the attribute as an enumerated value" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if !(dt.syntax instanceof SyntaxAttr)
				return null;
			if (dt.drnode != context.node)
				return null;
			AttrPtr pattr = dt.drnode.getAttrPtr(((SyntaxAttr)dt.syntax).name);
			return new EnumEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		if (pattr.slot.typeinfo.clazz == Boolean.class || pattr.slot.typeinfo.clazz == boolean.class) {
			menu.add(new JMenuItem(new SetSyntaxAction(Boolean.FALSE)));
			menu.add(new JMenuItem(new SetSyntaxAction(Boolean.TRUE)));
		} else {
			EnumSet ens = EnumSet.allOf(pattr.slot.typeinfo.clazz);
			foreach (Enum e; ens.toArray())
				menu.add(new JMenuItem(new SetSyntaxAction(e)));
		}
		int x = cur_elem.x;
		int y = cur_elem.y + cur_elem.h - editor.view_canvas.translated_y;
		menu.addPopupMenuListener(this);
		menu.show(editor.view_canvas, x, y);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	
	public void popupMenuCanceled(PopupMenuEvent e) {
		editor.view_canvas.remove(menu);
		editor.stopItemEditor(true);
	}
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

	class SetSyntaxAction extends TextAction {
		private Object val; // Enum or Boolean
		SetSyntaxAction(Object val) {
			super(String.valueOf(val));
			this.val = val;
		}
		public void actionPerformed(ActionEvent e) {
			editor.view_canvas.remove(menu);
			try {
				pattr.set(val);
			} catch (Throwable t) {
				editor.stopItemEditor(true);
				e = null;
			}
			if (e != null)
				editor.stopItemEditor(false);
		}
	}
}

class AccessEditor implements KeyListener, PopupMenuListener, Runnable, ActionListener {
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
		public String getDescr() { "Edit access attribute" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if !(dt instanceof DrawJavaAccess)
				return null;
			if (dt.drnode != context.node)
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
		int flags = ((MetaAccess)cur_elem.drnode).flags;
		menu.add(b=new JCheckBoxMenuItem("Access bits")); b.setSelected(flags != -1); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("public read",     1<<7, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("public write",    1<<6, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("protected read",  1<<5, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("protected write", 1<<4, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("package read",    1<<3, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("package write",   1<<2, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("private read",    1<<1, flags)); b.addActionListener(this);
		menu.add(b=new SetFlagsMenuItem("private write",   1<<0, flags)); b.addActionListener(this);
		int x = cur_elem.x;
		int y = cur_elem.y + cur_elem.h - editor.view_canvas.translated_y;
		menu.addPopupMenuListener(this);
		menu.show(editor.view_canvas, x, y);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	
	public void popupMenuCanceled(PopupMenuEvent e) {
		editor.view_canvas.remove(menu);
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
				((MetaAccess)cur_elem.drnode).simple = mi.val;
				setMenuVisible();
			}
			else if (e.getSource() instanceof SetFlagsMenuItem) {
				SetFlagsMenuItem mi = (SetFlagsMenuItem)e.getSource();
				if (mi.isSelected())
					((MetaAccess)cur_elem.drnode).flags |= mi.val;
				else
					((MetaAccess)cur_elem.drnode).flags &= ~mi.val;
				setMenuVisible();
			}
			else if (e.getSource() instanceof JCheckBoxMenuItem) {
				JCheckBoxMenuItem mi = (JCheckBoxMenuItem)e.getSource();
				if (mi.isSelected()) {
					int flags = ((MetaAccess)cur_elem.drnode).flags;
					foreach (SetFlagsMenuItem sf; menu.getSubElements()) {
						sf.setEnabled(true);
						sf.setSelected((flags & sf.val) != 0);
					}
				} else {
					foreach (SetFlagsMenuItem sf; menu.getSubElements()) {
						sf.setEnabled(false);
						sf.setSelected(false);
					}
					((MetaAccess)cur_elem.drnode).flags = -1;
				}
				setMenuVisible();
			}
			else {
				editor.view_canvas.remove(menu);
				editor.stopItemEditor(false);
			}
		} catch (Throwable t) {
			editor.view_canvas.remove(menu);
			editor.stopItemEditor(true);
		}
	}

	class SetSimpleMenuItem extends JRadioButtonMenuItem {
		final String val;
		SetSimpleMenuItem(String text, String val) {
			super(text);
			this.val = val;
			setSelected((((MetaAccess)cur_elem.drnode).simple == val));
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
}

class OperatorEditor implements KeyListener, PopupMenuListener, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final ENode			expr;
	private final JPopupMenu	menu;
	OperatorEditor(Editor editor, DrawTerm cur_elem) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.expr = (ENode)cur_elem.drnode;
		this.menu = new JPopupMenu();
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { "Edit the operator of an expression" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt.drnode != context.node)
				return null;
			if !(dt instanceof DrawToken && dt.drnode instanceof ENode && ((SyntaxToken)dt.syntax).kind == SyntaxToken.TokenKind.OPERATOR)
				return null;
			return new OperatorEditor(editor, dt);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		if (expr instanceof TypeExpr) {
			// show all postfix type operators
			foreach (Operator op; Operator.allOperatorNamesHash; op.name.startsWith("T "))
				menu.add(new JMenuItem(new SetSyntaxAction(op)));
		}
		else if (expr.getArgs().length == 2) {
			JMenu m_assign = new JMenu("Assign");
			menu.add(m_assign);
			foreach (Operator op; Operator.allAssignOperators; op.arity == 2)
				m_assign.add(new JMenuItem(new SetSyntaxAction(op)));

			JMenu m_bool   = new JMenu("Boolean");
			menu.add(m_bool);
			foreach (Operator op; Operator.allBoolOperators; op.arity == 2)
				m_bool.add(new JMenuItem(new SetSyntaxAction(op)));

			JMenu m_math   = new JMenu("Arithmetic");
			menu.add(m_math);
			foreach (Operator op; Operator.allMathOperators; op.arity == 2)
				m_math.add(new JMenuItem(new SetSyntaxAction(op)));

			JMenu m_others = new JMenu("Others");
			menu.add(m_others);
			foreach (Operator op; Operator.allOperatorNamesHash; op.arity == 2 && !op.name.startsWith("T ")) {
				if (Arrays.contains(Operator.allAssignOperators, op))
					continue;
				if (Arrays.contains(Operator.allBoolOperators, op))
					continue;
				if (Arrays.contains(Operator.allMathOperators, op))
					continue;
				m_others.add(new JMenuItem(new SetSyntaxAction(op)));
			}
		}
		else {
			int arity = expr.getArgs().length;
			foreach (Operator op; Operator.allOperatorNamesHash; op.arity == arity && !op.name.startsWith("T "))
				menu.add(new JMenuItem(new SetSyntaxAction(op)));
		}
		int x = cur_elem.x;
		int y = cur_elem.y + cur_elem.h - editor.view_canvas.translated_y;
		menu.addPopupMenuListener(this);
		menu.show(editor.view_canvas, x, y);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	
	public void popupMenuCanceled(PopupMenuEvent e) {
		editor.view_canvas.remove(menu);
		editor.stopItemEditor(true);
	}
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

	class SetSyntaxAction extends TextAction {
		private Operator op; // Enum or Boolean
		SetSyntaxAction(Operator op) {
			super(String.valueOf(op));
			this.op = op;
		}
		public void actionPerformed(ActionEvent e) {
			editor.view_canvas.remove(menu);
			try {
				ENode expr = this.expr;
				expr.setOp(op);
			} catch (Throwable t) {
				editor.stopItemEditor(true);
				e = null;
			}
			if (e != null)
				editor.stopItemEditor(false);
		}
	}
}


final class FindDialog extends JDialog implements ActionListener {
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
	private void lookup(String txt) {
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
		Vector<ANode> path = new Vector<ANode>();
		path.append(n);
		while (n.parent() != null) {
			n = n.parent();
			path.append(n);
			if (n instanceof FileUnit)
				break;
		}
		the_view.goToPath(path.toArray());
	}
	static class FoundException extends RuntimeException {
		final ANode node;
		FoundException(ANode n) { this.node = n; }
	}
}
