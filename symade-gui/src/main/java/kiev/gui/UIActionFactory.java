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

/**
 * This interface should implement classes that are actions. 
 * The action factory used to create UI Action.
 * @see kiev.gui.UIAction
 */
public interface UIActionFactory {
	
	/**
	 * Returns description. 
	 * @return String
	 */
	public String getDescr();
	
	/**
	 * Defines whether this action would be included in the pop-up menu. 
	 * @return boolean
	 */
	public boolean isForPopupMenu();
	
	/**
	 * Returns <code>UIAction</code> action or <code>null</code>
	 * if no action with this context found or the action is not ready.
	 * @param context the action context.
	 * @return <code>UIAction</code>
	 */
	public UIAction getAction(UIActionViewContext context);
}
