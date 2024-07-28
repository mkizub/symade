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

import kiev.fmt.DrawTerm;
import kiev.fmt.DrawValueTerm;
import kiev.fmt.Drawable;
import kiev.fmt.common.Draw_SyntaxFunc;
import kiev.fmt.common.UIDrawPath;
import kiev.vlang.FileUnit;


/**
 * IEditor.
 */
public interface IEditor extends IUIView {

	/**
	 * Returns file-unit of this editor.
	 */
	public FileUnit getFileUnit();
	
	/**
	 * Make Current Visible.
	 */
	public void makeCurrentVisible();
	
	/**
	 * Get Action Point.
	 * @param next boolean
	 * @return ActionPoint
	 */
	public ActionPoint getActionPoint();
	
	/**
	 * Get Function Target.
	 * @param sf Draw_SyntaxFunction
	 * @return Drawable
	 */
	public Drawable getFunctionTarget(Draw_SyntaxFunc sf);
	
	/**
	 * Select Draw Term.
	 * @param dr DrawTerm
	 */
	public void selectDrawTerm(DrawTerm dr);
	
	/**
	 * Go To Path.
	 * @param path
	 */
	public void goToPath(UIDrawPath path);
	
	/**
	 * Check if the editor is currently editing some text.
	 */
	public boolean isInTextEditMode();
	
	/**
	 * Start current text edit mode.
	 */
	public void startTextEditMode(DrawValueTerm term);
	
	/**
	 * Stop current text edit mode.
	 */
	public void stopTextEditMode();
	
	/**
	 * Get current edited item text && cursor offset
	 */
	public String getEditText();
	public int getEditOffset();

	/**
	 * Process key typed event
	 */
	public void editTypeChar(char ch);

	/**
	 * Process selection from combo-box event
	 */
	public void editSetItem(Object item);
}