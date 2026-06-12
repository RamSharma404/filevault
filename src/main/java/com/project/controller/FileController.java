package com.project.controller;

import com.project.annotation.RateLimit;
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

    @RateLimit(requests = 10, windowSeconds = 60)
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
            @RequestParam(value = "search", required = false) String search,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(fileService.searchFiles(user, search));
        }
        return ResponseEntity.ok(fileService.getUserFiles(user, folderId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<DownloadResponse> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(fileService.getDownloadUrl(id, user));
    }

    @GetMapping("/{id}/share")
    public ResponseEntity<DownloadResponse> shareFile(
            @PathVariable Long id,
            @RequestParam(value = "download", defaultValue = "false") boolean isDownload,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        String url = fileService.getShareUrl(id, user, isDownload);
        return ResponseEntity.ok(new DownloadResponse(url, 3600L));
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
