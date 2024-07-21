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

import kiev.vlang.Env;

public interface UnMarshallingContext {
	
	public Object getResult();
	
	public Object peekNode();
	public QName  peekAttr();
	
	/**
	 * Write attribute searching for the default string convertor
	 * @param attr the attribute name
	 * @param data the item to convert
	 */
	//public Object decodeData();
    
    /**
     * Write attribute using the specified convertor
	 * @param attr the attribute name
     * @param data       the next item to convert
     * @param convertor  the convertor to use
     */
	//public Object decodeData(Decoder decoder);

}

