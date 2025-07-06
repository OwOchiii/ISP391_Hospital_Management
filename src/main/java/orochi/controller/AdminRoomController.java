// src/main/java/orochi/controller/AdminRoomController.java
package orochi.controller;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
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

            @RequestParam(value = "search",       required = false) String  search,
            @RequestParam(value = "statusFilter", required = false) String  statusFilter,

            @RequestParam(value = "typeFilter",   required = false) String  typeFilter,
            @RequestParam(value = "deptFilter",   required = false) Integer deptFilter,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,

            Model model) {

        List<Room> rooms = roomService.getAllRooms();

        if (search != null && !search.isBlank()) {
            rooms = roomService.searchRooms(search.trim());
        }

        if (statusFilter != null && !statusFilter.isBlank()) {
            String sf = statusFilter.trim();
            rooms = rooms.stream()
                    .filter(r -> r.getStatus().equalsIgnoreCase(sf))
                    .toList();
        }

        if (typeFilter != null && !typeFilter.isBlank()) {
            String tf = typeFilter.trim();
            rooms = rooms.stream()
                    .filter(r -> r.getType().equalsIgnoreCase(tf))
                    .toList();
        }

        if (deptFilter != null) {
            rooms = rooms.stream()
                    .filter(r -> r.getDepartmentId().equals(deptFilter))
                    .toList();
        }

        int totalItems  = rooms.size();
        int totalPages  = totalItems > 0 ? (int) Math.ceil((double)totalItems / size) : 1;
        page = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = page * size;
        int toIndex   = Math.min(fromIndex + size, totalItems);
        List<Room> pageRooms = rooms.subList(fromIndex, toIndex);

        List<Department> departments = departmentService.getAllDepartments();

        List<String> types = rooms.stream()
                .map(Room::getType)
                .distinct()
                .toList();

        model.addAttribute("rooms",         pageRooms);
        model.addAttribute("departments",   departments);
        model.addAttribute("types",         types);

        model.addAttribute("search",        search);
        model.addAttribute("statusFilter",  statusFilter);
        model.addAttribute("typeFilter",    typeFilter);
        model.addAttribute("deptFilter",    deptFilter);

        model.addAttribute("currentPage",   page);
        model.addAttribute("totalPages",    totalPages);
        model.addAttribute("pageSize",      size);

        model.addAttribute("room",          new Room());

        model.addAttribute("adminId",       adminId);
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
            @Valid @ModelAttribute("room") Room room,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("search", null);
            model.addAttribute("statusFilter", null);
            return "admin/room/list";
        }

        roomService.save(room);
        return "redirect:/admin/rooms?adminId=" + adminId;
    }
}
