package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    // Repositories
    private final CourseRepository courseRepository;
    private final CourseExerciseRepository exerciseRepository;
    private final ExerciseVideoRepository videoRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    // Upload paths
    @Value("${upload.path.thumbnail}")
    private String thumbnailUploadPath;

    @Value("${upload.path.video}")
    private String videoUploadPath;

    /**
     * Générer un nom de fichier unique
     */
    private String generateUniqueFileName(String originalFilename) {
        String fileExtension = Optional.ofNullable(originalFilename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(originalFilename.lastIndexOf(".")))
                .orElse(".mp4");
        return UUID.randomUUID() + fileExtension;
    }

    /**
     * Sauvegarder un fichier de miniature
     * Modifié pour enregistrer le chemin du fichier au lieu du BLOB
     */
    private String saveThumbnailFile(MultipartFile thumbnailFile) throws IOException {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            return null;
        }

        // Créer le répertoire s'il n'existe pas
        Path uploadDir = Paths.get(thumbnailUploadPath);
        Files.createDirectories(uploadDir);

        // Générer un nom de fichier unique
        String uniqueFileName = generateUniqueFileName(thumbnailFile.getOriginalFilename());

        // Chemin complet du fichier
        Path targetLocation = uploadDir.resolve(uniqueFileName);

        // Copier le fichier
        Files.copy(thumbnailFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    /**
     * Sauvegarder un fichier vidéo
     */
    private String saveVideoFile(MultipartFile videoFile) throws IOException {
        if (videoFile == null || videoFile.isEmpty()) {
            return null;
        }

        // Créer le répertoire s'il n'existe pas
        Path uploadDir = Paths.get(videoUploadPath);
        Files.createDirectories(uploadDir);

        // Générer un nom de fichier unique
        String uniqueFileName = generateUniqueFileName(videoFile.getOriginalFilename());

        // Chemin complet du fichier
        Path targetLocation = uploadDir.resolve(uniqueFileName);

        // Copier le fichier
        Files.copy(videoFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    /**
     * Créer un nouveau cours avec ses exercices
     */
    @Transactional
    public Course createCourseWithExercises(CourseRequest courseRequest, User coach) throws IOException {
        // Validation du cours
        validateCourseRequest(courseRequest);

        // Créer le cours
        Course course = new Course();
        course.setTitle(courseRequest.getTitle());
        course.setDescription(courseRequest.getDescription());
        course.setLevel(courseRequest.getLevel());
        course.setDurationMinutes(courseRequest.getDurationMinutes());
        course.setPrice(courseRequest.getPrice());
        course.setPaid(courseRequest.isPaid());
        course.setFirstVideoFree(courseRequest.isFirstVideoFree());
        course.setCoach(coach);

        // Initialiser exerciseCount à 0 par défaut
        course.setExerciseCount(0);

        // Gérer la miniature - modifié pour utiliser le chemin au lieu du BLOB
        if (courseRequest.getThumbnail() != null) {
            course.setThumbnailPath(saveThumbnailFile(courseRequest.getThumbnail()));
        }

        // Sauvegarder le cours initial
        Course savedCourse = courseRepository.save(course);

        // Créer les exercices si présents
        if (courseRequest.getExercises() != null && !courseRequest.getExercises().isEmpty()) {
            List<CourseExercise> exercises = new ArrayList<>();

            for (int i = 0; i < courseRequest.getExercises().size(); i++) {
                ExerciseRequest exerciseRequest = courseRequest.getExercises().get(i);
                CourseExercise exercise = createCourseExercise(exerciseRequest, savedCourse, coach, i);
                exercises.add(exercise);
            }

            // Mettre à jour le nombre d'exercices
            savedCourse.setExerciseCount(exercises.size());
            savedCourse.setExercises(exercises);
        }

        return courseRepository.save(savedCourse);
    }

    /**
     * Créer un exercice pour un cours
     */
    private CourseExercise createCourseExercise(
            ExerciseRequest exerciseRequest,
            Course course,
            User coach,
            int orderIndex) throws IOException {

        // Validation de l'exercice
        validateExerciseRequest(exerciseRequest);

        // Créer l'exercice
        CourseExercise exercise = new CourseExercise();
        exercise.setName(exerciseRequest.getName());
        exercise.setDescription(exerciseRequest.getDescription());
        exercise.setDurationSeconds(exerciseRequest.getDurationSeconds());
        exercise.setRepetitions(exerciseRequest.getRepetitions());
        exercise.setOrderIndex(orderIndex);
        exercise.setLocked(exerciseRequest.isLocked());
        exercise.setFreePreview(exerciseRequest.isFreePreview());
        exercise.setCourse(course);

        // Gérer la vidéo de l'exercice
        if (exerciseRequest.getVideo() != null && !exerciseRequest.getVideo().isEmpty()) {
            ExerciseVideo video = createExerciseVideo(exerciseRequest.getVideo(), coach);
            exercise.setVideo(video);
            // Ne pas définir l'exercice dans la vidéo ici pour éviter les références circulaires
        }

        return exerciseRepository.save(exercise);
    }

    /**
     * Créer une vidéo pour un exercice
     * Modifié pour éviter la référence circulaire
     */
    /**
     * Créer une vidéo pour un exercice
     * Modifié pour éviter la référence circulaire en utilisant coachId au lieu de l'objet coach
     */
    private ExerciseVideo createExerciseVideo(
            MultipartFile videoFile,
            User coach) throws IOException {

        ExerciseVideo video = new ExerciseVideo();
        video.setTitle("Vidéo d'exercice");  // Titre temporaire, sera mis à jour après
        video.setDescription("Description de vidéo");  // Description temporaire
        video.setVideoPath(saveVideoFile(videoFile));
        video.setOriginalFilename(videoFile.getOriginalFilename());
        video.setDurationSeconds(0);  // Sera mis à jour après
        video.setPreview(false);  // Sera mis à jour après
        video.setCoachId(coach.getId());  // Utiliser l'ID du coach au lieu de l'objet coach
        // Ne pas définir l'exercice ici pour éviter les références circulaires

        return videoRepository.save(video);
    }

    /**
     * Mettre à jour les informations de la vidéo après création de l'exercice
     */
    private void updateVideoInfo(ExerciseVideo video, CourseExercise exercise) {
        video.setTitle(exercise.getName());
        video.setDescription(exercise.getDescription());
        video.setDurationSeconds(exercise.getDurationSeconds());
        video.setPreview(exercise.isFreePreview());
        video.setExercise(exercise);
        videoRepository.save(video);
    }

    /**
     * Valider une requête de cours
     */
    private void validateCourseRequest(CourseRequest request) {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new IllegalArgumentException("Le titre du cours est obligatoire");
        }
        if (request.getLevel() == null) {
            throw new IllegalArgumentException("Le niveau du cours est obligatoire");
        }
        if (request.getDurationMinutes() == null) {
            throw new IllegalArgumentException("La durée du cours est obligatoire");
        }
        if (request.getPrice() == null) {
            throw new IllegalArgumentException("Le prix du cours est obligatoire");
        }
    }

    /**
     * Valider une requête d'exercice
     */
    private void validateExerciseRequest(ExerciseRequest request) {
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'exercice est obligatoire");
        }
    }

    /**
     * Mettre à jour un cours
     */
    @Transactional
    public Course updateCourse(Long courseId, CourseRequest request, User coach) throws IOException {
        // Récupérer le cours existant
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Cours non trouvé"));

        // Mettre à jour les détails du cours
        existingCourse.setTitle(request.getTitle());
        existingCourse.setDescription(request.getDescription());
        existingCourse.setLevel(request.getLevel());
        existingCourse.setDurationMinutes(request.getDurationMinutes());
        existingCourse.setPrice(request.getPrice());
        existingCourse.setPaid(request.isPaid());
        existingCourse.setFirstVideoFree(request.isFirstVideoFree());

        // Mettre à jour la miniature si une nouvelle est fournie
        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            // Supprimer l'ancienne miniature si elle existe
            if (existingCourse.getThumbnailPath() != null) {
                try {
                    Path fullPath = Paths.get(thumbnailUploadPath, existingCourse.getThumbnailPath());
                    Files.deleteIfExists(fullPath);
                } catch (IOException e) {
                    log.warn("Impossible de supprimer l'ancienne miniature: {}", e.getMessage());
                }
            }
            existingCourse.setThumbnailPath(saveThumbnailFile(request.getThumbnail()));
        }

        // Mettre à jour les exercices
        updateCourseExercises(existingCourse, request.getExercises(), coach);

        return courseRepository.save(existingCourse);
    }

    /**
     * Mettre à jour les exercices d'un cours
     */
    @Transactional
    protected void updateCourseExercises(Course course, List<ExerciseRequest> exerciseRequests, User coach) throws IOException {
        // Nettoyer les anciens exercices et leurs vidéos
        for (CourseExercise exercise : new ArrayList<>(course.getExercises())) {
            if (exercise.getVideo() != null) {
                String videoPath = exercise.getVideo().getVideoPath();
                if (videoPath != null && !videoPath.isEmpty()) {
                    deleteVideoFile(videoPath);
                }
                videoRepository.delete(exercise.getVideo());
            }
            exerciseRepository.delete(exercise);
        }

        // Réinitialiser la liste des exercices
        course.getExercises().clear();

        // Créer de nouveaux exercices si fournis
        if (exerciseRequests != null && !exerciseRequests.isEmpty()) {
            List<CourseExercise> newExercises = new ArrayList<>();

            for (int i = 0; i < exerciseRequests.size(); i++) {
                CourseExercise exercise = createCourseExercise(
                        exerciseRequests.get(i),
                        course,
                        coach,
                        i
                );

                // Mettre à jour les informations de la vidéo si nécessaire
                if (exercise.getVideo() != null) {
                    updateVideoInfo(exercise.getVideo(), exercise);
                }

                newExercises.add(exercise);
            }

            // Mettre à jour le nombre d'exercices
            course.setExerciseCount(newExercises.size());
            course.setExercises(newExercises);
        } else {
            course.setExerciseCount(0);
        }
    }

    /**
     * Supprimer un cours
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Cours non trouvé"));

        // Supprimer la miniature si elle existe
        if (course.getThumbnailPath() != null) {
            try {
                Path fullPath = Paths.get(thumbnailUploadPath, course.getThumbnailPath());
                Files.deleteIfExists(fullPath);
            } catch (IOException e) {
                log.warn("Impossible de supprimer la miniature: {}", e.getMessage());
            }
        }

        // Supprimer les fichiers des vidéos
        for (CourseExercise exercise : course.getExercises()) {
            if (exercise.getVideo() != null) {
                deleteVideoFile(exercise.getVideo().getVideoPath());
            }
        }

        courseRepository.delete(course);
    }

    /**
     * Supprimer un exercice
     */
    @Transactional
    public void deleteExercise(Long exerciseId) {
        CourseExercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("Exercice non trouvé"));

        // Supprimer le fichier vidéo s'il existe
        if (exercise.getVideo() != null) {
            deleteVideoFile(exercise.getVideo().getVideoPath());
            videoRepository.delete(exercise.getVideo());
        }

        // Obtenir le cours associé
        Course course = exercise.getCourse();
        if (course != null) {
            // Retirer l'exercice de la liste des exercices du cours
            course.getExercises().remove(exercise);
            // Mettre à jour le nombre d'exercices
            course.setExerciseCount(course.getExercises().size());
            // Réorganiser les indices des exercices restants
            reorderExercises(course);
            // Sauvegarder les modifications du cours
            courseRepository.save(course);
        }

        exerciseRepository.delete(exercise);
    }

    /**
     * Réorganiser les indices des exercices d'un cours
     */
    private void reorderExercises(Course course) {
        List<CourseExercise> exercises = new ArrayList<>(course.getExercises());
        exercises.sort(Comparator.comparing(CourseExercise::getOrderIndex));

        for (int i = 0; i < exercises.size(); i++) {
            CourseExercise exercise = exercises.get(i);
            exercise.setOrderIndex(i);
            exerciseRepository.save(exercise);
        }
    }

    /**
     * Supprimer un fichier vidéo
     */
    private void deleteVideoFile(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            return;
        }

        try {
            Path fullPath = Paths.get(videoUploadPath, videoPath);
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier vidéo : {}", videoPath, e);
        }
    }

    /**
     * Récupérer un cours par son ID
     */
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Cours non trouvé"));
    }

    /**
     * Récupérer tous les cours
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Récupérer les cours par niveau
     */
    public List<Course> getCoursesByLevel(CourseLevel level) {
        return courseRepository.findByLevel(level);
    }

    /**
     * Récupérer les cours par coach
     */
    public List<Course> getCoursesByCoach(User user) {
        return courseRepository.findByCoach(user);
    }

    /**
     * Récupérer un exercice par son ID
     */
    public Optional<CourseExercise> getExerciseById(Long exerciseId) {
        return exerciseRepository.findById(exerciseId);
    }

    /**
     * Créer un exercice pour un cours
     */
    @Transactional
    public CourseExercise createExercise(ExerciseRequest request, Course course, User coach) throws IOException {
        // Validation de l'exercice
        validateExerciseRequest(request);

        // Créer l'exercice
        CourseExercise exercise = new CourseExercise();
        exercise.setName(request.getName());
        exercise.setDescription(request.getDescription());
        exercise.setDurationSeconds(request.getDurationSeconds());
        exercise.setRepetitions(request.getRepetitions());

        // Définir l'index à la fin de la liste des exercices du cours
        exercise.setOrderIndex(course.getExercises().size());

        exercise.setLocked(request.isLocked());
        exercise.setFreePreview(request.isFreePreview());
        exercise.setCourse(course);

        // Sauvegarder l'exercice pour obtenir un ID
        CourseExercise savedExercise = exerciseRepository.save(exercise);

        // Gérer la vidéo de l'exercice
        if (request.getVideo() != null && !request.getVideo().isEmpty()) {
            ExerciseVideo video = createExerciseVideo(request.getVideo(), coach);
            // Mettre à jour les infos de la vidéo avec les données de l'exercice
            updateVideoInfo(video, savedExercise);
            savedExercise.setVideo(video);
            savedExercise = exerciseRepository.save(savedExercise);
        }

        // Ajouter l'exercice à la liste des exercices du cours
        course.getExercises().add(savedExercise);

        // Mettre à jour le nombre d'exercices du cours
        course.setExerciseCount(course.getExercises().size());

        // Sauvegarder les modifications du cours
        courseRepository.save(course);

        return savedExercise;
    }

    /**
     * Récupérer les données d'une vidéo d'exercice
     */
    public byte[] getExerciseVideoBytes(Long exerciseId) {
        log.info("Demande de vidéo pour l'exercice ID: {}", exerciseId);

        Optional<CourseExercise> exerciseOpt = exerciseRepository.findById(exerciseId);
        if (exerciseOpt.isEmpty()) {
            log.warn("Exercice non trouvé avec l'ID: {}", exerciseId);
            return null;
        }

        CourseExercise exercise = exerciseOpt.get();
        log.info("Exercice trouvé: {}", exercise.getName());

        if (exercise.getVideo() == null) {
            log.warn("Pas de vidéo associée à l'exercice ID: {}", exerciseId);
            return null;
        }

        log.info("Vidéo trouvée avec ID: {}", exercise.getVideo().getId());

        String videoPath = exercise.getVideo().getVideoPath();
        if (videoPath == null || videoPath.isEmpty()) {
            log.warn("Chemin de vidéo vide pour l'exercice ID: {}", exerciseId);
            return null;
        }

        log.info("Chemin de la vidéo: {}", videoPath);

        try {
            Path fullPath = Paths.get(videoUploadPath, videoPath);
            log.info("Chemin complet de la vidéo: {}", fullPath.toAbsolutePath());

            if (!Files.exists(fullPath)) {
                log.warn("Le fichier vidéo n'existe pas à: {}", fullPath.toAbsolutePath());
                return null;
            }

            byte[] videoBytes = Files.readAllBytes(fullPath);
            log.info("Vidéo lue avec succès, taille: {} octets", videoBytes.length);
            return videoBytes;
        } catch (IOException e) {
            log.error("Impossible de lire la vidéo de l'exercice {}: {}", exerciseId, e.getMessage(), e);
            return null; // Retourne null au lieu de lancer une exception
        }
    }

    /**
     * Récupérer tous les exercices d'un cours par son ID
     */
    public List<CourseExercise> getExercisesByCourseId(Long courseId) {
        // Vérifier d'abord si le cours existe
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Cours non trouvé avec l'ID: " + courseId));

        // Récupérer les exercices triés par leur ordre
        return exerciseRepository.findByCourseIdOrderByOrderIndex(courseId);
    }

    /**
     * Récupérer tous les exercices
     */
    public List<CourseExercise> getAllExercises() {
        return exerciseRepository.findAll();
    }

    /**
     * Récupérer le chemin d'upload des vidéos
     */
    public String getVideoUploadPath() {
        return this.videoUploadPath;
    }

    /**
     * Récupérer la miniature d'un cours
     */
    public byte[] getCourseThumbnail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Cours non trouvé"));

        if (course.getThumbnailPath() == null || course.getThumbnailPath().isEmpty()) {
            return null;
        }

        try {
            Path fullPath = Paths.get(thumbnailUploadPath, course.getThumbnailPath());
            if (!Files.exists(fullPath)) {
                log.warn("Le fichier de miniature n'existe pas à: {}", fullPath.toAbsolutePath());
                return null;
            }

            return Files.readAllBytes(fullPath);
        } catch (IOException e) {
            log.error("Impossible de lire la miniature du cours {}: {}", courseId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Classe de requête pour la création/mise à jour de cours
     */
    @lombok.Data
    public static class CourseRequest {
        private String title;
        private String description;
        private CourseLevel level;
        private Integer durationMinutes;
        private BigDecimal price;
        private boolean isPaid;
        private boolean isFirstVideoFree;
        private MultipartFile thumbnail;
        private List<ExerciseRequest> exercises;
    }

    /**
     * Classe de requête pour la création/mise à jour d'exercices
     */
    @lombok.Data
    public static class ExerciseRequest {
        private String name;
        private String description;
        private Integer durationSeconds;
        private String repetitions;
        private boolean isLocked;
        private boolean isFreePreview;
        private MultipartFile video;
        private Integer orderIndex;

        public void setOrderIndex(int index) {
            this.orderIndex = index;
        }
    }

    @Transactional(readOnly = true)
    public List<Course> getPurchasedCourses(Long userId) {
        // Vérifier si l'utilisateur existe
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        // Utiliser directement la requête SQL native
        return courseRepository.findPurchasedCoursesByUserId(userId);
    }

}