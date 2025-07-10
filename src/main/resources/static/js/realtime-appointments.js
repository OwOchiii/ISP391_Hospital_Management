/**
 * Real-time Appointment Management for Dashboard
 * Handles loading and displaying today's appointment requests
 */

class RealTimeAppointments {
    constructor() {
        this.appointmentData = [];
        this.currentPage = 1;
        this.itemsPerPage = 5;
        this.isLoading = false;

        this.init();
    }

    init() {
        console.log('Initializing Appointments Manager');
        this.bindEvents();
        this.loadTodayAppointments(); // Load once without auto-refresh
    }

    bindEvents() {
        // Search functionality
        const searchInput = document.getElementById('patientSearch');
        if (searchInput) {
            searchInput.addEventListener('input', this.debounce(() => {
                this.searchPatients();
            }, 300));
        }

        // Manual refresh button
        const refreshBtn = document.getElementById('manualRefreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => {
                this.manualRefreshAppointments();
            });
        }
    }

    // Get today's date in Vietnamese timezone
    getTodayDate() {
        const now = new Date();
        const vietnamTime = new Date(now.getTime() + (7 * 60 * 60 * 1000));
        return vietnamTime.toISOString().split('T')[0];
    }

    // Load today's appointments (no auto-refresh)
    async loadTodayAppointments() {
        if (this.isLoading) {
            console.log('Already loading appointments, skipping...');
            return;
        }

        this.isLoading = true;
        const today = this.getTodayDate();

        console.log(`Loading appointments for today: ${today}`);

        // Show loading indicator
        this.showLoadingState();

        try {
            const response = await fetch(`/receptionist/pendingAppointmentRequest/today?date=${today}&timestamp=${Date.now()}`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: Failed to fetch appointment data`);
            }

            const data = await response.json();
            console.log('Received appointment data:', data);

            // Filter appointments for today only
            const todayAppointments = this.filterTodayAppointments(data, today);
            console.log(`Filtered ${todayAppointments.length} appointments for today from ${data.length} total`);

            this.appointmentData = todayAppointments;
            this.currentPage = 1;

            this.renderAppointmentTable(this.appointmentData, this.currentPage);
            this.updateAppointmentRequestsCount(todayAppointments.length);
            this.updateDashboardTitle(todayAppointments.length);

        } catch (error) {
            console.error('Error loading appointments:', error);
            this.showErrorState(error.message);
            this.showNotification('Failed to load appointments: ' + error.message, 'error');
        } finally {
            this.isLoading = false;
        }
    }

    // Enhanced filtering for today's appointments
    filterTodayAppointments(data, today) {
        return data.filter(appointment => {
            // Check multiple possible date fields
            const dateFields = [
                appointment.appointmentDateTime,
                appointment.dateTime,
                appointment.date,
                appointment.scheduledDate,
                appointment.visitDate,
                appointment.appointmentDate
            ];

            for (let dateField of dateFields) {
                if (dateField) {
                    try {
                        let appointmentDate;

                        if (typeof dateField === 'string') {
                            if (dateField.includes('T')) {
                                // ISO datetime format: 2025-07-03T10:30:00
                                appointmentDate = dateField.split('T')[0];
                            } else if (dateField.includes(' ')) {
                                // DateTime format: 2025-07-03 10:30:00
                                appointmentDate = dateField.split(' ')[0];
                            } else if (dateField.match(/^\d{4}-\d{2}-\d{2}$/)) {
                                // Date only format: 2025-07-03
                                appointmentDate = dateField;
                            } else {
                                // Try to parse as date
                                appointmentDate = new Date(dateField).toISOString().split('T')[0];
                            }
                        } else {
                            // Assume it's a Date object or timestamp
                            appointmentDate = new Date(dateField).toISOString().split('T')[0];
                        }

                        if (appointmentDate === today) {
                            return true;
                        }
                    } catch (error) {
                        console.warn('Error parsing date field:', dateField, error);
                    }
                }
            }
            return false;
        });
    }

    // Render appointment table with text confirm button
    renderAppointmentTable(data, page = 1) {
        const tableBody = document.getElementById('appointmentTableBody');
        if (!tableBody) return;

        tableBody.innerHTML = '';

        if (data.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-4">
                        <i class="fas fa-calendar-check fa-2x mb-2 d-block"></i>
                        No appointments scheduled for today
                    </td>
                </tr>
            `;
            this.updatePaginationInfo(1, 1);
            return;
        }

        const start = (page - 1) * this.itemsPerPage;
        const end = start + this.itemsPerPage;
        const paginatedData = data.slice(start, end);

        paginatedData.forEach(row => {
            const tr = document.createElement('tr');
            tr.className = 'appointment-row';
            tr.innerHTML = `
                <td class="fw-medium">${row.name || 'N/A'}</td>
                <td>${this.formatDate(row.date || row.appointmentDateTime || row.dateTime)}</td>
                <td>${this.formatTime(row.time || this.extractTime(row.appointmentDateTime || row.dateTime))}</td>
                <td>
                    <span class="badge ${this.getStatusBadgeClass(row.status)}">${row.status || 'Pending'}</span>
                </td>
                <td>
                    <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-primary" onclick="viewAppointmentDetails(${row.id})" title="View Details">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-success" onclick="showConfirmDialog(${row.id}, '${row.name || 'Patient'}')" title="Confirm Appointment">
                            Confirm
                        </button>
                    </div>
                </td>
            `;
            tableBody.appendChild(tr);
        });

        const totalPages = Math.ceil(data.length / this.itemsPerPage);
        this.updatePaginationInfo(page, totalPages);
    }

    // Update pagination controls
    updatePaginationInfo(currentPage, totalPages) {
        const pageInfo = document.getElementById('pageInfo');
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');

        if (pageInfo) pageInfo.textContent = `Page ${currentPage} of ${totalPages}`;
        if (prevBtn) prevBtn.disabled = currentPage === 1;
        if (nextBtn) nextBtn.disabled = currentPage === totalPages;
    }

    // Enhanced search functionality
    searchPatients() {
        const searchInput = document.getElementById('patientSearch');
        if (!searchInput) return;

        const searchTerm = searchInput.value.toLowerCase().trim();

        if (!searchTerm) {
            this.renderAppointmentTable(this.appointmentData, 1);
            this.hideSearchResults();
            return;
        }

        const filteredData = this.appointmentData.filter(row => {
            const searchFields = [
                row.name,
                row.phone,
                row.patientId,
                row.id?.toString(),
                row.gender,
                row.status
            ];

            return searchFields.some(field =>
                field && field.toString().toLowerCase().includes(searchTerm)
            );
        });

        this.currentPage = 1;
        this.renderAppointmentTable(filteredData, this.currentPage);
        this.showSearchResults(filteredData.length, searchTerm);
    }

    // UI Update methods (removed last updated time)
    updateDashboardTitle(count) {
        const now = new Date();
        const vietnamTime = new Date(now.getTime() + (7 * 60 * 60 * 1000));
        const today = vietnamTime.toLocaleDateString('vi-VN', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        const titleElement = document.querySelector('.card-title');
        if (titleElement && titleElement.textContent.includes('Appointment Requests')) {
            // Remove existing badges
            const existingBadges = titleElement.querySelectorAll('.badge');
            existingBadges.forEach(badge => badge.remove());

            titleElement.innerHTML = `
                Today's Appointment Requests 
                <small class="text-muted d-block">${today}</small>
                ${count > 0 ? `<span class="badge bg-warning ms-2">${count} pending</span>` : '<span class="badge bg-success ms-2">All clear</span>'}
            `;
        }
    }

    updateAppointmentRequestsCount(count) {
        // Update notification badge if exists
        const notificationCount = document.getElementById('notificationCount');
        if (notificationCount) {
            notificationCount.textContent = count;
            notificationCount.style.display = count > 0 ? 'block' : 'none';
        }
    }

    // UI State methods
    showLoadingState() {
        const tableBody = document.getElementById('appointmentTableBody');
        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-3">
                        <i class="fas fa-spinner fa-spin"></i> Loading today's appointments...
                    </td>
                </tr>
            `;
        }
    }

    showErrorState(message) {
        const tableBody = document.getElementById('appointmentTableBody');
        if (tableBody) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-danger py-3">
                        <i class="fas fa-exclamation-triangle"></i> 
                        Error: ${message}
                        <button class="btn btn-sm btn-outline-primary ms-2" onclick="realTimeAppointments.loadTodayAppointments()">
                            <i class="fas fa-redo"></i> Retry
                        </button>
                    </td>
                </tr>
            `;
        }
    }

    showSearchResults(count, term) {
        let resultsInfo = document.getElementById('searchResultsInfo');
        if (!resultsInfo) {
            const searchContainer = document.getElementById('patientSearch').parentElement;
            resultsInfo = document.createElement('div');
            resultsInfo.id = 'searchResultsInfo';
            resultsInfo.className = 'small text-muted mt-1';
            searchContainer.appendChild(resultsInfo);
        }
        resultsInfo.textContent = `Found ${count} results for "${term}"`;
        resultsInfo.style.display = 'block';
    }

    hideSearchResults() {
        const resultsInfo = document.getElementById('searchResultsInfo');
        if (resultsInfo) {
            resultsInfo.style.display = 'none';
        }
    }

    // Utility methods
    formatDate(dateString) {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('vi-VN');
        } catch {
            return dateString;
        }
    }

    formatTime(timeString) {
        if (!timeString) return 'N/A';
        try {
            if (timeString.includes(':')) {
                const [hours, minutes] = timeString.split(':');
                const time = new Date();
                time.setHours(parseInt(hours), parseInt(minutes));
                return time.toLocaleTimeString('vi-VN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    hour12: false
                });
            }
            return timeString;
        } catch {
            return timeString;
        }
    }

    extractTime(dateTimeString) {
        if (!dateTimeString) return null;
        try {
            if (dateTimeString.includes('T')) {
                return dateTimeString.split('T')[1].substring(0, 5);
            } else if (dateTimeString.includes(' ')) {
                return dateTimeString.split(' ')[1].substring(0, 5);
            }
        } catch {
            return null;
        }
        return null;
    }

    getStatusBadgeClass(status) {
        const statusClasses = {
            'pending': 'bg-warning',
            'confirmed': 'bg-success',
            'cancelled': 'bg-danger',
            'completed': 'bg-info'
        };
        return statusClasses[status?.toLowerCase()] || 'bg-warning';
    }

    manualRefreshAppointments() {
        this.showNotification('Refreshing appointments...', 'info');
        this.loadTodayAppointments();
    }

    showNotification(message, type) {
        // Use existing notification system if available
        if (window.showNotification) {
            window.showNotification(message, type);
        } else {
            console.log(`${type.toUpperCase()}: ${message}`);
        }
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Page navigation
    changePage(direction) {
        this.currentPage += direction;
        this.renderAppointmentTable(this.appointmentData, this.currentPage);
    }
}

// Initialize when DOM is loaded
let realTimeAppointments;
document.addEventListener('DOMContentLoaded', () => {
    realTimeAppointments = new RealTimeAppointments();
});

// Global functions for backward compatibility
function changePage(direction) {
    if (realTimeAppointments) {
        realTimeAppointments.changePage(direction);
    }
}

function searchPatients() {
    if (realTimeAppointments) {
        realTimeAppointments.searchPatients();
    }
}

// Global function for confirmation dialog
function showConfirmDialog(appointmentId, patientName) {
    if (confirm(`Are you sure you want to confirm the appointment for ${patientName}?`)) {
        confirmAppointment(appointmentId);
    }
}

// Existing confirmAppointment function (to be called after confirmation)
function confirmAppointment(appointmentId) {
    fetch('/receptionist/confirmAppointment', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `id=${appointmentId}`
    })
    .then(response => response.text())
    .then(result => {
        if (result.includes('successfully')) {
            alert('Appointment confirmed successfully!');
            // Reload appointments to reflect changes
            if (realTimeAppointments) {
                realTimeAppointments.loadTodayAppointments();
            }
        } else {
            alert('Error confirming appointment: ' + result);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error confirming appointment');
    });
}
