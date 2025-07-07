// src/main/java/orochi/service/impl/RoomServiceImpl.java
package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.Room;
import orochi.repository.RoomRepository;
import orochi.service.RoomService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Optional<Room> findById(Integer roomId) {
        return roomRepository.findById(roomId);
    }

    @Override
    public Room save(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public void deleteById(Integer roomId) {
        roomRepository.deleteById(roomId);
    }

    @Override
    public Integer getTotalRooms() {
        try {
            return Math.toIntExact(roomRepository.count());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getAvailableRooms() {
        try {
            return roomRepository.countByStatus("Available");
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<Room> searchRooms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return roomRepository.findAll();
        }
        return roomRepository.searchByNumberOrName(keyword.trim());
    }

    /**
     * Get rooms by doctor and specialty
     * Implements the SQL logic: SELECT DISTINCT s.SpecName AS SpecializationName, u.FullName AS DoctorName,
     * r.RoomID, r.RoomNumber, d.DeptName AS DepartmentName
     * FROM Specialization s INNER JOIN DoctorSpecialization ds ON s.SpecID = ds.SpecID
     * INNER JOIN Doctor doc ON ds.DoctorID = doc.DoctorID
     * INNER JOIN Users u ON doc.UserID = u.UserID
     * LEFT JOIN Schedule sch ON doc.DoctorID = sch.DoctorID
     * LEFT JOIN Room r ON sch.RoomID = r.RoomID
     * LEFT JOIN Department d ON r.DepartmentID = d.DepartmentID
     */
    public List<Map<String, Object>> getRoomsByDoctorAndSpecialty(Integer doctorId, Integer specialtyId) {
        List<Object[]> results = roomRepository.findRoomsByDoctorAndSpecialty(doctorId, specialtyId);

        return results.stream()
                .map(row -> {
                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("SpecializationName", row[0]);
                    roomData.put("DoctorName", row[1]);
                    roomData.put("RoomID", row[2]);
                    roomData.put("RoomNumber", row[3]);
                    roomData.put("DepartmentName", row[4]);
                    return roomData;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Room> getRoomsBySpecialtyId(Integer specialtyId) {
        try {
            // Try the JPA query first
            return roomRepository.findRoomsBySpecialtyId(specialtyId);
        } catch (Exception e) {
            // If JPA query fails, fall back to native query
            System.err.println("JPA query failed, falling back to native query: " + e.getMessage());
            return roomRepository.findRoomsBySpecialtyIdNative(specialtyId);
        }
    }

    @Override
    public List<Map<String, Object>> getRoomDetailsWithDepartmentBySpecialtyId(Integer specialtyId) {
        try {
            List<Object[]> results = roomRepository.findDetailedRoomsBySpecialtyId(specialtyId);

            return results.stream()
                    .map(row -> {
                        Map<String, Object> roomData = new HashMap<>();
                        roomData.put("roomId", row[0]);
                        roomData.put("roomNumber", row[1]);
                        roomData.put("roomName", row[2]);
                        roomData.put("type", row[3]);
                        roomData.put("capacity", row[4]);
                        roomData.put("status", row[5]);
                        roomData.put("departmentId", row[6]);
                        roomData.put("notes", row[7]);
                        roomData.put("departmentName", row[8]);
                        return roomData;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching detailed rooms by specialty: " + e.getMessage());
            // Return simplified room data as fallback
            List<Room> rooms = getRoomsBySpecialtyId(specialtyId);
            return rooms.stream()
                    .map(room -> {
                        Map<String, Object> roomData = new HashMap<>();
                        roomData.put("roomId", room.getRoomId());
                        roomData.put("roomNumber", room.getRoomNumber());
                        roomData.put("roomName", room.getRoomName());
                        roomData.put("type", room.getType());
                        roomData.put("capacity", room.getCapacity());
                        roomData.put("status", room.getStatus());
                        roomData.put("departmentId", room.getDepartmentId());
                        roomData.put("notes", room.getNotes());
                        roomData.put("departmentName", room.getDepartment() != null ? room.getDepartment().getDeptName() : "Unknown");
                        return roomData;
                    })
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Room> getAvailableRoomsByDepartmentId(Integer departmentId) {
        if (departmentId == null) {
            return roomRepository.findAllAvailableRooms();
        }
        return roomRepository.findByDepartmentIdAndStatusAvailable(departmentId);
    }
}
