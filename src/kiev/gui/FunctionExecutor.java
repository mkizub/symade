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
import kiev.fmt.common.Draw_FuncEval;
import kiev.fmt.common.Draw_FuncNewNode;
import kiev.fmt.common.Draw_FuncSetEnum;
import kiev.fmt.common.Draw_SyntaxFunc;
import kiev.gui.Editor;
import kiev.gui.NewElemHere;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIManager;
import kiev.gui.EditActions.ChooseItemEditor;

/**
 * Function Executor UI Action.
 */
public final class FunctionExecutor implements IPopupMenuListener, UIAction {

	/**
	 * The menu.
	 */
	private IPopupMenuPeer menu;
	
	/**
	 * The actions.
	 */
	private final java.util.Vector<IMenuItem>	actions;
	
	/**
	 * The editor.
	 */
	private final Editor editor;

	/**
	 * The constructor.
	 * @param editor editor
	 */
	public FunctionExecutor(Editor editor) {
		this.editor = editor;
		actions = new java.util.Vector<IMenuItem>();
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		menu = editor.getViewPeer().getPopupMenu(this, null);
		for (IMenuItem act: actions) menu.addItem(act);
		DrawLayoutInfo cur_dtli = editor.getDrawTerm().getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.height;
		int y = cur_dtli.getY() + h - editor.getViewPeer().getVertOffset();
		menu.showAt(x, y);
	}

	/**
	 * Function Executor UI Action Factory.
	 */
	public final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Popup list of functions for a current element"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			Editor editor = context.editor;
			Drawable dr = context.dt;
			if (editor == null ||  dr == null || dr.drnode != context.node) return null;
			Draw_SyntaxFunc[] sfs_funcs = dr.syntax.elem_decl.funcs;
			FunctionExecutor fe = new FunctionExecutor(editor);
			if (sfs_funcs != null && sfs_funcs.length > 0) {
				for (Draw_SyntaxFunc sfunc: sfs_funcs) {
					try {
						if (sfunc instanceof Draw_FuncNewNode) {
							ActionPoint ap = context.ap;
							if (ap.curr_node != null && ap.curr_slot != null) {
								if (NewElemEditor.checkNewFuncAvailable(ap.curr_syntax))
									fe.actions.add(fe.new NewElemAction(sfunc.title));
							}
							if (ap.space_node != null) {
								if (NewElemEditor.checkNewFuncAvailable(ap.space_syntax))
									fe.actions.add(fe.new NewElemAction(sfunc.title));
							}
							continue;
						}
						else if (sfunc instanceof Draw_FuncSetEnum) {
							if (context.ap.curr_node != null) {
								Draw_FuncSetEnum fs = (Draw_FuncSetEnum)sfunc;
								IMenu m = fs.makeMenu(context.ap.curr_node);
								fe.actions.add(m);
							}
							continue;
						}
						if (!(sfunc instanceof Draw_FuncEval))
							continue;
						dr = editor.getFunctionTarget(sfunc);
						if (dr == null) continue;
						Draw_FuncEval sf = (Draw_FuncEval)sfunc;
						if(sf.act == null)
							continue;
						try {
							Class<?> c = Class.forName(sf.act);
							UIActionFactory af = (UIActionFactory)c.newInstance();
							if (! af.isForPopupMenu()) continue;
							UIAction action = af.getAction(new UIActionViewContext(editor.window, null, editor));
							if (action != null) fe.actions.add(fe.new RunFuncAction(sf.title, action));
						} catch (Throwable t) {}
					} catch (Throwable t) {}
				}
			}
			
			for (UIActionFactory af: UIManager.getUIActions(context.ui).getAllActions()) {
				if(af.isForPopupMenu()) {
					try {
						UIAction action = af.getAction(new UIActionViewContext(editor.window, null, editor));
						if (action != null) fe.actions.add(fe.new RunFuncAction(af.getDescr(), action));
					} catch (Throwable t) {}
				}
			}
			if (fe.actions.size() > 0) return fe;
			return null;
		}
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
		editor.getWindow().getEditorThreadGroup().runTaskLater(new Runnable() {
			public void run() {
				action.exec();
				editor.formatAndPaint(true);
			}
		});
	}

	/**
	 * New Element Action.
	 */
	public class NewElemAction implements IMenuItem {
		
		/**
		 * The text.
		 */
		private final String text;
		
		/**
		 * The constructor.
		 * @param text the text
		 * @param node the node
		 * @param stx the draw syntax attribute
		 * @param tstx the draw text syntax
		 * @param attr_name the attribute name
		 */
		public NewElemAction(String text) {
			this.text = text;
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
			ActionPoint ap = editor.getActionPoint();
			NewElemHere neh = new NewElemHere(editor, editor.getActionPoint(), ap.prev_index);
			neh.exec();
		}
	}

	/**
	 * Edit Element UI Action.
	 */
	public class EditElemAction implements IMenuItem {
		
		/**
		 * The text.
		 */
		private final String text;
		
		/**
		 * The constructor.
		 * @param text the text
		 * @param dr the drawable
		 */
		public EditElemAction(String text) {
			this.text = text;
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
			UIAction action = new ChooseItemEditor().getAction(new UIActionViewContext(editor.window, null, editor));
			if (action != null) action.exec();
		}
	}

	/**
	 * Run Function Action.
	 */
	public class RunFuncAction implements IMenuItem {
		
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
		public RunFuncAction(String text, UIAction action) {
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
}

