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

/**
 * Text Editor UI Action.
 */
public class TextEditor implements UIAction {
	
	/**
	 * The editor.
	 */
	protected final Editor editor;
	
	/**
	 * The attributes.
	 */
	public final DrawValueTerm term;
	
	/**
	 * Text Editor UI Action Factory.
	 */
	public final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Edit the attribute as a text"; }
		
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
			DrawTerm dt = context.dt;
			if (!(dt instanceof DrawValueTerm)) return null;
			return new TextEditor(editor, (DrawValueTerm)dt);
		}
	}

	/**
	 * The constructor.
	 * @param editor the editor
	 * @param dr_term the draw term
	 * @param pattr the attributes
	 */
	public TextEditor(Editor editor, DrawValueTerm term) {
		this.editor = editor;
		this.term = term;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		this.editor.startTextEditMode(term);
	}
	
}

