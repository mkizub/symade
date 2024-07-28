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

public interface MarshallingContext {
	
	public Env getEnv();

	/**
	 * Adds new marshaller to this context
	 * @param m marshaler to add
	 * @return self
	 */
	public MarshallingContext add(Marshaller m);

	/**
	 * Adds new data converter to this context
	 * @param c converter to add
	 * @return self
	 */
	public MarshallingContext add(Convertor c);
	
	/**
	 * Marshal another object searching for the default marshaller
	 * @param data the next item to convert
	 */
    public void marshalData(Object data);
    
    /**
     * Marshal another object using the specified marshaller
     * @param data       the next item to convert
     * @param marshaller the marshaller to use
     */
    public void marshalData(Object data, Marshaller marshaller);

	/**
	 * Convert (attribute) data into String
	 * @param data the item to convert
	 */
    public String convertData(Object data);

	/**
	 * Write attribute searching for the default string convertor
	 * @param attr the attribute name
	 * @param data the item to convert
	 */
    public void attributeData(QName attr, Object data);

    /**
     * Write attribute using the specified convertor
	 * @param attr the attribute name
     * @param data       the next item to convert
     * @param convertor  the convertor to use
     */
    public void attributeData(QName attr, Object data, Convertor convertor);

}

