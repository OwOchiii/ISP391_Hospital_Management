// src/main/java/orochi/repository/RoomRepository.java
package orochi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import orochi.model.Room;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    // Tìm theo roomNumber hoặc roomName (dùng cho search)
    @Query("SELECT r FROM Room r WHERE " +
            "LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
            "LOWER(r.roomName) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<Room> searchByNumberOrName(@Param("kw") String keyword);

    // Đếm số phòng có trạng thái “Available” (có thể dùng nếu cần)
    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = :status")
    Integer countByStatus(@Param("status") String status);
}
