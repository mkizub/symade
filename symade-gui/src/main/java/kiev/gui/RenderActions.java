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
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.SyntaxManager;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.fmt.common.Draw_StyleSheet;
import kiev.fmt.common.TextDrawSyntax;
import kiev.vtree.AttrSlot;
import kiev.vtree.INode;
import kiev.vtree.ITreeWalker;
import kiev.vlang.ProjectStyleInfo;
import kiev.vlang.ProjectSyntaxFactory;
import kiev.vlang.ProjectSyntaxFactoryAny;
import kiev.vlang.ProjectSyntaxInfo;
import kiev.Kiev;

/**
 * Render Actions UI Action.
 */
public final class RenderActions implements IPopupMenuListener, UIAction {
	
	private static ProjectSyntaxInfo PROJECT_TREE_SYNTAX;
	private static ProjectSyntaxInfo KIEV_SYNTAX;
	private static ProjectSyntaxInfo SYNTAX_SYNTAX;
	private static ProjectStyleInfo  STYLE_NONE;
	private static ProjectStyleInfo  STYLE_DEFAULT;
	static {
		ProjectSyntaxFactoryAny sci;
		PROJECT_TREE_SYNTAX = new ProjectSyntaxInfo();
		PROJECT_TREE_SYNTAX.setDescription("Project Tree Syntax");
		sci = new ProjectSyntaxFactoryAny();
		sci.setFactory("kiev·fmt·common·DefaultTextProcessor");
		sci.addParam("class", "stx-fmt·syntax-for-project-tree");
		PROJECT_TREE_SYNTAX.setSyntax(sci);

		KIEV_SYNTAX = new ProjectSyntaxInfo();
		KIEV_SYNTAX.setDescription("Kiev Syntax");
		sci = new ProjectSyntaxFactoryAny();
		sci.setFactory("kiev·fmt·common·DefaultTextProcessor");
		sci.addParam("class", "stx-fmt·syntax-for-java");
		KIEV_SYNTAX.setSyntax(sci);

		SYNTAX_SYNTAX = new ProjectSyntaxInfo();
		SYNTAX_SYNTAX.setDescription("Syntax for Syntax");
		sci = new ProjectSyntaxFactoryAny();
		sci.setFactory("kiev·fmt·common·DefaultTextProcessor");
		sci.addParam("class", "stx-fmt·syntax-for-syntax");
		SYNTAX_SYNTAX.setSyntax(sci);
		
		STYLE_NONE = new ProjectStyleInfo();
		STYLE_NONE.setDescription("Erase style");
		
		STYLE_DEFAULT = new ProjectStyleInfo();
		STYLE_DEFAULT.setDescription("Default style");
		STYLE_DEFAULT.setQname("stx-fmt·style-sheet-default");
	}
	
	/**
	 * The UI View.
	 */
	private final IUIView ui;
	
	/**
	 * The action
	 * @see #RenderActions(UIView, String)
	 */
	private final String action;
	
	/**
	 * The popup menu.
	 */
	private IPopupMenuPeer menu;
	
