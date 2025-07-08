package orochi.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Map;

@Service
public class PdfExportService {

    public void generateAppointmentStatisticsPdf(
            LocalDate fromDate,
            LocalDate toDate,
            Integer doctorId,
            Integer specializationId,
            String status,
            String search,
            long total,
            long done,
            long canceled,
            double rateDone,
            double rateCan,
            Map<String, Long> monthlyTotals,
            Map<String, Long> monthlyDone,
            Map<String, Long> monthlyCanceled,
            OutputStream out
    ) throws DocumentException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // 1. Title
        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Appointment Statistics", h1);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        doc.add(Chunk.NEWLINE);

        // 2. Filters info
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPTable info = new PdfPTable(3);
        info.setWidths(new float[]{1,1,1});
        info.setWidthPercentage(100);
        info.addCell(new PdfPCell(new Phrase("From: " + fromDate, small)));
        info.addCell(new PdfPCell(new Phrase("To: "   + toDate,   small)));
        info.addCell(new PdfPCell(new Phrase("Doctor ID: " + (doctorId==null?"ALL":doctorId), small)));
        info.addCell(new PdfPCell(new Phrase("Specialty ID: " + (specializationId==null?"ALL":specializationId), small)));
        info.addCell(new PdfPCell(new Phrase("Status: " + status, small)));
        info.addCell(new PdfPCell(new Phrase("Keyword: " + (search==null?"":search), small)));
        info.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        doc.add(info);
        doc.add(Chunk.NEWLINE);

        // 3. Summary cards as a table
        PdfPTable summary = new PdfPTable(3);
        summary.setWidthPercentage(100);
        Font valF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        summary.addCell(cell("Total", small));
        summary.addCell(cell("Attendance Rate", small));
        summary.addCell(cell("Cancel Rate", small));
        summary.addCell(cell(Long.toString(total), valF));
        summary.addCell(cell(String.format("%.2f%%", rateDone), valF));
        summary.addCell(cell(String.format("%.2f%%", rateCan), valF));
        doc.add(summary);
        doc.add(Chunk.NEWLINE);

        // 4. Monthly data table
        PdfPTable months = new PdfPTable(4);
        months.setWidthPercentage(100);
        months.addCell(cell("Month", small));
        months.addCell(cell("Total", small));
        months.addCell(cell("Done", small));
        months.addCell(cell("Canceled", small));
        for (String m : monthlyTotals.keySet()) {
            months.addCell(cell(m, small));
            months.addCell(cell(monthlyTotals.get(m).toString(), small));
            months.addCell(cell(monthlyDone   .getOrDefault(m,0L).toString(), small));
            months.addCell(cell(monthlyCanceled.getOrDefault(m,0L).toString(), small));
        }
        doc.add(months);

        doc.close();
    }

    private PdfPCell cell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(4);
        return c;
    }
}
