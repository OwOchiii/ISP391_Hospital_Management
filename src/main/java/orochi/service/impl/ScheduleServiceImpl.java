package orochi.service.impl;
import org.hibernate.Hibernate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.util.StringUtils;
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
import orochi.repository.AppointmentRepository;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Autowired
    private AppointmentRepository appointmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional
    public ScheduleDTO saveSchedule(ScheduleDTO scheduleDTO) {
        Schedule schedule;
        if (scheduleDTO.getScheduleId() != null) {
            schedule = scheduleRepository.findById(scheduleDTO.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleDTO.getScheduleId()));
        } else {
            schedule = new Schedule();
        }
        schedule.setScheduleDate(scheduleDTO.getScheduleDate());
        schedule.setStartTime(scheduleDTO.getStartTime());
        schedule.setEndTime(scheduleDTO.getEndTime());
        schedule.setEventType(scheduleDTO.getEventType());
        schedule.setDescription(scheduleDTO.getDescription());
        schedule.setIsCompleted(scheduleDTO.getIsCompleted() != null ? scheduleDTO.getIsCompleted() : false);

        if (scheduleDTO.getDoctorId() != null) {
            schedule.setDoctor(doctorRepository.findById(scheduleDTO.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + scheduleDTO.getDoctorId())));
        } else {
            schedule.setDoctor(null);
        }
        if (scheduleDTO.getRoomId() != null) {
            schedule.setRoom(roomRepository.findById(scheduleDTO.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found with ID: " + scheduleDTO.getRoomId())));
        } else {
            schedule.setRoom(null);
        }
        if (scheduleDTO.getPatientId() != null) {
            schedule.setPatient(patientRepository.findById(scheduleDTO.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + scheduleDTO.getPatientId())));
        } else {
            schedule.setPatient(null);
        }
        if (scheduleDTO.getAppointmentId() != null) {
            schedule.setAppointment(appointmentRepository.findById(scheduleDTO.getAppointmentId())
                    .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + scheduleDTO.getAppointmentId())));
        } else {
            schedule.setAppointment(null);
        }
        schedule = scheduleRepository.saveAndFlush(schedule);
        return convertToDTO(schedule);
    }

    @Override
    public void deleteSchedule(Integer scheduleId) {
        Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
        scheduleOpt.ifPresent(schedule -> {
            Hibernate.initialize(schedule.getDoctor());
            Hibernate.initialize(schedule.getRoom());
            Hibernate.initialize(schedule.getPatient());
            Hibernate.initialize(schedule.getAppointment());
            entityManager.detach(schedule);
            scheduleRepository.deleteById(scheduleId);
        });
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

    @Override
    public List<ScheduleDTO> searchSchedulesPaginated(String keyword, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Schedule> schedulePage;
        if (keyword != null && startDate != null && endDate != null) {
            schedulePage = scheduleRepository.findByKeywordAndDateRangePaginated(keyword, startDate, endDate, pageable);
        } else if (startDate != null && endDate != null) {
            schedulePage = scheduleRepository.findByDateRangePaginated(startDate, endDate, pageable);
        } else if (keyword != null) {
            schedulePage = scheduleRepository.findByKeywordAndDatePaginated(keyword, null, pageable);
        } else {
            schedulePage = scheduleRepository.findAll(pageable);
        }
        System.out.println("Page content size: " + schedulePage.getContent().size()); // Debug
        System.out.println("Total elements from Page: " + schedulePage.getTotalElements()); // Debug
        return schedulePage.getContent().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public long countSchedules(String keyword, LocalDate startDate, LocalDate endDate) {
        long count = 0;
        if (keyword != null && startDate != null && endDate != null) {
            count = scheduleRepository.countByKeywordAndDateRange(keyword, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            count = scheduleRepository.countByDateRange(startDate, endDate);
        } else if (keyword != null) {
            count = scheduleRepository.countByKeywordAndDate(keyword, null);
        } else {
            count = scheduleRepository.count();
        }
        System.out.println("Count Schedules: " + count); // Debug
        return count;
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
            doctorRepository.findById(schedule.getDoctorId()).ifPresentOrElse(doctor -> {
                System.out.println("Found doctor: " + doctor.getUser().getFullName());
                dto.setDoctorName(doctor.getUser().getFullName());
                if (!doctor.getSpecializations().isEmpty()) {
                    dto.setSpecializationName(doctor.getSpecializations().get(0).getSpecName());
                } else {
                    dto.setSpecializationName("N/A");
                }
            }, () -> System.out.println("Doctor ID " + schedule.getDoctorId() + " not found"));
        }

        if (schedule.getRoom() != null) {
            dto.setRoomName(schedule.getRoom().getRoomName());
            dto.setRoomNumber(schedule.getRoom().getRoomNumber());
        } else if (schedule.getRoomId() != null) {
            roomRepository.findById(schedule.getRoomId()).ifPresentOrElse(room -> {
                System.out.println("Found room: " + room.getRoomNumber());
                dto.setRoomName(room.getRoomName());
                dto.setRoomNumber(room.getRoomNumber());
            }, () -> System.out.println("Room ID " + schedule.getRoomId() + " not found"));
        }

        if (schedule.getPatient() != null) {
            dto.setPatientName(schedule.getPatient().getFullName());
        } else if (schedule.getPatientId() != null) {
            patientRepository.findById(schedule.getPatientId()).ifPresentOrElse(patient -> {
                System.out.println("Found patient: " + patient.getFullName());
                dto.setPatientName(patient.getFullName());
            }, () -> System.out.println("Patient ID " + schedule.getPatientId() + " not found"));
        }

        return dto;
    }

    private Schedule convertToEntity(ScheduleDTO dto) {
        Schedule schedule = new Schedule();
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

    // Updated methods with pagination
    public Page<ScheduleDTO> searchByScheduleId(Integer scheduleId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Schedule> schedules = scheduleRepository.findByScheduleIdAndDateRangePaginated(scheduleId, startDate, endDate, pageable);
        return schedules.map(this::convertToDTO);
    }

    public Page<ScheduleDTO> searchByEventType(String eventType, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Schedule> schedules = scheduleRepository.findByEventTypeAndDateRangePaginated(eventType, startDate, endDate, pageable);
        return schedules.map(this::convertToDTO);
    }

    public Page<ScheduleDTO> searchByRoomId(Integer roomId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Schedule> schedules = scheduleRepository.findByRoomIdAndDateRangePaginated(roomId, startDate, endDate, pageable);
        return schedules.map(this::convertToDTO);
    }

    public Page<ScheduleDTO> searchByStartTime(LocalTime startTime, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Schedule> schedules = scheduleRepository.findByStartTimeAndDateRangePaginated(startTime, startDate, endDate, pageable);
        return schedules.map(this::convertToDTO);
    }

    public Page<ScheduleDTO> searchByEndTime(LocalTime endTime, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Schedule> schedules = scheduleRepository.findByEndTimeAndDateRangePaginated(endTime, startDate, endDate, pageable);
        return schedules.map(this::convertToDTO);
    }

    public long countByScheduleId(Integer scheduleId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.countByScheduleIdAndDateRange(scheduleId, startDate, endDate);
    }

    public long countByEventType(String eventType, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.countByEventTypeAndDateRange(eventType, startDate, endDate);
    }

    public long countByRoomId(Integer roomId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.countByRoomIdAndDateRange(roomId, startDate, endDate);
    }

    public long countByStartTime(LocalTime startTime, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.countByStartTimeAndDateRange(startTime, startDate, endDate);
    }

    public long countByEndTime(LocalTime endTime, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.countByEndTimeAndDateRange(endTime, startDate, endDate);
    }

    @Override
    public Page<ScheduleDTO> findSchedulesFiltered(
            Integer scheduleId,
            String eventType,
            Integer roomId,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {

        // tạo Pageable với sort mặc định trên scheduleDate, startTime
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("scheduleDate").ascending()
                        .and(Sort.by("startTime")));

        // build Specification động
        Specification<Schedule> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (scheduleId != null) {
                preds.add(cb.equal(root.get("scheduleId"), scheduleId));
            }
            if (StringUtils.hasText(eventType)) {
                preds.add(cb.equal(root.get("eventType"), eventType));
            }
            if (roomId != null) {
                preds.add(cb.equal(root.get("roomId"), roomId));
            }
            if (startTime != null) {
                preds.add(cb.equal(root.get("startTime"), startTime));
            }
            if (endTime != null) {
                preds.add(cb.equal(root.get("endTime"), endTime));
            }
            if (startDate != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("scheduleDate"), startDate));
            }
            if (endDate != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("scheduleDate"), endDate));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        // thực thi query + pagination
        Page<Schedule> pageEnt = scheduleRepository.findAll(spec, pageable);

        // map sang DTO và trả về
        return pageEnt.map(this::convertToDTO);
    }
}