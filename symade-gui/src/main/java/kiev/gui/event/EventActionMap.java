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
package kiev.gui.event;

import java.util.Hashtable;
import java.util.LinkedList;

import kiev.gui.UIActionFactory;

/**
 * The Event Action Map.
 */
public final class EventActionMap {
	
	/**
	 * The map of input events to action factories.
	 */
	private final Hashtable<InputEvent,UIActionFactory[]> naviMap;
	
	/**
	 * The constructor.
	 */
	public EventActionMap() {
		naviMap = new Hashtable<InputEvent,UIActionFactory[]>();
	}

	/**
	 * Add mapping.
	 * @param evt the input event
	 * @param factory the action factory
	 */
	public void add(InputEvent evt, UIActionFactory factory) {
		if (evt == null || factory == null) return;
		UIActionFactory[] actions = naviMap.get(evt);
		if (actions != null) {
			UIActionFactory[] tmp = new UIActionFactory[actions.length+1];
			for (int i=0; i < actions.length; i++) {
				if (actions[i].getClass() == factory.getClass()) return; // duplicate action
				tmp[i] = actions[i];
			}
			tmp[actions.length] = factory;
			naviMap.put(evt, tmp);
		} else {
			naviMap.put(evt, new UIActionFactory[]{factory});
		}
	}
	
	/**
	 * Removes input event from map.
	 * @param evt the event
	 */
	public void clear(InputEvent evt) {
		if (evt == null) return;
		naviMap.remove(evt);
	}

	/**
	 * Removes all input events from map.
	 */
	public void clearAll() {
		naviMap.clear();
	}
	
	/**
	 * Get All Actions excluding duplicates. 
	 * @return UIActionFactory[]
	 */
	public UIActionFactory[] getAllActions() {
		LinkedList<UIActionFactory> aflist = new LinkedList<UIActionFactory>();
		for (UIActionFactory[] actions: naviMap.values()) 
			for (UIActionFactory af: actions) {
				boolean contains = false;
				for (UIActionFactory uf: aflist) 
					if (af == uf || af.getClass() == uf.getClass()) {
						contains = true; break;
					}					
				if (! contains) aflist.add(af);
			}
		return aflist.toArray(new UIActionFactory[aflist.size()]);
	}

	/**
	 * Get factories for event.
	 * @param evt the event
	 * @return UIActionFactory[]
	 */
	public UIActionFactory[] get(InputEvent evt) {
		if (evt == null) return null;
		return naviMap.get(evt);
	}
}
