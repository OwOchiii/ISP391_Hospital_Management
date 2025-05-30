/**
 * JavaScript file for handling doctor notes functionality
 * Manages saving, loading, updating and deleting notes for appointments
 */

// Initialize when document is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Get references to UI elements
    const notesTextarea = document.getElementById('doctorNotes');
    const saveNotesBtn = document.querySelector('#notes .btn-primary-custom');
    const notesContainer = document.querySelector('.notes-section');

    // Get context information from the page
    const appointmentId = getAppointmentIdFromUrl();
    const doctorId = getDoctorIdFromUrl();

    // Add click handler to the save notes button
    if (saveNotesBtn) {
        saveNotesBtn.addEventListener('click', function() {
            saveNote(appointmentId, doctorId, notesTextarea.value);
        });
    }

    // Load existing notes when the notes tab is shown
    const notesTab = document.getElementById('notes-tab');
    if (notesTab) {
        notesTab.addEventListener('shown.bs.tab', function() {
            loadNotes(appointmentId, doctorId);
        });

        // Also load on initial page load if notes tab is active
        if (notesTab.classList.contains('active')) {
            loadNotes(appointmentId, doctorId);
        }
    }

    /**
     * Extracts the appointment ID from the current URL
     * @returns {number|null} The appointment ID or null if not found
     */
    function getAppointmentIdFromUrl() {
        const urlPath = window.location.pathname;
        const pathParts = urlPath.split('/');

        // URL format is typically /doctor/appointments/{appointmentId}
        for (let i = 0; i < pathParts.length; i++) {
            if (pathParts[i] === 'appointments' && i + 1 < pathParts.length) {
                return parseInt(pathParts[i + 1]);
            }
        }

        // If not found in the path, check query parameters
        const urlParams = new URLSearchParams(window.location.search);
        const appointmentId = urlParams.get('appointmentId');
        return appointmentId ? parseInt(appointmentId) : null;
    }

    /**
     * Extracts the doctor ID from the URL query parameters
     * @returns {number|null} The doctor ID or null if not found
     */
    function getDoctorIdFromUrl() {
        const urlParams = new URLSearchParams(window.location.search);
        const doctorId = urlParams.get('doctorId');
        return doctorId ? parseInt(doctorId) : null;
    }

    /**
     * Saves a new note for an appointment
     * @param {number} appointmentId - The appointment ID
     * @param {number} doctorId - The doctor ID
     * @param {string} noteContent - The content of the note
     */
    function saveNote(appointmentId, doctorId, noteContent) {
        // Validate inputs
        if (!appointmentId || !doctorId) {
            showAlert('Error: Missing appointment or doctor information.', 'error');
            return;
        }

        if (!noteContent || noteContent.trim() === '') {
            showAlert('Please enter note content before saving.', 'warning');
            return;
        }

        // Create form data for the request
        const formData = new FormData();
        formData.append('doctorId', doctorId);
        formData.append('noteContent', noteContent);

        // Get CSRF token if available
        const csrfHeader = getCsrfHeader();

        // Create and configure the request
        fetch(`/doctor/appointments/${appointmentId}/notes/save`, {
            method: 'POST',
            body: formData,
            // Don't set Content-Type header when using FormData
            headers: csrfHeader
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                showAlert('Note saved successfully!', 'success');
                notesTextarea.value = ''; // Clear the textarea
                loadNotes(appointmentId, doctorId); // Reload notes
            } else {
                showAlert(`Error: ${data.message}`, 'error');
            }
        })
        .catch(error => {
            console.error('Error saving note:', error);
            showAlert('Failed to save note. Please try again.', 'error');
        });
    }

    /**
     * Loads all notes for an appointment
     * @param {number} appointmentId - The appointment ID
     * @param {number} doctorId - The doctor ID
     */
    function loadNotes(appointmentId, doctorId) {
        if (!appointmentId || !doctorId) {
            console.error('Missing appointment or doctor information');
            return;
        }

        // Show loading indicator
        const loadingHtml = '<div class="text-center"><i class="bi bi-hourglass-split"></i> Loading notes...</div>';
        const notesListContainer = notesContainer.querySelector('div:not(h6)');
        if (notesListContainer) {
            notesListContainer.innerHTML = loadingHtml;
        } else {
            const newContainer = document.createElement('div');
            newContainer.innerHTML = loadingHtml;
            notesContainer.appendChild(newContainer);
        }

        // Fetch notes from the server
        fetch(`/doctor/appointments/${appointmentId}/notes?doctorId=${doctorId}`, {
            method: 'GET',
            headers: getCsrfHeader()
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderNotes(data.notes);
            } else {
                showAlert(`Error: ${data.message}`, 'error');
                notesContainer.querySelector('div:not(h6)').innerHTML =
                    '<div class="alert alert-warning">Failed to load notes.</div>';
            }
        })
        .catch(error => {
            console.error('Error loading notes:', error);
            notesContainer.querySelector('div:not(h6)').innerHTML =
                '<div class="alert alert-warning">Failed to load notes.</div>';
        });
    }

    /**
     * Renders the notes in the notes section
     * @param {Array} notes - Array of note objects
     */
    function renderNotes(notes) {
        // Clear previous notes
        const notesListContainer = notesContainer.querySelector('div:not(h6)');
        if (!notesListContainer) return;

        // If no notes, show a message
        if (!notes || notes.length === 0) {
            notesListContainer.innerHTML = '<div class="alert alert-info">No notes have been added yet.</div>';
            return;
        }

        // Create HTML for notes
        let notesHtml = '';
        notes.forEach(note => {
            const createdDate = new Date(note.createdAt);
            const formattedDate = createdDate.toLocaleDateString('en-US', {
                month: 'long',
                day: 'numeric',
                year: 'numeric',
                hour: 'numeric',
                minute: 'numeric',
                hour12: true
            });

            notesHtml += `
                <div class="note-item" data-note-id="${note.noteId}">
                    <div class="note-header">
                        <span class="note-author">Dr. ${note.doctor ? note.doctor.user.fullName : 'Unknown'}</span>
                        <span class="note-timestamp">${formattedDate}</span>
                    </div>
                    <div class="note-content">${note.noteContent}</div>
                    <div class="note-actions mt-2">
                        <button class="btn btn-sm btn-outline-primary edit-note-btn">
                            <i class="bi bi-pencil-square"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-outline-danger delete-note-btn">
                            <i class="bi bi-trash"></i> Delete
                        </button>
                    </div>
                </div>
            `;
        });

        notesListContainer.innerHTML = notesHtml;

        // Add event listeners to edit and delete buttons
        notesListContainer.querySelectorAll('.edit-note-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const noteItem = this.closest('.note-item');
                const noteId = noteItem.dataset.noteId;
                const noteContent = noteItem.querySelector('.note-content').textContent;
                editNote(noteId, noteContent);
            });
        });

        notesListContainer.querySelectorAll('.delete-note-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const noteItem = this.closest('.note-item');
                const noteId = noteItem.dataset.noteId;
                deleteNote(appointmentId, noteId, doctorId);
            });
        });
    }

    /**
     * Opens a note for editing
     * @param {number} noteId - The ID of the note to edit
     * @param {string} content - The current content of the note
     */
    function editNote(noteId, content) {
        // Fill the textarea with note content
        notesTextarea.value = content;

        // Change the save button to update mode
        saveNotesBtn.innerHTML = '<i class="bi bi-save me-2"></i> Update Note';
        saveNotesBtn.dataset.mode = 'update';
        saveNotesBtn.dataset.noteId = noteId;

        // Scroll to the textarea
        notesTextarea.scrollIntoView({ behavior: 'smooth' });
        notesTextarea.focus();

        // Change the button click handler
        saveNotesBtn.removeEventListener('click', saveNotesBtn.clickHandler);
        saveNotesBtn.clickHandler = function() {
            updateNote(appointmentId, noteId, doctorId, notesTextarea.value);
        };
        saveNotesBtn.addEventListener('click', saveNotesBtn.clickHandler);
    }

    /**
     * Updates an existing note
     * @param {number} appointmentId - The appointment ID
     * @param {number} noteId - The note ID to update
     * @param {number} doctorId - The doctor ID
     * @param {string} noteContent - The updated content
     */
    function updateNote(appointmentId, noteId, doctorId, noteContent) {
        // Validate inputs
        if (!noteContent || noteContent.trim() === '') {
            showAlert('Please enter note content before saving.', 'warning');
            return;
        }

        // Create form data
        const formData = new FormData();
        formData.append('doctorId', doctorId);
        formData.append('noteContent', noteContent);

        // Send update request
        fetch(`/doctor/appointments/${appointmentId}/notes/${noteId}`, {
            method: 'PUT',
            body: formData,
            headers: getCsrfHeader()
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showAlert('Note updated successfully!', 'success');
                notesTextarea.value = '';

                // Reset the save button
                saveNotesBtn.innerHTML = '<i class="bi bi-save me-2"></i> Save Notes';
                saveNotesBtn.removeAttribute('data-mode');
                saveNotesBtn.removeAttribute('data-note-id');

                // Reset the click handler
                saveNotesBtn.removeEventListener('click', saveNotesBtn.clickHandler);
                saveNotesBtn.clickHandler = function() {
                    saveNote(appointmentId, doctorId, notesTextarea.value);
                };
                saveNotesBtn.addEventListener('click', saveNotesBtn.clickHandler);

                // Reload notes
                loadNotes(appointmentId, doctorId);
            } else {
                showAlert(`Error: ${data.message}`, 'error');
            }
        })
        .catch(error => {
            console.error('Error updating note:', error);
            showAlert('Failed to update note. Please try again.', 'error');
        });
    }

    /**
     * Deletes a note
     * @param {number} appointmentId - The appointment ID
     * @param {number} noteId - The note ID to delete
     * @param {number} doctorId - The doctor ID
     */
    function deleteNote(appointmentId, noteId, doctorId) {
        if (!confirm('Are you sure you want to delete this note? This action cannot be undone.')) {
            return;
        }

        fetch(`/doctor/appointments/${appointmentId}/notes/${noteId}?doctorId=${doctorId}`, {
            method: 'DELETE',
            headers: getCsrfHeader()
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showAlert('Note deleted successfully!', 'success');
                loadNotes(appointmentId, doctorId);
            } else {
                showAlert(`Error: ${data.message}`, 'error');
            }
        })
        .catch(error => {
            console.error('Error deleting note:', error);
            showAlert('Failed to delete note. Please try again.', 'error');
        });
    }

    /**
     * Gets CSRF token header for AJAX requests
     * @returns {Object} Headers object with CSRF token
     */
    function getCsrfHeader() {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');

        if (token && header) {
            const headerObj = {};
            headerObj[header.getAttribute('content')] = token.getAttribute('content');
            return headerObj;
        }

        return {};
    }

    /**
     * Shows an alert message
     * @param {string} message - The message to show
     * @param {string} type - The type of alert (success, error, warning, info)
     */
    function showAlert(message, type) {
        // Create alert element
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert-custom';

        // Set alert type
        switch (type) {
            case 'success':
                alertDiv.className += ' alert-success-custom';
                alertDiv.innerHTML = `<i class="bi bi-check-circle"></i> ${message}`;
                break;
            case 'error':
                alertDiv.className += ' alert-warning-custom';
                alertDiv.innerHTML = `<i class="bi bi-exclamation-triangle"></i> ${message}`;
                break;
            case 'warning':
                alertDiv.className += ' alert-warning-custom';
                alertDiv.innerHTML = `<i class="bi bi-exclamation-circle"></i> ${message}`;
                break;
            case 'info':
                alertDiv.className += ' alert-info-custom';
                alertDiv.innerHTML = `<i class="bi bi-info-circle"></i> ${message}`;
                break;
        }

        // Find the right location to insert the alert
        const mainContent = document.querySelector('.main-content-card');
        if (mainContent) {
            mainContent.insertBefore(alertDiv, mainContent.firstChild);
        } else {
            // Fallback - append to body
            document.body.appendChild(alertDiv);
        }

        // Auto-dismiss after 4 seconds
        setTimeout(() => {
            alertDiv.remove();
        }, 4000);
    }
});
