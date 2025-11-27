/* JFlex specification for JSON lexer */

%%

%class JsonLexer
%unicode
%line
%column
%type String

%{
    // Token types
    public static final String LEFT_BRACE = "LEFT_BRACE";
    public static final String RIGHT_BRACE = "RIGHT_BRACE";
    public static final String LEFT_BRACKET = "LEFT_BRACKET";
    public static final String RIGHT_BRACKET = "RIGHT_BRACKET";
    public static final String COMMA = "COMMA";
    public static final String COLON = "COLON";
    public static final String STRING = "STRING";
    public static final String NUMBER = "NUMBER";
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";
    public static final String NULL = "NULL";
    public static final String ERROR = "ERROR";
    public static final String EOF = "EOF";
    
    private void error(String message) {
        System.err.println("Error at line " + (yyline + 1) + ", column " + (yycolumn + 1) + ": " + message);
    }
%}

/* Regular expression definitions */
WhiteSpace = [ \t\r\n]+
Digit = [0-9]
NonZeroDigit = [1-9]

/* Strict number format - no leading zeros except for "0" itself */
Integer = "-"? ("0" | {NonZeroDigit}{Digit}*)
Fraction = "."{Digit}+
Exponent = [eE][+-]?{Digit}+
Number = {Integer}{Fraction}?{Exponent}?

/* Invalid numbers to catch explicitly */
InvalidNumber = "-"? "0"{Digit}+ | {Number}"."{Digit}*"."

/* String components - be more strict */
/* Valid characters: printable chars except " and \ and control chars */
StringChar = [^\"\\\u0000-\u001F\u007F-\u009F]
/* Valid escape sequences only */
ValidEscape = "\\" ([\"\\/bfnrt] | "u"[0-9a-fA-F]{4})

/* Invalid escape sequences to catch */
InvalidEscape = "\\" [^\"\\\/bfnrtu] | "\\" "u" [0-9a-fA-F]{0,3}[^0-9a-fA-F] | "\\" "u" [0-9a-fA-F]{0,3}

/* Valid string */
String = \" ({StringChar} | {ValidEscape})* \"

/* Unterminated string */
UnterminatedString = \" ({StringChar} | {ValidEscape})*

/* String with invalid escape */
InvalidEscapeString = \" ({StringChar} | {ValidEscape})* {InvalidEscape}

/* String with control character */
ControlCharString = \" ({StringChar} | {ValidEscape})* [\u0000-\u001F]

/* Invalid keywords (wrong case) */
InvalidTrue = [Tt][Rr][Uu][Ee]
InvalidFalse = [Ff][Aa][Ll][Ss][Ee]
InvalidNull = [Nn][Uu][Ll][Ll]

%%

/* Lexical rules - ORDER MATTERS! More specific rules first */

<YYINITIAL> {
    /* Structural tokens */
    "{"                 { return LEFT_BRACE; }
    "}"                 { return RIGHT_BRACE; }
    "["                 { return LEFT_BRACKET; }
    "]"                 { return RIGHT_BRACKET; }
    ","                 { return COMMA; }
    ":"                 { return COLON; }
    
    /* Literals - exact match only */
    "true"              { return TRUE; }
    "false"             { return FALSE; }
    "null"              { return NULL; }
    
    /* Catch invalid boolean/null keywords (wrong case) */
    {InvalidTrue}       {
                          error("Invalid boolean: '" + yytext() + "' (must be lowercase 'true')");
                          return ERROR;
                        }
    {InvalidFalse}      {
                          error("Invalid boolean: '" + yytext() + "' (must be lowercase 'false')");
                          return ERROR;
                        }
    {InvalidNull}       {
                          error("Invalid null: '" + yytext() + "' (must be lowercase 'null')");
                          return ERROR;
                        }
    
    /* Catch invalid numbers first */
    {InvalidNumber}     {
                          error("Invalid number format: '" + yytext() + "'");
                          return ERROR;
                        }
    
    /* Valid number */
    {Number}            { return NUMBER; }
    
    /* Catch string errors before valid strings */
    {InvalidEscapeString} {
                          error("Invalid escape sequence in string");
                          return ERROR;
                        }
    
    {ControlCharString} {
                          error("Unescaped control character in string");
                          return ERROR;
                        }
    
    {UnterminatedString} {
                          error("Unterminated string");
                          return ERROR;
                        }
    
    /* Valid string */
    {String}            { return STRING; }
    
    /* Whitespace */
    {WhiteSpace}        { /* ignore */ }
    
    /* End of file */
    <<EOF>>             { return EOF; }
    
    /* Error handling - catch single/double quotes used incorrectly */
    "'"                 {
                          error("Invalid quote character (use double quotes for JSON strings)");
                          return ERROR;
                        }
    
    /* Catch any unidentified character */
    .                   { 
                          error("Unexpected character: '" + yytext() + "'");
                          return ERROR;
                        }
}
