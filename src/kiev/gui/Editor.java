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


import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.TextAction;

/**
 * @author mkizub
 */
@node(copyable=false)
public class Editor extends UIView implements KeyListener {
	
	/** Symbols used by editor */
	
	/** Current editor mode */
	private boolean			mode_edit;
	/** Current x position for scrolling up/down */
	private int				cur_x;
	/** Current edit offset */
	private int				edit_offset;
	/** Current item */
	@ref public Drawable	cur_elem;
	/** The object in clipboard */
	@ref public ASTNode		in_clipboard;
	
	public Editor(Window window, Syntax syntax, Canvas view_canvas) {
		super(window, syntax, view_canvas);
		view_canvas.addKeyListener(this);
	}
	
	public void setRoot(ASTNode root) {
		this.the_root = root;
		view_canvas.root = view_root = formatter.format(the_root);
		cur_elem = view_root.getFirstLeaf();
	}
	
	public void formatAndPaint() {
		view_canvas.current = cur_elem;
		formatter.format(the_root);
		view_canvas.repaint();
		ASTNode src = cur_elem!=null ? cur_elem.node : null;
		parent_window.info_view.the_root = src;
		parent_window.info_view.formatAndPaint();
	}

	public void keyReleased(KeyEvent evt) {
		//System.out.println(evt);
	}
	public void keyTyped(KeyEvent evt) {
		//System.out.println(evt);
	}
	
	private boolean isSpaceOrHidden(Drawable dr) {
		return dr.isUnvisible() || isSpace(dr);
	}
	
	private boolean isSpace(Drawable dr) {
		return dr instanceof DrawSpace;
	}
	
