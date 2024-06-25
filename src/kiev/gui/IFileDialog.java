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

import java.io.File;

/**
 * IFileDialog
 * @see kiev.gui.swt.FileDialog
 * @see kiev.gui.swing.FileDialog
 */
public interface IFileDialog {
	
	/**
	 * OPEN TYPE
	 */
	public static final int OPEN_TYPE = 1;
	
	/**
	 * SAVE TYPE
	 */
	public static final int SAVE_TYPE = 2;
	
	/**
	 * OK
	 */
	public static final int OK = 1 << 5;
	
	/**
	 * CANCEL
	 */
	public static final int CANCEL = 1 << 8;

	/**
	 * set Filters
	 * @param filters filters
	 */
	public void setFilters(FileFilter[] filters);
	
	/**
	 * set Filter Path
	 * @param path path
	 */
	public void setFilterPath(String path);
	
	/**
	 * set File Name
	 * @param name name
	 */
	public void setFileName(String name);
	
	/**
	 * open
	 * @return int
	 */
	public int open();
	
	/**
	 * get Selected File
	 * @return File
	 */
	public File getSelectedFile();
	
	/**
	 * check File Exists
	 * @param f File
	 * @return boolean
	 */
	public FileFilter getSelectedFilter();

	/**
	 * check File Exists 
	 * @param f File
	 * @return boolean
	 */
	public boolean checkFileExists(File f);
	
	/**
	 * check Filter Extensions
	 * @param f File
	 * @return boolean
	 */
	public boolean checkFilterExtensions(File f);
	
}
