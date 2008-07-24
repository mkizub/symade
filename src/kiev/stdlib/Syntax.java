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

typedef elem[] kiev.stdlib._array_<elem>;
typedef elem... kiev.stdlib._vararg_<elem>;
typedef wtp⊛ kiev.stdlib._wrapper_<wtp>;
//typedef wctp⁺ kiev.stdlib._wildcard_co_variant_<wctp>;
//typedef wctp⁻ kiev.stdlib._wildcard_contra_variant_<wctp>;
typedef pvtp@ kiev.stdlib.PVar<pvtp>⊛;
typedef rtp& kiev.stdlib.Ref<rtp>;
//typedef elem| kiev.stdlib.List<elem>;

// assign operators
operator  =    , lfy,   5;
operator  :=   , lfy,   5;
operator  |=   , lfy,   5;
operator  &=   , lfy,   5;
operator  ^=   , lfy,   5;
operator  <<=  , lfy,   5;
operator  >>=  , lfy,   5;
operator  >>>= , lfy,   5;
operator  +=   , lfy,   5;
operator  -=   , lfy,   5;
operator  *=   , lfy,   5;
operator  /=   , lfy,   5;
operator  %=   , lfy,   5;

// bind/iterate rule operators
operator "X ?= X" , 5;
operator "X @= X" , 5;

// infix operators
operator  ||         , yfx,  10;
operator  &&         , yfx,  20;
operator  |          , yfx,  30;
operator  ^          , yfx,  40;
operator  &          , yfx,  50;
operator  ==         , xfx,  60;
operator  !=         , xfx,  60;
operator  >=         , xfx,  80;
operator  <=         , xfx,  80;
operator  >          , xfx,  80;
operator  <          , xfx,  80;
operator  <<         , xfx,  90;
operator  >>         , xfx,  90;
operator  >>>        , xfx,  90;
operator  +          , yfx, 100;
operator  -          , yfx, 100;
operator  *          , yfx, 150;
operator  /          , yfx, 150;
operator  %          , yfx, 150;

// prefix operators
operator  +  ,  fy, 200;
operator  -  ,  fy, 200;
operator  ++ ,  fx, 210; // fl
operator  -- ,  fx, 210; // fl
operator  ~  ,  fy, 210;
operator  !  ,  fy, 210;

// postfix operators
operator  ++ ,  xf, 210; // lf
operator  -- ,  xf, 210; // lf

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

