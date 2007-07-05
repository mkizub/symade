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
import javax.swing.filechooser.FileNameExtensionFilter;

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
				ATextSyntax stx = (ATextSyntax)Env.resolveGlobalDNode(dff.syntax_qname);
				Env.dumpTextFile(fu, jfc.getSelectedFile(), stx.ncopy());
				fu.current_syntax = stx.qname();
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
				Env.dumpTextFile((ASTNode)uiv.the_root, jfc.getSelectedFile(), new XmlDumpSyntax("api"));
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
			ATextSyntax stx = null;
			if (fu.current_syntax != null) {
				DNode d = Env.resolveGlobalDNode(fu.current_syntax);
				if (d instanceof ATextSyntax)
					stx = (ATextSyntax)d;
			}
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
				stx = (ATextSyntax)Env.resolveGlobalDNode(dff.syntax_qname);
				f = jfc.getSelectedFile();
			}
			try {
				Env.dumpTextFile(fu, f, stx.ncopy());
				fu.current_syntax = stx.qname();
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
			FileUnit fu = null;
			Transaction tr = Transaction.open("Actions.java:load-as");
			try {
				EditorThread thr = EditorThread;
				fu = Env.loadFromXmlFile(jfc.getSelectedFile());
				try {
					thr.errCount = 0;
					thr.warnCount = 0;
					Compiler.runFrontEnd(thr,null,fu,true);
				} catch (Throwable t) { t.printStackTrace(); }
				System.out.println("Frontend compiler completed with "+thr.errCount+" error(s)");
				Kiev.lockNodeTree(fu);
			} catch( IOException e ) {
				System.out.println("Read error while Xml-to-Kiev importing: "+e);
			} finally { tr.close(); }
			if (fu != null)
				wnd.openEditor(fu);
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
		editor.changes.push(tr);
		EditorThread thr = EditorThread;
		try {
			thr.errCount = 0;
			thr.warnCount = 0;
			Compiler.runFrontEnd(thr,null,root,true);
		} catch (Throwable t) { t.printStackTrace(); }
		System.out.println("Frontend compiler completed with "+thr.errCount+" error(s)");
		if (tr.isEmpty()) {
			tr.close();
			editor.changes.pop();
		} else {
			tr.close();
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
				AttrPtr pattr = ((DrawNodeTerm)editor.cur_elem.dr).getAttrPtr();
				Object obj = pattr.get();
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
	
	final InfoView uiv;
	final String action;
	
	RenderActions(InfoView uiv, String action) {
		this.uiv = uiv;
		this.action = action;
	}
	
	public void run() {
		if (action == "select-syntax") {
			// build a menu of types to instantiate
			JPopupMenu m = new JPopupMenu();
			m.add(new JMenuItem(new SetSyntaxAction(uiv,"Kiev Syntax", "stx-fmt\u001fsyntax-for-java")));
//			m.add(new JMenuItem(new LoadSyntaxAction(uiv,"Kiev Syntax (java.xml)", "java.xml", "test\u001fsyntax-for-java")));
			m.add(new JMenuItem(new SetSyntaxAction(uiv,"TreeDL Syntax", "treedl\u001fsyntax-for-treedl")));
			m.add(new JMenuItem(new SetSyntaxAction(uiv,"XML dump Syntax (full)", XmlDumpSyntax.class, "full")));
			m.add(new JMenuItem(new SetSyntaxAction(uiv,"XML dump Syntax (api)", XmlDumpSyntax.class, "api")));
			m.add(new JMenuItem(new SetSyntaxAction(uiv,"Syntax for API", "stx-fmt\u001fsyntax-for-api")));
			m.add(new JMenuItem(new SetSyntaxAction(uiv,"Syntax for Syntax", "stx-fmt\u001fsyntax-for-syntax")));
//			m.add(new JMenuItem(new LoadSyntaxAction(uiv,"Syntax for Syntax (stx.xml)", "stx.xml", "test.syntax-for-syntax")));
			m.show(uiv.view_canvas, 0, 0);
		}
		else if (action == "unfold-all") {
			uiv.view_root.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) n.draw_folded = false; return true; }
			});
			uiv.formatAndPaint(true);
		}
		else if (action == "fold-all") {
			uiv.view_root.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) n.draw_folded = true; return true; }
			});
			uiv.formatAndPaint(true);
		}
		else if (action == "toggle-autogen") {
			uiv.show_auto_generated = !uiv.show_auto_generated;
			uiv.formatAndPaint(true);
		}
		else if (action == "toggle-placeholder") {
			uiv.show_placeholders = !uiv.show_placeholders;
			uiv.formatAndPaint(true);
		}
		else if (action == "toggle-escape") {
			uiv.show_hint_escapes = !uiv.show_hint_escapes;
			uiv.formatAndPaint(true);
		}
		else if (action == "redraw") {
			uiv.setSyntax(uiv.syntax);
			if (uiv instanceof Editor)
				((Editor)uiv).cur_elem.set(uiv.view_root.getFirstLeaf());
			uiv.view_canvas.root = uiv.view_root;
			uiv.formatAndPaint(false);
		}
	}

	static class SetSyntaxAction extends TextAction {
		private UIView uiv;
		private Class clazz;
		private String qname;
		SetSyntaxAction(UIView uiv, String text, Class clazz, String name) {
			super(text);
			this.uiv = uiv;
			this.clazz = clazz;
			this.qname = name;
		}
		SetSyntaxAction(UIView uiv, String text, String qname) {
			super(text);
			this.uiv = uiv;
			this.qname = qname;
		}
		public void actionPerformed(ActionEvent e) {
			if (clazz != null) {
				ATextSyntax stx = (ATextSyntax)clazz.newInstance();
				if (stx instanceof XmlDumpSyntax)
					stx.dump = qname;
				this.uiv.setSyntax(stx);
				return;
			}
			ATextSyntax stx = Env.resolveGlobalDNode(qname);
			this.uiv.setSyntax(stx);
		}
	}
	
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
				fu = Env.loadFromXmlFile(new File(this.file));
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
				this.uiv.setSyntax(stx);
				return;
			}
		}
	}

	final static class SyntaxFileAs implements UIActionFactory {
		public String getDescr() { "Set the syntax of the curret view" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.uiv, "select-syntax");
		}
	}

	final static class OpenFoldedAll implements UIActionFactory {
		public String getDescr() { "Open (unfold) all folded elements" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.uiv, "unfold-all");
		}
	}

	final static class CloseFoldedAll implements UIActionFactory {
		public String getDescr() { "Close (fold) all foldable elements" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			if (context.uiv == null || context.uiv.view_root == null)
				return null;
			return new RenderActions(context.uiv, "fold-all");
		}
	}

	final static class ToggleShowAutoGenerated implements UIActionFactory {
		public String getDescr() { "Toggle show of auto-generated code" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.uiv, "toggle-autogen");
		}
	}

	final static class ToggleShowPlaceholders implements UIActionFactory {
		public String getDescr() { "Toggle show of editor placeholders" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.uiv, "toggle-placeholder");
		}
	}

	final static class ToggleHintEscaped implements UIActionFactory {
		public String getDescr() { "Toggle idents and strings escaping" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.uiv, "toggle-escape");
		}
	}

	final static class Redraw implements UIActionFactory {
		public String getDescr() { "Redraw the window" }
		public boolean isForPopupMenu() { false }
		public Runnable getAction(UIActionViewContext context) {
			return new RenderActions(context.uiv, "redraw");
		}
	}
}

