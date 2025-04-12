package com.example.demo.controller;

import com.example.demo.model.Reel;
import com.example.demo.service.ReelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reels")
public class ReelController {
    @Autowired
    private ReelService reelService;

    @PostMapping
    public ResponseEntity<Reel> createReel(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("video") MultipartFile video) throws IOException {
        return ResponseEntity.ok(reelService.createReel(userId, title, description, video));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reel>> getUserReels(@PathVariable Long userId) {
        List<Reel> reels = reelService.getUserReels(userId);
        System.out.println("Number of reels returned: " + reels.size());
        return ResponseEntity.ok(reels);
    }

    /**
     * Récupère tous les reels disponibles dans le système avec les informations du créateur
     * @return Liste de tous les reels avec information du créateur (photo et prénom)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllReels() {
        List<Map<String, Object>> reels = reelService.getAllReelsWithCreatorInfo();
        System.out.println("Number of all reels returned: " + reels.size());
        return ResponseEntity.ok(reels);
    }

    /**
     * Permet de streamer une vidéo à partir de son ID
     * @param reelId ID du reel contenant la vidéo
     * @return Le fichier vidéo pour streaming
     */
    @GetMapping("/video/{reelId}")
    public ResponseEntity<Resource> getVideo(@PathVariable Long reelId) {
        try {
            Reel reel = reelService.getAllReels().stream()
                    .filter(r -> r.getId().equals(reelId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Reel not found"));

            Path filePath = Paths.get(reel.getVideoPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + reel.getOriginalFilename() + "\"");

                // Determine content type
                String contentType = determineContentType(reel.getOriginalFilename());

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    /**
     * Détermine le type MIME d'un fichier basé sur son extension
     */
    private String determineContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        filename = filename.toLowerCase();

        if (filename.endsWith(".mp4")) {
            return "video/mp4";
        } else if (filename.endsWith(".mov")) {
            return "video/quicktime";
        } else if (filename.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (filename.endsWith(".wmv")) {
            return "video/x-ms-wmv";
        } else if (filename.endsWith(".webm")) {
            return "video/webm";
        } else {
            return "application/octet-stream";
        }
    }
}