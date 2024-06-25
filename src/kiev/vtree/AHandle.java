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
package kiev.vtree;

import syntax kiev.Syntax;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

// Base context class.
// Holds only flags about it's lifecycle.
public class Context {
	public static final Context DEFAULT = new Context();
	
	// a flag that says, that data with this context is expired and can be deleted from AHandle
	public boolean expired;
}

public abstract class AHandleData {
	private final Context context;
	
	public AHandleData(Context context) {
		this.context = context;
	}
	
	public final Context getDataContext() {
		return this.context;
	}
}

public final class AHandle {
	private static final AtomicLong ID_FACTORY = new AtomicLong();
	private static final AHandleData[] emptyHandles = new AHandleData[0];

	// Node's unique ID
	final long              h_uid;
	
	// List of nodes for this handle
	AHandleData[]			h_nodes;

	public AHandle() {
		h_uid = ID_FACTORY.incrementAndGet();
		h_nodes = emptyHandles;
	}
	
	public synchronized void addData(AHandleData data) {
		AHandleData[] h_nodes = this.h_nodes;
		if (h_nodes == null || h_nodes.length == 0) {
			this.h_nodes = new AHandleData[]{data};
			return;
		}
		AHandleData[] tmp = new AHandleData[h_nodes.length+1];
		tmp[0] = data;
		System.arraycopy(h_nodes,0,tmp,1,h_nodes.length);
		this.h_nodes = tmp;
	}

	public synchronized void delData(AHandleData data) {
		AHandleData[] h_nodes = this.h_nodes;
		for (int idx=0; idx < h_nodes.length; idx++) {
			AHandleData nh = h_nodes[idx];
			if (nh != data)
				continue;
			if (h_nodes.length == 1) {
				this.h_nodes = emptyHandles;
				return;
			}
			AHandleData[] tmp = new AHandleData[h_nodes.length-1];
			int i=0;
			for (; i < idx; i++)
				tmp[i] = h_nodes[i];
			for (; i < tmp.length; i++)
				tmp[i] = h_nodes[i+1];
			this.h_nodes = tmp;
			return;
		}
	}

	public AHandleData[] getHandleData() {
		return h_nodes;
	}
}


