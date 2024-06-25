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

import kiev.WorkerThreadGroup;
import kiev.EditorThreadGroup;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vlang.Project;
import kiev.vtree.INode;
import kiev.vtree.Transaction;

/**
 * Common stuff. 
 */
public abstract class Window implements IWindow {
	
	private class TransactionInfo {
		String name;
		Transaction tr;
		IEditor editor;
		String fileName;
		public TransactionInfo(String name, Transaction tr, IEditor editor) {
			this.name = name;
			this.tr = tr;
			this.editor = editor;
			this.fileName = editor.getFileUnit().getFname();
		}
	}
	
	/**
	 * The current <code>Env</code>.
	 */
	protected final Env currentEnv;
	
	/**
	 * The current <code>EditorThreadGroup</code>.
	 */
	protected final EditorThreadGroup currentEditorThreadGroup;

	/**
	 * The views array.
	 */
	protected UIView[] views = new UIView[0];

	/**
	 * The changes.
	 */
	private java.util.Stack<TransactionInfo> changes = new java.util.Stack<TransactionInfo>();

	/**
	 * The constructor.
	 * @param env the <code>Env</code>
	 */
	public Window(WorkerThreadGroup thrg){
		currentEnv = thrg.getEnv();
		currentEditorThreadGroup = new EditorThreadGroup(thrg);
	}
	
	/** 
	 * List of current element change listeners. 
	 * When editor sets the current element these listeners are notified. 
	 */
	private ElementChangeListener[] elementChangeListeners = new ElementChangeListener[0];

	/**
	 * Returns the current environment.
	 * @return the environment
	 */
	public Env getCurrentEnv() {
		return currentEnv;
	}
		
	/**
	 * Returns the current project.
	 * @return the current project
	 */
	public Project getCurrentProject() {
		return currentEnv.proj;
	}

	/**
	 * Returns the <code>EditorThreadGroup</code>.
	 * @return the EditorThreadGroup
	 */
	public EditorThreadGroup getEditorThreadGroup() {
		return currentEditorThreadGroup;
	}

	/**
	 * Adds the listener to the list that's notified each time an element change
	 * occurs.
	 * @param	l	the <code>ElementChangeListener</code>
	 */
	public void addElementChangeListener(ElementChangeListener l) {
		for (ElementChangeListener ecl : elementChangeListeners) {
			if (ecl == l)
				return;
		}
		ElementChangeListener[] tmp = new ElementChangeListener[elementChangeListeners.length + 1];
		System.arraycopy(elementChangeListeners, 0, tmp, 0, elementChangeListeners.length);
		tmp[elementChangeListeners.length] = l;
		elementChangeListeners = tmp;
	}

	/**
	 * Removes the listener from the list of element change listeners.
	 * @param l the <code>ElementChangeListener</code>
	 */
	public void removeElementChangeListener(ElementChangeListener l) {
		int i = 0;
		for (ElementChangeListener ecl : elementChangeListeners) {
			if (ecl == l){
				ElementChangeListener[] tmp = new ElementChangeListener[elementChangeListeners.length-1];
				System.arraycopy(elementChangeListeners, 0, tmp, 0, i);
				System.arraycopy(elementChangeListeners, i+1, tmp, i, elementChangeListeners.length-(i+1));
				elementChangeListeners = tmp;
				break;
			}
			i++;
		}
	}

	/**
	 * Forwards the given notification event to all
	 * <code>ElementChangeListeners</code> that registered
	 * themselves as listeners for <code>ElementEvent</code> event. 
	 * Currently we redraw views in the background when editor selection changes.
	 * @param e  the event to be forwarded
	 * @see #addElementChangeListener
	 * @see ElementEvent
	 * @see EventListenerList
	 */
	public void fireElementChanged(ElementEvent e) {
		for (ElementChangeListener l : elementChangeListeners) {
			l.elementChanged(e);
		}
	}
	
	/**
	 * Notify that errors list may be changed.
	 */
	public abstract void fireErrorsModified();

	/**
	 * Checks and enables menu items.
	 */
	protected abstract void enableMenuItems();
	
	/*
	 * Start new transaction
	 */
	public void startTransaction(IEditor editor, String name) {
		TransactionInfo tri = new TransactionInfo(name, Transaction.open(name, getEditorThreadGroup()), editor);
		changes.push(tri);
	}

	/*
	 * Finish transaction
	 */
	public void stopTransaction(boolean revert) {
		TransactionInfo tri = changes.peek();
		tri.tr.close(getEditorThreadGroup());
		if (revert || tri.tr.isEmpty()) {
			changes.pop();
			tri.tr.rollback(false);
		}
		updateStatusBar();
	}
	
	/*
	 * Undo
	 */
	public void undoTransaction() {
		if (changes.isEmpty())
			return;
		TransactionInfo tri = changes.pop();
		tri.tr.rollback(false);
	}

	/*
	 * Get window's top undo transaction editor
	 */
	public IEditor getCurrentUndoEditor() {
		if (changes.isEmpty())
			return null;
		return changes.peek().editor;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#openEditor(kiev.vlang.FileUnit)
	 */
	public final IEditor openEditor(FileUnit fu) {
		return openEditor(fu, new INode[0]);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#closeEditor(kiev.gui.Editor)
	 */
	public void closeEditor(IEditor ed) {
		for (TransactionInfo tri : changes) {
			if (tri.editor == ed)
				tri.editor = null;
		}
	}
}
