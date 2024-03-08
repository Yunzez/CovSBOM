/* Generated By:JavaCC: Do not edit this line. FMParserConstants.java */
package freemarker.core;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
interface FMParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int BLANK = 1;
  /** RegularExpression Id. */
  int START_TAG = 2;
  /** RegularExpression Id. */
  int END_TAG = 3;
  /** RegularExpression Id. */
  int CLOSE_TAG1 = 4;
  /** RegularExpression Id. */
  int CLOSE_TAG2 = 5;
  /** RegularExpression Id. */
  int ATTEMPT = 6;
  /** RegularExpression Id. */
  int RECOVER = 7;
  /** RegularExpression Id. */
  int IF = 8;
  /** RegularExpression Id. */
  int ELSE_IF = 9;
  /** RegularExpression Id. */
  int LIST = 10;
  /** RegularExpression Id. */
  int ITEMS = 11;
  /** RegularExpression Id. */
  int SEP = 12;
  /** RegularExpression Id. */
  int FOREACH = 13;
  /** RegularExpression Id. */
  int SWITCH = 14;
  /** RegularExpression Id. */
  int CASE = 15;
  /** RegularExpression Id. */
  int ASSIGN = 16;
  /** RegularExpression Id. */
  int GLOBALASSIGN = 17;
  /** RegularExpression Id. */
  int LOCALASSIGN = 18;
  /** RegularExpression Id. */
  int _INCLUDE = 19;
  /** RegularExpression Id. */
  int IMPORT = 20;
  /** RegularExpression Id. */
  int FUNCTION = 21;
  /** RegularExpression Id. */
  int MACRO = 22;
  /** RegularExpression Id. */
  int TRANSFORM = 23;
  /** RegularExpression Id. */
  int VISIT = 24;
  /** RegularExpression Id. */
  int STOP = 25;
  /** RegularExpression Id. */
  int RETURN = 26;
  /** RegularExpression Id. */
  int CALL = 27;
  /** RegularExpression Id. */
  int SETTING = 28;
  /** RegularExpression Id. */
  int OUTPUTFORMAT = 29;
  /** RegularExpression Id. */
  int AUTOESC = 30;
  /** RegularExpression Id. */
  int NOAUTOESC = 31;
  /** RegularExpression Id. */
  int COMPRESS = 32;
  /** RegularExpression Id. */
  int COMMENT = 33;
  /** RegularExpression Id. */
  int TERSE_COMMENT = 34;
  /** RegularExpression Id. */
  int NOPARSE = 35;
  /** RegularExpression Id. */
  int END_IF = 36;
  /** RegularExpression Id. */
  int END_LIST = 37;
  /** RegularExpression Id. */
  int END_ITEMS = 38;
  /** RegularExpression Id. */
  int END_SEP = 39;
  /** RegularExpression Id. */
  int END_RECOVER = 40;
  /** RegularExpression Id. */
  int END_ATTEMPT = 41;
  /** RegularExpression Id. */
  int END_FOREACH = 42;
  /** RegularExpression Id. */
  int END_LOCAL = 43;
  /** RegularExpression Id. */
  int END_GLOBAL = 44;
  /** RegularExpression Id. */
  int END_ASSIGN = 45;
  /** RegularExpression Id. */
  int END_FUNCTION = 46;
  /** RegularExpression Id. */
  int END_MACRO = 47;
  /** RegularExpression Id. */
  int END_OUTPUTFORMAT = 48;
  /** RegularExpression Id. */
  int END_AUTOESC = 49;
  /** RegularExpression Id. */
  int END_NOAUTOESC = 50;
  /** RegularExpression Id. */
  int END_COMPRESS = 51;
  /** RegularExpression Id. */
  int END_TRANSFORM = 52;
  /** RegularExpression Id. */
  int END_SWITCH = 53;
  /** RegularExpression Id. */
  int ELSE = 54;
  /** RegularExpression Id. */
  int BREAK = 55;
  /** RegularExpression Id. */
  int CONTINUE = 56;
  /** RegularExpression Id. */
  int SIMPLE_RETURN = 57;
  /** RegularExpression Id. */
  int HALT = 58;
  /** RegularExpression Id. */
  int FLUSH = 59;
  /** RegularExpression Id. */
  int TRIM = 60;
  /** RegularExpression Id. */
  int LTRIM = 61;
  /** RegularExpression Id. */
  int RTRIM = 62;
  /** RegularExpression Id. */
  int NOTRIM = 63;
  /** RegularExpression Id. */
  int DEFAUL = 64;
  /** RegularExpression Id. */
  int SIMPLE_NESTED = 65;
  /** RegularExpression Id. */
  int NESTED = 66;
  /** RegularExpression Id. */
  int SIMPLE_RECURSE = 67;
  /** RegularExpression Id. */
  int RECURSE = 68;
  /** RegularExpression Id. */
  int FALLBACK = 69;
  /** RegularExpression Id. */
  int ESCAPE = 70;
  /** RegularExpression Id. */
  int END_ESCAPE = 71;
  /** RegularExpression Id. */
  int NOESCAPE = 72;
  /** RegularExpression Id. */
  int END_NOESCAPE = 73;
  /** RegularExpression Id. */
  int UNIFIED_CALL = 74;
  /** RegularExpression Id. */
  int UNIFIED_CALL_END = 75;
  /** RegularExpression Id. */
  int FTL_HEADER = 76;
  /** RegularExpression Id. */
  int TRIVIAL_FTL_HEADER = 77;
  /** RegularExpression Id. */
  int UNKNOWN_DIRECTIVE = 78;
  /** RegularExpression Id. */
  int STATIC_TEXT_WS = 79;
  /** RegularExpression Id. */
  int STATIC_TEXT_NON_WS = 80;
  /** RegularExpression Id. */
  int STATIC_TEXT_FALSE_ALARM = 81;
  /** RegularExpression Id. */
  int DOLLAR_INTERPOLATION_OPENING = 82;
  /** RegularExpression Id. */
  int HASH_INTERPOLATION_OPENING = 83;
  /** RegularExpression Id. */
  int SQUARE_BRACKET_INTERPOLATION_OPENING = 84;
  /** RegularExpression Id. */
  int ESCAPED_CHAR = 92;
  /** RegularExpression Id. */
  int STRING_LITERAL = 93;
  /** RegularExpression Id. */
  int RAW_STRING = 94;
  /** RegularExpression Id. */
  int FALSE = 95;
  /** RegularExpression Id. */
  int TRUE = 96;
  /** RegularExpression Id. */
  int INTEGER = 97;
  /** RegularExpression Id. */
  int DECIMAL = 98;
  /** RegularExpression Id. */
  int DOT = 99;
  /** RegularExpression Id. */
  int DOT_DOT = 100;
  /** RegularExpression Id. */
  int DOT_DOT_LESS = 101;
  /** RegularExpression Id. */
  int DOT_DOT_ASTERISK = 102;
  /** RegularExpression Id. */
  int BUILT_IN = 103;
  /** RegularExpression Id. */
  int EXISTS = 104;
  /** RegularExpression Id. */
  int EQUALS = 105;
  /** RegularExpression Id. */
  int DOUBLE_EQUALS = 106;
  /** RegularExpression Id. */
  int NOT_EQUALS = 107;
  /** RegularExpression Id. */
  int PLUS_EQUALS = 108;
  /** RegularExpression Id. */
  int MINUS_EQUALS = 109;
  /** RegularExpression Id. */
  int TIMES_EQUALS = 110;
  /** RegularExpression Id. */
  int DIV_EQUALS = 111;
  /** RegularExpression Id. */
  int MOD_EQUALS = 112;
  /** RegularExpression Id. */
  int PLUS_PLUS = 113;
  /** RegularExpression Id. */
  int MINUS_MINUS = 114;
  /** RegularExpression Id. */
  int LESS_THAN = 115;
  /** RegularExpression Id. */
  int LESS_THAN_EQUALS = 116;
  /** RegularExpression Id. */
  int ESCAPED_GT = 117;
  /** RegularExpression Id. */
  int ESCAPED_GTE = 118;
  /** RegularExpression Id. */
  int PLUS = 119;
  /** RegularExpression Id. */
  int MINUS = 120;
  /** RegularExpression Id. */
  int TIMES = 121;
  /** RegularExpression Id. */
  int DOUBLE_STAR = 122;
  /** RegularExpression Id. */
  int ELLIPSIS = 123;
  /** RegularExpression Id. */
  int DIVIDE = 124;
  /** RegularExpression Id. */
  int PERCENT = 125;
  /** RegularExpression Id. */
  int AND = 126;
  /** RegularExpression Id. */
  int OR = 127;
  /** RegularExpression Id. */
  int EXCLAM = 128;
  /** RegularExpression Id. */
  int COMMA = 129;
  /** RegularExpression Id. */
  int SEMICOLON = 130;
  /** RegularExpression Id. */
  int COLON = 131;
  /** RegularExpression Id. */
  int OPEN_BRACKET = 132;
  /** RegularExpression Id. */
  int CLOSE_BRACKET = 133;
  /** RegularExpression Id. */
  int OPEN_PAREN = 134;
  /** RegularExpression Id. */
  int CLOSE_PAREN = 135;
  /** RegularExpression Id. */
  int OPENING_CURLY_BRACKET = 136;
  /** RegularExpression Id. */
  int CLOSING_CURLY_BRACKET = 137;
  /** RegularExpression Id. */
  int IN = 138;
  /** RegularExpression Id. */
  int AS = 139;
  /** RegularExpression Id. */
  int USING = 140;
  /** RegularExpression Id. */
  int ID = 141;
  /** RegularExpression Id. */
  int OPEN_MISPLACED_INTERPOLATION = 142;
  /** RegularExpression Id. */
  int NON_ESCAPED_ID_START_CHAR = 143;
  /** RegularExpression Id. */
  int ESCAPED_ID_CHAR = 144;
  /** RegularExpression Id. */
  int ID_START_CHAR = 145;
  /** RegularExpression Id. */
  int ASCII_DIGIT = 146;
  /** RegularExpression Id. */
  int DIRECTIVE_END = 147;
  /** RegularExpression Id. */
  int EMPTY_DIRECTIVE_END = 148;
  /** RegularExpression Id. */
  int NATURAL_GT = 149;
  /** RegularExpression Id. */
  int NATURAL_GTE = 150;
  /** RegularExpression Id. */
  int TERMINATING_WHITESPACE = 151;
  /** RegularExpression Id. */
  int TERMINATING_EXCLAM = 152;
  /** RegularExpression Id. */
  int TERSE_COMMENT_END = 153;
  /** RegularExpression Id. */
  int MAYBE_END = 154;
  /** RegularExpression Id. */
  int KEEP_GOING = 155;
  /** RegularExpression Id. */
  int LONE_LESS_THAN_OR_DASH = 156;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int NO_DIRECTIVE = 1;
  /** Lexical state. */
  int FM_EXPRESSION = 2;
  /** Lexical state. */
  int IN_PAREN = 3;
  /** Lexical state. */
  int NAMED_PARAMETER_EXPRESSION = 4;
  /** Lexical state. */
  int EXPRESSION_COMMENT = 5;
  /** Lexical state. */
  int NO_SPACE_EXPRESSION = 6;
  /** Lexical state. */
  int NO_PARSE = 7;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "<BLANK>",
    "<START_TAG>",
    "<END_TAG>",
    "<CLOSE_TAG1>",
    "<CLOSE_TAG2>",
    "<ATTEMPT>",
    "<RECOVER>",
    "<IF>",
    "<ELSE_IF>",
    "<LIST>",
    "<ITEMS>",
    "<SEP>",
    "<FOREACH>",
    "<SWITCH>",
    "<CASE>",
    "<ASSIGN>",
    "<GLOBALASSIGN>",
    "<LOCALASSIGN>",
    "<_INCLUDE>",
    "<IMPORT>",
    "<FUNCTION>",
    "<MACRO>",
    "<TRANSFORM>",
    "<VISIT>",
    "<STOP>",
    "<RETURN>",
    "<CALL>",
    "<SETTING>",
    "<OUTPUTFORMAT>",
    "<AUTOESC>",
    "<NOAUTOESC>",
    "<COMPRESS>",
    "<COMMENT>",
    "<TERSE_COMMENT>",
    "<NOPARSE>",
    "<END_IF>",
    "<END_LIST>",
    "<END_ITEMS>",
    "<END_SEP>",
    "<END_RECOVER>",
    "<END_ATTEMPT>",
    "<END_FOREACH>",
    "<END_LOCAL>",
    "<END_GLOBAL>",
    "<END_ASSIGN>",
    "<END_FUNCTION>",
    "<END_MACRO>",
    "<END_OUTPUTFORMAT>",
    "<END_AUTOESC>",
    "<END_NOAUTOESC>",
    "<END_COMPRESS>",
    "<END_TRANSFORM>",
    "<END_SWITCH>",
    "<ELSE>",
    "<BREAK>",
    "<CONTINUE>",
    "<SIMPLE_RETURN>",
    "<HALT>",
    "<FLUSH>",
    "<TRIM>",
    "<LTRIM>",
    "<RTRIM>",
    "<NOTRIM>",
    "<DEFAUL>",
    "<SIMPLE_NESTED>",
    "<NESTED>",
    "<SIMPLE_RECURSE>",
    "<RECURSE>",
    "<FALLBACK>",
    "<ESCAPE>",
    "<END_ESCAPE>",
    "<NOESCAPE>",
    "<END_NOESCAPE>",
    "<UNIFIED_CALL>",
    "<UNIFIED_CALL_END>",
    "<FTL_HEADER>",
    "<TRIVIAL_FTL_HEADER>",
    "<UNKNOWN_DIRECTIVE>",
    "<STATIC_TEXT_WS>",
    "<STATIC_TEXT_NON_WS>",
    "<STATIC_TEXT_FALSE_ALARM>",
    "\"${\"",
    "\"#{\"",
    "\"[=\"",
    "<token of kind 85>",
    "<token of kind 86>",
    "<token of kind 87>",
    "\">\"",
    "\"]\"",
    "\"-\"",
    "<token of kind 91>",
    "<ESCAPED_CHAR>",
    "<STRING_LITERAL>",
    "<RAW_STRING>",
    "\"false\"",
    "\"true\"",
    "<INTEGER>",
    "<DECIMAL>",
    "\".\"",
    "\"..\"",
    "<DOT_DOT_LESS>",
    "\"..*\"",
    "\"?\"",
    "\"??\"",
    "\"=\"",
    "\"==\"",
    "\"!=\"",
    "\"+=\"",
    "\"-=\"",
    "\"*=\"",
    "\"/=\"",
    "\"%=\"",
    "\"++\"",
    "\"--\"",
    "<LESS_THAN>",
    "<LESS_THAN_EQUALS>",
    "<ESCAPED_GT>",
    "<ESCAPED_GTE>",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"**\"",
    "\"...\"",
    "\"/\"",
    "\"%\"",
    "<AND>",
    "<OR>",
    "\"!\"",
    "\",\"",
    "\";\"",
    "\":\"",
    "\"[\"",
    "\"]\"",
    "\"(\"",
    "\")\"",
    "\"{\"",
    "\"}\"",
    "\"in\"",
    "\"as\"",
    "\"using\"",
    "<ID>",
    "<OPEN_MISPLACED_INTERPOLATION>",
    "<NON_ESCAPED_ID_START_CHAR>",
    "<ESCAPED_ID_CHAR>",
    "<ID_START_CHAR>",
    "<ASCII_DIGIT>",
    "\">\"",
    "<EMPTY_DIRECTIVE_END>",
    "\">\"",
    "\">=\"",
    "<TERMINATING_WHITESPACE>",
    "<TERMINATING_EXCLAM>",
    "<TERSE_COMMENT_END>",
    "<MAYBE_END>",
    "<KEEP_GOING>",
    "<LONE_LESS_THAN_OR_DASH>",
  };

}