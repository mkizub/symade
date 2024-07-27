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
package kiev;

syntax Syntax extends kiev.stdlib.Syntax {

import kiev.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.vtree.*;
import kiev.transf.*;
import kiev.parser.*;
import static kiev.stdlib.Asserts.*;
import static kiev.stdlib.Traces.*;

operator "T ∅", type;
operator "T ⋈", type;
operator "T ⇑", type;

typedef elem∅	kiev.vtree.NodeSpace<elem>;
typedef elem⋈	kiev.vtree.NodeExtSpace<elem>;
typedef elem⇑	kiev.vtree.NodeSymbolRef<elem>;

operator "X ≡ X", 60;	// == equiv
operator "X ≢ X", 60;	// !=
operator "X ≈ X", 60;	// approx aeq
operator "X ≉ X", 60;	// naeq
operator "X ≅ X", 60;	// cong
operator "X ≃ X", 60;	// simeq
operator "X ≥ X", 60;	// geq >=
operator "X ≤ X", 60;	// leq
operator "X ≺ X", 60;	// prec
operator "X ≼ X", 60;	// preceq
}

