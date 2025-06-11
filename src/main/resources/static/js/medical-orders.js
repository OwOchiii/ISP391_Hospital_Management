/**
 * JavaScript file for handling medical order functionality
 * Manages creating, updating, and viewing medical orders for appointments
 */

document.addEventListener('DOMContentLoaded', function() {
    // Get CSRF token and header
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    // Get references to UI elements
    const createOrderForm = document.querySelector('#medicalOrderModal form');
    const updateStatusLinks = document.querySelectorAll('.update-order-status');

    // Get context information from the page
    const appointmentId = getAppointmentIdFromUrl();
    const doctorId = getDoctorIdFromUrl();

    // Add event listener for the form submission
    if (createOrderForm) {
        createOrderForm.addEventListener('submit', function(event) {
            // Form will be submitted normally through the action attribute
            // No need to prevent default
            showLoadingState(createOrderForm);
        });
    }

    // Add event listeners to all status update links
    if (updateStatusLinks) {
        updateStatusLinks.forEach(function(link) {
            link.addEventListener('click', function(e) {
                e.preventDefault();

                const orderId = this.getAttribute('data-order-id');
                const status = this.getAttribute('data-status');

                if (orderId && doctorId && status) {
                    updateOrderStatus(appointmentId, orderId, doctorId, status);
                } else {
                    showAlert('Missing information needed to update status', 'error');
                }
            });
        });
    }

    /**
     * Updates the status of a medical order
     * @param {number} appointmentId - The appointment ID
     * @param {number} orderId - The medical order ID
     * @param {number} doctorId - The doctor ID
     * @param {string} status - The new status
     */
    function updateOrderStatus(appointmentId, orderId, doctorId, status) {
        // Create form data for the request
        const formData = new FormData();
        formData.append('doctorId', doctorId);
        formData.append('status', status);

        // Create headers object
        const headers = {};
        if (header && token) {
            headers[header] = token;
        }

        // Send AJAX request
        fetch(`/doctor/appointments/${appointmentId}/orders/${orderId}/update-status`, {
            method: 'POST',
            headers: headers,
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showAlert(data.message, 'success');

                // Reload the page to reflect changes
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                showAlert(`Error: ${data.message}`, 'error');
            }
        })
        .catch(error => {
            console.error('Error updating order status:', error);
            showAlert('Failed to update order status. Please try again.', 'error');
        });
    }

    /**
     * Shows an alert message to the user
     * @param {string} message - The message to display
     * @param {string} type - The type of alert ('success', 'error', 'warning', 'info')
     */
    function showAlert(message, type) {
        // Create the alert element
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert-custom alert-${type}-custom`;
        alertDiv.innerHTML = `<i class="bi bi-${type === 'success' ? 'check-circle' : 
                                               type === 'error' ? 'exclamation-triangle' : 
                                               type === 'warning' ? 'exclamation-triangle' : 
                                               'info-circle'}"></i> ${message}`;

        // Get container for the alert
        const container = document.querySelector('.appointment-container');
        if (container) {
            // Insert at the top of the container
            container.insertBefore(alertDiv, container.firstChild);

            // Auto-dismiss after 3 seconds
            setTimeout(() => {
                alertDiv.remove();
            }, 3000);
        }
    }

    /**
     * Shows loading state on a form during submission
     * @param {HTMLFormElement} form - The form being submitted
     */
    function showLoadingState(form) {
        const submitButton = form.querySelector('button[type="submit"]');
        if (submitButton) {
            // Save original text
            submitButton.dataset.originalText = submitButton.innerHTML;

            // Update to loading state
            submitButton.innerHTML = '<i class="bi bi-hourglass-split me-2"></i> Processing...';
            submitButton.disabled = true;
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
});
