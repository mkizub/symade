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

import javax.xml.namespace.QName;


public interface UnMarshaller {
    /**
     * Determines whether the converter can unmarshall.
	 *
     * @param name The XML name of the found element.
     * @param attrs The XML attributes of the found element.
     * @param context The context.
     */
    public boolean canUnMarshal(QName name, AttributeSet attrs, UnMarshallingContext context);

    /**
     * Determines whether the current unmarshaller can accept object's element.
	 *
     * @param name The XML name of the found element.
     * @param attrs The XML attributes of the found element.
     * @param context The context.
     */
    public AcceptInfo canAccept(Object self, QName name, AttributeSet attrs, UnMarshallingContext context);

    /**
     * Create unmarshalled object.
     *
     * @param name The XML name of the found element.
     * @param attrs The XML attributes of the found element.
     * @param context The context.
     */
    public Object create(QName name, AttributeSet attrs, UnMarshallingContext context);

    /**
     * Accept sub-element.
     *
     * @param name The XML name of the found element.
     * @param context The context.
     */
    public void accept(Object self, QName name, Object target, UnMarshallingContext context);

    /**
     * End of object, can return itself or a replaced object.
     *
     * @param name The XML name of the found element.
     * @param attrs The XML attributes of the found element.
     * @param context The context.
     */
    public Object exit(Object self, UnMarshallingContext context);

}

