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

import kiev.fmt.DrawFolded;
import kiev.fmt.Drawable;

/**
 * Folder Trigger UI Action.
 */
public final class FolderTrigger implements UIAction {
	
	/**
	 * The editor.
	 */
	private final Editor editor;
	
	/**
	 * Draw Folded.
	 */
	private final DrawFolded df;
	
	/**
	 * The constructor.
	 * @param editor the editor
	 * @param df the draw folded
	 */
	public FolderTrigger(Editor editor, DrawFolded df) {
		this.editor = editor;
		this.df = df;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		df.setDrawFolded(!df.getDrawFolded());
		editor.formatAndPaint(true);
	}
	
	/**
	 * Folder Trigger UI Action Factory.
	 */
	public final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Toggle folding"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return true; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null) return null;
			Editor editor = context.editor;
			for (Drawable dr = editor.getDrawTerm(); dr != null; dr = (Drawable)dr.parent()) {
				if (dr instanceof DrawFolded) return new FolderTrigger(editor, (DrawFolded)dr);
			}
			return null;
		}
	}
}
