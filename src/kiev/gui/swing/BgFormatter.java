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
package kiev.gui.swing;

import kiev.gui.IBgFormatter;
import kiev.gui.UIView;

public class BgFormatter extends Thread implements IBgFormatter {
	private boolean do_format;
	private UIView view;
	public BgFormatter(UIView view) {
		this.view = view;
		this.setDaemon(true);
		this.setPriority(Thread.NORM_PRIORITY - 1);
	}
	@Override
	public void run() {
		for (;;) {
			while (!do_format) {
				synchronized(this) { try {
					this.wait();
				} catch (InterruptedException e) {}
				}
				continue;
			}
			this.do_format = false;
			view.formatAndPaint(true);
		}
	}
	public synchronized void schedule_run() {
		this.do_format = true;
		this.notify();
	}
}

