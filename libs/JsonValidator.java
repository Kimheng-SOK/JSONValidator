import java.io.*;

/**
 * Complete JSON Validator
 * Performs both lexical analysis and structural parsing
 */
public class JsonValidator {
    
    /**
     * Validation result containing status and error details
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String[] errors;
        
        public ValidationResult(boolean valid, String message, String[] errors) {
            this.valid = valid;
            this.message = message;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String[] getErrors() {
            return errors;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("===========================================\n");
            sb.append("           JSON VALIDATION RESULT\n");
            sb.append("===========================================\n");
            
            if (valid) {
                sb.append("Status: VALID JSON\n");
                sb.append(message).append("\n");
            } else {
                sb.append("Status: INVALID JSON\n");
                sb.append(message).append("\n");
                
                if (errors != null && errors.length > 0) {
                    sb.append("\nErrors Found:\n");
                    for (int i = 0; i < errors.length; i++) {
                        sb.append("  ").append(i + 1).append(". ").append(errors[i]).append("\n");
                    }
                }
            }
            
            sb.append("===========================================");
            return sb.toString();
        }
    }
    
    /**
     * Validate a JSON string
     * 
     * @param jsonString The JSON string to validate
     * @return ValidationResult with status and error details
     */
    public static ValidationResult validate(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ValidationResult(false, "Empty or null input", 
                new String[]{"JSON input cannot be empty"});
        }
        
        try {
            // Create parser with the JSON string
            StringReader reader = new StringReader(jsonString);
            JsonParser parser = new JsonParser(reader);
            
            // Parse the JSON
            boolean isValid = parser.parse();
            
            if (isValid) {
                return new ValidationResult(true, 
                    "JSON is well-formed and structurally valid", null);
            } else {
                // Get errors from parser
                String[] errorArray = parser.getErrors().toArray(new String[0]);
                return new ValidationResult(false, 
                    "JSON validation failed", errorArray);
            }
            
        } catch (IOException e) {
            return new ValidationResult(false, 
                "IO error during validation", 
                new String[]{"IOException: " + e.getMessage()});
        } catch (Exception e) {
            return new ValidationResult(false, 
                "Unexpected error during validation", 
                new String[]{"Exception: " + e.getMessage()});
        }
    }
    
    /**
     * Validate a JSON file
     * 
     * @param filename Path to the JSON file
     * @return ValidationResult with status and error details
     */
    public static ValidationResult validateFile(String filename) {
        try {
            // Read file content
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // Validate the content
            return validate(content.toString());
            
        } catch (FileNotFoundException e) {
            return new ValidationResult(false, 
                "File not found", 
                new String[]{"Cannot find file: " + filename});
        } catch (IOException e) {
            return new ValidationResult(false, 
                "Error reading file", 
                new String[]{"IOException: " + e.getMessage()});
        }
    }
    
    /**
     * Main method for command-line usage
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("     JSON VALIDATOR - Complex Mode     ");
        System.out.println("===========================================\n");
        
        if (args.length == 0) {
            // Run test cases
            runTestCases();
        } else {
            // Validate file
            System.out.println("Validating file: " + args[0] + "\n");
            ValidationResult result = validateFile(args[0]);
            System.out.println(result);
        }
    }
    
    /**
     * Run comprehensive test cases
     */
    private static void runTestCases() {
        String[] validTests = {
            "{}",
            "[]",
            "{\"name\": \"John\"}",
            "[1, 2, 3]",
            "{\"user\": {\"name\": \"Alice\", \"age\": 30}}",
            "[{\"id\": 1}, {\"id\": 2}]",
            "{\"data\": [1, 2, 3], \"meta\": {\"count\": 3}}",
            "{\"values\": [true, false, null]}",
            "{\"number\": 123, \"float\": 3.14, \"negative\": -42}",
            "{\"scientific\": 1.5e10, \"small\": 2.5e-3}"
        };
        
        String[] invalidTests = {
            "{",                                    // Unclosed brace
            "[1, 2,]",                             // Trailing comma
            "{\"name\": \"John\",}",               // Trailing comma in object
            "{name: \"John\"}",                    // Unquoted key
            "{'name': 'John'}",                    // Single quotes
            "{\"name\" \"John\"}",                 // Missing colon
            "{\"name\": }",                        // Missing value
            "[1, 2 3]",                            // Missing comma
            "{\"a\": 1 \"b\": 2}",                 // Missing comma
            "{\"name\": \"John}",                  // Unterminated string
            "{\"value\": 007}",                    // Leading zero
            "{\"flag\": True}",                    // Wrong case boolean
            "{\"text\": \"invalid\\xescape\"}",    // Invalid escape
            "null null",                           // Multiple values
            "{\"a\": [1, 2}",                      // Mismatched brackets
        };
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("         VALID JSON TEST CASES");
        System.out.println("═══════════════════════════════════════\n");
        
        for (int i = 0; i < validTests.length; i++) {
            System.out.println("Test " + (i + 1) + ": " + validTests[i]);
            ValidationResult result = validate(validTests[i]);
            System.out.println("Result: " + (result.isValid() ? "[PASS]" : "[FAIL]"));
            if (!result.isValid()) {
                System.out.println("Unexpected errors:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }
            System.out.println();
        }
        
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("        INVALID JSON TEST CASES");
        System.out.println("═══════════════════════════════════════\n");
        
        for (int i = 0; i < invalidTests.length; i++) {
            System.out.println("Test " + (i + 1) + ": " + invalidTests[i]);
            ValidationResult result = validate(invalidTests[i]);
            System.out.println("Result: " + (!result.isValid() ? "[PASS] (correctly rejected)" : "[FAIL] (should be invalid)"));
            if (!result.isValid() && result.getErrors() != null) {
                System.out.println("Errors detected:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
            }
            System.out.println();
        }
    }
}
