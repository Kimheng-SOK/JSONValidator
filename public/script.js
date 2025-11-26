// Change this to your ngrok URL when using ngrok
// Example: const API_URL = 'https://xxxx-xx-xx-xx-xx.ngrok-free.app/api';
const API_URL = window.location.origin === 'http://localhost:5000' 
    ? 'http://localhost:5000/api' 
    : `${window.location.origin}/api`;

let examples = null;

// Load examples on page load
async function loadExamples() {
    try {
        const response = await fetch(`${API_URL}/examples`);
        examples = await response.json();
    } catch (error) {
        console.error('Failed to load examples:', error);
    }
}

// Load example JSON
function loadExample(type) {
    if (!examples) {
        alert('Examples not loaded yet');
        return;
    }
    
    const exampleList = type === 'valid' ? examples.valid : examples.invalid;
    const randomIndex = Math.floor(Math.random() * exampleList.length);
    const example = exampleList[randomIndex];
    
    document.getElementById('jsonInput').value = example;
    document.getElementById('jsonInput').dispatchEvent(new Event('input')); // Trigger visual update
}

// Clear input
function clearInput() {
    document.getElementById('jsonInput').value = '';
    document.getElementById('fileInput').value = ''; 
    document.getElementById('resultContainer').innerHTML = `
        <div class="placeholder">
            <span class="material-icons">data_object</span>
            <p>Enter JSON or upload a file to start validation.</p>
        </div>
    `;
    document.getElementById('statusBadge').textContent = '';
    document.getElementById('statusBadge').className = 'status-badge';
    document.getElementById('errorList').classList.add('hidden');
    document.getElementById('dropMessage').classList.remove('hidden-message'); // Show message
}

// Function to read the file content locally and populate the textarea
function processFile(file) {
    if (!file) return;

    if (file.type !== 'application/json' && !file.name.endsWith('.json')) {
        alert('Please select or drop a valid JSON file.');
        return;
    }

    const reader = new FileReader();
    const jsonInput = document.getElementById('jsonInput');
    
    reader.onload = function(e) {
        try {
            const content = e.target.result;
            jsonInput.value = content;
            // Manually trigger the input event to update the drop message visibility
            jsonInput.dispatchEvent(new Event('input')); 
        } catch (error) {
            alert('Error reading file content.');
            console.error('File Read Error:', error);
        }
    };
    
    reader.onerror = function() {
        alert('Failed to read the file.');
    };

    reader.readAsText(file);
}


// Handler for the click-to-upload button
function handleFileUpload(event) {
    const file = event.target.files[0];
    if (file) {
        processFile(file);
    }
    // Clear file input value to allow the same file to be uploaded again (if needed)
    event.target.value = ''; 
}


// **New Functionality: Drag and Drop Setup**
function setupDragAndDrop() {
    const dropZone = document.getElementById('dropZone');
    const jsonInput = document.getElementById('jsonInput');
    const dropMessage = document.getElementById('dropMessage');
    
    // Toggle the drop message visibility when the user types/pastes JSON
    const toggleDropMessage = () => {
        if (jsonInput.value.trim() === '') {
            dropMessage.classList.remove('hidden-message');
        } else {
            dropMessage.classList.add('hidden-message');
        }
    };
    
    jsonInput.addEventListener('input', toggleDropMessage);
    toggleDropMessage(); // Set initial state
    
    // 1. Drag Enter/Over: Prevent default and add visual feedback
    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.add('dragover');
        }, false);
    });

    // 2. Drag Leave/Drop: Remove visual feedback
    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            dropZone.classList.remove('dragover');
        }, false);
    });
    
    // 3. Drop: Handle the file
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            processFile(files[0]);
        }
        
    }, false);
}
// **End Drag and Drop Setup**