public final class ExprEditActions implements Runnable, KeyListener {
	
	final Editor editor;
	final UIActionViewContext context;
	final String action;
	
	private ASTExpression expr;
	
	ExprEditActions(UIActionViewContext context, String action) {
		this.editor = context.editor;
		this.context = context;
		this.action = action;
	}
	
	public void run() {
		if (action == "split") {
			ENode en = context.node;
			DrawNonTerm nt = (DrawNonTerm)context.dr.parent();
			DrawTerm first = nt.getFirstLeaf();
			DrawTerm last = nt.getLastLeaf().getNextLeaf();
			expr = new ASTExpression();
			for (DrawTerm dt = first; dt != null && dt != last; dt = dt.getNextLeaf()) {
				if (dt.isUnvisible())
					continue;
				EToken et = null;
				if (dt instanceof DrawToken) {
					switch (((SyntaxToken)dt.syntax).kind) {
					case SyntaxToken.TokenKind.UNKNOWN:
						et = new EToken(0,dt.getText(),0);
						break;
					case SyntaxToken.TokenKind.KEYWORD:
						et = new EToken(0,dt.getText(),EToken.IS_IDENTIFIER|EToken.IS_KEYWORD|EToken.IS_OPERATOR);
						break;
					case SyntaxToken.TokenKind.OPERATOR:
						et = new EToken(0,dt.getText(),EToken.IS_OPERATOR);
						break;
					case SyntaxToken.TokenKind.SEPARATOR:
						et = new EToken(0,dt.getText(),EToken.IS_OPERATOR);
						break;
					}
				}
				else if (dt instanceof DrawNodeTerm) {
					if (dt.drnode instanceof ConstExpr)
						et = new EToken(0,dt.getText(),EToken.IS_CONSTANT); //((ConstExpr)dt.drnode).ncopy();
					else
						et = new EToken(0,dt.getText(),0);
				}
				if (et != null) {
					//et.guessKind();
					expr.nodes += et;
				}
			}
			editor.insert_mode = true;
			editor.startItemEditor(this);
			context.node.replaceWithNode(expr);
			editor.formatAndPaint(true);
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
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
			ASTNode e = expr;
			try {
				rewalk:
				Kiev.runProcessorsOn(e);
			}
			catch (ReWalkNodeException t) { e = t.replacer; goto rewalk; }
			catch (Throwable t) { t.printStackTrace(); }
			editor.stopItemEditor(false);
			return;
		case KeyEvent.VK_ESCAPE:
			editor.insert_mode = true;
			editor.stopItemEditor(true);
			return;
		}
		DrawTerm dt = editor.cur_elem.dr;
		ANode n = editor.cur_elem.node;
		if (n == null || (n != expr && n.parent() != expr) || dt == null || dt.drnode != n) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		String text = dt.getText();
		if (text == null) { text = ""; }
		int prefix_offset = dt.getPrefix().length();
		int suffix_offset = dt.getSuffix().length();
		text = text.substring(prefix_offset, text.length() - prefix_offset - suffix_offset);
		int edit_offset = editor.view_canvas.cursor_offset - prefix_offset;
		if (edit_offset < 0 || edit_offset > text.length()) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		switch (code) {
		case KeyEvent.VK_DELETE:
			if (edit_offset < text.length()) {
				text = text.substring(0, edit_offset)+text.substring(edit_offset+1);
				this.setText(text);
			}
			else if (edit_offset == 0 && text.length() == 0) {
				this.setText(null);
			}
			break;
		case KeyEvent.VK_BACK_SPACE:
			if (edit_offset > 0) {
				edit_offset--;
				text = text.substring(0, edit_offset)+text.substring(edit_offset+1);
				this.setText(text);
			}
			else if (edit_offset == 0 && text.length() == 0) {
				this.setText(null);
			}
			break;
		default:
			if (evt.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
				char ch = evt.getKeyChar();
				if (ch == '.' && dt instanceof DrawIdent)
					ch = '\u001f';
				text = text.substring(0, edit_offset)+ch+text.substring(edit_offset);
				edit_offset++;
				this.setText(text);
			}
		}
		editor.view_canvas.cursor_offset = edit_offset+prefix_offset;
		editor.formatAndPaint(true);
	}
	
