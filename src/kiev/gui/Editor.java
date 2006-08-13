package kiev.gui;

import kiev.Kiev;
import kiev.Compiler;
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

import java.awt.datatransfer.*;

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
	public final CurElem	cur_elem;
	/** The object in clipboard */
	public final Clipboard	clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	
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
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_N),         new NewElemEditor(this,NewElemEditor.SETNEW_HERE));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_A),         new NewElemEditor(this,NewElemEditor.INSERT_NEXT));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_F),         new FolderTrigger(this));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_O),         new OptionalTrigger(this));
	}
	
	public Editor(Window window, TextSyntax syntax, Canvas view_canvas) {
		super(window, syntax, view_canvas);
		cur_elem = new CurElem();
	}
	
	public void setRoot(ANode root) {
		super.setRoot(root);
		cur_elem.set(view_root.getFirstLeaf());
	}
	
	public void setSyntax(TextSyntax syntax) {
		super.setSyntax(syntax);
		cur_elem.restore();
	}

	public void formatAndPaint(boolean full) {
		if (cur_elem == null)
			return;
		cur_elem.restore();
		view_canvas.current = cur_elem.dr;
		if (full) {
			this.formatter.setWidth(view_canvas.getWidth());
			view_canvas.root = null;
			if (the_root != null && full)
				view_canvas.root = view_root = formatter.format(the_root, view_root);
			cur_elem.restore();
		}
		view_canvas.repaint();
		ANode src = cur_elem.dr != null ? cur_elem.dr.node : null;
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
			//case KeyEvent.VK_UP:
			//	if (cur_elem.dr.isAttached()) {
			//		cur_elem.set((Drawable)cur_elem.parent());
			//		view_canvas.repaint();
			//	}
			//	evt.consume(); 
			//	break;
			case KeyEvent.VK_DOWN:
				if (cur_elem.dr instanceof DrawNonTerm) {
					cur_elem.set(cur_elem.dr.getFirstLeaf());
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_C:
				evt.consume();
				System.out.println("Running backend compiler...");
				try {
					Kiev.errCount = 0;
					Compiler.runBackEnd(null);
				} catch (Throwable t) { t.printStackTrace(); }
				System.out.println("Backend compiler completed with "+Kiev.errCount+" error(s)");
				foreach (FileUnit fu; Env.root.files) {
					walkTree(new TreeWalker() {
						public boolean pre_exec(ANode n) { if (n instanceof ASTNode) { n.compileflags &= 0xFFFF0000; } return true; }
					});
				}
				formatAndPaint(true);
				break;
			case KeyEvent.VK_V:
				evt.consume();
				if (the_root instanceof ASTNode) {
					System.out.println("Running frontend compiler...");
					Transaction tr = Transaction.open();
					changes.push(tr);
					try {
						Kiev.errCount = 0;
						Kiev.runProcessorsOn((ASTNode)the_root);
					} catch (Throwable t) { t.printStackTrace(); }
					System.out.println("Frontend compiler completed with "+Kiev.errCount+" error(s)");
					if (tr.isEmpty()) {
						tr.close();
						changes.pop();
					} else {
						tr.close();
					}
					formatAndPaint(true);
				}
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
			case KeyEvent.VK_W:
				if (item_editor != null) {
					stopItemEditor(true);
					item_editor = null;
				}
				parent_window.closeEditor(this);
				break;
			case KeyEvent.VK_X:
				if (cur_elem.dr != null) {
					ANode node = cur_elem.dr.node;
					changes.push(Transaction.open());
					node.detach();
					changes.peek().close();
					TransferableANode tr = new TransferableANode(node);
					clipboard.setContents(tr, tr);
					formatAndPaint(true);
				}
				break;
			case KeyEvent.VK_C:
				if (cur_elem.dr instanceof DrawNodeTerm) {
					AttrPtr pattr = ((DrawNodeTerm)cur_elem.dr).getAttrPtr();
					Object obj = pattr.get();
					Transferable tr = null;
					if (obj instanceof ANode)
						tr = new TransferableANode((ANode)obj);
					else
						tr = new StringSelection(String.valueOf(obj));
					clipboard.setContents(tr, (ClipboardOwner)tr);
				} else {
					Transferable tr = new TransferableANode(cur_elem.dr.node);
					clipboard.setContents(tr, (ClipboardOwner)tr);
				}
				break;
			case KeyEvent.VK_A:
				{
					Transferable content = clipboard.getContents(null);
					if (content.isDataFlavorSupported(TransferableANode.transferableANodeFlavor)) {
						ANode node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
						Drawable dr = cur_elem.dr;
						while (dr != null && !(dr.parent() instanceof DrawNonTermList))
							dr = (Drawable)dr.parent();
						if (dr != null && dr.parent() instanceof DrawNonTermList) {
							DrawNonTermList lst = (DrawNonTermList)dr.parent();
							SyntaxList slst = (SyntaxList)lst.syntax;
							if (node.isAttached())
								node = node.ncopy();
							SpacePtr sptr = lst.node.getSpacePtr(slst.name);
							if (sptr.slot.typeinfo.$instanceof(node)) {
								int idx = lst.getInsertIndex(dr) + 1;
								if (idx < 0)
									idx = 0;
								else if (idx > sptr.length)
									idx = sptr.length;
								changes.push(Transaction.open());
								try {
									sptr.slot.insert(lst.node,idx,node);
								} finally {
									changes.peek().close();
								}
								this.formatAndPaint(true);
							}
						}
					}
				}
				break;
			case KeyEvent.VK_V:
				{
					Transferable content = clipboard.getContents(null);
					if (content.isDataFlavorSupported(TransferableANode.transferableANodeFlavor)) {
						ANode node = (ANode)content.getTransferData(TransferableANode.transferableANodeFlavor);
						Drawable dr = cur_elem.dr;
						if (dr instanceof DrawNodeTerm) {
							AttrPtr pattr;
							if (dr.node instanceof SymbolRef)
								pattr = new AttrPtr(dr.node.parent(),dr.node.pslot());
							else
								pattr = ((DrawNodeTerm)dr).getAttrPtr();
							if (pattr.slot.typeinfo.clazz == SymbolRef.class) {
								DNode dn = null;
								if (node instanceof Symbol)
									dn = (DNode)node.parent();
								else if (node instanceof DNode)
									dn = (DNode)node;
								if (dn != null) {
									changes.push(Transaction.open());
									try {
										SymbolRef obj = (SymbolRef)pattr.get();
										if (obj != null) {
											obj.open();
											obj.name = dn.id.sname;
											obj.symbol = dn.id;
										} else {
											pattr.node.open();
											obj = (SymbolRef)pattr.slot.typeinfo.newInstance();
											obj.symbol = dn.id;
											pattr.set(obj);
										}
									} finally {
										changes.peek().close();
									}
									this.formatAndPaint(true);
									return;
								}
							}
						}
						while (dr != null && !(dr.parent() instanceof DrawNonTermList))
							dr = (Drawable)dr.parent();
						if (dr != null && dr.parent() instanceof DrawNonTermList) {
							if (node.isAttached())
								node = node.ncopy();
							DrawNonTermList lst = (DrawNonTermList)dr.parent();
							SyntaxList slst = (SyntaxList)lst.syntax;
							SpacePtr sptr = lst.node.getSpacePtr(slst.name);
							if (sptr.slot.typeinfo.$instanceof(node)) {
								int idx = lst.getInsertIndex(dr);
								if (idx < 0)
									idx = 0;
								else if (idx > sptr.length)
									idx = sptr.length;
								changes.push(Transaction.open());
								try {
									sptr.slot.insert(lst.node,idx,node);
								} finally {
									changes.peek().close();
								}
								this.formatAndPaint(true);
							}
						}
					}
				}
				break;
			case KeyEvent.VK_R:
				setSyntax(this.syntax);
				cur_elem.set(view_root.getFirstLeaf());
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
	
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY() + view_canvas.translated_y;
		DrawTerm dr = view_canvas.first_visible;
		for (; dr != null; dr = dr.getNextLeaf()) {
			if (dr.x < x && dr.y < y && dr.x+dr.w >= x && dr.y+dr.h >= y) {
				cur_elem.set(dr);
				cur_x = cur_elem.dr.x;
				formatAndPaint(false);
				break;
			}
			if (dr == view_canvas.last_visible)
				break;
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
							if (n instanceof DrawNodeTerm && n.getAttrPtr().get() == node || n.node == node) {
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
		int top_lineno = view_canvas.first_visible.lineno;
		int bot_lineno = view_canvas.last_visible.lineno;
		int height = bot_lineno - top_lineno;
		
		if (top_lineno > 0 && cur_elem.dr.getFirstLeaf().lineno <= top_lineno) {
			view_canvas.first_line = cur_elem.dr.getFirstLeaf().lineno -1;
		}
		if (bot_lineno < view_canvas.num_lines && cur_elem.dr.getFirstLeaf().lineno >= bot_lineno) {
			view_canvas.first_line = cur_elem.dr.getFirstLeaf().lineno - height + 1;
		}
		if (view_canvas.first_line < 0)
			view_canvas.first_line = 0;
	}

	final class CurElem {
		DrawTerm		dr;
		int				x, y;
		Drawable[]		path = Drawable.emptyArray;
	
		void set(DrawTerm dr) {
			Editor.this.view_canvas.current = dr;
			this.dr = dr;
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
				if (path[i].ctx_root == root)
					last = path[i];
				else
					bad = path[i];
			}
			set(last.getFirstLeaf());
			return;
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
		DrawTerm prev = uiv.cur_elem.dr.getFirstLeaf().getPrevLeaf();
		if (prev != null) {
			uiv.cur_elem.set(prev);
			uiv.cur_x = prev.x;
		}
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateNext(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm next = uiv.cur_elem.dr.getFirstLeaf().getNextLeaf();
		if (next != null) {
			uiv.cur_elem.set(next);
			uiv.cur_x = next.x;
		}
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateUp(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm n = null;
		DrawTerm prev = uiv.cur_elem.dr.getFirstLeaf();
		if (prev != null)
			prev = prev.getPrevLeaf();
		while (prev != null) {
			if (prev.do_newline > 0) {
				n = prev;
				break;
			}
			prev = prev.getPrevLeaf();
		}
		while (n != null) {
			if (n.x <= uiv.cur_x && n.x+n.w >= uiv.cur_x)
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.do_newline > 0)
				break;
			if (prev.x+prev.w < uiv.cur_x)
				break;
			n = prev;
		}
		if (n != null)
			uiv.cur_elem.set(n);
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateDn(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		DrawTerm n = null;
		DrawTerm next = uiv.cur_elem.dr.getFirstLeaf();
		while (next != null) {
			if (next.do_newline > 0) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			if (n.x <= uiv.cur_x && n.x+n.w >= uiv.cur_x)
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.x > uiv.cur_x)
				break;
			if (next.do_newline > 0)
				break;
			n = next;
		}
		if (n != null)
			uiv.cur_elem.set(n);
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateLineHome(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		int lineno = uiv.cur_elem.dr.getFirstLeaf().lineno;
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getPrevLeaf();
			if (dr == null || dr.lineno != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.x;
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigateLineEnd(boolean repaint) {
		final Editor uiv = (Editor)this.uiv;
		int lineno = uiv.cur_elem.dr.getFirstLeaf().lineno;
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getNextLeaf();
			if (dr == null || dr.lineno != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.x;
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigatePageUp() {
		final Editor uiv = (Editor)this.uiv;
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.first_line = 0;
			return;
		}
		int offs = uiv.view_canvas.last_visible.lineno - uiv.view_canvas.first_visible.lineno -1;
		uiv.view_canvas.first_line -= offs;
		for (int i=offs; i >= 0; i--)
			navigateUp(i==0);
		return;
	}
	private void navigatePageDn() {
		final Editor uiv = (Editor)this.uiv;
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.first_line = 0;
			return;
		}
		int offs = uiv.view_canvas.last_visible.lineno - uiv.view_canvas.first_visible.lineno -1;
		uiv.view_canvas.first_line += offs;
		for (int i=offs; i >= 0; i--)
			navigateDn(i==0);
		return;
	}

}

final class ChooseItemEditor implements KeyHandler {

	private final Editor	editor;

	ChooseItemEditor(Editor editor) {
		this.editor = editor;
	}

	public void process() {
		Drawable dr = editor.cur_elem.dr;
		if (dr instanceof DrawNodeTerm) {
			DrawNodeTerm dt = (DrawNodeTerm)dr;
			AttrPtr pattr = dt.getAttrPtr();
			Object obj = pattr.get();
			if (obj instanceof Symbol)
				editor.startItemEditor((Symbol)obj, new SymbolEditor((Symbol)obj, editor, dt));
			else if (obj instanceof SymbolRef)
				editor.startItemEditor((SymbolRef)obj, new SymRefEditor((SymbolRef)obj, editor, dt));
			else if (obj instanceof String || obj == null && pattr.slot.typeinfo.clazz == String.class) {
				if (pattr.node instanceof SymbolRef)
					editor.startItemEditor((SymbolRef)pattr.node, new SymRefEditor((SymbolRef)pattr.node, editor, dt));
				else
					editor.startItemEditor(pattr.node, new StrEditor(pattr, editor, dt));
			}
			else if (obj instanceof Integer)
				editor.startItemEditor(pattr.node, new IntEditor(pattr, editor, dt));
			else if (obj instanceof Boolean)
				editor.startItemEditor(pattr.node, new BoolEditor(pattr, editor));
			else if (obj instanceof ConstIntExpr)
				editor.startItemEditor((ConstIntExpr)obj, new IntEditor(obj.getAttrPtr("value"), editor, dt));
			else if (Enum.class.isAssignableFrom(pattr.slot.typeinfo.clazz))
				editor.startItemEditor(pattr.node, new EnumEditor(pattr, dt, editor));
		}
		else if (dr.parent() instanceof DrawEnumChoice) {
			DrawEnumChoice dec = (DrawEnumChoice)dr.parent();
			SyntaxEnumChoice stx = (SyntaxEnumChoice)dec.syntax;
			editor.startItemEditor(dec.node, new EnumEditor(dec.node.getAttrPtr(stx.name), dr.getFirstLeaf(), editor));
		}
	}
}

final class FolderTrigger implements KeyHandler {

	private final Editor	editor;

	FolderTrigger(Editor editor) {
		this.editor = editor;
	}

	public void process() {
		for (Drawable dr = editor.cur_elem.dr; dr != null; dr = (Drawable)dr.parent()) {
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
		ANode n = editor.cur_elem.dr;
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

	static final int SETNEW_HERE = 0;
	static final int INSERT_NEXT = 1;

	final Editor		editor;
	final int			mode;
	      int			idx;
	      JPopupMenu	menu;

	NewElemEditor(Editor editor, int mode) {
		this.editor = editor;
		this.mode = mode;
	}

	public void process() {
		Drawable dr = editor.cur_elem.dr;
		if (mode == SETNEW_HERE) {
			if (dr instanceof DrawNodeTerm && (dr.node == null || dr.getAttrPtr().get() == null)) {
				ANode n = dr.node;
				while (n == null) {
					dr = (Drawable)dr.parent();
					n = dr.node;
				}
				SyntaxAttr satt = (SyntaxAttr)dr.syntax;
				if (satt.expected_types.length > 0) {
					menu = new JPopupMenu("Set new item");
					foreach (SymbolRef sr; satt.expected_types; sr.dnode instanceof Struct) {
						menu.add(new JMenuItem(new NewElemAction((Struct)sr.dnode, n, satt.name)));
					}
					int x = editor.cur_elem.dr.x;
					int y = editor.cur_elem.dr.y + editor.cur_elem.dr.h - editor.view_canvas.translated_y;
					menu.addPopupMenuListener(this);
					menu.show(editor.view_canvas, x, y);
					editor.startItemEditor(n, this);
				}
				return;
			}
			while (dr != null && !(dr.parent() instanceof DrawNonTermList))
				dr = (Drawable)dr.parent();
			if (dr.parent() instanceof DrawNonTermList) {
				DrawNonTermList lst = (DrawNonTermList)dr.parent();
				SyntaxList slst = (SyntaxList)lst.syntax;
				if (slst.expected_types.length > 0) {
					this.idx = lst.getInsertIndex(dr);
					if (slst.expected_types.length > 0) {
						menu = new JPopupMenu("Insert new item");
						foreach (SymbolRef sr; slst.expected_types; sr.dnode instanceof Struct) {
							menu.add(new JMenuItem(new NewElemAction((Struct)sr.dnode, lst.node, slst.name)));
						}
						int x = editor.cur_elem.dr.x;
						int y = editor.cur_elem.dr.y + editor.cur_elem.dr.h - editor.view_canvas.translated_y;
						menu.addPopupMenuListener(this);
						menu.show(editor.view_canvas, x, y);
						editor.startItemEditor(lst.node, this);
					}
				}
				return;
			}
		}
		else if (mode == INSERT_NEXT) {
			while (dr != null && !(dr.parent() instanceof DrawNonTermList))
				dr = (Drawable)dr.parent();
			if (dr != null && dr.parent() instanceof DrawNonTermList) {
				DrawNonTermList lst = (DrawNonTermList)dr.parent();
				SyntaxList slst = (SyntaxList)lst.syntax;
				if (slst.expected_types.length > 0) {
					this.idx = lst.getInsertIndex(dr) + 1;
					if (slst.expected_types.length > 0) {
						menu = new JPopupMenu("Append new item");
						foreach (SymbolRef sr; slst.expected_types; sr.dnode instanceof Struct) {
							menu.add(new JMenuItem(new NewElemAction((Struct)sr.dnode, lst.node, slst.name)));
						}
						int x = editor.cur_elem.dr.x;
						int y = editor.cur_elem.dr.y + editor.cur_elem.dr.h - editor.view_canvas.translated_y;
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
			foreach (AttrSlot a; node.values(); a.name == attr) {
				try {
					ANode obj = (ANode)Class.forName(cls.qname()).newInstance();
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
	protected final DrawTerm	dr_term;
	protected       int			edit_offset;
	protected       boolean		in_combo;
	protected       JComboBox	combo;

	TextEditor(Editor editor, DrawTerm dr_term) {
		this.editor = editor;
		this.dr_term = dr_term;
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
				text = text.substring(0, edit_offset)+evt.getKeyChar()+text.substring(edit_offset);
				edit_offset++;
				this.setText(text);
			}
		}
		editor.view_canvas.cursor_offset = edit_offset+prefix_offset;
		editor.formatAndPaint(true);
	}
}

final class SymbolEditor extends TextEditor {
	
	private final Symbol	symbol;

	SymbolEditor(Symbol symbol, Editor editor, DrawTerm dr_term) {
		super(editor, dr_term);
		this.symbol = symbol;
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.view_canvas.cursor_offset = edit_offset + dr_term.getPrefix().length();
		}
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

	SymRefEditor(SymbolRef<DNode> symref, Editor editor, DrawTerm dr_term) {
		super(editor, dr_term);
		this.symref = symref;
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.view_canvas.cursor_offset = edit_offset + dr_term.getPrefix().length();
		}
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
			combo.addItem(dn.id.sname);
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

	StrEditor(AttrPtr pattr, Editor editor, DrawTerm dr_term) {
		super(editor, dr_term);
		this.pattr = pattr;
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.view_canvas.cursor_offset = edit_offset + dr_term.getPrefix().length();
		}
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

	IntEditor(AttrPtr pattr, Editor editor, DrawTerm dr_term) {
		super(editor, dr_term);
		this.pattr = pattr;
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

final class BoolEditor implements KeyListener {
	
	private final Editor	editor;
	private final AttrPtr	pattr;

	BoolEditor(AttrPtr pattr, Editor editor) {
		this.editor = editor;
		this.pattr = pattr;
	}
	
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask != 0)
			return;
		evt.consume();
		switch (code) {
		case KeyEvent.VK_SPACE:
			Boolean val = (Boolean)pattr.get();
			if (val.booleanValue())
				pattr.set(Boolean.FALSE);
			else
				pattr.set(Boolean.TRUE);
			editor.stopItemEditor(false);
			break;
		case KeyEvent.VK_T:
			pattr.set(Boolean.TRUE);
			editor.stopItemEditor(false);
			break;
		case KeyEvent.VK_F:
			pattr.set(Boolean.FALSE);
			editor.stopItemEditor(false);
			break;
		case KeyEvent.VK_ENTER:
			editor.stopItemEditor(false);
			return;
		case KeyEvent.VK_ESCAPE:
			editor.stopItemEditor(true);
			return;
		}
		editor.formatAndPaint(true);
	}
}

class EnumEditor implements KeyListener, PopupMenuListener {
	private final Editor		editor;
	private final AttrPtr		pattr;
	private final JPopupMenu	menu;
	EnumEditor(AttrPtr pattr, DrawTerm cur_elem, Editor editor) {
		this.editor = editor;
		this.pattr = pattr;
		menu = new JPopupMenu();
		EnumSet ens = EnumSet.allOf(pattr.slot.typeinfo.clazz);
		foreach (Enum e; ens.toArray())
			menu.add(new JMenuItem(new SetSyntaxAction(e)));
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

