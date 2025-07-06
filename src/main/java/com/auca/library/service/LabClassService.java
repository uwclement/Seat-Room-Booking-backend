package com.auca.library.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auca.library.dto.request.LabClassRequest;
import com.auca.library.dto.response.LabClassResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Equipment;
import com.auca.library.model.LabClass;
import com.auca.library.repository.EquipmentRepository;
import com.auca.library.repository.LabClassRepository;

@Service
public class LabClassService {

    @Autowired
    private LabClassRepository labClassRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;

    public List<LabClassResponse> getAllLabClasses() {
        return labClassRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LabClassResponse> getAvailableLabClasses() {
        return labClassRepository.findByAvailableTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LabClassResponse getLabClassById(Long id) {
        LabClass labClass = findLabClassById(id);
        return mapToResponse(labClass);
    }

    @Transactional
    public LabClassResponse createLabClass(LabClassRequest request) {
        if (labClassRepository.existsByLabNumber(request.getLabNumber())) {
            throw new IllegalArgumentException("Lab class with number '" + request.getLabNumber() + "' already exists");
        }

        LabClass labClass = new LabClass(
            request.getLabNumber(), 
            request.getName(), 
            request.getCapacity(),
            request.getBuilding(), 
            request.getFloor()
        );
        labClass.setDescription(request.getDescription());

        // Add equipment if specified
        if (request.getEquipmentIds() != null && !request.getEquipmentIds().isEmpty()) {
            Set<Equipment> equipment = request.getEquipmentIds().stream()
                    .map(equipmentRepository::findById)
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .collect(Collectors.toSet());
            labClass.setEquipment(equipment);
        }

        labClass = labClassRepository.save(labClass);
        return mapToResponse(labClass);
    }

    @Transactional
    public LabClassResponse updateLabClass(Long id, LabClassRequest request) {
        LabClass labClass = findLabClassById(id);

        // Check if lab number is being changed and conflicts
        if (!labClass.getLabNumber().equals(request.getLabNumber()) && 
            labClassRepository.existsByLabNumber(request.getLabNumber())) {
            throw new IllegalArgumentException("Lab class with number '" + request.getLabNumber() + "' already exists");
        }

        labClass.setLabNumber(request.getLabNumber());
        labClass.setName(request.getName());
        labClass.setDescription(request.getDescription());
        labClass.setCapacity(request.getCapacity());
        labClass.setBuilding(request.getBuilding());
        labClass.setFloor(request.getFloor());

        // Update equipment
        if (request.getEquipmentIds() != null) {
            Set<Equipment> equipment = request.getEquipmentIds().stream()
                    .map(equipmentRepository::findById)
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .collect(Collectors.toSet());
            labClass.setEquipment(equipment);
        }

        labClass = labClassRepository.save(labClass);
        return mapToResponse(labClass);
    }

    @Transactional
    public MessageResponse deleteLabClass(Long id) {
        LabClass labClass = findLabClassById(id);
        labClassRepository.delete(labClass);
        return new MessageResponse("Lab class deleted successfully");
    }

    @Transactional
    public LabClassResponse toggleLabAvailability(Long id) {
        LabClass labClass = findLabClassById(id);
        labClass.setAvailable(!labClass.isAvailable());
        labClass = labClassRepository.save(labClass);
        return mapToResponse(labClass);
    }

    public List<LabClassResponse> searchLabClasses(String keyword) {
        return labClassRepository.searchLabClasses(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean isLabAvailable(Long labId, LocalDateTime startTime, LocalDateTime endTime) {
        return labClassRepository.isLabAvailable(labId, startTime, endTime);
    }

    private LabClass findLabClassById(Long id) {
        return labClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab class not found with id: " + id));
    }

    private LabClassResponse mapToResponse(LabClass labClass) {
        LabClassResponse response = new LabClassResponse();
        response.setId(labClass.getId());
        response.setLabNumber(labClass.getLabNumber());
        response.setName(labClass.getName());
        response.setDescription(labClass.getDescription());
        response.setCapacity(labClass.getCapacity());
        response.setBuilding(labClass.getBuilding());
        response.setFloor(labClass.getFloor());
        response.setAvailable(labClass.isAvailable());
        
        // Map equipment
        Set<Equipment> equipment = labClass.getEquipment();
        response.setEquipmentIds(labClass.getEquipment().stream()
                .map(Equipment::getId)
                .collect(Collectors.toList()));
        response.setEquipmentNames(labClass.getEquipment().stream()
                .map(Equipment::getName)
                .collect(Collectors.toList()));
        response.setEquipmentCount(equipment.size());        
        
        return response;
    }
}