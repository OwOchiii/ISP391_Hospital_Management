// src/main/java/orochi/controller/AdminRoomController.java
package orochi.controller;

import orochi.model.Room;
import orochi.model.Department;
import orochi.service.RoomService;
import orochi.service.DepartmentService;
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

    @Autowired
    private DepartmentService departmentService;  // Thêm DepartmentService để lấy danh sách phòng ban

    /**
     * GET /admin/rooms?adminId=...
     * Hiển thị danh sách các phòng, hỗ trợ search (tìm theo roomNumber hoặc roomName),
     * filter theo status (Available/Occupied), và phân trang.
     */
    @GetMapping
    public String listRooms(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            Model model) {

        // 1. Lấy danh sách phòng có filter/search
        List<Room> rooms = roomService.getAllRooms();
        if (search != null && !search.isBlank()) {
            rooms = roomService.searchRooms(search);
        }
        if (statusFilter != null && !statusFilter.isBlank()) {
            String sf = statusFilter.trim();
            rooms = rooms.stream()
                    .filter(r -> r.getStatus().equalsIgnoreCase(sf))
                    .toList();
        }

        // 2. Phân trang trên List<Room>
        int totalItems = rooms.size();
        int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / size) : 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<Room> pageRooms = rooms.subList(fromIndex, toIndex);

        // 3. Lấy danh sách departments
        List<Department> departments = departmentService.getAllDepartments();

        // 4. Đẩy dữ liệu lên model
        model.addAttribute("rooms", pageRooms);
        model.addAttribute("departments", departments);
        model.addAttribute("adminId", adminId);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);

        return "admin/room/list";
    }

    /**
     * POST /admin/rooms/{id}/toggleStatus?adminId=...
     * Đổi trạng thái phòng: Available <-> Occupied.
     */
    @PostMapping("/{id}/toggleStatus")
    public String toggleRoomStatus(
            @PathVariable("id") Integer roomId,
            @RequestParam("adminId") Integer adminId) {

        Optional<Room> roomOpt = roomService.findById(roomId);
        if (roomOpt.isPresent()) {
            Room r = roomOpt.get();
            String newStatus = r.getStatus().equalsIgnoreCase("Available") ? "Occupied" : "Available";
            r.setStatus(newStatus);
            roomService.save(r);
        }
        return "redirect:/admin/rooms?adminId=" + adminId;
    }

    /**
     * POST /admin/rooms/save?adminId=...
     * Lưu (tạo mới hoặc cập nhật) phòng.
     */
    @PostMapping("/save")
    public String saveRoom(
            @RequestParam("adminId") Integer adminId,
            @ModelAttribute Room room) {

        roomService.save(room);
        return "redirect:/admin/rooms?adminId=" + adminId;
    }
}
