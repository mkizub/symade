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

import kiev.Compiler;
import kiev.CompilerParseInfo;
import kiev.CompilerThread;
import kiev.EditorThread;
import kiev.vtree.*;
import kiev.vlang.*;
import kiev.parser.*;
import kiev.fmt.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.InputEvent;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import javax.swing.text.TextAction;
import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import java.awt.datatransfer.*;

final class InputEventInfo {
	private final int mask;
	private final int code;
	public InputEventInfo(int mask, int code) {
		this.mask = mask;
		this.code = code;
	}
	public int hashCode() { return mask ^ code; }
	public boolean equals(Object ie) {
		if (ie instanceof InputEventInfo)
			return this.mask == ((InputEventInfo)ie).mask && ((InputEventInfo)ie).code == code;
		return false;
	}
	public String toString() {
		String mod = InputEvent.getModifiersExText(mask);
		if (mod == null || mod.length() == 0)
			return KeyEvent.getKeyText(code);
		return mod+"+"+KeyEvent.getKeyText(code);
	}
}

class UIActionViewContext {
	public final Window		wnd;
	public final UIView		ui;
	public final InfoView	uiv;
	public final Editor		editor;
	public final DrawTerm	dt;
	public final ANode		node;
	public Drawable			dr;
	public UIActionViewContext(Window wnd, UIView ui) {
		this.wnd = wnd;
		this.ui = ui;
		if (ui instanceof InfoView) {
			this.uiv = (InfoView)ui;
		} else {
			this.uiv = null;
		}
		if (ui instanceof Editor) {
			this.editor = (Editor)ui;
			this.dt = editor.cur_elem.dr;
			this.node = editor.cur_elem.node;
			this.dr = dt;
		} else {
			this.editor = null;
			this.dt = null;
			this.node = null;
			this.dr = null;
		}
	}
	public UIActionViewContext(Window wnd, Editor editor, Drawable dr) {
		this.wnd = wnd;
		this.ui = editor;
		this.uiv = editor;
		this.editor = editor;
		this.dt = editor.cur_elem.dr;
		this.node = editor.cur_elem.node;
		this.dr = dr;
	}
}

interface UIActionFactory {
	public String getDescr();
	public boolean isForPopupMenu();
	public Runnable getAction(UIActionViewContext context);
}

final class NavigateView implements Runnable {
	
	final InfoView uiv;
	final int incr;
	
	NavigateView(InfoView uiv, int incr) {
		this.uiv = uiv;
		this.incr = incr;
	}
	
	public void run() {
		this.uiv.view_canvas.incrFirstLine(this.incr);
	}

	final static class LineUp implements UIActionFactory {
		public String getDescr() { return "Scroll the view one line up"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new NavigateView(context.uiv, -1);
		}
	}
	final static class LineDn implements UIActionFactory {
		public String getDescr() { return "Scroll the view one line down"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new NavigateView(context.uiv, +1);
		}
	}
	final static class PageUp implements UIActionFactory {
		public String getDescr() { return "Scroll the view one page up"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			InfoView uiv = context.uiv;
			return new NavigateView(uiv, -uiv.view_canvas.last_visible.getLineNo() + uiv.view_canvas.first_visible.getLineNo() + 1);
		}
	}
	final static class PageDn implements UIActionFactory {
		public String getDescr() { return "Scroll the view one page down"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			InfoView uiv = context.uiv;
			return new NavigateView(uiv, +uiv.view_canvas.last_visible.getLineNo() - uiv.view_canvas.first_visible.getLineNo() - 1);
		}
	}
}

class NavigateEditor implements Runnable {

	final Editor uiv;
	final int incr;
	
	NavigateEditor(Editor uiv, int incr) {
		this.uiv = uiv;
		this.incr = incr;
	}
	
	public void run() {
		switch (incr) {
		case -1: navigatePrev(uiv,true); break;
		case +1: navigateNext(uiv,true); break;
		case -2: navigateUp(uiv,true); break;
		case +2: navigateDn(uiv,true); break;
		case -3: navigateLineHome(uiv,true); break;
		case +3: navigateLineEnd(uiv,true); break;
		case -4: navigatePageUp(uiv); break;
		case +4: navigatePageDn(uiv); break;
		}
	}
	
