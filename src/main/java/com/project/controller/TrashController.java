package com.project.controller;

import com.project.dto.FileResponse;
import com.project.dto.FolderResponse;
import com.project.model.User;
import com.project.repository.UserRepository;
import com.project.service.TrashService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trash")
public class TrashController {

    private final TrashService trashService;
    private final UserRepository userRepository;

    public TrashController(TrashService trashService, UserRepository userRepository) {
        this.trashService = trashService;
        this.userRepository = userRepository;
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileResponse>> getTrashedFiles(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(trashService.getTrashedFiles(user));
    }

    @GetMapping("/folders")
    public ResponseEntity<List<FolderResponse>> getTrashedFolders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(trashService.getTrashedFolders(user));
    }

    @PostMapping("/files/{id}/restore")
    public ResponseEntity<Void> restoreFile(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        trashService.restoreFile(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/folders/{id}/restore")
    public ResponseEntity<Void> restoreFolder(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        trashService.restoreFolder(id, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> permanentlyDeleteFile(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        trashService.permanentlyDeleteFile(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> permanentlyDeleteFolder(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        trashService.permanentlyDeleteFolder(id, user);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
