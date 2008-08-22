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

import kiev.fmt.ATextSyntax;
import kiev.fmt.DrawFolded;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.SyntaxManager;
import kiev.fmt.XmlDumpSyntax;
import kiev.vtree.ANode;
import kiev.vtree.TreeWalker;

public final class RenderActions implements IPopupMenuListener, Runnable {
	
	final UIView ui;
	final String action;
	
	private IPopupMenuPeer menu;
	
	RenderActions(UIView ui, String action) {
		this.ui = ui;
		this.action = action;
	}
	
	public void run() {
		UIView ui = this.ui;
		if (action == "select-syntax") {
			// build a menu of types to instantiate
			menu = UIManager.newPopupMenu(ui, this);
			if (ui instanceof InfoView) {
				menu.addItem(new SetSyntaxAction(ui,"Kiev Syntax", "stx-fmt·syntax-for-java", false));
				menu.addItem(new SetSyntaxAction(ui,"Kiev Syntax (current)", "stx-fmt·syntax-for-java", true));
				menu.addItem(new SetSyntaxAction(ui,"XML dump Syntax (full)", XmlDumpSyntax.class, "full"));
				menu.addItem(new SetSyntaxAction(ui,"XML dump Syntax (api)", XmlDumpSyntax.class, "api"));
				menu.addItem(new SetSyntaxAction(ui,"Project Tree Syntax", "stx-fmt·syntax-for-project-tree", false));
				menu.addItem(new SetSyntaxAction(ui,"Project Tree Syntax  (current)", "stx-fmt·syntax-for-project-tree", true));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for API", "stx-fmt·syntax-for-api", false));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for VDOM", "stx-fmt·syntax-for-vdom", false));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for VDOM (current)", "stx-fmt·syntax-for-vdom", true));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for Events", "stx-fmt·syntax-for-evt", false));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for Events (current)", "stx-fmt·syntax-for-evt", true));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for Syntax", "stx-fmt·syntax-for-syntax", false));
				menu.addItem(new SetSyntaxAction(ui,"Syntax for Syntax (current)", "stx-fmt·syntax-for-syntax", true));
				menu.showAt(0, 0);
			}
			else if (ui instanceof ProjectView) {
				menu.addItem(new SetSyntaxAction(ui,"Project Tree Syntax", "stx-fmt·syntax-for-project-tree", false));
				menu.addItem(new SetSyntaxAction(ui,"Project Tree Syntax  (current)", "stx-fmt·syntax-for-project-tree", true));
				menu.showAt(0, 0);
			}
		}
		else if (action == "unfold-all") {
			if (ui instanceof InfoView) {
				ui.view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) ((DrawFolded)n).setDrawFolded(false); return true; }
				});
			}
			ui.formatAndPaint(true);
		}
		else if (action == "fold-all") {
			if (ui instanceof InfoView) {
				ui.view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) ((DrawFolded)n).setDrawFolded(true); return true; }
				});
			}
			ui.formatAndPaint(true);
		}
		else if (action == "toggle-autogen") {
			if (ui instanceof InfoView)
				ui.show_auto_generated = !ui.show_auto_generated;
			ui.formatAndPaint(true);
		}
		else if (action == "toggle-placeholder") {
			if (ui instanceof InfoView)
				ui.show_placeholders = !ui.show_placeholders;
			ui.formatAndPaint(true);
		}
		else if (action == "toggle-escape") {
			if (ui instanceof InfoView)
				ui.show_hint_escapes = !ui.show_hint_escapes;
			ui.formatAndPaint(true);
		}
		else if (action == "redraw") {
			ui.setSyntax(ui.syntax);
			if (ui instanceof Editor)
				((Editor)ui).getCur_elem().set(ui.view_root.getFirstLeaf());
			//ui.view_canvas.root = ui.view_root;
			ui.formatAndPaint(false);
		}
	}

	public void popupMenuCanceled() {
		menu.remove();
		((Editor)ui).stopItemEditor(true);
	}
	public void popupMenuExecuted(IMenuItem item) {
		SetSyntaxAction sa = (SetSyntaxAction)item;
		menu.remove();
		try {
			sa.run();
			sa = null;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	static class SetSyntaxAction implements IMenuItem {
		private String text;
		private UIView uiv;
		private Class<? extends ATextSyntax> clazz;
		private String qname;
		private boolean in_project;
		SetSyntaxAction(UIView uiv, String text, Class<? extends ATextSyntax> clazz, String name) {
			this.text = text;
			this.uiv = uiv;
			this.clazz = clazz;
			this.qname = name;
		}
		SetSyntaxAction(UIView uiv, String text, String qname, boolean in_project) {
			this.text = text;
			this.uiv = uiv;
			this.qname = qname;
			this.in_project = in_project;
		}
		public String getText() {
			return text;
		}
		public void run() {
			if (clazz != null) {
				ATextSyntax stx = null;
				try {
					stx = (ATextSyntax)clazz.newInstance();
				} catch (Exception ex) {
					ex.printStackTrace();
					return;
				}
				if (stx instanceof XmlDumpSyntax)
					((XmlDumpSyntax)stx).setDump(qname);
				this.uiv.setSyntax(stx.getCompiled().init());
				return;
			}
			Draw_ATextSyntax stx = SyntaxManager.getLanguageSyntax(qname, in_project);
			this.uiv.setSyntax(stx);
		}
	}
/*
	static class LoadSyntaxAction extends TextAction {
		private UIView uiv;
		private String file;
		private String name;
		LoadSyntaxAction(UIView uiv, String text, String file, String name) {
			super(text);
			this.uiv = uiv;
			this.file = file.replace('/',File.separatorChar);
			this.name = name.intern();
		}
		public void actionPerformed(ActionEvent e) {
			FileUnit fu = null;
			Transaction tr = Transaction.open("Actions.java:LoadSyntaxAction()");
			try {
				EditorThreadGroup thrg = EditorThreadGroup;
				fu = Env.getRoot().loadFromXmlFile(new File(this.file), null);
				try {
					thrg.errCount = 0;
					thrg.warnCount = 0;
					Compiler.runFrontEnd(thrg,null,fu);
				} catch (Throwable t) { t.printStackTrace(); }
				System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
				Kiev.lockNodeTree(fu);
			} catch( IOException e ) {
				System.out.println("Read error while syntax importing: "+e);
			} finally { tr.close(); }

			foreach (ATextSyntax stx; fu.members; stx.sname == name) {
				this.uiv.setSyntax(stx.getCompiled().init());
				return;
			}
		}
	}
*/
	public final static class SyntaxFileAs implements UIActionFactory {
		public String getDescr() { return "Set the syntax of the curret view"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "select-syntax");
		}
	}

	public final static class OpenFoldedAll implements UIActionFactory {
		public String getDescr() { return "Open (unfold) all folded elements"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.ui, "unfold-all");
		}
	}

	public final static class CloseFoldedAll implements UIActionFactory {
		public String getDescr() { return "Close (fold) all foldable elements"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.ui, "fold-all");
		}
	}

	public final static class ToggleShowAutoGenerated implements UIActionFactory {
		public String getDescr() { return "Toggle show of auto-generated code"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-autogen");
		}
	}

	public final static class ToggleShowPlaceholders implements UIActionFactory {
		public String getDescr() { return "Toggle show of editor placeholders"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-placeholder");
		}
	}

	public final static class ToggleHintEscaped implements UIActionFactory {
		public String getDescr() { return "Toggle idents and strings escaping"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-escape");
		}
	}

	public final static class Redraw implements UIActionFactory {
		public String getDescr() { return "Redraw the window"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "redraw");
		}
	}
}

