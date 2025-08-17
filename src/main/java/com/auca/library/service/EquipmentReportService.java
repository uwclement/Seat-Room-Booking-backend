package com.auca.library.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.auca.library.dto.response.EquipmentReportData;
import com.auca.library.model.EquipmentAssignment;
import com.auca.library.model.EquipmentUnit;
import com.auca.library.model.Location;
import com.auca.library.model.User;
import com.auca.library.repository.EquipmentAssignmentRepository;
import com.auca.library.repository.EquipmentRequestRepository;
import com.auca.library.repository.EquipmentUnitRepository;
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
public class EquipmentReportService {

    @Autowired
    private EquipmentUnitRepository equipmentUnitRepository;
    
    @Autowired
    private EquipmentAssignmentRepository assignmentRepository;
    
    @Autowired
    private EquipmentRequestRepository equipmentRequestRepository;
    
    @Autowired
    private ResourceLoader resourceLoader;

    // University configuration - can be moved to application.properties
    private static final String UNIVERSITY_NAME = "Adventist University of Central Africa";
    private static final String UNIVERSITY_LOGO_PATH = "classpath:static/images/university-logo.png";

    // Generate Equipment Report PDF
    public byte[] generateEquipmentReportPDF(Location location, String reportType, boolean detailed, User admin) 
            throws DocumentException {
        
        validateLocationAccess(location, admin);
        
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        // Add university header
        addUniversityHeader(document);
        
        // Add report title and metadata
        addReportHeader(document, reportType, location, detailed);
        
        // Generate report content based on type
        EquipmentReportData reportData = generateReportData(location, admin);
        
        if (detailed) {
            addDetailedReportContent(document, reportData, reportType);
        } else {
            addSimpleReportContent(document, reportData, reportType);
        }
        
        // Add footer
        addReportFooter(document);
        
        document.close();
        return outputStream.toByteArray();
    }

    // Generate report data
    private EquipmentReportData generateReportData(Location location, User admin) {
        EquipmentReportData data = new EquipmentReportData();
        
        // Basic counts
        data.setTotalEquipment(equipmentUnitRepository.countByLocation(location));
        data.setAvailableEquipment(equipmentUnitRepository.countByLocationAndStatus(location, EquipmentUnit.UnitStatus.AVAILABLE));
        data.setAssignedEquipment(assignmentRepository.countActiveByLocation(location));
        data.setMaintenanceEquipment(equipmentUnitRepository.countByLocationAndStatus(location, EquipmentUnit.UnitStatus.MAINTENANCE));
        data.setDamagedEquipment(equipmentUnitRepository.countByLocationAndStatus(location, EquipmentUnit.UnitStatus.DAMAGED));
        
        // Detailed lists
        data.setAllEquipmentUnits(equipmentUnitRepository.findByEquipmentLocation(location));
        data.setMaintenanceUnits(equipmentUnitRepository.findByEquipmentLocationAndStatus(location, EquipmentUnit.UnitStatus.MAINTENANCE));
        data.setDamagedUnits(equipmentUnitRepository.findByEquipmentLocationAndStatus(location, EquipmentUnit.UnitStatus.DAMAGED));
        data.setActiveAssignments(assignmentRepository.findByLocationAndStatus(location, EquipmentAssignment.AssignmentStatus.ACTIVE));
        
        // Staff assignments
        data.setStaffAssignmentCount(assignmentRepository.countActiveStaffAssignmentsByLocation(location));
        data.setRoomAssignmentCount(assignmentRepository.countActiveRoomAssignmentsByLocation(location));
        
        // Most requested equipment
        data.setMostRequestedEquipment(getMostRequestedEquipment(location));
        
        return data;
    }

    // Add university header with logo
    private void addUniversityHeader(Document document) throws DocumentException {
        try {
            // Add logo
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

    // Add report header with metadata
    private void addReportHeader(Document document, String reportType, Location location, boolean detailed) 
            throws DocumentException {
        
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLACK);
        Font metaFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);
        
        // Report title
        String title = "Equipment " + reportType + " Report" + (detailed ? " (Detailed)" : " (Summary)");
        Paragraph reportTitle = new Paragraph(title, headerFont);
        reportTitle.setAlignment(Element.ALIGN_CENTER);
        reportTitle.setSpacingAfter(15);
        document.add(reportTitle);
        
        // Report metadata
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm"));
        Paragraph metadata = new Paragraph();
        metadata.add(new Chunk("Location: " + location.name(), metaFont));
        metadata.add(Chunk.NEWLINE);
        metadata.add(new Chunk("Generated on: " + currentDate, metaFont));
        metadata.add(Chunk.NEWLINE);
        metadata.add(new Chunk("Report Type: " + (detailed ? "Detailed Analysis" : "Summary Overview"), metaFont));
        metadata.setSpacingAfter(20);
        document.add(metadata);
    }

    // Add simple report content
    private void addSimpleReportContent(Document document, EquipmentReportData data, String reportType) 
            throws DocumentException {
        
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        // Summary statistics table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        addTableHeader(summaryTable, "Equipment Summary", sectionFont);
        
        addTableRow(summaryTable, "Total Equipment Units:", data.getTotalEquipment().toString(), contentFont);
        addTableRow(summaryTable, "Available (In Stock):", data.getAvailableEquipment().toString(), contentFont);
        addTableRow(summaryTable, "Currently Assigned:", data.getAssignedEquipment().toString(), contentFont);
        addTableRow(summaryTable, "Under Maintenance:", data.getMaintenanceEquipment().toString(), contentFont);
        addTableRow(summaryTable, "Damaged/Lost:", data.getDamagedEquipment().toString(), contentFont);
        
        document.add(summaryTable);
        document.add(new Paragraph(" ")); // Space
        
        // Assignment breakdown
        PdfPTable assignmentTable = new PdfPTable(2);
        assignmentTable.setWidthPercentage(60);
        assignmentTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        addTableHeader(assignmentTable, "Assignment Breakdown", sectionFont);
        
        addTableRow(assignmentTable, "Staff Assignments:", data.getStaffAssignmentCount().toString(), contentFont);
        addTableRow(assignmentTable, "Room Assignments:", data.getRoomAssignmentCount().toString(), contentFont);
        
        document.add(assignmentTable);
    }

