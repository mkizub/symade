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

import kiev.Kiev;
import kiev.Compiler;
import kiev.CompilerThread;
import kiev.EditorThread;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.InputEvent;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;

import javax.swing.text.TextAction;
import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.awt.datatransfer.*;

public final class InputEventInfo {
	private final int mask;
	private final int code;
	public InputEventInfo(int mask, int code) {
		this.mask = mask;
		this.code = code;
	}
	public int hashCode() { return mask ^ code; }
	public boolean equals(Object ie) {
		if (ie instanceof InputEventInfo)
			return this.mask == ie.mask && ie.code == code;
		return false;
	}
	public String toString() {
		String mod = InputEvent.getModifiersExText(mask);
		if (mod == null || mod.length() == 0)
			return KeyEvent.getKeyText(code);
		return mod+"+"+KeyEvent.getKeyText(code);
	}
}

public class UIActionViewContext {
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
		}
		if (ui instanceof Editor) {
			this.editor = (Editor)ui;
			this.dt = editor.cur_elem.dr;
			this.node = editor.cur_elem.node;
			this.dr = dt;
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

public interface UIActionFactory {
	public String getDescr();
	public boolean isForPopupMenu();
	public Runnable getAction(UIActionViewContext context);
}

public final class NavigateView implements Runnable {
	
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
		public String getDescr() { "Scroll the view one line up" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new NavigateView(context.uiv, -1);
		}
	}
	final static class LineDn implements UIActionFactory {
		public String getDescr() { "Scroll the view one line down" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new NavigateView(context.uiv, +1);
		}
	}
	final static class PageUp implements UIActionFactory {
		public String getDescr() { "Scroll the view one page up" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			InfoView uiv = context.uiv;
			return new NavigateView(uiv, -uiv.view_canvas.last_visible.lineno + uiv.view_canvas.first_visible.lineno + 1);
		}
	}
	final static class PageDn implements UIActionFactory {
		public String getDescr() { "Scroll the view one page down" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			InfoView uiv = context.uiv;
			return new NavigateView(uiv, +uiv.view_canvas.last_visible.lineno - uiv.view_canvas.first_visible.lineno - 1);
		}
	}
}

