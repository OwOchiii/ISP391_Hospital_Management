package orochi.service.impl;

import orochi.model.Doctor;
import orochi.model.Room;
import orochi.model.Schedule;
import orochi.repository.DoctorRepository;
import orochi.repository.RoomRepository;
import orochi.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleServiceImpl {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private RoomRepository roomRepository;

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public List<Schedule> searchSchedules(String keyword, LocalDate date) {
        if (keyword != null && date != null) {
            return scheduleRepository.findByKeywordAndDate(keyword, date);
        } else if (date != null) {
            return scheduleRepository.findByScheduleDate(date);
        } else if (keyword != null) {
            return scheduleRepository.findByKeywordAndDate(keyword, null);
        }
        return scheduleRepository.findAll();
    }

    public Schedule saveSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(Integer scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}