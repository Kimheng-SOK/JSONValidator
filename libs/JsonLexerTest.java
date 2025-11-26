import java.io.*;

public class JsonLexerTest {
    
    public static void main(String[] args) {
        // Test cases
        String[] testCases = {
            // Simple object
            "{\"name\": \"John\", \"age\": 30}",
            
            // Array
            "[1, 2, 3, true, false, null]",
            
            // Nested structure
            "{\"user\": {\"name\": \"Alice\", \"scores\": [95, 87, 92]}}",
            
            // Numbers with scientific notation
            "{\"pi\": 3.14159, \"large\": 1.5e10, \"small\": -2.5e-3}",
            
            // Escape sequences
            "{\"path\": \"C:\\\\Users\\\\file.txt\", \"quote\": \"He said \\\"Hello\\\"\"}",
            
            // Complex JSON
            "{\"items\": [{\"id\": 1, \"active\": true}, {\"id\": 2, \"active\": false}], \"total\": 2}"
        };
        
        // Negative test cases (invalid JSON)
        String[] negativeTests = {
            // Unquoted string
            "{name: \"John\"}",
            
            // Single quotes instead of double quotes
            "{'name': 'John'}",
            
            // Unterminated string
            "{\"name\": \"John}",
            
            // Invalid number format
            "{\"age\": 03}",
            
            // Trailing comma
            "{\"name\": \"John\",}",
            
            // Missing colon
            "{\"name\" \"John\"}",
            
            // Invalid escape sequence
            "{\"text\": \"invalid\\xescape\"}",
            
            // Unescaped control character
            "{\"text\": \"line\nbreak\"}",
            
            // Invalid boolean
            "{\"flag\": True}",
            
            // Multiple decimal points
            "{\"value\": 3.14.159}",
            
            // Leading zeros
            "{\"id\": 007}",
            
            // Invalid Unicode escape
            "{\"char\": \"\\u12G5\"}"
        };
        
        System.out.println("========================================");
        System.out.println("         POSITIVE TEST CASES");
        System.out.println("========================================");
        
        for (int i = 0; i < testCases.length; i++) {
            System.out.println("\n=== Test Case " + (i + 1) + " ===");
            System.out.println("Input: " + testCases[i]);
            testLexer(testCases[i]);
        }
        
        System.out.println("\n\n========================================");
        System.out.println("         NEGATIVE TEST CASES");
        System.out.println("========================================");
        System.out.println("(These should produce ERROR tokens)\n");
        
        for (int i = 0; i < negativeTests.length; i++) {
            System.out.println("\n=== Negative Test " + (i + 1) + " ===");
            System.out.println("Input: " + negativeTests[i]);
            System.out.println("Expected: Should detect errors");
            testLexer(negativeTests[i]);
        }
        
        // Test with file input if provided
        if (args.length > 0) {
            System.out.println("\n\n========================================");
            System.out.println("         FILE INPUT TEST");
            System.out.println("========================================");
            System.out.println("\n=== Testing file: " + args[0] + " ===");
            testFile(args[0]);
        }
    }
    
    private static void testLexer(String input) {
        try {
            JsonLexer lexer = new JsonLexer(new StringReader(input));
            String token;
            int count = 0;
            boolean hasError = false;
            StringBuilder tokenList = new StringBuilder();
            
            // First pass: scan all tokens and collect results
            while (!(token = lexer.yylex()).equals(JsonLexer.EOF)) {
                count++;
                String text = lexer.yytext();
                
                if (token.equals(JsonLexer.ERROR)) {
                    hasError = true;
                    tokenList.append(String.format("  %3d. *** ERROR ***   : %s%n", count, 
                        text.length() > 50 ? text.substring(0, 47) + "..." : text));
                    break;
                } else {
                    tokenList.append(String.format("  %3d. %-15s : %s%n", count, token, 
                        text.length() > 50 ? text.substring(0, 47) + "..." : text));
                }
            }
            
            // Display status first
            if (hasError) {
                System.out.println("Status: ❌ INVALID JSON");
            } else {
                System.out.println("Status: ✅ VALID JSON");
            }
            
            // Then display tokens
            System.out.println("Tokens:");
            System.out.print(tokenList.toString());
            
            // Summary
            if (hasError) {
                System.out.println("Error detected at token " + count);
            } else {
                System.out.println("Total tokens: " + count);
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private static void testFile(String filename) {
        try {
            JsonLexer lexer = new JsonLexer(new FileReader(filename));
            String token;
            int count = 0;
            boolean hasError = false;
            StringBuilder tokenList = new StringBuilder();
            
            // First pass: scan all tokens and collect results
            while (!(token = lexer.yylex()).equals(JsonLexer.EOF)) {
                count++;
                String text = lexer.yytext();
                
                if (token.equals(JsonLexer.ERROR)) {
                    hasError = true;
                    tokenList.append(String.format("  %3d. *** ERROR ***   : %s%n", count, 
                        text.length() > 50 ? text.substring(0, 47) + "..." : text));
                    break;
                } else {
                    tokenList.append(String.format("  %3d. %-15s : %s%n", count, token, 
                        text.length() > 50 ? text.substring(0, 47) + "..." : text));
                }
            }
            
            // Display status first
            if (hasError) {
                System.out.println("Status: ❌ INVALID JSON");
            } else {
                System.out.println("Status: ✅ VALID JSON");
            }
            
            // Then display tokens
            System.out.println("Tokens:");
            System.out.print(tokenList.toString());
            
            // Summary
            if (hasError) {
                System.out.println("Error detected at token " + count);
            } else {
                System.out.println("Total tokens: " + count);
            }
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