public class NavigateEditor implements Runnable {

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
		public String getDescr() { "Go to the previous element" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-1);
			return null;
		}
	}
	final static class GoNext implements UIActionFactory {
		public String getDescr() { "Go to the next element" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+1);
			return null;
		}
	}
	final static class GoLineUp implements UIActionFactory {
		public String getDescr() { "Go to an element above" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-2);
			return null;
		}
	}
	final static class GoLineDn implements UIActionFactory {
		public String getDescr() { "Go to an element below" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+2);
			return null;
		}
	}
	final static class GoLineHome implements UIActionFactory {
		public String getDescr() { "Go to the first element on the line" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-3);
			return null;
		}
	}
	final static class GoLineEnd implements UIActionFactory {
		public String getDescr() { "Go to the last element on the line" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+3);
			return null;
		}
	}
	final static class GoPageUp implements UIActionFactory {
		public String getDescr() { "Go to an element one screen above" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-4);
			return null;
		}
	}
	final static class GoPageDn implements UIActionFactory {
		public String getDescr() { "Go to an element one screen below" }
		public boolean isForPopupMenu() { false }
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
				uiv.cur_x = prev.x;
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
				uiv.cur_x = next.x;
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
			if (prev.do_newline) {
				n = prev;
				break;
			}
			prev = prev.getPrevLeaf();
		}
		while (n != null) {
			if (n.x <= uiv.cur_x && n.x+n.w >= uiv.cur_x)
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.do_newline)
				break;
			if (prev.x+prev.w < uiv.cur_x)
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
			if (next.do_newline) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			if (n.x <= uiv.cur_x && n.x+n.w >= uiv.cur_x)
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.x > uiv.cur_x)
				break;
			if (next.do_newline)
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
		int lineno = uiv.cur_elem.dr.getFirstLeaf().lineno;
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getPrevLeaf();
			if (dr == null || dr.lineno != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.x;
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigateLineEnd(Editor uiv, boolean repaint) {
		int lineno = uiv.cur_elem.dr.getFirstLeaf().lineno;
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getNextLeaf();
			if (dr == null || dr.lineno != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.x;
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigatePageUp(Editor uiv) {
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.setFirstLine(0);
			return;
		}
		int offs = uiv.view_canvas.last_visible.lineno - uiv.view_canvas.first_visible.lineno -1;
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
		int offs = uiv.view_canvas.last_visible.lineno - uiv.view_canvas.first_visible.lineno -1;
		uiv.view_canvas.incrFirstLine(+offs);
		for (int i=offs; i >= 0; i--)
			navigateDn(uiv,i==0);
		return;
	}

}

public class NavigateNode implements Runnable {

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
		public String getDescr() { "Select parent node" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.node != null && context.editor.cur_elem.node.parent() != null)
				return new NavigateNode(context.editor, "select-up");
			return null;
		}
	}

	final static class InsertMode implements UIActionFactory {
		public String getDescr() { "Change insert/command editor mode" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateNode(context.editor, "insert-mode");
			return null;
		}
	}
	
}


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
		public String getDescription() { description }
		public boolean accept(File f) { f.isDirectory() || f.getName().toLowerCase().endsWith("."+extension) }
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
				fu = (FileUnit)uiv.the_root.ctx_file_unit;
			JFileChooser jfc = new JFileChooser(".");
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);
			File f = new File(fu.pname());
			if (f.getParentFile() != null)
				jfc.setCurrentDirectory(f.getParentFile());
			jfc.setSelectedFile(f);
			jfc.setAcceptAllFileFilterUsed(false);
			foreach (DumpFileFilter dff; dumpFileFilters)
				jfc.addChoosableFileFilter(dff);
			foreach (DumpFileFilter dff; dumpFileFilters; fu.current_syntax == dff.syntax_qname)
				jfc.setFileFilter(dff);
			if (JFileChooser.APPROVE_OPTION != jfc.showDialog(null, "Save"))
				return;
			DumpFileFilter dff = (DumpFileFilter)jfc.getFileFilter();
			try {
				Draw_ATextSyntax stx = Env.getRoot().loadLanguageSyntax(dff.syntax_qname);
				Env.getRoot().dumpTextFile(fu, jfc.getSelectedFile(), stx);
				fu.current_syntax = stx.q_name;
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
				Env.getRoot().dumpTextFile((ASTNode)uiv.the_root, jfc.getSelectedFile(), new XmlDumpSyntax("api").getCompiled().init());
			} catch( IOException e ) {
				System.out.println("Create/write error while Kiev-to-Xml API exporting: "+e);
			}
		}
		else if (action == "save") {
			FileUnit fu;
			if (uiv.the_root instanceof FileUnit)
				fu = (FileUnit)uiv.the_root;
			else
				fu = (FileUnit)uiv.the_root.ctx_file_unit;
			Draw_ATextSyntax stx = null;
			if (fu.current_syntax != null)
				stx = Env.getRoot().loadLanguageSyntax(fu.current_syntax);
			File f = new File(fu.pname());
			if (stx == null || stx != uiv.syntax) {
				JFileChooser jfc = new JFileChooser(".");
				jfc.setDialogType(JFileChooser.SAVE_DIALOG);
				File f = new File(fu.pname());
				if (f.getParentFile() != null)
					jfc.setCurrentDirectory(f.getParentFile());
				jfc.setSelectedFile(f);
				jfc.setAcceptAllFileFilterUsed(false);
				foreach (DumpFileFilter dff; dumpFileFilters)
					jfc.addChoosableFileFilter(dff);
				foreach (DumpFileFilter dff; dumpFileFilters; fu.current_syntax == dff.syntax_qname)
					jfc.setFileFilter(dff);
				if (JFileChooser.APPROVE_OPTION != jfc.showDialog(null,"Save"))
					return;
				DumpFileFilter dff = (DumpFileFilter)jfc.getFileFilter();
				stx = Env.getRoot().loadLanguageSyntax(dff.syntax_qname);
				f = jfc.getSelectedFile();
			}
			try {
				Env.getRoot().dumpTextFile(fu, f, stx);
				fu.current_syntax = stx.q_name;
			} catch( IOException e ) {
				System.out.println("Create/write error while Kiev-to-Xml exporting: "+e);
			}
		}
		else if (action == "load-as") {
			JFileChooser jfc = new JFileChooser(".");
			jfc.setFileFilter(new FileFilter() {
				public boolean accept(File f) { f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") }
				public String getDescription() { "XML file for node tree import" }
			});
			if (JFileChooser.APPROVE_OPTION != jfc.showOpenDialog(null))
				return;
			CompilerParseInfo cpi = new CompilerParseInfo(jfc.getSelectedFile(), false);
			Transaction tr = Transaction.open("Actions.java:load-as");
			try {
				EditorThread thr = EditorThread;
				Compiler.runFrontEnd(thr,new CompilerParseInfo[]{cpi},null,true);
				System.out.println("Frontend compiler completed with "+thr.errCount+" error(s)");
			} catch( IOException e ) {
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
			CompilerThread thr = CompilerThread;
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
			EditorThread thr = EditorThread;
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
		public String getDescr() { "Save the file as a new file" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if !(context.uiv != null && context.uiv.the_root instanceof ASTNode)
				return null;
			return new FileActions(context.uiv, "save-as");
		}
	}

	final static class SaveFileAsApi implements UIActionFactory {
		public String getDescr() { "Save the file as a new API file" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if !(context.uiv != null && context.uiv.the_root instanceof ASTNode)
				return null;
			return new FileActions(context.uiv, "save-as-api");
		}
	}

	final static class SaveFile implements UIActionFactory {
		public String getDescr() { "Save the file" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if !(context.uiv != null && context.uiv.the_root instanceof ASTNode)
				return null;
			return new FileActions(context.uiv, "save");
		}
	}

	final static class LoadFileAs implements UIActionFactory {
		public String getDescr() { "Load a file into current view as a file with specified syntax" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "load-as");
		}
	}

	final static class MergeTreeAll implements UIActionFactory {
		public String getDescr() { "Merge editor's changes into working tree for the whole project" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new FileActions(context.wnd, "merge-all");
		}
	}

	final static class RunBackendAll implements UIActionFactory {
		public String getDescr() { "Run back-end compilation for the whole project" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv != null)
				return new FileActions(context.uiv, "run-backend");
			return null;
		}
	}

	final static class RunFrontendAll implements UIActionFactory {
		public String getDescr() { "Run front-end compilation for the whole project" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new FileActions(context.editor, "run-frontend-all");
			return null;
		}
	}

	final static class RunFrontend implements UIActionFactory {
		public String getDescr() { "Run front-end compilation for the current compilation unit" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new FileActions(context.editor, "run-frontend");
			return null;
		}
	}
}

