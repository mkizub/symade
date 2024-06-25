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

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.fmt.common.Draw_StyleSheet;
import kiev.fmt.common.Draw_TopLevelTextSyntax;
import kiev.fmt.common.GfxFormatter;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.gui.event.InputEvent;
import kiev.vtree.ChangeListener;
import kiev.vtree.INode;
import kiev.vtree.RootNodeProjector;
import kiev.vtree.NodeProjectorFactory;
import kiev.vtree.NodeChangeInfo;

/**
 * The view components of the GUI. 
 */
public class UIView implements IUIView, ElementChangeListener, ChangeListener {

	private static int bgFormatterCounter;
	/** 
	 * The reference to the <code>Window</code> that is the main GUI start. 
	 */
	public final Window window;
	
	/** 
	 * The GUI toolkit peer. 
	 */
	public final ICanvas peer;
	
	/** 
	 * The formatter of the current view.
	 */
	public GfxFormatter formatter;
	
	/** 
	 * The semantic root node to display. 
	 */
	public INode the_semantic_root;
	
	/** 
	 * The root node to display. 
	 */
	private INode the_root;
	
	/**
	 * The root projection used to get the_root
	 */
	private NodeProjectorFactory proj;
	
	/** 
	 * The root node of document we edit - the whole program. 
	 */
	protected Drawable view_root;
	
	/** 
	 * The syntax in use. 
	 */
	public Draw_ATextSyntax syntax;
	
	/** 
	 * The style in use. 
	 */
	public Draw_StyleSheet style;
	
	/** 
	 * A hint to show placeholders. 
	 */
	public boolean show_placeholders;
	
	/** 
	 * A background thread to format and paint. 
	 */
	private final BackgroundFormatter bg_formatter;


	/**
	 * A background thread to format and paint.
	 */
	private final class BackgroundFormatter extends Thread {
		/**
		 * is started
		 */
		private boolean is_started;
		
		/**
		 * is do format
		 */
		private boolean do_format;
		
		/**
		 * The constructor
		 * @param view the view
		 */
		BackgroundFormatter() {
			super("bg-formatter "+(++bgFormatterCounter));
			this.setDaemon(true);
			this.setPriority(Thread.NORM_PRIORITY - 1);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			is_started = true;
			for (;;) {
				while (!do_format) {
					synchronized(this) { try {
						this.wait();
					} catch (InterruptedException e) {}
					}
					continue;
				}
				this.do_format = false;
				window.getEditorThreadGroup().runTask(new Runnable() {
					public void run() {
						formatAndPaint(true);
					}
				});
			}
		}
		
		/**
		 * Daemon waked up.
		 */
		synchronized void schedule_run() {
			if (!is_started)
				this.start();
			this.do_format = true;
			synchronized(this) {
				this.notify();
			}
		}
	}

	/**
	 * The constructor.
	 * @param window the window
	 * @param peer the peer
	 * @param syntax the syntax
	 */
	public UIView(Window window, ICanvas peer, Draw_ATextSyntax syntax) {
		this.window = window;
		this.peer = peer;
		this.syntax = syntax;
		this.window.views = (UIView[])kiev.stdlib.Arrays.append(this.window.views, this);
		this.peer.setUIView(this);
		this.formatter = new GfxFormatter(window.getCurrentEnv(), peer.getFmtGraphics());
		this.bg_formatter = new BackgroundFormatter();
	}
	
