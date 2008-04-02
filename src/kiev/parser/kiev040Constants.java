/* Generated By:JavaCC: Do not edit this line. kiev040Constants.java */
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
package kiev.parser;

public interface kiev040Constants {

  int EOF = 0;
  int SINGLE_LINE_COMMENT = 11;
  int FORMAL_COMMENT = 12;
  int MULTI_LINE_COMMENT = 13;
  int ABSTRACT = 15;
  int BREAK = 16;
  int CASE = 17;
  int CATCH = 18;
  int CLASS = 19;
  int CONTINUE = 20;
  int _DEFAULT = 21;
  int DO = 22;
  int ELSE = 23;
  int EXTENDS = 24;
  int FINAL = 25;
  int FINALLY = 26;
  int FOR = 27;
  int FOREACH = 28;
  int GOTO = 29;
  int IF = 30;
  int IF_REWR = 31;
  int IMPLEMENTS = 32;
  int IMPORT = 33;
  int INTERFACE = 34;
  int METATYPE = 35;
  int NATIVE = 36;
  int PACKAGE = 37;
  int RETURN = 38;
  int STATIC = 39;
  int SWITCH = 40;
  int SYNCHRONIZED = 41;
  int WITH = 42;
  int THROW = 43;
  int THROWS = 44;
  int TRANSIENT = 45;
  int TRY = 46;
  int VOLATILE = 47;
  int VIEW = 48;
  int WHILE = 49;
  int VARARGS = 50;
  int PCUT = 51;
  int ALIAS = 52;
  int TYPEDEF = 53;
  int ENUM = 54;
  int REQUIRE = 55;
  int ENSURE = 56;
  int INVARIANT = 57;
  int META_INTERFACE = 58;
  int META_SINGLETON = 59;
  int META_MIXIN = 60;
  int META_FORWARD = 61;
  int META_UNERASABLE = 62;
  int META_VIRTUAL = 63;
  int META_PACKED = 64;
  int META_MACRO = 65;
  int META_STATIC = 66;
  int META_ABSTRACT = 67;
  int META_FINAL = 68;
  int META_NATIVE = 69;
  int META_SYNCHRONIZED = 70;
  int META_TRANSIENT = 71;
  int META_VOLATILE = 72;
  int META_THROWS = 73;
  int META_UUID = 74;
  int META_ACCESS1 = 75;
  int META_PUBLIC = 76;
  int PUBLIC = 77;
  int META_PROTECTED = 78;
  int PROTECTED = 79;
  int META_PRIVATE = 80;
  int PRIVATE = 81;
  int READ_ONLY = 82;
  int WRITE_ONLY = 83;
  int READ_WRITE = 84;
  int NO_READ_WRITE = 85;
  int COMMA1 = 86;
  int COLON1 = 87;
  int OPEN_ACCESS = 88;
  int CLOSE_ACCESS = 89;
  int PRAGMA = 95;
  int PRAGMA_ENABLE = 96;
  int PRAGMA_DISABLE = 97;
  int FUNCTION = 98;
  int FALSE = 99;
  int NEW = 100;
  int NULL = 101;
  int TRUE = 102;
  int OPERATOR_ID = 103;
  int INTEGER_LITERAL = 104;
  int LONG_INTEGER_LITERAL = 105;
  int DECIMAL_LITERAL = 106;
  int HEX_LITERAL = 107;
  int OCTAL_LITERAL = 108;
  int FLOATING_POINT_LITERAL = 109;
  int DOUBLE_POINT_LITERAL = 110;
  int EXPONENT = 111;
  int CHARACTER_LITERAL = 112;
  int STRING_LITERAL = 113;
  int IDENTIFIER = 114;
  int LETTER = 115;
  int DIGIT = 116;
  int ID_STRING_LITERAL = 117;
  int LPAREN = 118;
  int RPAREN = 119;
  int LBRACE = 120;
  int RBRACE = 121;
  int LBRACKET = 122;
  int RBRACKET = 123;
  int SEMICOLON = 124;
  int COLON = 125;
  int DOT = 126;
  int COMMA = 127;
  int LT = 128;
  int GT = 129;
  int LANGLE = 130;
  int RANGLE = 131;
  int ASSIGN = 132;
  int ASSIGN2 = 133;
  int IS_THE = 134;
  int IS_ONE_OF = 135;
  int ARROW = 136;
  int OPERATOR_AT = 137;
  int OPERATOR_SHARP = 138;
  int OPERATOR_LRBRACKETS = 139;
  int OPERATOR_UPPER_BOUND = 140;
  int OPERATOR_LOWER_BOUND = 141;
  int OPERATOR = 142;

