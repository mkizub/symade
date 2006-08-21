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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Graphics2D;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;

import javax.swing.text.TextAction;
import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public class InfoView extends UIView implements KeyListener, MouseWheelListener {

	/** The canvas to show definition of current node */
	protected Canvas		view_canvas;

	protected final Hashtable<Integer,KeyHandler> naviMap;

	{
		this.naviMap = new Hashtable<Integer,KeyHandler>();
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_UP),        new NavigateView(this,NavigateView.LINE_UP));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_DOWN),      new NavigateView(this,NavigateView.LINE_DOWN));
//		this.naviMap.put(Integer.valueOf(KeyEvent.VK_HOME),      new NavigateView(this,NavigateView.LINE_HOME));
//		this.naviMap.put(Integer.valueOf(KeyEvent.VK_END),       new NavigateView(this,NavigateView.LINE_END));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_PAGE_UP),   new NavigateView(this,NavigateView.PAGE_UP));
		this.naviMap.put(Integer.valueOf(KeyEvent.VK_PAGE_DOWN), new NavigateView(this,NavigateView.PAGE_DOWN));
	}
	
	public InfoView(Window window, TextSyntax syntax, Canvas view_canvas) {
		super(window, syntax);
		this.view_canvas = view_canvas;
		this.formatter = new GfxFormatter(syntax, (Graphics2D)view_canvas.getGraphics());
		view_canvas.addMouseListener(this);
		view_canvas.addComponentListener(this);
		view_canvas.addKeyListener(this);
		view_canvas.addMouseWheelListener(this);
	}

	public void setRoot(ANode root) {
		this.the_root = root;
		view_canvas.root = view_root = formatter.format(the_root, view_root);
	}
	
	public void formatAndPaint(boolean full) {
		this.formatter.setWidth(view_canvas.imgWidth);
		view_canvas.root = null;
		if (the_root != null && full)
			view_canvas.root = view_root = formatter.format(the_root, view_root);
		view_canvas.repaint();
	}

	public void mouseClicked(MouseEvent e) {
		view_canvas.requestFocus();
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollAmount() != 0) {
			JScrollBar toScroll = view_canvas.verticalScrollBar;
			int direction = 0;
			// find which scrollbar to scroll, or return if none
			if (toScroll == null || !toScroll.isVisible()) { 
				//toScroll = scrollpane.getHorizontalScrollBar();
				//if (toScroll == null || !toScroll.isVisible()) { 
				//	return;
				//}
				return;
			}
			direction = e.getWheelRotation() < 0 ? -1 : 1;
			if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
				scrollByUnits(toScroll, direction, e.getScrollAmount());
			else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL)
				scrollByBlock(toScroll, direction);
		}
	}
    static void scrollByBlock(JScrollBar scrollbar, int direction) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, and also from scrollByBlock().
	    int oldValue = scrollbar.getValue();
	    int blockIncrement = scrollbar.getBlockIncrement(direction);
	    int delta = blockIncrement * ((direction > 0) ? +1 : -1);
	    int newValue = oldValue + delta;
	    
	    // Check for overflow.
	    if (delta > 0 && newValue < oldValue) {
		newValue = scrollbar.getMaximum();
	    }
	    else if (delta < 0 && newValue > oldValue) {
		newValue = scrollbar.getMinimum();
	    }

	    scrollbar.setValue(newValue);			
    }
    static void scrollByUnits(JScrollBar scrollbar, int direction,
                                   int units) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, as well as from scrollByUnit().
        int delta;

	for (int i=0; i<units; i++) {
	    if (direction > 0) {
		delta = scrollbar.getUnitIncrement(direction);
	    }
	    else {
		delta = -scrollbar.getUnitIncrement(direction);
	    }

	    int oldValue = scrollbar.getValue();
	    int newValue = oldValue + delta;
	    
	    // Check for overflow.
	    if (delta > 0 && newValue < oldValue) {
		newValue = scrollbar.getMaximum();
	    }
	    else if (delta < 0 && newValue > oldValue) {
		newValue = scrollbar.getMinimum();
	    }
	    if (oldValue == newValue) {
		break;
	    }
	    scrollbar.setValue(newValue);
	}
    }
	
	
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == 0) {
			KeyHandler kh = naviMap.get(Integer.valueOf(code));
			if (kh != null) {
				kh.process();
				evt.consume();
			}
			return;
		}
		else if (mask == KeyEvent.ALT_DOWN_MASK) {
			switch (code) {
			case KeyEvent.VK_S: {
				evt.consume();
				if !(this.the_root instanceof ASTNode)
					break;
				JFileChooser jfc = new JFileChooser(".");
				jfc.setFileFilter(new FileFilter() {
					public boolean accept(File f) { f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") }
					public String getDescription() { "XML file for node tree dump" }
				});
				if (JFileChooser.APPROVE_OPTION != jfc.showOpenDialog(null))
					break;
				try {
					Env.dumpTextFile((ASTNode)this.the_root, jfc.getSelectedFile(), new XmlDumpSyntax());
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
					setRoot(Env.loadFromXmlFile(jfc.getSelectedFile()));
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
				m.add(new JMenuItem(new SetSyntaxAction("Kiev Syntax", "stx-fmt.syntax-for-java")));
				m.add(new JMenuItem(new LoadSyntaxAction("Kiev Syntax (java.xml)", "java.xml", "test.syntax-for-java")));
				m.add(new JMenuItem(new SetSyntaxAction("XML dump Syntax", XmlDumpSyntax.class)));
				m.add(new JMenuItem(new SetSyntaxAction("Syntax for Syntax", "stx-fmt.syntax-for-syntax")));
				m.add(new JMenuItem(new LoadSyntaxAction("Syntax for Syntax (stx.xml)", "stx.xml", "test.syntax-for-syntax")));
				m.show(view_canvas, 0, 0);
				break;
				}
			case KeyEvent.VK_F:
				evt.consume();
				// fold everything
				if (this.view_root != null) {
					this.view_root.walkTree(new TreeWalker() {
						public boolean pre_exec(ANode n) { if (n instanceof DrawFolded) n.draw_folded = true; return true; }
					});
					this.formatAndPaint(true);
				}
				break;
			}
		}
	}
}

