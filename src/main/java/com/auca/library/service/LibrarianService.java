package com.auca.library.service;

import com.auca.library.dto.request.LibrarianRequest;
import com.auca.library.dto.response.LibrarianResponse;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.exception.ResourceNotFoundException;
import com.auca.library.model.Librarian;
import com.auca.library.repository.LibrarianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibrarianService {

    @Autowired
    private LibrarianRepository librarianRepository;

    public List<LibrarianResponse> getAllLibrarians() {
        return librarianRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LibrarianResponse> getActiveLibrariansForDay(LocalDate day) {
        return librarianRepository.findByWorkingDayAndActiveToday(day, true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LibrarianResponse getLibrarianById(Long id) {
        Librarian librarian = findLibrarianById(id);
        return mapToResponse(librarian);
    }

    @Transactional
    public LibrarianResponse createLibrarian(LibrarianRequest request) {
        if (request.isActiveToday()) {
            long activeCount = librarianRepository.countActiveLibrarians(request.getWorkingDay());
            if (activeCount >= 2) {
                throw new IllegalStateException("Only 2 librarians can be active per day.");
            }
        }

        if (request.isDefault()) {
            librarianRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setIsDefault(false);
                librarianRepository.save(existing);
            });
        }

        Librarian librarian = new Librarian();
        librarian.setName(request.getName());
        librarian.setPhone(request.getPhone());
        librarian.setActiveToday(request.isActiveToday());
        librarian.setIsDefault(request.isDefault());
        librarian.setWorkingDay(request.getWorkingDay());

        return mapToResponse(librarianRepository.save(librarian));
    }

    @Transactional
    public LibrarianResponse updateLibrarian(Long id, LibrarianRequest request) {
        Librarian librarian = findLibrarianById(id);

        if (request.isActiveToday() && !librarian.isActiveToday()) {
            long activeCount = librarianRepository.countActiveLibrarians(request.getWorkingDay());
            if (activeCount >= 2) {
                throw new IllegalStateException("Only 2 librarians can be active per day.");
            }
        }

        if (request.isDefault() && !librarian.isDefault()) {
            librarianRepository.findByIsDefaultTrue().ifPresent(existing -> {
                existing.setIsDefault(false);
                librarianRepository.save(existing);
            });
        }

        librarian.setName(request.getName());
        librarian.setPhone(request.getPhone());
        librarian.setActiveToday(request.isActiveToday());
        librarian.setIsDefault(request.isDefault());
        librarian.setWorkingDay(request.getWorkingDay());

        return mapToResponse(librarianRepository.save(librarian));
    }

    @Transactional
    public MessageResponse deleteLibrarian(Long id) {
        Librarian librarian = findLibrarianById(id);
        librarianRepository.delete(librarian);
        return new MessageResponse("Librarian deleted successfully.");
    }

    public LibrarianResponse getActiveOrDefaultLibrarian(LocalDate day) {
        List<Librarian> active = librarianRepository.findByWorkingDayAndActiveToday(day, true);
        if (!active.isEmpty()) {
            return mapToResponse(active.get(0));
        }

        return librarianRepository.findByIsDefaultTrue()
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No default librarian found."));
    }

    private Librarian findLibrarianById(Long id) {
        return librarianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Librarian not found with id: " + id));
    }

    private LibrarianResponse mapToResponse(Librarian librarian) {
        LibrarianResponse response = new LibrarianResponse();
        response.setId(librarian.getId());
        response.setName(librarian.getName());
        response.setPhone(librarian.getPhone());
        response.setActiveToday(librarian.isActiveToday());
        response.setIsDefault(librarian.isDefault());
        response.setWorkingDay(librarian.getWorkingDay());
        return response;
    }
}
