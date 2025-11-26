from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
import subprocess
import json
import os
import tempfile
import threading

app = Flask(__name__, static_folder='public')

# Configure CORS to allow all origins and methods
CORS(app, resources={
    r"/api/*": {
        "origins": "*",
        "methods": ["GET", "POST", "OPTIONS"],
        "allow_headers": ["Content-Type"]
    }
})

# Configuration
JAVA_CLASS_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'libs')
VALIDATOR_CLASS = "JsonValidator"

# --- Visitor Counter Configuration ---
COUNT_FILE = 'visitor_count.json'
COUNT_LOCK = threading.Lock()

def get_visitor_count_from_file():
    """Reads the current visitor count from the file."""
    with COUNT_LOCK:
        if not os.path.exists(COUNT_FILE):
            return 0
        try:
            with open(COUNT_FILE, 'r') as f:
                data = json.load(f)
                return data.get('count', 0)
        except (IOError, json.JSONDecodeError):
            # If file is corrupt or empty, reset count
            return 0

def save_visitor_count_to_file(count):
    """Saves the current visitor count to the file."""
    with COUNT_LOCK:
        with open(COUNT_FILE, 'w') as f:
            json.dump({'count': count}, f)

@app.route('/api/visitor_count', methods=['GET', 'POST', 'OPTIONS'])
def visitor_counter():
    """Handles incrementing and retrieving the visitor count."""
    if request.method == 'OPTIONS':
        return '', 204
        
    if request.method == 'POST':
        # Increment the count
        current_count = get_visitor_count_from_file()
        new_count = current_count + 1
        save_visitor_count_to_file(new_count)
        
        # Return the new count after increment
        return jsonify({'count': new_count}), 200

    if request.method == 'GET':
        # Retrieve the count
        current_count = get_visitor_count_from_file()
        return jsonify({'count': current_count}), 200

@app.route('/')
def index():
    """Serve the main HTML page"""
    return send_from_directory('public', 'index.html')

@app.route('/api/validate', methods=['POST', 'OPTIONS'])
def validate_json():
    """
    Validate JSON input using Java validator
    Expects JSON body: { "json": "..." }
    Returns: { "valid": bool, "message": str, "errors": [] }
    """
    # Handle preflight OPTIONS request
    if request.method == 'OPTIONS':
        return '', 204
    
    try:
        data = request.get_json()
        
        if not data or 'json' not in data:
            return jsonify({
                'valid': False,
                'message': 'No JSON input provided',
                'errors': ['Request must include "json" field']
            }), 400
        
        json_input = data['json']
        
        # Create temporary file with JSON input
        with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as temp_file:
            temp_file.write(json_input)
            temp_filename = temp_file.name
        
        try:
            # Call Java validator using subprocess
            print(f"\n{'='*60}")
            print(f"Running Java validator...")
            print(f"Classpath: {JAVA_CLASS_PATH}")
            print(f"Class: {VALIDATOR_CLASS}")
            print(f"Temp file: {temp_filename}")
            print(f"{'='*60}")
            
            result = subprocess.run(
                ['java', '-Dfile.encoding=UTF-8', '-cp', JAVA_CLASS_PATH, VALIDATOR_CLASS, temp_filename],
                capture_output=True,
                text=True,
                encoding='utf-8',
                timeout=10
            )
            
            # Print subprocess results
            print(f"\nReturn code: {result.returncode}")
            print(f"\n--- STDOUT ---")
            print(result.stdout)
            print(f"\n--- STDERR ---")
            print(result.stderr)
            print(f"{'='*60}\n")
            
            # Parse the output
            output = result.stdout
            
            # Check if validation passed - look for multiple indicators
            is_valid = (
                '✅ VALID JSON' in output or 
                'VALID JSON' in output and 'INVALID JSON' not in output or
                'JSON is well-formed and structurally valid' in output
            )
            
            # Extract errors if any
            errors = []
            if 'Errors Found:' in output:
                error_section = output.split('Errors Found:')[1].split('==========')[0]
                error_lines = [line.strip() for line in error_section.split('\n') if line.strip() and line.strip()[0].isdigit()]
                errors = [line.split('. ', 1)[1] if '. ' in line else line for line in error_lines]
            
            # Extract message - improved parsing
            message_section = 'Validation completed'
            if 'INVALID JSON' in output:
                # Find the line after "INVALID JSON"
                lines = output.split('\n')
                for i, line in enumerate(lines):
                    if 'INVALID JSON' in line and i + 1 < len(lines):
                        message_section = lines[i + 1].strip()
                        if message_section:
                            break
            elif 'VALID JSON' in output or 'JSON is well-formed' in output:
                # Find the descriptive message
                if 'JSON is well-formed and structurally valid' in output:
                    message_section = 'JSON is well-formed and structurally valid'
                else:
                    lines = output.split('\n')
                    for i, line in enumerate(lines):
                        if 'VALID JSON' in line and i + 1 < len(lines):
                            message_section = lines[i + 1].strip()
                            if message_section:
                                break
            
            return jsonify({
                'valid': is_valid,
                'message': message_section,
                'errors': errors
            })
            
        finally:
            # Clean up temp file
            if os.path.exists(temp_filename):
                os.remove(temp_filename)
    
    except subprocess.TimeoutExpired:
        print("ERROR: Subprocess timeout!")
        return jsonify({
            'valid': False,
            'message': 'Validation timeout',
            'errors': ['Validation process took too long']
        }), 500
    
    except Exception as e:
        print(f"ERROR: {type(e).__name__}: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({
            'valid': False,
            'message': 'Server error',
            'errors': [str(e)]
        }), 500

@app.route('/api/examples', methods=['GET'])
def get_examples():
    """Return example JSON strings"""
    examples = {
        'valid': [
            '{"name": "John", "age": 30}',
            '[1, 2, 3, 4, 5]',
            '{"user": {"name": "Alice", "scores": [95, 87, 92]}}',
            '{"pi": 3.14159, "active": true, "data": null}',
            '[]',
            '{}',
        ],
        'invalid': [
            '{name: "John"}',
            "{'name': 'John'}",
            '{"name": "John,}',
            '{"name" "John"}',
            '{"name": "John}',
            '{{"nested": true}',
            '[1, 2, 3,]',
            '{"value": 007}',
        ]
    }
    return jsonify(examples)

@app.route('/api/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    # Check if Java validator is available
    try:
        result = subprocess.run(
            ['java', '-version'],
            capture_output=True,
            timeout=5
        )
        java_available = result.returncode == 0
    except:
        java_available = False
    
    return jsonify({
        'status': 'healthy' if java_available else 'degraded',
        'java_available': java_available,
        'validator_path': JAVA_CLASS_PATH
    })

if __name__ == '__main__':
    print("=" * 50)
    print("JSON Validator Server")
    print("=" * 50)
    print(f"Java classpath: {JAVA_CLASS_PATH}")
    print(f"Server running on http://localhost:5000")
    print(f"CORS enabled for all origins")
    print("=" * 50)
    app.run(debug=True, host='0.0.0.0', port=5000)
