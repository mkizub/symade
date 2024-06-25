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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import kiev.vlang.Env;
import kiev.vlang.ProjectSyntaxFactory;
import kiev.vlang.ProjectSyntaxFactoryBinDump;
import kiev.vlang.ProjectSyntaxFactoryXmlDump;
import kiev.vlang.ProjectSyntaxInfo;

import kiev.CompilerThreadGroup;

import kiev.Compiler;
import kiev.EditorThreadGroup;
import kiev.CompilerParseInfo;
import kiev.Kiev;
import kiev.dump.DumpFactory;
import kiev.fmt.Drawable;
import kiev.fmt.common.TextParser;
import kiev.fmt.common.TextPrinter;
import kiev.fmt.proj.KievETokenNode;
import kiev.fmt.proj.KievExprNode;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIManager;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.INode;
import kiev.vtree.Transaction;

/**
 * Group of actions in the File menu section. 
 */
public final class FileActions implements UIAction {
	
	private static ProjectSyntaxInfo XML_DUMP;
	private static ProjectSyntaxInfo BIN_DUMP;
	static {
		XML_DUMP = new ProjectSyntaxInfo();
		XML_DUMP.setFile_ext("xml");
		XML_DUMP.setDescription("Kiev XML full dump");
		XML_DUMP.setQname("<xml-dump>");
		XML_DUMP.setSyntax(new ProjectSyntaxFactoryXmlDump());
		XML_DUMP.setPrinter(new ProjectSyntaxFactoryXmlDump());
		XML_DUMP.setParser(new ProjectSyntaxFactoryXmlDump());

		BIN_DUMP = new ProjectSyntaxInfo();
		BIN_DUMP.setFile_ext("btd");
		BIN_DUMP.setDescription("Kiev BIN full dump");
		BIN_DUMP.setQname("<bin-dump>");
		BIN_DUMP.setPrinter(new ProjectSyntaxFactoryBinDump());
		BIN_DUMP.setParser(new ProjectSyntaxFactoryBinDump());
}
	
	/**
	 * The Window.
	 */
	private final IWindow wnd;
	
	/**
	 * The View.
	 */
	private final IUIView uiv;
	
	/**
	 * The Drawable.
	 */
	private final Drawable dr;
	
	/**
	 * The action.
	 */
	private final String action;
	
	/**
	 * The File Unit.
	 */
	private FileUnit fu;

	/**
	 * The constructor.
	 * @param wnd the window
	 * @param action the action name
	 */
	public FileActions(IWindow wnd, String action) {
		this.wnd = wnd;
		this.action = action;
		this.uiv = null;
		this.dr = null;
	}
	
	/**
	 * The constructor.
	 * @param uiv the info view
	 * @param action the action name
	 */
	public FileActions(IUIView uiv, String action) {
		this.wnd = uiv.getWindow();
		this.uiv = uiv;
		this.action = action;
		this.dr = null;
	}
	
	/**
	 * The constructor.
	 * @param uiv the info view
	 * @param dr the drawable object
	 * @param action the action name
	 */
	public FileActions(IUIView uiv, Drawable dr, String action) {
		this.wnd = uiv.getWindow();
		this.uiv = uiv;
		this.action = action;
		this.dr = dr;
	}
	
	/**
	 * Performs dump the AST node to a text file.
	 * @param node the AST node
	 * @param f the file
	 * @param stx the syntax
	 */
	private void dumpTextFile(final INode node, final File f, final ProjectSyntaxFactory psi, final boolean current) {
		TextPrinter printer = (TextPrinter)psi.makeTextProcessor();
		if (current)
			printer.setProperty("current", "true");
		printer.print(new INode[]{node}, f,  wnd.getCurrentEnv());
	}
	
