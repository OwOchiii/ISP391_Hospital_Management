package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.dto.ScheduleDTO;
import orochi.model.Doctor;
import orochi.model.Patient;
import orochi.model.Room;
import orochi.model.Schedule;
import orochi.repository.DoctorRepository;
import orochi.repository.PatientRepository;
import orochi.repository.RoomRepository;
import orochi.repository.ScheduleRepository;
import orochi.service.ScheduleService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Override
    public List<ScheduleDTO> getDoctorScheduleForWeek(Integer doctorId, LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(doctorId, weekStart, weekEnd);
        return schedules.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleDTO> getDoctorScheduleForDay(Integer doctorId, LocalDate date) {
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndScheduleDateOrderByStartTimeAsc(doctorId, date);
        return schedules.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public ScheduleStatistics getDoctorScheduleStatistics(Integer doctorId, LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate weekEnd = weekStart.plusDays(6);
        Integer weeklyAppointments = scheduleRepository.countAppointmentsInDateRange(doctorId, weekStart, weekEnd);
        Integer onCallHours = scheduleRepository.sumOnCallHoursInDateRange(doctorId, weekStart, weekEnd);
        Integer roomsAssigned = scheduleRepository.countDistinctRoomsInDateRange(doctorId, weekStart, weekEnd);
        Integer totalHours = scheduleRepository.sumTotalHoursInDateRange(doctorId, weekStart, weekEnd);
        Integer completedSchedules = scheduleRepository.countCompletedSchedulesInDateRange(doctorId, weekStart, weekEnd);

        if (weeklyAppointments == null) weeklyAppointments = 0;
        if (onCallHours == null) onCallHours = 0;
        if (roomsAssigned == null) roomsAssigned = 0;
        if (totalHours == null) totalHours = 0;
        if (completedSchedules == null) completedSchedules = 0;

        ScheduleStatistics stats = new ScheduleStatistics();
        stats.setWeeklyAppointments(weeklyAppointments);
        stats.setOnCallHours(onCallHours);
        stats.setRoomsAssigned(roomsAssigned);
        stats.setTotalHours(totalHours);
        stats.setCompletedSchedules(completedSchedules);
        return stats;
    }

    @Override
    public ScheduleDTO saveSchedule(ScheduleDTO scheduleDTO) {
        Schedule schedule = convertToEntity(scheduleDTO);
        schedule = scheduleRepository.save(schedule);
        return convertToDTO(schedule);
    }

    @Override
    public void deleteSchedule(Integer scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    @Override
    public ScheduleDTO getScheduleById(Integer scheduleId) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        return scheduleOpt.map(this::convertToDTO).orElse(null);
    }

    @Override
    public List<ScheduleDTO> getAllSchedules() {
        return scheduleRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleDTO> searchSchedules(String keyword, LocalDate startDate, LocalDate endDate) {
        List<Schedule> schedules;
        if (keyword != null && startDate != null && endDate != null) {
            schedules = scheduleRepository.findByKeywordAndDateRange(keyword, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            schedules = scheduleRepository.findByDateRange(startDate, endDate);
        } else if (keyword != null) {
            schedules = scheduleRepository.findByKeywordAndDate(keyword, null);
        } else {
            schedules = scheduleRepository.findAll();
        }
        return schedules.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public String formatWeekDateRange(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        return String.format("%s - %s, %d",
                startDate.getMonth().toString().substring(0, 3) + " " + startDate.getDayOfMonth(),
                endDate.getMonth().toString().substring(0, 3) + " " + endDate.getDayOfMonth(),
                endDate.getYear());
    }

    private ScheduleDTO convertToDTO(Schedule schedule) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setScheduleId(schedule.getScheduleId());
        dto.setDoctorId(schedule.getDoctorId());
        dto.setRoomId(schedule.getRoomId());
        dto.setPatientId(schedule.getPatientId());
        dto.setAppointmentId(schedule.getAppointmentId());
        dto.setScheduleDate(schedule.getScheduleDate());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setEventType(schedule.getEventType());
        dto.setDescription(schedule.getDescription());
        dto.setIsCompleted(schedule.getIsCompleted());

        if (schedule.getDoctor() != null) {
            dto.setDoctorName(schedule.getDoctor().getUser().getFullName());
            if (!schedule.getDoctor().getSpecializations().isEmpty()) {
                dto.setSpecializationName(schedule.getDoctor().getSpecializations().get(0).getSpecName()); 
            } else {
                dto.setSpecializationName("N/A");
            }
        } else if (schedule.getDoctorId() != null) {
            doctorRepository.findById(schedule.getDoctorId())
                    .ifPresent(doctor -> {
                        dto.setDoctorName(doctor.getUser().getFullName());
                        if (!doctor.getSpecializations().isEmpty()) {
                            dto.setSpecializationName(doctor.getSpecializations().get(0).getSpecName());
                        } else {
                            dto.setSpecializationName("N/A");
                        }
                    });
        }

        if (schedule.getRoom() != null) {
            dto.setRoomName(schedule.getRoom().getRoomName());
            dto.setRoomNumber(schedule.getRoom().getRoomNumber());
        } else if (schedule.getRoomId() != null) {
            roomRepository.findById(schedule.getRoomId())
                    .ifPresent(room -> {
                        dto.setRoomName(room.getRoomName());
                        dto.setRoomNumber(room.getRoomNumber());
                    });
        }

        if (schedule.getPatient() != null) {
            dto.setPatientName(schedule.getPatient().getFullName());
        } else if (schedule.getPatientId() != null) {
            patientRepository.findById(schedule.getPatientId())
                    .ifPresent(patient -> dto.setPatientName(patient.getFullName()));
        }

        return dto;
    }

    private Schedule convertToEntity(ScheduleDTO dto) {
        Schedule schedule = new Schedule();
        if (dto.getScheduleId() != null) {
            Optional<Schedule> existingSchedule = scheduleRepository.findById(dto.getScheduleId());
            if (existingSchedule.isPresent()) {
                schedule = existingSchedule.get();
            }
        }
        schedule.setDoctorId(dto.getDoctorId());
        schedule.setRoomId(dto.getRoomId());
        schedule.setPatientId(dto.getPatientId());
        schedule.setAppointmentId(dto.getAppointmentId());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setEventType(dto.getEventType());
        schedule.setDescription(dto.getDescription());
        schedule.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
        return schedule;
    }

    @Override
    public boolean toggleScheduleCompletion(Integer scheduleId, Integer doctorId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));
        if (!schedule.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Schedule does not belong to the specified doctor");
        }
        boolean newStatus = !schedule.getIsCompleted();
        schedule.setIsCompleted(newStatus);
        scheduleRepository.save(schedule);
        return newStatus;
    }

    @Override
    public boolean toggleAppointmentCompletion(Integer appointmentId, Integer doctorId) {
        Schedule schedule = scheduleRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Schedule not found for appointment with ID: " + appointmentId));
        if (!schedule.getDoctorId().equals(doctorId)) {
            throw new RuntimeException("Appointment does not belong to the specified doctor");
        }
        boolean newStatus = !schedule.getIsCompleted();
        schedule.setIsCompleted(newStatus);
        scheduleRepository.save(schedule);
        return newStatus;
    }
}