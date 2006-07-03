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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

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
	@ref public ASTNode		in_clipboard;
	
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
	}
	
	public Editor(Window window, TextSyntax syntax, Canvas view_canvas) {
		super(window, syntax, view_canvas);
	}
	
	public void setRoot(ASTNode root) {
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
		ASTNode src = cur_elem!=null ? cur_elem.node : null;
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
			if (code == KeyEvent.VK_E) {
				if (cur_elem instanceof DrawNodeTerm) {
					AttrPtr pattr = ((DrawNodeTerm)cur_elem).getAttrPtr();
					Object obj = pattr.get();
					if (obj instanceof Symbol) {
						item_editor = new SymbolEditor((Symbol)obj, this);
					}
					else if (obj instanceof Integer) {
						obj = pattr.node;
						item_editor = new IntEditor(pattr, this);
					}
					else if (obj instanceof ConstIntExpr) {
						item_editor = new IntEditor(obj.getAttrPtr("value"), this);
					}
					else if (Enum.class.isAssignableFrom(pattr.slot.clazz)) {
						obj = pattr.node;
						item_editor = new EnumEditor(pattr, cur_elem, this);
					}

					if (item_editor != null) {
						changes.push(Transaction.open());
						((ANode)obj).open();
						view_canvas.repaint();
					}
				}
				evt.consume(); 
				return;
			}
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
		for (; dr != null && dr != view_canvas.last_visible; dr = dr.getNextLeaf()) {
			if (dr.geometry.x < x && dr.geometry.y < y && dr.geometry.x+dr.geometry.w >= x && dr.geometry.y+dr.geometry.h >= y) {
				cur_elem = dr;
				cur_x = cur_elem.geometry.x;
				formatAndPaint(false);
				break;
			}
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

abstract class TextEditor implements KeyListener {
	
	private final Editor	editor;
	private       int		edit_offset;

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
		if (mask != 0)
			return;
		evt.consume();
		String text = this.getText();
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
			return;
		case KeyEvent.VK_ESCAPE:
			editor.view_canvas.cursor_offset = edit_offset = -1;
			editor.stopItemEditor(true);
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
	private final Editor	editor;
	private final AttrPtr	pattr;
	EnumEditor(AttrPtr pattr, Drawable cur_elem, Editor editor) {
		this.editor = editor;
		this.pattr = pattr;
		JPopupMenu m = new JPopupMenu();
		EnumSet ens = EnumSet.allOf(pattr.slot.clazz);
		foreach (Enum e; ens.toArray())
			m.add(new JMenuItem(new SetSyntaxAction(e)));
		int x = cur_elem.geometry.x;
		int y = cur_elem.geometry.y + cur_elem.geometry.h;
		m.addPopupMenuListener(this);
		m.show(editor.view_canvas, x, y);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	
	public void popupMenuCanceled(PopupMenuEvent e) { editor.stopItemEditor(true); }
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

	class SetSyntaxAction extends TextAction {
		private Enum val;
		SetSyntaxAction(Enum val) {
			super(val.toString());
			this.val = val;
		}
		public void actionPerformed(ActionEvent e) {
			pattr.set(val);
			editor.stopItemEditor(false);
		}
	}
}

