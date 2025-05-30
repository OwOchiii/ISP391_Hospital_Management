package orochi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import orochi.dto.DoctorNoteDTO;
import orochi.model.DoctorNote;
import orochi.service.DoctorNoteService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for handling doctor's notes operations.
 */
@Controller
@RequestMapping("/doctor/appointments")
public class DoctorNoteController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorNoteController.class);

    @Autowired
    private DoctorNoteService doctorNoteService;

    /**
     * Save a new doctor's note for an appointment.
     */
    @PostMapping("/{appointmentId}/notes/save")
    @ResponseBody
    public ResponseEntity<?> saveNote(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId,
            @RequestParam String noteContent) {

        try {
            logger.info("Saving new note for appointment ID: {} by doctor ID: {}", appointmentId, doctorId);

            // Input validation
            if (noteContent == null || noteContent.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "Note content cannot be empty"
                        ));
            }

            // Create the note
            DoctorNote note = doctorNoteService.createNote(appointmentId, doctorId, noteContent);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Note saved successfully",
                    "noteId", note.getNoteId(),
                    "createdAt", note.getCreatedAt()
            ));

        } catch (Exception e) {
            logger.error("Error saving note for appointment ID: {} by doctor ID: {}",
                    appointmentId, doctorId, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to save note: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get all notes for an appointment.
     */
    @GetMapping("/{appointmentId}/notes")
    @ResponseBody
    public ResponseEntity<?> getNotes(
            @PathVariable Integer appointmentId,
            @RequestParam Integer doctorId) {

        try {
            logger.info("Fetching notes for appointment ID: {} by doctor ID: {}", appointmentId, doctorId);

            List<DoctorNote> notes = doctorNoteService.getNotesByAppointment(appointmentId);

            // Convert entities to DTOs to avoid circular references
            List<DoctorNoteDTO> noteDTOs = notes.stream()
                .map(note -> {
                    DoctorNoteDTO dto = new DoctorNoteDTO();
                    dto.setNoteId(note.getNoteId());
                    dto.setAppointmentId(note.getAppointmentId());
                    dto.setDoctorId(note.getDoctorId());
                    dto.setNoteContent(note.getNoteContent());
                    dto.setCreatedAt(note.getCreatedAt());
                    dto.setUpdatedAt(note.getUpdatedAt());

                    // Safely extract doctor name if available
                    if (note.getDoctor() != null && note.getDoctor().getUser() != null) {
                        dto.setDoctorName(note.getDoctor().getUser().getFullName());
                    }

                    return dto;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notes", noteDTOs
            ));

        } catch (Exception e) {
            logger.error("Error fetching notes for appointment ID: {} by doctor ID: {}",
                    appointmentId, doctorId, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to fetch notes: " + e.getMessage()
                    ));
        }
    }

    /**
     * Update an existing note.
     */
    @PutMapping("/{appointmentId}/notes/{noteId}")
    @ResponseBody
    public ResponseEntity<?> updateNote(
            @PathVariable Integer appointmentId,
            @PathVariable Integer noteId,
            @RequestParam Integer doctorId,
            @RequestParam String noteContent) {

        try {
            logger.info("Updating note ID: {} for appointment ID: {} by doctor ID: {}",
                    noteId, appointmentId, doctorId);

            // Input validation
            if (noteContent == null || noteContent.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "Note content cannot be empty"
                        ));
            }

            // Update the note
            Optional<DoctorNote> updatedNote = doctorNoteService.updateNote(noteId, doctorId, noteContent);

            if (updatedNote.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Note updated successfully",
                        "note", updatedNote.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Note not found or you don't have permission to update it"
                        ));
            }

        } catch (Exception e) {
            logger.error("Error updating note ID: {} for appointment ID: {} by doctor ID: {}",
                    noteId, appointmentId, doctorId, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to update note: " + e.getMessage()
                    ));
        }
    }

    /**
     * Delete a note.
     */
    @DeleteMapping("/{appointmentId}/notes/{noteId}")
    @ResponseBody
    public ResponseEntity<?> deleteNote(
            @PathVariable Integer appointmentId,
            @PathVariable Integer noteId,
            @RequestParam Integer doctorId) {

        try {
            logger.info("Deleting note ID: {} for appointment ID: {} by doctor ID: {}",
                    noteId, appointmentId, doctorId);

            boolean deleted = doctorNoteService.deleteNote(noteId, doctorId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Note deleted successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Note not found or you don't have permission to delete it"
                        ));
            }

        } catch (Exception e) {
            logger.error("Error deleting note ID: {} for appointment ID: {} by doctor ID: {}",
                    noteId, appointmentId, doctorId, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to delete note: " + e.getMessage()
                    ));
        }
    }
}
