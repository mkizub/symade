/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import kiev.Kiev;
import kiev.fmt.DrawCtrl;
import kiev.fmt.DrawTokenTerm;
import kiev.fmt.DrawValueTerm;
import kiev.fmt.DrawOptional;
import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.common.DrawLayoutInfo;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.fmt.common.Draw_FuncEval;
import kiev.fmt.common.Draw_FuncNewNode;
import kiev.fmt.common.Draw_SyntaxAttr;
import kiev.fmt.common.Draw_SyntaxElem;
import kiev.fmt.common.Draw_SyntaxElemDecl;
import kiev.fmt.common.Draw_SyntaxFunc;
import kiev.fmt.common.UIDrawPath;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.gui.event.InputEvent;
import kiev.vtree.AttrSlot;
import kiev.vtree.AutoCompleteResult;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.INode;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;
import kiev.vtree.ITreeWalker;
import kiev.vlang.FileUnit;

/**
 * The Editor.
 */
public class Editor extends UIView implements IEditor, ElementChangeListener {

	/** 
	 * The FileUnit showed in this editor 
	 */	
	private FileUnit the_file_unit;

	/** 
	 * The current editor mode.
	 */
	private boolean insert_mode;
	
	/**
	 * The current text attribute we edit
	 */
	private DrawValueTerm		edit_term;

	/** 
	 * The current X position for scrolling up/down.
	 */
	protected int cur_x;

	/** 
	 * The current item.
	 */
	private final CurElem	cur_elem;

	/**
	 * Current Element.
	 */
	private final class CurElem {
		class DrawPathElem {
			final INode		parent;	// the parent node 
			final AttrSlot	slot;	// the slot in the parent node
			final int		idx;	// the index in the slot (if it's a space slot)
			final INode		node;	// recorded value stored in the slot (if it's a child slot)
			final Drawable	dr;		// the drawable 
			public DrawPathElem(INode parent, AttrSlot slot, int idx, INode node, Drawable dr) {
				this.parent = parent;
				this.slot = slot;
				this.idx = idx;
				this.node = node;
				this.dr = dr;
				assert (dr.drnode == node);
			}
		}
		class FoundNodeException extends RuntimeException {
			final Drawable dr;
			FoundNodeException(Drawable dr) { this.dr = dr; }
		}

		/** Current (pointed) Draw Term. */
		private DrawTerm dr;
		/** Current Node. */
		private INode node;
		/** The Path of Drawables. */
		private DrawPathElem[] path = new DrawPathElem[0];

		/**
		 * Set Draw Term.
		 * @param dr DrawTerm
		 */
		void set(DrawTerm dr) {
			if (this.dr == dr) return;
			this.dr = dr;
			setNode(dr == null ? null : dr.drnode);
			Editor.this.getViewPeer().setCurrent(dr, node);
			// make the path
			Drawable d = dr;
			ArrayList<DrawPathElem> lst = new ArrayList<DrawPathElem>();
			while (d != null) {
				INode node = d.drnode;
				AttrSlot slot = node.pslot();
				INode parent = node.parent();
				int idx = 0;
				if (slot instanceof ASpaceAttrSlot)
					idx = ((ASpaceAttrSlot)slot).indexOf(parent, node);
				DrawPathElem dpe = new DrawPathElem(parent, slot, idx, node, d);
				lst.add(dpe);
				d = (Drawable)d.parent();
			}
			Collections.reverse(lst);
			path = lst.toArray(new DrawPathElem[lst.size()]);
		}

		/**
		 * Restore.
		 */
		void restore() {
			Drawable dr = Editor.this.view_root;
			if (dr == null)
				return;
			int i=0;
			next_drawable:
			for (; i < path.length; i++) {
				DrawPathElem dpe = path[i];
				if (dr == dpe.dr)
					continue;
				for (Drawable ch : dr.getChildren()) {
					if (ch == dpe.dr) {
						dr = ch;
						continue next_drawable;
					}
				}
				break;
			}
			if (i == path.length) {
				setDrawTerm(dr.getFirstLeaf());
				return;
			}
			//setDrawTerm(dr.getFirstLeaf());
			//return;
			// we lost the drawable's path, continue with nodes
			for (; i < path.length; i++) {
				DrawPathElem dpe = path[i];
				Drawable fnd = findDrawable(dr, dpe.node);
				if (fnd != null) {
					dr = fnd;
					continue;
				}
				fnd = findDrawable(dr, dpe.parent);
				if (fnd != null) {
					dr = fnd;
					int idx = dpe.idx;
					if (idx < 0) idx = 0;
					for (; idx >= 0; idx--) {
						try {
							Object val = dpe.parent.getVal(dpe.slot, dpe.idx);
							if (val instanceof INode) {
								fnd = findDrawable(dr, (INode)val);
								if (fnd != null) {
									dr = fnd;
									break;
								}
							}
						} catch (Exception e) {}
					}
				}
				break;
			}
			setDrawTerm(dr.getFirstLeaf());
			return;
		}
		
