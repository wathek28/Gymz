package com.example.demo.controller;

import com.example.demo.model.Photo;
import com.example.demo.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*")
public class PhotoController {
    @Autowired
    private PhotoService photoService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        try {
            Photo saved = photoService.savePhoto(file, userId);
            return ResponseEntity.ok(saved);
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'upload de la photo: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur inattendue: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PhotoService.PhotoDTO>> getUserPhotos(@PathVariable Long userId) {
        try {
            List<PhotoService.PhotoDTO> photoDTOs = photoService.getUserPhotoInfo(userId);

            if (photoDTOs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ArrayList<>());
            }

            // Modification pour que chaque DTO contienne uniquement les informations essentielles
            // et non les chemins absolus du système de fichiers local
            return ResponseEntity.ok(photoDTOs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }



    @GetMapping("/metadata/{photoId}")
    public ResponseEntity<?> getPhotoMetadata(@PathVariable Long photoId) {
        try {
            PhotoService.PhotoDTO photoDTO = photoService.getPhotoInfoById(photoId);
            return ResponseEntity.ok(photoDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des métadonnées: " + e.getMessage());
        }
    }

    @GetMapping("/thumbnails/user/{userId}")
    public ResponseEntity<?> getUserPhotoThumbnails(@PathVariable Long userId) {
        try {
            List<PhotoService.PhotoDTO> photoDTOs = photoService.getUserPhotoInfo(userId);

            if (photoDTOs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ArrayList<>());
            }

            return ResponseEntity.ok(photoDTOs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des miniatures: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPhotos(@RequestParam("query") String query) {
        try {
            List<PhotoService.PhotoDTO> results = photoService.searchPhotoInfoByFileName(query);

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ArrayList<>());
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la recherche: " + e.getMessage());
        }
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long photoId) {
        try {
            photoService.deletePhoto(photoId);
            return ResponseEntity.ok().body("Photo supprimée avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @PutMapping("/{photoId}")
    public ResponseEntity<?> updatePhotoMetadata(
            @PathVariable Long photoId,
            @RequestBody Map<String, String> metadata) {
        try {
            Photo updated = photoService.updatePhotoMetadata(photoId, metadata);
            PhotoService.PhotoDTO dto = photoService.getPhotoInfoById(updated.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour des métadonnées: " + e.getMessage());
        }
    }

    @GetMapping("/count/user/{userId}")
    public ResponseEntity<?> getPhotoCount(@PathVariable Long userId) {
        try {
            List<Photo> photos = photoService.getUserPhotos(userId);
            Map<String, Long> response = Map.of("count", (long) photos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du comptage des photos: " + e.getMessage());
        }
    }
    @GetMapping(value = "/{photoId}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<?> getPhoto(@PathVariable Long photoId) {
        try {
            Photo photo = photoService.getPhotoById(photoId);
            if (photo == null) {
                return ResponseEntity.notFound().build();
            }

            Path imagePath = Paths.get(photo.getThumbnailPath());
            if (!Files.exists(imagePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Image non trouvée sur le serveur");
            }

            // Déterminer le type de contenu basé sur l'extension du fichier
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Lire le contenu du fichier
            byte[] imageData = Files.readAllBytes(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFileName() + "\"")
                    .body(imageData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération de l'image: " + e.getMessage());
        }
    }
}