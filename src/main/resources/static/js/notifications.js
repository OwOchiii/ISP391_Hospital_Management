/**
 * Notifications module for handling notification fetching and display
 */

// Global variable to store the current user ID
let currentUserId = null;

/**
 * Set the current user ID for notifications
 * @param {number} userId - The ID of the current user
 */
function setCurrentUser(userId) {
    currentUserId = userId;
}

/**
 * Fetch notifications for the current user
 * @returns {Promise<Array>} Promise resolving to array of notification objects
 */
async function fetchNotifications() {
    if (!currentUserId) {
        console.error('Cannot fetch notifications: No user ID set');
        return [];
    }

    try {
        const response = await fetch(`/api/notifications?userId=${currentUserId}`);

        if (!response.ok) {
            throw new Error(`Failed to fetch notifications: ${response.status} ${response.statusText}`);
        }

        const notifications = await response.json();
        return notifications;
    } catch (error) {
        console.error('Error fetching notifications:', error);
        return [];
    }
}

/**
 * Mark a notification as read
 * @param {number} notificationId - The ID of the notification to mark as read
 * @returns {Promise<boolean>} Promise resolving to true if successful, false otherwise
 */
async function markNotificationAsRead(notificationId) {
    try {
        const response = await fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        return response.ok;
    } catch (error) {
        console.error('Error marking notification as read:', error);
        return false;
    }
}

/**
 * Render notifications in the notification container
 * @param {string} containerId - The ID of the HTML element to render notifications in
 */
async function renderNotifications(containerId = 'notification-container') {
    const container = document.getElementById(containerId);
    if (!container) {
        console.error(`Notification container with ID '${containerId}' not found`);
        return;
    }

    const notifications = await fetchNotifications();

    // Clear existing notifications
    container.innerHTML = '';

    if (notifications.length === 0) {
        container.innerHTML = '<div class="empty-notification">No notifications</div>';
        return;
    }

    // Update notification badge count
    const badges = document.querySelectorAll('.notification-badge');
    const unreadCount = notifications.filter(notification => !notification.read).length;

    badges.forEach(badge => {
        badge.textContent = unreadCount;
        badge.style.display = unreadCount > 0 ? 'flex' : 'none';
    });

    // Create notification elements
    notifications.forEach(notification => {
        const notificationElement = document.createElement('div');
        notificationElement.className = `notification-item ${notification.read ? 'read' : 'unread'}`;
        notificationElement.dataset.id = notification.notificationId;

        const createdAt = new Date(notification.createdAt);
        const formattedDate = createdAt.toLocaleString();

        notificationElement.innerHTML = `
            <div class="notification-type">${notification.type}</div>
            <div class="notification-message">${notification.message}</div>
            <div class="notification-time">${formattedDate}</div>
        `;

        notificationElement.addEventListener('click', async () => {
            if (!notification.read) {
                const success = await markNotificationAsRead(notification.notificationId);
                if (success) {
                    notification.read = true;
                    notificationElement.classList.remove('unread');
                    notificationElement.classList.add('read');

                    // Update badge count
                    const newUnreadCount = unreadCount - 1;
                    badges.forEach(badge => {
                        badge.textContent = newUnreadCount;
                        badge.style.display = newUnreadCount > 0 ? 'flex' : 'none';
                    });
                }
            }
        });

        container.appendChild(notificationElement);
    });
}

/**
 * Initialize notifications system
 * @param {number} userId - The ID of the current user
 * @param {string} containerId - The ID of the HTML element to render notifications in
 */
function initNotifications(userId, containerId = 'notification-container') {
    setCurrentUser(userId);

    // Initial render
    renderNotifications(containerId);

    // Setup refresh interval (every 30 seconds)
    setInterval(() => {
        renderNotifications(containerId);
    }, 30000);

    // Setup notification button toggle
    const notificationButtons = document.querySelectorAll('.notification-btn');
    const notificationPanel = document.getElementById(containerId);

    if (notificationButtons.length > 0 && notificationPanel) {
        notificationButtons.forEach(button => {
            button.addEventListener('click', () => {
                notificationPanel.classList.toggle('show');
                renderNotifications(containerId); // Refresh notifications when panel is opened
            });
        });

        // Close notification panel when clicking outside
        document.addEventListener('click', (event) => {
            if (!event.target.closest('.notification-btn') &&
                !event.target.closest(`#${containerId}`) &&
                notificationPanel.classList.contains('show')) {
                notificationPanel.classList.remove('show');
            }
        });
    }
}

// Export functions for use in other scripts
window.NotificationSystem = {
    init: initNotifications,
    refresh: renderNotifications,
    markAsRead: markNotificationAsRead
};