	final static class GoPrev implements UIActionFactory {
		public String getDescr() { return "Go to the previous element"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-1);
			return null;
		}
	}
	final static class GoNext implements UIActionFactory {
		public String getDescr() { return "Go to the next element"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+1);
			return null;
		}
	}
	final static class GoLineUp implements UIActionFactory {
		public String getDescr() { return "Go to an element above"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-2);
			return null;
		}
	}
	final static class GoLineDn implements UIActionFactory {
		public String getDescr() { return "Go to an element below"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+2);
			return null;
		}
	}
	final static class GoLineHome implements UIActionFactory {
		public String getDescr() { return "Go to the first element on the line"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-3);
			return null;
		}
	}
	final static class GoLineEnd implements UIActionFactory {
		public String getDescr() { return "Go to the last element on the line"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+3);
			return null;
		}
	}
	final static class GoPageUp implements UIActionFactory {
		public String getDescr() { return "Go to an element one screen above"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-4);
			return null;
		}
	}
	final static class GoPageDn implements UIActionFactory {
		public String getDescr() { return "Go to an element one screen below"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+4);
			return null;
		}
	}

	private void navigatePrev(Editor uiv, boolean repaint) {
		if (uiv.insert_mode && uiv.view_canvas.cursor_offset > 0) {
			uiv.view_canvas.cursor_offset --;
		} else {
			DrawTerm prev = uiv.cur_elem.dr.getFirstLeaf().getPrevLeaf();
			if (prev != null) {
				uiv.cur_elem.set(prev);
				uiv.cur_x = prev.getX();
				if (uiv.insert_mode) {
					String text = prev.getText();
					if (text != null)
						uiv.view_canvas.cursor_offset = text.length();
					else
						uiv.view_canvas.cursor_offset = 0;
				}
			}
		}
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateNext(Editor uiv, boolean repaint) {
		DrawTerm curr = uiv.cur_elem.dr;
		if (curr != null && curr.getText() != null && uiv.insert_mode && uiv.view_canvas.cursor_offset < curr.getText().length()) {
			uiv.view_canvas.cursor_offset ++;
		} else {
			DrawTerm next = uiv.cur_elem.dr.getFirstLeaf().getNextLeaf();
			if (next != null) {
				uiv.cur_elem.set(next);
				uiv.cur_x = next.getX();
				uiv.view_canvas.cursor_offset = 0;
			}
		}
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateUp(Editor uiv, boolean repaint) {
		DrawTerm n = null;
		DrawTerm prev = uiv.cur_elem.dr.getFirstLeaf();
		if (prev != null)
			prev = prev.getPrevLeaf();
		while (prev != null) {
			if (prev.get$do_newline()) {
				n = prev;
				break;
			}
			prev = prev.getPrevLeaf();
		}
		while (n != null) {
			int w = n.getWidth();
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.get$do_newline())
				break;
			w = prev.getWidth();
			if (prev.getX()+w < uiv.cur_x) 
				break;
			n = prev;
		}
		if (n != null)
			uiv.cur_elem.set(n);
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateDn(Editor uiv, boolean repaint) {
		DrawTerm n = null;
		DrawTerm next = uiv.cur_elem.dr.getFirstLeaf();
		while (next != null) {
			if (next.get$do_newline()) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			int w = n.getWidth();
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.getX() > uiv.cur_x)
				break;
			if (next.get$do_newline())
				break;
			n = next;
		}
		if (n != null)
			uiv.cur_elem.set(n);
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateLineHome(Editor uiv, boolean repaint) {
		int lineno = uiv.cur_elem.dr.getFirstLeaf().getLineNo();
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getPrevLeaf();
			if (dr == null || dr.getLineNo() != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.getX();
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigateLineEnd(Editor uiv, boolean repaint) {
		int lineno = uiv.cur_elem.dr.getFirstLeaf().getLineNo();
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getNextLeaf();
			if (dr == null || dr.getLineNo() != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.getX();
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigatePageUp(Editor uiv) {
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.setFirstLine(0);
			return;
		}
		int offs = uiv.view_canvas.last_visible.getLineNo() - uiv.view_canvas.first_visible.getLineNo() -1;
		uiv.view_canvas.incrFirstLine(-offs);
		for (int i=offs; i >= 0; i--)
			navigateUp(uiv,i==0);
		return;
	}
	private void navigatePageDn(Editor uiv) {
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.setFirstLine(0);
			return;
		}
		int offs = uiv.view_canvas.last_visible.getLineNo() - uiv.view_canvas.first_visible.getLineNo() -1;
		uiv.view_canvas.incrFirstLine(+offs);
		for (int i=offs; i >= 0; i--)
			navigateDn(uiv,i==0);
		return;
	}

}

class NavigateNode implements Runnable {

	final Editor uiv;
	final String cmd;
	
	NavigateNode(Editor uiv, String cmd) {
		this.uiv = uiv;
		this.cmd = cmd;
	}
	
	public void run() {
		if (cmd == "select-up") {
			uiv.cur_elem.nodeUp();
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
			if (context.editor != null && context.editor.cur_elem.node != null && context.editor.cur_elem.node.parent() != null)
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


final class FileActions implements Runnable {
	
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
		new DumpFileFilter("treedl\u001fsyntax-for-treedl", "TreeDL source files", "java"),
		new DumpFileFilter("stx-fmt\u001fsyntax-dump-full", "Kiev XML full dump", "xml"),
		//new DumpFileFilter("stx-fmt\u001fsyntax-dump-api",  "Kiev XML API dump", "xml"),
	};
	
	final Window wnd;
	final InfoView uiv;
	final String action;
	
	FileActions(Window wnd, String action) {
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
				if ("stx-fmt\u001fsyntax-dump-full".equals(dff.syntax_qname)) {
					DumpUtils.dumpToXMLFile("full", fu, jfc.getSelectedFile());
					fu.setCurrentSyntax("stx-fmt\u001fsyntax-dump-full");
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
			jfc.setFileFilter(new DumpFileFilter("stx-fmt\u001fsyntax-dump-api",  "Kiev XML API dump", "xml"));
			if (JFileChooser.APPROVE_OPTION != jfc.showDialog(null, "Save"))
				return;
			try {
				if (true) {
					DumpUtils.dumpToXMLFile("api", (ASTNode)uiv.the_root, jfc.getSelectedFile());
				} else {
					SyntaxManager.dumpTextFile((ASTNode)uiv.the_root, jfc.getSelectedFile(), new XmlDumpSyntax("api").getCompiled().init());
				}
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
			String stx_name = null;
			Draw_ATextSyntax stx = null;
			if (fu.getCurrentSyntax() != null) {
				stx = SyntaxManager.loadLanguageSyntax(fu.getCurrentSyntax());
				stx_name = fu.getCurrentSyntax();
			}
			File f = new File(fu.pname());
			if (stx == null || stx != uiv.syntax) {
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
				stx = SyntaxManager.loadLanguageSyntax(dff.syntax_qname);
				stx_name = dff.syntax_qname;
				f = jfc.getSelectedFile();
			}
			try {
				if ("stx-fmt\u001fsyntax-dump-full".equals(stx_name)) {
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
			CompilerParseInfo cpi = new CompilerParseInfo(jfc.getSelectedFile(), false);
			Transaction tr = Transaction.open("Actions.java:load-as");
			try {
				EditorThread thr = EditorThread.getInst();
				Compiler.runFrontEnd(thr,new CompilerParseInfo[]{cpi},null,true);
				System.out.println("Frontend compiler completed with "+thr.errCount+" error(s)");
			} catch( Exception e ) {
				System.out.println("Read error while Xml-to-Kiev importing: "+e);
			} finally { tr.close(); }
			if (cpi.fu != null)
				wnd.openEditor(cpi.fu);
		}
		else if (action == "merge-all") {
			Env.getRoot().mergeTree();
			System.out.println("Tree merged to the editor version.");
		}
		else if (action == "run-backend") {
			System.out.println("Running backend compiler...");
			CompilerThread thr = CompilerThread.getInst();
			thr.errCount = 0;
			thr.warnCount = 0;
			Compiler.runBackEnd(thr, Env.getRoot(), null, false);
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
			EditorThread thr = EditorThread.getInst();
			Compiler.runFrontEnd(thr,null,root,true);
			System.out.println("Frontend compiler completed with "+thr.errCount+" error(s)");
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

final class EditActions implements Runnable {
	
	final Editor editor;
	final String action;
	
	EditActions(Editor editor, String action) {
		this.editor = editor;
		this.action = action;
	}
	
	public void run() {
		if (action == "close") {
			if (editor.item_editor != null) {
				editor.stopItemEditor(true);
				editor.item_editor = null;
			}
			editor.parent_window.closeEditor(editor);
		}
		else if (action == "undo") {
			Transaction tr = editor.changes.pop();
			tr.rollback(false);
			editor.formatAndPaint(true);
		}
		else if (action == "cut" || action == "del") {
			ANode node = editor.cur_elem.node;
			editor.changes.push(Transaction.open("Actions.java:cut"));
			node.detach();
			editor.changes.peek().close();
			if (action == "cut") {
				TransferableANode tr = new TransferableANode(node);
				editor.clipboard.setContents(tr, tr);
			}
			editor.formatAndPaint(true);
		}
		else if (action == "copy") {
			if (editor.cur_elem.dr instanceof DrawNodeTerm) {
				Object obj = ((DrawNodeTerm)editor.cur_elem.dr).getAttrObject();
				Transferable tr = null;
				if (obj instanceof ANode)
					tr = new TransferableANode((ANode)obj);
				else
					tr = new StringSelection(String.valueOf(obj));
				editor.clipboard.setContents(tr, (ClipboardOwner)tr);
			} else {
				Transferable tr = new TransferableANode(editor.cur_elem.node);
				editor.clipboard.setContents(tr, (ClipboardOwner)tr);
			}
		}
	}

	final static class CloseWindow implements UIActionFactory {
		public String getDescr() { return "Close the editor window"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new EditActions(context.editor, "close");
			return null;
		}
	}

	final static class Undo implements UIActionFactory {
		public String getDescr() { return "Undo last change"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.changes.size() > 0)
				return new EditActions(context.editor, "undo");
			return null;
		}
	}
	
	final static class Cut implements UIActionFactory {
		public String getDescr() { return "Cut current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.dr != null)
				return new EditActions(context.editor, "cut");
			return null;
		}
	}
	
	final static class Del implements UIActionFactory {
		public String getDescr() { return "Delete current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.dr != null)
				return new EditActions(context.editor, "del");
			return null;
		}
	}
	
	final static class Copy implements UIActionFactory {
		public String getDescr() { return "Copy current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.dr != null)
				return new EditActions(context.editor, "copy");
			return null;
		}
	}
	
}

final class RenderActions implements Runnable {
	
	final UIView ui;
	final String action;
	
	RenderActions(UIView ui, String action) {
		this.ui = ui;
		this.action = action;
	}
	
	public void run() {
		UIView ui = this.ui;
		if (action == "select-syntax") {
			// build a menu of types to instantiate
			JPopupMenu m = new JPopupMenu();
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Kiev Syntax", "stx-fmt\u001fsyntax-for-java", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Kiev Syntax (current)", "stx-fmt\u001fsyntax-for-java", true)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"TreeDL Syntax", "treedl\u001fsyntax-for-treedl", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"XML dump Syntax (full)", XmlDumpSyntax.class, "full")));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"XML dump Syntax (api)", XmlDumpSyntax.class, "api")));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"XML dump Syntax (full, namespace)", NsXmlDumpSyntax.class, "full")));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"XML dump Syntax (api, namespace)", NsXmlDumpSyntax.class, "api")));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Project Tree Syntax", "stx-fmt\u001fsyntax-for-project-tree", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Project Tree Syntax  (current)", "stx-fmt\u001fsyntax-for-project-tree", true)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for API", "stx-fmt\u001fsyntax-for-api", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for VDOM", "stx-fmt\u001fsyntax-for-vdom", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for VDOM (current)", "stx-fmt\u001fsyntax-for-vdom", true)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for Syntax", "stx-fmt\u001fsyntax-for-syntax", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for Syntax (current)", "stx-fmt\u001fsyntax-for-syntax", true)));
			if (ui instanceof InfoView)
				m.show(((InfoView)ui).view_canvas, 0, 0);
			else if (ui instanceof TreeView)
				m.show(((TreeView)ui).the_tree, 0, 0);
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
				((Editor)ui).cur_elem.set(ui.view_root.getFirstLeaf());
			//ui.view_canvas.root = ui.view_root;
			ui.formatAndPaint(false);
		}
	}

	static class SetSyntaxAction extends TextAction {
		private UIView uiv;
		private Class<? extends ATextSyntax> clazz;
		private String qname;
		private boolean in_project;
		SetSyntaxAction(UIView uiv, String text, Class<? extends ATextSyntax> clazz, String name) {
			super(text);
			this.uiv = uiv;
			this.clazz = clazz;
			this.qname = name;
		}
		SetSyntaxAction(UIView uiv, String text, String qname, boolean in_project) {
			super(text);
			this.uiv = uiv;
			this.qname = qname;
			this.in_project = in_project;
		}
		public void actionPerformed(ActionEvent e) {
			if (clazz != null) {
				ATextSyntax stx = null;
				try {
					stx = (ATextSyntax)clazz.newInstance();
				} catch (Exception ex) {
					ex.printStackTrace();
					return;
				}
				if (stx instanceof XmlDumpSyntax)
					((XmlDumpSyntax)stx).set$dump(qname);
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
				EditorThread thr = EditorThread;
				fu = Env.getRoot().loadFromXmlFile(new File(this.file), null);
				try {
					thr.errCount = 0;
					thr.warnCount = 0;
					Compiler.runFrontEnd(thr,null,fu,true);
				} catch (Throwable t) { t.printStackTrace(); }
				System.out.println("Frontend compiler completed with "+thr.errCount+" error(s)");
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
	final static class SyntaxFileAs implements UIActionFactory {
		public String getDescr() { return "Set the syntax of the curret view"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "select-syntax");
		}
	}

	final static class OpenFoldedAll implements UIActionFactory {
		public String getDescr() { return "Open (unfold) all folded elements"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.ui, "unfold-all");
		}
	}

	final static class CloseFoldedAll implements UIActionFactory {
		public String getDescr() { return "Close (fold) all foldable elements"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.ui, "fold-all");
		}
	}

	final static class ToggleShowAutoGenerated implements UIActionFactory {
		public String getDescr() { return "Toggle show of auto-generated code"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-autogen");
		}
	}

	final static class ToggleShowPlaceholders implements UIActionFactory {
		public String getDescr() { return "Toggle show of editor placeholders"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-placeholder");
		}
	}

	final static class ToggleHintEscaped implements UIActionFactory {
		public String getDescr() { return "Toggle idents and strings escaping"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-escape");
		}
	}

	final static class Redraw implements UIActionFactory {
		public String getDescr() { return "Redraw the window"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "redraw");
		}
	}
}

