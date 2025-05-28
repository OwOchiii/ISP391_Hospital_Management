package orochi.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class PatientUserRelationshipTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private orochi.repository.UserRepository userRepository;

    @Autowired
    private orochi.repository.PatientRepository patientRepository;

    @Test
    public void testCreateAndRetrievePatientWithUser() {
        // Create a user
        Users user = new Users();
        user.setFullName("Test Patient");
        user.setEmail("patient@test.com");
        user.setPasswordHash("hashedpassword");
        user.setPhoneNumber("1234567890");
        user.setRoleId(2); // Assuming 2 is patient role
        user.setGuest(false);

        entityManager.persist(user);

        // Create a patient with the user
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        patient.setGender("Male");
        patient.setAddress("123 Test Street");
        patient.setDescription("Test patient description");

        entityManager.persist(patient);
        entityManager.flush();

        // Clear persistence context to force a database read
        entityManager.clear();

        // Retrieve the patient and verify
        Optional<Patient> foundPatient = patientRepository.findById(user.getUserId());
        assertTrue(foundPatient.isPresent());
        assertEquals("Test Patient", foundPatient.get().getFullName());
        assertEquals(user.getUserId(), foundPatient.get().getPatientId());
        assertEquals("Male", foundPatient.get().getGender());
        assertEquals("123 Test Street", foundPatient.get().getAddress());

        // Verify bidirectional relationship
        Optional<Users> foundUser = userRepository.findById(user.getUserId());
        assertTrue(foundUser.isPresent());
        assertNotNull(foundUser.get().getPatient());
        assertEquals("Test patient description", foundUser.get().getPatient().getDescription());
    }

    @Test
    public void testPatientGetterAndSetterForFullName() {
        // Test getter with null user
        Patient patient = new Patient();
        assertNull(patient.getFullName());

        // Test setter with null user (should create a new user)
        patient.setFullName("Jane Doe");
        assertNotNull(patient.getUser());
        assertEquals("Jane Doe", patient.getUser().getFullName());

        // Test with existing user
        Users existingUser = new Users();
        existingUser.setFullName("John Smith");

        Patient patientWithUser = new Patient();
        patientWithUser.setUser(existingUser);
        assertEquals("John Smith", patientWithUser.getFullName());

        // Update full name and verify
        patientWithUser.setFullName("John Doe");
        assertEquals("John Doe", existingUser.getFullName());
    }

    @Test
    public void testSearchPatientsByFullName() {
        // Create test data
        createTestPatient("John Doe", "john@example.com");
        createTestPatient("Jane Doe", "jane@example.com");
        createTestPatient("Alice Smith", "alice@example.com");

        entityManager.flush();
        entityManager.clear();

        // Test search using the custom query method
        List<Patient> results = patientRepository.findByFullNameContainingIgnoreCase("doe");
        assertEquals(2, results.size());

        // Verify search results contain expected names
        List<String> names = results.stream()
            .map(Patient::getFullName)
            .toList();

        assertTrue(names.contains("John Doe"));
        assertTrue(names.contains("Jane Doe"));
        assertFalse(names.contains("Alice Smith"));
    }

    private void createTestPatient(String fullName, String email) {
        Users user = new Users();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash("password");
        user.setPhoneNumber(fullName.toLowerCase().replace(" ", "") + "_phone");
        user.setRoleId(2); // Assuming 2 is patient role
        user.setGuest(false);

        entityManager.persist(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setGender("Other");
        patient.setDateOfBirth(LocalDate.now().minusYears(30));
        patient.setAddress(fullName + "'s Address");

        entityManager.persist(patient);
    }
}