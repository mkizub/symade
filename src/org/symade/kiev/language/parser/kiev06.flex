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
package org.symade.kiev.language.parser;

//import com.intellij.lexer.FlexLexer;
//import com.intellij.psi.tree.IElementType;
//import org.symade.kiev.language.psi.KievTokenTypes;
//import org.symade.kiev.language.psi.KievTokenType;
//import com.intellij.psi.KievTokenType;

%%

%public
%class KievLexer
//%implements FlexLexer
//%implements TokenConstants
%function advance
%type KievTokenType
%unicode
%line
%column

%{
      public final int getLineNo() {
        return yyline+1;
      }
      public final int getColumn() {
        return yycolumn+1;
      }
%}
//CRLF=\R
WHITE_SPACE=[\ \n\r\t\f]+
LINE_COMMENT="//".*
BLOCK_COMMENT="/"\*([^*]|\*+[^*/])*(\*+"/")?

SUPERSCRIPT_PLUS  = [\u207a]  // T⁺ u207a
SUPERSCRIPT_MINUS = [\u207b]  // T⁻ u207b
UPPER_BOUND       = [\u2264]  // ≤ u2264
LOWER_BOUND       = [\u2265]  // ≥ u2265

IDENTIFIER     = [:jletter:] [:jletterdigit:]*
ESC_IDENTIFIER = `([^\r\n`\\]|{ESCAPE_SEQUENCE})*`?
OPERATOR       = [\!\~\|\&\+\-\*\/\^\%\u2190-\u22F1]

DIGIT = [0-9]
DIGIT_OR_UNDERSCORE = [_0-9]
DIGITS = {DIGIT} | {DIGIT} {DIGIT_OR_UNDERSCORE}*
HEX_DIGIT_OR_UNDERSCORE = [_0-9A-Fa-f]

INTEGER_LITERAL = {DIGITS} | {HEX_INTEGER_LITERAL} | {BIN_INTEGER_LITERAL}
LONG_LITERAL = {INTEGER_LITERAL} [Ll]
HEX_INTEGER_LITERAL = 0 [Xx] {HEX_DIGIT_OR_UNDERSCORE}*
BIN_INTEGER_LITERAL = 0 [Bb] {DIGIT_OR_UNDERSCORE}*

FLOAT_LITERAL = ({DEC_FP_LITERAL} | {HEX_FP_LITERAL}) [Ff] | {DIGITS} [Ff]
DOUBLE_LITERAL = ({DEC_FP_LITERAL} | {HEX_FP_LITERAL}) [Dd]? | {DIGITS} [Dd]
DEC_FP_LITERAL = {DIGITS} {DEC_EXPONENT} | {DEC_SIGNIFICAND} {DEC_EXPONENT}?
DEC_SIGNIFICAND = "." {DIGITS} | {DIGITS} "." {DIGIT_OR_UNDERSCORE}*
DEC_EXPONENT = [Ee] [+-]? {DIGIT_OR_UNDERSCORE}*
HEX_FP_LITERAL = {HEX_SIGNIFICAND} {HEX_EXPONENT}
HEX_SIGNIFICAND = 0 [Xx] ({HEX_DIGIT_OR_UNDERSCORE}+ "."? | {HEX_DIGIT_OR_UNDERSCORE}* "." {HEX_DIGIT_OR_UNDERSCORE}+)
HEX_EXPONENT = [Pp] [+-]? {DIGIT_OR_UNDERSCORE}*

STRING_LITERAL=\"([^\r\n\"\\]|{ESCAPE_SEQUENCE})*\"?
CHARACTER_LITERAL='([^\\]|{ESCAPE_SEQUENCE})'?
ESCAPE_SEQUENCE=\\([btnfr\"\'\\]|{OCTAL_ESCAPE}|{UNICODE_ESCAPE})
OCTAL_ESCAPE=[0-3][0-7][0-7] | [0-7][0-7] | [0-7]
UNICODE_ESCAPE=u[0-9A-Fa-f]{4}

%state WAITING_VALUE
%state IN_IMPORT
%xstate IN_ACCESS
%xstate IN_ACCESS_COLON
%xstate IN_ACCESS_PARENTH

%%

<IN_ACCESS_COLON> {
    {WHITE_SPACE}             { yybegin(YYINITIAL); return KievTokenTypes.WHITE_SPACE; }
    ","                       { return KievTokenTypes.OP_COMMA; }
}
<IN_ACCESS_PARENTH> {
    {WHITE_SPACE}             { return KievTokenTypes.WHITE_SPACE; }
    ","                       { return KievTokenTypes.OP_COMMA; }
    ")"                       { yybegin(YYINITIAL); return KievTokenTypes.OP_RPAREN; }
}

<IN_ACCESS> {
    {WHITE_SPACE}             { yybegin(YYINITIAL);         return KievTokenTypes.WHITE_SPACE; }
    ":"                       { yybegin(IN_ACCESS_COLON);   return KievTokenTypes.OP_COLON; }
    "("                       { yybegin(IN_ACCESS_PARENTH); return KievTokenTypes.OP_LPAREN; }
}

<IN_ACCESS_COLON, IN_ACCESS_PARENTH> {
    "ro"                      { return KievTokenTypes.KW_ACC_RO; }
    "wo"                      { return KievTokenTypes.KW_ACC_WO; }
    "rw"                      { return KievTokenTypes.KW_ACC_RW; }
    "no"                      { return KievTokenTypes.KW_ACC_NO; }
    [^]                       { yybegin(YYINITIAL); return KievTokenTypes.BAD_CHARACTER; }
}

<IN_IMPORT> {
  "syntax"                  { return KievTokenTypes.KW_SYNTAX; }
  ";"                       { yybegin(YYINITIAL); return KievTokenTypes.OP_SEMICOLON; }
}

{WHITE_SPACE}               { return KievTokenTypes.WHITE_SPACE; }
{LINE_COMMENT}              { return KievTokenTypes.LINE_COMMENT; }
{BLOCK_COMMENT}             { return KievTokenTypes.BLOCK_COMMENT; }

{LONG_LITERAL}              { return KievTokenTypes.LONG_LITERAL; }
{INTEGER_LITERAL}           { return KievTokenTypes.INTEGER_LITERAL; }
{FLOAT_LITERAL}             { return KievTokenTypes.FLOAT_LITERAL; }
{DOUBLE_LITERAL}            { return KievTokenTypes.DOUBLE_LITERAL; }
{STRING_LITERAL}            { return KievTokenTypes.STRING_LITERAL; }
{CHARACTER_LITERAL}         { return KievTokenTypes.CHARACTER_LITERAL; }

"package"                   { return KievTokenTypes.KW_PACKAGE; }
"import"                    { yybegin(IN_IMPORT); return KievTokenTypes.KW_IMPORT; }
//"syntax"                    { return KievTokenTypes.KW_SYNTAX; }
"pragma"                    { return KievTokenTypes.KW_PRAGMA; }

"class"                     { return KievTokenTypes.KW_CLASS; }
"interface"                 { return KievTokenTypes.KW_INTERFACE; }
"enum"                      { return KievTokenTypes.KW_ENUM; }
//  "view"                      { return KievTokenTypes.KW_VIEW; }
"extends"                   { return KievTokenTypes.KW_EXTENDS; }
"implements"                { return KievTokenTypes.KW_IMPLEMENTS; }
"type"                      { return KievTokenTypes.KW_TYPE; }
"typedef"                   { return KievTokenTypes.KW_TYPEDEF; }

"@access"                   { yybegin(IN_ACCESS); return KievTokenTypes.META_ACCESS; }
"@public"                   { yybegin(IN_ACCESS); return KievTokenTypes.META_PUBLIC; }
"@protected"                { yybegin(IN_ACCESS); return KievTokenTypes.META_PROTECTED; }
"@private"                  { yybegin(IN_ACCESS); return KievTokenTypes.META_PRIVATE; }
"public"                    { yybegin(IN_ACCESS); return KievTokenTypes.KW_PUBLIC; }
"protected"                 { yybegin(IN_ACCESS); return KievTokenTypes.KW_PROTECTED; }
"private"                   { yybegin(IN_ACCESS); return KievTokenTypes.KW_PRIVATE; }
//  "ro"                        { return KievTokenTypes.KW_ACC_RO; }
//  "wo"                        { return KievTokenTypes.KW_ACC_WO; }
//  "rw"                        { return KievTokenTypes.KW_ACC_RW; }
//  "no"                        { return KievTokenTypes.KW_ACC_NO; }

//  "@interface"                { return KievTokenTypes.META_INTERFACE; }
//  "@singleton"                { return KievTokenTypes.META_SINGLETON; }
//  "@mixin"                    { return KievTokenTypes.META_MIXIN; }
//  "@forward"                  { return KievTokenTypes.META_FORWARD; }
//  "@unerasable"               { return KievTokenTypes.META_UNERASABLE; }
//  "@virtual"                  { return KievTokenTypes.META_VIRTUAL; }
//  "@packed"                   { return KievTokenTypes.META_PACKED; }
//  "@macro"                    { return KievTokenTypes.META_MACRO; }
//  "@static"                   { return KievTokenTypes.META_STATIC; }
//  "@abstract"                 { return KievTokenTypes.META_ABSTRACT; }
//  "@final"                    { return KievTokenTypes.META_FINAL; }
//  "@native"                   { return KievTokenTypes.META_NATIVE; }
//  "@synchronized"             { return KievTokenTypes.META_SYNCHRONIZED; }
//  "@transient"                { return KievTokenTypes.META_TRANSIENT; }
//  "@volatile"                 { return KievTokenTypes.META_VOLATILE; }
//  "@throws"                   { return KievTokenTypes.META_THROWS; }
//  "@uuid"                     { return KievTokenTypes.META_UUID; }
//  "@getter"                   { return KievTokenTypes.META_GETTER; }
//  "@setter"                   { return KievTokenTypes.META_SETTER; }
"static"                    { return KievTokenTypes.KW_STATIC; }
"abstract"                  { return KievTokenTypes.KW_ABSTRACT; }
"final"                     { return KievTokenTypes.KW_FINAL; }
"native"                    { return KievTokenTypes.KW_NATIVE; }
"synchronized"              { return KievTokenTypes.KW_SYNCHRONIZED; }
"transient"                 { return KievTokenTypes.KW_TRANSIENT; }
"volatile"                  { return KievTokenTypes.KW_VOLATILE; }

"operator"                  { return KievTokenTypes.KW_OPERATOR; }
"throws"                    { return KievTokenTypes.KW_THROWS; }
"alias"                     { return KievTokenTypes.KW_ALIAS; }
"require"                   { return KievTokenTypes.KW_REQUIRE; }
"ensure"                    { return KievTokenTypes.KW_ENSURE; }
"invariant"                 { return KievTokenTypes.KW_INVARIANT; }

"return"                    { return KievTokenTypes.KW_RETURN; }
"break"                     { return KievTokenTypes.KW_BREAK; }
"continue"                  { return KievTokenTypes.KW_CONTINUE; }
"goto"                      { return KievTokenTypes.KW_GOTO; }
"case"                      { return KievTokenTypes.KW_CASE; }
"default"                   { return KievTokenTypes.KW_DEFAULT; }
"do"                        { return KievTokenTypes.KW_DO; }
"while"                     { return KievTokenTypes.KW_WHILE; }
"for"                       { return KievTokenTypes.KW_FOR; }
"foreach"                   { return KievTokenTypes.KW_FOREACH; }
"if"                        { return KievTokenTypes.KW_IF; }
"else"                      { return KievTokenTypes.KW_ELSE; }
"try"                       { return KievTokenTypes.KW_TRY; }
"catch"                     { return KievTokenTypes.KW_CATCH; }
"finally"                   { return KievTokenTypes.KW_FINALLY; }
"switch"                    { return KievTokenTypes.KW_SWITCH; }
"with"                      { return KievTokenTypes.KW_WITH; }

"throw"                     { return KievTokenTypes.KW_THROW; }
"fun"                       { return KievTokenTypes.KW_FUN; }
"new"                       { return KievTokenTypes.KW_NEW; }

"$cut"                      { return KievTokenTypes.KW_PCUT; }

"this"                      { return KievTokenTypes.KW_THIS; }
"super"                     { return KievTokenTypes.KW_SUPER; }
"true"                      { return KievTokenTypes.KW_TRUE; }
"false"                     { return KievTokenTypes.KW_FALSE; }
"null"                      { return KievTokenTypes.KW_NULL; }

// identifier after all keyword
{IDENTIFIER}                { return KievTokenTypes.IDENTIFIER; }
{ESC_IDENTIFIER}            { return KievTokenTypes.ESC_IDENTIFIER; }

// multi-character operators first
"?="                      { return KievTokenTypes.OP_IS_THE; }
"@="                      { return KievTokenTypes.OP_IS_ONE_OF; }
"->"                      { return KievTokenTypes.OP_ARROW; }
"[]"                      { return KievTokenTypes.OP_LRBRACKETS; }
//">="                    { return KievTokenTypes.OP_GE; }
//"<="                    { return KievTokenTypes.OP_LE; }
"..."                     { return KievTokenTypes.OP_VARARGS; }

// unicode operators
{SUPERSCRIPT_PLUS}        { return KievTokenTypes.OP_SUPERSCRIPT_PLUS; } // T⁺ u207a
{SUPERSCRIPT_MINUS}       { return KievTokenTypes.OP_SUPERSCRIPT_MINUS; } // T⁻ u207b
{UPPER_BOUND}             { return KievTokenTypes.OP_UPPER_BOUND; } // ≤ u2264
{LOWER_BOUND}             { return KievTokenTypes.OP_LOWER_BOUND; } // ≥ u2265

// used in parser single-char operators
"."                       { return KievTokenTypes.OP_DOT; }
"("                       { return KievTokenTypes.OP_LPAREN; }
")"                       { return KievTokenTypes.OP_RPAREN; }
"{"                       { return KievTokenTypes.OP_LBRACE; }
"}"                       { return KievTokenTypes.OP_RBRACE; }
"["                       { return KievTokenTypes.OP_LBRACKET; }
"]"                       { return KievTokenTypes.OP_RBRACKET; }
";"                       { return KievTokenTypes.OP_SEMICOLON; }
":"                       { return KievTokenTypes.OP_COLON; }
","                       { return KievTokenTypes.OP_COMMA; }
"?"                       { return KievTokenTypes.OP_QUESTION; }
"<"                       { return KievTokenTypes.OP_LT; }
">"                       { return KievTokenTypes.OP_GT; }
"="                       { return KievTokenTypes.OP_ASSIGN; }
"@"                       { return KievTokenTypes.OP_AT; }
"#"                       { return KievTokenTypes.OP_SHARP; }

// single-character operators after all operators
{OPERATOR}	              { return KievTokenTypes.OPERATOR; }

[^]                       { return KievTokenTypes.BAD_CHARACTER; }
