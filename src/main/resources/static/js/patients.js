// patients.js - Dedicated JavaScript for the patients.html template

document.addEventListener('DOMContentLoaded', function() {
    // Initialize sidebar state based on screen size
    checkScreenSize();

    // Add event listeners
    window.addEventListener('resize', checkScreenSize);

    // Initialize search functionality
    initializeSearch();

    // Initialize filter tabs
    initializeFilterTabs();

    // Initialize advanced filters toggle
    initializeAdvancedFilters();
});

/**
 * Check screen size and adjust sidebar visibility
 */
function checkScreenSize() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');

    if (window.innerWidth <= 768) {
        sidebar.classList.add('collapsed');
        mainContent.classList.add('expanded');
    } else {
        sidebar.classList.remove('collapsed');
        mainContent.classList.remove('expanded');
    }
}

/**
 * Toggle sidebar visibility
 */
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');

    sidebar.classList.toggle('collapsed');
    mainContent.classList.toggle('expanded');
}

/**
 * Initialize the search functionality
 */
function initializeSearch() {
    const searchInput = document.getElementById('searchInput');
    const searchForm = document.getElementById('searchForm');

    if (!searchInput || !searchForm) return;

    // Add form submit handler
    searchForm.addEventListener('submit', function(event) {
        event.preventDefault();
        performSearch();
        return false;
    });

    // Add input event for real-time filtering
    searchInput.addEventListener('input', function() {
        performSearch();
    });

    // Add clear search functionality if there's a clear button
    const clearSearchButton = document.getElementById('clearSearch');
    if (clearSearchButton) {
        clearSearchButton.addEventListener('click', function() {
            searchInput.value = '';
            performSearch();
        });
    }
}

/**
 * Perform client-side search on patients
 */
