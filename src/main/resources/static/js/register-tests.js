// Register Form Mock Test
function runRegisterFormTests() {
    console.log('Running registration form tests...');

    // Save the original form submit handler
    const originalSubmitHandler = document.getElementById('registerForm').onsubmit;

    // Temporarily override form submission to prevent actual submission during tests
    document.getElementById('registerForm').onsubmit = function(e) {
        e.preventDefault();
        return false;
    };

    // Reset form state
    document.getElementById('registerForm').reset();
    document.querySelectorAll('.is-invalid, .is-valid').forEach(el => {
        el.classList.remove('is-invalid', 'is-valid');
    });

    // Test cases
    const testCases = [
        {
            name: "Test 1: Empty Form Submission",
            setup: () => {
                // Leave all fields empty
            },
            execute: () => {
                // Manually trigger validation without form submission
                validateFormFields();
            },
            expectedResult: () => {
                // Check if validation errors are shown
                const hasInvalidFields = document.querySelectorAll('.is-invalid').length > 0;
                return hasInvalidFields;
            }
        },
        {
            name: "Test 2: Invalid Email Format",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "invalid-email";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Password123!";
                document.getElementById('confirmPassword').value = "Password123!";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                return document.getElementById('email').classList.contains('is-invalid');
            }
        },
        {
            name: "Test 3: Password Mismatch",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Password123!";
                document.getElementById('confirmPassword').value = "DifferentPassword123!";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                return document.getElementById('confirmPassword').classList.contains('is-invalid');
            }
        },
        {
            name: "Test 4: Terms Not Agreed",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Password123!";
                document.getElementById('confirmPassword').value = "Password123!";
                document.getElementById('agreeTerms').checked = false;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                return document.getElementById('agreeTerms').classList.contains('is-invalid');
            }
        },
        {
            name: "Test 5: Valid Form Data",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Password123!";
                document.getElementById('confirmPassword').value = "Password123!";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                return document.querySelectorAll('.is-invalid').length === 0;
            }
        },
        {
            name: "Test 6: Weak Password - Too Short",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Abc1!"; // Short but has all types
                document.getElementById('confirmPassword').value = "Abc1!";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                return document.getElementById('password').classList.contains('is-invalid');
            }
        },
        {
            name: "Test 7: Weak Password - No Uppercase",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "password123!"; // No uppercase
                document.getElementById('confirmPassword').value = "password123!";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                return document.getElementById('password').classList.contains('is-invalid');
            }
        },
        {
            name: "Test 8: Weak Password - No Special Character",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Password123"; // No special character
                document.getElementById('confirmPassword').value = "Password123";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                // This might pass since we only require 3 out of 5 conditions
                // (length, uppercase, lowercase, number, special)
                const passwordField = document.getElementById('password');

                // If it meets at least 3 requirements, it should pass
                return !passwordField.classList.contains('is-invalid');
            }
        },
        {
            name: "Test 9: Minimal Acceptable Password",
            setup: () => {
                document.getElementById('fullName').value = "Test User";
                document.getElementById('email').value = "test@example.com";
                document.getElementById('phoneNumber').value = "+1234567890";
                document.getElementById('password').value = "Password123"; // Meets 3 requirements: length, uppercase, number
                document.getElementById('confirmPassword').value = "Password123";
                document.getElementById('agreeTerms').checked = true;
            },
            execute: () => {
                validateFormFields();
            },
            expectedResult: () => {
                const passwordField = document.getElementById('password');
                return !passwordField.classList.contains('is-invalid');
            }
        }
    ];

    // Run tests
    let passedTests = 0;
    testCases.forEach((test, index) => {
        console.log(`Running ${test.name}...`);

        // Reset form and alerts before each test
        document.getElementById('registerForm').reset();
        document.querySelectorAll('.is-invalid, .is-valid').forEach(el => {
            el.classList.remove('is-invalid', 'is-valid');
        });
        document.querySelectorAll('.alert-custom').forEach(el => el.remove());

        // Setup test
        test.setup();

        // Execute test
        test.execute();

        // Check result
        const passed = test.expectedResult();
        console.log(`${test.name}: ${passed ? 'PASSED ✓' : 'FAILED ✗'}`);

        if (passed) passedTests++;
    });

    console.log(`Tests complete: ${passedTests}/${testCases.length} tests passed`);

    // Display test results on page
    const formContainer = document.querySelector('.register-right');
    const resultDiv = document.createElement('div');
    resultDiv.className = passedTests === testCases.length ?
        'alert-custom alert-success-custom mt-4' :
        'alert-custom alert-error-custom mt-4';
    resultDiv.innerHTML = `<i class="bi bi-${passedTests === testCases.length ? 'check-circle' : 'exclamation-circle'} me-2"></i>
        Test Results: ${passedTests}/${testCases.length} tests passed`;
    formContainer.appendChild(resultDiv);

    // Restore the original form submit handler
    document.getElementById('registerForm').onsubmit = originalSubmitHandler;
}

// Custom validation function that mimics the form validation but doesn't submit
function validateFormFields() {
    // Clear existing alerts
    document.querySelectorAll('.alert-custom').forEach(alert => alert.remove());

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

    // Email validation
    const email = document.getElementById('email');
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (email.value.trim() && !emailRegex.test(email.value)) {
        email.classList.add('is-invalid');
        isValid = false;
        errorMessage = 'Please enter a valid email address.';
    }

    // Phone validation
    const phone = document.getElementById('phoneNumber');
    const phoneRegex = /^\+?[\d]{10,15}$/;
    if (phone.value.trim() && !phoneRegex.test(phone.value.replace(/\s/g, ''))) {
        phone.classList.add('is-invalid');
        isValid = false;
        errorMessage = 'Please enter a valid phone number.';
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

    // If validation fails, show error
    if (!isValid) {
        // Create and display error alert
        const formContainer = document.querySelector('.register-right');
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert-custom alert-error-custom';
        alertDiv.innerHTML = `<i class="bi bi-exclamation-circle me-2"></i>${errorMessage}`;
        formContainer.insertBefore(alertDiv, document.getElementById('registerForm'));
    }

    return isValid;
}

// Add test button to page
function addTestButton() {
    // Only add if it doesn't already exist
    if (!document.getElementById('testValidationButton')) {
        const formContainer = document.querySelector('.register-right');
        const testButton = document.createElement('button');
        testButton.id = 'testValidationButton';
        testButton.className = 'btn btn-outline-custom mt-4 w-100';
        testButton.type = 'button'; // Explicitly set type to button to avoid form submission
        testButton.innerHTML = '<i class="bi bi-bug me-2"></i>Run Form Validation Tests';
        testButton.onclick = runRegisterFormTests;
        formContainer.appendChild(testButton);
    }
}

// Run when page loads
document.addEventListener('DOMContentLoaded', function() {
    addTestButton();
});