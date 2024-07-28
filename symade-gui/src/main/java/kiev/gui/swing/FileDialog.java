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
package kiev.gui.swing;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;


import kiev.gui.IFileDialog;
import kiev.gui.IWindow;
import kiev.gui.FileFilter;

/**
 * File Dialog.
 */
public class FileDialog implements IFileDialog {
	
	/**
	 * The window.
	 */
	private final Window window;
	
	/**
	 * The type.
	 */
	private final int dlgType;
	
	/**
	 * The name.
	 */
	private String name;
	
	/**
	 * The file chooser.
	 */
	private JFileChooser dialog;
		
	/**
	 * The path.
	 */
	private String path;
	 
	/**
	 * Filters.
	 */
	private FileFilter[] filters;

	/**
	 * Choosable File Filter.
	 */
	class ChoosableFileFilter extends javax.swing.filechooser.FileFilter {
		/**
		 * File filter.
		 */
		private final FileFilter ff;
		
		/**
		 * The constructor.
		 * @param ff the File Filter
		 */
		private ChoosableFileFilter(FileFilter ff){
			this.ff = ff;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(File f) {
			return ff.accept(f);
		}
		/* (non-Javadoc)
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		@Override
		public String getDescription() {
			return ff.getDescription();
		}
	}
 
	/**
	 * The constructor.
	 * @param window the window
	 * @param dlgType the type
	 */
	public FileDialog(IWindow window, int dlgType){
		this.window = (Window)window;
		this.dlgType = dlgType;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#getSelectedFile()
	 */
	public File getSelectedFile() {
		return dialog.getSelectedFile();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#checkFileExists(java.io.File)
	 */
	public boolean checkFileExists(File f){
		if (f.exists()){
			Object[] options = { "OK", "Cancel" };
			if (JOptionPane.OK_OPTION != JOptionPane.showOptionDialog(window.getFrame(), "File already exists. Do you want to overwrite it?",
					"File New", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, options, options[0]))
				return false;				
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#checkFilterExtensions(java.io.File)
	 */
	public boolean checkFilterExtensions(File f) {
		boolean found = false;
		for (FileFilter ff: filters){
			if (f.getName().toLowerCase().endsWith(("."+ff.extension).toLowerCase())) {
				found = true;
				break;
			}
		}
		if (! found){
			Object[] options = { "OK" };
			JOptionPane.showOptionDialog(window.getFrame(), "File doesn't have extension. Please, open dialog and type extension again.",
					"File", JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,
					null, options, options[0]);
			return false;	
		}
		return true;
	}
		
	/**
	 * Apply filters.
	 */
	private void applyFilters(){
		if (filters != null) {
			dialog.setAcceptAllFileFilterUsed(false);
			for (FileFilter flt: filters)
				dialog.addChoosableFileFilter(new ChoosableFileFilter(flt));
		}
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#open()
	 */
	public int open() {
		dialog = new JFileChooser(path);
		if (name != null)
			dialog.setSelectedFile(new File(name));
		applyFilters();
		if (dlgType == OPEN_TYPE){
			if (JFileChooser.APPROVE_OPTION != dialog.showOpenDialog(null))
				return CANCEL;
		} else if (dlgType == SAVE_TYPE){
			if (JFileChooser.APPROVE_OPTION != dialog.showSaveDialog(null))
				return CANCEL;
		}
		return OK;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#setFilters(kiev.gui.FileFilter[])
	 */
	public void setFilters(FileFilter[] filters) {
		this.filters = filters;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#setFilterPath(java.lang.String)
	 */
	public void setFilterPath(String path) {
		this.path = path;
		
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#setFileName(java.lang.String)
	 */
	public void setFileName(String name) {
		this.name = name;		
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IFileDialog#getSelectedFilter()
	 */
	public FileFilter getSelectedFilter() {
		javax.swing.filechooser.FileFilter ff = dialog.getFileFilter();
		if (ff instanceof ChoosableFileFilter)
			return ((ChoosableFileFilter)ff).ff;
		return null;
	}

}
