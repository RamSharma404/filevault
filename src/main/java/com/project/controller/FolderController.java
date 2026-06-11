package com.project.controller;

import com.project.dto.FolderResponse;
import com.project.model.User;
import com.project.repository.UserRepository;
import com.project.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserRepository userRepository;

    public FolderController(FolderService folderService, UserRepository userRepository) {
        this.folderService = folderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return ResponseEntity.ok(folderService.createFolder(name, parentId, user));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getFolders(
            @RequestParam(value = "parentId", required = false) Long parentId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(folderService.getFolders(parentId, user));
    }

    @GetMapping("/{id}/breadcrumbs")
    public ResponseEntity<List<FolderResponse>> getBreadcrumbs(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(folderService.getBreadcrumbs(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        folderService.deleteFolder(id, user);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