	/**
	 * Get the root window. 
	 * @return the root node.
	 */
	public final IWindow getWindow() {
		return this.window;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#getSyntax()
	 */
	public Draw_ATextSyntax getSyntax() { return syntax; }

	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#setSyntax(kiev.fmt.Draw_ATextSyntax)
	 */
	public void setSyntax(Draw_ATextSyntax syntax) {
		this.syntax = syntax;
		view_root = null;
		if (syntax instanceof Draw_TopLevelTextSyntax) {
			final Draw_TopLevelTextSyntax tlsyntax = (Draw_TopLevelTextSyntax)syntax;
			if (tlsyntax.root_projection != null && tlsyntax.root_projection.length() > 0) {
				setRoot(the_semantic_root, true);
				return;
			}
		}
		setRoot(the_semantic_root, true);
	}
		
	/**
	 * Returns the style.
	 */
	public Draw_StyleSheet getStyle() {
		return this.style;
	}
	public void setStyle(Draw_StyleSheet style) {
		this.style = style;
		setSyntax(getSyntax());
	}
		
	/**
	 * Get the root node. 
	 * @return the root node.
	 */
	public INode getRoot() {
		return this.the_root;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#setRoot(kiev.vtree.INode)
	 */
	public void setRoot(INode root, boolean format) {
		the_semantic_root = root;
		the_root = root;
		if (proj != null) {
			proj.dispose();
			proj = null;
		}
		if (syntax instanceof Draw_TopLevelTextSyntax) {
			final Draw_TopLevelTextSyntax tlsyntax = (Draw_TopLevelTextSyntax)syntax;
			if (tlsyntax.root_projection != null && tlsyntax.root_projection.length() > 0) {
				final INode xroot = root;
				try {
					proj = (NodeProjectorFactory)Class.forName(tlsyntax.root_projection).newInstance();
					RootNodeProjector rnp = proj.getRootProjector(xroot, null);
					rnp.project();
					the_root = rnp.getDstNode();
					proj.setListener(UIView.this);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (format) {
			view_root = null;
			peer.setDlb_root(null);
			if (the_root != null) {
				formatter.format(the_root, view_root, getSyntax(), getStyle());
				view_root = formatter.getRootDrawable();
				peer.setDlb_root(formatter.getRootDrawLayoutBlock());
			}
			peer.repaint();
		}
	}

	/**
	 * Get the view root node. 
	 * @return the view root node.
	 */
	public final Drawable getViewRoot() {
		return this.view_root;
	}

	/**
	 * Get currently selected node. 
	 * @return the selected node or null if nothing selected.
	 */
	public INode getSelectedNode() { return null; }

	/**
	 * Get current DrawTerm under cursor. 
	 * @return the pointed DrawTerm or null.
	 */
	public DrawTerm getDrawTerm() { return null; }

	/**
	 * Set new selected (by mouse or so) draw term.
	 * @param dr selected term
	 */
	public void selectDrawTerm(DrawTerm dr) {}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#formatAndPaint(boolean)
	 */
	public void formatAndPaint(boolean full) {
		this.formatter.setWidth(peer.getImgWidth());
		peer.setDlb_root(null);
		if (the_root != null && full) {
			formatter.format(the_root, view_root, getSyntax(), getStyle());
			view_root = formatter.getRootDrawable();
			peer.setDlb_root(formatter.getRootDrawLayoutBlock());
		}
		peer.repaint();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#formatAndPaintLater()
	 */
	public void formatAndPaintLater() {
		bg_formatter.schedule_run();
	}

	public void callbackNodeChanged(NodeChangeInfo info) {
		formatAndPaintLater();		
	}

	/* (non-Javadoc)
	 * @see kiev.gui.event.ElementChangeListener#elementChanged(kiev.gui.event.ElementEvent)
	 */
	public void elementChanged(ElementEvent e) {
		INode node = e.node;
		setRoot(node, false);
		formatAndPaintLater();
	}

	/**
	 * Transforms native event to internal event and run associated action. 
	 * TODO this is subject to override.
	 * @param evt the event
	 * @return false if the action is not run
	 */
	public boolean inputEvent(InputEvent evt) {
		UIActionFactory[] actions = UIManager.getUIActions(this).get(evt);
		if (actions == null) return false;
		for (UIActionFactory af: actions) {
			final UIAction action = af.getAction(new UIActionViewContext(window, evt, this));
			if (action != null) {
				window.getEditorThreadGroup().runTask(new Runnable() {
					public void run() {
						action.exec();
					}
				});
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#getViewPeer()
	 */
	public ICanvas getViewPeer() {
		return this.peer;
	}

	/**
	 * Checks if this viewable is in text (insert) mode
	 */
	public boolean isInInsertMode() {
		return false;
	}
	/**
	 * Sets insert mode (for editors)
	 */
	public void setInInsertMode(boolean value) {
		// ignore
	}
	
}
