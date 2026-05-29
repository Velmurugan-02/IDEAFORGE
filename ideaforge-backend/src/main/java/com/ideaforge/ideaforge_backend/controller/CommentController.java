package com.ideaforge.ideaforge_backend.controller;

import com.ideaforge.ideaforge_backend.dto.CommentRequest;
import com.ideaforge.ideaforge_backend.dto.CommentResponse;
import com.ideaforge.ideaforge_backend.model.User;
import com.ideaforge.ideaforge_backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/ideas/{ideaId}/comments")
    public ResponseEntity<CommentResponse> postComment(
            @PathVariable Long ideaId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User currentUser) {
        CommentResponse response = commentService.postComment(ideaId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/ideas/{ideaId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long ideaId,
            @RequestParam(required = false) String tag) {
        List<CommentResponse> responses = commentService.getComments(ideaId, tag);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/comments/{commentId}/answer")
    public ResponseEntity<CommentResponse> answerComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {
        CommentResponse response = commentService.answerComment(commentId, currentUser);
        return ResponseEntity.ok(response);
    }
}
