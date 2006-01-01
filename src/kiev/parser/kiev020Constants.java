/* Generated By:JJTree&JavaCC: Do not edit this line. kiev020Constants.java */
package kiev.parser;

public interface kiev020Constants {

  int EOF = 0;
  int SINGLE_LINE_COMMENT = 14;
  int FORMAL_COMMENT = 15;
  int MULTI_LINE_COMMENT = 16;
  int END_CONSTRAINT = 18;
  int ABSTRACT = 19;
  int BOOLEAN = 20;
  int BREAK = 21;
  int BYTE = 22;
  int CASE = 23;
  int CATCH = 24;
  int CHAR = 25;
  int CLASS = 26;
  int CONTINUE = 27;
  int _DEFAULT = 28;
  int DO = 29;
  int DOUBLE = 30;
  int ELSE = 31;
  int EXTENDS = 32;
  int FALSE = 33;
  int FINAL = 34;
  int FINALLY = 35;
  int FLOAT = 36;
  int FOR = 37;
  int FOREACH = 38;
  int GOTO = 39;
  int IF = 40;
  int IMPLEMENTS = 41;
  int IMPORT = 42;
  int INT = 43;
  int INTERFACE = 44;
  int LONG = 45;
  int NATIVE = 46;
  int NEW = 47;
  int NULL = 48;
  int PACKAGE = 49;
  int PRIVATE = 50;
  int PROTECTED = 51;
  int PUBLIC = 52;
  int RETURN = 53;
  int SHORT = 54;
  int STATIC = 55;
  int SWITCH = 56;
  int SYNCHRONIZED = 57;
  int WITH = 58;
  int THROW = 59;
  int THROWS = 60;
  int TRANSIENT = 61;
  int TRUE = 62;
  int TRY = 63;
  int VOID = 64;
  int VOLATILE = 65;
  int VIEW = 66;
  int WHILE = 67;
  int UNDERSCORE = 68;
  int ARROW = 69;
  int FUNCTION = 70;
  int VIRTUAL = 71;
  int VARARGS = 72;
  int FORWARD = 73;
  int RULE = 74;
  int PCUT = 75;
  int CAST = 76;
  int REINTERP = 77;
  int ALIAS = 78;
  int OPERATOR_ID = 79;
  int TYPEDEF = 80;
  int ENUM = 81;
  int REQUIRE = 82;
  int ENSURE = 83;
  int INVARIANT = 84;
  int GENERATE = 85;
  int PACKED = 86;
  int WRAPPER = 87;
  int ACCESS = 88;
  int READ_ONLY = 89;
  int WRITE_ONLY = 90;
  int READ_WRITE = 91;
  int NO_READ_WRITE = 92;
  int COMMA1 = 93;
  int PRAGMA = 99;
  int PRAGMA_ENABLE = 100;
  int PRAGMA_DISABLE = 101;
  int INTEGER_LITERAL = 102;
  int LONG_INTEGER_LITERAL = 103;
  int DECIMAL_LITERAL = 104;
  int HEX_LITERAL = 105;
  int OCTAL_LITERAL = 106;
  int FLOATING_POINT_LITERAL = 107;
  int DOUBLE_POINT_LITERAL = 108;
  int EXPONENT = 109;
  int REPARSE_EXPRESSION = 110;
  int REPARSE_STATEMENT = 111;
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
  int COMMA = 125;
  int DOT = 126;
  int COLON = 127;
  int QUESTION = 128;
  int LT = 129;
  int GT = 130;
  int ASSIGN = 131;
  int ASSIGN2 = 132;
  int BANG = 133;
  int TILDE = 134;
  int EQ = 135;
  int LE = 136;
  int GE = 137;
  int NE = 138;
  int SC_OR = 139;
  int SC_AND = 140;
  int INCR = 141;
  int DECR = 142;
  int PLUS = 143;
  int MINUS = 144;
  int STAR = 145;
  int SLASH = 146;
  int BIT_AND = 147;
  int BIT_OR = 148;
  int XOR = 149;
  int REM = 150;
  int LSHIFT = 151;
  int PLUSASSIGN = 152;
  int MINUSASSIGN = 153;
  int STARASSIGN = 154;
  int SLASHASSIGN = 155;
  int ANDASSIGN = 156;
  int ORASSIGN = 157;
  int XORASSIGN = 158;
  int REMASSIGN = 159;
  int LSHIFTASSIGN = 160;
  int RSIGNEDSHIFTASSIGN = 161;
  int RUNSIGNEDSHIFTASSIGN = 162;
  int IS_THE = 163;
  int IS_ONE_OF = 164;
  int OPERATOR_AT = 165;
  int OPERATOR_SHARP = 166;
  int OPERATOR = 167;

  int DEFAULT = 0;
  int IN_CONSTRAINT = 1;
  int IN_PRAGMA = 2;
  int IN_SINGLE_LINE_COMMENT = 3;
  int IN_FORMAL_COMMENT = 4;
  int IN_MULTI_LINE_COMMENT = 5;
  int IN_ACCESS = 6;

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
    "<token of kind 10>",
    "<token of kind 11>",
    "<token of kind 12>",
    "\"/*\"",
    "<SINGLE_LINE_COMMENT>",
    "\"*/\"",
    "\"*/\"",
    "<token of kind 17>",
    "\"**/\"",
    "\"abstract\"",
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
    "\"long\"",
    "\"native\"",
    "\"new\"",
    "\"null\"",
    "\"package\"",
    "\"private\"",
    "\"protected\"",
    "\"public\"",
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
    "\"_\"",
    "\"->\"",
    "\"fun\"",
    "\"virtual\"",
    "\"...\"",
    "\"forward\"",
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
    "\"$generate\"",
    "\"packed\"",
    "\"$wrapper\"",
    "\"access:\"",
    "<READ_ONLY>",
    "<WRITE_ONLY>",
    "\"rw\"",
    "<NO_READ_WRITE>",
    "\",\"",
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
    "<OPERATOR>",
  };

}