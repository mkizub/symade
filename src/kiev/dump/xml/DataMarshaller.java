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

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.ATextNode;

public class DataMarshaller implements Marshaller {
    public boolean canMarshal(Object data, MarshallingContext context) {
		return true;
	}

    public void marshal(Object data, DumpWriter _out, MarshallingContext context) throws Exception {
    	XMLDumpWriter out = (XMLDumpWriter)_out;
		if (data instanceof ATextNode)
			out.addText(((ATextNode)data).toText());
		else
			out.addText(String.valueOf(data));
	}
}

