package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.PlagiarismReportRequest;
import com.ideaforge.ideaforge_backend.dto.PlagiarismReportResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.PlagiarismReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ideas/{id}")
@RequiredArgsConstructor
public class PlagiarismReportController {

    private final PlagiarismReportService reportService;

    @PostMapping("/report")
    public ResponseEntity<PlagiarismReportResponse> submitReport(
            @PathVariable Long id,
            @Valid @RequestBody PlagiarismReportRequest request,
            @AuthenticationPrincipal User currentUser) {
        PlagiarismReportResponse response = reportService.submitReport(id, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public ResponseEntity<List<PlagiarismReportResponse>> getReportsForIdea(
            @PathVariable Long id) {
        List<PlagiarismReportResponse> responses = reportService.getReportsForIdea(id);
        return ResponseEntity.ok(responses);
    }
}
