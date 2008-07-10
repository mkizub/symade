/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.gui;


public class NavigateNode implements Runnable {

	final Editor uiv;
	final String cmd;
	
	NavigateNode(Editor uiv, String cmd) {
		this.uiv = uiv;
		this.cmd = cmd;
	}
	
	public static NodeUp newNodeUp(){
		return new NodeUp();
	}

	public static InsertMode newInsertMode(){
		return new InsertMode();
	}
	public void run() {
		if (cmd == "select-up") {
			uiv.getCur_elem().nodeUp();
			uiv.formatAndPaint(false);
		}
		else if (cmd == "insert-mode") {
			uiv.insert_mode = !uiv.insert_mode;
			uiv.formatAndPaint(false);
		}
	}

	final static class NodeUp implements UIActionFactory {
		public String getDescr() { return "Select parent node"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().node != null && context.editor.getCur_elem().node.parent() != null)
				return new NavigateNode(context.editor, "select-up");
			return null;
		}
	}

	final static class InsertMode implements UIActionFactory {
		public String getDescr() { return "Change insert/command editor mode"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateNode(context.editor, "insert-mode");
			return null;
		}
	}
	
}
