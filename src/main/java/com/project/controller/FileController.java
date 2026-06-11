package com.project.controller;

import com.project.dto.DownloadResponse;
import com.project.dto.FileResponse;
import com.project.model.User;
import com.project.repository.FileRepository;
import com.project.repository.UserRepository;
import com.project.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    public FileController(FileService fileService, UserRepository userRepository, FileRepository fileRepository) {
        this.fileService = fileService;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(fileService.uploadFile(file, user, folderId));
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles(
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(fileService.getUserFiles(user, folderId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<DownloadResponse> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(fileService.getDownloadUrl(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        fileService.deleteFile(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/storage-info")
    public ResponseEntity<Map<String, Long>> getStorageInfo(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Long used = fileRepository.getTotalSizeByUser(user);
        return ResponseEntity.ok(Map.of("used", used, "total", 1073741824L));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