final class ExprEditActions implements Runnable, KeyListener {
	
	final Editor editor;
	final UIActionViewContext context;
	final String action;
	
	private ASTExpression		expr;
	private JPopupMenu			menu;
	
	ExprEditActions(UIActionViewContext context, String action) {
		this.editor = context.editor;
		this.context = context;
		this.action = action;
	}
	
	public void run() {
		if (action == "split") {
			DrawNonTerm nt = null;
			{
				Drawable d = context.dr;
				while (d != null && !(d instanceof DrawNonTerm))
					d = (Drawable)d.parent();
				if (!(d instanceof DrawNonTerm))
					return;
				nt = (DrawNonTerm)d;
			}
			DrawTerm first = nt.getFirstLeaf();
			DrawTerm last = nt.getLastLeaf().getNextLeaf();
			expr = new ASTExpression();
			SpacePtr enodes = expr.getSpacePtr("nodes");
			for (DrawTerm dt = first; dt != null && dt != last; dt = dt.getNextLeaf()) {
				if (dt.isUnvisible())
					continue;
				if (dt instanceof DrawToken) {
					if (((Draw_SyntaxToken)dt.syntax).kind == SyntaxTokenKind.UNKNOWN)
						enodes.add(new EToken(0, dt.getText(), ETokenKind.UNKNOWN, false));
					else
						enodes.add(new EToken(0, dt.getText(), ETokenKind.OPERATOR, true));
				}
				else if (dt instanceof DrawNodeTerm) {
					if (dt.get$drnode() instanceof ConstExpr)
						enodes.add(new EToken((ConstExpr)dt.get$drnode()));
					else
						enodes.add(new EToken(0,dt.getText(),ETokenKind.UNKNOWN,false));
				}
			}
			editor.insert_mode = true;
			editor.startItemEditor(this);
			context.node.replaceWithNode(expr);
			for (EToken et: (EToken[])expr.getNodes())
				et.guessKind();
			editor.formatAndPaint(true);
		}
	}
	
