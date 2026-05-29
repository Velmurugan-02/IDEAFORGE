package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.CollabRequestRequest;
import com.ideaforge.ideaforge_backend.dto.CollabRequestResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.CollabRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollabRequestController {

    private final CollabRequestService collabRequestService;

    @PostMapping("/ideas/{id}/collab-request")
    public ResponseEntity<CollabRequestResponse> createCollabRequest(
            @PathVariable Long id,
            @RequestBody CollabRequestRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collabRequestService.createCollabRequest(id, request, currentUser));
    }

    @GetMapping("/collab-requests/received")
    public ResponseEntity<List<CollabRequestResponse>> getReceivedRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(collabRequestService.getReceivedRequests(currentUser));
    }

    @GetMapping("/collab-requests/sent")
    public ResponseEntity<List<CollabRequestResponse>> getSentRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(collabRequestService.getSentRequests(currentUser));
    }

    @PutMapping("/collab-requests/{id}/accept")
    public ResponseEntity<CollabRequestResponse> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(collabRequestService.acceptRequest(id, currentUser));
    }

    @PutMapping("/collab-requests/{id}/reject")
    public ResponseEntity<CollabRequestResponse> rejectRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(collabRequestService.rejectRequest(id, currentUser));
    }
}
