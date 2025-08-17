package com.auca.library.service.analytics;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.auca.library.dto.response.analytics.AnalyticsCard;
import com.auca.library.dto.response.analytics.ChartData;
import com.auca.library.dto.response.analytics.EquipmentAnalyticsSummary;
import com.auca.library.dto.response.analytics.EquipmentChartsData;
import com.auca.library.dto.response.analytics.RoomAnalyticsSummary;
import com.auca.library.dto.response.analytics.RoomChartsData;
import com.auca.library.dto.response.analytics.SeatAnalyticsSummary;
import com.auca.library.dto.response.analytics.SeatChartsData;
import com.auca.library.dto.response.analytics.UserAnalyticsSummary;
import com.auca.library.dto.response.analytics.UserChartsData;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class AnalyticsReportService {

    @Autowired
    private ResourceLoader resourceLoader;

    // University configuration
    private static final String UNIVERSITY_NAME = "Adventist University of Central Africa";
    private static final String UNIVERSITY_LOGO_PATH = "classpath:static/images/university-logo.png";

    // ===== SEAT ANALYTICS REPORTS =====

    public byte[] generateSeatSimpleReport(SeatAnalyticsSummary summary, SeatChartsData charts) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            // Add header
            addUniversityHeader(document);
            addReportTitle(document, "Seat Analytics - Simple Report", summary);
            
            // Add summary cards
            addSummaryCards(document, summary.getSummaryCards());
            
            // Add key insights
            addSeatSimpleInsights(document, summary, charts);
            
            // Add footer
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating seat simple report", e);
        }
    }

    public byte[] generateSeatDetailedReport(SeatAnalyticsSummary summary, SeatChartsData charts,
            List<Map<String, Object>> topPerformingSeats, List<Map<String, Object>> underutilizedSeats,
            List<Map<String, Object>> maintenanceSeats) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            // Add header
            addUniversityHeader(document);
            addReportTitle(document, "Seat Analytics - Detailed Report", summary);
            
            // Add summary cards
            addSummaryCards(document, summary.getSummaryCards());
            
            // Add detailed sections
            addSeatDetailedAnalysis(document, topPerformingSeats, underutilizedSeats, maintenanceSeats);
            
            // Add recommendations
            addSeatRecommendations(document, summary);
            
            addReportFooter(document);
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating seat detailed report", e);
        }
    }

    // ===== ROOM ANALYTICS REPORTS =====

    public byte[] generateRoomSimpleReport(RoomAnalyticsSummary summary, RoomChartsData charts) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            addUniversityHeader(document);
            addReportTitle(document, "Room Analytics - Simple Report", summary);
            addSummaryCards(document, summary.getSummaryCards());
            addRoomSimpleInsights(document, summary, charts);
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating room simple report", e);
        }
    }

    public byte[] generateRoomDetailedReport(RoomAnalyticsSummary summary, RoomChartsData charts,
            List<Map<String, Object>> topBookedRooms, List<Map<String, Object>> underutilizedRooms,
            List<Map<String, Object>> equipmentRequests, List<Map<String, Object>> rejectedBookings) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            addUniversityHeader(document);
            addReportTitle(document, "Room Analytics - Detailed Report", summary);
            addSummaryCards(document, summary.getSummaryCards());
            addRoomDetailedAnalysis(document, topBookedRooms, underutilizedRooms, equipmentRequests, rejectedBookings);
            addRoomRecommendations(document, summary);
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating room detailed report", e);
        }
    }

    // ===== EQUIPMENT ANALYTICS REPORTS =====

    public byte[] generateEquipmentSimpleReport(EquipmentAnalyticsSummary summary, EquipmentChartsData charts) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            addUniversityHeader(document);
            addReportTitle(document, "Equipment Analytics - Simple Report", summary);
            addSummaryCards(document, summary.getSummaryCards());
            addEquipmentSimpleInsights(document, summary, charts);
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating equipment simple report", e);
        }
    }

    public byte[] generateEquipmentDetailedReport(EquipmentAnalyticsSummary summary, EquipmentChartsData charts,
            List<Map<String, Object>> equipmentTypeBreakdown, List<Map<String, Object>> unitStatusDetails,
            List<Map<String, Object>> maintenanceUnits, List<Map<String, Object>> assignmentHistory,
            List<Map<String, Object>> requestAnalysis) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            addUniversityHeader(document);
            addReportTitle(document, "Equipment Analytics - Detailed Report", summary);
            addSummaryCards(document, summary.getSummaryCards());
            addEquipmentDetailedAnalysis(document, equipmentTypeBreakdown, unitStatusDetails, 
                maintenanceUnits, assignmentHistory, requestAnalysis);
            addEquipmentRecommendations(document, summary);
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating equipment detailed report", e);
        }
    }

    // ===== USER ANALYTICS REPORTS =====

    public byte[] generateUserSimpleReport(UserAnalyticsSummary summary, UserChartsData charts) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            addUniversityHeader(document);
            addReportTitle(document, "User Analytics - Simple Report", summary);
            addSummaryCards(document, summary.getSummaryCards());
            addUserSimpleInsights(document, summary, charts);
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating user simple report", e);
        }
    }

    public byte[] generateUserDetailedReport(UserAnalyticsSummary summary, UserChartsData charts,
            List<Map<String, Object>> userActivityBreakdown, List<Map<String, Object>> topActiveUsers,
            List<Map<String, Object>> professorApprovalStatus, List<Map<String, Object>> librarianWorkload,
            List<Map<String, Object>> newUserTrends) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            
            document.open();
            
            addUniversityHeader(document);
            addReportTitle(document, "User Analytics - Detailed Report", summary);
            addSummaryCards(document, summary.getSummaryCards());
            addUserDetailedAnalysis(document, userActivityBreakdown, topActiveUsers, 
                professorApprovalStatus, librarianWorkload, newUserTrends);
            addUserRecommendations(document, summary);
            addReportFooter(document);
            
            document.close();
            return outputStream.toByteArray();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating user detailed report", e);
        }
    }

    // ===== COMMON REPORT COMPONENTS =====

    private void addUniversityHeader(Document document) throws DocumentException {
        try {
            // Add logo if available
            Resource logoResource = resourceLoader.getResource(UNIVERSITY_LOGO_PATH);
            if (logoResource.exists()) {
                Image logo = Image.getInstance(logoResource.getInputStream().readAllBytes());
                logo.scaleToFit(60, 60);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            }
            
            // Add university name
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph universityName = new Paragraph(UNIVERSITY_NAME, titleFont);
            universityName.setAlignment(Element.ALIGN_CENTER);
            universityName.setSpacingAfter(10);
            document.add(universityName);
            
            // Add separator line
            Paragraph separator = new Paragraph("_".repeat(80));
            separator.setAlignment(Element.ALIGN_CENTER);
            separator.setSpacingAfter(15);
            document.add(separator);
            
        } catch (Exception e) {
            // If logo fails, continue without it
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph universityName = new Paragraph(UNIVERSITY_NAME, titleFont);
            universityName.setAlignment(Element.ALIGN_CENTER);
            universityName.setSpacingAfter(20);
            document.add(universityName);
        }
    }

    private void addReportTitle(Document document, String reportTitle, Object summary) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
        Font metaFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);
        
        // Report title
        Paragraph title = new Paragraph(reportTitle, headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);
        
        // Report metadata
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm"));
        Paragraph metadata = new Paragraph();
        
        if (summary instanceof SeatAnalyticsSummary) {
            SeatAnalyticsSummary seatSummary = (SeatAnalyticsSummary) summary;
            metadata.add(new Chunk("Location: " + seatSummary.getLocation(), metaFont));
            metadata.add(Chunk.NEWLINE);
            metadata.add(new Chunk("Date Range: " + seatSummary.getDateRange(), metaFont));
        } else if (summary instanceof RoomAnalyticsSummary) {
            RoomAnalyticsSummary roomSummary = (RoomAnalyticsSummary) summary;
            metadata.add(new Chunk("Location: " + roomSummary.getLocation(), metaFont));
            metadata.add(Chunk.NEWLINE);
            metadata.add(new Chunk("Date Range: " + roomSummary.getDateRange(), metaFont));
        } else if (summary instanceof EquipmentAnalyticsSummary) {
            EquipmentAnalyticsSummary equipSummary = (EquipmentAnalyticsSummary) summary;
            metadata.add(new Chunk("Location: " + equipSummary.getLocation(), metaFont));
            metadata.add(Chunk.NEWLINE);
            metadata.add(new Chunk("Date Range: " + equipSummary.getDateRange(), metaFont));
        } else if (summary instanceof UserAnalyticsSummary) {
            UserAnalyticsSummary userSummary = (UserAnalyticsSummary) summary;
            metadata.add(new Chunk("Location: " + userSummary.getLocation(), metaFont));
            metadata.add(Chunk.NEWLINE);
            metadata.add(new Chunk("Date Range: " + userSummary.getDateRange(), metaFont));
        }
        
        metadata.add(Chunk.NEWLINE);
        metadata.add(new Chunk("Generated on: " + currentDate, metaFont));
        metadata.setSpacingAfter(20);
        document.add(metadata);
    }

    private void addSummaryCards(Document document, List<AnalyticsCard> cards) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph sectionTitle = new Paragraph("Summary Statistics", sectionFont);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);
        
        for (AnalyticsCard card : cards) {
            PdfPCell titleCell = new PdfPCell(new Phrase(card.getTitle() + ":", contentFont));
            titleCell.setBorder(PdfPCell.NO_BORDER);
            titleCell.setPadding(5);
            summaryTable.addCell(titleCell);
            
            String valueText = card.getValue();
            if (card.getTrend() != null && !card.getTrend().equals("stable")) {
                valueText += " (" + (card.getTrend().equals("up") ? "↑" : "↓") + ")";
            }
            
            PdfPCell valueCell = new PdfPCell(new Phrase(valueText, contentFont));
            valueCell.setBorder(PdfPCell.NO_BORDER);
            valueCell.setPadding(5);
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(valueCell);
        }
        
        document.add(summaryTable);
        document.add(new Paragraph(" ")); // Space
    }

    private void addReportFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
        
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20);
        footer.add(new Chunk("This report was generated automatically by the Library Management System.", footerFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("For questions or additional information, please contact the Library Administration.", footerFont));
        footer.setAlignment(Element.ALIGN_CENTER);
        
        document.add(footer);
    }

    // ===== SPECIFIC REPORT CONTENT METHODS =====

    private void addSeatSimpleInsights(Document document, SeatAnalyticsSummary summary, SeatChartsData charts) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph insights = new Paragraph("Key Insights", sectionFont);
        insights.setSpacingBefore(15);
        document.add(insights);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Seat utilization patterns show optimal usage during peak hours", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Zone distribution indicates balanced usage across different study areas", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Weekly trends demonstrate consistent student engagement", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Seat availability meets current demand with room for growth", contentFont));
        
        document.add(content);
    }

    private void addSeatDetailedAnalysis(Document document, List<Map<String, Object>> topPerformingSeats,
            List<Map<String, Object>> underutilizedSeats, List<Map<String, Object>> maintenanceSeats) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        
        // Top Performing Seats
        if (!topPerformingSeats.isEmpty()) {
            Paragraph section = new Paragraph("Top Performing Seats", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"Seat Number", "Booking Count", "Zone Type"}, contentFont);
            
            for (Map<String, Object> seat : topPerformingSeats) {
                addTableRow(table, new String[]{
                    seat.get("seatNumber").toString(),
                    seat.get("bookingCount").toString(),
                    seat.get("zoneType").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
        
        // Underutilized Seats
        if (!underutilizedSeats.isEmpty()) {
            Paragraph section = new Paragraph("Underutilized Seats", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"Seat Number", "Booking Count", "Zone Type"}, contentFont);
            
            for (Map<String, Object> seat : underutilizedSeats) {
                addTableRow(table, new String[]{
                    seat.get("seatNumber").toString(),
                    seat.get("bookingCount").toString(),
                    seat.get("zoneType").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
    }

    private void addSeatRecommendations(Document document, SeatAnalyticsSummary summary) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph recommendations = new Paragraph("Recommendations", sectionFont);
        recommendations.setSpacingBefore(15);
        document.add(recommendations);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Consider relocating underutilized seats to high-demand areas", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Schedule maintenance for disabled seats during low-usage periods", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Monitor peak usage patterns to optimize seat allocation", contentFont));
        
        document.add(content);
    }

    private void addRoomSimpleInsights(Document document, RoomAnalyticsSummary summary, RoomChartsData charts) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph insights = new Paragraph("Key Insights", sectionFont);
        insights.setSpacingBefore(15);
        document.add(insights);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Room booking approval rate indicates efficient administrative processes", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Capacity utilization shows optimal room size allocation", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Equipment requests demonstrate integrated resource management", contentFont));
        
        document.add(content);
    }

    private void addRoomDetailedAnalysis(Document document, List<Map<String, Object>> topBookedRooms,
            List<Map<String, Object>> underutilizedRooms, List<Map<String, Object>> equipmentRequests,
            List<Map<String, Object>> rejectedBookings) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        
        // Top Booked Rooms
        if (!topBookedRooms.isEmpty()) {
            Paragraph section = new Paragraph("Most Booked Rooms", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"Room Number", "Room Name", "Booking Count", "Category"}, contentFont);
            
            for (Map<String, Object> room : topBookedRooms) {
                addTableRow(table, new String[]{
                    room.get("roomNumber").toString(),
                    room.get("roomName").toString(),
                    room.get("bookingCount").toString(),
                    room.get("category").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
        
        // Equipment Requests Summary
        if (!equipmentRequests.isEmpty()) {
            Paragraph section = new Paragraph("Most Requested Equipment", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"Equipment", "Request Count"}, contentFont);
            
            for (Map<String, Object> equipment : equipmentRequests) {
                addTableRow(table, new String[]{
                    equipment.get("equipmentName").toString(),
                    equipment.get("requestCount").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
    }

    private void addRoomRecommendations(Document document, RoomAnalyticsSummary summary) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph recommendations = new Paragraph("Recommendations", sectionFont);
        recommendations.setSpacingBefore(15);
        document.add(recommendations);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Optimize room allocation based on booking patterns and capacity utilization", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Review rejected booking reasons to improve approval processes", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Consider equipment bundling for frequently requested items", contentFont));
        
        document.add(content);
    }

    private void addEquipmentSimpleInsights(Document document, EquipmentAnalyticsSummary summary, EquipmentChartsData charts) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph insights = new Paragraph("Key Insights", sectionFont);
        insights.setSpacingBefore(15);
        document.add(insights);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Equipment unit status distribution shows healthy inventory management", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Request patterns indicate appropriate equipment availability", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Assignment trends demonstrate effective utilization tracking", contentFont));
        
        document.add(content);
    }

    private void addEquipmentDetailedAnalysis(Document document, List<Map<String, Object>> equipmentTypeBreakdown,
            List<Map<String, Object>> unitStatusDetails, List<Map<String, Object>> maintenanceUnits,
            List<Map<String, Object>> assignmentHistory, List<Map<String, Object>> requestAnalysis) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        
        // Equipment Type Breakdown
        if (!equipmentTypeBreakdown.isEmpty()) {
            Paragraph section = new Paragraph("Equipment Type Breakdown", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"Equipment", "Total Units", "Available", "Assigned", "Maintenance"}, contentFont);
            
            for (Map<String, Object> equipment : equipmentTypeBreakdown) {
                addTableRow(table, new String[]{
                    equipment.get("equipmentName").toString(),
                    equipment.get("totalUnits").toString(),
                    equipment.get("availableUnits").toString(),
                    equipment.get("assignedUnits").toString(),
                    equipment.get("maintenanceUnits").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
        
        // Maintenance Required Units
        if (!maintenanceUnits.isEmpty()) {
            Paragraph section = new Paragraph("Units Requiring Maintenance", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"Serial Number", "Equipment", "Status", "Condition"}, contentFont);
            
            for (Map<String, Object> unit : maintenanceUnits) {
                addTableRow(table, new String[]{
                    unit.get("serialNumber").toString(),
                    unit.get("equipmentName").toString(),
                    unit.get("status").toString(),
                    unit.get("condition").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
    }

    private void addEquipmentRecommendations(Document document, EquipmentAnalyticsSummary summary) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph recommendations = new Paragraph("Recommendations", sectionFont);
        recommendations.setSpacingBefore(15);
        document.add(recommendations);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Schedule maintenance for units showing wear patterns", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Consider purchasing additional units for high-demand equipment", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Implement preventive maintenance schedules based on usage patterns", contentFont));
        
        document.add(content);
    }

    private void addUserSimpleInsights(Document document, UserAnalyticsSummary summary, UserChartsData charts) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph insights = new Paragraph("Key Insights", sectionFont);
        insights.setSpacingBefore(15);
        document.add(insights);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• User distribution shows balanced academic community engagement", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Daily active user patterns indicate consistent library utilization", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• New user registration trends show growing community adoption", contentFont));
        
        document.add(content);
    }

    private void addUserDetailedAnalysis(Document document, List<Map<String, Object>> userActivityBreakdown,
            List<Map<String, Object>> topActiveUsers, List<Map<String, Object>> professorApprovalStatus,
            List<Map<String, Object>> librarianWorkload, List<Map<String, Object>> newUserTrends) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        
        // User Activity Breakdown
        if (!userActivityBreakdown.isEmpty()) {
            Map<String, Object> breakdown = userActivityBreakdown.get(0);
            
            Paragraph section = new Paragraph("User Activity Overview", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setSpacingBefore(10);
            
            addTableRow(table, new String[]{"Total Seat Bookings:", breakdown.get("totalSeatBookings").toString()}, contentFont);
            addTableRow(table, new String[]{"Total Room Bookings:", breakdown.get("totalRoomBookings").toString()}, contentFont);
            addTableRow(table, new String[]{"Total Equipment Requests:", breakdown.get("totalEquipmentRequests").toString()}, contentFont);
            addTableRow(table, new String[]{"Unique Active Users:", breakdown.get("uniqueActiveUsers").toString()}, contentFont);
            
            document.add(table);
        }
        
        // Top Active Users
        if (!topActiveUsers.isEmpty()) {
            Paragraph section = new Paragraph("Most Active Users", sectionFont);
            section.setSpacingBefore(15);
            document.add(section);
            
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            
            addTableHeader(table, new String[]{"User Name", "User Type", "Activity Count"}, contentFont);
            
            for (Map<String, Object> user : topActiveUsers.subList(0, Math.min(10, topActiveUsers.size()))) {
                addTableRow(table, new String[]{
                    user.get("userName").toString(),
                    user.get("userType").toString(),
                    user.get("activityCount").toString()
                }, contentFont);
            }
            
            document.add(table);
        }
    }

    private void addUserRecommendations(Document document, UserAnalyticsSummary summary) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        Paragraph recommendations = new Paragraph("Recommendations", sectionFont);
        recommendations.setSpacingBefore(15);
        document.add(recommendations);
        
        Paragraph content = new Paragraph();
        content.setSpacingBefore(10);
        content.add(new Chunk("• Engage with low-activity users to increase library utilization", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Consider expanding services based on user growth trends", contentFont));
        content.add(Chunk.NEWLINE);
        content.add(new Chunk("• Streamline professor approval processes for faster onboarding", contentFont));
        
        document.add(content);
    }

    // ===== UTILITY METHODS =====

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String[] values, Font font) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setPadding(3);
            table.addCell(cell);
        }
    }
}
