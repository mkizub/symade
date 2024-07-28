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
package kiev.dump.xml;

import kiev.dump.Convertor;
import kiev.dump.MarshallingContext;
import kiev.vlang.ATextNode;

public class DataConvertor implements Convertor {
    public boolean canConvert(Object data) {
		return true;
	}

    public String convert(Object data, MarshallingContext context) {
		if (data instanceof ATextNode)
			return ((ATextNode)data).toText();
		else
			return String.valueOf(data);
	}
}
