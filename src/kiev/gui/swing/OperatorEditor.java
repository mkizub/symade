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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.TextAction;

import kiev.fmt.DrawTerm;
import kiev.fmt.DrawToken;
import kiev.fmt.Draw_SyntaxToken;
import kiev.fmt.SyntaxTokenKind;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.ENode;
import kiev.vlang.Operator;
import kiev.vlang.types.TypeExpr;

public class OperatorEditor implements KeyListener, PopupMenuListener, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final ENode			expr;
	private final JPopupMenu	menu;
	
	public OperatorEditor(Editor editor, DrawTerm cur_elem) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.expr = (ENode)cur_elem.get$drnode();
		this.menu = new JPopupMenu();
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the operator of an expression"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt == null || context.node == null)
				return null;
			if (dt.get$drnode() != context.node)
				return null;
			if (!(dt instanceof DrawToken && dt.get$drnode() instanceof ENode && ((Draw_SyntaxToken)dt.syntax).kind == SyntaxTokenKind.OPERATOR))
				return null;
			return new OperatorEditor(editor, dt);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		if (expr instanceof TypeExpr) {
			// show all postfix type operators
			for(kiev.stdlib.Enumeration op$iter = Operator.allOperatorNamesHash.elements(); op$iter.hasMoreElements();) {
				Operator op = (Operator)op$iter.nextElement();
				if (op.name.startsWith("T "))
					menu.add(new JMenuItem(new SetSyntaxAction(op)));
			}
		}
		else if (expr.getArgs().length == 2) {
			JMenu m_assign = new JMenu("Assign");
			menu.add(m_assign);
			for (Operator op: Operator.allAssignOperators) 
				if (op.arity == 2)
					m_assign.add(new JMenuItem(new SetSyntaxAction(op)));

			JMenu m_bool   = new JMenu("Boolean");
			menu.add(m_bool);
			for (Operator op: Operator.allBoolOperators) 
				if(op.arity == 2)
					m_bool.add(new JMenuItem(new SetSyntaxAction(op)));

			JMenu m_math   = new JMenu("Arithmetic");
			menu.add(m_math);
			for (Operator op: Operator.allMathOperators) 
				if (op.arity == 2)
					m_math.add(new JMenuItem(new SetSyntaxAction(op)));

			JMenu m_others = new JMenu("Others");
			menu.add(m_others);
			for(kiev.stdlib.Enumeration op$iter = Operator.allOperatorNamesHash.elements(); op$iter.hasMoreElements();) {
				Operator op = (Operator)op$iter.nextElement();
				if (op.arity == 2 && !op.name.startsWith("T ")) {
					if (kiev.stdlib.Arrays.contains(Operator.allAssignOperators, op))
						continue;
					if (kiev.stdlib.Arrays.contains(Operator.allBoolOperators, op))
						continue;
					if (kiev.stdlib.Arrays.contains(Operator.allMathOperators, op))
						continue;
					m_others.add(new JMenuItem(new SetSyntaxAction(op)));
				}
			}
		}
		else {
			int arity = expr.getArgs().length;
			for(kiev.stdlib.Enumeration op$iter = Operator.allOperatorNamesHash.elements(); op$iter.hasMoreElements();) {
				Operator op = (Operator)op$iter.nextElement();
				if( op.arity == arity && !op.name.startsWith("T "))
					menu.add(new JMenuItem(new SetSyntaxAction(op)));
			}
		}
		int x = cur_elem.getX();
		int h = cur_elem.getHeight();
		int y = cur_elem.getY() + h - editor.view_canvas.translated_y;
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
		private static final long serialVersionUID = -8936694424280098620L;
		private Operator op; // Enum or Boolean
		SetSyntaxAction(Operator op) {
			super(String.valueOf(op));
			this.op = op;
		}
		public void actionPerformed(ActionEvent e) {
			editor.view_canvas.remove(menu);
			try {
				ENode expr = OperatorEditor.this.expr;
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