public final class EditActions implements Runnable {
	
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
		public String getDescr() { "Close the editor window" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new EditActions(context.editor, "close");
			return null;
		}
	}

	final static class Undo implements UIActionFactory {
		public String getDescr() { "Undo last change" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.changes.length > 0)
				return new EditActions(context.editor, "undo");
			return null;
		}
	}
	
	final static class Cut implements UIActionFactory {
		public String getDescr() { "Cut current node" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.dr != null)
				return new EditActions(context.editor, "cut");
			return null;
		}
	}
	
	final static class Del implements UIActionFactory {
		public String getDescr() { "Delete current node" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.dr != null)
				return new EditActions(context.editor, "del");
			return null;
		}
	}
	
	final static class Copy implements UIActionFactory {
		public String getDescr() { "Copy current node" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.cur_elem.dr != null)
				return new EditActions(context.editor, "copy");
			return null;
		}
	}
	
}

public final class RenderActions implements Runnable {
	
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
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for API", "stx-fmt\u001fsyntax-for-api", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for VDOM", "stx-fmt\u001fsyntax-for-vdom", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for VDOM (current)", "stx-fmt\u001fsyntax-for-vdom", true)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for Syntax", "stx-fmt\u001fsyntax-for-syntax", false)));
			m.add(new JMenuItem(new SetSyntaxAction(ui,"Syntax for Syntax (current)", "stx-fmt\u001fsyntax-for-syntax", true)));
			if (ui instanceof InfoView)
				m.show(ui.view_canvas, 0, 0);
			else if (ui instanceof TreeView)
				m.show(ui.the_tree, 0, 0);
		}
		else if (action == "unfold-all") {
			if (ui instanceof InfoView) {
				ui.view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) n.draw_folded = false; return true; }
				});
			}
			ui.formatAndPaint(true);
		}
		else if (action == "fold-all") {
			if (ui instanceof InfoView) {
				ui.view_root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) n.draw_folded = true; return true; }
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
				ui.cur_elem.set(ui.view_root.getFirstLeaf());
			//ui.view_canvas.root = ui.view_root;
			ui.formatAndPaint(false);
		}
	}

	static class SetSyntaxAction extends TextAction {
		private UIView uiv;
		private Class clazz;
		private String qname;
		private boolean in_project;
		SetSyntaxAction(UIView uiv, String text, Class clazz, String name) {
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
				ATextSyntax stx = (ATextSyntax)clazz.newInstance();
				if (stx instanceof XmlDumpSyntax)
					stx.dump = qname;
				this.uiv.setSyntax(stx.getCompiled().init());
				return;
			}
			Draw_ATextSyntax stx = Env.getRoot().getLanguageSyntax(qname, in_project);
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
		public String getDescr() { "Set the syntax of the curret view" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "select-syntax");
		}
	}

	final static class OpenFoldedAll implements UIActionFactory {
		public String getDescr() { "Open (unfold) all folded elements" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.ui, "unfold-all");
		}
	}

	final static class CloseFoldedAll implements UIActionFactory {
		public String getDescr() { "Close (fold) all foldable elements" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.ui, "fold-all");
		}
	}

	final static class ToggleShowAutoGenerated implements UIActionFactory {
		public String getDescr() { "Toggle show of auto-generated code" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-autogen");
		}
	}

	final static class ToggleShowPlaceholders implements UIActionFactory {
		public String getDescr() { "Toggle show of editor placeholders" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-placeholder");
		}
	}

	final static class ToggleHintEscaped implements UIActionFactory {
		public String getDescr() { "Toggle idents and strings escaping" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "toggle-escape");
		}
	}

	final static class Redraw implements UIActionFactory {
		public String getDescr() { "Redraw the window" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.ui, "redraw");
		}
	}
}

