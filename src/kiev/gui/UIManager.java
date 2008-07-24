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

import java.util.Hashtable;

import kiev.fmt.DrawTerm;
import kiev.vtree.ScalarPtr;

public class UIManager {

	public static ICanvas newCanvas(){
		return new kiev.gui.swing.Canvas();
	}
	
	public static IWindow newWindow(){
		return new kiev.gui.swing.Window();
	}
	
	public static Hashtable<Object,UIActionFactory[]> getUIActions(UIView uiv) {
		if (uiv instanceof Editor)
			return kiev.gui.swing.Configuration.getEditorActionMap();
		if (uiv instanceof ProjectView)
			return kiev.gui.swing.Configuration.getProjectViewActionMap();
		if (uiv instanceof InfoView)
			return kiev.gui.swing.Configuration.getInfoViewActionMap();
		if (uiv instanceof TreeView)
			return kiev.gui.swing.Configuration.getTreeViewActionMap();
		return new Hashtable<Object,UIActionFactory[]>();
	}
	
	public static Runnable newEnumEditor(Editor editor, DrawTerm cur_elem, ScalarPtr pattr){
		return new kiev.gui.swing.EnumEditor(editor, cur_elem, pattr);
	}
	
	public static Runnable newIntEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr){
		return new kiev.gui.swing.IntEditor(editor, dr_term, pattr);
	}

	public static Runnable newOperatorEditor(Editor editor, DrawTerm cur_elem){
		return new kiev.gui.swing.OperatorEditor(editor, cur_elem);
	}
	
	public static Runnable newTextEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr){
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
		kiev.gui.swing.Configuration.doGUIBeep();
	}

}
