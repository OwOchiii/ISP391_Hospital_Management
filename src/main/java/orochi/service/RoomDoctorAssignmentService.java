package orochi.service;

import orochi.model.Doctor;
import orochi.model.Room;
import orochi.model.Schedule;
import orochi.repository.DoctorRepository;
import orochi.repository.RoomRepository;
import orochi.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RoomDoctorAssignmentService {

    @Autowired private RoomRepository roomRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private ScheduleRepository scheduleRepo;

    /**
     * Trả về list bác sĩ cố định của room nhưng chưa assign on‑call
     * @param roomId  ID phòng
     * @param date    ngày trực (yyyy‑MM‑dd)
     * @param shift   "morning"|"afternoon"
     */
    public List<Doctor> findEligibleOncallDoctors(Integer roomId, LocalDate date, String shift) {
        // 1) Lấy Room từ DB
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
        String desc = room.getNotes();
        List<Integer> mappedIds = parseMappedDoctorIds(desc);

        // 2) Xác định giờ bắt đầu của ca
        LocalTime shiftStart = "morning".equals(shift)
                ? LocalTime.of(8, 0)
                : LocalTime.of(13, 0);

        // 3) Lấy tất cả lịch on‑call cùng phòng + ngày (không quan tâm đến giờ)
        List<Schedule> allOncall =
                scheduleRepo.findByRoomIdAndEventTypeAndScheduleDate(roomId, "oncall", date);

        // 4) Lọc ra những lịch đã dùng đúng ca đó rồi
        List<Integer> usedIds = allOncall.stream()
                .filter(s -> s.getStartTime().equals(shiftStart))   // startTime là LocalTime rồi
                .map(Schedule::getDoctorId)
                .collect(Collectors.toList());

        // 5) Trả về những bác sĩ trong mapping nhưng chưa dùng
        return doctorRepo.findAllById(
                mappedIds.stream()
                        .filter(id -> !usedIds.contains(id))
                        .collect(Collectors.toList())
        );
    }


    // parse "oncallDoctors:10,11" → [10,11]
    private List<Integer> parseMappedDoctorIds(String desc) {
        if (desc != null && desc.startsWith("oncallDoctors:")) {
            return Arrays.stream(desc.substring("oncallDoctors:".length()).split(","))
                    .map(String::trim).filter(s->!s.isEmpty())
                    .map(Integer::valueOf).toList();
        }
        return List.of();
    }

    public List<Doctor> findDoctorsByRoom(Integer roomId) {
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

        // ví dụ bạn encode doctor IDs trong room.getNotes() giống oncall: "oncallDoctors:10,11"
        List<Integer> mappedIds = parseMappedDoctorIds(room.getNotes());

        // nếu không muốn dùng notes, thay parseMappedDoctorIds bằng repo mapping riêng
        return mappedIds.isEmpty()
                ? Collections.emptyList()
                : doctorRepo.findAllById(mappedIds);
    }

    public List<Doctor> findEligibleDoctorsForShift(
            Integer roomId,
            String shift,      // "morning" hoặc "afternoon"
            LocalDate date     // ngày trực
    ) {
        // 1) Xác định chuỗi giờ của ca
        String shiftStart = "morning".equals(shift) ? "08:00:00" : "13:00:00";

        // 2) Lấy lịch lịch sử chỉ quan tâm đến room+shift (bỏ eventType)
        List<Schedule> history = scheduleRepo.findHistoryByRoomAndShift(roomId, shiftStart);

        // 3) Nếu đã có lịch, fix luôn doctor đầu tiên
        if (!history.isEmpty()) {
            Integer firstDocId = history.get(0).getDoctorId();
            return doctorRepo.findById(firstDocId)
                    .map(List::of)
                    .orElse(List.of());
        }

        // 4) Lần đầu: lấy toàn bộ bác sĩ, nhưng loại bỏ những ai đã có schedule date này
        List<Integer> busyDoctorIds = scheduleRepo.findByScheduleDate(date).stream()
                .map(Schedule::getDoctorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return doctorRepo.findAll().stream()
                .filter(d -> !busyDoctorIds.contains(d.getDoctorId()))
                .toList();
    }

}