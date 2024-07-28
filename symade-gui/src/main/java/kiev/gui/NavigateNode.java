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

import kiev.vtree.INode;


/**
 * Navigate Node Actions.
 */
public final class NavigateNode implements UIAction {

	/**
	 * The editor.
	 */
	private final Editor uiv;
	
	/**
	 * The action.
	 */
	private final String cmd;
	
	/**
	 * The constructor.
	 * @param uiv the view
	 * @param cmd the action
	 */
	public NavigateNode(Editor uiv, String cmd) {
		this.uiv = uiv;
		this.cmd = cmd;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		if (cmd == "select-up") {
			INode node = uiv.getSelectedNode();
			if (node != null && node.parent() != null) { 
				uiv.setSelectedNode(node.parent());
				uiv.formatAndPaint(true);
			}
		}
		else if (cmd == "insert-mode") {
			uiv.stopTextEditMode();
			uiv.getViewPeer().setCursor_offset(-1);
			uiv.setInInsertMode(!uiv.isInInsertMode());
		}
	}

	/**
	 * Node Up UI Action Factory.
	 */
	public final static class NodeUp implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Select parent node"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getSelectedNode() != null && context.editor.getSelectedNode().parent() != null)
				return new NavigateNode(context.editor, "select-up");
			return null;
		}
	}

	/**
	 * Insert Mode UI Action Factory.
	 */
	public final static class InsertMode implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Change insert/command editor mode"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateNode(context.editor, "insert-mode");
			return null;
		}
	}
	
}
