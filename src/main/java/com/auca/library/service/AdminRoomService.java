package com.auca.library.service;

import com.auca.library.dto.request.*;
import com.auca.library.dto.response.*;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.*;
import com.auca.library.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private RoomTemplateRepository roomTemplateRepository;

    // === Room CRUD Operations ===

    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse getRoomById(Long id) {
        Room room = findRoomById(id);
        return mapToResponse(room);
    }

    public List<RoomResponse> getRoomsByCategory(RoomCategory category) {
        return roomRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {
        // Validate room number uniqueness
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new IllegalArgumentException("Room with number '" + request.getRoomNumber() + "' already exists");
        }

        // Create room entity
        Room room = new Room(
                request.getRoomNumber(),
                request.getName(),
                request.getCategory(),
                request.getCapacity(),
                request.getMaxBookingHours()
        );

        // Set optional fields
        room.setDescription(request.getDescription());
        room.setMaxBookingsPerDay(request.getMaxBookingsPerDay());
        room.setAdvanceBookingDays(request.getAdvanceBookingDays());
        room.setAvailable(request.isAvailable());
        room.setBuilding(request.getBuilding());
        room.setFloor(request.getFloor());
        room.setDepartment(request.getDepartment());
        room.setMaintenanceStart(request.getMaintenanceStart());
        room.setMaintenanceEnd(request.getMaintenanceEnd());
        room.setMaintenanceNotes(request.getMaintenanceNotes());
        room.setRequiresApproval(request.isRequiresApproval());

        // Add equipment if specified
        if (request.getEquipmentIds() != null && !request.getEquipmentIds().isEmpty()) {
            Set<Equipment> equipment = new HashSet<>(equipmentRepository.findAllById(request.getEquipmentIds()));
            room.setEquipment(equipment);
        }

        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomUpdateRequest request) {
        Room room = findRoomById(id);

        // Update fields if provided
        if (request.getName() != null) room.setName(request.getName());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getCategory() != null) room.setCategory(request.getCategory());
        if (request.getCapacity() != null) room.setCapacity(request.getCapacity());
        if (request.getMaxBookingHours() != null) room.setMaxBookingHours(request.getMaxBookingHours());
        if (request.getMaxBookingsPerDay() != null) room.setMaxBookingsPerDay(request.getMaxBookingsPerDay());
        if (request.getAdvanceBookingDays() != null) room.setAdvanceBookingDays(request.getAdvanceBookingDays());
        if (request.getAvailable() != null) room.setAvailable(request.getAvailable());
        if (request.getBuilding() != null) room.setBuilding(request.getBuilding());
        if (request.getFloor() != null) room.setFloor(request.getFloor());
        if (request.getDepartment() != null) room.setDepartment(request.getDepartment());
        if (request.getMaintenanceStart() != null) room.setMaintenanceStart(request.getMaintenanceStart());
        if (request.getMaintenanceEnd() != null) room.setMaintenanceEnd(request.getMaintenanceEnd());
        if (request.getMaintenanceNotes() != null) room.setMaintenanceNotes(request.getMaintenanceNotes());
        if (request.getRequiresApproval() != null) room.setRequiresApproval(request.getRequiresApproval());

        // Update equipment if specified
        if (request.getEquipmentIds() != null) {
            Set<Equipment> equipment = new HashSet<>(equipmentRepository.findAllById(request.getEquipmentIds()));
            room.setEquipment(equipment);
        }

        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Transactional
    public MessageResponse deleteRoom(Long id) {
        Room room = findRoomById(id);
        roomRepository.delete(room);
        return new MessageResponse("Room deleted successfully");
    }

    // === Room Status Management ===

    @Transactional
    public RoomResponse toggleRoomAvailability(Long id) {
        Room room = findRoomById(id);
        room.setAvailable(!room.isAvailable());
        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Transactional
    public RoomResponse setMaintenanceWindow(Long id, LocalDateTime startTime, LocalDateTime endTime, String notes) {
        Room room = findRoomById(id);
        room.setMaintenanceStart(startTime);
        room.setMaintenanceEnd(endTime);
        room.setMaintenanceNotes(notes);
        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Transactional
    public RoomResponse clearMaintenanceWindow(Long id) {
        Room room = findRoomById(id);
        room.setMaintenanceStart(null);
        room.setMaintenanceEnd(null);
        room.setMaintenanceNotes(null);
        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    // === Equipment Management ===

    @Transactional
    public RoomResponse addEquipmentToRoom(Long roomId, Set<Long> equipmentIds) {
        Room room = findRoomById(roomId);
        Set<Equipment> equipment = new HashSet<>(equipmentRepository.findAllById(equipmentIds));
        
        room.getEquipment().addAll(equipment);
        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Transactional
    public RoomResponse removeEquipmentFromRoom(Long roomId, Set<Long> equipmentIds) {
        Room room = findRoomById(roomId);
        Set<Equipment> equipmentToRemove = new HashSet<>(equipmentRepository.findAllById(equipmentIds));
        
        room.getEquipment().removeAll(equipmentToRemove);
        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    // === Bulk Operations ===

    @Transactional
    public MessageResponse performBulkOperation(BulkRoomOperationRequest request) {
        List<Room> rooms = roomRepository.findAllById(request.getRoomIds());
        
        if (rooms.size() != request.getRoomIds().size()) {
            throw new ResourceNotFoundException("Some rooms were not found");
        }

        switch (request.getOperation().toLowerCase()) {
            case "enable":
                rooms.forEach(room -> room.setAvailable(true));
                break;
            case "disable":
                rooms.forEach(room -> room.setAvailable(false));
                break;
            case "set_maintenance":
                rooms.forEach(room -> {
                    room.setMaintenanceStart(request.getMaintenanceStart());
                    room.setMaintenanceEnd(request.getMaintenanceEnd());
                    room.setMaintenanceNotes(request.getMaintenanceNotes());
                });
                break;
            case "clear_maintenance":
                rooms.forEach(room -> {
                    room.setMaintenanceStart(null);
                    room.setMaintenanceEnd(null);
                    room.setMaintenanceNotes(null);
                });
                break;
            case "add_equipment":
                if (request.getEquipmentIds() != null) {
                    Set<Equipment> equipment = new HashSet<>(equipmentRepository.findAllById(request.getEquipmentIds()));
                    rooms.forEach(room -> room.getEquipment().addAll(equipment));
                }
                break;
            case "remove_equipment":
                if (request.getEquipmentIds() != null) {
                    Set<Equipment> equipment = new HashSet<>(equipmentRepository.findAllById(request.getEquipmentIds()));
                    rooms.forEach(room -> room.getEquipment().removeAll(equipment));
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + request.getOperation());
        }

        roomRepository.saveAll(rooms);
        return new MessageResponse("Bulk operation completed successfully on " + rooms.size() + " rooms");
    }

    // === Search and Filter ===

    public List<RoomResponse> searchRooms(String keyword) {
        return roomRepository.searchRooms(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> filterRooms(RoomCategory category, Boolean available, Integer minCapacity, 
                                         Integer maxCapacity, String building, String floor, String department) {
        return roomRepository.findRoomsWithFilters(category, available, minCapacity, maxCapacity, building, floor, department)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // === Room Templates ===

    public List<RoomTemplateResponse> getAllTemplates() {
        return roomTemplateRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::mapTemplateToResponse)
                .collect(Collectors.toList());
    }

    public RoomTemplateResponse getTemplateById(Long id) {
        RoomTemplate template = findTemplateById(id);
        return mapTemplateToResponse(template);
    }

    @Transactional
    public RoomTemplateResponse createTemplate(RoomTemplateRequest request) {
        if (roomTemplateRepository.existsByTemplateName(request.getTemplateName())) {
            throw new IllegalArgumentException("Template with name '" + request.getTemplateName() + "' already exists");
        }

        RoomTemplate template = new RoomTemplate(
                request.getTemplateName(),
                request.getCategory(),
                request.getCapacity(),
                request.getMaxBookingHours()
        );

        template.setDescription(request.getDescription());
        template.setMaxBookingsPerDay(request.getMaxBookingsPerDay());
        template.setAdvanceBookingDays(request.getAdvanceBookingDays());
        template.setRequiresApproval(request.isRequiresApproval());

        // Add default equipment if specified
        if (request.getDefaultEquipmentIds() != null && !request.getDefaultEquipmentIds().isEmpty()) {
            Set<Equipment> equipment = new HashSet<>(equipmentRepository.findAllById(request.getDefaultEquipmentIds()));
            template.setDefaultEquipment(equipment);
        }

        template = roomTemplateRepository.save(template);
        return mapTemplateToResponse(template);
    }

    @Transactional
    public RoomResponse createRoomFromTemplate(Long templateId, String roomNumber, String name) {
        RoomTemplate template = findTemplateById(templateId);

        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new IllegalArgumentException("Room with number '" + roomNumber + "' already exists");
        }

        Room room = new Room(roomNumber, name, template.getCategory(), template.getCapacity(), template.getMaxBookingHours());
        room.setMaxBookingsPerDay(template.getMaxBookingsPerDay());
        room.setAdvanceBookingDays(template.getAdvanceBookingDays());
        room.setRequiresApproval(template.isRequiresApproval());
        room.setEquipment(new HashSet<>(template.getDefaultEquipment()));

        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Transactional
    public MessageResponse deleteTemplate(Long id) {
        RoomTemplate template = findTemplateById(id);
        roomTemplateRepository.delete(template);
        return new MessageResponse("Template deleted successfully");
    }

    // === Statistics and Reporting ===

    public List<RoomResponse> getRoomsUnderMaintenance() {
        return roomRepository.findRoomsUnderMaintenance(LocalDateTime.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> getRecentlyUpdatedRooms(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return roomRepository.findRecentlyUpdatedRooms(since).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // === Helper Methods ===

    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    private RoomTemplate findTemplateById(Long id) {
        return roomTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
    }

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setName(room.getName());
        response.setDescription(room.getDescription());
        response.setCategory(room.getCategory());
        response.setCapacity(room.getCapacity());
        response.setMaxBookingHours(room.getMaxBookingHours());
        response.setMaxBookingsPerDay(room.getMaxBookingsPerDay());
        response.setAdvanceBookingDays(room.getAdvanceBookingDays());
        response.setAvailable(room.isAvailable());
        response.setRequiresBooking(room.requiresBooking());
        response.setBuilding(room.getBuilding());
        response.setFloor(room.getFloor());
        response.setDepartment(room.getDepartment());
        response.setMaintenanceStart(room.getMaintenanceStart());
        response.setMaintenanceEnd(room.getMaintenanceEnd());
        response.setMaintenanceNotes(room.getMaintenanceNotes());
        response.setUnderMaintenance(room.isUnderMaintenance());
        response.setRequiresApproval(room.isRequiresApproval());
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());

        // Map equipment
        Set<EquipmentResponse> equipmentResponses = room.getEquipment().stream()
                .map(equipment -> {
                    EquipmentResponse equipResponse = new EquipmentResponse();
                    equipResponse.setId(equipment.getId());
                    equipResponse.setName(equipment.getName());
                    equipResponse.setDescription(equipment.getDescription());
                    equipResponse.setAvailable(equipment.isAvailable());
                    return equipResponse;
                })
                .collect(Collectors.toSet());
        
        response.setEquipment(equipmentResponses);
        return response;
    }

    private RoomTemplateResponse mapTemplateToResponse(RoomTemplate template) {
        RoomTemplateResponse response = new RoomTemplateResponse();
        response.setId(template.getId());
        response.setTemplateName(template.getTemplateName());
        response.setDescription(template.getDescription());
        response.setCategory(template.getCategory());
        response.setCapacity(template.getCapacity());
        response.setMaxBookingHours(template.getMaxBookingHours());
        response.setMaxBookingsPerDay(template.getMaxBookingsPerDay());
        response.setAdvanceBookingDays(template.getAdvanceBookingDays());
        response.setRequiresApproval(template.isRequiresApproval());
        response.setCreatedAt(template.getCreatedAt());

        // Map default equipment
        Set<EquipmentResponse> equipmentResponses = template.getDefaultEquipment().stream()
                .map(equipment -> {
                    EquipmentResponse equipResponse = new EquipmentResponse();
                    equipResponse.setId(equipment.getId());
                    equipResponse.setName(equipment.getName());
                    equipResponse.setDescription(equipment.getDescription());
                    equipResponse.setAvailable(equipment.isAvailable());
                    return equipResponse;
                })
                .collect(Collectors.toSet());
        
        response.setDefaultEquipment(equipmentResponses);
        return response;
    }
}