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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.JPopupMenu;
import javax.swing.text.TextAction;

import kiev.Compiler;
import kiev.EditorThread;
import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawNonTerm;
import kiev.fmt.DrawTerm;
import kiev.fmt.DrawToken;
import kiev.fmt.Draw_SyntaxToken;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.fmt.SyntaxTokenKind;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.NavigateEditor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.parser.ASTExpression;
import kiev.parser.EToken;
import kiev.parser.ETokenKind;
import kiev.vlang.ConstExpr;
import kiev.vlang.ENode;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;
import kiev.vtree.SpaceAttrSlot;
import kiev.vtree.SpacePtr;

public final class ExprEditActions 
	implements ItemEditor, Runnable {
	
	final Editor editor;
	final UIActionViewContext context;
	final String action;
	
	private ASTExpression		expr;
	private JPopupMenu			menu;
	
	public ExprEditActions(UIActionViewContext context, String action) {
		this.editor = context.editor;
		this.context = context;
		this.action = action;
	}
	
	public void run() {
		if (action == "split") {
			DrawNonTerm nt = null;
			{
				Drawable d = context.dr;
				while (d != null && !(d instanceof DrawNonTerm))
					d = (Drawable)d.parent();
				if (!(d instanceof DrawNonTerm))
					return;
				nt = (DrawNonTerm)d;
			}
			DrawTerm first = nt.getFirstLeaf();
			DrawTerm last = nt.getLastLeaf().getNextLeaf();
			expr = new ASTExpression();
			SpacePtr enodes = expr.getSpacePtr("nodes");
			for (DrawTerm dt = first; dt != null && dt != last; dt = dt.getNextLeaf()) {
				if (dt.isUnvisible())
					continue;
				if (dt instanceof DrawToken) {
					if (((Draw_SyntaxToken)dt.syntax).kind == SyntaxTokenKind.UNKNOWN)
						enodes.add(new EToken(0, dt.getText(), ETokenKind.UNKNOWN, false));
					else
						enodes.add(new EToken(0, dt.getText(), ETokenKind.OPERATOR, true));
				}
				else if (dt instanceof DrawNodeTerm) {
					if (dt.get$drnode() instanceof ConstExpr)
						enodes.add(new EToken((ConstExpr)dt.get$drnode()));
					else
						enodes.add(new EToken(0,dt.getText(),ETokenKind.UNKNOWN,false));
				}
			}
			editor.insert_mode = true;
			editor.startItemEditor(this);
			context.node.replaceWithNode(expr);
			for (EToken et: (EToken[])expr.getNodes())
				et.guessKind();
			editor.formatAndPaint(true);
		}
	}
	
	class SetKindAction extends TextAction {
		private static final long serialVersionUID = 8225219266004726459L;
		private EToken et;
		private ETokenKind kind;
		SetKindAction(EToken et, ETokenKind kind) {
			super(String.valueOf(kind));
			this.et = et;
			this.kind = kind;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null)
				editor.getView_canvas().remove(menu);
			menu = null;
			if (kind == ETokenKind.UNKNOWN) {
				et.setKind(ETokenKind.UNKNOWN);
				et.set$explicit(false);
			} else {
				et.setKind(kind);
				et.set$explicit(true);
			}
			et.guessKind();
			editor.formatAndPaint(true);
		}
	}
  
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (code == KeyEvent.VK_F && mask == KeyEvent.CTRL_DOWN_MASK) {
			DrawTerm dt = editor.getCur_elem().dr;
			ANode n = editor.getCur_elem().node;
			if (!(n instanceof EToken) || n.parent() != expr || dt == null || dt.get$drnode() != n)
				return;
			EToken et = (EToken)n;
			menu = new JPopupMenu();
			for (ETokenKind k: ETokenKind.class.getEnumConstants())
				menu.add(new SetKindAction(et, k));
			GfxDrawTermLayoutInfo dtli = dt.getGfxFmtInfo();
			int x = dtli.getX();
			int h = dtli.getHeight();
			int y = dtli.getY() + h - editor.getView_canvas().getTranslated_y();
			menu.show((Component)editor.getView_canvas(), x, y);
			return;
		}
		if (mask != 0 && mask != KeyEvent.SHIFT_DOWN_MASK)
			return;
		evt.consume();
		switch (code) {
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_UP:
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		case KeyEvent.VK_HOME:
			if (expr.getNodes().length > 0)
				editor.goToPath(makePathTo(expr.getNodes()[0]));
			else
				editor.goToPath(makePathTo(expr));
			editor.getView_canvas().setCursor_offset(0);
			return;
		case KeyEvent.VK_END:
			if (expr.getNodes().length > 0)
				editor.goToPath(makePathTo(expr.getNodes()[expr.getNodes().length-1]));
			else
				editor.goToPath(makePathTo(expr));
			if (editor.getCur_elem().dr != null && editor.getCur_elem().dr.getText() != null)
				editor.getView_canvas().setCursor_offset(editor.getCur_elem().dr.getText().length());
			else
				editor.getView_canvas().setCursor_offset(0);
			return;
		case KeyEvent.VK_LEFT:
			new NavigateEditor(context.editor,-1).run();
			return;
		case KeyEvent.VK_RIGHT:
			new NavigateEditor(context.editor,+1).run();
			return;
		case KeyEvent.VK_ENTER:
			editor.insert_mode = true;
			EditorThread thr = EditorThread.getInst();
			try {
				thr.errCount = 0;
				thr.warnCount = 0;
				Compiler.runFrontEnd(thr,null,(ASTNode)expr.parent(),true);
			} catch (Throwable t) { t.printStackTrace(); }
			editor.insert_mode = false;
			editor.stopItemEditor(false);
			return;
		case KeyEvent.VK_ESCAPE:
			editor.insert_mode = false;
			editor.stopItemEditor(true);
			return;
		}
		if (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_ALT || code == KeyEvent.VK_CONTROL)
			return;
		DrawTerm dt = editor.getCur_elem().dr;
		ANode n = editor.getCur_elem().node;
		if (!(n instanceof EToken) || n.parent() != expr || dt == null || dt.get$drnode() != n) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		EToken et = (EToken)n;
		String prefix_text = dt.getPrefix();
		String suffix_text = dt.getSuffix();
		String text = dt.getText();
		if (text == null) { text = ""; }
		int prefix_offset = prefix_text.length();
		int suffix_offset = suffix_text.length();
		text = text.substring(prefix_offset, text.length() - suffix_offset);
		int edit_offset = editor.getView_canvas().getCursor_offset() - prefix_offset;
		if (edit_offset < 0 || edit_offset > text.length()) {
			char ch = evt.getKeyChar();
			if (ch != KeyEvent.CHAR_UNDEFINED) {
				if (edit_offset < 0) {
					prependNode(dt,et,ch);
					return;
				}
				else if (edit_offset > text.length()) {
					appendNode(dt,et,ch);
					return;
				}
			}
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		switch (code) {
		case KeyEvent.VK_DELETE:
			if (text.length() == 0) {
				deleteNode(dt,et,false);
				return;
			}
			else if (edit_offset >= text.length()) {
				joinNodes(dt,et,false);
				return;
			}
			else if (edit_offset < text.length()) {
				et.setText(text = text.substring(0, edit_offset)+text.substring(edit_offset+1));
			}
			break;
		case KeyEvent.VK_BACK_SPACE:
			if (text.length() == 0) {
				deleteNode(dt,et,true);
				return;
			}
			else if (edit_offset == 0) {
				joinNodes(dt,et,true);
				return;
			}
			else if (edit_offset > 0) {
				edit_offset--;
				et.setText(text = text.substring(0, edit_offset)+text.substring(edit_offset+1));
			}
			break;
		case KeyEvent.VK_SPACE:
			// split the node, if it's not a string/char expression
			if (et.getKind() != ETokenKind.EXPR_STRING && et.getKind() != ETokenKind.EXPR_CHAR) {
				if (et.get$explicit() && edit_offset != 0 && edit_offset != text.length()) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}
				else if (edit_offset < 0 || edit_offset > text.length()) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}
				// split the node
				splitNode(dt,et,text.substring(0,edit_offset),text.substring(edit_offset));
				return;
			} // fall through
		default:
			if (evt.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			} else {
				char ch = evt.getKeyChar();
				if (et.getKind() == ETokenKind.EXPR_STRING || et.getKind() == ETokenKind.EXPR_CHAR) {
					text = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					edit_offset++;
					et.setText(text);
					break;
				}
				else if (et.getKind() == ETokenKind.EXPR_NUMBER) {
					String s = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					if (EToken.patternIntConst.matcher(s).matches() || EToken.patternFloatConst.matcher(s).matches()) {
						edit_offset++;
						et.setText(s);
					}
					else if (edit_offset == 0) {
						prependNode(dt,et,ch);
						return;
					}
					else if (edit_offset >= text.length()) {
						appendNode(dt,et,ch);
						return;
					}
					else
						java.awt.Toolkit.getDefaultToolkit().beep();
					break;
				}
				else if (EToken.patternIdent.matcher(text).matches()) {
					String s = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					if (EToken.patternIdent.matcher(s).matches()) {
						edit_offset++;
						et.setText(s);
					}
					else if (edit_offset == 0) {
						prependNode(dt,et,ch);
						return;
					}
					else if (edit_offset >= text.length()) {
						appendNode(dt,et,ch);
						return;
					}
					else
						java.awt.Toolkit.getDefaultToolkit().beep();
					break;
				}
				else if (EToken.patternOper.matcher(text).matches()) {
					String s = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					if (EToken.patternOper.matcher(s).matches()) {
						edit_offset++;
						et.setText(s);
					}
					else if (edit_offset == 0) {
						prependNode(dt,et,ch);
						return;
					}
					else if (edit_offset >= text.length()) {
						appendNode(dt,et,ch);
						return;
					}
					else
						java.awt.Toolkit.getDefaultToolkit().beep();
					break;
				}
				// unknown
				et.setText(text.substring(0, edit_offset)+ch+text.substring(edit_offset));
				edit_offset++;
				break;
			}
		}
		editor.getView_canvas().setCursor_offset(edit_offset+prefix_offset);
		editor.formatAndPaint(true);
	}
	
	private void deleteNode(DrawTerm dt, EToken et, boolean by_backspace) {
		dt = (by_backspace ? dt.getPrevLeaf() : dt.getNextLeaf());
		editor.getCur_elem().set(dt);
		if (by_backspace && dt != null && dt.getText() != null)
			editor.getView_canvas().setCursor_offset(dt.getText().length());
		else
			editor.getView_canvas().setCursor_offset(0);
		et.detach();
		editor.formatAndPaint(true);
	}
	private void joinNodes(DrawTerm dt, EToken et, boolean by_backspace) {
		if (by_backspace) {
			DrawTerm pt = dt.getPrevLeaf();
			if (pt != null && pt.get$drnode() instanceof EToken) {
				EToken pe = (EToken)pt.get$drnode();
				editor.getCur_elem().set(pt);
				editor.getView_canvas().setCursor_offset(pt.getText().length());
				pe.setText(pe.get$ident() + et.get$ident());
				et.detach();
			}
		} else {
			DrawTerm nt = dt.getNextLeaf();
			if (nt != null && nt.get$drnode() instanceof EToken) {
				EToken pe = (EToken)nt.get$drnode();
				et.setText(et.get$ident() + pe.get$ident());
				pe.detach();
			}
		}
		editor.formatAndPaint(true);
	}
	private void splitNode(DrawTerm dt, EToken et, String left, String right) {
		if (left == null) left = "";
		if (right == null) right = "";
		EToken ne = new EToken();
		SpaceAttrSlot sas = (SpaceAttrSlot)et.pslot();
		int idx = sas.indexOf(et.parent(),et);
		if (left.length() == 0) {
			// insert a new node before
			sas.insert(et.parent(),idx,ne);
			ne.setText(left);
			et.setText(right);
		} else {
			// insert new node after
			sas.insert(et.parent(),idx+1,ne);
			et.setText(left);
			ne.setText(right);
		}
		// set new node to be current
		editor.getView_canvas().setCursor_offset(0);
		editor.formatAndPaint(true);
		editor.goToPath(makePathTo(ne));
		editor.formatAndPaint(false);
	}
	private void prependNode(DrawTerm dt, EToken et, char ch) {
		EToken ne = new EToken();
		SpaceAttrSlot sas = (SpaceAttrSlot)et.pslot();
		int idx = sas.indexOf(et.parent(),et);
		sas.insert(et.parent(),idx,ne);
		ne.setText(String.valueOf(ch));
		editor.formatAndPaint(true);
		editor.goToPath(makePathTo(ne));
		editor.getView_canvas().setCursor_offset(1);
		editor.formatAndPaint(false);
	}
	private void appendNode(DrawTerm dt, EToken et, char ch) {
		EToken ne = new EToken();
		SpaceAttrSlot sas = (SpaceAttrSlot)et.pslot();
		int idx = sas.indexOf(et.parent(),et);
		sas.insert(et.parent(),idx+1,ne);
		ne.setText(String.valueOf(ch));
		editor.formatAndPaint(true);
		editor.goToPath(makePathTo(ne));
		editor.getView_canvas().setCursor_offset(1);
		editor.formatAndPaint(false);
	}
	
	private ANode[] makePathTo(ANode n) {
		Vector<ANode> path = new Vector<ANode>();
		path.add(n);
		while (n.parent() != null) {
			n = n.parent();
			path.add(n);
			if (n instanceof FileUnit)
				break;
		}
		return path.toArray(new ANode[path.size()]);
	}

	public static Flatten newFlatten(){
		return new Flatten();
	}
	
	final static class Flatten implements UIActionFactory {
		public String getDescr() { return "Flatten expresison tree"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			ANode node = context.node;
			Drawable dr = context.dr;
			if (context.editor == null || node == null || dr == null)
				return null;
			if (!(node instanceof ENode))
				return null;
			return new ExprEditActions(context, "split");
		}
	}
}
