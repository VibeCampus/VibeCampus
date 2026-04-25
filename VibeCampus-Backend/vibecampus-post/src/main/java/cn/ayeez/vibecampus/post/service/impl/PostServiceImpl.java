package cn.ayeez.vibecampus.post.service.impl;

import cn.ayeez.vibecampus.post.dto.PostAuthorResponse;
import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.model.PostAuthorEntity;
import cn.ayeez.vibecampus.post.mapper.PostMapper;
import cn.ayeez.vibecampus.post.model.PostEntity;
import cn.ayeez.vibecampus.post.model.PostMediaEntity;
import cn.ayeez.vibecampus.post.service.PostService;
import cn.ayeez.vibecampus.post.util.PostCurrentUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 帖子服务实现：负责发帖参数校验、媒体落盘、数据库持久化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final int POST_TYPE_TEXT = 0;
    private static final int POST_TYPE_IMAGE = 1;
    private static final int POST_TYPE_VIDEO = 2;

    private static final int STATUS_NORMAL = 0;

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_IMAGE_COUNT = 9;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_VIDEO_SIZE_BYTES = 100L * 1024L * 1024L;
    private static final int SIGNATURE_READ_LIMIT = 256;

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "social_find", "social_buddy", "social_love", "share", "trade", "general"
    );

    private static final Set<StoredFileType> ALLOWED_IMAGE_TYPES = Set.of(
            StoredFileType.JPEG, StoredFileType.PNG, StoredFileType.WEBP, StoredFileType.GIF
    );

    private static final Set<StoredFileType> ALLOWED_VIDEO_TYPES = Set.of(
            StoredFileType.MP4, StoredFileType.WEBM, StoredFileType.MOV
    );

    private static final Path UPLOAD_BASE_DIR = Paths.get("uploads", "posts");

    private final PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostResponse createPost(String category, String content, String anonymous, MultipartFile[] images, MultipartFile video) {
        List<Path> createdFilePaths = new ArrayList<>();
        registerRollbackCleanup(createdFilePaths);
        try {
            String normalizedCategory = normalizeAndValidateCategory(category);
            String normalizedContent = normalizeAndValidateContent(content);
            boolean anonymousFlag = parseAnonymous(anonymous);

            MultipartFile[] safeImages = normalizeImages(images);
            validateImages(safeImages);
            validateVideo(video);

            int postType = resolvePostType(safeImages, video);
            Long currentUserId = PostCurrentUserContext.getCurrentUserId();
            Long authorId = anonymousFlag ? null : currentUserId;

            PostEntity postEntity = new PostEntity();
            postEntity.setAuthorId(authorId);
            postEntity.setCategorySlug(normalizedCategory);
            postEntity.setContent(normalizedContent);
            postEntity.setPostType(postType);
            postEntity.setStatus(STATUS_NORMAL);
            postEntity.setAnonymous(anonymousFlag ? 1 : 0);

            int insertRows = postMapper.insertPost(postEntity);
            if (insertRows <= 0 || postEntity.getId() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "发帖失败，请稍后重试");
            }

            List<String> imageUrls = saveImages(postEntity.getId(), safeImages, createdFilePaths);
            if (video != null && !video.isEmpty()) {
                saveVideo(postEntity.getId(), video, createdFilePaths);
            }

            PostEntity createdPost = postMapper.selectPostById(postEntity.getId());
            if (createdPost == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "发帖成功但读取结果失败");
            }

            if (imageUrls.isEmpty()) {
                imageUrls = postMapper.selectImageUrlsByPostId(postEntity.getId());
            }
            return new PostResponse(
                    createdPost.getId(),
                    createdPost.getCategorySlug(),
                    createdPost.getContent(),
                    createdPost.getAnonymous() != null && createdPost.getAnonymous() == 1,
                    createdPost.getAuthorId(),
                    toAuthorResponse(createdPost.getAuthorId()),
                    imageUrls,
                    createdPost.getCreatedAt() == null ? null : createdPost.getCreatedAt().toString(),
                    createdPost.getLikeCount() == null ? 0 : createdPost.getLikeCount(),
                    createdPost.getCommentCount() == null ? 0 : createdPost.getCommentCount()
            );
        }
        catch (RuntimeException ex) {
            // 业务执行期异常时可立即清理；事务回调中的删除为幂等兜底。
            cleanupCreatedFiles(createdFilePaths);
            throw ex;
        }
    }

    @Override
    public PostPageResponse getPosts(String category, Integer page, Integer pageSize) {
        String normalizedCategory = normalizeAndValidateQueryCategory(category);
        boolean socialCategory = "social".equals(normalizedCategory);
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePage - 1) * safePageSize;

        long total = postMapper.countVisiblePosts(normalizedCategory, socialCategory);
        if (total == 0) {
            return new PostPageResponse(Collections.emptyList(), 0L, safePage, safePageSize);
        }

        List<PostEntity> posts = postMapper.selectVisiblePosts(normalizedCategory, socialCategory, offset, safePageSize);
        List<Long> postIds = posts.stream().map(PostEntity::getId).toList();
        Map<Long, List<String>> imageMap = buildImageMap(postIds);
        Map<Long, PostAuthorResponse> authorMap = buildAuthorMap(posts);
        List<PostResponse> list = new ArrayList<>(posts.size());
        for (PostEntity post : posts) {
            list.add(toPostResponse(
                    post,
                    imageMap.getOrDefault(post.getId(), Collections.emptyList()),
                    authorMap.get(post.getAuthorId())
            ));
        }
        return new PostPageResponse(list, total, safePage, safePageSize);
    }

    @Override
    public PostResponse getPostDetail(Long postId) {
        if (postId == null || postId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "帖子ID非法");
        }
        PostEntity post = postMapper.selectVisiblePostById(postId);
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在");
        }
        return toPostResponse(post, postMapper.selectImageUrlsByPostId(postId), toAuthorResponse(post.getAuthorId()));
    }

    private String normalizeAndValidateCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category不能为空");
        }
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category取值非法");
        }
        return normalized;
    }

    private String normalizeAndValidateQueryCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim();
        if ("social".equals(normalized)) {
            return normalized;
        }
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category取值非法");
        }
        return normalized;
    }

    private int normalizePage(Integer page) {
        if (page == null || page <= 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageSize不能超过100");
        }
        return pageSize;
    }

    private String normalizeAndValidateContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content不能为空");
        }
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正文长度不能超过2000字符");
        }
        return normalized;
    }

    private boolean parseAnonymous(String anonymous) {
        if (anonymous == null || anonymous.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anonymous不能为空");
        }
        if ("true".equalsIgnoreCase(anonymous.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(anonymous.trim())) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anonymous仅支持true或false");
    }

    private MultipartFile[] normalizeImages(MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return new MultipartFile[0];
        }
        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image != null && !image.isEmpty()) {
                validFiles.add(image);
            }
        }
        return validFiles.toArray(new MultipartFile[0]);
    }

    private void validateImages(MultipartFile[] images) {
        if (images.length > MAX_IMAGE_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片数量不能超过9张");
        }
        for (MultipartFile image : images) {
            StoredFileType fileType = detectFileType(image);
            if (!ALLOWED_IMAGE_TYPES.contains(fileType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片格式仅支持jpg/png/webp/gif");
            }
            if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单张图片大小不能超过5MB");
            }
        }
    }

    private void validateVideo(MultipartFile video) {
        if (video == null || video.isEmpty()) {
            return;
        }
        StoredFileType fileType = detectFileType(video);
        if (!ALLOWED_VIDEO_TYPES.contains(fileType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频格式仅支持mp4/webm/mov");
        }
        if (video.getSize() > MAX_VIDEO_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频大小不能超过100MB");
        }
    }

    private int resolvePostType(MultipartFile[] images, MultipartFile video) {
        if (video != null && !video.isEmpty()) {
            return POST_TYPE_VIDEO;
        }
        if (images.length > 0) {
            return POST_TYPE_IMAGE;
        }
        return POST_TYPE_TEXT;
    }

    private List<String> saveImages(Long postId, MultipartFile[] images, List<Path> createdFilePaths) {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < images.length; i++) {
            MultipartFile image = images[i];
            StoredFileType fileType = detectFileType(image);
            SavedFile savedFile = saveFile(image, fileType, "images");
            // 文件落盘后立即纳入补偿列表，避免后续 DB 异常导致孤儿文件。
            createdFilePaths.add(savedFile.path());
            PostMediaEntity postMediaEntity = new PostMediaEntity();
            postMediaEntity.setPostId(postId);
            postMediaEntity.setMediaType(MEDIA_TYPE_IMAGE);
            postMediaEntity.setUrl(savedFile.url());
            postMediaEntity.setSortOrder(i);
            postMapper.insertPostMedia(postMediaEntity);
            urls.add(savedFile.url());
        }
        return urls;
    }

    private void saveVideo(Long postId, MultipartFile video, List<Path> createdFilePaths) {
        StoredFileType fileType = detectFileType(video);
        SavedFile savedFile = saveFile(video, fileType, "videos");
        // 文件落盘后立即纳入补偿列表，避免后续 DB 异常导致孤儿文件。
        createdFilePaths.add(savedFile.path());
        PostMediaEntity postMediaEntity = new PostMediaEntity();
        postMediaEntity.setPostId(postId);
        postMediaEntity.setMediaType(MEDIA_TYPE_VIDEO);
        postMediaEntity.setUrl(savedFile.url());
        postMediaEntity.setSortOrder(0);
        postMapper.insertPostMedia(postMediaEntity);
    }

    /**
     * 将上传文件落盘到本地 uploads 目录，并返回可访问 URL。
     * TODO(ayeez): 当前为本地磁盘存储的过渡方案，后续应替换为图床/对象存储（如 OSS/COS/MinIO），
     * 由存储服务返回稳定公网 URL，并在此方法中改为调用远程上传 SDK。
     */
    private SavedFile saveFile(MultipartFile file, StoredFileType fileType, String subDir) {
        try {
            LocalDate now = LocalDate.now();
            Path relativeDirectory = Paths.get(subDir, String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()));
            Path targetDirectory = UPLOAD_BASE_DIR.resolve(relativeDirectory);
            Files.createDirectories(targetDirectory);

            String fileName = UUID.randomUUID().toString().replace("-", "") + fileType.getExtension();
            Path targetPath = targetDirectory.resolve(fileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = "/uploads/posts/" + relativeDirectory.toString().replace("\\", "/") + "/" + fileName;
            return new SavedFile(targetPath, url);
        }
        catch (IOException e) {
            log.error("保存媒体文件失败，fileName={}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败，请稍后重试");
        }
    }

    /**
     * 通过文件魔数识别真实文件类型，避免仅依赖可伪造的 contentType。
     */
    private StoredFileType detectFileType(MultipartFile file) {
        byte[] header = readFileHeader(file);
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return StoredFileType.JPEG;
        }
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return StoredFileType.PNG;
        }
        if (header.length >= 6) {
            String gifHeader = new String(header, 0, 6);
            if ("GIF87a".equals(gifHeader) || "GIF89a".equals(gifHeader)) {
                return StoredFileType.GIF;
            }
        }
        if (header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
            return StoredFileType.WEBP;
        }
        if (header.length >= 12 && header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
            String brand = new String(header, 8, 4).toLowerCase();
            if (brand.startsWith("qt")) {
                return StoredFileType.MOV;
            }
            return StoredFileType.MP4;
        }
        if (header.length >= 4
                && (header[0] & 0xFF) == 0x1A
                && (header[1] & 0xFF) == 0x45
                && (header[2] & 0xFF) == 0xDF
                && (header[3] & 0xFF) == 0xA3
                && containsAsciiIgnoreCase(header, "webm")) {
            return StoredFileType.WEBM;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件类型非法或不受支持");
    }

    private byte[] readFileHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(SIGNATURE_READ_LIMIT);
        }
        catch (IOException e) {
            log.error("读取上传文件头失败，fileName={}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件读取失败");
        }
    }

    private boolean containsAsciiIgnoreCase(byte[] source, String token) {
        byte[] tokenBytes = token.toLowerCase().getBytes();
        for (int i = 0; i <= source.length - tokenBytes.length; i++) {
            boolean matched = true;
            for (int j = 0; j < tokenBytes.length; j++) {
                byte current = source[i + j];
                if (current >= 'A' && current <= 'Z') {
                    current = (byte) (current + 32);
                }
                if (current != tokenBytes[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private PostResponse toPostResponse(PostEntity post, List<String> images, PostAuthorResponse authorResponse) {
        return new PostResponse(
                post.getId(),
                post.getCategorySlug(),
                post.getContent(),
                post.getAnonymous() != null && post.getAnonymous() == 1,
                post.getAuthorId(),
                authorResponse,
                images == null ? Collections.emptyList() : images,
                post.getCreatedAt() == null ? null : post.getCreatedAt().toString(),
                post.getLikeCount() == null ? 0 : post.getLikeCount(),
                post.getCommentCount() == null ? 0 : post.getCommentCount()
        );
    }

    private Map<Long, List<String>> buildImageMap(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PostMediaEntity> mediaEntities = postMapper.selectImageMediaByPostIds(postIds);
        Map<Long, List<String>> imageMap = new HashMap<>();
        for (PostMediaEntity mediaEntity : mediaEntities) {
            imageMap.computeIfAbsent(mediaEntity.getPostId(), key -> new ArrayList<>()).add(mediaEntity.getUrl());
        }
        return imageMap;
    }

    private PostAuthorResponse toAuthorResponse(Long authorId) {
        if (authorId == null) {
            return null;
        }
        PostAuthorEntity authorEntity = postMapper.selectAuthorById(authorId);
        if (authorEntity == null) {
            return null;
        }
        return new PostAuthorResponse(authorEntity.getId(), authorEntity.getUsername(), authorEntity.getAvatar());
    }

    private Map<Long, PostAuthorResponse> buildAuthorMap(List<PostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> authorIds = new HashSet<>();
        for (PostEntity post : posts) {
            if (post.getAuthorId() != null) {
                authorIds.add(post.getAuthorId());
            }
        }
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PostAuthorEntity> authorEntities = postMapper.selectAuthorsByIds(new ArrayList<>(authorIds));
        Map<Long, PostAuthorResponse> authorMap = new HashMap<>();
        for (PostAuthorEntity authorEntity : authorEntities) {
            authorMap.put(
                    authorEntity.getId(),
                    new PostAuthorResponse(authorEntity.getId(), authorEntity.getUsername(), authorEntity.getAvatar())
            );
        }
        return authorMap;
    }

    private void cleanupCreatedFiles(List<Path> createdFilePaths) {
        for (Path path : createdFilePaths) {
            try {
                Files.deleteIfExists(path);
            }
            catch (IOException e) {
                log.warn("回滚时删除文件失败，path={}", path, e);
            }
        }
    }

    /**
     * 在事务结束时根据结果做文件补偿删除，覆盖 commit 阶段失败导致的回滚场景。
     */
    private void registerRollbackCleanup(List<Path> createdFilePaths) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    cleanupCreatedFiles(createdFilePaths);
                }
            }
        });
    }

    private record SavedFile(Path path, String url) {
    }

    private enum StoredFileType {
        JPEG(".jpg"),
        PNG(".png"),
        WEBP(".webp"),
        GIF(".gif"),
        MP4(".mp4"),
        WEBM(".webm"),
        MOV(".mov");

        private final String extension;

        StoredFileType(String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }
}
