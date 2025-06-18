// Register page functionality
let currentStep = 1;

function onCaptchaSuccess(token) {
    document.querySelector('input[name="g-recaptcha-response"]').value = token;
    console.log('CAPTCHA verified successfully');
}

function onCaptchaExpired() {
    document.querySelector('input[name="g-recaptcha-response"]').value = '';
    console.log('CAPTCHA verification expired');
}

function onCaptchaError() {
    document.querySelector('input[name="g-recaptcha-response"]').value = '';
    console.log('CAPTCHA verification error');
}

// Step navigation
function nextStep(step) {
    if (validateStep(step)) {
        currentStep++;
        showStep(currentStep);
        updateStepIndicator();
    }
}

function prevStep(step) {
    currentStep--;
    showStep(currentStep);
    updateStepIndicator();
}

function showStep(step) {
    document.querySelectorAll('.form-step').forEach(stepDiv => {
        stepDiv.style.display = 'none';
    });
    document.getElementById(`formStep${step}`).style.display = 'block';
}

function updateStepIndicator() {
    for (let i = 1; i <= 2; i++) {
        const stepElement = document.getElementById(`step${i}`);
        stepElement.classList.remove('active', 'completed');

        if (i < currentStep) {
            stepElement.classList.add('completed');
        } else if (i === currentStep) {
            stepElement.classList.add('active');
        }
    }
}

// Step validation
function validateStep(step) {
    if (step === 1) {
        const requiredFields = ['fullName', 'email', 'phoneNumber'];
        let isValid = true;

        // Clear existing alerts first
        hideAlerts();

        for (const fieldId of requiredFields) {
            const field = document.getElementById(fieldId);
            if (!field.value.trim()) {
                field.classList.add('is-invalid');
                isValid = false;
            } else {
                field.classList.remove('is-invalid');
                field.classList.add('is-valid');
            }
        }

        if (!isValid) {
            showAlert('error', 'Please fill in all required fields.');
            return false;
        }

        // Full name validation - only letters, spaces, and some common name characters
        const fullName = document.getElementById('fullName');
        const nameRegex = /^[A-Za-z\s'.,-]+$/;
        if (!nameRegex.test(fullName.value)) {
            fullName.classList.add('is-invalid');
            showAlert('error', 'Full name should only contain letters, spaces, and common name characters.');
            return false;
        }

        // Email validation
        const email = document.getElementById('email');
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email.value)) {
            email.classList.add('is-invalid');
            showAlert('error', 'Please enter a valid email address.');
            return false;
        }

        // Phone validation - only numbers and + sign
        const phone = document.getElementById('phoneNumber');
        const phoneRegex = /^\+?[\d]{10,15}$/;
        if (!phoneRegex.test(phone.value.replace(/\s/g, ''))) {
            phone.classList.add('is-invalid');
            showAlert('error', 'Please enter a valid phone number. Only numbers and + sign are allowed.');
            return false;
        }
    }
    return true;
}

