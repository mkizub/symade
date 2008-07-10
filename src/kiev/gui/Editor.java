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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;


import kiev.fmt.DrawCtrl;
import kiev.fmt.DrawFolded;
import kiev.fmt.DrawListWrapper;
import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawNonTermList;
import kiev.fmt.DrawOptional;
import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxElem;
import kiev.fmt.Draw_SyntaxFunction;
import kiev.fmt.Draw_SyntaxSet;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.SpaceAttrSlot;
import kiev.vtree.Transaction;
import kiev.vtree.TreeWalker;

public class Editor extends InfoView implements KeyListener, ElementChangeListener {
	
	/** Symbols used by editor */	
	private ItemEditor	item_editor;
	
	/** Current editor mode */
	public boolean insert_mode;
	
	/** Current x position for scrolling up/down */
	public int cur_x;
	
	/** Current item */
	private final CurElem	cur_elem;
	
	public java.util.Stack<Transaction>		changes = new java.util.Stack<Transaction>();
	
	protected final java.util.Hashtable<InputEventInfo,String[]> keyActionMap;

	{
		//final int SHIFT = KeyEvent.SHIFT_DOWN_MASK;
		final int CTRL  = KeyEvent.CTRL_DOWN_MASK;
		final int ALT   = KeyEvent.ALT_DOWN_MASK;

		this.naviMap.put(new InputEventInfo(ALT,				KeyEvent.VK_X),				UIManager.newExprEditActionsFlatten());

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_UP),			 NavigateView.newLineUp());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_DOWN),			 NavigateView.newLineDn());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_PAGE_UP),		 NavigateView.newPageUp());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_PAGE_DOWN),		NavigateView.newPageDn());

		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_LEFT),			 NavigateEditor.newGoPrev());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_RIGHT),			 NavigateEditor.newGoNext());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_UP),			 NavigateEditor.newGoLineUp());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_DOWN),			 NavigateEditor.newGoLineDn());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_HOME),			 NavigateEditor.newGoLineHome());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_END),			 NavigateEditor.newGoLineEnd());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_PAGE_UP),		 NavigateEditor.newGoPageUp());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_PAGE_DOWN),		 NavigateEditor.newGoPageDn());

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_Z),				EditActions.newUndo());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_C),				 EditActions.newCopy());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_X),				 EditActions.newCut());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_DELETE),		 EditActions.newDel());

		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_F),				UIManager.newFunctionExecutorFactory());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_F),				UIManager.newFunctionExecutorFactory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_O),				FolderTrigger.newFactory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_N),				UIManager.newNewElemHereFactory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_A),				UIManager.newNewElemNextFactory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_V),				UIManager.newPasteHereFactory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_B),				UIManager.newPasteNextFactory());
		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_E),				new ChooseItemEditor());
		
		this.keyActionMap = new java.util.Hashtable<InputEventInfo,String[]>();
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_E), new String[]{"kiev.gui.TextEditor$Factory",
			"kiev.gui.IntEditor$Factory","kiev.gui.EnumEditor$Factory","kiev.gui.AccessEditor$Factory","kiev.gui.ChooseItemEditor"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_O), new String[]{"kiev.gui.FolderTrigger$Factory"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_N), new String[]{"kiev.gui.NewElemHere$Factory"});
		this.keyActionMap.put(new InputEventInfo(0,KeyEvent.VK_A), new String[]{"kiev.gui.NewElemNext$Factory"});
	}
	
	public Editor(IWindow window, Draw_ATextSyntax syntax, ICanvas view_canvas) {
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
		if (insert_mode && view_canvas.getCursor_offset() < 0)
			view_canvas.setCursor_offset(0);
		else if (!insert_mode && item_editor == null && view_canvas.getCursor_offset() >= 0)
			view_canvas.setCursor_offset(-1);
		cur_elem.restore();
		view_canvas.setCurrent(cur_elem.dr, cur_elem.node);
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
			Runnable r = (af == null) ? null : af.getAction(new UIActionViewContext(parent_window, this));
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
					UIManager.doGUIBeep();
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

	public void startItemEditor(ItemEditor item_editor) {
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
		int y = e.getY() + view_canvas.getTranslated_y();
		GfxDrawTermLayoutInfo dr = view_canvas.getFirst_visible();
		GfxDrawTermLayoutInfo last = view_canvas.getLast_visible();
		for (; dr != null; dr = dr.getNext()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.x < x && dr.y < y && dr.x+w >= x && dr.y+h >= y) {
				break;
			}
			if (dr == last)
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
		int y = e.getY() + view_canvas.getTranslated_y();
		GfxDrawTermLayoutInfo dr = view_canvas.getFirst_visible();
		GfxDrawTermLayoutInfo last = view_canvas.getLast_visible();
		for (; dr != null; dr = dr.getNext()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.x < x && dr.y < y && dr.x+w >= x && dr.y+h >= y) {
				break;
			}
			if (dr == last)
				return;
		}
		if (dr == null)
			return;
		if (e.getButton() == MouseEvent.BUTTON3) {
			UIActionFactory af =  UIManager.newFunctionExecutorFactory();
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

	public void makeCurrentVisible() {
		try {
			int top_lineno = view_canvas.getFirst_visible().getLineNo();
			int bot_lineno = view_canvas.getLast_visible().getLineNo();
			int height = bot_lineno - top_lineno;
			int first_line = view_canvas.getFirst_line();
			
			if (top_lineno > 0 && cur_elem.dr.getFirstLeaf().getGfxFmtInfo().getLineNo() <= top_lineno)
				first_line = cur_elem.dr.getFirstLeaf().getGfxFmtInfo().getLineNo() -1;
			if (bot_lineno < view_canvas.getNum_lines() && cur_elem.dr.getFirstLeaf().getGfxFmtInfo().getLineNo() >= bot_lineno)
				first_line = cur_elem.dr.getFirstLeaf().getGfxFmtInfo().getLineNo() - height + 1;
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
			Editor.this.view_canvas.setCurrent(dr, node);
			if (dr != null) {
				GfxDrawTermLayoutInfo info = dr.getGfxFmtInfo();
				int w = info.getWidth();
				this.x = info.getX() + w / 2;
				this.y = info.getY();
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
		public void nodeUp() {
			if (node != null && node.parent() != null) {
				setNode(node.parent());
				Editor.this.view_canvas.setCurrent(null, node.parent());
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

	/**
	 * @return the item_editor
	 */
	public KeyListener getItem_editor() {
		return item_editor;
	}

	/**
	 * @param item_editor the item_editor to set
	 */
	public void setItem_editor(ItemEditor item_editor) {
		this.item_editor = item_editor;
	}

	/**
	 * @return the cur_elem
	 */
	public CurElem getCur_elem() {
		return cur_elem;
	}

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

	public static Factory newFactory(){
		return new Factory();
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Toggle folding"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			for (Drawable dr = editor.getCur_elem().dr; dr != null; dr = (Drawable)dr.parent()) {
				if (dr instanceof DrawFolded)
					return new FolderTrigger(editor, (DrawFolded)dr);
			}
			return null;
		}
	}
}

