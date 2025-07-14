package orochi.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import orochi.model.Appointment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportAppointmentsToExcel(List<Appointment> appointments, String patientName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Appointments");

            // Create header font
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLUE.getIndex());

            // Create header cell style
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setBorderBottom(BorderStyle.THIN);
            headerCellStyle.setBorderTop(BorderStyle.THIN);
            headerCellStyle.setBorderRight(BorderStyle.THIN);
            headerCellStyle.setBorderLeft(BorderStyle.THIN);

            // Create data cell style
            CellStyle dataCellStyle = workbook.createCellStyle();
            dataCellStyle.setBorderBottom(BorderStyle.THIN);
            dataCellStyle.setBorderTop(BorderStyle.THIN);
            dataCellStyle.setBorderRight(BorderStyle.THIN);
            dataCellStyle.setBorderLeft(BorderStyle.THIN);

            // Create title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Appointment History for " + patientName);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Create header row
            Row headerRow = sheet.createRow(2);

            // Define columns based on the Appointment entity structure
            String[] columns = {
                    "Appointment ID",
                    "Date & Time",
                    "Doctor",
                    "Specialty",
                    "Room",
                    "Status",
                    "Description",
                    "Email",
                    "Phone"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create data rows
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            int rowNum = 3;
            for (Appointment appointment : appointments) {
                Row row = sheet.createRow(rowNum++);
                int cellIndex = 0;

                // Appointment ID
                row.createCell(cellIndex++).setCellValue(appointment.getAppointmentId());

                // Date & Time
                if (appointment.getDateTime() != null) {
                    row.createCell(cellIndex++).setCellValue(appointment.getDateTime().format(dateTimeFormatter));
                } else {
                    row.createCell(cellIndex++).setCellValue("");
                }

                // Doctor (name instead of ID)
                if (appointment.getDoctor() != null && appointment.getDoctor().getUser() != null) {
                    row.createCell(cellIndex++).setCellValue(appointment.getDoctor().getUser().getFullName());
                } else {
                    row.createCell(cellIndex++).setCellValue("");
                }

                // Doctor Specialty
                if (appointment.getDoctor() != null && appointment.getDoctor().getSpecializations() != null
                        && !appointment.getDoctor().getSpecializations().isEmpty()) {
                    row.createCell(cellIndex++).setCellValue(appointment.getDoctor().getSpecializations().get(0).getSpecName());
                } else {
                    row.createCell(cellIndex++).setCellValue("");
                }

                // Room
                if (appointment.getRoom() != null) {
                    row.createCell(cellIndex++).setCellValue("Room " + appointment.getRoom().getRoomNumber());
                } else {
                    row.createCell(cellIndex++).setCellValue("");
                }

                // Status
                row.createCell(cellIndex++).setCellValue(appointment.getStatus());

                // Description
                row.createCell(cellIndex++).setCellValue(appointment.getDescription() != null ?
                        appointment.getDescription() : "");

                // Email
                row.createCell(cellIndex++).setCellValue(appointment.getEmail() != null ?
                        appointment.getEmail() : "");

                // Phone
                row.createCell(cellIndex++).setCellValue(appointment.getPhoneNumber() != null ?
                        appointment.getPhoneNumber() : "");

                // Apply data style to all cells
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        cell.setCellStyle(dataCellStyle);
                    }
                }
            }

            // Resize all columns to fit the content
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Merge cells for the title
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns.length - 1));

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
