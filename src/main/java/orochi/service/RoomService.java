// src/main/java/orochi/service/RoomService.java
package orochi.service;

import orochi.model.Room;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RoomService {
    // Trả về danh sách tất cả phòng
    List<Room> getAllRooms();

    // Tìm 1 phòng theo ID
    Optional<Room> findById(Integer roomId);

    // Lưu (tạo mới hoặc cập nhật)
    Room save(Room room);

    // Xóa phòng
    void deleteById(Integer roomId);

    // Đếm tổng số phòng
    Integer getTotalRooms();

    // Đếm số phòng Available
    Integer getAvailableRooms();

    // Tìm theo từ khóa (search)
    List<Room> searchRooms(String keyword);

    // NEW: Get rooms by specialty ID
    List<Room> getRoomsBySpecialtyId(Integer specialtyId);

    // NEW: Get room details by specialty ID with department info
    List<Map<String, Object>> getRoomDetailsWithDepartmentBySpecialtyId(Integer specialtyId);

    // NEW: Get available rooms by department ID
    List<Room> getAvailableRoomsByDepartmentId(Integer departmentId);

    Optional<Room> findByRoomNumber(String roomNumber);
}