class NavigateView implements KeyHandler {
	static final int NONE       = 0;
	static final int LEFT       = 1;
	static final int RIGHT      = 2;
	static final int LINE_UP    = 3;
	static final int LINE_DOWN  = 4;
	static final int LINE_HOME  = 5;
	static final int LINE_END   = 6;
	static final int PAGE_UP    = 7;
	static final int PAGE_DOWN  = 8;

	final InfoView uiv;
	final int cmd;
	NavigateView(InfoView uiv, int cmd) {
		this.uiv = uiv;
		this.cmd = cmd;
	}

	public void process() {
		switch (cmd) {
		case LINE_UP:    navigateUp();   return;
		case LINE_DOWN:  navigateDn(); return;
		case PAGE_UP:    navigatePageUp();  return;
		case PAGE_DOWN:  navigatePageDn();  return;
		}
	}

	private void navigateUp() {
		Canvas view_canvas = uiv.view_canvas;
		view_canvas.incrFirstLine(-1);
	}
	private void navigateDn() {
		Canvas view_canvas = uiv.view_canvas;
		view_canvas.incrFirstLine(+1);
	}
	private void navigatePageUp() {
		Canvas view_canvas = uiv.view_canvas;
		view_canvas.incrFirstLine(-view_canvas.last_visible.lineno + view_canvas.first_visible.lineno + 1);
	}
	private void navigatePageDn() {
		Canvas view_canvas = uiv.view_canvas;
		view_canvas.incrFirstLine(view_canvas.last_visible.lineno - view_canvas.first_visible.lineno - 1);
	}
}


