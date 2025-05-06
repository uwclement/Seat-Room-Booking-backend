package com.auca.library.controller;

import com.auca.library.dto.request.WaitListRequest;
import com.auca.library.dto.response.MessageResponse;
import com.auca.library.dto.response.WaitListDTO;
import com.auca.library.service.WaitListService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/waitlist")
public class WaitListController {

    @Autowired
    private WaitListService waitListService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WaitListDTO> joinWaitList(@Valid @RequestBody WaitListRequest request) {
        WaitListDTO waitList = waitListService.joinWaitList(request);
        return ResponseEntity.ok(waitList);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> cancelWaitList(@PathVariable Long id) {
        WaitListDTO waitList = waitListService.cancelWaitList(id);
        return ResponseEntity.ok(new MessageResponse("Wait list entry cancelled successfully"));
    }
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WaitListDTO>> getUserWaitList() {
        List<WaitListDTO> waitList = waitListService.getUserWaitList();
        return ResponseEntity.ok(waitList);
    }
}