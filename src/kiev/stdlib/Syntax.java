package kiev.stdlib;

syntax Syntax {

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

