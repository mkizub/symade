/* Generated By:JavaCC: Do not edit this line. kiev040Constants.java */
package kiev.parser;

public interface kiev040Constants {

  int EOF = 0;
  int SINGLE_LINE_COMMENT = 11;
  int FORMAL_COMMENT = 12;
  int MULTI_LINE_COMMENT = 13;
  int ABSTRACT = 15;
  int ANY = 16;
  int BOOLEAN = 17;
  int BREAK = 18;
  int BYTE = 19;
  int CASE = 20;
  int CATCH = 21;
  int CHAR = 22;
  int CLASS = 23;
  int CONTINUE = 24;
  int _DEFAULT = 25;
  int DO = 26;
  int DOUBLE = 27;
  int ELSE = 28;
  int EXTENDS = 29;
  int FALSE = 30;
  int FINAL = 31;
  int FINALLY = 32;
  int FLOAT = 33;
  int FOR = 34;
  int FOREACH = 35;
  int GOTO = 36;
  int IF = 37;
  int IMPLEMENTS = 38;
  int IMPORT = 39;
  int INT = 40;
  int INTERFACE = 41;
  int METATYPE = 42;
  int LONG = 43;
  int NATIVE = 44;
  int NEW = 45;
  int NULL = 46;
  int PACKAGE = 47;
  int RETURN = 48;
  int SHORT = 49;
  int STATIC = 50;
  int SWITCH = 51;
  int SYNCHRONIZED = 52;
  int WITH = 53;
  int THROW = 54;
  int THROWS = 55;
  int TRANSIENT = 56;
  int TRUE = 57;
  int TRY = 58;
  int VOID = 59;
  int VOLATILE = 60;
  int VIEW = 61;
  int WHILE = 62;
  int ARROW = 63;
  int FUNCTION = 64;
  int VARARGS = 65;
  int RULE = 66;
  int PCUT = 67;
  int CAST = 68;
  int REINTERP = 69;
  int ALIAS = 70;
  int OPERATOR_ID = 71;
  int TYPEDEF = 72;
  int ENUM = 73;
  int REQUIRE = 74;
  int ENSURE = 75;
  int INVARIANT = 76;
  int META_INTERFACE = 77;
  int META_SINGLETON = 78;
  int META_FORWARD = 79;
  int META_UNERASABLE = 80;
  int META_VIRTUAL = 81;
  int META_PACKED = 82;
  int META_MACRO = 83;
  int META_STATIC = 84;
  int META_ABSTRACT = 85;
  int META_FINAL = 86;
  int META_NATIVE = 87;
  int META_SYNCHRONIZED = 88;
  int META_TRANSIENT = 89;
  int META_VOLATILE = 90;
  int META_THROWS = 91;
  int META_ACCESS1 = 92;
  int META_PUBLIC = 93;
  int PUBLIC = 94;
  int META_PROTECTED = 95;
  int PROTECTED = 96;
  int META_PRIVATE = 97;
  int PRIVATE = 98;
  int READ_ONLY = 99;
  int WRITE_ONLY = 100;
  int READ_WRITE = 101;
  int NO_READ_WRITE = 102;
  int COMMA1 = 103;
  int COLON1 = 104;
  int OPEN_ACCESS = 105;
  int CLOSE_ACCESS = 106;
  int PRAGMA = 112;
  int PRAGMA_ENABLE = 113;
  int PRAGMA_DISABLE = 114;
  int INTEGER_LITERAL = 115;
  int LONG_INTEGER_LITERAL = 116;
  int DECIMAL_LITERAL = 117;
  int HEX_LITERAL = 118;
  int OCTAL_LITERAL = 119;
  int FLOATING_POINT_LITERAL = 120;
  int DOUBLE_POINT_LITERAL = 121;
  int EXPONENT = 122;
  int REPARSE_EXPRESSION = 123;
  int REPARSE_STATEMENT = 124;
  int REPARSE_TYPE = 125;
  int CHARACTER_LITERAL = 126;
  int STRING_LITERAL = 127;
  int IDENTIFIER = 128;
  int LETTER = 129;
  int DIGIT = 130;
  int ID_STRING_LITERAL = 131;
  int LPAREN = 132;
  int RPAREN = 133;
  int LBRACE = 134;
  int RBRACE = 135;
  int LBRACKET = 136;
  int RBRACKET = 137;
  int SEMICOLON = 138;
  int COMMA = 139;
  int DOT = 140;
  int COLON = 141;
  int QUESTION = 142;
  int LT = 143;
  int GT = 144;
  int ASSIGN = 145;
  int ASSIGN2 = 146;
  int BANG = 147;
  int TILDE = 148;
  int EQ = 149;
  int LE = 150;
  int GE = 151;
  int NE = 152;
  int SC_OR = 153;
  int SC_AND = 154;
  int INCR = 155;
  int DECR = 156;
  int PLUS = 157;
  int MINUS = 158;
  int STAR = 159;
  int SLASH = 160;
  int BIT_AND = 161;
  int BIT_OR = 162;
  int XOR = 163;
  int REM = 164;
  int LSHIFT = 165;
  int PLUSASSIGN = 166;
  int MINUSASSIGN = 167;
  int STARASSIGN = 168;
  int SLASHASSIGN = 169;
  int ANDASSIGN = 170;
  int ORASSIGN = 171;
  int XORASSIGN = 172;
  int REMASSIGN = 173;
  int LSHIFTASSIGN = 174;
  int RSIGNEDSHIFTASSIGN = 175;
  int RUNSIGNEDSHIFTASSIGN = 176;
  int IS_THE = 177;
  int IS_ONE_OF = 178;
  int OPERATOR_AT = 179;
  int OPERATOR_SHARP = 180;
  int OPERATOR_LRBRACKETS = 181;
  int OPERATOR_UPPER_BOUND = 182;
  int OPERATOR_LOWER_BOUND = 183;
  int OPERATOR = 184;

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
    "\"any\"",
    "\"boolean\"",
    "\"break\"",
    "\"byte\"",
    "\"case\"",
    "\"catch\"",
    "\"char\"",
    "\"class\"",
    "\"continue\"",
    "\"default\"",
    "\"do\"",
    "\"double\"",
    "\"else\"",
    "\"extends\"",
    "\"false\"",
    "\"final\"",
    "\"finally\"",
    "\"float\"",
    "\"for\"",
    "\"foreach\"",
    "\"goto\"",
    "\"if\"",
    "\"implements\"",
    "\"import\"",
    "\"int\"",
    "\"interface\"",
    "\"metatype\"",
    "\"long\"",
    "\"native\"",
    "\"new\"",
    "\"null\"",
    "\"package\"",
    "\"return\"",
    "\"short\"",
    "\"static\"",
    "\"switch\"",
    "\"synchronized\"",
    "\"with\"",
    "\"throw\"",
    "\"throws\"",
    "\"transient\"",
    "\"true\"",
    "\"try\"",
    "\"void\"",
    "\"volatile\"",
    "\"view\"",
    "\"while\"",
    "\"->\"",
    "\"fun\"",
    "\"...\"",
    "\"rule\"",
    "\"$cut\"",
    "\"$cast\"",
    "\"$reinterp\"",
    "\"alias\"",
    "\"operator\"",
    "\"typedef\"",
    "\"enum\"",
    "\"require\"",
    "\"ensure\"",
    "\"invariant\"",
    "\"@interface\"",
    "\"@singleton\"",
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
    "<INTEGER_LITERAL>",
    "<LONG_INTEGER_LITERAL>",
    "<DECIMAL_LITERAL>",
    "<HEX_LITERAL>",
    "<OCTAL_LITERAL>",
    "<FLOATING_POINT_LITERAL>",
    "<DOUBLE_POINT_LITERAL>",
    "<EXPONENT>",
    "<REPARSE_EXPRESSION>",
    "<REPARSE_STATEMENT>",
    "<REPARSE_TYPE>",
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
    "\",\"",
    "\".\"",
    "\":\"",
    "\"?\"",
    "\"<\"",
    "\">\"",
    "\"=\"",
    "\":=\"",
    "\"!\"",
    "\"~\"",
    "\"==\"",
    "\"<=\"",
    "\">=\"",
    "\"!=\"",
    "\"||\"",
    "\"&&\"",
    "\"++\"",
    "\"--\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"&\"",
    "\"|\"",
    "\"^\"",
    "\"%\"",
    "\"<<\"",
    "\"+=\"",
    "\"-=\"",
    "\"*=\"",
    "\"/=\"",
    "\"&=\"",
    "\"|=\"",
    "\"^=\"",
    "\"%=\"",
    "\"<<=\"",
    "\">>=\"",
    "\">>>=\"",
    "\"?=\"",
    "\"@=\"",
    "\"@\"",
    "\"#\"",
    "<OPERATOR_LRBRACKETS>",
    "\"\\u2264\"",
    "\"\\u2265\"",
    "<OPERATOR>",
  };

}
