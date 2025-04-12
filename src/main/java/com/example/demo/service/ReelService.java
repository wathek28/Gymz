package com.example.demo.service;

import com.example.demo.model.Reel;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.ReelRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReelService {
    @Autowired
    private ReelRepository reelRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir:uploads/videos}")
    private String uploadDir;

    public Reel createReel(Long userId, String title, String description, MultipartFile videoFile) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.COACH && user.getRole() != Role.GYM) {
            throw new RuntimeException("Only coaches and gyms can create reels");
        }

        // Create directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate unique filename
        String originalFilename = videoFile.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        String filePath = uploadDir + File.separator + uniqueFilename;

        // Save file to disk
        Path targetLocation = Paths.get(filePath);
        Files.copy(videoFile.getInputStream(), targetLocation);

        Reel reel = new Reel();
        reel.setTitle(title);
        reel.setDescription(description);
        reel.setVideoPath(filePath);
        reel.setOriginalFilename(originalFilename);
        reel.setUser(user);

        return reelRepository.save(reel);
    }

    public List<Reel> getUserReels(Long userId) {
        List<Reel> reels = reelRepository.findByUserId(userId);
        System.out.println("Number of reels found: " + reels.size());
        return reels;
    }

    public List<Reel> getAllReels() {
        return reelRepository.findAll();
    }

    /**
     * Récupère tous les reels avec les informations du créateur
     * Structure adaptée pour correspondre à ce qu'attend le front-end React Native
     * @return Liste de tous les reels avec les informations du créateur
     */
    public List<Map<String, Object>> getAllReelsWithCreatorInfo() {
        List<Reel> reels = reelRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Reel reel : reels) {
            Map<String, Object> reelMap = new HashMap<>();
            // Informations du reel
            reelMap.put("id", reel.getId());
            reelMap.put("title", reel.getTitle());
            reelMap.put("description", reel.getDescription());
            reelMap.put("videoPath", reel.getVideoPath());
            reelMap.put("originalFilename", reel.getOriginalFilename());
            reelMap.put("createdAt", reel.getCreatedAt());

            // Informations du créateur dans un objet "author" pour correspondre au front-end
            User user = reel.getUser();
            if (user != null) {
                Map<String, Object> authorMap = new HashMap<>();
                authorMap.put("id", user.getId());
                authorMap.put("firstName", user.getFirstName());
                authorMap.put("photo", user.getPhoto());

                // Ajouter l'objet author au reel
                reelMap.put("author", authorMap);
            }

            result.add(reelMap);
        }

        return result;
    }
}