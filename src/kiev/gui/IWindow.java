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

import kiev.EditorThreadGroup;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vlang.Project;
import kiev.vtree.INode;

/**
 * This interface should implement different instances of application
 * such as Swing or SWT.
 *
 */
public interface IWindow {
	
	/**
	 * Opens <code>Editor</code> tab in the editors tab folder.
	 * 
	 * @param fu the file unit to open
	 */
	public IEditor openEditor(FileUnit fu);
	
	/**
	 * Opens <code>Editor</code> tab in the editors tab folder for a node path.
	 * 
	 * @param fu the file unit to open
	 * @param path the node path
	 */
	public IEditor openEditor(FileUnit fu, INode[] path);
	
	/**
	 * Closes <code>Editor</code> tab. When current editor is closed the first is
	 * selected. 
	 * 
	 * @param ed the editor
	 */
	public void closeEditor(IEditor ed);
		
	/**
	 * Returns the current view selected.
	 * 
	 * @return the current view
	 */
	public UIView getCurrentView();
	
	/**
	 * Request to update status bar.
	 */
	public void updateStatusBar();
	
	/**
	 * Returns the current environment.
	 * @return the environment
	 */
	public Env getCurrentEnv();
		
	/**
	 * Returns the current project.
	 * @return the current project
	 */
	public Project getCurrentProject();

	/**
	 * Returns the <code>EditorThreadGroup</code>.
	 * @return the EditorThreadGroup
	 */
	public EditorThreadGroup getEditorThreadGroup();

	/**
	 * Start new transaction
	 */
	public void startTransaction(IEditor editor, String name);

	/**
	 * Finish transaction
	 */
	public void stopTransaction(boolean revert);

	/**
	 * Undo
	 */
	public void undoTransaction();

	/**
	 * Get window's top undo transaction editor
	 */
	public IEditor getCurrentUndoEditor();

	/**
	 * Notify that errors list may be changed.
	 */
	public abstract void fireErrorsModified();

}