  int DEFAULT = 0;
  int IN_PRAGMA = 1;
  int IN_SINGLE_LINE_COMMENT = 2;
  int IN_FORMAL_COMMENT = 3;
  int IN_MULTI_LINE_COMMENT = 4;
  int IN_ACCESS = 5;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "\"/*{\"",
    "\"}*/\"",
    "\"//\"",
    "<token of kind 9>",
    "\"/*\"",
    "<SINGLE_LINE_COMMENT>",
    "\"*/\"",
    "\"*/\"",
    "<token of kind 14>",
    "\"abstract\"",
    "\"break\"",
    "\"case\"",
    "\"catch\"",
    "\"class\"",
    "\"continue\"",
    "\"default\"",
    "\"do\"",
    "\"else\"",
    "\"extends\"",
    "\"final\"",
    "\"finally\"",
    "\"for\"",
    "\"foreach\"",
    "\"goto\"",
    "\"if\"",
    "\"if#\"",
    "\"implements\"",
    "\"import\"",
    "\"interface\"",
    "\"metatype\"",
    "\"native\"",
    "\"package\"",
    "\"return\"",
    "\"static\"",
    "\"switch\"",
    "\"synchronized\"",
    "\"with\"",
    "\"throw\"",
    "\"throws\"",
    "\"transient\"",
    "\"try\"",
    "\"volatile\"",
    "\"view\"",
    "\"while\"",
    "\"...\"",
    "\"$cut\"",
    "\"alias\"",
    "\"typedef\"",
    "\"enum\"",
    "\"require\"",
    "\"ensure\"",
    "\"invariant\"",
    "\"@interface\"",
    "\"@singleton\"",
    "\"@mixin\"",
    "\"@forward\"",
    "\"@unerasable\"",
    "\"@virtual\"",
    "\"@packed\"",
    "\"@macro\"",
    "\"@static\"",
    "\"@abstract\"",
    "\"@final\"",
    "\"@native\"",
    "\"@synchronized\"",
    "\"@transient\"",
    "\"@volatile\"",
    "\"@throws\"",
    "\"@uuid\"",
    "\"@access\"",
    "\"@public\"",
    "\"public\"",
    "\"@protected\"",
    "\"protected\"",
    "\"@private\"",
    "\"private\"",
    "<READ_ONLY>",
    "<WRITE_ONLY>",
    "\"rw\"",
    "<NO_READ_WRITE>",
    "\",\"",
    "\":\"",
    "\"(\"",
    "\")\"",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "\"pragma\"",
    "\"enable\"",
    "\"disable\"",
    "\"fun\"",
    "\"false\"",
    "\"new\"",
    "\"null\"",
    "\"true\"",
    "\"operator\"",
    "<INTEGER_LITERAL>",
    "<LONG_INTEGER_LITERAL>",
    "<DECIMAL_LITERAL>",
    "<HEX_LITERAL>",
    "<OCTAL_LITERAL>",
    "<FLOATING_POINT_LITERAL>",
    "<DOUBLE_POINT_LITERAL>",
    "<EXPONENT>",
    "<CHARACTER_LITERAL>",
    "<STRING_LITERAL>",
    "<IDENTIFIER>",
    "<LETTER>",
    "<DIGIT>",
    "<ID_STRING_LITERAL>",
    "\"(\"",
    "\")\"",
    "\"{\"",
    "\"}\"",
    "\"[\"",
    "\"]\"",
    "\";\"",
    "\":\"",
    "\".\"",
    "\",\"",
    "\"<\"",
    "\">\"",
    "\"<\\u0335\"",
    "\">\\u0335\"",
    "\"=\"",
    "\":=\"",
    "\"?=\"",
    "\"@=\"",
    "\"->\"",
    "\"@\"",
    "\"#\"",
    "<OPERATOR_LRBRACKETS>",
    "\"\\u2264\"",
    "\"\\u2265\"",
    "<OPERATOR>",
  };

}
