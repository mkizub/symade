package kiev.gui;

import kiev.Kiev;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.text.TextAction;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;


/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public class InfoView extends UIView implements KeyListener {

	public InfoView(Window window, TextSyntax syntax, Canvas info_canvas) {
		super(window, syntax, info_canvas);
		view_canvas.addKeyListener(this);
	}

	public void formatAndPaint(boolean full) {
		view_canvas.root = null;
		if (the_root != null && full)
			view_canvas.root = view_root = formatter.format(the_root);
		view_canvas.repaint();
	}

	public void keyReleased(KeyEvent evt) {
		//System.out.println(evt);
	}
	public void keyTyped(KeyEvent evt) {
		//System.out.println(evt);
	}
	
	public void keyPressed(KeyEvent evt) {
		//System.out.println(evt);
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == 0) {
			switch (code) {
			case KeyEvent.VK_UP:
				if (view_canvas.first_line > 0) {
					--view_canvas.first_line;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_DOWN:
				if (view_canvas.first_line < view_canvas.num_lines) {
					++view_canvas.first_line;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_PAGE_UP:
				if (view_canvas.first_line > 0) {
					view_canvas.first_line -= view_canvas.last_visible.geometry.lineno - view_canvas.first_visible.geometry.lineno - 1;
					if (view_canvas.first_line < 0)
						view_canvas.first_line = 0;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if (view_canvas.first_line < view_canvas.num_lines) {
					view_canvas.first_line += view_canvas.last_visible.geometry.lineno - view_canvas.first_visible.geometry.lineno -1;
					if (view_canvas.first_line >= view_canvas.num_lines)
						view_canvas.first_line = view_canvas.num_lines-1;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			}
		}
		else if (mask == KeyEvent.ALT_DOWN_MASK) {
			switch (code) {
			case KeyEvent.VK_S: {
				evt.consume();
				if (this.the_root == null)
					break;
				JFileChooser jfc = new JFileChooser(".");
				jfc.setFileFilter(new FileFilter() {
					public boolean accept(File f) { f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") }
					public String getDescription() { "XML file for node tree dump" }
				});
				if (JFileChooser.APPROVE_OPTION != jfc.showOpenDialog(null))
					break;
				try {
					Env.dumpTextFile(this.the_root, jfc.getSelectedFile(), new XmlDumpSyntax());
				} catch( IOException e ) {
					System.out.println("Create/write error while Kiev-to-Xml exporting: "+e);
				}
				}
				break;
			case KeyEvent.VK_L: {
				evt.consume();
				JFileChooser jfc = new JFileChooser(".");
				jfc.setFileFilter(new FileFilter() {
					public boolean accept(File f) { f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") }
					public String getDescription() { "XML file for node tree import" }
				});
				if (JFileChooser.APPROVE_OPTION != jfc.showOpenDialog(null))
					break;
				try {
					this.the_root = Env.loadFromXmlFile(jfc.getSelectedFile());
				} catch( IOException e ) {
					System.out.println("Read error while Xml-to-Kiev importing: "+e);
				}
				this.formatAndPaint(true);
				}
				break;
			}
		}
		else if (mask == (KeyEvent.CTRL_DOWN_MASK|KeyEvent.ALT_DOWN_MASK)) {
			switch (code) {
			case KeyEvent.VK_S: {
				evt.consume();
				// build a menu of types to instantiate
				JPopupMenu m = new JPopupMenu();
				m.add(new JMenuItem(new SetSyntaxAction("Java Syntax", JavaSyntax.class)));
				m.add(new JMenuItem(new SetSyntaxAction("XML dump Syntax", XmlDumpSyntax.class)));
				m.add(new JMenuItem(new SetSyntaxAction("Syntax for Syntax", SyntaxForSyntax.class)));
				m.show(view_canvas, 0, 0);
				break;
				}
			}
		}
	}
	
	class SetSyntaxAction extends TextAction {
		private Class clazz;
		SetSyntaxAction(String text, Class clazz) {
			super(text);
			this.clazz = clazz;
		}
		public void actionPerformed(ActionEvent e) {
			TextSyntax stx = (TextSyntax)clazz.newInstance();
			InfoView.this.setSyntax(stx);
		}
	}
}