// Form submission
document.addEventListener('DOMContentLoaded', function() {
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault(); // Always prevent default to validate first

            // Clear existing alerts
            const existingAlerts = document.querySelectorAll('.alert-custom');
            existingAlerts.forEach(alert => alert.remove());

            // Form validation flags
            let isValid = true;
            let errorMessage = '';

            // Validate all required fields across both steps
            const requiredFields = ['fullName', 'email', 'phoneNumber', 'password', 'confirmPassword'];
            for (const fieldId of requiredFields) {
                const field = document.getElementById(fieldId);
                if (!field.value.trim()) {
                    field.classList.add('is-invalid');
                    isValid = false;
                    errorMessage = 'Please fill in all required fields.';
                } else {
                    field.classList.remove('is-invalid');
                }
            }

            // Full name validation - only letters, spaces, and some common name characters
            const fullName = document.getElementById('fullName');
            const nameRegex = /^[A-Za-z\s'.,-]+$/;
            if (fullName.value.trim() && !nameRegex.test(fullName.value)) {
                fullName.classList.add('is-invalid');
                isValid = false;
                errorMessage = 'Full name should only contain letters, spaces, and common name characters.';
            }

            // Email validation
            const email = document.getElementById('email');
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (email.value.trim() && !emailRegex.test(email.value)) {
                email.classList.add('is-invalid');
                isValid = false;
                errorMessage = 'Please enter a valid email address.';
            }

            // Phone validation - only numbers and + sign
            const phone = document.getElementById('phoneNumber');
            const phoneRegex = /^\+?[\d]{10,15}$/;
            if (phone.value.trim() && !phoneRegex.test(phone.value.replace(/\s/g, ''))) {
                phone.classList.add('is-invalid');
                isValid = false;
                errorMessage = 'Please enter a valid phone number. Only numbers and + sign are allowed.';
            }

            // Password validation
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            // Check password requirements
            const requirements = {
                length: password.length >= 8,
                uppercase: /[A-Z]/.test(password),
                lowercase: /[a-z]/.test(password),
                number: /\d/.test(password),
                special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
            };

            const metRequirements = Object.values(requirements).filter(Boolean).length;

            // Password strength - require at least 3 requirements
            if (metRequirements < 3) {
                document.getElementById('password').classList.add('is-invalid');
                isValid = false;
                errorMessage = 'Password does not meet minimum security requirements.';
            }

            // Check if passwords match
            if (password !== confirmPassword) {
                document.getElementById('confirmPassword').classList.add('is-invalid');
                isValid = false;
                errorMessage = 'Passwords do not match.';
            }

            // Terms agreement validation
            const agreeTerms = document.getElementById('agreeTerms').checked;
            if (!agreeTerms) {
                document.getElementById('agreeTerms').classList.add('is-invalid');
                isValid = false;
                errorMessage = 'You must agree to the terms and conditions to continue.';
            }

            // If validation fails, show error and return
            if (!isValid) {
                // Create and display error alert
                const formContainer = document.querySelector('.register-right');
                const alertDiv = document.createElement('div');
                alertDiv.className = 'alert-custom alert-error-custom';
                alertDiv.innerHTML = `<i class="bi bi-exclamation-circle me-2"></i>${errorMessage}`;
                formContainer.insertBefore(alertDiv, this);
                return;
            }

            // Show loading state
            const btnText = document.getElementById('registerBtnText');
            const submitBtn = document.querySelector('button[type="submit"]');
            const spinner = document.getElementById('registerSpinner');

            btnText.textContent = 'Creating Account...';
            spinner.style.display = 'inline-block';
            submitBtn.disabled = true;

            // If all validation passes, submit the form
            this.submit();
        });
    }
});

// Password confirmation validation
document.addEventListener('DOMContentLoaded', function() {
    const confirmPasswordField = document.getElementById('confirmPassword');
    if (confirmPasswordField) {
        confirmPasswordField.addEventListener('input', function() {
            const password = document.getElementById('password').value;
            const confirmPassword = this.value;

            if (confirmPassword && password !== confirmPassword) {
                this.classList.add('is-invalid');
            } else {
                this.classList.remove('is-invalid');
                if (confirmPassword) this.classList.add('is-valid');
            }
        });
    }
});

// Toggle password visibility
function togglePassword(fieldId) {
    const passwordInput = document.getElementById(fieldId);
    const passwordIcon = document.getElementById(fieldId + 'Icon');

    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        passwordIcon.className = 'bi bi-eye-slash';
    } else {
        passwordInput.type = 'password';
        passwordIcon.className = 'bi bi-eye';
    }
}

// Show/hide loading state
function showLoading(show) {
    const btnText = document.getElementById('registerBtnText');
    const spinner = document.getElementById('registerSpinner');
    const submitBtn = document.querySelector('button[type="submit"]');

    if (show) {
        btnText.textContent = 'Creating Account...';
        spinner.style.display = 'inline-block';
        submitBtn.disabled = true;
    } else {
        btnText.textContent = 'Create Account';
        spinner.style.display = 'none';
        submitBtn.disabled = false;
    }
}

// Show alert messages
function showAlert(type, message) {
    hideAlerts();

    // Create and append alert element
    const formContainer = document.querySelector('.register-right');
    if (!formContainer) return;

    const alertDiv = document.createElement('div');
    alertDiv.className = type === 'success' ? 'alert-custom alert-success-custom' : 'alert-custom alert-error-custom';
    alertDiv.innerHTML = `<i class="bi bi-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>${message}`;

    // Insert at the top of the form container
    const form = document.getElementById('registerForm');
    if (form) {
        formContainer.insertBefore(alertDiv, form);
    } else {
        formContainer.appendChild(alertDiv);
    }

    // Auto hide after 5 seconds
    setTimeout(hideAlerts, 5000);
}

