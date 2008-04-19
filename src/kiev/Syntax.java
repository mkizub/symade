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
import static kiev.stdlib.Debug.*;

typedef type∅ kiev.vlang.NodeSpace<type>;

operator ≡ , xfx, 60;	// == equiv
operator ≢ , xfx, 60;	// !=
operator ≈ , xfx, 60;	// approx aeq
operator ≉ , xfx, 60;	// naeq
operator ≅ , xfx, 60;	// cong
operator ≃ , xfx, 60;	// simeq
operator ≥ , xfx, 60;	// geq >=
operator ≤ , xfx, 60;	// leq
operator ≺ , xfx, 60;	// prec
operator ≼ , xfx, 60;	// preceq
}

