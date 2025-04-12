package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public CourseController(
            CourseService courseService,
            UserService userService,
            UserRepository userRepository, CourseRepository courseRepository
    ) {
        this.courseService = courseService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * Créer un nouveau cours
     */
    @PostMapping
    public ResponseEntity<Course> createCourse(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("level") CourseLevel level,
            @RequestParam("durationMinutes") Integer durationMinutes,
            @RequestParam("price") BigDecimal price,
            @RequestParam("isPaid") boolean isPaid,
            @RequestParam("isFirstVideoFree") boolean isFirstVideoFree,
            @RequestParam("coachId") Long coachId,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {

        try {
            // Récupérer l'utilisateur coach par ID
            User coach = userRepository.findById(coachId)
                    .orElseThrow(() -> new NoSuchElementException("Coach non trouvé"));

            // Vérifier que l'utilisateur est bien un coach
            if (coach.getRole() != Role.COACH) {
                throw new IllegalArgumentException("L'utilisateur n'est pas un coach");
            }

            // Créer l'objet CourseRequest manuellement
            CourseService.CourseRequest courseRequest = new CourseService.CourseRequest();
            courseRequest.setTitle(title);
            courseRequest.setDescription(description);
            courseRequest.setLevel(level);
            courseRequest.setDurationMinutes(durationMinutes);
            courseRequest.setPrice(price);
            courseRequest.setPaid(isPaid);
            courseRequest.setFirstVideoFree(isFirstVideoFree);
            courseRequest.setThumbnail(thumbnail);

            Course course = courseService.createCourseWithExercises(courseRequest, coach);
            return ResponseEntity.status(HttpStatus.CREATED).body(course);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Récupérer tous les exercices d'un cours
     */
    @GetMapping("/{courseId}/exercises")
    public ResponseEntity<List<CourseExercise>> getExercisesByCourseId(@PathVariable Long courseId) {
        try {
            List<CourseExercise> exercises = courseService.getExercisesByCourseId(courseId);
            return ResponseEntity.ok(exercises);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Mettre à jour un cours existant
     */
    @PutMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCourse(
            @PathVariable Long courseId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("level") CourseLevel level,
            @RequestParam("durationMinutes") Integer durationMinutes,
            @RequestParam("price") BigDecimal price,
            @RequestParam("isPaid") boolean isPaid,
            @RequestParam("isFirstVideoFree") boolean isFirstVideoFree,
            @RequestParam("coachId") Long coachId,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        try {
            // Récupérer le coach
            Optional<User> coach = userService.getUserById(coachId);
            if (coach.isEmpty() || coach.get().getRole() != Role.COACH) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utilisateur non autorisé");
            }

            // Préparer la requête de mise à jour de cours
            CourseService.CourseRequest request = new CourseService.CourseRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setLevel(level);
            request.setDurationMinutes(durationMinutes);
            request.setPrice(price);
            request.setPaid(isPaid);
            request.setFirstVideoFree(isFirstVideoFree);
            request.setThumbnail(thumbnail);

            // Mettre à jour le cours
            Course updatedCourse = courseService.updateCourse(courseId, request, coach.get());
            return ResponseEntity.ok(updatedCourse);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la mise à jour du cours : " + e.getMessage());
        }
    }

    /**
     * Récupérer un cours par son ID
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseById(@PathVariable Long courseId) {
        try {
            Course course = courseService.getCourseById(courseId);
            return ResponseEntity.ok(course);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupérer la miniature d'un cours
     */
    @GetMapping("/{courseId}/thumbnail")
    public ResponseEntity<byte[]> getCourseThumbnail(@PathVariable Long courseId) {
        try {
            byte[] thumbnailData = courseService.getCourseThumbnail(courseId);

            if (thumbnailData == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG); // Ajuster selon le type d'image
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename("course_thumbnail_" + courseId + ".jpg")
                            .build()
            );

            return new ResponseEntity<>(thumbnailData, headers, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Ajouter un exercice à un cours
     */
    @PostMapping("/{courseId}/exercises")
    public ResponseEntity<?> addExerciseToCourse(
            @PathVariable Long courseId,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("durationSeconds") Integer durationSeconds,
            @RequestParam("repetitions") String repetitions,
            @RequestParam("isLocked") boolean isLocked,
            @RequestParam("isFreePreview") boolean isFreePreview,
            @RequestParam("coachId") Long coachId,
            @RequestPart(value = "video", required = false) MultipartFile video
    ) {
        try {
            // Récupérer le cours
            Course course = courseService.getCourseById(courseId);

            // Vérifier les permissions du coach
            Optional<User> coach = userService.getUserById(coachId);
            if (coach.isEmpty() ||
                    coach.get().getRole() != Role.COACH ||
                    !course.getCoach().getId().equals(coach.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès non autorisé");
            }

            // Préparer la requête de création d'exercice
            CourseService.ExerciseRequest request = new CourseService.ExerciseRequest();
            request.setName(name);
            request.setDescription(description);
            request.setDurationSeconds(durationSeconds);
            request.setRepetitions(repetitions);
            request.setOrderIndex(course.getExercises().size()); // Ajouter à la fin
            request.setLocked(isLocked);
            request.setFreePreview(isFreePreview);
            request.setVideo(video);

            // Créer l'exercice
            CourseExercise createdExercise = courseService.createExercise(request, course, coach.get());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdExercise);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'ajout de l'exercice : " + e.getMessage());
        }
    }

    /**
     * Récupérer tous les cours
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * Récupérer les cours par niveau
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Course>> getCoursesByLevel(@PathVariable String level) {
        try {
            CourseLevel courseLevel = CourseLevel.fromString(level);
            return ResponseEntity.ok(courseService.getCoursesByLevel(courseLevel));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Récupérer les cours par coach
     */
    @GetMapping("/coach/{coachId}")
    public ResponseEntity<List<Course>> getCoursesByCoach(@PathVariable Long coachId) {
        try {
            Optional<User> coach = userService.getUserById(coachId);
            if (coach.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(courseService.getCoursesByCoach(coach.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprimer un cours
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(
            @PathVariable Long courseId,
            @RequestParam Long userId
    ) {
        try {
            Optional<User> user = userService.getUserById(userId);
            Course course = courseService.getCourseById(courseId);

            if (user.isEmpty() ||
                    !(user.get().getRole() == Role.ADMIN ||
                            (user.get().getRole() == Role.COACH &&
                                    course.getCoach().getId().equals(user.get().getId())))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès non autorisé");
            }

            courseService.deleteCourse(courseId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du cours : " + e.getMessage());
        }
    }

    /**
     * Supprimer un exercice
     */
    @DeleteMapping("/exercises/{exerciseId}")
    public ResponseEntity<?> deleteExercise(
            @PathVariable Long exerciseId,
            @RequestParam Long coachId
    ) {
        try {
            Optional<User> coach = userService.getUserById(coachId);
            Optional<CourseExercise> exerciseOpt = courseService.getExerciseById(exerciseId);

            if (exerciseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CourseExercise exercise = exerciseOpt.get();
            if (coach.isEmpty() ||
                    coach.get().getRole() != Role.COACH ||
                    !exercise.getCourse().getCoach().getId().equals(coach.get().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès non autorisé");
            }

            courseService.deleteExercise(exerciseId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression de l'exercice : " + e.getMessage());
        }
    }

    /**
     * Récupérer la vidéo d'un exercice
     */
    @GetMapping("/exercises/{exerciseId}/video")
    public ResponseEntity<byte[]> getExerciseVideo(@PathVariable Long exerciseId) {
        try {
            Optional<CourseExercise> exerciseOpt = courseService.getExerciseById(exerciseId);

            if (exerciseOpt.isEmpty() || exerciseOpt.get().getVideo() == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] videoBytes = courseService.getExerciseVideoBytes(exerciseId);

            if (videoBytes == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            // Headers CORS importants pour iOS
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Accept");
            // Headers importants pour le streaming
            headers.add("Accept-Ranges", "bytes");
            headers.setContentLength(videoBytes.length);
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename("exercise_video_" + exerciseId + ".mp4")
                            .build()
            );

            return new ResponseEntity<>(videoBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Point de test pour l'accès aux vidéos
     */
    @GetMapping("/test-video-access")
    public ResponseEntity<Map<String, Object>> testVideoAccess() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Accéder au chemin de stockage vidéo depuis le service
            String videoUploadPath = courseService.getVideoUploadPath();

            // Tester l'accès au répertoire de vidéos
            Path videoDir = Paths.get(videoUploadPath);
            boolean videoDirExists = Files.exists(videoDir);
            response.put("videoDirExists", videoDirExists);
            response.put("videoDirPath", videoDir.toAbsolutePath().toString());

            if (videoDirExists) {
                // Lister les fichiers dans le répertoire
                List<String> files = Files.list(videoDir)
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
                response.put("files", files);
            }

            // Vérifier les exercices avec vidéos
            List<Map<String, Object>> exercisesWithVideos = new ArrayList<>();
            List<CourseExercise> exercises = courseService.getAllExercises();

            for (CourseExercise exercise : exercises) {
                if (exercise.getVideo() != null) {
                    Map<String, Object> exerciseInfo = new HashMap<>();
                    exerciseInfo.put("exerciseId", exercise.getId());
                    exerciseInfo.put("exerciseName", exercise.getName());
                    exerciseInfo.put("videoId", exercise.getVideo().getId());
                    exerciseInfo.put("videoPath", exercise.getVideo().getVideoPath());

                    // Vérifier si le fichier existe
                    String videoFilePath = exercise.getVideo().getVideoPath();
                    if (videoFilePath != null) {
                        Path videoFile = Paths.get(videoUploadPath, videoFilePath);
                        exerciseInfo.put("fileExists", Files.exists(videoFile));
                        exerciseInfo.put("absolutePath", videoFile.toAbsolutePath().toString());
                    }

                    exercisesWithVideos.add(exerciseInfo);
                }
            }

            response.put("exercisesWithVideos", exercisesWithVideos);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            response.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Récupérer les cours par coach avec les exercices (pour l'application mobile)
     */
    @GetMapping("/coach/{coachId}/with-exercises")
    public ResponseEntity<List<Course>> getCoursesByCoachWithExercises(@PathVariable Long coachId) {
        try {
            Optional<User> coach = userService.getUserById(coachId);
            if (coach.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<Course> courses = courseService.getCoursesByCoach(coach.get());

            // Pour chaque cours, charger explicitement les exercices
            for (Course course : courses) {
                List<CourseExercise> exercises = courseService.getExercisesByCourseId(course.getId());
                course.setExercises(exercises);
            }

            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les détails d'un exercice par son ID
     */
    @GetMapping("/exercises/{exerciseId}")
    public ResponseEntity<CourseExercise> getExerciseById(@PathVariable Long exerciseId) {
        try {
            Optional<CourseExercise> exercise = courseService.getExerciseById(exerciseId);

            if (exercise.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(exercise.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint pour tester l'accès aux miniatures
     */
    @GetMapping("/test-thumbnail-access")
    public ResponseEntity<Map<String, Object>> testThumbnailAccess() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Liste des cours avec leurs miniatures
            List<Map<String, Object>> coursesWithThumbnails = new ArrayList<>();
            List<Course> courses = courseService.getAllCourses();

            for (Course course : courses) {
                if (course.getThumbnailPath() != null && !course.getThumbnailPath().isEmpty()) {
                    Map<String, Object> courseInfo = new HashMap<>();
                    courseInfo.put("courseId", course.getId());
                    courseInfo.put("courseTitle", course.getTitle());
                    courseInfo.put("thumbnailPath", course.getThumbnailPath());

                    coursesWithThumbnails.add(courseInfo);
                }
            }

            response.put("coursesWithThumbnails", coursesWithThumbnails);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            return ResponseEntity.status(500).body(response);
        }
    }
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @GetMapping("/purchased")
    public ResponseEntity<?> getPurchasedCourses(@RequestParam("userId") Long userId) {
        try {
            // Récupérer tous les cours (avec leurs inscriptions)
            List<Course> allCourses = courseRepository.findAll();

            // Liste pour stocker les cours achetés
            List<Course> purchasedCourses = new ArrayList<>();

            // Filtrer manuellement
            for (Course course : allCourses) {
                if (course.getEnrollments() != null && !course.getEnrollments().isEmpty()) {
                    // Vérifier chaque inscription
                    for (Enrollment enrollment : course.getEnrollments()) {
                        // Si l'inscription appartient à l'utilisateur demandé
                        if (enrollment != null && userId.equals(enrollment.getUserId())) {
                            purchasedCourses.add(course);
                            break;  // Passer au cours suivant une fois trouvé
                        }
                    }
                }
            }

            return ResponseEntity.ok(purchasedCourses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }
}