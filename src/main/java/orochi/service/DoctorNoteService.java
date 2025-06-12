package orochi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import orochi.model.DoctorNote;
import orochi.repository.DoctorNoteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class to handle business logic for doctor notes.
 */
@Service
public class DoctorNoteService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorNoteService.class);

    @Autowired
    private DoctorNoteRepository doctorNoteRepository;

    /**
     * Create a new doctor note for an appointment.
     *
     * @param appointmentId The ID of the appointment
     * @param doctorId The ID of the doctor creating the note
     * @param content The content of the note
     * @return The created note
     */
    @Transactional
    public DoctorNote createNote(Integer appointmentId, Integer doctorId, String content) {
        logger.info("Creating new note for appointment ID: {} by doctor ID: {}", appointmentId, doctorId);

        DoctorNote note = new DoctorNote();
        note.setAppointmentId(appointmentId);
        note.setDoctorId(doctorId);
        note.setNoteContent(content);
        note.setCreatedAt(LocalDateTime.now());

        return doctorNoteRepository.save(note);
    }

    /**
     * Update an existing doctor note.
     *
     * @param noteId The ID of the note to update
     * @param doctorId The ID of the doctor updating the note
     * @param content The new content
     * @return The updated note, or empty if note doesn't exist or doctor doesn't have permission
     */
    @Transactional
    public Optional<DoctorNote> updateNote(Integer noteId, Integer doctorId, String content) {
        logger.info("Updating note ID: {} by doctor ID: {}", noteId, doctorId);

        Optional<DoctorNote> noteOpt = doctorNoteRepository.findById(noteId);

        if (noteOpt.isPresent()) {
            DoctorNote note = noteOpt.get();

            // Verify the doctor has permission to update this note
            if (!note.getDoctorId().equals(doctorId)) {
                logger.warn("Doctor ID: {} attempted to update note ID: {} belonging to another doctor",
                        doctorId, noteId);
                return Optional.empty();
            }

            note.setNoteContent(content);
            note.setUpdatedAt(LocalDateTime.now());
            return Optional.of(doctorNoteRepository.save(note));
        }

        return Optional.empty();
    }

    /**
     * Get all notes for an appointment.
     *
     * @param appointmentId The ID of the appointment
     * @return List of notes for the appointment
     */
    public List<DoctorNote> getNotesByAppointment(Integer appointmentId) {
        logger.info("Fetching notes for appointment ID: {}", appointmentId);
        return doctorNoteRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId);
    }

    /**
     * Get all notes created by a doctor.
     *
     * @param doctorId The ID of the doctor
     * @return List of notes created by the doctor
     */
    public List<DoctorNote> getNotesByDoctor(Integer doctorId) {
        logger.info("Fetching notes created by doctor ID: {}", doctorId);
        return doctorNoteRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    /**
     * Get a specific note by ID.
     *
     * @param noteId The ID of the note
     * @return The note, or empty if not found
     */
    public Optional<DoctorNote> getNoteById(Integer noteId) {
        logger.info("Fetching note with ID: {}", noteId);
        return doctorNoteRepository.findById(noteId);
    }

    /**
     * Delete a doctor note.
     *
     * @param noteId The ID of the note to delete
     * @param doctorId The ID of the doctor attempting to delete
     * @return true if deleted successfully, false otherwise
     */
    @Transactional
    public boolean deleteNote(Integer noteId, Integer doctorId) {
        logger.info("Attempting to delete note ID: {} by doctor ID: {}", noteId, doctorId);

        Optional<DoctorNote> noteOpt = doctorNoteRepository.findById(noteId);

        if (noteOpt.isPresent()) {
            DoctorNote note = noteOpt.get();

            // Verify the doctor has permission to delete this note
            if (!note.getDoctorId().equals(doctorId)) {
                logger.warn("Doctor ID: {} attempted to delete note ID: {} belonging to another doctor",
                        doctorId, noteId);
                return false;
            }

            doctorNoteRepository.delete(note);
            return true;
        }

        return false;
    }
}
