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

import kiev.fmt.DrawTerm;
import kiev.fmt.DrawToken;
import kiev.fmt.Draw_SyntaxToken;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.fmt.SyntaxTokenKind;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.IPopupMenuPeer;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.ENode;
import kiev.vlang.Operator;
import kiev.vlang.types.TypeExpr;

public class OperatorEditor implements ItemEditor, IPopupMenuListener {
	private final Editor			editor;
	private final DrawTerm			cur_elem;
	private final ENode				expr;
	private       IPopupMenuPeer	menu;
	
	public OperatorEditor(Editor editor, DrawTerm cur_elem) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.expr = (ENode)cur_elem.drnode;
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
			if (dt.drnode != context.node)
				return null;
			if (!(dt instanceof DrawToken && dt.drnode instanceof ENode && ((Draw_SyntaxToken)dt.syntax).kind == SyntaxTokenKind.OPERATOR))
				return null;
			return new OperatorEditor(editor, dt);
		}
	}

	public void run() {
		this.menu = UIManager.newPopupMenu(editor, this);
		editor.startItemEditor(this);
		if (expr instanceof TypeExpr) {
			// show all postfix type operators
			for(kiev.stdlib.Enumeration op$iter = Operator.allOperatorNamesHash.elements(); op$iter.hasMoreElements();) {
				Operator op = (Operator)op$iter.nextElement();
				if (op.name.startsWith("T "))
					menu.addItem(new SetSyntaxAction(op));
			}
		}
		else if (expr.getArgs().length == 2) {
			ISubMenuPeer m_assign = menu.newSubMenu("Assign");
			for (Operator op: Operator.allAssignOperators) 
				if (op.arity == 2)
					m_assign.addItem(new SetSyntaxAction(op));

			ISubMenuPeer m_bool   = menu.newSubMenu("Boolean");
			for (Operator op: Operator.allBoolOperators) 
				if(op.arity == 2)
					m_bool.addItem(new SetSyntaxAction(op));

			ISubMenuPeer m_math   = menu.newSubMenu("Arithmetic");
			for (Operator op: Operator.allMathOperators) 
				if (op.arity == 2)
					m_math.addItem(new SetSyntaxAction(op));

			ISubMenuPeer m_others = menu.newSubMenu("Others");
			for(kiev.stdlib.Enumeration op$iter = Operator.allOperatorNamesHash.elements(); op$iter.hasMoreElements();) {
				Operator op = (Operator)op$iter.nextElement();
				if (op.arity == 2 && !op.name.startsWith("T ")) {
					if (kiev.stdlib.Arrays.contains(Operator.allAssignOperators, op))
						continue;
					if (kiev.stdlib.Arrays.contains(Operator.allBoolOperators, op))
						continue;
					if (kiev.stdlib.Arrays.contains(Operator.allMathOperators, op))
						continue;
					m_others.addItem(new SetSyntaxAction(op));
				}
			}
		}
		else {
			int arity = expr.getArgs().length;
			for(kiev.stdlib.Enumeration op$iter = Operator.allOperatorNamesHash.elements(); op$iter.hasMoreElements();) {
				Operator op = (Operator)op$iter.nextElement();
				if( op.arity == arity && !op.name.startsWith("T "))
					menu.addItem(new SetSyntaxAction(op));
			}
		}
		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		menu.showAt(x, y);
	}

	public void popupMenuCanceled() {
		menu.remove();
		editor.stopItemEditor(true);
	}

	public void popupMenuExecuted(IMenuItem item) {
		SetSyntaxAction sa = (SetSyntaxAction)item;
		menu.remove();
		try {
			ENode expr = OperatorEditor.this.expr;
			expr.setOp(sa.op);
			sa = null;
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			editor.stopItemEditor(sa != null);
		}
	}

	class SetSyntaxAction implements IMenuItem {
		private Operator op; // Enum or Boolean
		SetSyntaxAction(Operator op) {
			this.op = op;
		}
		public String getText() {
			return String.valueOf(op);
		}
	}
}