// Hide all alerts
function hideAlerts() {
    // Clear any existing alert messages from the page
    const existingAlerts = document.querySelectorAll('.alert-custom');
    existingAlerts.forEach(alert => {
        alert.remove();
    });
}

// Fill demo data
function fillDemoData() {
    document.getElementById('fullName').value = 'John Doe';
    document.getElementById('email').value = 'john.doe@example.com';
    document.getElementById('phoneNumber').value = '+1234567890';

    // Trigger validation
    document.querySelectorAll('#formStep1 input').forEach(input => {
        input.classList.add('is-valid');
    });

    showAlert('success', 'Demo data filled. You can now proceed to the next step.');
}

// Real-time validation for all fields
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('input').forEach(field => {
        field.addEventListener('input', function() {
            if (this.id === 'fullName') {
                const nameRegex = /^[A-Za-z\s'.,-]+$/;
                if (nameRegex.test(this.value) && this.value.trim()) {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                } else if (this.value.trim()) {
                    this.classList.remove('is-valid');
                    this.classList.add('is-invalid');
                }
            } else if (this.id === 'email') {
                if (this.validity.valid && this.value.trim()) {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                } else if (this.value.trim()) {
                    this.classList.remove('is-valid');
                    this.classList.add('is-invalid');
                }
            } else if (this.id === 'phoneNumber') {
                const phoneRegex = /^\+?[\d]{10,15}$/;
                if (phoneRegex.test(this.value.replace(/\s/g, '')) && this.value.trim()) {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                } else if (this.value.trim()) {
                    this.classList.remove('is-valid');
                    this.classList.add('is-invalid');
                }
            } else if (this.id === 'password') {
                updatePasswordStrength(this.value);
                if (this.value.trim()) {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            } else {
                if (this.value.trim()) {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            }
        });
    });
});

// Password strength meter function
function updatePasswordStrength(password) {
    // Check requirements
    const requirements = {
        length: password.length >= 8,
        uppercase: /[A-Z]/.test(password),
        lowercase: /[a-z]/.test(password),
        number: /\d/.test(password),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(password)
    };

    // Update requirement indicators
    for (const [req, met] of Object.entries(requirements)) {
        const reqElement = document.getElementById(`req-${req}`);
        if (reqElement) {
            reqElement.className = met ? 'requirement met' : 'requirement unmet';
            reqElement.querySelector('i').className = met ? 'bi bi-check-circle' : 'bi bi-circle';
        }
    }

    // Count met requirements for strength calculation
    const metCount = Object.values(requirements).filter(Boolean).length;

    // Update strength bar
    const strengthFill = document.getElementById('strengthFill');
    const strengthText = document.getElementById('strengthText');

    if (!strengthFill || !strengthText) return;

    if (!password) {
        strengthFill.className = 'strength-fill';
        strengthFill.style.width = '0%';
        strengthText.textContent = 'Password strength: None';
        return;
    }

    // Determine strength level
    let strengthClass = '';
    let strengthLevel = '';
    let strengthWidth = '';

    if (metCount <= 1) {
        strengthClass = 'strength-weak';
        strengthLevel = 'Weak';
        strengthWidth = '25%';
    } else if (metCount === 2) {
        strengthClass = 'strength-fair';
        strengthLevel = 'Fair';
        strengthWidth = '50%';
    } else if (metCount === 3 || metCount === 4) {
        strengthClass = 'strength-good';
        strengthLevel = 'Good';
        strengthWidth = '75%';
    } else {
        strengthClass = 'strength-strong';
        strengthLevel = 'Strong';
        strengthWidth = '100%';
    }

    // Apply changes to the DOM
    strengthFill.className = `strength-fill ${strengthClass}`;
    strengthFill.style.width = strengthWidth;
    strengthText.textContent = `Password strength: ${strengthLevel}`;
}

// Initialize everything when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Show first step by default
    showStep(1);
    updateStepIndicator();
});