function performSearch() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase().trim();
    const patientsContainer = document.getElementById('patientsContainer');
    const emptyState = document.getElementById('emptyState');

    if (!patientsContainer || !emptyState) return;

    const patientCards = patientsContainer.querySelectorAll('.patient-card');
    let visibleCount = 0;

    // Loop through each patient card and check if it matches search term
    patientCards.forEach(card => {
        let matchFound = false;

        // Check patient name
        const patientNameEl = card.querySelector('.patient-name');
        if (patientNameEl && patientNameEl.textContent.toLowerCase().includes(searchTerm)) {
            matchFound = true;
        }

        // Check patient ID
        const patientIdEl = card.querySelector('.patient-id');
        if (patientIdEl && patientIdEl.textContent.toLowerCase().includes(searchTerm)) {
            matchFound = true;
        }

        // Check phone numbers and other details
        const detailValues = card.querySelectorAll('.detail-value');
        detailValues.forEach(detail => {
            if (detail.textContent.toLowerCase().includes(searchTerm)) {
                matchFound = true;
            }
        });

        // Set display based on match
        if (matchFound || searchTerm === '') {
            card.style.display = 'block';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    // Toggle empty state visibility
    if (visibleCount === 0 && searchTerm !== '') {
        emptyState.style.display = 'block';
        patientsContainer.style.display = 'none';
    } else {
        emptyState.style.display = 'none';
        patientsContainer.style.display = 'grid';
    }

    // Update counts display if exists
    updateFilterCounts(visibleCount);
}

/**
 * Update the filter tab counts based on visible patients
 * @param {number} visibleCount - Number of visible patients
 */
function updateFilterCounts(visibleCount) {
    const totalCountEl = document.querySelector('.filter-tab.active .badge');
    if (totalCountEl) {
        // Only update the active filter's count
        totalCountEl.textContent = visibleCount;
    }
}

/**
 * Initialize the filter tabs functionality
 */
function initializeFilterTabs() {
    const filterTabs = document.querySelectorAll('.filter-tab');

    filterTabs.forEach(tab => {
        tab.addEventListener('click', function(event) {
            // This is handled by the server-side, but we can add client-side
            // visual feedback if needed
            filterTabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

/**
 * Initialize advanced filters functionality
 */
function initializeAdvancedFilters() {
    // Get filter elements
    const genderFilter = document.getElementById('genderFilter');
    const ageFilter = document.getElementById('ageFilter');
    const lastVisitFilter = document.getElementById('lastVisitFilter');

    // Add event listeners to filter elements
    if (genderFilter) {
        genderFilter.addEventListener('change', applyFilters);
    }

    if (ageFilter) {
        ageFilter.addEventListener('change', applyFilters);
    }

    if (lastVisitFilter) {
        lastVisitFilter.addEventListener('change', applyFilters);
    }
}

/**
 * Toggle advanced filters visibility
 */
function toggleAdvancedFilters() {
    const advancedFilters = document.getElementById('advancedFilters');
    if (advancedFilters) {
        advancedFilters.classList.toggle('show');
    }
}

/**
 * Apply all filters to the patient list
 */
function applyFilters() {
    const searchTerm = document.getElementById('searchInput')?.value.toLowerCase() || '';
    const genderValue = document.getElementById('genderFilter')?.value || '';
    const ageRange = document.getElementById('ageFilter')?.value || '';
    const lastVisitValue = document.getElementById('lastVisitFilter')?.value || '';

    const patientCards = document.querySelectorAll('.patient-card');
    let visibleCount = 0;

    patientCards.forEach(card => {
        // Get patient information from the card
        const patientNameEl = card.querySelector('.patient-name');
        const patientIdEl = card.querySelector('.patient-id');
        const patientName = patientNameEl ? patientNameEl.textContent.toLowerCase() : '';
        const patientId = patientIdEl ? patientIdEl.textContent.toLowerCase() : '';

        // Extract gender from gender icon class
        const genderIcon = card.querySelector('.gender-icon');
        let gender = '';
        if (genderIcon) {
            if (genderIcon.classList.contains('gender-male')) gender = 'male';
            else if (genderIcon.classList.contains('gender-female')) gender = 'female';
            else gender = 'other';
        }

        // Extract age from the age detail element
        let age = 0;
        const ageEl = Array.from(card.querySelectorAll('.detail-label'))
            .find(el => el.textContent.includes('Age'))?.nextElementSibling;
        if (ageEl) {
            const ageText = ageEl.textContent;
            age = parseInt(ageText);
        }

        // Extract last visit from the last visit stat
        let lastVisit = '';
        const lastVisitEl = Array.from(card.querySelectorAll('.stat-label'))
            .find(el => el.textContent.includes('Last Visit'))?.previousElementSibling;
        if (lastVisitEl) {
            lastVisit = lastVisitEl.textContent.toLowerCase();
        }

        // Check if the patient matches the search term
        const matchesSearch = patientName.includes(searchTerm) || patientId.includes(searchTerm);

        // Check if the patient matches the gender filter
        const matchesGender = !genderValue ||
            (gender && gender.toLowerCase() === genderValue.toLowerCase());

        // Check if the patient matches the age range filter
        let matchesAge = true;
        if (ageRange && !isNaN(age)) {
            if (ageRange === '0-18') {
                matchesAge = age >= 0 && age <= 18;
            } else if (ageRange === '19-35') {
                matchesAge = age >= 19 && age <= 35;
            } else if (ageRange === '36-50') {
                matchesAge = age >= 36 && age <= 50;
            } else if (ageRange === '51-65') {
                matchesAge = age >= 51 && age <= 65;
            } else if (ageRange === '65+') {
                matchesAge = age >= 65;
            }
        }

        // Check if the patient matches the last visit filter
        let matchesLastVisit = true;
        if (lastVisitValue && lastVisit) {
            if (lastVisitValue === 'week' &&
                !(lastVisit.includes('today') || lastVisit.includes('yesterday') ||
                (lastVisit.includes('days ago') && parseInt(lastVisit) <= 7))) {
                matchesLastVisit = false;
            } else if (lastVisitValue === 'month' &&
                !(lastVisit.includes('today') || lastVisit.includes('yesterday') ||
                (lastVisit.includes('days ago') && parseInt(lastVisit) <= 30))) {
                matchesLastVisit = false;
            } else if (lastVisitValue === '3months' &&
                !(lastVisit.includes('today') || lastVisit.includes('yesterday') ||
                (lastVisit.includes('days ago') && parseInt(lastVisit) <= 90))) {
                matchesLastVisit = false;
            } else if (lastVisitValue === '6months' &&
                !(lastVisit.includes('today') || lastVisit.includes('yesterday') ||
                (lastVisit.includes('days ago') && parseInt(lastVisit) <= 180))) {
                matchesLastVisit = false;
            } else if (lastVisitValue === 'year' &&
                !(lastVisit.includes('today') || lastVisit.includes('yesterday') ||
                (lastVisit.includes('days ago') && parseInt(lastVisit) <= 365))) {
                matchesLastVisit = false;
            }
        }

        // Show or hide the card based on all filters
        if (matchesSearch && matchesGender && matchesAge && matchesLastVisit) {
            card.style.display = 'block';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    // Show/hide empty state
    const emptyState = document.getElementById('emptyState');
    const patientsContainer = document.getElementById('patientsContainer');

    if (visibleCount === 0) {
        if (emptyState) emptyState.style.display = 'block';
        if (patientsContainer) patientsContainer.style.display = 'none';
    } else {
        if (emptyState) emptyState.style.display = 'none';
        if (patientsContainer) patientsContainer.style.display = 'grid';
    }

    // Update filter tab counts
    updateFilterCounts(visibleCount);
}

/**
 * Export patients data (placeholder function)
 */
function exportPatients() {
    // This would typically connect to a server endpoint
    alert('Export functionality would be implemented here.');

    // Example of how this might work:
    // window.location.href = '/doctor/patients/export?doctorId=' + doctorId + '&format=csv';
}

/**
 * Apply advanced filters
 */
function applyAdvancedFilters() {
    // Get filter values
    const statusFilter = document.getElementById('statusFilter')?.value;
    const genderFilter = document.getElementById('genderFilter')?.value;
    const ageFilter = document.getElementById('ageFilter')?.value;

    // Apply filters to patient cards
    const patientCards = document.querySelectorAll('.patient-card');
    let visibleCount = 0;

    patientCards.forEach(card => {
        let statusMatch = true;
        let genderMatch = true;
        let ageMatch = true;

        // Check status match
        if (statusFilter && statusFilter !== 'all') {
            const patientStatus = card.querySelector('.status-badge')?.textContent.toLowerCase();
            statusMatch = patientStatus && patientStatus.includes(statusFilter.toLowerCase());
        }

        // Check gender match
        if (genderFilter && genderFilter !== 'all') {
            const genderIcon = card.querySelector('.gender-icon');
            genderMatch = genderIcon && genderIcon.classList.contains('gender-' + genderFilter.toLowerCase());
        }

        // Check age match
        if (ageFilter && ageFilter !== 'all') {
            const ageText = Array.from(card.querySelectorAll('.detail-value'))
                .find(el => el.previousElementSibling?.textContent.includes('Age'))?.textContent;

            if (ageText) {
                const age = parseInt(ageText);

                switch(ageFilter) {
                    case 'under18':
                        ageMatch = age < 18;
                        break;
                    case '18to35':
                        ageMatch = age >= 18 && age <= 35;
                        break;
                    case '36to50':
                        ageMatch = age >= 36 && age <= 50;
                        break;
                    case 'over50':
                        ageMatch = age > 50;
                        break;
                }
            } else {
                ageMatch = false;
            }
        }

        // Apply combined filters
        if (statusMatch && genderMatch && ageMatch) {
            card.style.display = 'block';
            visibleCount++;
        } else {
            card.style.display = 'none';
        }
    });

    // Show/hide empty state
    const emptyState = document.getElementById('emptyState');
    const patientsContainer = document.getElementById('patientsContainer');

    if (visibleCount === 0) {
        emptyState.style.display = 'block';
        patientsContainer.style.display = 'none';
    } else {
        emptyState.style.display = 'none';
        patientsContainer.style.display = 'grid';
    }
}

/**
 * Reset advanced filters
 */
function resetAdvancedFilters() {
    // Reset all filter inputs
    const filterInputs = document.querySelectorAll('#advancedFilters select');
    filterInputs.forEach(input => {
        input.value = 'all';
    });

    // Show all patient cards
    const patientCards = document.querySelectorAll('.patient-card');
    patientCards.forEach(card => {
        card.style.display = 'block';
    });

    // Hide empty state
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('patientsContainer').style.display = 'grid';
}