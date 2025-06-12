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

    // Set filter values from URL parameters
    setFilterValuesFromURL();
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
    const patientsContainer = document.getElementById('patientsContainer');
    const emptyState = document.getElementById('emptyState');

    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            clientSideSearch(searchTerm);
        });

        searchInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && searchForm) {
                e.preventDefault();
                searchForm.submit();
            }
        });
    }

    // Client-side search function
    window.clientSideSearch = function(searchTerm) {
        if (!patientsContainer) return;

        const patientCards = patientsContainer.querySelectorAll('.patient-card');
        let visibleCount = 0;

        patientCards.forEach(card => {
            const patientName = card.querySelector('.patient-name')?.textContent.toLowerCase() || '';
            const patientId = card.querySelector('.patient-id')?.textContent.toLowerCase() || '';

            // Get all detail values (phone, email, etc.)
            const detailValues = card.querySelectorAll('.detail-value');
            let matchFound = false;

            // Check if search term is in patient name or ID
            if (patientName.includes(searchTerm) || patientId.includes(searchTerm)) {
                matchFound = true;
            } else {
                // Check if search term is in any detail value
                detailValues.forEach(detail => {
                    if (detail.textContent.toLowerCase().includes(searchTerm)) {
                        matchFound = true;
                    }
                });
            }

            if (matchFound) {
                card.style.display = 'block';
                visibleCount++;
            } else {
                card.style.display = 'none';
            }
        });

        // Show/hide empty state
        if (emptyState) {
            if (visibleCount === 0 && searchTerm !== '') {
                emptyState.style.display = 'block';
                patientsContainer.style.display = 'none';
            } else {
                emptyState.style.display = 'none';
                patientsContainer.style.display = 'grid';
            }
        }
    };
}

/**
 * Initialize the filter tabs functionality
 */
function initializeFilterTabs() {
    const filterTabs = document.querySelectorAll('.filter-tab');
    const patientsContainer = document.getElementById('patientsContainer');
    const emptyState = document.getElementById('emptyState');

    filterTabs.forEach(tab => {
        tab.addEventListener('click', function(e) {
            // For client-side filtering only - prevent default if we want to handle it here
            // e.preventDefault();

            // Extract filter value
            const filter = this.getAttribute('data-filter') ||
                          (this.href ? this.href.split('status=')[1] : null);

            // Skip client-side filtering if we want server navigation
            if (!filter) return;

            // Client-side filtering - only use when needed
            e.preventDefault();

            // Update active tab
            filterTabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            filterPatients(filter);
        });
    });

    function filterPatients(filter) {
        if (!patientsContainer) return;

        const patientCards = patientsContainer.querySelectorAll('.patient-card');
        let visibleCount = 0;

        patientCards.forEach(card => {
            const statusBadge = card.querySelector('.status-badge');
            const status = statusBadge ? statusBadge.textContent.toLowerCase() : 'active';

            let shouldShow = false;

            switch(filter) {
                case 'all':
                    shouldShow = true;
                    break;
                case 'active':
                    shouldShow = status.includes('active');
                    break;
                case 'new':
                    shouldShow = status.includes('new');
                    break;
                case 'inactive':
                    shouldShow = status.includes('inactive');
                    break;
                default:
                    shouldShow = true;
            }

            if (shouldShow) {
                card.style.display = 'block';
                visibleCount++;
            } else {
                card.style.display = 'none';
            }
        });

        // Show/hide empty state
        if (emptyState) {
            if (visibleCount === 0) {
                emptyState.style.display = 'block';
                patientsContainer.style.display = 'none';
            } else {
                emptyState.style.display = 'none';
                patientsContainer.style.display = 'grid';
            }
        }
    }

    // Make function available globally
    window.filterPatients = filterPatients;
}

/**
 * Initialize advanced filters functionality
 */
function initializeAdvancedFilters() {
    const advancedFiltersToggle = document.querySelector('[onclick="toggleAdvancedFilters()"]');
    const advancedFilters = document.getElementById('advancedFilters');
    const applyFiltersBtn = document.querySelector('[onclick="applyFilters()"]');

    // Toggle advanced filters visibility
    window.toggleAdvancedFilters = function() {
        if (advancedFilters) {
            advancedFilters.classList.toggle('show');
        }
    };

    // Apply advanced filters
    window.applyFilters = function() {
        const gender = document.getElementById('genderFilter')?.value || '';
        const age = document.getElementById('ageFilter')?.value || '';
        const lastVisit = document.getElementById('lastVisitFilter')?.value || '';
        const status = document.getElementById('statusInput')?.value || 'all';
        const doctorId = document.querySelector('input[name="doctorId"]')?.value || '';
        const searchTerm = document.getElementById('searchInput')?.value || '';

        // Build the URL with all filter parameters
        let url = `/doctor/patients?doctorId=${doctorId}`;

        if (status && status !== 'null') {
            url += `&status=${status}`;
        }

        if (gender) {
            url += `&gender=${gender}`;
        }

        if (age) {
            url += `&ageRange=${age}`;
        }

        if (lastVisit) {
            url += `&lastVisit=${lastVisit}`;
        }

        if (searchTerm) {
            url += `&searchTerm=${encodeURIComponent(searchTerm)}`;
        }

        // Navigate to the filtered URL
        window.location.href = url;
    };

    // Export patients function
    window.exportPatients = function() {
        const doctorId = document.querySelector('input[name="doctorId"]')?.value || '';
        const status = document.getElementById('statusInput')?.value || 'all';

        // Export functionality
        window.location.href = `/doctor/patients/export?doctorId=${doctorId}&status=${status}`;
    };
}

/**
 * Set filter values from URL parameters
 */
function setFilterValuesFromURL() {
    // Get URL parameters
    const urlParams = new URLSearchParams(window.location.search);

    // Set search input value from URL
    const searchTerm = urlParams.get('searchTerm');
    if (searchTerm) {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = searchTerm;
        }
    }

    // Set gender filter value from URL
    const gender = urlParams.get('gender');
    if (gender) {
        const genderFilter = document.getElementById('genderFilter');
        if (genderFilter) {
            genderFilter.value = gender;
        }
    }

    // Set age range filter value from URL
    const ageRange = urlParams.get('ageRange');
    if (ageRange) {
        const ageFilter = document.getElementById('ageFilter');
        if (ageFilter) {
            ageFilter.value = ageRange;
        }
    }

    // Set last visit filter value from URL
    const lastVisit = urlParams.get('lastVisit');
    if (lastVisit) {
        const lastVisitFilter = document.getElementById('lastVisitFilter');
        if (lastVisitFilter) {
            lastVisitFilter.value = lastVisit;
        }
    }

    // If any advanced filter is set, show the advanced filters section
    if (gender || ageRange || lastVisit) {
        const advancedFilters = document.getElementById('advancedFilters');
        if (advancedFilters) {
            advancedFilters.classList.add('show');
        }
    }
}
