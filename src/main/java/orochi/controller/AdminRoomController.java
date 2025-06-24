// src/main/java/orochi/controller/RoomController.java
package orochi.controller;

import orochi.model.Room;
import orochi.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    @Autowired
    private RoomService roomService;

    /**
     * GET /admin/rooms?adminId=...
     * Hiển thị danh sách các phòng, hỗ trợ search (tìm theo roomNumber hoặc roomName),
     * và filter theo status (Available/Occupied).
     */
    @GetMapping
    public String listRooms(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            Model model) {

        // 1. Lấy danh sách phòng
        List<Room> rooms = roomService.getAllRooms();

        // 2. Nếu có search, filter trước
        if (search != null && !search.isBlank()) {
            rooms = roomService.searchRooms(search);
        }

        // 3. Nếu có filter theo status
        if (statusFilter != null && !statusFilter.isBlank()) {
            String sf = statusFilter.trim();
            rooms = rooms.stream()
                    .filter(r -> r.getStatus().equalsIgnoreCase(sf))
                    .toList();
        }

        // Đẩy lên model
        model.addAttribute("rooms", rooms);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);

        return "admin/room/list";  // Thymeleaf sẽ tìm file: templates/admin/room/list.html
    }

    /**
     * POST /admin/rooms/{id}/toggleStatus?adminId=...
     * Chuyển trạng thái phòng: nếu "Available" thì sang "Occupied", ngược lại nếu là "Occupied" thì sang "Available".
     */
    @PostMapping("/{id}/toggleStatus")
    public String toggleRoomStatus(
            @PathVariable("id") Integer roomId,
            @RequestParam("adminId") Integer adminId) {

        Optional<Room> roomOpt = roomService.findById(roomId);
        if (roomOpt.isPresent()) {
            Room r = roomOpt.get();
            // Giả sử chỉ có hai trạng thái chính: "Available" / "Occupied"
            String newStatus = r.getStatus().equalsIgnoreCase("Available")
                    ? "Occupied" : "Available";
            r.setStatus(newStatus);
            roomService.save(r);
        }
        return "redirect:/admin/rooms?adminId=" + adminId;
    }

    /**
     * POST /admin/rooms/save?adminId=...
     * Lưu thông tin phòng (cả tạo mới và cập nhật).
     */
    @PostMapping("/save")
    public String saveRoom(
            @RequestParam("adminId") Integer adminId,
            @ModelAttribute Room room) {

        // Nếu là edit, thì Room sẽ có roomId, nếu là add thì roomId==null
        // Lưu vào database
        roomService.save(room);

        return "redirect:/admin/rooms?adminId=" + adminId;
    }
}
