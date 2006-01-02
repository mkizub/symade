/* Generated By:JavaCC: Do not edit this line. kiev040Constants.java */
package kiev.parser;

public interface kiev040Constants {

  int EOF = 0;
  int SINGLE_LINE_COMMENT = 11;
  int FORMAL_COMMENT = 12;
  int MULTI_LINE_COMMENT = 13;
  int ABSTRACT = 15;
  int BOOLEAN = 16;
  int BREAK = 17;
  int BYTE = 18;
  int CASE = 19;
  int CATCH = 20;
  int CHAR = 21;
  int CLASS = 22;
  int CONTINUE = 23;
  int _DEFAULT = 24;
  int DO = 25;
  int DOUBLE = 26;
  int ELSE = 27;
  int EXTENDS = 28;
  int FALSE = 29;
  int FINAL = 30;
  int FINALLY = 31;
  int FLOAT = 32;
  int FOR = 33;
  int FOREACH = 34;
  int GOTO = 35;
  int IF = 36;
  int IMPLEMENTS = 37;
  int IMPORT = 38;
  int INT = 39;
  int INTERFACE = 40;
  int LONG = 41;
  int NATIVE = 42;
  int NEW = 43;
  int NULL = 44;
  int PACKAGE = 45;
  int PRIVATE = 46;
  int PROTECTED = 47;
  int PUBLIC = 48;
  int RETURN = 49;
  int SHORT = 50;
  int STATIC = 51;
  int SWITCH = 52;
  int SYNCHRONIZED = 53;
  int WITH = 54;
  int THROW = 55;
  int THROWS = 56;
  int TRANSIENT = 57;
  int TRUE = 58;
  int TRY = 59;
  int VOID = 60;
  int VOLATILE = 61;
  int VIEW = 62;
  int WHILE = 63;
  int ARROW = 64;
  int FUNCTION = 65;
  int VIRTUAL = 66;
  int VARARGS = 67;
  int FORWARD = 68;
  int RULE = 69;
  int PCUT = 70;
  int CAST = 71;
  int REINTERP = 72;
  int ALIAS = 73;
  int OPERATOR_ID = 74;
  int TYPEDEF = 75;
  int ENUM = 76;
  int REQUIRE = 77;
  int ENSURE = 78;
  int INVARIANT = 79;
  int PACKED = 80;
  int WRAPPER = 81;
  int ACCESS = 82;
  int READ_ONLY = 83;
  int WRITE_ONLY = 84;
  int READ_WRITE = 85;
  int NO_READ_WRITE = 86;
  int COMMA1 = 87;
  int PRAGMA = 93;
  int PRAGMA_ENABLE = 94;
  int PRAGMA_DISABLE = 95;
  int INTEGER_LITERAL = 96;
  int LONG_INTEGER_LITERAL = 97;
  int DECIMAL_LITERAL = 98;
  int HEX_LITERAL = 99;
  int OCTAL_LITERAL = 100;
  int FLOATING_POINT_LITERAL = 101;
  int DOUBLE_POINT_LITERAL = 102;
  int EXPONENT = 103;
  int REPARSE_EXPRESSION = 104;
  int REPARSE_STATEMENT = 105;
  int REPARSE_TYPE = 106;
  int CHARACTER_LITERAL = 107;
  int STRING_LITERAL = 108;
  int IDENTIFIER = 109;
  int LETTER = 110;
  int DIGIT = 111;
  int ID_STRING_LITERAL = 112;
  int LPAREN = 113;
  int RPAREN = 114;
  int LBRACE = 115;
  int RBRACE = 116;
  int LBRACKET = 117;
  int RBRACKET = 118;
  int SEMICOLON = 119;
  int COMMA = 120;
  int DOT = 121;
  int COLON = 122;
  int QUESTION = 123;
  int LT = 124;
  int GT = 125;
  int ASSIGN = 126;
  int ASSIGN2 = 127;
  int BANG = 128;
  int TILDE = 129;
  int EQ = 130;
  int LE = 131;
  int GE = 132;
  int NE = 133;
  int SC_OR = 134;
  int SC_AND = 135;
  int INCR = 136;
  int DECR = 137;
  int PLUS = 138;
  int MINUS = 139;
  int STAR = 140;
  int SLASH = 141;
  int BIT_AND = 142;
  int BIT_OR = 143;
  int XOR = 144;
  int REM = 145;
  int LSHIFT = 146;
  int PLUSASSIGN = 147;
  int MINUSASSIGN = 148;
  int STARASSIGN = 149;
  int SLASHASSIGN = 150;
  int ANDASSIGN = 151;
  int ORASSIGN = 152;
  int XORASSIGN = 153;
  int REMASSIGN = 154;
  int LSHIFTASSIGN = 155;
  int RSIGNEDSHIFTASSIGN = 156;
  int RUNSIGNEDSHIFTASSIGN = 157;
  int IS_THE = 158;
  int IS_ONE_OF = 159;
  int OPERATOR_AT = 160;
  int OPERATOR_SHARP = 161;
  int OPERATOR_LRBRACKETS = 162;
  int OPERATOR = 163;

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
    "<OPERATOR>",
  };

}
