package com.example.demo.service;

import com.example.demo.model.Photo;
import com.example.demo.model.User;
import com.example.demo.repository.PhotoRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PhotoService {
    // Internal DTO class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoDTO {
        private Long id;
        private String fileName;
        private LocalDateTime uploadDate;
        private Long userId;
        private String photoUrl;

        // Constructor used for conversion
        public PhotoDTO(Long id, String fileName, LocalDateTime uploadDate, Long userId) {
            this.id = id;
            this.fileName = fileName;
            this.uploadDate = uploadDate;
            this.userId = userId;
            this.photoUrl = "/api/photos/" + id;
        }
    }

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.upload.dir:${user.home}/uploads/thumbnails}")
    private String uploadDir;

    public Photo savePhoto(MultipartFile file, Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Save the file to the filesystem
        Files.copy(file.getInputStream(), filePath);

        // Save the path in the database
        Photo photo = new Photo();
        photo.setFileName(file.getOriginalFilename());
        photo.setThumbnailPath(filePath.toString());
        photo.setUploadDate(LocalDateTime.now());
        photo.setUser(user);

        return photoRepository.save(photo);
    }

    public List<Photo> getUserPhotos(Long userId) {
        List<Photo> photos = photoRepository.findByUserId(userId);
        System.out.println("Nombre de photos récupérées pour l'utilisateur " + userId + ": " + photos.size());
        return photos;
    }

    public List<PhotoDTO> getUserPhotoInfo(Long userId) {
        // Manual conversion since we need to use the internal DTO class
        List<Photo> photos = photoRepository.findByUserId(userId);
        return convertToPhotoDTO(photos);
    }

    // Manually convert Photo entities to DTOs
    public List<PhotoDTO> convertToPhotoDTO(List<Photo> photos) {
        return photos.stream()
                .map(photo -> new PhotoDTO(
                        photo.getId(),
                        photo.getFileName(),
                        photo.getUploadDate(),
                        photo.getUser().getId(),
                        "/api/photos/" + photo.getId()
                ))
                .collect(Collectors.toList());
    }

    public Photo getPhotoById(Long photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));
    }

    public PhotoDTO getPhotoInfoById(Long photoId) {
        Photo photo = getPhotoById(photoId);
        return new PhotoDTO(
                photo.getId(),
                photo.getFileName(),
                photo.getUploadDate(),
                photo.getUser().getId(),
                "/api/photos/" + photo.getId()
        );
    }

    public List<Photo> searchPhotosByFileName(String fileName) {
        return photoRepository.findByFileNameContainingIgnoreCase(fileName);
    }

    public List<PhotoDTO> searchPhotoInfoByFileName(String fileName) {
        List<Photo> photos = photoRepository.findByFileNameContainingIgnoreCase(fileName);
        return convertToPhotoDTO(photos);
    }

    public Photo updatePhotoMetadata(Long photoId, Map<String, String> metadata) {
        Photo photo = getPhotoById(photoId);

        // Update fields based on the provided metadata
        if (metadata.containsKey("fileName")) {
            photo.setFileName(metadata.get("fileName"));
        }

        // Add more fields as needed

        return photoRepository.save(photo);
    }

    public void deletePhoto(Long photoId) {
        // Get the photo to delete the file from the filesystem
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo non trouvée"));

        try {
            // Delete the file from the filesystem
            Path filePath = Paths.get(photo.getThumbnailPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log the error but continue to delete from database
            System.err.println("Impossible de supprimer le fichier: " + e.getMessage());
        }

        // Delete the photo entry from the database
        photoRepository.deleteById(photoId);
    }
}