public final class ExprEditActions implements Runnable, KeyListener {
	
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
			ENode en = (ENode)context.node;
			DrawNonTerm nt = null;
			{
				Drawable d = context.dr;
				while (d != null && !(d instanceof DrawNonTerm))
					d = (Drawable)d.parent();
				if !(d instanceof DrawNonTerm)
					return;
				nt = (DrawNonTerm)d;
			}
			DrawTerm first = nt.getFirstLeaf();
			DrawTerm last = nt.getLastLeaf().getNextLeaf();
			expr = new ASTExpression();
			for (DrawTerm dt = first; dt != null && dt != last; dt = dt.getNextLeaf()) {
				if (dt.isUnvisible())
					continue;
				if (dt instanceof DrawToken) {
					if (((Draw_SyntaxToken)dt.syntax).kind == SyntaxToken.TokenKind.UNKNOWN)
						expr.nodes += new EToken(0,dt.getText(),ETokenKind.UNKNOWN,false);
					else
						expr.nodes += new EToken(0,dt.getText(),ETokenKind.OPERATOR,true);
				}
				else if (dt instanceof DrawNodeTerm) {
					if (dt.drnode instanceof ConstExpr)
						expr.nodes += new EToken((ConstExpr)dt.drnode);
					else
						expr.nodes += new EToken(0,dt.getText(),ETokenKind.UNKNOWN,false);
				}
			}
			editor.insert_mode = true;
			editor.startItemEditor(this);
			context.node.replaceWithNode(expr);
			foreach (EToken et; expr.nodes)
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
				et.base_kind = ETokenKind.UNKNOWN;
				et.explicit = false;
			} else {
				et.base_kind = kind;
				et.explicit = true;
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
			if (!(n instanceof EToken) || n.parent() != expr || dt == null || dt.drnode != n)
				return;
			EToken et = (EToken)n;
			menu = new JPopupMenu();
			foreach (ETokenKind k; ETokenKind.values())
				menu.add(new SetKindAction(et, k));
			int x = dt.x;
			int y = dt.y + dt.h - editor.view_canvas.translated_y;
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
			if (expr.nodes.length > 0)
				editor.goToPath(makePathTo(expr.nodes[0]));
			else
				editor.goToPath(makePathTo(expr));
			editor.view_canvas.cursor_offset = 0;
			return;
		case KeyEvent.VK_END:
			if (expr.nodes.length > 0)
				editor.goToPath(makePathTo(expr.nodes[expr.nodes.length-1]));
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
			EditorThread thr = EditorThread;
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
		if (!(n instanceof EToken) || n.parent() != expr || dt == null || dt.drnode != n) {
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
			if (et.base_kind != ETokenKind.EXPR_STRING && et.base_kind != ETokenKind.EXPR_CHAR) {
				if (et.explicit && edit_offset != 0 && edit_offset != text.length()) {
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
				if (et.base_kind == ETokenKind.EXPR_STRING || et.base_kind == ETokenKind.EXPR_CHAR) {
					text = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
					edit_offset++;
					et.setText(text);
					break;
				}
				else if (et.base_kind == ETokenKind.EXPR_NUMBER) {
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
			if (pt != null && pt.drnode instanceof EToken) {
				EToken pe = (EToken)pt.drnode;
				editor.cur_elem.set(pt);
				editor.view_canvas.cursor_offset = pt.getText().length();
				pe.setText(pe.ident + et.ident);
				et.detach();
			}
		} else {
			DrawTerm nt = dt.getNextLeaf();
			if (nt != null && nt.drnode instanceof EToken) {
				EToken pe = (EToken)nt.drnode;
				et.setText(et.ident + pe.ident);
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
		path.append(n);
		while (n.parent() != null) {
			n = n.parent();
			path.append(n);
			if (n instanceof FileUnit)
				break;
		}
		return path.toArray();
	}

	final static class Flatten implements UIActionFactory {
		public String getDescr() { "Flatten expresison tree" }
		public boolean isForPopupMenu() { true }
		public Runnable getAction(UIActionViewContext context) {
			ANode node = context.node;
			Drawable dr = context.dr;
			if (context.editor == null || node == null || dr == null)
				return null;
			if !(node instanceof ENode)
				return null;
			return new ExprEditActions(context, "split");
		}
	}
}

