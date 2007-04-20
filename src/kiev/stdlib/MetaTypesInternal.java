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
package kiev.stdlib;

import kiev.vlang.NewExpr;
import kiev.vlang.CallExpr;
import syntax kiev.stdlib.Syntax;


// UUID name-based generated as:
// URL is http://www.symade.com/ and UUID is generated as:
// java -jar jug-lgpl-2.0.0.jar --name http://www.symade.com/ --namespace URL name-based => 6189bf45-88e8-3a27-8ebf-0a14795e29a7
// UUID for names in this package were geberated using this UUID as a base, for instance:
// java -jar jug-lgpl-2.0.0.jar --name kiev.stdlib.any --namespace 6189bf45-88e8-3a27-8ebf-0a14795e29a7 name-based

/**
 * @author Maxim Kizub
 * @version $Revision: 4 $
 *
 */

@uuid("bbf03b4b-62d4-3e29-8f0d-acd6c47b9a04")
public metatype _array_< @uuid("74843bf1-3c28-374b-ad11-006af8a31a71") _elem_ extends any > extends Object {
	@macro @native
	public:ro final int length;

	@macro @native
	public _elem_ get(int idx) alias operator "V [ V ]" ;
}

@uuid("67544053-836d-3bac-b94d-0c4b14ae9c55")
public metatype _wrapper_< @uuid("400f213e-a4bb-3ee2-b870-9ec1951fd955") _boxed_ extends Object > extends Object {
}

@uuid("25395a72-2b16-317a-85b2-5490309bdffc")
public metatype _call_type_ extends any {
}

@uuid("8aa32751-ac53-343e-b456-6f8521b01647")
public metatype _vararg_< @uuid("924f219a-37cf-3654-b761-7cb5e26ceef0") _elem_ extends Object > extends _array_<_elem_> {
}

@uuid("3e32f9c7-9846-393e-8c6e-11512191ec94")
public metatype _astnode_< @uuid("f23d4ec5-7fc2-3bbb-9b8f-46a309fc5f24") _node_ extends Object > extends any {
}

