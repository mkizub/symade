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
package kiev.gui.event;

import java.util.ArrayList;
import java.util.Hashtable;

import kiev.gui.UIActionFactory;

public final class EventActionMap {
	
	private final Hashtable<InputEvent,UIActionFactory[]> naviMap;
	
	public EventActionMap() {
		naviMap = new Hashtable<InputEvent,UIActionFactory[]>();
	}

	public void add(InputEventInfo evt, UIActionFactory factory) {
		if (evt == null || factory == null)
			return;
		UIActionFactory[] actions = naviMap.get(evt);
		if (actions != null) {
			UIActionFactory[] tmp = new UIActionFactory[actions.length+1];
			for (int i=0; i < actions.length; i++) {
				if (actions[i].getClass() == factory.getClass())
					return; // duplicate action
				tmp[i] = actions[i];
			}
			tmp[actions.length] = factory;
			naviMap.put(evt, tmp);
		} else {
			naviMap.put(evt, new UIActionFactory[]{factory});
		}
	}
	
	public void clear(InputEvent evt) {
		if (evt == null)
			return;
		naviMap.remove(evt);
	}

	public void clearAll() {
		naviMap.clear();
	}
	
	public UIActionFactory[] getAllActions() {
		ArrayList<UIActionFactory> aflist = new ArrayList<UIActionFactory>();
		for (UIActionFactory[] actions: naviMap.values()) {
			for (UIActionFactory af: actions) {
				if (!aflist.contains(af))
					aflist.add(af);
			}
		}
		return aflist.toArray(new UIActionFactory[aflist.size()]);
	}

	public UIActionFactory[] get(InputEvent evt) {
		if (evt == null)
			return null;
		return naviMap.get(evt);
	}
}
