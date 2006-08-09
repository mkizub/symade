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

import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Graphics2D;
import javax.swing.text.TextAction;

/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public abstract class UIView extends ANode implements MouseListener, ComponentListener  {

	/** The workplace window */
	protected Window		parent_window;
	/** The formatter of the current view */
	protected GfxFormatter	formatter;
	/** The root node to display */
	@ref public ANode		the_root;
	/** The root node of document we edit - the whole program */
	@ref public Drawable	view_root;
	/** The syntax in use */
	@ref public TextSyntax	syntax;

	public UIView(Window window, TextSyntax syntax) {
		this.parent_window = window;
		this.syntax        = syntax;
	}
	
	public TextSyntax getSyntax() { return syntax; }

	public void setSyntax(TextSyntax syntax) {
		this.syntax = syntax;
		view_root = null;
		formatter.setSyntax(syntax);
		formatAndPaint(true);
	}
	
	public abstract void setRoot(ANode root) {}
	
	public abstract void formatAndPaint(boolean full);

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {
		formatAndPaint(true);
	}

	protected class SetSyntaxAction extends TextAction {
		private Class clazz;
		private String qname;
		SetSyntaxAction(String text, Class clazz) {
			super(text);
			this.clazz = clazz;
		}
		SetSyntaxAction(String text, String qname) {
			super(text);
			this.qname = qname;
		}
		public void actionPerformed(ActionEvent e) {
			if (clazz != null) {
				TextSyntax stx = (TextSyntax)clazz.newInstance();
				UIView.this.setSyntax(stx);
				return;
			}
			TextSyntax stx = Env.resolveGlobalDNode(qname);
			UIView.this.setSyntax(stx);
		}
	}
	
	protected class LoadSyntaxAction extends TextAction {
		private String file;
		private String name;
		LoadSyntaxAction(String text, String file, String name) {
			super(text);
			this.file = file.replace('/',File.separatorChar);
			this.name = name.intern();
		}
		public void actionPerformed(ActionEvent e) {
			FileUnit fu = (FileUnit)Env.loadFromXmlFile(new File(this.file));
			foreach (TextSyntax stx; fu.members; stx.u_name == name) {
				UIView.this.setSyntax(stx);
				return;
			}
		}
	}
}