	class SetKindAction extends TextAction {
		private EToken et;
		private ETokenKind kind;
		SetKindAction(EToken et, ETokenKind kind) {
			super(String.valueOf(kind));
			this.et = et;
			this.kind = kind;
		}
		public void actionPerformed(ActionEvent e) {
			if (menu != null)
				editor.view_canvas.remove(menu);
			menu = null;
			if (kind == ETokenKind.UNKNOWN) {
				et.setKind(ETokenKind.UNKNOWN);
				et.set$explicit(false);
			} else {
				et.setKind(kind);
				et.set$explicit(true);
			}
			et.guessKind();
			editor.formatAndPaint(true);
		}
	}
  
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (code == KeyEvent.VK_F && mask == KeyEvent.CTRL_DOWN_MASK) {
			DrawTerm dt = editor.cur_elem.dr;
			ANode n = editor.cur_elem.node;
			if (!(n instanceof EToken) || n.parent() != expr || dt == null || dt.get$drnode() != n)
				return;
			EToken et = (EToken)n;
			menu = new JPopupMenu();
			for (ETokenKind k: ETokenKind.class.getEnumConstants())
				menu.add(new SetKindAction(et, k));
			int x = dt.getX();
			int h = dt.getHeight();
			int y = dt.getY() + h - editor.view_canvas.translated_y;
			menu.show(editor.view_canvas, x, y);
			return;
		}
		if (mask != 0 && mask != KeyEvent.SHIFT_DOWN_MASK)
			return;
		evt.consume();
		switch (code) {
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_UP:
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		case KeyEvent.VK_HOME:
			if (expr.getNodes().length > 0)
				editor.goToPath(makePathTo(expr.getNodes()[0]));
			else
				editor.goToPath(makePathTo(expr));
			editor.view_canvas.cursor_offset = 0;
			return;
		case KeyEvent.VK_END:
			if (expr.getNodes().length > 0)
				editor.goToPath(makePathTo(expr.getNodes()[expr.getNodes().length-1]));
			else
				editor.goToPath(makePathTo(expr));
			if (editor.cur_elem.dr != null && editor.cur_elem.dr.getText() != null)
				editor.view_canvas.cursor_offset = editor.cur_elem.dr.getText().length();
			else
				editor.view_canvas.cursor_offset = 0;
			return;
		case KeyEvent.VK_LEFT:
			new NavigateEditor(context.editor,-1).run();
			return;
		case KeyEvent.VK_RIGHT:
			new NavigateEditor(context.editor,+1).run();
			return;
		case KeyEvent.VK_ENTER:
			editor.insert_mode = true;
			EditorThread thr = EditorThread.getInst();
			try {
				thr.errCount = 0;
				thr.warnCount = 0;
				Compiler.runFrontEnd(thr,null,(ASTNode)expr.parent(),true);
			} catch (Throwable t) { t.printStackTrace(); }
			editor.insert_mode = false;
			editor.stopItemEditor(false);
			return;
		case KeyEvent.VK_ESCAPE:
			editor.insert_mode = false;
			editor.stopItemEditor(true);
			return;
		}
		if (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_ALT || code == KeyEvent.VK_CONTROL)
			return;
		DrawTerm dt = editor.cur_elem.dr;
		ANode n = editor.cur_elem.node;
		if (!(n instanceof EToken) || n.parent() != expr || dt == null || dt.get$drnode() != n) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		EToken et = (EToken)n;
		String prefix_text = dt.getPrefix();
		String suffix_text = dt.getSuffix();
		String text = dt.getText();
		if (text == null) { text = ""; }
		int prefix_offset = prefix_text.length();
		int suffix_offset = suffix_text.length();
		text = text.substring(prefix_offset, text.length() - suffix_offset);
		int edit_offset = editor.view_canvas.cursor_offset - prefix_offset;
		if (edit_offset < 0 || edit_offset > text.length()) {
			char ch = evt.getKeyChar();
			if (ch != KeyEvent.CHAR_UNDEFINED) {
				if (edit_offset < 0) {
					prependNode(dt,et,ch);
					return;
				}
				else if (edit_offset > text.length()) {
					appendNode(dt,et,ch);
					return;
				}
			}
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		switch (code) {
		case KeyEvent.VK_DELETE:
			if (text.length() == 0) {
				deleteNode(dt,et,false);
				return;
			}
			else if (edit_offset >= text.length()) {
				joinNodes(dt,et,false);
				return;
			}
			else if (edit_offset < text.length()) {
				et.setText(text = text.substring(0, edit_offset)+text.substring(edit_offset+1));
			}
			break;
		case KeyEvent.VK_BACK_SPACE:
			if (text.length() == 0) {
				deleteNode(dt,et,true);
				return;
			}
			else if (edit_offset == 0) {
				joinNodes(dt,et,true);
				return;
			}
			else if (edit_offset > 0) {
				edit_offset--;
				et.setText(text = text.substring(0, edit_offset)+text.substring(edit_offset+1));
			}
			break;
		case KeyEvent.VK_SPACE:
			// split the node, if it's not a string/char expression
			if (et.getKind() != ETokenKind.EXPR_STRING && et.getKind() != ETokenKind.EXPR_CHAR) {
				if (et.get$explicit() && edit_offset != 0 && edit_offset != text.length()) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}
				else if (edit_offset < 0 || edit_offset > text.length()) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}
				// split the node
				splitNode(dt,et,text.substring(0,edit_offset),text.substring(edit_offset));
				return;
			} // fall through
		default:
			if (evt.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
				java.awt.Toolkit.getDefaultToolkit().beep();
				return;
			} else {
				char ch = evt.getKeyChar();
				if (et.getKind() == ETokenKind.EXPR_STRING || et.getKind() == ETokenKind.EXPR_CHAR) {
					text = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					edit_offset++;
					et.setText(text);
					break;
				}
				else if (et.getKind() == ETokenKind.EXPR_NUMBER) {
					String s = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					if (EToken.patternIntConst.matcher(s).matches() || EToken.patternFloatConst.matcher(s).matches()) {
						edit_offset++;
						et.setText(s);
					}
					else if (edit_offset == 0) {
						prependNode(dt,et,ch);
						return;
					}
					else if (edit_offset >= text.length()) {
						appendNode(dt,et,ch);
						return;
					}
					else
						java.awt.Toolkit.getDefaultToolkit().beep();
					break;
				}
				else if (EToken.patternIdent.matcher(text).matches()) {
					String s = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					if (EToken.patternIdent.matcher(s).matches()) {
						edit_offset++;
						et.setText(s);
					}
					else if (edit_offset == 0) {
						prependNode(dt,et,ch);
						return;
					}
					else if (edit_offset >= text.length()) {
						appendNode(dt,et,ch);
						return;
					}
					else
						java.awt.Toolkit.getDefaultToolkit().beep();
					break;
				}
				else if (EToken.patternOper.matcher(text).matches()) {
					String s = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					if (EToken.patternOper.matcher(s).matches()) {
						edit_offset++;
						et.setText(s);
					}
					else if (edit_offset == 0) {
						prependNode(dt,et,ch);
						return;
					}
					else if (edit_offset >= text.length()) {
						appendNode(dt,et,ch);
						return;
					}
					else
						java.awt.Toolkit.getDefaultToolkit().beep();
					break;
				}
				// unknown
				et.setText(text.substring(0, edit_offset)+ch+text.substring(edit_offset));
				edit_offset++;
				break;
			}
		}
		editor.view_canvas.cursor_offset = edit_offset+prefix_offset;
		editor.formatAndPaint(true);
	}
	
	private void deleteNode(DrawTerm dt, EToken et, boolean by_backspace) {
		dt = (by_backspace ? dt.getPrevLeaf() : dt.getNextLeaf());
		editor.cur_elem.set(dt);
		if (by_backspace && dt != null && dt.getText() != null)
			editor.view_canvas.cursor_offset = dt.getText().length();
		else
			editor.view_canvas.cursor_offset = 0;
		et.detach();
		editor.formatAndPaint(true);
	}
	private void joinNodes(DrawTerm dt, EToken et, boolean by_backspace) {
		if (by_backspace) {
			DrawTerm pt = dt.getPrevLeaf();
			if (pt != null && pt.get$drnode() instanceof EToken) {
				EToken pe = (EToken)pt.get$drnode();
				editor.cur_elem.set(pt);
				editor.view_canvas.cursor_offset = pt.getText().length();
				pe.setText(pe.get$ident() + et.get$ident());
				et.detach();
			}
		} else {
			DrawTerm nt = dt.getNextLeaf();
			if (nt != null && nt.get$drnode() instanceof EToken) {
				EToken pe = (EToken)nt.get$drnode();
				et.setText(et.get$ident() + pe.get$ident());
				pe.detach();
			}
		}
		editor.formatAndPaint(true);
	}
	private void splitNode(DrawTerm dt, EToken et, String left, String right) {
		if (left == null) left = "";
		if (right == null) right = "";
		EToken ne = new EToken();
		SpaceAttrSlot sas = (SpaceAttrSlot)et.pslot();
		int idx = sas.indexOf(et.parent(),et);
		if (left.length() == 0) {
			// insert a new node before
			sas.insert(et.parent(),idx,ne);
			ne.setText(left);
			et.setText(right);
		} else {
			// insert new node after
			sas.insert(et.parent(),idx+1,ne);
			et.setText(left);
			ne.setText(right);
		}
		// set new node to be current
		editor.view_canvas.cursor_offset = 0;
		editor.formatAndPaint(true);
		editor.goToPath(makePathTo(ne));
		editor.formatAndPaint(false);
	}
	private void prependNode(DrawTerm dt, EToken et, char ch) {
		EToken ne = new EToken();
		SpaceAttrSlot sas = (SpaceAttrSlot)et.pslot();
		int idx = sas.indexOf(et.parent(),et);
		sas.insert(et.parent(),idx,ne);
		ne.setText(String.valueOf(ch));
		editor.formatAndPaint(true);
		editor.goToPath(makePathTo(ne));
		editor.view_canvas.cursor_offset = 1;
		editor.formatAndPaint(false);
	}
	private void appendNode(DrawTerm dt, EToken et, char ch) {
		EToken ne = new EToken();
		SpaceAttrSlot sas = (SpaceAttrSlot)et.pslot();
		int idx = sas.indexOf(et.parent(),et);
		sas.insert(et.parent(),idx+1,ne);
		ne.setText(String.valueOf(ch));
		editor.formatAndPaint(true);
		editor.goToPath(makePathTo(ne));
		editor.view_canvas.cursor_offset = 1;
		editor.formatAndPaint(false);
	}
	
	private ANode[] makePathTo(ANode n) {
		Vector<ANode> path = new Vector<ANode>();
		path.add(n);
		while (n.parent() != null) {
			n = n.parent();
			path.add(n);
			if (n instanceof FileUnit)
				break;
		}
		return path.toArray(new ANode[path.size()]);
	}

	public final static class Flatten implements UIActionFactory {
		public String getDescr() { return "Flatten expresison tree"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			ANode node = context.node;
			Drawable dr = context.dr;
			if (context.editor == null || node == null || dr == null)
				return null;
			if (!(node instanceof ENode))
				return null;
			return new ExprEditActions(context, "split");
		}
	}
}

