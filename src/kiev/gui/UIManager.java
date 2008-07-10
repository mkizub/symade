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
package kiev.gui;

public class UIManager {

	public static ICanvas newCanvas(){
		return new kiev.gui.swing.Canvas();
	}
	
	public static IWindow newWindow(){
		return new kiev.gui.swing.Window();
	}
	
}
