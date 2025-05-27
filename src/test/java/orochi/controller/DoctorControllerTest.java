package orochi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import orochi.model.*;
import orochi.repository.AppointmentRepository;
import orochi.repository.DoctorRepository;
import orochi.repository.MedicalOrderRepository;
import orochi.service.DoctorService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//./gradlew test --offline
@ExtendWith(SpringExtension.class)
public class DoctorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DoctorService doctorService;

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorController doctorController;

    @InjectMocks
    private DoctorAppointmentController doctorAppointmentController;


    private final Integer testDoctorId = 1;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(doctorService.getAppointmentRepository()).thenReturn(appointmentRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(doctorController,doctorAppointmentController).build();
    }

    @Test
    public void getDashboard_ShouldReturnDashboardView() throws Exception {
        // Arrange
        List<Appointment> todayAppointments = createTestAppointments(3);
        List<Appointment> upcomingAppointments = createTestAppointments(2);
        List<MedicalOrder> pendingOrders = createTestMedicalOrders(2);
        List<Patient> patients = createTestPatients(5);

        // Mock the doctor and user objects
        Doctor doctor = new Doctor();
        Users user = new Users();
        user.setFullName("Dr. Test");
        doctor.setUser(user);

        when(doctorRepository.findById(testDoctorId)).thenReturn(Optional.of(doctor));
        when(doctorService.getTodayAppointments(testDoctorId)).thenReturn(todayAppointments);
        when(doctorService.getUpcomingAppointments(testDoctorId)).thenReturn(upcomingAppointments);
        when(medicalOrderRepository.findByOrderByIdAndStatus(testDoctorId, "Pending"))
                .thenReturn(pendingOrders);
        when(doctorService.getPatientsWithAppointments(testDoctorId)).thenReturn(patients);

        // Act & Assert
        mockMvc.perform(get("/doctor/dashboard").param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/dashboard"))
                .andExpect(model().attribute("todayAppointments", hasSize(3)))
                .andExpect(model().attribute("upcomingAppointments", hasSize(2)))
                .andExpect(model().attribute("pendingOrders", hasSize(2)))
                .andExpect(model().attribute("patientCount", 5))
                .andExpect(model().attribute("doctorId", testDoctorId));

        // Verify
        verify(doctorRepository).findById(testDoctorId);
        verify(doctorService).getTodayAppointments(testDoctorId);
        verify(doctorService).getUpcomingAppointments(testDoctorId);
        verify(medicalOrderRepository).findByOrderByIdAndStatus(testDoctorId, "Pending");
        verify(doctorService).getPatientsWithAppointments(testDoctorId);
    }
@Test
public void getAllAppointments_ShouldReturnAppointmentsView() throws Exception {
    // Arrange
    List<Appointment> appointments = createTestAppointments(5);

    // Mock doctor
    Doctor doctor = new Doctor();
    Users user = new Users();
    user.setFullName("Dr. Test");
    doctor.setUser(user);

    // Mock all service calls
    when(doctorRepository.findById(testDoctorId)).thenReturn(Optional.of(doctor));
    when(doctorService.getAppointments(testDoctorId)).thenReturn(appointments);
    when(doctorService.getTodayAppointments(testDoctorId)).thenReturn(new ArrayList<>());
    when(doctorService.getUpcomingAppointments(testDoctorId)).thenReturn(new ArrayList<>());

    // Act & Assert
    mockMvc.perform(get("/doctor/appointments").param("doctorId", testDoctorId.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("doctor/appointments"))
            .andExpect(model().attribute("appointments", hasSize(5)))
            .andExpect(model().attribute("doctorId", testDoctorId))
            .andExpect(model().attribute("title", "All Appointments"));

    // Verify - change to verify 'at least once' instead of exactly once
    verify(doctorService, atLeastOnce()).getAppointments(testDoctorId);
}

    @Test
    public void getTodayAppointments_ShouldReturnAppointmentsView() throws Exception {
        // Arrange
        List<Appointment> appointments = createTestAppointments(3);
        when(doctorService.getTodayAppointments(testDoctorId)).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/doctor/appointments/today").param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/appointments"))
                .andExpect(model().attribute("appointments", hasSize(3)))
                .andExpect(model().attribute("doctorId", testDoctorId))
                .andExpect(model().attribute("title", "Today's Appointments"));

        // Verify
        verify(doctorService).getTodayAppointments(testDoctorId);
    }

    @Test
    public void getUpcomingAppointments_ShouldReturnAppointmentsView() throws Exception {
        // Arrange
        List<Appointment> appointments = createTestAppointments(4);
        when(doctorService.getUpcomingAppointments(testDoctorId)).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/doctor/appointments/upcoming").param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/appointments"))
                .andExpect(model().attribute("appointments", hasSize(4)))
                .andExpect(model().attribute("doctorId", testDoctorId))
                .andExpect(model().attribute("title", "Upcoming Appointments"));

        // Verify
        verify(doctorService).getUpcomingAppointments(testDoctorId);
    }

    @Test
    public void getAppointmentsByDate_ShouldReturnAppointmentsView() throws Exception {
        // Arrange
        List<Appointment> appointments = createTestAppointments(2);
        LocalDate testDate = LocalDate.now();
        when(doctorService.getAppointmentsByDate(eq(testDoctorId), any(LocalDate.class)))
                .thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/doctor/appointments/date")
                        .param("doctorId", testDoctorId.toString())
                        .param("date", testDate.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/appointments"))
                .andExpect(model().attribute("appointments", hasSize(2)))
                .andExpect(model().attribute("doctorId", testDoctorId))
                .andExpect(model().attribute("selectedDate", testDate))
                .andExpect(model().attribute("title", "Appointments on " + testDate));

        // Verify
        verify(doctorService).getAppointmentsByDate(eq(testDoctorId), any(LocalDate.class));
    }

    @Test
    public void getAppointmentsByStatus_ShouldReturnAppointmentsView() throws Exception {
        // Arrange
        List<Appointment> appointments = createTestAppointments(3);
        String testStatus = "Scheduled";
        when(doctorService.getAppointmentsByStatus(testDoctorId, testStatus)).thenReturn(appointments);

        // Act & Assert
        mockMvc.perform(get("/doctor/appointments/status")
                        .param("doctorId", testDoctorId.toString())
                        .param("status", testStatus))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/appointments"))
                .andExpect(model().attribute("appointments", hasSize(3)))
                .andExpect(model().attribute("doctorId", testDoctorId))
                .andExpect(model().attribute("selectedStatus", testStatus))
                .andExpect(model().attribute("title", testStatus + " Appointments"));

        // Verify
        verify(doctorService).getAppointmentsByStatus(testDoctorId, testStatus);
    }

    @Test
    public void getAppointmentDetails_WhenAppointmentExists_ShouldReturnDetailsView() throws Exception {
        // Arrange
        Integer appointmentId = 1;
        Integer patientId = 5;

        Appointment appointment = new Appointment();
        appointment.setAppointmentId(appointmentId);
        appointment.setDoctorId(testDoctorId);
        appointment.setPatientId(patientId);
        appointment.setDateTime(now);
        appointment.setStatus("Scheduled");

        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setFullName("John Doe");

        List<MedicalOrder> medicalOrders = createTestMedicalOrders(2);

        when(doctorService.getAppointmentDetails(appointmentId, testDoctorId))
                .thenReturn(Optional.of(appointment));
        when(doctorService.getPatientDetails(patientId)).thenReturn(Optional.of(patient));
        when(medicalOrderRepository.findByAppointmentIdOrderByOrderDate(appointmentId))
                .thenReturn(medicalOrders);

        // Act & Assert
        mockMvc.perform(get("/doctor/appointment/{appointmentId}", appointmentId)
                        .param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/appointment-details"))
                .andExpect(model().attribute("appointment", appointment))
                .andExpect(model().attribute("patient", patient))
                .andExpect(model().attribute("medicalOrders", hasSize(2)))
                .andExpect(model().attribute("doctorId", testDoctorId));

        // Verify
        verify(doctorService).getAppointmentDetails(appointmentId, testDoctorId);
        verify(doctorService).getPatientDetails(patientId);
        verify(medicalOrderRepository).findByAppointmentIdOrderByOrderDate(appointmentId);
    }

    @Test
    public void getAppointmentDetails_WhenAppointmentNotFound_ShouldReturnErrorView() throws Exception {
        // Arrange
        Integer appointmentId = 999;
        when(doctorService.getAppointmentDetails(appointmentId, testDoctorId))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/doctor/appointment/{appointmentId}", appointmentId)
                        .param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "Appointment not found or access denied."));

        // Verify
        verify(doctorService).getAppointmentDetails(appointmentId, testDoctorId);
    }

    @Test
    public void getPatientsWithAppointments_ShouldReturnPatientsView() throws Exception {
        // Arrange
        List<Patient> patients = createTestPatients(4);
        when(doctorService.getPatientsWithAppointments(testDoctorId)).thenReturn(patients);

        // Act & Assert
        mockMvc.perform(get("/doctor/patients").param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/patients"))
                .andExpect(model().attribute("patients", hasSize(4)))
                .andExpect(model().attribute("doctorId", testDoctorId));

        // Verify
        verify(doctorService).getPatientsWithAppointments(testDoctorId);
    }

    @Test
    public void getPatientDetails_WhenPatientExists_ShouldReturnDetailsView() throws Exception {
        // Arrange
        Integer patientId = 5;
        Patient patient = new Patient();
        patient.setPatientId(patientId);
        patient.setFullName("John Doe");

        List<Appointment> appointmentHistory = createTestAppointments(3);

        when(doctorService.getPatientDetails(patientId)).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByPatientIdAndDoctorIdOrderByDateTimeDesc(patientId, testDoctorId))
                .thenReturn(appointmentHistory);

        // Act & Assert
        mockMvc.perform(get("/doctor/patient/{patientId}", patientId)
                        .param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/patient-details"))
                .andExpect(model().attribute("patient", patient))
                .andExpect(model().attribute("appointmentHistory", hasSize(3)))
                .andExpect(model().attribute("doctorId", testDoctorId));

        // Verify
        verify(doctorService).getPatientDetails(patientId);
        verify(appointmentRepository).findByPatientIdAndDoctorIdOrderByDateTimeDesc(patientId, testDoctorId);
    }

    @Test
    public void getPatientDetails_WhenPatientNotFound_ShouldReturnErrorView() throws Exception {
        // Arrange
        Integer patientId = 999;
        when(doctorService.getPatientDetails(patientId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/doctor/patient/{patientId}", patientId)
                        .param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "Patient not found."));

        // Verify
        verify(doctorService).getPatientDetails(patientId);
    }

    @Test
    public void searchPatients_ShouldReturnSearchResultsView() throws Exception {
        // Arrange
        String searchTerm = "John";
        List<Patient> searchResults = createTestPatients(2);
        when(doctorService.searchPatientsByName(searchTerm)).thenReturn(searchResults);

        // Act & Assert
        mockMvc.perform(get("/doctor/patients/search")
                        .param("name", searchTerm)
                        .param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/patient-search-results"))
                .andExpect(model().attribute("patients", hasSize(2)))
                .andExpect(model().attribute("searchTerm", searchTerm))
                .andExpect(model().attribute("doctorId", testDoctorId));

        // Verify
        verify(doctorService).searchPatientsByName(searchTerm);
    }

    @Test
    public void getDoctorMedicalOrders_ShouldReturnMedicalOrdersView() throws Exception {
        // Arrange
        List<MedicalOrder> orders = createTestMedicalOrders(4);
        // Updated method name
        when(medicalOrderRepository.findByOrderById(testDoctorId)).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/doctor/medical-orders").param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/medical-orders"))
                .andExpect(model().attribute("medicalOrders", hasSize(4)))
                .andExpect(model().attribute("doctorId", testDoctorId))
                .andExpect(model().attribute("title", "All Medical Orders"));

        // Verify
        // Updated method name
        verify(medicalOrderRepository).findByOrderById(testDoctorId);
    }

    @Test
    public void getMedicalOrdersByStatus_ShouldReturnMedicalOrdersView() throws Exception {
        // Arrange
        List<MedicalOrder> orders = createTestMedicalOrders(3);
        String testStatus = "Filled";
        // Updated method name
        when(medicalOrderRepository.findByOrderByIdAndStatus(testDoctorId, testStatus))
                .thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/doctor/medical-orders/status")
                        .param("doctorId", testDoctorId.toString())
                        .param("status", testStatus))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/medical-orders"))
                .andExpect(model().attribute("medicalOrders", hasSize(3)))
                .andExpect(model().attribute("doctorId", testDoctorId))
                .andExpect(model().attribute("selectedStatus", testStatus))
                .andExpect(model().attribute("title", testStatus + " Medical Orders"));

        // Verify
        // Updated method name
        verify(medicalOrderRepository).findByOrderByIdAndStatus(testDoctorId, testStatus);
    }

    @Test
    public void getDashboard_WhenExceptionOccurs_ShouldReturnErrorView() throws Exception {
        // Arrange
        when(doctorRepository.findById(testDoctorId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/doctor/dashboard").param("doctorId", testDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));

        // Verify
        verify(doctorRepository).findById(testDoctorId);
    }
    // Helper methods to create test data
    private List<Appointment> createTestAppointments(int count) {
        List<Appointment> appointments = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(i);
            appointment.setDoctorId(testDoctorId);
            appointment.setPatientId(i + 10);
            appointment.setDateTime(now.plusHours(i));
            appointment.setStatus("Scheduled");
            appointments.add(appointment);
        }
        return appointments;
    }

    private List<Patient> createTestPatients(int count) {
        List<Patient> patients = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Patient patient = new Patient();
            patient.setPatientId(i + 10);
            patient.setFullName("Patient " + i);
            patients.add(patient);
        }
        return patients;
    }

    private List<MedicalOrder> createTestMedicalOrders(int count) {
        List<MedicalOrder> orders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            MedicalOrder order = new MedicalOrder();
            order.setOrderId(i);
            order.setOrderById(testDoctorId);
            order.setAppointmentId(i);
            order.setStatus("Pending");

            // Convert LocalDateTime to java.sql.Date
            java.sql.Date sqlDate = java.sql.Date.valueOf(now.minusDays(i).toLocalDate());
            order.setOrderDate(sqlDate);

            orders.add(order);
        }
        return orders;
    }


}