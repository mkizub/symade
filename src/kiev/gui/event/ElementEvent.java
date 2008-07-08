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

import java.util.EventObject;



public class ElementEvent extends EventObject {

  protected int id;
  /**
   * The first number in the range of ids used for element events.
   */
  public static final int ELEMENT_FIRST		= 1000;

  /**
   * The last number in the range of ids used for element events.
   */
  public static final int ELEMENT_LAST		= 1000;

  /**
   * This event indicates that the element's changed.
   */
  public static final int ELEMENT_CHANGED	= ELEMENT_FIRST;
  
	public ElementEvent(Object source, int type) {
		super(source);
		this.id = id;
	}
	
  /**
   * Returns the event type.
   */
  public int getID() {
      return id;
  }

}
