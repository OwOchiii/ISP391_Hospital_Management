package orochi.controller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import orochi.service.RevenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports/revenue")
public class AdminRevenueController {

    private final RevenueService revenueService;

    @Autowired
    public AdminRevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping
    public String showRevenuePage(
            @RequestParam("adminId") Integer adminId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "year", required = false) Integer year,
            Model model
    ) {
        LocalDate today = LocalDate.now();

        // Mặc định: đầu tháng đến hôm nay, và năm hiện tại
        if (from == null) from = today.withDayOfMonth(1);
        if (to == null)   to   = today;
        if (year == null) year = today.getYear();

        // Lấy dữ liệu từ service
        Double totalRevenue = revenueService.getTotalRevenue(from, to);
        List<Map<String, Object>> byDept = revenueService.getRevenueByDepartment(from, to);
        List<Map<String, Object>> byDoc  = revenueService.getRevenueByDoctor(from, to);
        List<Map<String, Object>> monthlyComp   = revenueService.getMonthlyComparison(year);
        List<Map<String, Object>> quarterlyComp = revenueService.getQuarterlyComparison(year);
        List<Map<String, Object>> details       = revenueService.getReceiptDetails(from, to);
        int pct       = revenueService.computePeriodGrowth(from, to);
        int pctYear   = revenueService.computeYearlyGrowth(from, to);
        model.addAttribute("percentChange", pct);
        model.addAttribute("percentMonthlyChange", pctYear);

        model.addAttribute("adminId", adminId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("year", year);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("revenueByDepartment", byDept);
        model.addAttribute("revenueByDoctor", byDoc);
        model.addAttribute("monthlyComparison", monthlyComp);
        model.addAttribute("quarterlyComparison", quarterlyComp);
        model.addAttribute("receiptDetails", details);

        return "admin/reports";
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportRevenuePdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws DocumentException {
        double totalRevenue = revenueService.getTotalRevenue(from, to);
        List<Map<String, Object>> revenueByDept = revenueService.getRevenueByDepartment(from, to);
        List<Map<String, Object>> revenueByDoc  = revenueService.getRevenueByDoctor(from, to);

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont  = FontFactory.getFont(FontFactory.HELVETICA, 10);

        document.add(new Paragraph("REVENUE REPORT", titleFont));
        document.add(new Paragraph(String.format("Period: %s to %s", from, to), normalFont));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("1. Total Revenue: " + String.format("%,.2f", totalRevenue), sectionFont));
        document.add(Chunk.NEWLINE);

        // Bảng doanh thu theo khoa
        document.add(new Paragraph("2. Revenue by Department", sectionFont));
        PdfPTable deptTable = new PdfPTable(2);
        deptTable.setWidths(new int[]{3, 2});
        deptTable.addCell("Department");
        deptTable.addCell("Revenue");
        for (Map<String, Object> row : revenueByDept) {
            deptTable.addCell(row.get("dept").toString());
            deptTable.addCell(String.format("%,.2f", row.get("revenue")));
        }
        document.add(deptTable);
        document.add(Chunk.NEWLINE);

        // Bảng doanh thu theo bác sĩ
        document.add(new Paragraph("3. Revenue by Doctor", sectionFont));
        PdfPTable docTable = new PdfPTable(2);
        docTable.setWidths(new int[]{3, 2});
        docTable.addCell("Doctor");
        docTable.addCell("Revenue");
        for (Map<String, Object> row : revenueByDoc) {
            docTable.addCell(row.get("doctor").toString());
            docTable.addCell(String.format("%,.2f", row.get("revenue")));
        }
        document.add(docTable);

        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("RevenueReport.pdf")
                .build());

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(baos.toByteArray());
    }
}
