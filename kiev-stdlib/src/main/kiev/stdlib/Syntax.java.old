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

syntax Syntax {

import kiev.stdlib.*;
import kiev.stdlib.meta.*;
import java.lang.*;

// array type
operator "T []", type;
typedef elem[] kiev.stdlib._array_<elem>;		// creates an array type
operator "T ...", type;
typedef elem... kiev.stdlib._vararg_<elem>;		// creates an vararg array type

// wrapper type
operator "T ⊛", type;
typedef wtp⊛ kiev.stdlib._wrapper_<wtp>;

// Reference wrapper
operator "T &", type;
typedef rtp& kiev.stdlib.Ref<rtp>;

// PVar wrapper
operator "T @", type;
typedef pvtp@ kiev.stdlib.PVar<pvtp>⊛;

// AST type (for compiler nodes); handled by compiler internally
operator "T #", type;

// Wildcard types
operator "T ⁺", type;
//typedef wctp⁺ kiev.stdlib._wildcard_co_variant_<wctp>;
operator "T ⁻", type;
//typedef wctp⁻ kiev.stdlib._wildcard_contra_variant_<wctp>;

// assign operators
operator  "X = Y"   ,   5;
operator  "X |= Y"  ,   5;
operator  "X &= Y"  ,   5;
operator  "X ^= Y"  ,   5;
operator  "X <<= Y" ,   5;
operator  "X >>= Y" ,   5;
operator  "X >>>= Y",   5;
operator  "X += Y"  ,   5;
operator  "X -= Y"  ,   5;
operator  "X *= Y"  ,   5;
operator  "X /= Y"  ,   5;
operator  "X %= Y"  ,   5;

// bind/iterate rule operators
operator "X ?= X"   ,   5;
operator "X @= X"   ,   5;

// infix operators
operator  "Y || X"  ,  10;
operator  "Y && X"  ,  20;
operator  "Y | X"   ,  30;
operator  "Y ^ X"   ,  40;
operator  "Y & X"   ,  50;
operator  "X == X"  ,  60;
operator  "X != X"  ,  60;
operator  "X >= X"  ,  80;
operator  "X <= X"  ,  80;
operator  "X > X"   ,  80;
operator  "X < X"   ,  80;
operator  "X << X"  ,  90;
operator  "X >> X"  ,  90;
operator  "X >>> X" ,  90;
operator  "Y + X"   , 100;
operator  "Y - X"   , 100;
operator  "Y * X"   , 150;
operator  "Y / X"   , 150;
operator  "Y % X"   , 150;

// prefix operators
operator  "+ Y"     , 200;
operator  "- Y"     , 200;
operator  "++ X"    , 210;
operator  "-- X"    , 210;
operator  "~ Y"     , 210;
operator  "! Y"     , 210;

// postfix operators
operator  "X ++"    , 210;
operator  "X --"    , 210;

// parenthethis operators
operator  "( Z )"   , 255;

// type operators
operator  "X instanceof T",    70;
operator  "( T ) Y",           180;
operator  "( $cast T ) Y",     180;
operator  "( $reinterp T ) Y", 180;
operator  "T . class",         240;

import static kiev.stdlib.#id"any"#._instanceof_(any, any);
//import static kiev.stdlib.#id"boolean"#.#id"false"#;
//import static kiev.stdlib.#id"boolean"#.#id"true"#;
//import static kiev.stdlib.#id"null"#.#id"null"#;
import static kiev.stdlib.GString.str_concat_ss(String, String); // string concatenation String + String
}