	/**
	 * Performs dump the AST node to a text file.
	 * @param node the AST node
	 * @param f the file
	 * @param stx the syntax
	 */
	private INode[] parseTextFile(final File f, final ProjectSyntaxFactory psi) {
		final ArrayList<INode> res = new ArrayList<INode>();
		TextParser printer = (TextParser)psi.makeTextProcessor();
		INode[] nodes = printer.parse(f,  wnd.getCurrentEnv());
		for (INode node : nodes)
			res.add(node);
		return res.toArray(new INode[res.size()]);
	}
	
	/**
	 * Make new FileUnit.
	 * @param f the file
	 */
	private void makeFileUnit(final File f) {
		try {
			String rel_path = DumpFactory.getRelativePath(f);
			fu = FileUnit.makeFile(rel_path, wnd.getCurrentProject(), false);
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		if (action == "new"){
			System.out.println("Running \"new\" action");
			IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.OPEN_TYPE);
			ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
			filters.add(new FileFilter(XML_DUMP, false));
			for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos()) {
				if (psi.getParser() != null)
					filters.add(new FileFilter(psi, false));
			}
			dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));
			dialog.setFilterPath(wnd.getCurrentProject().getRoot_dir().getName());
			if (! (IFileDialog.OK == dialog.open())) return;
			final File f = dialog.getSelectedFile();
			if (f == null) return;						
			if (! dialog.checkFileExists(f)) return;
			if (! dialog.checkFilterExtensions(f)) return;
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			makeFileUnit(f);
			wnd.openEditor(fu);
		} 
		if (action == "add"){
			System.out.println("Running \"add\" action");
			IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.OPEN_TYPE);
			ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
			filters.add(new FileFilter(XML_DUMP, false)); 
			for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos())
				filters.add(new FileFilter(psi, false));
			dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));;
			dialog.setFilterPath(wnd.getCurrentProject().getRoot_dir().getName());
			if (! (IFileDialog.OK == dialog.open())) return;
			final File f = dialog.getSelectedFile();
			if (f == null) return;						
			if (! dialog.checkFilterExtensions(f)) return;			
			FileFilter dff = dialog.getSelectedFilter();
			if (dff != null && !dff.accept(f)) return;
			CompilerParseInfo cpi = new CompilerParseInfo(f, dff.syntax_info, true);
			EditorThreadGroup thrg = wnd.getEditorThreadGroup();
			Transaction tr = Transaction.open("Actions.java:load-as", thrg);
			try {
				Compiler.runFrontEnd(new CompilerParseInfo[]{cpi});
				System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
			} catch( Exception e ) {
				System.out.println("Read error while Xml-to-Kiev importing: "+e);
			} finally { tr.close(thrg); }
			wnd.fireErrorsModified();
			wnd.getCurrentEnv().dumpProjectFile();
		} 
		else if (action == "load-as") {
			System.out.println("Running \"load-as\" action");
			IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.OPEN_TYPE);
			ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
			filters.add(new FileFilter(BIN_DUMP, false)); 
			filters.add(new FileFilter(XML_DUMP, false)); 
			for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos()) {
				if (psi.getParser() != null)
					filters.add(new FileFilter(psi, false));
			}
			dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));;
			dialog.setFilterPath(wnd.getCurrentProject().getRoot_dir().getName());
			if (! (IFileDialog.OK == dialog.open())) return;
			File f = dialog.getSelectedFile();
			if (f == null) return;
			String name = f.getName();
			if (name == null) return;
			FileFilter dff = dialog.getSelectedFilter();
			if (dff != null && !dff.accept(f)) return;
			FileUnit fu = wnd.getCurrentProject().getLoadedFile(f);
			if (fu == null) {
				CompilerParseInfo cpi = new CompilerParseInfo(f, dff.syntax_info, false);
				EditorThreadGroup thrg = wnd.getEditorThreadGroup();
				Transaction tr = Transaction.open("Actions.java:load-as", thrg);
				try {
					Compiler.runFrontEnd(new CompilerParseInfo[]{cpi});
					System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
					fu = cpi.fu;
				} catch( Exception e ) {
					System.out.println("Read error while Xml-to-Kiev importing: "+e);
				} finally { tr.close(thrg); }
			}
			if (fu != null)
				wnd.openEditor(fu);
			wnd.fireErrorsModified();
		}
		else if (action == "save-as") {
			System.out.println("Running \"save-as\" action");
			FileUnit fu;
			if (uiv.getRoot() instanceof FileUnit) fu = (FileUnit)uiv.getRoot();
			else fu = (FileUnit)Env.ctxFileUnit(this.uiv.getRoot());
			if (fu == null) return;
			IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.SAVE_TYPE);	    
			File f = new File(fu.pname());
			if (f.getParentFile() != null)
				dialog.setFilterPath(f.getParentFile().getPath());
			ArrayList<FileFilter> filters = new ArrayList<FileFilter>(); 
			filters.add(new FileFilter(BIN_DUMP, false)); 
			filters.add(new FileFilter(XML_DUMP, false)); 
			for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos()) {
				if (psi.getPrinter() != null) {
					filters.add(new FileFilter(psi, false)); 
					filters.add(new FileFilter(psi, true));
				}
			}
			dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));
			dialog.setFileName(f.getName());
			if (! (IFileDialog.OK == dialog.open())) return;
			f = dialog.getSelectedFile();
			if (f == null) return;
			String name = f.getName();
			if (name == null) return;
			if (! dialog.checkFileExists(f)) return;
			if (! dialog.checkFilterExtensions(f)) return;
			FileFilter dff = dialog.getSelectedFilter();
			if (dff != null)
				dumpTextFile(fu, f, dff.syntax_info.getPrinter(), dff.current);
		}
		else if (action == "save") {
			System.out.println("Running \"save\" action");
			FileUnit fu;
			if (uiv.getRoot() instanceof FileUnit)	fu = (FileUnit)uiv.getRoot();
			else fu = (FileUnit)Env.ctxFileUnit(uiv.getRoot());
			File f = new File(fu.pname());
			ProjectSyntaxFactory stx_factory = fu.getCurrent_syntax();
			if (stx_factory == null) {
				IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.SAVE_TYPE);
				dialog.setFileName(f.getName());
				f = new File(fu.pname());
				if (f.getParentFile() != null)
					dialog.setFilterPath(f.getParentFile().getPath());
				ArrayList<FileFilter> filters = new ArrayList<FileFilter>(); 
				filters.add(new FileFilter(XML_DUMP, false)); 
				filters.add(new FileFilter(BIN_DUMP, false)); 
				for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos()) {
					if (psi.getPrinter() != null) {
						filters.add(new FileFilter(psi, false)); 
						filters.add(new FileFilter(psi, true));
					}
				}
				dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));
				if (! (IFileDialog.OK == dialog.open())) return;
				f = dialog.getSelectedFile();
				if (f == null) return;
				String name = f.getName();
				if (name == null) return;
				FileFilter dff = dialog.getSelectedFilter();
				if (dff != null)
					stx_factory = dff.syntax_info.getPrinter();
			}
			if (stx_factory != null) {
				dumpTextFile(fu, f, stx_factory, false);
				if (fu.getCurrent_syntax() != stx_factory)
					fu.setCurrent_syntax(stx_factory);
			}
		}
		else if (action == "import-as") {
			System.out.println("Running \"import-as\" action");
			IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.OPEN_TYPE);
			ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
			filters.add(new FileFilter(XML_DUMP, false)); 
			filters.add(new FileFilter(BIN_DUMP, false)); 
			for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos()) {
				if (psi.getParser() != null)
					filters.add(new FileFilter(psi, false));
			}
			dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));;
			dialog.setFilterPath(wnd.getCurrentProject().getRoot_dir().getName());
			if (! (IFileDialog.OK == dialog.open())) return;
			File f = dialog.getSelectedFile();
			if (f == null) return;
			String name = f.getName();
			if (name == null) return;
			FileFilter dff = dialog.getSelectedFilter();
			if (dff != null && !dff.accept(f)) return;
			fu = wnd.getCurrentProject().getLoadedFile(f);
			if (fu != null) {
				wnd.openEditor(fu);
				return;
			}
			EditorThreadGroup thrg = wnd.getEditorThreadGroup();
			Transaction tr = Transaction.open("Actions.java:import-as", thrg);
			try {
				ProjectSyntaxFactory psi = dff.syntax_info.getParser();
				INode[] nodes = parseTextFile(f, psi);
				ArrayList<FileUnit> files = new ArrayList<FileUnit>();
				for (INode res : nodes) {
					if (res instanceof FileUnit) {
						files.add((FileUnit)res);
					} else {
						makeFileUnit(f);
						Env.getSpacePtr(fu, "members").add(res);
						if (!files.contains(fu))
							files.add(fu);
					}
					Compiler.runFrontEnd(thrg,files.toArray(new FileUnit[files.size()]));
				}
				System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
			} catch( Exception e ) {
				System.out.println("Read error while Xml-to-Kiev importing: "+e);
			} finally { tr.close(thrg); }
			wnd.fireErrorsModified();
		}
		else if (action == "export-as") {
			System.out.println("Running \"export-as\" action");
			FileUnit fu;
			if (uiv.getRoot() instanceof FileUnit)	fu = (FileUnit)uiv.getRoot();
			else fu = (FileUnit)Env.ctxFileUnit(uiv.getRoot());
			IFileDialog dialog = UIManager.newFileDialog(wnd, IFileDialog.OPEN_TYPE);
			ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
			for (ProjectSyntaxInfo psi : wnd.getCurrentProject().getSyntax_infos()) {
				if (psi.getPrinter() != null)
					filters.add(new FileFilter(psi, false));
			}
			dialog.setFilters(filters.toArray(new FileFilter[filters.size()]));;
			dialog.setFilterPath(wnd.getCurrentProject().getRoot_dir().getName());
			if (! (IFileDialog.OK == dialog.open())) return;
			File f = dialog.getSelectedFile();
			if (f == null) return;
			String name = f.getName();
			if (name == null) return;
			FileFilter dff = dialog.getSelectedFilter();
			if (dff != null && !dff.accept(f)) return;
			dumpTextFile(fu, f, dff.syntax_info.getPrinter(), dff.current);
		}
		else if (action == "merge-all") {
			//wnd.getCurrentEnv().root.mergeTree();
			System.out.println("Tree merged to the editor version.");
		}
		else if (action == "run-backend") {
			System.out.println("Running backend compiler...");
			CompilerThreadGroup thrg = new CompilerThreadGroup(wnd.getEditorThreadGroup());
			thrg.errCount = 0;
			thrg.warnCount = 0;
			Compiler.runBackEnd(thrg, null);
			wnd.fireErrorsModified();
		}
		else if (action == "run-frontend-all") {
			runFrontEndCompiler((Editor)uiv, new INode[]{wnd.getCurrentEnv().root});
		}
		else if (action == "run-frontend") {
			runFrontEndCompiler((Editor)uiv, new INode[]{uiv.getRoot()});
		}
		else if (action == "run-token-list-compilation") {
			Editor edt = (Editor)uiv;
			INode node = edt.getSelectedNode();
			while (node instanceof KievETokenNode)
				node = node.parent();
			if (node instanceof KievExprNode) {
				KievExprNode expr = (KievExprNode)node;
				expr.parseExpr(true);
			}
		}
		else if (action == "use-bindings") {
			kiev.fmt.evt.BindingSet bs = (kiev.fmt.evt.BindingSet)dr.drnode;
			UIManager.attachEventBindings(bs);
		}
		else if (action == "reset-bindings") {
			UIManager.resetEventBindings();
		}
	}

	/**
	 * Runs compiler's front-end. 
	 * @param editor the editor
	 * @param root the root node
	 */
	private void runFrontEndCompiler(Editor editor, INode[] roots) {
		System.out.println("Running frontend compiler...");
		editor.getWindow().startTransaction(editor, "Action:runFrontEndCompiler");
		try {
			EditorThreadGroup thrg = wnd.getEditorThreadGroup();
			Compiler.runFrontEnd(thrg,roots);
			System.out.println("Frontend compiler completed with "+thrg.errCount+" error(s)");
		} finally {
			editor.getWindow().stopTransaction(false);
		}
		wnd.fireErrorsModified();
		editor.formatAndPaint(true);
	}
	

	/**
	 * "Save As..." action factory.
	 */
	public final static class SaveFileAs implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Save the file as a new file"; }
		
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
			return new FileActions(context.ui, "save-as");
		}
	}

	/**
	 * "Save" action factory.
	 */
	public final static class SaveFile implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Save the file"; }
		
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
			return new FileActions(context.ui, "save");
		}
	}

	/**
	 * "New..." action factory.
	 */
	public final static class NewFile implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "New file into the current view"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "new");
		}
	}

	/**
	 * "Add..." action factory.
	 */
	public final static class AddFile implements UIActionFactory {
		
		/**
		 * The description.
		 */
		private String descr = "Add file to the current project";
		
		/**
		 * Is for pop-up menu.
		 */
		private boolean forPopupMenu;
		
		/**
		 * Explicit constructor.
		 */
		public AddFile() {}
		
		/**
		 * The constructor. 
		 * @param descr the description
		 * @param forPopupMenu is for pop-up menu 
		 */
		public AddFile(String descr, boolean forPopupMenu) {
			this.descr = descr;
			this.forPopupMenu = forPopupMenu;
		}

		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return descr; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return forPopupMenu; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "add");
		}
	}
	
	/**
	 * "Load..." action factory. 
	 */
	public final static class LoadFileAs implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Load a file into current view as a file with specified syntax"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "load-as");
		}
	}

	/**
	 * "Import..." action factory. 
	 */
	public final static class ImportFileAs implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Import file with specified syntax"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "import-as");
		}
	}

	/**
	 * "Import..." action factory. 
	 */
	public final static class ExportFileAs implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Export file with specified syntax"; }
		
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
			return new FileActions(context.ui, "export-as");
		}
	}

	/**
	 * "Merge tree" action factory. 
	 */
	public final static class MergeTreeAll implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Merge editor's changes into working tree for the whole project"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "merge-all");
		}
	}

	/**
	 * "Run back-end all" action factory.
	 */
	public final static class RunBackendAll implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Run back-end compilation for the whole project"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.ui != null)
				return new FileActions(context.ui, "run-backend");
			return null;
		}
	}

	/**
	 * "Run front-end all" action factory.
	 */
	public final static class RunFrontendAll implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Run front-end compilation for the whole project"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new FileActions(context.editor, "run-frontend-all");
			return null;
		}
	}

	/**
	 * "Run frontend" action factory. 
	 */
	public final static class RunFrontend implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Run front-end compilation for the current compilation unit"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new FileActions(context.editor, "run-frontend");
			return null;
		}
	}
	
	/**
	 * "Test compile KievExprNode" action factory. 
	 */
	public final static class RunTokenListCompilation implements UIActionFactory {
		
		public String getDescr() { return "Run compilation of KievExprNode"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && (context.node instanceof KievExprNode || context.node instanceof KievETokenNode))
				return new FileActions(context.editor, "run-token-list-compilation");
			return null;
		}
	}
	
	/**
	 * "Use event bindings" action factory.
	 */
	public final static class UseEventBindings implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Compile and use event bindings"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && context.dt != null && context.dt.drnode instanceof kiev.fmt.evt.BindingSet)
				return new FileActions(context.editor, context.dt, "use-bindings");
			return null;
		}
	}

	/**
	 * "Reset event bindings" action factory.
	 */
	public final static class ResetEventBindings implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Reset event bindings to default"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "reset-bindings");
		}
	}

}
