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

import kiev.vlang.ProjectSyntaxInfo;
import java.io.File;

/**
 * File Filter.
 */
public final class FileFilter {
	
	/**
	 * The syntax information (drawing, printing, parsing)
	 */
	public final ProjectSyntaxInfo syntax_info;
	
	/**
	 * The description.
	 */
	public final String description;
	
	/**
	 * The extension.
	 */
	public final String extension;
	
	/**
	 * The current.
	 */
	public final boolean current;
	
	/**
	 * The constructor. 
	 * @param description the description
	 * @param extension the extension
	 * @param syntax the syntax qualified name
	 * @param printer the printer qualified name
	 * @param parser the parser qualified name
	 */
	public FileFilter(ProjectSyntaxInfo syntax_info, boolean current) {
		this.syntax_info = syntax_info;
		this.description = syntax_info.getDescription() + (current ? " (current)" : "") + " (."+syntax_info.getFile_ext()+")";
		this.extension = syntax_info.getFile_ext();
		this.current = current;
	}
	
	/**
	 * Returns description.
	 * @return the description
	 */
	public String getDescription() { return description + " (*."+extension+")"; }
	
	/**
	 * Check if the file is a directory or has a given extension.
	 * @param f the file
	 * @return true or false
	 */
	public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith("."+extension); }
}
