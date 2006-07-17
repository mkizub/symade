package kiev.gui;

import kiev.Kiev;
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

import javax.swing.JComboBox;
import javax.swing.ComboBoxEditor;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
	private KeyListener		item_editor;
	/** Current x position for scrolling up/down */
	int						cur_x;
	/** Current item */
	@ref public Drawable	cur_elem;
	/** The object in clipboard */
	@ref public ANode		in_clipboard;
	
	private Stack<Transaction>		changes = new Stack<Transaction>();
	
	{
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_LEFT),      new NavigateEditor(this,NavigateView.LEFT));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_RIGHT),     new NavigateEditor(this,NavigateView.RIGHT));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_UP),        new NavigateEditor(this,NavigateView.LINE_UP));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_DOWN),      new NavigateEditor(this,NavigateView.LINE_DOWN));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_HOME),      new NavigateEditor(this,NavigateView.LINE_HOME));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_END),       new NavigateEditor(this,NavigateView.LINE_END));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_PAGE_UP),   new NavigateEditor(this,NavigateView.PAGE_UP));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_PAGE_DOWN), new NavigateEditor(this,NavigateView.PAGE_DOWN));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_E),         new ChooseItemEditor(this));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_P),         new NewElemEditor(this,NewElemEditor.INSERT_HERE));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_A),         new NewElemEditor(this,NewElemEditor.INSERT_NEXT));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_N),         new NewElemEditor(this,NewElemEditor.SETNEW_HERE));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_F),         new FolderTrigger(this));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_O),         new OptionalTrigger(this));
	}
	
	public Editor(Window window, TextSyntax syntax, Canvas view_canvas) {
		super(window, syntax, view_canvas);
	}
	
	public void setRoot(ANode root) {
		super.setRoot(root);
		cur_elem = view_root.getFirstLeaf();
	}
	
	public void setSyntax(TextSyntax syntax) {
		cur_elem = null;
		super.setSyntax(syntax);
	}

	public void formatAndPaint(boolean full) {
		view_canvas.current = cur_elem;
		if (full)
			super.formatAndPaint(full);
		else
			view_canvas.repaint();
		ANode src = cur_elem!=null ? cur_elem.node : null;
		parent_window.info_view.the_root = src;
		parent_window.info_view.formatAndPaint(true);
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
		if (mask == 0) {
			KeyHandler kh = naviMap.get(Integer.valueOf(code));
			if (kh != null) {
				kh.process();
				evt.consume();
				return;
			}
			return;
		}
		else if (mask == KeyEvent.ALT_DOWN_MASK) {
			switch (code) {
			case KeyEvent.VK_UP:
				if (cur_elem.isAttached()) {
					view_canvas.current = cur_elem = (Drawable)cur_elem.parent();
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_DOWN:
				if (cur_elem instanceof DrawNonTerm) {
					view_canvas.current = cur_elem = cur_elem.getFirstLeaf();
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_C:
				evt.consume();
				kiev.Compiler.runBackEnd(null);
				foreach (FileUnit fu; kiev.Kiev.files) {
					walkTree(new TreeWalker() {
						public boolean pre_exec(ANode n) { if (n instanceof ASTNode) { n.compileflags &= 0xFFFF0000; } return true; }
					});
				}
				formatAndPaint(true);
				break;
			}
		}
		else if (mask == KeyEvent.CTRL_DOWN_MASK) {
			evt.consume(); 
			switch (code) {
			case KeyEvent.VK_Z:
				if (changes.length > 0) {
					Transaction tr = changes.pop();
					tr.rollback(false);
					formatAndPaint(true);
				}
				break;
			case KeyEvent.VK_C:
				if (cur_elem instanceof DrawNodeTerm) {
					AttrPtr pattr = ((DrawNodeTerm)cur_elem).getAttrPtr();
					Object obj = pattr.get();
					if (obj instanceof Symbol) {
						in_clipboard = ((Symbol)obj).parent();
						parent_window.clip_view.setRoot(in_clipboard);
						parent_window.clip_view.formatAndPaint(true);
					}
				}
				break;
			case KeyEvent.VK_V:
				if (in_clipboard != null) {
					if (cur_elem instanceof DrawNodeTerm) {
						if (in_clipboard instanceof DNode) {
							AttrPtr pattr = ((DrawNodeTerm)cur_elem).getAttrPtr();
							if (pattr.slot.clazz == SymbolRef.class) {
								DNode dn = (DNode)in_clipboard;
								changes.push(Transaction.open());
								try {
									SymbolRef obj = (SymbolRef)pattr.get();
									if (obj != null) {
										obj.open();
										obj.name = dn.id.sname;
										obj.symbol = dn;
									} else {
										pattr.node.open();
										pattr.set(new SymbolRef<DNode>(0,dn));
									}
								} finally {
									changes.peek().close();
								}
								this.formatAndPaint(true);
							}
						}
					}
				}
				break;
			case KeyEvent.VK_X:
				{
					ANode node = cur_elem.node;
					changes.push(Transaction.open());
					in_clipboard = node;
					node.detach();
					changes.peek().close();
					formatAndPaint(true);
					parent_window.clip_view.setRoot(in_clipboard);
					parent_window.clip_view.formatAndPaint(true);
				}
				break;
			case KeyEvent.VK_R:
				setSyntax(this.syntax);
				cur_elem = view_root.getFirstLeaf();
				view_canvas.root = view_root;
				formatAndPaint(false);
				break;
			}
		}
/*		else if (mask == KeyEvent.CTRL_DOWN_MASK && !mode_edit) {
			switch (code) {
			case KeyEvent.VK_UP:
				if (view_canvas.first_line > 0) {
					--view_canvas.first_line;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_DOWN:
				if (view_canvas.first_line < view_canvas.num_lines) {
					++view_canvas.first_line;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_E:
				if (cur_elem instanceof MTerminal) {
					World.export(((MTerminal)cur_elem).src);
//					if (cur_elem.getn(World._attr_term_edit_nv_attr) == World._attr_self) {
//						parent_window.export_view.the_root = cur_elem.getn(World._attr_term_edit_nv_node);
//						parent_window.export_view.formatAndPaint(true);
//					}
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_D:{
				MDrawable nt = cur_elem;
				if (nt != null && Type.isA(nt.getDrwParent(), World.theTypeOfNonTermArrays) &&
					(Type.isA(nt, World.theTypeOfTerminals) ||
					 Type.isA(nt, World.theTypeOfNonTermArrays) ||
					 Type.isA(nt, World.theTypeOfNonTermStructs))
				) {
					List lst = ((MNonTermArray)nt.getDrwParent()).src;
					int idx = nt.pslot_in_src;
					nt = nt.getDrwParent();
					MDrawable dr = cur_elem = ((MNonTermArray)nt).args[0];
					lst.remove(idx);
					for (; dr != null; dr = dr.dr_next) {
						if (dr.pslot_in_src <= idx)
							cur_elem = dr;
						if (dr.pslot_in_src == idx)
							break;
					}
					formatAndPaint(true);
					Verificator.verify();
				}
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_C:
				if (cur_elem instanceof MTerminal) {
					if (((MTerminal)cur_elem).getAttr() == World._attr_self) {
						in_clipboard = ((MTerminal)cur_elem).src;
						parent_window.clip_view.the_root = in_clipboard;
						parent_window.clip_view.formatAndPaint(true);
					}
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_V:
				if (in_clipboard != null) {
					MDrawable nt = cur_elem.getDrwParent();
					if (Type.isA(nt, World.theTypeOfNonTermArrays) && cur_elem instanceof MSpace) {
						List src = (List)nt.getn(World._attr_nt_arr_src);
						if (!src.of_definitions) {
							MType tp = (MType)World.getType(src).representation_node.getn(World._attr_list_element_type);
							if (tp == null || Type.isA(in_clipboard, tp)) {
								int pos = cur_elem.pslot_in_src;
								if (pos < 0) pos = 0;
								if (pos > src.size()) pos = src.size();
								src.insert(pos, in_clipboard);
								cur_elem = nt.getFirstLeaf();
								MDrawable dr = cur_elem;
								for (; dr != null; dr = dr.dr_next) {
									if (dr instanceof MTerminal) {
										if (dr.pslot_in_src >= pos) {
											cur_elem = dr;
											break;
										}
									}
								}
								formatAndPaint(true);
								Verificator.verify();
							}
						}
					}
					else if (Type.isA(nt, World.theTypeOfNonTermStructs) && cur_elem instanceof MToken
							&& Type.isA(cur_elem, World.theTypeOfTerminals))
					{
						MNode src = nt.getn(World._attr_nt_struct_src);
						MAttr attr = ((MTerminal)cur_elem).getAttr(); 
						if (attr.getDefinitionMode() != Attr.ATTR_DEFINITION_REQUIRED) {
							MType tp = (MType)attr.getn(World._attr_attr_type);
							if (tp == null || Type.isA(in_clipboard, tp)) {
								src.setv(attr, in_clipboard);
								formatAndPaint(true);
								Verificator.verify();
							}
						}
					}
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_G:
				System.gc();
				Verificator.verify();
				evt.consume(); 
				break;
			case KeyEvent.VK_R:
				System.gc();
				System.gc();
				DefaultDrawer.create(the_root, this, World._attr_ui_view_root);
				cur_elem = view_root.getFirstLeaf();
				view_canvas.root = view_root;
				formatAndPaint(true);
				System.gc();
				System.gc();
				Verificator.verify();
				evt.consume(); 
				break;
			}
		}
*/
		super.keyPressed(evt);
	}
	
	public void startItemEditor(ANode obj, KeyListener item_editor) {
		assert (this.item_editor == null);
		this.item_editor = item_editor;
		changes.push(Transaction.open());
		obj.open();
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
	
/*	private JPopupMenu buildTypeSelectPopupMenu(Type tp, List lst, int idx) {
		// build a menu of types to instantiate
		JPopupMenu m = new JPopupMenu();
		m.add(new JMenuItem(new AddNewNodeToListAction((MType)tp.representation_node, lst, idx)));
		m.addSeparator();
		Type[] children = tp.getDirectChildren();
		for (int i=0; i < children.length; i++) {
			Type ch = children[i];
			if (ch.getDirectChildren().length == 0)
				m.add(new JMenuItem(new AddNewNodeToListAction((MType)ch.representation_node, lst, idx)));
			else
				m.add(buildTypeSelectSubMenu(ch, lst, idx));
		}
		return m;
	}
	private JMenu buildTypeSelectSubMenu(Type tp, List lst, int idx) {
		// build a menu of types to instantiate
		JMenu m = new JMenu(((MType)tp.representation_node).text);
		m.add(new JMenuItem(new AddNewNodeToListAction((MType)tp.representation_node, lst, idx)));
		m.addSeparator();
		Type[] children = tp.getDirectChildren();
		for (int i=0; i < children.length; i++) {
			Type ch = children[i];
			if (ch.getDirectChildren().length == 0)
				m.add(new JMenuItem(new AddNewNodeToListAction((MType)ch.representation_node, lst, idx)));
			else
				m.add(buildTypeSelectSubMenu(ch, lst, idx));
		}
		return m;
	}
*/
	
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY() + view_canvas.translated_y;
		Drawable dr = view_canvas.first_visible;
		for (; dr != null; dr = dr.getNextLeaf()) {
			if (dr.geometry.x < x && dr.geometry.y < y && dr.geometry.x+dr.geometry.w >= x && dr.geometry.y+dr.geometry.h >= y) {
				cur_elem = dr;
				cur_x = cur_elem.geometry.x;
				formatAndPaint(false);
				break;
			}
			if (dr == view_canvas.last_visible)
				break;
		}
	}
/*
	class AddNewNodeToListAction extends TextAction {
		private final Class				cls;
		private final ASTNode[]			lst;
		private final int				index;
		AddNewNodeToListAction(Class cls, ASTNode[] lst, int index) {
			super(cls.getName());
			this.cls   = cls;
			this.lst   = lst;
			this.index = index;
		}
		public void actionPerformed(ActionEvent e) {
			ASTNode node = cls.newInstance();
			lst.insert(node, index);
			formatAndPaint(true);
		}
	}
*/
}

public interface KeyHandler {
	public void process();
}

final class NavigateEditor extends NavigateView implements KeyHandler {

	NavigateEditor(Editor uiv, int cmd) {
		super(uiv,cmd);
	}

	public void process() {
		switch (cmd) {
		case LEFT:       navigatePrev(true); return;
		case RIGHT:      navigateNext(true); return;
		case LINE_UP:    navigateUp(true);   return;
		case LINE_DOWN:  navigateDn(true); return;
		case LINE_HOME:  navigateLineHome(true); return;
		case LINE_END:   navigateLineEnd(true);  return;
		case PAGE_UP:    navigatePageUp();  return;
		case PAGE_DOWN:  navigatePageDn();  return;
		}
	}

	private void navigatePrev(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm prev = uiv.cur_elem.getFirstLeaf().getPrevLeaf();
		if (prev != null) {
			uiv.view_canvas.current = uiv.cur_elem = prev;
			uiv.cur_x = prev.geometry.x;
		}
		if (repaint) {
			makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateNext(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm next = uiv.cur_elem.getFirstLeaf().getNextLeaf();
		if (next != null) {
			uiv.view_canvas.current = uiv.cur_elem = next;
			uiv.cur_x = next.geometry.x;
		}
		if (repaint) {
			makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateUp(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm n = null;
		DrawTerm prev = uiv.cur_elem.getFirstLeaf();
		if (prev != null)
			prev = prev.getPrevLeaf();
		while (prev != null) {
			if (prev.geometry.do_newline > 0) {
				n = prev;
				break;
			}
			prev = prev.getPrevLeaf();
		}
		while (n != null) {
			if (n.geometry.x <= uiv.cur_x && n.geometry.x+n.geometry.w >= uiv.cur_x)
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.geometry.do_newline > 0)
				break;
			if (prev.geometry.x+prev.geometry.w < uiv.cur_x)
				break;
			n = prev;
		}
		if (n != null) {
			uiv.view_canvas.current = uiv.cur_elem = n;
		}
		if (repaint) {
			makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateDn(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm n = null;
		DrawTerm next = uiv.cur_elem.getFirstLeaf();
		while (next != null) {
			if (next.geometry.do_newline > 0) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			if (n.geometry.x <= uiv.cur_x && n.geometry.x+n.geometry.w >= uiv.cur_x)
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.geometry.x > uiv.cur_x)
				break;
			if (next.geometry.do_newline > 0)
				break;
			n = next;
		}
		if (n != null) {
			uiv.view_canvas.current = uiv.cur_elem = n;
		}
		if (repaint) {
			makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateLineHome(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		int lineno = uiv.cur_elem.getFirstLeaf().geometry.lineno;
		for (;;) {
			DrawTerm dr = uiv.cur_elem.getPrevLeaf();
			if (dr == null || dr.geometry.lineno != lineno)
				break;
			uiv.cur_elem = dr;
		}
		uiv.view_canvas.current = uiv.cur_elem;
		uiv.cur_x = uiv.cur_elem.geometry.x;
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigateLineEnd(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		int lineno = uiv.cur_elem.getFirstLeaf().geometry.lineno;
		for (;;) {
			DrawTerm dr = uiv.cur_elem.getNextLeaf();
			if (dr == null || dr.geometry.lineno != lineno)
				break;
			uiv.cur_elem = dr;
		}
		uiv.view_canvas.current = uiv.cur_elem;
		uiv.cur_x = uiv.cur_elem.geometry.x;
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigatePageUp() {
		final Editor uiv = (Editor)this.uiv;
		int offs = uiv.view_canvas.last_visible.geometry.lineno - uiv.view_canvas.first_visible.geometry.lineno -1;
		uiv.view_canvas.first_line -= offs;
		for (int i=offs; i >= 0; i--)
			navigateUp(i==0);
		return;
	}
	private void navigatePageDn() {
		final Editor uiv = (Editor)this.uiv;
		int offs = uiv.view_canvas.last_visible.geometry.lineno - uiv.view_canvas.first_visible.geometry.lineno -1;
		uiv.view_canvas.first_line += offs;
		for (int i=offs; i >= 0; i--)
			navigateDn(i==0);
		return;
	}

	private void makeCurrentVisible() {
		final Editor uiv = (Editor)this.uiv;
		int top_lineno = uiv.view_canvas.first_visible.geometry.lineno;
		int bot_lineno = uiv.view_canvas.last_visible.geometry.lineno;
		int height = bot_lineno - top_lineno;
		
		if (top_lineno > 0 && uiv.cur_elem.getFirstLeaf().geometry.lineno <= top_lineno) {
			uiv.view_canvas.first_line = uiv.cur_elem.getFirstLeaf().geometry.lineno -1;
		}
		if (bot_lineno < uiv.view_canvas.num_lines && uiv.cur_elem.getFirstLeaf().geometry.lineno >= bot_lineno) {
			uiv.view_canvas.first_line = uiv.cur_elem.getFirstLeaf().geometry.lineno - height + 1;
		}
		if (uiv.view_canvas.first_line < 0)
			uiv.view_canvas.first_line = 0;
	}

}

final class ChooseItemEditor implements KeyHandler {

	private final Editor	editor;

	ChooseItemEditor(Editor editor) {
		this.editor = editor;
	}

	public void process() {
		Drawable dr = editor.cur_elem;
		if (dr instanceof DrawNodeTerm) {
			AttrPtr pattr = dr.getAttrPtr();
			Object obj = pattr.get();
			if (obj instanceof Symbol)
				editor.startItemEditor((Symbol)obj, new SymbolEditor((Symbol)obj, editor));
			else if (obj instanceof SymbolRef)
				editor.startItemEditor((SymbolRef)obj, new SymRefEditor((SymbolRef)obj, editor));
			else if (obj instanceof String || obj == null && pattr.slot.clazz == String.class) {
				if (pattr.node instanceof SymbolRef)
					editor.startItemEditor((SymbolRef)pattr.node, new SymRefEditor((SymbolRef)pattr.node, editor));
				else
					editor.startItemEditor(pattr.node, new StrEditor(pattr, editor));
			}
			else if (obj instanceof Integer)
				editor.startItemEditor(pattr.node, new IntEditor(pattr, editor));
			else if (obj instanceof ConstIntExpr)
				editor.startItemEditor((ConstIntExpr)obj, new IntEditor(obj.getAttrPtr("value"), editor));
			else if (Enum.class.isAssignableFrom(pattr.slot.clazz))
				editor.startItemEditor(pattr.node, new EnumEditor(pattr, dr, editor));
		}
		else if (dr.parent() instanceof DrawEnumChoice) {
			DrawEnumChoice dec = (DrawEnumChoice)dr.parent();
			SyntaxEnumChoice stx = (SyntaxEnumChoice)dec.syntax;
			editor.startItemEditor(dec.node, new EnumEditor(dec.node.getAttrPtr(stx.name), dr, editor));
		}
	}
}

final class FolderTrigger implements KeyHandler {

	private final Editor	editor;

	FolderTrigger(Editor editor) {
		this.editor = editor;
	}

	public void process() {
		for (Drawable dr = editor.cur_elem; dr != null; dr = (Drawable)dr.parent()) {
			if (dr instanceof DrawFolded) {
				dr.draw_folded = !dr.draw_folded;
				editor.formatAndPaint(true);
				return;
			}
		}
	}
}

final class OptionalTrigger implements KeyHandler {

	private final Editor	editor;

	OptionalTrigger(Editor editor) {
		this.editor = editor;
	}

	public void process() {
		ANode n = editor.cur_elem;
		while (n != null && !(n instanceof DrawNonTerm))
			n = n.parent();
		if (n == null)
			return;
		boolean repaint = false;
		DrawNonTerm drnt = (DrawNonTerm)n;
		foreach (Drawable dr; drnt.args) {
			if (dr instanceof DrawOptional) {
				dr.draw_optional = !dr.draw_optional;
				repaint = true;
			}
			else if (dr instanceof DrawNonTermList) {
				dr.draw_optional = !dr.draw_optional;
				repaint = true;
			}
		}
		if (repaint)
			editor.formatAndPaint(true);
	}
}


final class NewElemEditor implements KeyHandler, KeyListener, PopupMenuListener {

	static final int INSERT_HERE = 0;
	static final int INSERT_NEXT = 1;
	static final int SETNEW_HERE = 2;

	private final Editor		editor;
	private final int			mode;
	private       int			idx;
	private       JPopupMenu	menu;

	NewElemEditor(Editor editor, int mode) {
		this.editor = editor;
		this.mode = mode;
	}

	public void process() {
		Drawable dr = editor.cur_elem;
		if (mode == SETNEW_HERE) {
			ANode n = dr.node;
			while (n == null) {
				dr = (Drawable)dr.parent();
				if (dr == null)
					break;
				n = dr.node;
			}
			if (n == null)
				return;
			if (dr.syntax instanceof SyntaxAttr) {
				SyntaxAttr satt = (SyntaxAttr)dr.syntax;
				if (satt.expected_types.length > 0) {
					menu = new JPopupMenu("Set new item");
					foreach (SymbolRef sr; satt.expected_types; sr.symbol instanceof Struct) {
						menu.add(new JMenuItem(new NewElemAction((Struct)sr.symbol, n, satt.name)));
					}
					int x = dr.geometry.x;
					int y = dr.geometry.y + dr.geometry.h;
					menu.addPopupMenuListener(this);
					menu.show(editor.view_canvas, x, y);
					editor.startItemEditor(n, this);
				}
			}
		} else {
			while (dr != null && !(dr.parent() instanceof DrawNonTermList))
				dr = (Drawable)dr.parent();
			if (dr != null && dr.parent() instanceof DrawNonTermList) {
				DrawNonTermList lst = (DrawNonTermList)dr.parent();
				SyntaxList slst = (SyntaxList)lst.syntax;
				if (slst.expected_types.length > 0) {
					this.idx = lst.args.indexOf(dr);
					if (mode == INSERT_NEXT)
						this.idx += 1;
					if (slst.expected_types.length > 0) {
						menu = new JPopupMenu(mode==INSERT_HERE ? "Prepend new item" : "Append new item");
						foreach (SymbolRef sr; slst.expected_types; sr.symbol instanceof Struct) {
							menu.add(new JMenuItem(new NewElemAction((Struct)sr.symbol, lst.node, slst.name)));
						}
						int x = dr.geometry.x;
						int y = dr.geometry.y + dr.geometry.h;
						menu.addPopupMenuListener(this);
						menu.show(editor.view_canvas, x, y);
						editor.startItemEditor(lst.node, this);
					}
				}
			}
		}
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
			super(cls.id.sname);
			this.cls = cls;
			this.node = node;
			this.attr = attr;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null)
				editor.view_canvas.remove(menu);
			ANode obj = (ANode)Class.forName(cls.qname()).newInstance();
			foreach (AttrSlot a; node.values(); a.name == attr) {
				try {
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

abstract class TextEditor implements KeyListener {
	
	protected final Editor		editor;
	private         int			edit_offset;
	protected       JComboBox	combo;

	TextEditor(Editor editor) {
		this.editor = editor;
		this.editor.view_canvas.cursor_offset = edit_offset;
	}

	abstract String getText();
	abstract void setText(String text);

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
		if (edit_offset < 0) { editor.view_canvas.cursor_offset = edit_offset = 0; }
		if (edit_offset > text.length()) { editor.view_canvas.cursor_offset = edit_offset = text.length(); }
		switch (code) {
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
			editor.view_canvas.cursor_offset = edit_offset = -1;
			editor.stopItemEditor(false);
			if (combo != null)
				editor.view_canvas.remove(combo);
			return;
		case KeyEvent.VK_ESCAPE:
			editor.view_canvas.cursor_offset = edit_offset = -1;
			editor.stopItemEditor(true);
			if (combo != null)
				editor.view_canvas.remove(combo);
			return;
		default:
			if (evt.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
				text = text.substring(0, edit_offset)+evt.getKeyChar()+text.substring(edit_offset);
				edit_offset++;
				this.setText(text);
			}
		}
		editor.view_canvas.cursor_offset = edit_offset;
		editor.formatAndPaint(true);
	}
}

final class SymbolEditor extends TextEditor {
	
	private final Symbol	symbol;

	SymbolEditor(Symbol symbol, Editor editor) {
		super(editor);
		this.symbol = symbol;
	}
	
	String getText() {
		return symbol.sname;
	}
	void setText(String text) {
		symbol.sname = text;
	}
}

final class SymRefEditor extends TextEditor implements ComboBoxEditor {
	
	private final SymbolRef<DNode>		symref;

	SymRefEditor(SymbolRef<DNode> symref, Editor editor) {
		super(editor);
		this.symref = symref;
		showAutoComplete();
	}
	
	String getText() {
		String name = symref.name;
		return name;
	}
	void setText(String text) {
		String name = symref.name;
		if (name == null || !name.equals(text)) {
			symref.name = text;
			symref.symbol = null;
			showAutoComplete();
		}
	}
	
	void showAutoComplete() {
		String name = symref.name;
		if (name == null || name.length() == 0)
			return;
		DNode[] decls = symref.findForResolve(false);
		if (decls == null)
			return;
		if (combo == null) {
			combo = new JComboBox();
			combo.setEditable(true);
			combo.setEditor(this);
			combo.configureEditor(this, name);
			combo.setMaximumRowCount(10);
			editor.view_canvas.add(combo);
		} else {
			combo.removeAllItems();
		}
		Drawable dr = editor.cur_elem;
		int x = dr.geometry.x;
		int y = dr.geometry.y;
		int w = dr.geometry.w;
		int h = dr.geometry.h;
		combo.setBounds(x, y, w+100, h);
		combo.setPopupVisible(false);
		boolean popup = false;
		foreach (DNode dn; decls) {
			combo.addItem(dn.id.sname);
			popup = true;
		}
		combo.setPopupVisible(popup);
	}
	
	public void addActionListener(ActionListener l) {}
	public void removeActionListener(ActionListener l) {}
	public Component getEditorComponent() { return null; }
	public Object getItem() { return symref.name; }
	public void selectAll() {}
	public void setItem(Object text) {
		if (text != null) {
			setText((String)text);
			editor.formatAndPaint(true);
		}
	}
}

final class StrEditor extends TextEditor {
	
	private final AttrPtr	pattr;

	StrEditor(AttrPtr pattr, Editor editor) {
		super(editor);
		this.pattr = pattr;
	}
	
	String getText() {
		return (String)pattr.get();
	}
	void setText(String text) {
		pattr.set(text);
	}
}

final class IntEditor extends TextEditor {
	
	private final AttrPtr	pattr;

	IntEditor(AttrPtr pattr, Editor editor) {
		super(editor);
		this.pattr = pattr;
	}
	
	String getText() {
		return String.valueOf(pattr.get());
	}
	void setText(String text) {
		pattr.set(Integer.valueOf(text));
	}
}

class EnumEditor implements KeyListener, PopupMenuListener {
	private final Editor		editor;
	private final AttrPtr		pattr;
	private final JPopupMenu	menu;
	EnumEditor(AttrPtr pattr, Drawable cur_elem, Editor editor) {
		this.editor = editor;
		this.pattr = pattr;
		menu = new JPopupMenu();
		EnumSet ens = EnumSet.allOf(pattr.slot.clazz);
		foreach (Enum e; ens.toArray())
			menu.add(new JMenuItem(new SetSyntaxAction(e)));
		int x = cur_elem.geometry.x;
		int y = cur_elem.geometry.y + cur_elem.geometry.h;
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
		private Enum val;
		SetSyntaxAction(Enum val) {
			super(val.toString());
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

