// src/main/java/orochi/service/RoomService.java
package orochi.service;

import orochi.model.Room;

import java.util.List;
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
}
