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
package kiev.gui.swt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import kiev.CompilerThreadGroup;

import kiev.Compiler;
import kiev.EditorThreadGroup;
import kiev.CompilerParseInfo;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Drawable;
import kiev.fmt.SyntaxManager;
import kiev.gui.Editor;
import kiev.gui.IWindow;
import kiev.gui.InfoView;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIManager;
import kiev.vlang.DumpUtils;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;
import kiev.vtree.Transaction;

public final class FileActions implements Runnable {
	
	final IWindow wnd;
	final InfoView uiv;
	final Drawable dr;
	final String action;

	static class DumpFileFilter {
		final String syntax_qname;
		final String description;
		final String extension;
		
		static String getSyntax_qname(String extension){
			for (DumpFileFilter i: dumpFileFilters) 
				if (i.extension.equals(extension)) {return i.syntax_qname;}
			return null;
		}
		DumpFileFilter(String syntax_qname, String description, String extension) {
			this.syntax_qname = syntax_qname;
			this.description = description;
			this.extension = extension;
		}
		public String getDescription() { return description; }
		public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith("."+extension); }
	}
	
	static DumpFileFilter[] dumpFileFilters = {
		new DumpFileFilter("stx-fmt·syntax-for-java",  "Kiev source files", "java"),
		new DumpFileFilter("<xml-dump>", "Kiev XML full dump", "xml"),
	};
	
		
	FileActions(IWindow wnd, String action) {
		this.wnd = wnd;
		this.action = action;
		this.uiv = null;
		this.dr = null;
	}
	
	FileActions(InfoView uiv, String action) {
		this.wnd = uiv.parent_window;
		this.uiv = uiv;
		this.action = action;
		this.dr = null;
	}
	
	FileActions(InfoView uiv, Drawable dr, String action) {
		this.wnd = uiv.parent_window;
		this.uiv = uiv;
		this.action = action;
		this.dr = dr;
	}
	
	public void run() {
		Shell shell = ((Window)wnd).shell;
		if (action == "save-as") {
			FileUnit fu;
			if (uiv.the_root instanceof FileUnit)
				fu = (FileUnit)uiv.the_root;
			else
				fu = (FileUnit)this.uiv.the_root.get$ctx_file_unit();
			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
	    
			File f = new File(fu.pname());
			if (f.getParentFile() != null)
				dialog.setFilterPath(f.getParentFile().getPath());
			ArrayList<String> filterNames = new ArrayList<String>(); 
			ArrayList<String> filterExtensions = new ArrayList<String>();
			for (DumpFileFilter dff: dumpFileFilters) 
				if (fu.getCurrentSyntax().equals(dff.syntax_qname)){
					filterNames.add(dff.description);
					filterExtensions.add("*."+dff.extension);
				}
			dialog.setFilterNames(filterNames.toArray(new String[filterNames.size()]));
	    dialog.setFilterExtensions(filterExtensions.toArray(new String[filterExtensions.size()])); 
	    dialog.setFileName(f.getName());
	    String name = dialog.open();
			if (name == null)
				return;
			String ext = name.substring(name.lastIndexOf('.')+1);
			String stx_name = DumpFileFilter.getSyntax_qname(ext);
			try {
				if ("<xml-dump>".equals(stx_name)) {
					DumpUtils.dumpToXMLFile("full", fu, new File(name));
					fu.setCurrentSyntax("<xml-dump>");
				} else {
					Draw_ATextSyntax stx = SyntaxManager.loadLanguageSyntax(stx_name);
					SyntaxManager.dumpTextFile(fu, new File(name), stx);
					fu.setCurrentSyntax(stx.q_name);
				}
			} catch( IOException e ) {
				System.out.println("Create/write error while Kiev-to-Xml exporting: "+e);
			}
		}
		else if (action == "save-as-api") {
			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			DumpFileFilter dff = new DumpFileFilter("<xml-dump-api>",  "Kiev XML API dump", "xml");
			dialog.setFilterNames(new String[]{dff.description});
			dialog.setFilterExtensions(new String[]{"*."+dff.extension});
	    String name = dialog.open();
			if (name == null)
				return;
			try {
				DumpUtils.dumpToXMLFile("api", (ASTNode)uiv.the_root, new File(name));
			} catch (IOException e) {
				System.out.println("Create/write error while Kiev-to-Xml API exporting: "+e);
			}
		}
		else if (action == "save") {
			FileUnit fu;
			if (uiv.the_root instanceof FileUnit)
				fu = (FileUnit)uiv.the_root;
			else
				fu = (FileUnit)uiv.the_root.get$ctx_file_unit();
			String stx_name = fu.getCurrentSyntax();
			Draw_ATextSyntax stx = null;
			if (stx_name != null && !"<xml-dump>".equals(stx_name)) {
				stx = SyntaxManager.loadLanguageSyntax(fu.getCurrentSyntax());
				stx_name = fu.getCurrentSyntax();
			}
			File f = new File(fu.pname());
			if (stx == null && !"<xml-dump>".equals(stx_name) || stx != uiv.syntax) {
				FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		    dialog.setFileName(fu.pname());
				f = new File(fu.pname());
				if (f.getParentFile() != null)
					dialog.setFilterPath(f.getParentFile().getPath());
				ArrayList<String> filterNames = new ArrayList<String>(); 
				ArrayList<String> filterExtensions = new ArrayList<String>();
				for (DumpFileFilter dff: dumpFileFilters) 
					if (fu.getCurrentSyntax() == dff.syntax_qname){
						filterNames.add(dff.description);
						filterExtensions.add("*."+dff.extension);
					}
				dialog.setFilterNames(filterNames.toArray(new String[filterNames.size()]));
		    dialog.setFilterExtensions(filterExtensions.toArray(new String[filterExtensions.size()])); 
		    String name = dialog.open();
				if (name == null)
					return;
				String ext = name.substring(name.lastIndexOf('.')+1);
				stx_name = DumpFileFilter.getSyntax_qname(ext);
				f = new File(name);
				if (!"<xml-dump>".equals(stx_name))
					stx = SyntaxManager.loadLanguageSyntax(stx_name);
			}
			try {
				if ("<xml-dump>".equals(stx_name)) {
					DumpUtils.dumpToXMLFile("full", fu, f);
				} else {
					SyntaxManager.dumpTextFile(fu, f, stx);
				}
				fu.setCurrentSyntax(stx_name);
			} catch( IOException e ) {
				System.out.println("Create/write error while Kiev-to-Xml exporting: "+e);
			}
		}
		else if (action == "load-as") {
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			DumpFileFilter dff = new DumpFileFilter("<xml-dump>","XML file for node tree import", "xml"); 
			dialog.setFilterNames(new String[]{dff.description});
			dialog.setFilterExtensions(new String[]{"*."+dff.extension});
			dialog.setFilterPath(Env.getProject().get$root_dir().get$name());
			String name = dialog.open();
			if (name == null)
				return;
			File f = new File(name);
			if (!dff.accept(f)) return;
			FileUnit fu = Env.getProject().getLoadedFile(f);
			if (fu == null) {
				CompilerParseInfo cpi = new CompilerParseInfo(f, false);
				Transaction tr = Transaction.open("Actions.java:load-as");
				try {
					EditorThreadGroup thrg = EditorThreadGroup.getInst();
					Compiler.runFrontEnd(thrg,new CompilerParseInfo[]{cpi},null);
					System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
					fu = cpi.fu;
				} catch( Exception e ) {
					System.out.println("Read error while Xml-to-Kiev importing: "+e);
				} finally { tr.close(); }
			}
			if (fu != null)
				wnd.openEditor(fu);
		}
		else if (action == "merge-all") {
			Env.getRoot().mergeTree();
			System.out.println("Tree merged to the editor version.");
		}
		else if (action == "run-backend") {
			System.out.println("Running backend compiler...");
			CompilerThreadGroup thrg = CompilerThreadGroup.getInst();
			thrg.errCount = 0;
			thrg.warnCount = 0;
			Compiler.runBackEnd(thrg, Env.getRoot(), null);
		}
		else if (action == "run-frontend-all") {
			runFrontEndCompiler((Editor)uiv, Env.getRoot());
		}
		else if (action == "run-frontend") {
			runFrontEndCompiler((Editor)uiv, uiv.the_root);
		}
		else if (action == "use-bindings") {
			kiev.fmt.evt.BindingSet bs = (kiev.fmt.evt.BindingSet)dr.drnode;
			UIManager.attachEventBindings(bs.getCompiled().init());
		}
		else if (action == "reset-bindings") {
			UIManager.resetEventBindings();
		}
	}

	private void runFrontEndCompiler(Editor editor, ANode root) {
		System.out.println("Running frontend compiler...");
		Transaction tr = Transaction.open("Actions.java:runFrontEndCompiler()");
		try {
			editor.changes.push(tr);
			EditorThreadGroup thrg = EditorThreadGroup.getInst();
			Compiler.runFrontEnd(thrg,null,root);
			System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
		} finally {
			if (tr.isEmpty()) {
				tr.close();
				editor.changes.pop();
			} else {
				tr.close();
			}
		}
		editor.formatAndPaint(true);
	}
	

	final static class SaveFileAs implements UIActionFactory {
		public String getDescr() { return "Save the file as a new file"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (!(context.uiv != null && context.uiv.the_root instanceof ASTNode))
				return null;
			return new FileActions(context.uiv, "save-as");
		}
	}

	final static class SaveFileAsApi implements UIActionFactory {
		public String getDescr() { return "Save the file as a new API file"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (!(context.uiv != null && context.uiv.the_root instanceof ASTNode))
				return null;
			return new FileActions(context.uiv, "save-as-api");
		}
	}

	final static class SaveFile implements UIActionFactory {
		public String getDescr() { return "Save the file"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (!(context.uiv != null && context.uiv.the_root instanceof ASTNode))
				return null;
			return new FileActions(context.uiv, "save");
		}
	}

	final static class LoadFileAs implements UIActionFactory {
		public String getDescr() { return "Load a file into current view as a file with specified syntax"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "load-as");
		}
	}

	final static class MergeTreeAll implements UIActionFactory {
		public String getDescr() { return "Merge editor's changes into working tree for the whole project"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "merge-all");
		}
	}

	final static class RunBackendAll implements UIActionFactory {
		public String getDescr() { return "Run back-end compilation for the whole project"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv != null)
				return new FileActions(context.uiv, "run-backend");
			return null;
		}
	}

	final static class RunFrontendAll implements UIActionFactory {
		public String getDescr() { return "Run front-end compilation for the whole project"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new FileActions(context.editor, "run-frontend-all");
			return null;
		}
	}

	final static class RunFrontend implements UIActionFactory {
		public String getDescr() { return "Run front-end compilation for the current compilation unit"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new FileActions(context.editor, "run-frontend");
			return null;
		}
	}
	
	final static class UseEventBindings implements UIActionFactory {
		public String getDescr() { return "Compile and use event bindings"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.dr != null && context.dr.drnode instanceof kiev.fmt.evt.BindingSet)
				return new FileActions(context.editor, context.dr, "use-bindings");
			return null;
		}
	}

	final static class ResetEventBindings implements UIActionFactory {
		public String getDescr() { return "Reset event bindings to default"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "reset-bindings");
		}
	}

}
