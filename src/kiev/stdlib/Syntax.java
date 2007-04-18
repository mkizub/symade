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

// infix operators
operator  ||         , yfx,  10;
operator  &&         , yfx,  20;
operator  |          , yfx,  30;
operator  ^          , yfx,  40;
operator  &          , yfx,  50;
operator  ==         , xfx,  60;
operator  !=         , xfx,  60;
operator  instanceof , xfx,  70;
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

//operator  ?=         , xfx, 5;
//operator  @=         , xfx, 5;

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

}

