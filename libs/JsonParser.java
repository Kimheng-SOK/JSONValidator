import java.io.*;
import java.util.*;

/**
 * JSON Parser - Validates JSON structure using tokens from JsonLexer
 * Implements recursive descent parsing
 */
public class JsonParser {
    private JsonLexer lexer;
    private String currentToken;
    private String currentText;
    private List<String> errors;
    
    public JsonParser(Reader reader) throws IOException {
        this.lexer = new JsonLexer(reader);
        this.errors = new ArrayList<>();
        advance(); // Read first token
    }
    
    /**
     * Parse and validate JSON input
     * @return true if valid, false otherwise
     */
    public boolean parse() {
        try {
            parseValue();
            
            // After parsing, should be at EOF
            if (!currentToken.equals(JsonLexer.EOF)) {
                error("Unexpected content after JSON value");
                return false;
            }
            
            return errors.isEmpty();
        } catch (Exception e) {
            error("Parse error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all parsing errors
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Advance to next token
     */
    private void advance() throws IOException {
        currentText = lexer.yytext();
        currentToken = lexer.yylex();
        
        // If lexer found an error, record it
        if (currentToken.equals(JsonLexer.ERROR)) {
            error("Lexical error: " + currentText);
        }
    }
    
    /**
     * Check if current token matches expected, then advance
     */
    private boolean match(String expected) throws IOException {
        if (currentToken.equals(expected)) {
            advance();
            return true;
        }
        return false;
    }
    
    /**
     * Require a specific token
     */
    private void expect(String expected) throws IOException {
        if (!currentToken.equals(expected)) {
            error("Expected " + expected + " but found " + currentToken);
            throw new RuntimeException("Parse error");
        }
        advance();
    }
    
    /**
     * Add error message
     */
    private void error(String message) {
        errors.add(message);
    }
    
    /**
     * Parse any JSON value
     */
    private void parseValue() throws IOException {
        if (currentToken.equals(JsonLexer.ERROR)) {
            throw new RuntimeException("Lexical error encountered");
        }
        
        switch (currentToken) {
            case JsonLexer.LEFT_BRACE:
                parseObject();
                break;
            case JsonLexer.LEFT_BRACKET:
                parseArray();
                break;
            case JsonLexer.STRING:
            case JsonLexer.NUMBER:
            case JsonLexer.TRUE:
            case JsonLexer.FALSE:
            case JsonLexer.NULL:
                advance(); // Consume the literal
                break;
            default:
                error("Expected value but found " + currentToken);
                throw new RuntimeException("Parse error");
        }
    }
    
    /**
     * Parse JSON object: { "key": value, ... }
     */
    private void parseObject() throws IOException {
        expect(JsonLexer.LEFT_BRACE);
        
        // Empty object is valid
        if (match(JsonLexer.RIGHT_BRACE)) {
            return;
        }
        
        // Parse key-value pairs
        do {
            // Expect string key
            if (!currentToken.equals(JsonLexer.STRING)) {
                error("Expected string key but found " + currentToken);
                throw new RuntimeException("Parse error");
            }
            advance();
            
            // Expect colon
            expect(JsonLexer.COLON);
            
            // Parse value
            parseValue();
            
        } while (match(JsonLexer.COMMA));
        
        // Expect closing brace
        expect(JsonLexer.RIGHT_BRACE);
    }
    
    /**
     * Parse JSON array: [ value, ... ]
     */
    private void parseArray() throws IOException {
        expect(JsonLexer.LEFT_BRACKET);
        
        // Empty array is valid
        if (match(JsonLexer.RIGHT_BRACKET)) {
            return;
        }
        
        // Parse values
        do {
            parseValue();
        } while (match(JsonLexer.COMMA));
        
        // Expect closing bracket
        expect(JsonLexer.RIGHT_BRACKET);
    }
}
