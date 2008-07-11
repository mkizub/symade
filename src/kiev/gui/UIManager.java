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
import kiev.fmt.Draw_ATextSyntax;
import kiev.gui.swing.ANodeTable;
import kiev.gui.swing.ANodeTree;
import kiev.vtree.ScalarPtr;

public class UIManager {

	public static ICanvas newCanvas(){
		return new kiev.gui.swing.Canvas();
	}
	
	public static IWindow newWindow(){
		return new kiev.gui.swing.Window();
	}
	
	public static ItemEditor newEnumEditor(Editor editor, DrawTerm cur_elem, ScalarPtr pattr){
		return new kiev.gui.swing.EnumEditor(editor, cur_elem, pattr);
	}
	
	public static ItemEditor newIntEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr){
		return new kiev.gui.swing.EnumEditor(editor, dr_term, pattr);
	}

	public static ItemEditor newOperatorEditor(Editor editor, DrawTerm cur_elem){
		return new kiev.gui.swing.OperatorEditor(editor, cur_elem);
	}
	
	public static ItemEditor newTextEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr){
		return new kiev.gui.swing.TextEditor(editor, dr_term, pattr);
	}
	
	public static UIActionFactory newExprEditActionsFlatten(){
		return kiev.gui.swing.ExprEditActions.newFlatten();
	}
	
	public static UIActionFactory newFunctionExecutorFactory(){
		return kiev.gui.swing.FunctionExecutor.newFactory();
	}
	
	public static UIActionFactory newNewElemHereFactory(){
		return kiev.gui.swing.NewElemHere.newFactory();
	}
	
	public static UIActionFactory newNewElemNextFactory(){
		return kiev.gui.swing.NewElemNext.newFactory();
	}

	public static UIActionFactory newPasteHereFactory(){
		return kiev.gui.swing.Clipboard.newPasteHereFactory();
	}

	public static UIActionFactory newPasteNextFactory(){
		return kiev.gui.swing.Clipboard.newPasteNextFactory();
	}
	
	public static void setClipboardContent(Object obj) {
		kiev.gui.swing.Clipboard.setClipboardContent(obj);
	}

	public static void doGUIBeep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	public static TableView newTableView(IWindow window, Draw_ATextSyntax syntax, ANodeTable table){
		return new kiev.gui.swing.TableViewImpl(window, syntax, table);
	}
	public static TreeView newTreeView(IWindow window, Draw_ATextSyntax syntax, ANodeTree the_tree){
		return new kiev.gui.swing.TreeViewImpl(window, syntax, the_tree);
	}

}
