package cn.ayeez.vibecampus.post.service.impl;

import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.mapper.PostMapper;
import cn.ayeez.vibecampus.post.model.PostAuthorEntity;
import cn.ayeez.vibecampus.post.model.PostEntity;
import cn.ayeez.vibecampus.post.model.PostMediaEntity;
import cn.ayeez.vibecampus.post.util.PostCurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PostServiceImpl 单元测试：
 * 重点覆盖分页查询、详情查询、发帖校验与异常清理等核心逻辑。
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    private static final Path UPLOAD_ROOT = Path.of("uploads", "posts");

    @Mock
    private PostMapper postMapper;

    @AfterEach
    void clearUploadRootIfEmpty() throws IOException {
        // 清理测试执行后可能残留的空目录，避免污染仓库。
        if (!Files.exists(UPLOAD_ROOT)) {
            return;
        }
        try (var walk = Files.walk(UPLOAD_ROOT)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(UPLOAD_ROOT))
                    .forEach(path -> {
                        try {
                            if (Files.isDirectory(path) && isDirectoryEmpty(path)) {
                                Files.deleteIfExists(path);
                            }
                        }
                        catch (IOException ignored) {
                            // 测试清理逻辑不影响断言结果，忽略异常。
                        }
                    });
        }
    }

    @Test
    void getPosts_shouldSupportSocialCategoryAndBatchLoadMedia() {
        PostServiceImpl service = new PostServiceImpl(postMapper);
        PostEntity post = buildPost(1001L, 2001L, "social_find");
        PostMediaEntity media = new PostMediaEntity();
        media.setPostId(1001L);
        media.setMediaType(1);
        media.setUrl("/uploads/posts/images/2026/04/img-a.jpg");
        media.setSortOrder(0);
        PostAuthorEntity authorEntity = new PostAuthorEntity();
        authorEntity.setId(2001L);
        authorEntity.setUsername("alice");
        authorEntity.setAvatar("/uploads/avatar/alice.jpg");

        when(postMapper.countVisiblePosts(eq("social"), eq(true))).thenReturn(1L);
        when(postMapper.selectVisiblePosts(eq("social"), eq(true), eq(0), eq(20))).thenReturn(List.of(post));
        when(postMapper.selectImageMediaByPostIds(eq(List.of(1001L)))).thenReturn(List.of(media));
        when(postMapper.selectAuthorsByIds(any())).thenReturn(List.of(authorEntity));

        PostPageResponse response = service.getPosts("social", null, null);

        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(20, response.getPageSize());
        assertEquals(1, response.getList().size());
        assertEquals("/uploads/posts/images/2026/04/img-a.jpg", response.getList().getFirst().getImages().getFirst());
        assertNotNull(response.getList().getFirst().getAuthor());
        assertEquals("alice", response.getList().getFirst().getAuthor().getUsername());

        verify(postMapper, never()).selectImageUrlsByPostId(anyLong());
        verify(postMapper).selectImageMediaByPostIds(eq(List.of(1001L)));
    }

    @Test
    void getPosts_shouldRejectTooLargePageSize() {
        PostServiceImpl service = new PostServiceImpl(postMapper);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getPosts(null, 1, 101));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("pageSize不能超过100", ex.getReason());
        verify(postMapper, never()).countVisiblePosts(anyString(), anyBoolean());
    }

    @Test
    void getPostDetail_shouldReturn404WhenMissing() {
        PostServiceImpl service = new PostServiceImpl(postMapper);
        when(postMapper.selectVisiblePostById(999L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getPostDetail(999L));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("帖子不存在", ex.getReason());
    }

    @Test
    void createPost_shouldRejectFileWithFakeSignature() {
        PostServiceImpl service = new PostServiceImpl(postMapper);
        MockMultipartFile fakeImage = new MockMultipartFile(
                "images",
                "payload.png",
                "image/png",
                "not-a-real-image".getBytes()
        );

        try (MockedStatic<PostCurrentUserContext> mockedStatic = org.mockito.Mockito.mockStatic(PostCurrentUserContext.class)) {
            mockedStatic.when(PostCurrentUserContext::getCurrentUserId).thenReturn(7L);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createPost("share", "hello", "false", new MockMultipartFile[]{fakeImage}, null));

            assertEquals(400, ex.getStatusCode().value());
            assertEquals("文件类型非法或不受支持", ex.getReason());
            verify(postMapper, never()).insertPost(any());
        }
    }

    @Test
    void createPost_shouldCleanupCreatedFilesWhenMapperFails() throws IOException {
        PostServiceImpl service = new PostServiceImpl(postMapper);
        byte[] pngData = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", pngData);

        Set<Path> before = snapshotUploadFiles();
        try (MockedStatic<PostCurrentUserContext> mockedStatic = org.mockito.Mockito.mockStatic(PostCurrentUserContext.class)) {
            mockedStatic.when(PostCurrentUserContext::getCurrentUserId).thenReturn(8L);

            when(postMapper.insertPost(any())).thenAnswer(invocation -> {
                PostEntity entity = invocation.getArgument(0);
                entity.setId(12345L);
                return 1;
            });
            when(postMapper.insertPostMedia(any())).thenThrow(new RuntimeException("db failure"));

            assertThrows(RuntimeException.class,
                    () -> service.createPost("share", "content", "false", new MockMultipartFile[]{image}, null));
        }

        Set<Path> after = snapshotUploadFiles();
        assertEquals(before, after, "发帖失败后不应遗留新增上传文件");
    }

    private PostEntity buildPost(Long id, Long authorId, String category) {
        PostEntity entity = new PostEntity();
        entity.setId(id);
        entity.setAuthorId(authorId);
        entity.setCategorySlug(category);
        entity.setContent("content");
        entity.setAnonymous(0);
        entity.setLikeCount(2);
        entity.setCommentCount(1);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private Set<Path> snapshotUploadFiles() throws IOException {
        if (!Files.exists(UPLOAD_ROOT)) {
            return Set.of();
        }
        try (var walk = Files.walk(UPLOAD_ROOT)) {
            Set<Path> fileSet = new HashSet<>();
            walk.filter(Files::isRegularFile)
                    .forEach(path -> fileSet.add(UPLOAD_ROOT.relativize(path)));
            return fileSet;
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }
}
