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
package kiev.gui.swt;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import kiev.gui.FileFilter;
import kiev.gui.IFileDialog;
import kiev.gui.IWindow;

/**
 * The dialog for file actions like save/open.
 */
public class FileDialog implements IFileDialog {
	/**
	 * The shell.
	 */
	private final Shell shell;
	
	/**
	 * The window.
	 */
	@SuppressWarnings("unused")
	private final Window window;
	
	/**
	 * The dialog type. 
	 */
	@SuppressWarnings("unused")
	private final int dlgType;
	
	/**
	 * The <code>File</code> object returned via this dialog.
	 */
	private File file;
	
	/**
	 * The native widgets dialog.
	 */
	private org.eclipse.swt.widgets.FileDialog dialog;
	
	/**
	 * The filters.
	 */
	private FileFilter[] filters;
	
	/**
	 * The constructor.
	 * @param window the window
	 * @param dlgType the type
	 */
	public FileDialog(IWindow window, int dlgType){
		this.window = (Window)window;
		this.dlgType = dlgType;
		shell = Window.getShell();
		if (dlgType == OPEN_TYPE){
			dialog = new org.eclipse.swt.widgets.FileDialog(shell, SWT.OPEN);
		} else if (dlgType == SAVE_TYPE){
			dialog = new org.eclipse.swt.widgets.FileDialog(shell, SWT.SAVE);			
		} else throw new RuntimeException(Window.resources.getString("FileDialog_Exception_unsupported_type"));
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#checkFileExists(java.io.File)
	 */
	public boolean checkFileExists(File f){
		if (f.exists()){
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
			mb.setText("File");
			mb.setMessage(Window.resources.getString("FileDialog_file_exists"));
			if (SWT.OK != mb.open())
				return false;				
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#checkFilterExtensions(java.io.File)
	 */
	public boolean checkFilterExtensions(File f){
		boolean found = false;
		for (String ext: dialog.getFilterExtensions()){
			if (f.getName().endsWith(ext.substring(1))){
				found = true;
				break;
			}
		}
		if (! found){
			MessageBox mb = new MessageBox(shell, SWT.OK | SWT.CLOSE | SWT.ICON_WARNING | SWT.APPLICATION_MODAL);
			mb.setText("File");
			mb.setMessage(Window.resources.getString("FileDialog_file_no_extension"));
			mb.open();
			return false;				
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#getSelectedFile()
	 */
	public File getSelectedFile() {
		return file;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#getSelectedFilter()
	 */
	public FileFilter getSelectedFilter() {
		return filters[dialog.getFilterIndex()];
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#open()
	 */
	public int open() {
		String name = dialog.open();
		if (name == null) return CANCEL;
		file = new File(name);
		return OK;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#setFilterExtensions(java.lang.String[])
	 */
	public void setFilters(FileFilter[] filters) {
		String[] extensions = new String[filters.length];
		String[] names = new String[filters.length];
		for (int i=0; i < filters.length; i++) {
			extensions[i] = "*."+filters[i].extension;
			names[i] = filters[i].description;
		}
		dialog.setFilterExtensions(extensions);
		dialog.setFilterNames(names);
		this.filters = filters;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#setFilterPath(java.lang.String)
	 */
	public void setFilterPath(String path) {
		dialog.setFilterPath(path);		
	}


	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#setFileName(java.lang.String)
	 */
	public void setFileName(String name) {
		dialog.setFileName(name);		
	}
}
