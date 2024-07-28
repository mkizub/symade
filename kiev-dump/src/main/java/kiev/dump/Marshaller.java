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



public interface Marshaller {
    /**
     * Determines whether the converter can marshall a particular object.
     * @param data The object to be marshalled.
     */
    public boolean canMarshal(Object data, MarshallingContext context);

    /**
     * Marshal an object to XML data.
     *
     * @param data    The object to be marshalled.
     * @param writer  A stream to write to.
     * @param context A context that allows nested objects to be processed.
     */
    public void marshal(Object data, DumpWriter writer, MarshallingContext context) throws Exception;

}

