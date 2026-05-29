package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.AdminPlatformStatsResponse;
import com.ideaforge.ideaforge_backend.dto.AdminReportResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/setup")
    public ResponseEntity<Void> setupAdmin(@AuthenticationPrincipal User currentUser) {
        adminService.setupFirstAdmin(currentUser);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public ResponseEntity<List<AdminReportResponse>> getPendingReports() {
        return ResponseEntity.ok(adminService.getPendingReports());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<AdminPlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reports/{id}/confirm")
    public ResponseEntity<Void> confirmReport(@PathVariable Long id) {
        adminService.confirmReport(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reports/{id}/dismiss")
    public ResponseEntity<Void> dismissReport(@PathVariable Long id) {
        adminService.dismissReport(id);
        return ResponseEntity.ok().build();
    }
}
