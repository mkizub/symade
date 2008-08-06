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
package kiev.gui.swing;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import kiev.Compiler;
import kiev.CompilerParseInfo;
import kiev.CompilerThreadGroup;
import kiev.EditorThreadGroup;
import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.SyntaxManager;
import kiev.fmt.XmlDumpSyntax;
import kiev.gui.Editor;
import kiev.gui.IWindow;
import kiev.gui.InfoView;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vlang.DumpUtils;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;
import kiev.vtree.Transaction;

public final class FileActions implements Runnable {
	
	static class DumpFileFilter extends FileFilter {
		final String syntax_qname;
		final String description;
		final String extension;
		DumpFileFilter(String syntax_qname, String description, String extension) {
			this.syntax_qname = syntax_qname;
			this.description = description;
			this.extension = extension;
		}
		public String getDescription() { return description; }
		public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith("."+extension); }
	}
	
	static DumpFileFilter[] dumpFileFilters = {
		new DumpFileFilter("stx-fmt\u001fsyntax-for-java",  "Kiev source files", "java"),
		new DumpFileFilter("<xml-dump>", "Kiev XML full dump", "xml"),
	};
	
	final IWindow wnd;
	final InfoView uiv;
	final String action;
	
	FileActions(IWindow wnd, String action) {
		this.wnd = wnd;
		this.action = action;
		this.uiv = null;
	}
	
	FileActions(InfoView uiv, String action) {
		this.wnd = uiv.parent_window;
		this.uiv = uiv;
		this.action = action;
	}
	
	public void run() {
		if (action == "save-as") {
			FileUnit fu;
			if (uiv.the_root instanceof FileUnit)
				fu = (FileUnit)uiv.the_root;
			else
				fu = (FileUnit)this.uiv.the_root.get$ctx_file_unit();
			JFileChooser jfc = new JFileChooser(".");
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);
			File f = new File(fu.pname());
			if (f.getParentFile() != null)
				jfc.setCurrentDirectory(f.getParentFile());
			jfc.setSelectedFile(f);
			jfc.setAcceptAllFileFilterUsed(false);
			for (DumpFileFilter dff: dumpFileFilters)
				jfc.addChoosableFileFilter(dff);
			for (DumpFileFilter dff: dumpFileFilters) 
				if (fu.getCurrentSyntax() == dff.syntax_qname)
					jfc.setFileFilter(dff);
			if (JFileChooser.APPROVE_OPTION != jfc.showDialog(null, "Save"))
				return;
			DumpFileFilter dff = (DumpFileFilter)jfc.getFileFilter();
			try {
				if ("<xml-dump>".equals(dff.syntax_qname)) {
					DumpUtils.dumpToXMLFile("full", fu, jfc.getSelectedFile());
					fu.setCurrentSyntax("<xml-dump>");
				} else {
					Draw_ATextSyntax stx = SyntaxManager.loadLanguageSyntax(dff.syntax_qname);
					SyntaxManager.dumpTextFile(fu, jfc.getSelectedFile(), stx);
					fu.setCurrentSyntax(stx.q_name);
				}
			} catch( IOException e ) {
				System.out.println("Create/write error while Kiev-to-Xml exporting: "+e);
			}
		}
		else if (action == "save-as-api") {
			JFileChooser jfc = new JFileChooser(".");
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.setFileFilter(new DumpFileFilter("<xml-dump-api>",  "Kiev XML API dump", "xml"));
			if (JFileChooser.APPROVE_OPTION != jfc.showDialog(null, "Save"))
				return;
			try {
				DumpUtils.dumpToXMLFile("api", (ASTNode)uiv.the_root, jfc.getSelectedFile());
			} catch( IOException e ) {
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
				JFileChooser jfc = new JFileChooser(".");
				jfc.setDialogType(JFileChooser.SAVE_DIALOG);
				f = new File(fu.pname());
				if (f.getParentFile() != null)
					jfc.setCurrentDirectory(f.getParentFile());
				jfc.setSelectedFile(f);
				jfc.setAcceptAllFileFilterUsed(false);
				for (DumpFileFilter dff: dumpFileFilters)
					jfc.addChoosableFileFilter(dff);
				for (DumpFileFilter dff: dumpFileFilters) 
					if (fu.getCurrentSyntax() == dff.syntax_qname)
						jfc.setFileFilter(dff);
				if (JFileChooser.APPROVE_OPTION != jfc.showDialog(null,"Save"))
					return;
				DumpFileFilter dff = (DumpFileFilter)jfc.getFileFilter();
				stx_name = dff.syntax_qname;
				f = jfc.getSelectedFile();
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
			JFileChooser jfc = new JFileChooser(".");
			jfc.setFileFilter(new FileFilter() {
				public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"); }
				public String getDescription() { return "XML file for node tree import"; }
			});
			if (JFileChooser.APPROVE_OPTION != jfc.showOpenDialog(null))
				return;
			FileUnit fu = Env.getProject().getLoadedFile(jfc.getSelectedFile());
			if (fu == null) {
				CompilerParseInfo cpi = new CompilerParseInfo(jfc.getSelectedFile(), false);
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
}