// **NEW FUNCTION: REAL VISITOR COUNTING VIA API**
async function loadVisitorCount() {
    const visitorCountElement = document.getElementById('visitorCount');
    
    // Step 1: Notify the server of a new visit (POST request)
    // The server handles incrementing the count persistently.
    try {
        await fetch(`${API_URL}/visitor_count`, {
            method: 'POST'
        });
    } catch (error) {
        console.error('Visitor Counter Increment Error:', error);
        // Continue to retrieval even if increment fails (might be a permission issue)
    }

    // Step 2: Retrieve the total count (GET request)
    try {
        const response = await fetch(`${API_URL}/visitor_count`);
        
        if (!response.ok) {
            throw new Error(`Failed to retrieve visitor count (Status: ${response.status})`);
        }

        // The server is expected to return: { "count": 123456 }
        const data = await response.json();
        const count = data.count;

        // Format the number with commas for readability (e.g., 123,456)
        const formattedCount = new Number(count).toLocaleString();

        // Update the HTML
        visitorCountElement.innerHTML = `Total Visits: <strong>${formattedCount}</strong>`;
        
    } catch (error) {
        console.error('Visitor Counter Retrieval Error:', error);
        visitorCountElement.innerHTML = `Total Visits: <span style="color: ${document.querySelector(':root').style.getPropertyValue('--error-color') || 'red'};">Tracking Offline</span>`;
    }
}


// Validate JSON (existing logic remains the same, it uses the content of jsonInput)
async function validateJSON() {
    const input = document.getElementById('jsonInput').value.trim();
    const resultContainer = document.getElementById('resultContainer');
    const statusBadge = document.getElementById('statusBadge');
    const errorList = document.getElementById('errorList');
    
    if (!input) {
        alert('Please enter JSON or upload a file to validate');
        return;
    }
    
    // Show loading state
    resultContainer.innerHTML = `
        <div class="placeholder">
            <div class="loading"></div>
            <p>Validating...</p>
        </div>
    `;
    statusBadge.textContent = 'Processing...';
    statusBadge.className = 'status-badge';
    errorList.classList.add('hidden'); // Hide errors before validation

    
    try {
        // Send the JSON string to the backend for validation
        const response = await fetch(`${API_URL}/validate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ json: input })
        });
        
        const result = await response.json();
        
        // --- NEW: Dynamic Result Display ---
        if (response.ok && result.valid) {
            statusBadge.textContent = '✅ Valid';
            statusBadge.className = 'status-badge valid';
            
            // Success Card
            resultContainer.innerHTML = `
                <div class="result-card valid">
                    <span class="material-icons icon">check_circle</span>
                    <h3>Validation Successful!</h3>
                    <p>${result.message || 'The structure and syntax are perfectly valid JSON.'}</p>
                </div>
            `;
            
            errorList.classList.add('hidden');

        } else if (response.ok && !result.valid) {
            statusBadge.textContent = '❌ Invalid';
            statusBadge.className = 'status-badge invalid';
            
            // Invalid Card
            resultContainer.innerHTML = `
                <div class="result-card invalid">
                    <span class="material-icons icon">cancel</span>
                    <h3>Validation Failed</h3>
                    <p>${result.message || 'Syntax or structure errors were detected in your JSON.'}</p>
                </div>
            `;
            
            // Show error list
            if (result.errors && result.errors.length > 0) {
                errorList.innerHTML = `
                    <h4>Detailed Errors Found (${result.errors.length}):</h4>
                    <ul>
                        ${result.errors.map(error => `<li>${error}</li>`).join('')}
                    </ul>
                `;
                errorList.classList.remove('hidden');
            } else {
                errorList.classList.add('hidden');
            }
        } else {
             // Handle HTTP errors (e.g., 404, 500 from the backend API itself)
            throw new Error(`Server returned HTTP ${response.status}: ${response.statusText}`);
        }
        
    } catch (error) {
        console.error('Validation error:', error);
        statusBadge.textContent = '⚠️ Error';
        statusBadge.className = 'status-badge invalid';
        
        // Server Error Card
        resultContainer.innerHTML = `
            <div class="result-card server-error">
                <span class="material-icons icon">warning</span>
                <h3>API/Network Error</h3>
                <p>Failed to connect to the validation service. Please check the API status.</p>
            </div>
        `;
        
        errorList.innerHTML = `
            <h4>Error Details:</h4>
            <ul>
                <li>${error.message}</li>
            </ul>
        `;
        errorList.classList.remove('hidden');
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    loadExamples();
    setupDragAndDrop(); // Initialize the drag and drop feature
    loadVisitorCount(); // Load the simulated visitor count
    
    // Add enter key support for textarea (Ctrl+Enter to validate)
    document.getElementById('jsonInput').addEventListener('keydown', (e) => {
        if (e.ctrlKey && e.key === 'Enter') {
            validateJSON();
        }
    });
});