	/**
	 * The constructor.
	 * @param ui the view
	 * @param action the action
	 */
	public RenderActions(IUIView ui, String action) {
		this.ui = ui;
		this.action = action;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		IUIView ui = this.ui;
		if (action == "select-syntax") {
			// build a menu of types to instantiate
			menu = ui.getViewPeer().getPopupMenu(this, null);
			if (ui instanceof ProjectView) {
				menu.addItem(new SetSyntaxAction(ui,PROJECT_TREE_SYNTAX, false));
				menu.addItem(new SetSyntaxAction(ui,PROJECT_TREE_SYNTAX, true));
				menu.showAt(0, 0);
			} else {
				menu.addItem(new SetSyntaxAction(ui,KIEV_SYNTAX, false));
				menu.addItem(new SetSyntaxAction(ui,KIEV_SYNTAX, true));
				menu.addItem(new SetSyntaxAction(ui,SYNTAX_SYNTAX, false));
				menu.addItem(new SetSyntaxAction(ui,SYNTAX_SYNTAX, true));
				for (ProjectSyntaxInfo psi : ui.getWindow().getCurrentProject().getSyntax_infos()) {
					menu.addItem(new SetSyntaxAction(ui, psi, false));
					menu.addItem(new SetSyntaxAction(ui, psi, true));
				}
				menu.showAt(0, 0);
			}
		}
		if (action == "select-style") {
			// build a menu of types to instantiate
			menu = ui.getViewPeer().getPopupMenu(this, null);
			menu.addItem(new SetStyleAction(ui,STYLE_NONE, false));
			menu.addItem(new SetStyleAction(ui,STYLE_DEFAULT, false));
			menu.addItem(new SetStyleAction(ui,STYLE_DEFAULT, true));
			for (ProjectStyleInfo psi : ui.getWindow().getCurrentProject().getStyle_infos()) {
				menu.addItem(new SetStyleAction(ui, psi, false));
				menu.addItem(new SetStyleAction(ui, psi, true));
			}
			menu.showAt(0, 0);
		}
		else if (action == "unfold-all") {
			ui.getViewRoot().walkTree(null, null, new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					if (n instanceof DrawFolded) ((DrawFolded)n).setDrawFolded(false); return true;
				}
			});
			ui.formatAndPaint(true);
		}
		else if (action == "fold-all") {
			ui.getViewRoot().walkTree(null, null, new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					if (n instanceof DrawFolded) ((DrawFolded)n).setDrawFolded(true); return true;
				}
			});
			ui.formatAndPaint(true);
		}
		else if (action == "toggle-placeholder" && ui instanceof Editor) {
			Editor edt = (Editor)ui;
			edt.show_placeholders = !edt.show_placeholders;
			final boolean hide = !edt.show_placeholders;
			ui.getViewRoot().walkTree(null, null, new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					if (n instanceof DrawPlaceHolder) ((DrawPlaceHolder)n).is_hidden = hide; return true;
				}
			});
			ui.formatAndPaint(true);
		}
		else if (action == "redraw") {
			ui.setSyntax(ui.getSyntax());
			if (ui instanceof Editor)
				((Editor)ui).setDrawTerm(ui.getViewRoot().getFirstLeaf());
			ui.formatAndPaint(true);
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
	public void popupMenuExecuted(final IMenuItem item) {
		menu.remove();
		ui.getWindow().getEditorThreadGroup().runTaskLater(new Runnable() {
			public void run() {
				item.exec();
			}
		});
	}
	
	/**
	 * Set Syntax Action.
	 */
	public class SetSyntaxAction implements IMenuItem {
		
		/**
		 * The UI View.
		 */
		final IUIView uiv;

		/**
		 * The text.
		 */
		final String text;
		
		/**
		 * The qualified name.
		 */
		final ProjectSyntaxInfo psi;
		
		/**
		 * Is in project.
		 */
		final boolean in_project;

		/**
		 * The constructor.
		 * @param uiv the view
		 * @param text the text
		 * @param qname the name
		 * @param in_project in project
		 */
		public SetSyntaxAction(IUIView uiv, ProjectSyntaxInfo psi, boolean in_project) {
			this.uiv = uiv;
			this.text = psi.getDescription() + (in_project ? " (current)" : "");
			this.psi = psi;
			this.in_project = in_project;
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
			ProjectSyntaxFactory pcs = psi.getSyntax();
			TextDrawSyntax tds = (TextDrawSyntax)pcs.makeTextProcessor();
			if (in_project)
				tds.setProperty("current", "true");
			Draw_ATextSyntax stx = tds.lookup(in_project ? uiv.getWindow().getCurrentEnv() : null);
			uiv.setSyntax(stx);
		}
	}

	/**
	 * Set Syntax Action.
	 */
	public class SetStyleAction implements IMenuItem {
		
		/**
		 * The UI View.
		 */
		final IUIView uiv;

		/**
		 * The text.
		 */
		final String text;
		
		/**
		 * The qualified name.
		 */
		final ProjectStyleInfo psi;
		
		/**
		 * Is in project.
		 */
		final boolean in_project;

		/**
		 * The constructor.
		 * @param uiv the view
		 * @param text the text
		 * @param qname the name
		 * @param in_project in project
		 */
		public SetStyleAction(IUIView uiv, ProjectStyleInfo psi, boolean in_project) {
			this.uiv = uiv;
			this.text = psi.getDescription() + (in_project ? " (current)" : "");
			this.psi = psi;
			this.in_project = in_project;
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
			if (psi == STYLE_NONE) {
				uiv.setStyle(null);
				return;
			}
			Draw_StyleSheet dss = SyntaxManager.getStyleSheet(psi.getQname(), in_project ? uiv.getWindow().getCurrentEnv() : null);
			uiv.setStyle(dss);
		}
	}

	/**
	 * Syntax File As UI Action Factory.
	 */
	public final static class SyntaxFileAs implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Set the syntax of the curret view"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "select-syntax");
		}
	}

	/**
	 * Syntax File As UI Action Factory.
	 */
	public final static class StyleAs implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Set the style of the curret syntax"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "select-style");
		}
	}

	/**
	 * Open Folded All UI Action Factory.
	 */
	public final static class OpenFoldedAll implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Open (unfold) all folded elements"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.ui == null || context.ui.getRoot() == null)
				return null;
			return new RenderActions(context.ui, "unfold-all");
		}
	}

	/**
	 * Close Folded All UI Action Factory.
	 */
	public final static class CloseFoldedAll implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Close (fold) all foldable elements"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.ui == null || context.ui.getRoot() == null)
				return null;
			return new RenderActions(context.ui, "fold-all");
		}
	}

	/**
	 * Toggle Show Place holders UI Action Factory.
	 */
	public final static class ToggleShowPlaceholders implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Toggle show of editor placeholders"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-placeholder");
		}
	}

	/**
	 * Redraw UI Action Factory.
	 */
	public final static class Redraw implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Redraw the window"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "redraw");
		}
	}
		
}

