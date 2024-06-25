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
package kiev.dump;



public interface Convertor {
    /**
     * Determines whether the converter can convert to String a particular type.
     * @param data The object to be converted.
     */
    public boolean canConvert(Object data);

    /**
     * Convert an object to textual data.
     *
     * @param data    The object to be marshalled.
     * @param context A context that provides additional information.
     */
    public String convert(Object data, MarshallingContext context);

}

