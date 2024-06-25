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

import kiev.fmt.Drawable;
import kiev.fmt.common.DrawLayoutInfo;

/**
 * Project Actions UI Action.
 */
public class ProjectActions implements UIAction, IPopupMenuListener  {

	/**
	 * The menu.
	 */
	private IPopupMenuPeer menu;
	
	/**
	 * The actions.
	 */
	private final java.util.Vector<IMenuItem> actions = new java.util.Vector<IMenuItem>();

	
	private final ProjectView view;

	/**
	 * The singleton.
	 */
	public ProjectActions(ProjectView view){
		this.view = view;
	}

	/**
	 * Add File Factory.
	 */
	public static final class Factory implements UIActionFactory {

		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Pop-up list of actions for a current project element"; }

		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return true; }

		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			ProjectView view = (ProjectView)context.ui;
			Drawable dr = context.dt;
			if (view == null || dr == null) return null;
			ProjectActions pa = new ProjectActions(view);
			for (UIActionFactory af: UIManager.getUIActions(context.ui).getAllActions()) {
				if(af.isForPopupMenu()) {
					try {
						UIAction action = af.getAction(new UIActionViewContext(view.window, null, view));
						if (action != null) pa.actions.add(pa.new RunMenuAction(af.getDescr(), action));
					} catch (Throwable t) {}
				}
			}
			if (pa.actions.size() > 0) return pa;
			return null;
		}
	}

	/**
	 * Run Function Action.
	 */
	public class RunMenuAction implements IMenuItem {
		
		/**
		 * The text.
		 */
		private final String text;
		
		/**
		 * The action.
		 */
		private final UIAction action;
		
		/**
		 * The constructor.
		 * @param text the text
		 * @param action the action
		 */
		public RunMenuAction(String text, UIAction action) {
			this.text = text;
			this.action = action;
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.IMenuItem#getText()
		 */
		public String getText() {
			return text;
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIAction#run()
		 */
		public void exec() {
			action.exec();
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		menu = view.getViewPeer().getPopupMenu(this, null);
		for (IMenuItem act: actions) menu.addItem(act);
		DrawLayoutInfo cur_dtli = view.getViewPeer().getCurrent().getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.height;
		int y = cur_dtli.getY() + h - view.getViewPeer().getVertOffset();
		menu.showAt(x, y);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuListener#popupMenuCanceled()
	 */
	public void popupMenuCanceled() {
		menu.remove();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuListener#popupMenuExecuted(kiev.gui.IMenuItem)
	 */
	public void popupMenuExecuted(IMenuItem item) {
		final UIAction action = (UIAction)item;
		menu.remove();
		view.getWindow().getEditorThreadGroup().runTaskLater(new Runnable() {
			public void run() {
				action.exec();
			}
		});
	}

}