    // Add detailed report content
    private void addDetailedReportContent(Document document, EquipmentReportData data, String reportType) 
            throws DocumentException {
        
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        
        // Add simple summary first
        addSimpleReportContent(document, data, reportType);
        document.add(new Paragraph(" ")); // Space
        
        // Detailed equipment list
        if (!data.getAllEquipmentUnits().isEmpty()) {
            Paragraph sectionTitle = new Paragraph("Complete Equipment Inventory", sectionFont);
            sectionTitle.setSpacingBefore(10);
            document.add(sectionTitle);
            
            PdfPTable equipmentTable = new PdfPTable(5);
            equipmentTable.setWidthPercentage(100);
            equipmentTable.setWidths(new float[]{3, 2, 2, 2, 2});
            
            // Headers
            addDetailedTableHeader(equipmentTable, new String[]{"Equipment", "Serial Number", "Status", "Condition", "Assignment"}, sectionFont);
            
            for (EquipmentUnit unit : data.getAllEquipmentUnits()) {
                addDetailedTableRow(equipmentTable, new String[]{
                    unit.getEquipmentName(),
                    unit.getSerialNumber(),
                    unit.getStatus().name(),
                    unit.getCondition(),
                    getAssignmentInfo(unit)
                }, contentFont);
            }
            
            document.add(equipmentTable);
            document.add(new Paragraph(" ")); // Space
        }
        
        // Active assignments
        if (!data.getActiveAssignments().isEmpty()) {
            Paragraph sectionTitle = new Paragraph("Active Equipment Assignments", sectionFont);
            sectionTitle.setSpacingBefore(10);
            document.add(sectionTitle);
            
            PdfPTable assignmentTable = new PdfPTable(5);
            assignmentTable.setWidthPercentage(100);
            assignmentTable.setWidths(new float[]{2, 2, 2, 2, 2});
            
            // Headers
            addDetailedTableHeader(assignmentTable, new String[]{"Equipment", "Serial No.", "Assigned To", "Type", "Date"}, sectionFont);
            
            for (EquipmentAssignment assignment : data.getActiveAssignments()) {
                addDetailedTableRow(assignmentTable, new String[]{
                    assignment.getEquipmentUnit().getEquipmentName(),
                    assignment.getEquipmentUnit().getSerialNumber(),
                    assignment.getAssignedToName(),
                    assignment.getAssignmentType().name().replace("_", " "),
                    assignment.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }, contentFont);
            }
            
            document.add(assignmentTable);
            document.add(new Paragraph(" ")); // Space
        }
        
        // Maintenance/Damaged equipment
        List<EquipmentUnit> maintenanceAndDamaged = data.getMaintenanceUnits();
        maintenanceAndDamaged.addAll(data.getDamagedUnits());
        
        if (!maintenanceAndDamaged.isEmpty()) {
            Paragraph sectionTitle = new Paragraph("Equipment Under Maintenance/Damaged", sectionFont);
            sectionTitle.setSpacingBefore(10);
            document.add(sectionTitle);
            
            PdfPTable maintenanceTable = new PdfPTable(4);
            maintenanceTable.setWidthPercentage(100);
            maintenanceTable.setWidths(new float[]{3, 2, 2, 3});
            
            // Headers
            addDetailedTableHeader(maintenanceTable, new String[]{"Equipment", "Serial Number", "Status", "Notes"}, sectionFont);
            
            for (EquipmentUnit unit : maintenanceAndDamaged) {
                addDetailedTableRow(maintenanceTable, new String[]{
                    unit.getEquipmentName(),
                    unit.getSerialNumber(),
                    unit.getStatus().name(),
                    unit.getNotes() != null ? unit.getNotes() : "No notes"
                }, contentFont);
            }
            
            document.add(maintenanceTable);
        }
    }

    // Add report footer
    private void addReportFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
        
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20);
        footer.add(new Chunk("This report was generated automatically by the Equipment Management System.", footerFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("For questions or discrepancies, please contact the Equipment Administration.", footerFont));
        footer.setAlignment(Element.ALIGN_CENTER);
        
        document.add(footer);
    }

    // Helper methods for table creation
    private void addTableHeader(PdfPTable table, String title, Font font) {
        PdfPCell headerCell = new PdfPCell(new Phrase(title, font));
        headerCell.setColspan(2);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setPadding(8);
        table.addCell(headerCell);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(5);
        labelCell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setPadding(5);
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addDetailedTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addDetailedTableRow(PdfPTable table, String[] values, Font font) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    private String getAssignmentInfo(EquipmentUnit unit) {
        return assignmentRepository.findActiveByEquipmentUnit(unit)
                .map(assignment -> assignment.getAssignedToName())
                .orElse("Not Assigned");
    }

    private Map<String, Long> getMostRequestedEquipment(Location location) {
        // This would need implementation with your existing EquipmentRequestRepository
        // For now, return empty map
        return Map.of();
    }

    private void validateLocationAccess(Location location, User admin) {
        if (!admin.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"))) {
            if (!admin.getLocation().equals(location)) {
                throw new SecurityException("Access denied: Cannot generate reports for different location");
            }
        }
    }
}