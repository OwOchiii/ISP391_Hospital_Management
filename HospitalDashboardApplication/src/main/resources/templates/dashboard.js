document.addEventListener('DOMContentLoaded', function() {
    // Theme Toggle
    const themeToggle = document.getElementById('themeToggle');
    const body = document.body;

    themeToggle.addEventListener('click', () => {
        body.classList.toggle('dark-theme');
        const isDarkTheme = body.classList.contains('dark-theme');
        themeToggle.innerHTML = `<i class="fas ${isDarkTheme ? 'fa-sun' : 'fa-moon'}"></i>`;
    });

    // Patient Chart
    const statusCounts = /*[[${statusCounts}]]*/ [];
    const labels = statusCounts.map(item => item[0]); // Status names
    const data = statusCounts.map(item => item[1]);   // Counts
    const colors = [
        'rgba(59, 130, 246, 0.6)',  // Blue for Pending
        'rgba(16, 185, 129, 0.6)', // Green for Confirmed
        'rgba(239, 68, 68, 0.6)',   // Red for Outpatient
        'rgba(245, 158, 11, 0.6)'   // Amber for Rejected
    ];
    const borderColors = colors.map(color => color.replace('0.6', '1'));

    const patientChart = new Chart(document.getElementById('patientChart'), {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Patient Status',
                data: data,
                backgroundColor: colors.slice(0, labels.length),
                borderColor: borderColors.slice(0, labels.length),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: body.classList.contains('dark-theme') ? '#e2e8f0' : '#1e293b'
                    },
                    grid: {
                        color: body.classList.contains('dark-theme') ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
                    }
                },
                x: {
                    ticks: {
                        color: body.classList.contains('dark-theme') ? '#e2e8f0' : '#1e293b'
                    },
                    grid: {
                        color: body.classList.contains('dark-theme') ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
                    }
                }
            },
            plugins: {
                legend: {
                    labels: {
                        color: body.classList.contains('dark-theme') ? '#e2e8f0' : '#1e293b'
                    }
                }
            }
        }
    });

    // Update chart colors on theme change
    themeToggle.addEventListener('click', () => {
        const isDarkTheme = body.classList.contains('dark-theme');
        patientChart.options.scales.y.ticks.color = isDarkTheme ? '#e2e8f0' : '#1e293b';
        patientChart.options.scales.x.ticks.color = isDarkTheme ? '#e2e8f0' : '#1e293b';
        patientChart.options.scales.y.grid.color = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';
        patientChart.options.scales.x.grid.color = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';
        patientChart.options.plugins.legend.labels.color = isDarkTheme ? '#e2e8f0' : '#1e293b';
        patientChart.update();
    });

    // Appointment Verification
    document.querySelectorAll('.btn-approve, .btn-reject').forEach(button => {
        button.addEventListener('click', function() {
            const appointmentId = this.dataset.id;
            const statusName = this.classList.contains('btn-approve') ? 'Confirmed' : 'Rejected';

            console.log(`Sending AJAX: id=${appointmentId}, status=${statusName}`);
            fetch(`/appointments/${appointmentId}/verify`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `statusName=${encodeURIComponent(statusName)}`
            })
                .then(response => response.text())
                .then(data => {
                    console.log(`Response: ${data}`);
                    if (data === 'success') {
                        const row = this.closest('.table-row');
                        const badge = row.querySelector('.status-badge');
                        badge.textContent = statusName;
                        badge.className = `status-badge status-${statusName.toLowerCase()}`;
                    } else {
                        alert(`Error: ${data}`);
                    }
                })
                .catch(error => console.error('Error:', error));
        });
    });

    // Appointment Deletion
    document.querySelectorAll('.btn-delete').forEach(button => {
        button.addEventListener('click', function() {
            if (confirm('Are you sure you want to delete this appointment?')) {
                const appointmentId = this.dataset.id;

                fetch(`/appointments/${appointmentId}`, {
                    method: 'DELETE'
                })
                    .then(response => response.text())
                    .then(data => {
                        if (data === 'success') {
                            this.closest('.table-row').remove();
                        } else {
                            alert('Error: Failed to delete appointment');
                        }
                    })
                    .catch(error => console.error('Error:', error));
            }
        });
    });
});