	private void setText(String text) {
		DrawTerm dt = editor.cur_elem.dr;
		ANode n = editor.cur_elem.node;
		if (n == null || (n != expr && n.parent() != expr) || dt == null || dt.drnode != n) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
		if (text == null || text.length() == 0) {
			if (n.parent() == expr) {
				// delete current token
				if (n instanceof EToken)
					n.ident = "";
				editor.cur_elem.set(dt.getPrevLeaf().getPrevLeaf());
				n.detach();
				return;
			}
		}
		else if (dt instanceof DrawToken && dt.drnode == expr) {
			// create new token
			ActionPoint ap = editor.getActionPoint(false);
			if (ap != null && ap.length >= 0) {
				EToken et = new EToken(0,text,0);
				expr.nodes.insert(ap.index, et);
				et.guessKind();
				editor.formatAndPaint(true);
				editor.goToPath(makePathTo(et));
				return;
			}
		}
		else if (n instanceof EToken) {
			if (n.parent() == expr) {
				if (text.equals("\"") && (n.ident == null || n.ident == "")) {
					n.ident = "\"\"";
				}
				else if (text.equals("\'") && (n.ident == null || n.ident == "")) {
					n.ident = "\'\'";
				}
				else { 
					n.ident = text;
				}
				n.guessKind();
				return;
			}
		}
		java.awt.Toolkit.getDefaultToolkit().beep();
		return;
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