		private Drawable findDrawable(Drawable dr, final INode node) {
			try {
				dr.walkTree(null, null, new ITreeWalker() {
					public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
						if (n instanceof Drawable) {
							Drawable d = (Drawable)n;
							if (d.drnode == node)
								throw new FoundNodeException(d);
							return true;
						}
						return false;
					}
				});
			} catch (FoundNodeException fne) {
				return fne.dr;
			}
			return null;
		}

		/**
		 * Set Node.
		 * @param node the node to set
		 */
		void setNode(INode node) {
			if (this.node != node) {
				this.node = node;
				window.fireElementChanged(new ElementEvent(node, Editor.this));
			}
		}
	}

	/**
	 * The constructor.
	 * @param window the window
	 * @param view_canvas the canvas
	 * @param syntax the syntax
	 */
	public Editor(Window window, ICanvas view_canvas, Draw_ATextSyntax syntax) {
		super(window, view_canvas, syntax);
		this.show_placeholders = true;
		cur_elem = new CurElem();
	}

	public void setFileUnit(FileUnit root) {
		this.the_file_unit = root;
		super.setRoot(root, true);
		if (view_root != null)
			cur_elem.set(view_root.getFirstLeaf());
	}

	public FileUnit getFileUnit() {
		return this.the_file_unit;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIView#setRoot(kiev.vtree.INode)
	 */
	@Override
	public void setRoot(INode root, boolean format) {
		if (root != the_semantic_root)
			throw new RuntimeException("setRoot() for Editor must be setFileUnit()");
		super.setRoot(root, format);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIView#setSyntax(kiev.fmt.Draw_ATextSyntax)
	 */
	@Override
	public void setSyntax(Draw_ATextSyntax syntax) {
		Vector<INode> nodes = new Vector<INode>();
		INode n = getSelectedNode();
		while (n != null) {
			nodes.add(n);
			n = n.parent();
		}
		//UIDrawPath path = new UIDrawPath(nodes.toArray(new INode[nodes.size()]), getViewPeer().getCursor_offset());
		//setSelectedNode(null);
		super.setSyntax(syntax);
		//goToPath(path);
		cur_elem.restore();
	}

	/**
	 * Set currently selected node. 
	 * @return the selected node or null if nothing selected.
	 */
	public void setSelectedNode(INode node) {
		cur_elem.setNode(node);
	}

	/**
	 * Get currently selected node. 
	 * @return the selected node or null if nothing selected.
	 */
	public INode getSelectedNode() {
		return cur_elem.node;
	}

	/**
	 * Set currentl DrawTerm. 
	 */
	public void setDrawTerm(DrawTerm dt) {
		if (dt != getDrawTerm())
			stopTextEditMode();
		cur_elem.set(dt);
	}

	/**
	 * Get current DrawTerm under cursor. 
	 * @return the pointed DrawTerm or null.
	 */
	public DrawTerm getDrawTerm() {
		return cur_elem.dr;
	}

	private void execAction(UIAction action) {
		Kiev.setSemContext(window.currentEditorThreadGroup.semantic_context);
		try {
			action.exec();
		} finally {
			Kiev.setSemContext(null);
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIView#formatAndPaint(boolean)
	 */
	@Override
	public void formatAndPaint(boolean full) {
		if (cur_elem == null)
			return;
		if (insert_mode && getViewPeer().getCursor_offset() < 0 && getDrawTerm()  instanceof DrawValueTerm)
			getViewPeer().setCursor_offset(0);
		cur_elem.restore();
		getViewPeer().setCurrent(cur_elem.dr, cur_elem.node);
		if (full) {
			formatter.setWidth(getViewPeer().getImgWidth());
			getViewPeer().setDlb_root(null);
			if (getRoot() != null && full) {
				formatter.format(getRoot(), view_root, getSyntax(), getStyle());
				view_root = formatter.getRootDrawable();
				getViewPeer().setDlb_root(formatter.getRootDrawLayoutBlock());
			}
			cur_elem.restore();
		}
		getViewPeer().repaint();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IEditor#getActionPoint()
	 */
	public ActionPoint getActionPoint() {
		return new ActionPoint(this, cur_elem.dr);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIView#inputEvent(kiev.gui.event.InputEvent)
	 */
	@Override
	public boolean inputEvent(InputEvent evt) {
		UIActionFactory[] actions = UIManager.getUIActions(this).get(evt);
		UIActionViewContext avc = new UIActionViewContext(this.window, evt, this);
		if (actions != null) {
			Draw_SyntaxFunc[] funcs = null;
			Drawable dr = cur_elem.dr;
			if (dr != null && dr.syntax.elem_decl != null && dr.syntax.elem_decl.funcs != null)
				funcs = dr.syntax.elem_decl.funcs;
			for (UIActionFactory af: actions) {
				if (funcs != null) {
					for (Draw_SyntaxFunc f: funcs) {
						if (f instanceof Draw_FuncEval) {
							dr = getFunctionTarget(f);
							if (dr != null) {
								Draw_FuncEval fe = (Draw_FuncEval)f;
								UIAction action = null;
								try {
									action = ((UIActionFactory)Class.forName(fe.act).getDeclaredConstructor().newInstance()).getAction(avc);
								} catch (Exception e) {
									e.printStackTrace();
								}
								if (action != null) {
									execAction(action);
									return true;
								}
							}
						}
					}
				}
				UIAction action = af.getAction(new UIActionViewContext(window, evt, this));
				if (action != null) {
					execAction(action);
					return true;
				}
			}
		}
		if (insert_mode && evt.isKeyboardTyping() && !isInTextEditMode()) {
			UIAction edt = new EditActions.ChooseItemEditor().getAction(avc);
			if (edt != null) {
				execAction(edt);
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IEditor#getFunctionTarget(kiev.fmt.Draw_SyntaxFunction)
	 */
	public Drawable getFunctionTarget(Draw_SyntaxFunc sf) {
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

	/**
	 * Check Function Target.
	 * @param attr the String
	 * @param dr the Drawable
	 * @return the Drawable
	 */
	private Drawable checkFunctionTarget(String attr, Drawable dr) {
		if (dr == null) return null;
		Draw_SyntaxElem stx0 = dr.syntax;
		Drawable x;
		if (stx0 instanceof Draw_SyntaxAttr && attr.equals(((Draw_SyntaxAttr)stx0).name))
			return dr;
		if (dr.syntax.elem_decl != null && dr.syntax.elem_decl.funcs != null) {
			Draw_SyntaxElemDecl sted = dr.syntax.elem_decl;
			for (Draw_SyntaxFunc f: sted.funcs) {
				if (f instanceof Draw_FuncNewNode && ((Draw_FuncNewNode)f).checkApplicable(attr))
					return dr;
			}
		}
		//if (stx0 instanceof Draw_SyntaxSet && ((Draw_SyntaxSet)stx0).nested_function_lookup) {
		//	for (Drawable d: dr.getChildren()) 
		//		if( (x=checkFunctionTarget(attr, d)) != null) return x;
		//}
		if (dr instanceof DrawOptional && (x=checkFunctionTarget(attr, ((DrawOptional)dr).getArg())) != null) return x;
		return null;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IEditor#selectDrawTerm(kiev.fmt.DrawTerm)
	 */
	public void selectDrawTerm(DrawTerm dr) {
		cur_elem.set(dr);
		cur_x = dr.getGfxFmtInfo().getX();
		formatAndPaint(false);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IEditor#goToPath(kiev.vtree.INode[])
	 */
	public void goToPath(UIDrawPath path) {
		if (view_root == null)
			return;
		if (path.path.length > 0 && path.path[0] != getSelectedNode()) {
			token_scan:
			for (final INode node: path.path) {
				for (DrawTerm dt = view_root.getFirstLeaf(); dt != null; dt = dt.getNextLeaf()) {
					if (dt.drnode == node) {
						cur_elem.set(dt);
						cur_x = cur_elem.dr.getGfxFmtInfo().getX();
						break token_scan;
					}
				}
			}
		}
		
		DrawTerm dt = getDrawTerm();
		if (dt != edit_term)
			stopTextEditMode();
		if (dt != null && dt.isTextual() && path.cursor >= 0 ) {
			Object obj = dt.getTermObj();
			String text = "";
			if(obj instanceof String)
				text = (String)obj;
			if (path.cursor > text.length())
				getViewPeer().setCursor_offset(text.length());
			else
				getViewPeer().setCursor_offset(path.cursor);
		}
		makeCurrentVisible();
		formatAndPaint(false);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IEditor#makeCurrentVisible()
	 */
	public void makeCurrentVisible() {
		try {
			DrawLayoutInfo cur_gfx = cur_elem.dr.getFirstLeaf().getGfxFmtInfo();
			int bound = cur_gfx.height + 1;
			int first_y = getViewPeer().getVertOffset();
			int height = getViewPeer().getImgHeight();

			if (first_y > 0 && cur_gfx.getY() < first_y+bound)
				first_y = cur_gfx.getY() - bound;
			if (first_y < 0)
				first_y = 0;
			if (cur_gfx.getY() + cur_gfx.height >= first_y+height-bound)
				first_y = cur_gfx.getY() + cur_gfx.height - height + bound;
			if (first_y != getViewPeer().getVertOffset())
				getViewPeer().setVertOffset(first_y);
		} catch (NullPointerException e) {}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIView#elementChanged(kiev.gui.event.ElementEvent)
	 */
	@Override
	public void elementChanged(ElementEvent e) {
		window.enableMenuItems();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#formatAndPaintLater()
	 */
	public void formatAndPaintLater() {
		formatAndPaint(true);
	}

	public boolean isInInsertMode() {
		return insert_mode;
	}
	public void setInInsertMode(boolean value) {
		insert_mode = value;
		getWindow().updateStatusBar();
		formatAndPaint(false);
	}
	
	public boolean isInTextEditMode() {
		return edit_term != null;
	}
	
	public void startTextEditMode(DrawValueTerm term) {
		stopTextEditMode();
		edit_term = term;
		if (getViewPeer().getCursor_offset() < 0)
			getViewPeer().setCursor_offset(0);
		showAutoComplete(false);
		getWindow().updateStatusBar();
	}

	public void stopTextEditMode() {
		if (isInTextEditMode()) {
			edit_term = null;
			getWindow().updateStatusBar();
		}
	}

	//
	// Text editor interface
	//
	
	public void editTypeChar(final char ch) {
		int edit_offset = getEditOffset();
		if (edit_term instanceof DrawTokenTerm) {
			DrawTokenTerm tok_term = (DrawTokenTerm)edit_term;
			UIDrawPath path = null;
			if (ch == 127) {	// del
				path = tok_term.delChar(edit_offset, false);
			}
			else if (ch == 8) {	// bs
				path = tok_term.delChar(edit_offset, true);
			}
			else if (ch >= 32 && ch < 127) {
				path = tok_term.insChar(edit_offset, ch, false);
			}
			if (path != null) {
				formatAndPaint(true);
				showAutoComplete(true);
				goToPath(path);
			}
		} else {
			String text = Editor.this.getEditText();
			if (text == null) text = "";
			if (ch == 127) {	// del
				if (edit_offset < text.length()) {
					text = text.substring(0, edit_offset)+text.substring(edit_offset+1);
					Editor.this.editSetItem(text);
				}
			}
			else if (ch == 8) {	// bs
				if (edit_offset > 0) {
					text = text.substring(0, edit_offset-1)+text.substring(edit_offset);
					Editor.this.editSetItem(text);
					execAction(new NavigateEditor(Editor.this, -1));
				}
			}
			else if (ch >= 32 && ch != 127) {
				text = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
				Editor.this.editSetItem(text);
				execAction(new NavigateEditor(Editor.this, +1));
			}
		}
	}
	
	public String getEditText() {
		Object o = edit_term.getTermObj();
		if (o == null || o == DrawTerm.NULL_VALUE)
			return "";
		if (o instanceof Symbol) {
			if (edit_term.attr_slot.isSymRef() && edit_term.drnode instanceof SymbolRef && ((SymbolRef)edit_term.drnode).isQualified())
				return ((Symbol)o).qname();
		}
		return o.toString();
	}

	public int getEditOffset() {
		int edit_offset = getViewPeer().getCursor_offset();
		String text = this.getEditText();
		if (edit_offset < 0 || text == null) {
			edit_offset = 0;
			getViewPeer().setCursor_offset(edit_offset);
		} 
		else if (edit_offset > text.length()) {
			edit_offset = text.length();
			getViewPeer().setCursor_offset(edit_offset);
		}
		return edit_offset;
	}
	
	public void editSetItem(Object item) {
		if (item == null)
			return;
		getWindow().startTransaction(this, "Editor:setItem");
		try {
			edit_term.setValue(item);
		} finally {
			getWindow().stopTransaction(false);
		}
		showAutoComplete(true);
		formatAndPaint(true);
	}
	
	/**
	 * Auto-Completer.
	 */
	private class AutoCompleter {
		
		/**
		 * The name.
		 */
		private String name;
		
		/**
		 * The declarations to show.
		 */
		public AutoCompleteResult result;
		
		/**
		 * the constructor.
		 * @param name the name
		 */
		AutoCompleter(String name) {
			this.name = name;
			result = edit_term.drnode.asANode().resolveAutoComplete(name==null?"":name,edit_term.attr_slot);
		}
		
	}

	/**
	 * show Auto-Complete.
	 * @param force force pop-up
	 */
	void showAutoComplete(boolean force) {
		String name = getEditText();
		if (name == null || name.length() == 0) {
			if (! force) return;
		}
		boolean qualified = name==null ? false : name.indexOf('·') > 0;
		AutoCompleteResult autocomplete_result = new AutoCompleter(name).result;
		getViewPeer().setPopupComboContent(autocomplete_result, qualified);
	}
	
}
