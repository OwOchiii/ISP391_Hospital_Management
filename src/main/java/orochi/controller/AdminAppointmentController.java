package orochi.controller;

import orochi.config.CustomUserDetails;
import orochi.model.Appointment;
import orochi.service.AppointmentMetricService;
import orochi.service.AppointmentService;
import orochi.service.PdfExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.itextpdf.text.DocumentException;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/admin/appointments")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentMetricService appointmentMetricService;

    @Autowired
    private PdfExportService pdfService;

    // Status constants that match the database constraints
    public static final String STATUS_SCHEDULED = "Scheduled";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_CANCELLED = "Cancel";
    public static final String STATUS_PENDING = "Pending";

    @Autowired
    public AdminAppointmentController(AppointmentService appointmentService,
                                      AppointmentMetricService appointmentMetricService) {
        this.appointmentService = appointmentService;
        this.appointmentMetricService = appointmentMetricService;
    }

    @GetMapping
    public String showAppointments(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(defaultValue = "ALL") String status,
                                   @RequestParam(required = false) String search,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Appointment> appointments;

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            Integer doctorId = userDetails.getDoctorId();
            if (search != null && !search.isBlank()) {
                appointments = appointmentService.searchAppointmentsByDoctorId(doctorId, search, pageable);
            } else {
                appointments = appointmentService.getAppointmentsByDoctorId(doctorId, pageable);
            }
            model.addAttribute("currentStatus", "ALL");
        } else {
            if ("ALL".equals(status)) {
                appointments = appointmentService.getAllAppointments(pageable);
            } else {
                appointments = appointmentService.getAppointmentsByStatus(status, pageable);
            }
            model.addAttribute("currentStatus", status);
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("totalAppointments", appointmentMetricService.getTotalAppointments());
        model.addAttribute("inProgressCount", appointmentMetricService.getTotalAppointments(STATUS_PENDING));
        model.addAttribute("completedCount", appointmentMetricService.getTotalAppointments(STATUS_COMPLETED));
        model.addAttribute("rejectedCount", appointmentMetricService.getTotalAppointments(STATUS_CANCELLED));
        model.addAttribute("search", search);

        // Add Chart.js data
        model.addAttribute("chartData", getChartData());

        return "admin/appointments";
    }

    @PostMapping("/updateStatus")
    public String updateAppointmentStatus(@RequestParam Integer appointmentId,
                                          @RequestParam String status,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getAuthorities().stream().noneMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_DOCTOR") ||
                a.getAuthority().equals("ROLE_RECEPTIONIST"))) {
            throw new SecurityException("Unauthorized action");
        }

        // Just pass the string status - no enum conversion
        appointmentService.updateAppointmentStatus(appointmentId, status);
        return "redirect:/admin/appointments";
    }

    private String getChartData() {
        return "{"
                + "\"labels\": [\"Pending\", \"Completed\", \"Cancelled\", \"Scheduled\"],"
                + "\"datasets\": [{"
                + "\"label\": \"Appointments\","
                + "\"data\": ["
                + appointmentMetricService.getAppointmentsByStatus(STATUS_PENDING) + ","
                + appointmentMetricService.getAppointmentsByStatus(STATUS_COMPLETED) + ","
                + appointmentMetricService.getAppointmentsByStatus(STATUS_CANCELLED) + ","
                + appointmentMetricService.getAppointmentsByStatus(STATUS_SCHEDULED)
                + "],"
                + "\"backgroundColor\": [\"#36A2EB\", \"#4CAF50\", \"#FF6384\", \"#FFCE56\"],"
                + "\"borderColor\": [\"#36A2EB\", \"#4CAF50\", \"#FF6384\", \"#FFCE56\"],"
                + "\"borderWidth\": 1"
                + "}]"
                + "}";
    }

    @GetMapping("/statistics/appointments")
    public String showOrExportStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") Integer doctorId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String export,
            @RequestParam(defaultValue = "month") String groupBy,
            Model model,
            HttpServletResponse response
    ) throws IOException, DocumentException {
        if (fromDate == null) fromDate = LocalDate.now().withDayOfMonth(1);
        if (toDate   == null) toDate   = LocalDate.now();
        Integer docFilter = doctorId > 0 ? doctorId : null;
        Integer specFilter = null;

        // 1) Lấy 3 map gốc
        Map<String, Long> mapAll     = appointmentService.fetchPeriodCounts2(fromDate, toDate, docFilter, specFilter, status,      groupBy);
        Map<String, Long> mapDone    = appointmentService.fetchPeriodCounts2(fromDate, toDate, docFilter, specFilter, STATUS_COMPLETED, groupBy);
        Map<String, Long> mapCancel  = appointmentService.fetchPeriodCounts2(fromDate, toDate, docFilter, specFilter, STATUS_CANCELLED, groupBy);

        // 2) Tính tổng mỗi loại
        long totalSched = mapAll.values().stream().mapToLong(l -> l).sum()
                - mapDone.values().stream().mapToLong(l -> l).sum()
                - mapCancel.values().stream().mapToLong(l -> l).sum();
        long totalDone  = mapDone.values().stream().mapToLong(l -> l).sum();
        long totalCanc  = mapCancel.values().stream().mapToLong(l -> l).sum();

        // 3) Chuẩn bị zero-map helper
        Function<Set<String>, Map<String,Long>> zeroMap = keys ->
                keys.stream().collect(Collectors.toMap(
                        k -> k, k -> 0L,
                        (a,b)->b, LinkedHashMap::new
                ));

        // 4) Build 3 map thực tế để bind cho view
        Map<String,Long> dispSched, dispDone, dispCanc, dispTotal;
        switch(status.toUpperCase()) {
            case "SCHEDULED":
                dispTotal = mapAll;              // mapAll tại đây thực chất là chỉ slot scheduled (vì status filter)
                dispSched = mapAll;
                dispDone  = zeroMap.apply(mapAll.keySet());
                dispCanc  = zeroMap.apply(mapAll.keySet());
                break;
            case "COMPLETED":
                dispTotal = mapDone;
                dispSched = zeroMap.apply(mapDone.keySet());
                dispDone  = mapDone;
                dispCanc  = zeroMap.apply(mapDone.keySet());
                break;
            case "CANCEL","CANCELLED":
                dispTotal = mapCancel;
                dispSched = zeroMap.apply(mapCancel.keySet());
                dispDone  = zeroMap.apply(mapCancel.keySet());
                dispCanc  = mapCancel;
                break;
            default:  // ALL
                dispTotal = mapAll;
                // scheduled = all – done – cancel
                dispSched = mapAll.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue()
                                        - mapDone.getOrDefault(e.getKey(),0L)
                                        - mapCancel.getOrDefault(e.getKey(),0L),
                                (a,b)->b, LinkedHashMap::new
                        ));
                dispDone  = mapDone;
                dispCanc  = mapCancel;
        }

        long totalCount = dispTotal.values().stream().mapToLong(l->l).sum();
        long schedCount = dispSched.values().stream().mapToLong(l->l).sum();
        long doneCount  = dispDone.values().stream().mapToLong(l->l).sum();
        long canCount   = dispCanc.values().stream().mapToLong(l->l).sum();
        double rateDone = totalCount>0? doneCount*100.0/totalCount : 0;
        double rateCan  = totalCount>0? canCount*100.0/totalCount : 0;

        // 5) Xuất PDF nếu cần
        if ("pdf".equalsIgnoreCase(export)) {
            // thiết lập để browser download file
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"appointment_statistics.pdf\"");

            // gọi service sinh PDF, truyền null cho search vì bạn không có filter nào tương ứng
            pdfService.generateAppointmentStatisticsPdf(
                    fromDate,
                    toDate,
                    docFilter,
                    specFilter,
                    status,
                    null,
                    totalCount,
                    doneCount,
                    canCount,
                    rateDone,
                    rateCan,
                    dispTotal,
                    dispDone,
                    dispCanc,
                    response.getOutputStream()
            );
            return null;
        }




        // 5) Bind tất cả về view
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate",   toDate);
        model.addAttribute("doctorList",    appointmentService.getAllDoctors());
        model.addAttribute("selectedDoctor", doctorId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("groupBy",        groupBy);

        model.addAttribute("total",     totalCount);
        model.addAttribute("scheduled", schedCount);
        model.addAttribute("done",      doneCount);
        model.addAttribute("canceled",  canCount);
        model.addAttribute("rateDone",  rateDone);
        model.addAttribute("rateCan",   rateCan);

        // --- VÀ DÙNG 3 MAP ĐÃ ĐIỀU CHỈNH ĐỂ VẼ BIỂU ĐỒ ---
        model.addAttribute("periodTotals",   dispTotal);
        model.addAttribute("periodScheduled", dispSched);
        model.addAttribute("periodDone",     dispDone);
        model.addAttribute("periodCanceled", dispCanc);

        return "admin/appointmentStatistics";
    }


    /**
     * Trả về một LinkedHashMap cùng key với source nhưng tất cả value = 0L
     */
    private Map<String,Long> zeroMap(Map<String,Long> source) {
        return source.keySet().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        k -> 0L,
                        (a,b)->a,
                        LinkedHashMap::new
                ));
    }

}