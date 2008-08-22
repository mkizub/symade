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

import kiev.fmt.DrawCtrl;
import kiev.fmt.DrawElemWrapper;
import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawNonTermList;
import kiev.fmt.DrawOptional;
import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxElem;
import kiev.fmt.Draw_SyntaxElemDecl;
import kiev.fmt.Draw_SyntaxFunction;
import kiev.fmt.Draw_SyntaxSet;
import kiev.fmt.Drawable;
import kiev.fmt.ExpectedAttrTypeInfo;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.gui.event.InputEvent;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.SpaceAttrSlot;
import kiev.vtree.Transaction;
import kiev.vtree.TreeWalker;

public class Editor extends InfoView implements ElementChangeListener {
	
	/** Symbols used by editor */	
	private Runnable	item_editor;
	
	/** Current editor mode */
	public boolean insert_mode;
	
	/** Current x position for scrolling up/down */
	public int cur_x;
	
	/** Current item */
	private final CurElem	cur_elem;
	
	public java.util.Stack<Transaction>		changes = new java.util.Stack<Transaction>();
	
	public Editor(IWindow window, ICanvas view_canvas, Draw_ATextSyntax syntax) {
		super(window, view_canvas, syntax);
		this.show_placeholders = true;
		cur_elem = new CurElem();
	}
	
	@Override
	public void setRoot(ANode root) {
		super.setRoot(root);
		cur_elem.set(view_root.getFirstLeaf());
		
	}
	
	@Override
	public void setSyntax(Draw_ATextSyntax syntax) {
		super.setSyntax(syntax);
		cur_elem.restore();
	}

	@Override
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
			else if (p instanceof DrawElemWrapper) {
				slot = ((DrawElemWrapper)p).sub_attr;
				if (slot instanceof SpaceAttrSlot)
					return new ActionPoint(p,(SpaceAttrSlot)slot,((DrawElemWrapper)p).getInsertIndex(dr, next));
				else if (slot instanceof ExtSpaceAttrSlot)
					return new ActionPoint(p,(ExtSpaceAttrSlot)slot,((DrawElemWrapper)p).getInsertIndex(dr, next));
			}
			dr = p;
		}
		return null;
	}

	@Override
	public boolean inputEvent(InputEvent evt) {
		UIActionFactory[] actions = UIManager.getUIActions(this).get(evt);
		if (actions == null)
			return false;
		Draw_SyntaxFunction[] funcs = null;
		if (cur_elem.dr != null && cur_elem.dr.syntax.funcs != null)
			funcs = cur_elem.dr.syntax.funcs;
		for (UIActionFactory af: actions) {
			if (funcs != null) {
				for (Draw_SyntaxFunction f: funcs) {
					if (af.getClass().getName().equals(f.act)) {
						Drawable dr = getFunctionTarget(f);
						if (dr != null) {
							Runnable r = af.getAction(new UIActionViewContext(this.parent_window, evt, this, dr));
							if (r != null) {
								r.run();
								return true;
							}
						}
					}
				}
			}
			Runnable r = af.getAction(new UIActionViewContext(parent_window, evt, this));
			if (r != null) {
				r.run();
				return true;
			}
		}
		return false;
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
			Drawable x = checkFunctionTarget(attr, dr);
			if (x != null) {
				dr = x;
				continue next_attr;
			}
			Drawable parent =  (Drawable)dr.parent();
			if (parent != null) {
				for (Drawable d: parent.getChildren()) {
					x = checkFunctionTarget(attr, d);
					if (x != null) {
						dr = x;
						continue next_attr;
					}
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
		if (dr.syntax.elem_decl != null && dr.syntax.elem_decl.attr_types != null) {
			Draw_SyntaxElemDecl sted = dr.syntax.elem_decl;
			for (ExpectedAttrTypeInfo eti: sted.attr_types) {
				if (attr.equals(eti.attr_name))
					return dr;
			}
		}
		if (stx0 instanceof Draw_SyntaxSet && ((Draw_SyntaxSet)stx0).nested_function_lookup) {
			for (Drawable d: dr.getChildren()) 
				if( (x=checkFunctionTarget(attr, d)) != null)
					return x;
		}
		if (dr instanceof DrawOptional && (x=checkFunctionTarget(attr, ((DrawOptional)dr).getArg())) != null)
			return x;
		return null;
	}

	public void startItemEditor(Runnable item_editor) {
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
/*	
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
*/	
	public void selectDrawTerm(DrawTerm dr) {
		cur_elem.set(dr);
		cur_x = dr.getGfxFmtInfo().getX();
		formatAndPaint(false);
	}

	public void goToPath(ANode[] path) {
		if (view_root == null)
			return;
		try {
			for (final ANode node: path) {
				view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if (n instanceof Drawable) {
							if (n instanceof DrawNodeTerm && ((DrawNodeTerm)n).getAttrObject() == node || ((Drawable)n).drnode == node) {
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
			setNode(dr == null ? null : dr.drnode);
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
			if (dr.getCtx_root() == root)
				return;
			if (path.length == 0) {
				set(root.getFirstLeaf());
				return;
			}
			Drawable last = path[path.length-1];
			Drawable bad = null;
			for (int i=path.length-1; i >= 0 && bad == null; i--) {
				if (path[i].getCtx_root() == root && path[i].getFirstLeaf() != null)
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
	@Override
	public Runnable getItem_editor() {
		return item_editor;
	}

	/**
	 * @return the cur_elem
	 */
	public CurElem getCur_elem() {
		return cur_elem;
	}

}
