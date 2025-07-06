package com.auca.library.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.EquipmentRequest;
import com.auca.library.dto.response.EquipmentResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Equipment;
import com.auca.library.repository.EquipmentRepository;

@Service
public class EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    public List<EquipmentResponse> getAllEquipment() {
        return equipmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponse> getAvailableEquipment() {
        return equipmentRepository.findByAvailableTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // NEW: Get equipment allowed for students
    public List<EquipmentResponse> getStudentAllowedEquipment() {
        return equipmentRepository.findAll().stream()
                .filter(eq -> eq.isAllowedToStudents() && eq.isAvailable())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EquipmentResponse getEquipmentById(Long id) {
        Equipment equipment = findEquipmentById(id);
        return mapToResponse(equipment);
    }

    @Transactional
    public EquipmentResponse createEquipment(EquipmentRequest request) {
        if (equipmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Equipment with name '" + request.getName() + "' already exists");
        }

        Equipment equipment = new Equipment(request.getName(), request.getDescription());
        equipment.setAvailable(request.isAvailable());
        equipment.setAllowedToStudents(request.isAllowedToStudents());
        
        // Set quantity fields
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            equipment.setQuantity(request.getQuantity());
            equipment.setAvailableQuantity(request.getQuantity());
        }
        
        equipment = equipmentRepository.save(equipment);
        return mapToResponse(equipment);
    }

    @Transactional
    public EquipmentResponse updateEquipment(Long id, EquipmentRequest request) {
        Equipment equipment = findEquipmentById(id);

        // Check if name is being changed and conflicts
        if (!equipment.getName().equals(request.getName()) && 
            equipmentRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Equipment with name '" + request.getName() + "' already exists");
        }

        equipment.setName(request.getName());
        equipment.setDescription(request.getDescription());
        equipment.setAvailable(request.isAvailable());
        equipment.setAllowedToStudents(request.isAllowedToStudents());
        
        // Update quantity - be careful not to go below current reservations
        if (request.getQuantity() != null && request.getQuantity() > 0) {
            int currentReserved = equipment.getQuantity() - equipment.getAvailableQuantity();
            if (request.getQuantity() >= currentReserved) {
                equipment.setQuantity(request.getQuantity());
                equipment.setAvailableQuantity(request.getQuantity() - currentReserved);
            } else {
                throw new IllegalArgumentException("Cannot reduce quantity below currently reserved amount");
            }
        }

        equipment = equipmentRepository.save(equipment);
        return mapToResponse(equipment);
    }

    @Transactional
    public MessageResponse deleteEquipment(Long id) {
        Equipment equipment = findEquipmentById(id);
        equipmentRepository.delete(equipment);
        return new MessageResponse("Equipment deleted successfully");
    }

    @Transactional
    public EquipmentResponse toggleEquipmentAvailability(Long id) {
        Equipment equipment = findEquipmentById(id);
        equipment.setAvailable(!equipment.isAvailable());
        equipment = equipmentRepository.save(equipment);
        return mapToResponse(equipment);
    }

    // NEW: Reserve equipment quantity
    @Transactional
    public boolean reserveEquipment(Long equipmentId, int quantity) {
        Equipment equipment = findEquipmentById(equipmentId);
        if (equipment.isAvailableInQuantity(quantity)) {
            equipment.setAvailableQuantity(equipment.getAvailableQuantity() - quantity);
            equipmentRepository.save(equipment);
            return true;
        }
        return false;
    }

    // NEW: Release equipment quantity
    @Transactional
    public void releaseEquipment(Long equipmentId, int quantity) {
        Equipment equipment = findEquipmentById(equipmentId);
        int newAvailable = Math.min(equipment.getAvailableQuantity() + quantity, equipment.getQuantity());
        equipment.setAvailableQuantity(newAvailable);
        equipmentRepository.save(equipment);
    }

    public List<EquipmentResponse> searchEquipment(String keyword) {
        return equipmentRepository.searchEquipment(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Equipment findEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id: " + id));
    }

    private EquipmentResponse mapToResponse(Equipment equipment) {
        EquipmentResponse response = new EquipmentResponse();
        response.setId(equipment.getId());
        response.setName(equipment.getName());
        response.setDescription(equipment.getDescription());
        response.setAvailable(equipment.isAvailable());
        response.setAllowedToStudents(equipment.isAllowedToStudents());
        response.setQuantity(equipment.getQuantity());
        response.setAvailableQuantity(equipment.getAvailableQuantity());
        return response;
    }
}