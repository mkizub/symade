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
package kiev.fmt.evt;
import syntax kiev.Syntax;


import java.io.Serializable;
import java.io.ObjectStreamException;


public class Compiled_BindingSet implements Serializable {

	public Compiled_BindingSet				parent_set;
	public Compiled_BindingSet[]			sub_set;
	public String						q_name;	// qualified name

	
	Object readResolve() throws ObjectStreamException {
		if (this.q_name != null) this.q_name = this.q_name.intern();
		this.init();
		return this;
	}

	public Compiled_BindingSet init() {
		return this;
	}
}

public final class Compiled_Binding implements Serializable {
	public UIEvent[] events = UIEvent.emptyArray;
	public String action;

	Object readResolve() throws ObjectStreamException {
		if (this.action != null) this.action = this.action.intern();
		return this;
	}
}

public final class Compiled_Action implements Serializable {
	public String description;
	public boolean isForPopupMenu;
	public String actionClass;

	Object readResolve() throws ObjectStreamException {
		if (this.description != null) this.description = this.description.intern();
		if (this.actionClass != null) this.actionClass = this.actionClass.intern();
		return this;
	}
}

public final class Compiled_KeyboardEvent implements Serializable {
	public int keyCode;
	public boolean withCtrl;
	public boolean withAlt;
	public boolean withShift;

	Object readResolve() throws ObjectStreamException {
		return this;
	}
}