	public void keyPressed(KeyEvent evt) {
		//System.out.println(evt);
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == 0 && !mode_edit) {
			switch (code) {
			case KeyEvent.VK_LEFT:  navigatePrev(true);     evt.consume(); break;
			case KeyEvent.VK_RIGHT: navigateNext(true);     evt.consume(); break;
			case KeyEvent.VK_UP:    navigateUp  (true);     evt.consume(); break;
			case KeyEvent.VK_DOWN:  navigateDown(true);     evt.consume(); break;
			case KeyEvent.VK_HOME:  navigateLineHome(true); evt.consume(); break;
			case KeyEvent.VK_END:   navigateLineEnd(true);  evt.consume(); break;
			case KeyEvent.VK_PAGE_UP:
				code = view_canvas.last_visible.geometry.lineno - view_canvas.first_visible.geometry.lineno -1;
				view_canvas.first_line -= code;
				for (int i=code; i >= 0; i--)
					navigateUp(i==0);
				evt.consume();
				break;
			case KeyEvent.VK_PAGE_DOWN:
				code = view_canvas.last_visible.geometry.lineno - view_canvas.first_visible.geometry.lineno -1;
				view_canvas.first_line += code;
				for (int i=code; i >= 0; i--)
					navigateDown(i==0);
				evt.consume();
				break;
			case KeyEvent.VK_E:
				if (cur_elem instanceof DrawNodeTerm) {
					Object obj = ((DrawNodeTerm)cur_elem).getTextObject();
					if (obj instanceof Symbol) {
						edit_offset = 0;
						mode_edit = true;
						view_canvas.cursor_offset = edit_offset;
						view_canvas.repaint();
					}
				}
				evt.consume(); 
				break;
/*			case KeyEvent.VK_I:{
				MDrawable nt = cur_elem.getDrwParent();
				if (Type.isA(nt, World.theTypeOfNonTermStructs)) {
					MNode src = (MNode)nt.getn(World._attr_nt_struct_src);
					if (src instanceof MType) {
						MValue val = Type.cast(src).instantiate(World.getWorld((MNode)the_root));
						if (val != null) {
							List lst = World.theMeta_container;
							lst.add(val);
							formatAndPaint();
							Verificator.verify();
						}
					}
				}
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_N:{
				MDrawable nt = cur_elem.getDrwParent();
				if (Type.isA(nt, World.theTypeOfNonTermStructs) && cur_elem instanceof MToken &&
						Type.isA(cur_elem, World.theTypeOfTerminals))
				{
					MNode parent = (MNode)nt.getn(World._attr_nt_struct_src);
					MNode src  = ((MTerminal)cur_elem).src;
					MAttr attr = ((MTerminal)cur_elem).getAttr();
					MType type = (MType)attr.getn(World._attr_attr_type);
					if (src.getn(attr) == null && type != null && attr.getDefinitionMode()==Attr.ATTR_DEFINITION_REQUIRED) {
						MValue val = Type.cast(type).instantiate(World.getWorld((MNode)the_root));
						if (val != null) {
							val.init(parent, attr);
							formatAndPaint();
							Verificator.verify();
						}
					}
				}
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_A:{
				MDrawable nt = cur_elem.getDrwParent();
				if (Type.isA(nt, World.theTypeOfNonTermArrays)) {
					List src = (List)nt.getn(World._attr_nt_arr_src);
					int idx = cur_elem.pslot_in_src;
					if (idx < 0) idx = 0;
					if (idx > src.size()) idx = src.size();
					if (src.of_definitions) {
						MType tp = (MType)World.getType(src).representation_node.getn(World._attr_list_element_type);
						if (tp.hashed_type.getDirectChildren().length == 0) {
							MNode n = MNode.newMNode(tp, src, idx);
							formatAndPaint();
							Verificator.verify();
						} else {
							// build a menu of types to instantiate
							JPopupMenu m = buildTypeSelectPopupMenu(tp.hashed_type, src, idx);
							m.show(view_canvas, cur_elem.x, cur_elem.y + cur_elem.h);
						}
					}
				}
				}
				evt.consume(); 
				break;
*/			}
		}
		else if (mask == KeyEvent.ALT_DOWN_MASK && !mode_edit) {
			switch (code) {
			case KeyEvent.VK_UP:
				if (cur_elem.isAttached()) {
					view_canvas.current = cur_elem = cur_elem.parent();
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
				formatAndPaint();
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
//						parent_window.export_view.formatAndPaint();
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
					formatAndPaint();
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
						parent_window.clip_view.formatAndPaint();
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
								formatAndPaint();
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
								formatAndPaint();
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
				formatAndPaint();
				System.gc();
				System.gc();
				Verificator.verify();
				evt.consume(); 
				break;
			}
		}
*/		else if (mode_edit) {
			Symbol symbol = (Symbol)((DrawNodeTerm)cur_elem).getTextObject();
			String text = symbol.sname.toString();
			evt.consume();
			switch (code) {
			case KeyEvent.VK_LEFT:
				if (edit_offset > 0)
					edit_offset--;
				break;
			case KeyEvent.VK_RIGHT:
				if (edit_offset < text.length())
					edit_offset++;
				break;
			case KeyEvent.VK_ENTER:
				edit_offset = -1;
				mode_edit = false;
				break;
			case KeyEvent.VK_DELETE:
				if (edit_offset < text.length()) {
					text = text.substring(0, edit_offset)+
					       text.substring(edit_offset+1);
					symbol.sname = text;
				}
				break;
			case KeyEvent.VK_BACK_SPACE:
				if (edit_offset > 0) {
					edit_offset--;
					text = text.substring(0, edit_offset)+
					       text.substring(edit_offset+1);
					symbol.sname = text;
				}
				break;
			default:
				if (evt.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
					text = text.substring(0, edit_offset)+
					       evt.getKeyChar()+
						   text.substring(edit_offset);
					edit_offset++;
					symbol.sname = text;
				}
			}
			view_canvas.cursor_offset = edit_offset;
			formatAndPaint();
		}
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
	private void navigatePrev(boolean repaint) {
		DrawTerm prev = cur_elem.getFirstLeaf().getPrevLeaf();
		if (prev != null) {
			view_canvas.current = cur_elem = prev;
			cur_x = prev.geometry.x;
		}
		if (repaint) {
			makeCurrentVisible();
			formatAndPaint();
		}
	}
	private void navigateNext(boolean repaint) {
		DrawTerm next = cur_elem.getFirstLeaf().getNextLeaf();
		if (next != null) {
			view_canvas.current = cur_elem = next;
			cur_x = next.geometry.x;
		}
		if (repaint) {
			makeCurrentVisible();
			formatAndPaint();
		}
	}
	private void navigateUp(boolean repaint) {
		DrawTerm n = null;
		DrawTerm prev = cur_elem.getFirstLeaf();
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
			if (n.geometry.x <= cur_x && n.geometry.x+n.geometry.w >= cur_x)
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.geometry.do_newline > 0)
				break;
			if (prev.geometry.x+prev.geometry.w < cur_x)
				break;
			n = prev;
		}
		if (n != null) {
			view_canvas.current = cur_elem = n;
		}
		if (repaint) {
			makeCurrentVisible();
			formatAndPaint();
		}
	}
	private void navigateDown(boolean repaint) {
		DrawTerm n = null;
		DrawTerm next = cur_elem.getFirstLeaf();
		while (next != null) {
			if (next.geometry.do_newline > 0) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			if (n.geometry.x <= cur_x && n.geometry.x+n.geometry.w >= cur_x)
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.geometry.x > cur_x)
				break;
			if (next.geometry.do_newline > 0)
				break;
			n = next;
		}
		if (n != null) {
			view_canvas.current = cur_elem = n;
		}
		if (repaint) {
			makeCurrentVisible();
			formatAndPaint();
		}
	}
	private void navigateLineHome(boolean repaint) {
		int lineno = cur_elem.getFirstLeaf().geometry.lineno;
		for (;;) {
			DrawTerm dr = cur_elem.getPrevLeaf();
			if (dr == null || dr.geometry.lineno != lineno)
				break;
			cur_elem = dr;
		}
		view_canvas.current = cur_elem;
		cur_x = cur_elem.geometry.x;
		if (repaint)
			formatAndPaint();
	}
	private void navigateLineEnd(boolean repaint) {
		int lineno = cur_elem.getFirstLeaf().geometry.lineno;
		for (;;) {
			DrawTerm dr = cur_elem.getNextLeaf();
			if (dr == null || dr.geometry.lineno != lineno)
				break;
			cur_elem = dr;
		}
		view_canvas.current = cur_elem;
		cur_x = cur_elem.geometry.x;
		if (repaint)
			formatAndPaint();
	}
	
	private void makeCurrentVisible() {
		int top_lineno = view_canvas.first_visible.geometry.lineno;
		int bot_lineno = view_canvas.last_visible.geometry.lineno;
		int height = bot_lineno - top_lineno;
		
		if (top_lineno > 0 && cur_elem.getFirstLeaf().geometry.lineno <= top_lineno) {
			view_canvas.first_line = cur_elem.getFirstLeaf().geometry.lineno -1;
		}
		if (bot_lineno < view_canvas.num_lines && cur_elem.getFirstLeaf().geometry.lineno >= bot_lineno) {
			view_canvas.first_line = cur_elem.getFirstLeaf().geometry.lineno - height + 1;
		}
		if (view_canvas.first_line < 0)
			view_canvas.first_line = 0;
	}

	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY() + view_canvas.translated_y;
		Drawable dr = view_canvas.first_visible;
		for (; dr != null && dr != view_canvas.last_visible; dr = dr.getNextLeaf()) {
			if (dr.geometry.x < x && dr.geometry.y < y && dr.geometry.x+dr.geometry.w >= x && dr.geometry.y+dr.geometry.h >= y) {
				cur_elem = dr;
				cur_x = cur_elem.geometry.x;
				formatAndPaint();
				break;
			}
		}
	}
/*
	class AddNewNodeToListAction extends TextAction {
		private final Class				cls;
		private final ASTNode[]			lst;
		private final int				index;
		AddNewNodeToListAction(Class cls, NArr<ASTNode> lst, int index) {
			super(cls.getName());
			this.cls   = cls;
			this.lst   = lst;
			this.index = index;
		}
		public void actionPerformed(ActionEvent e) {
			ASTNode node = cls.newInstance();
			lst.insert(node, index);
			formatAndPaint();
		}
	}
*/
}
