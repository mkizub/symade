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

import java.util.EventListener;

/**
 * Element Change Listener should implement classes that 
 * listen on element selection event in the view.
 */
public interface ElementChangeListener extends EventListener {

  /**
   * Constitutes element change event.
   * @param e the element event
   */
  public void elementChanged(ElementEvent e);

}
