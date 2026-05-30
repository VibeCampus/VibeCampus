package cn.ayeez.vibecampus.post.controller;

import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.service.PostService;
import cn.ayeez.vibecampus.post.util.PostCurrentUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子接口控制器。
 * <p>提供帖子的查询、发布等核心功能。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 获取帖子列表（分页）。
     * <p>支持按分类筛选，自动填充当前用户的点赞和收藏状态。</p>
     *
     * @param category 分类标识（可选，如 "social"、"general" 等）
     * @param page     页码（从1开始，默认1）
     * @param pageSize 每页条数（默认20，最大100）
     * @return 帖子分页响应，包含帖子列表和总数
     */
    @GetMapping
    public PostPageResponse getPosts(@RequestParam(value = "category", required = false) String category,
                                     @RequestParam(value = "page", required = false) Integer page,
                                     @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Long currentUserId = PostCurrentUserContext.getCurrentUserIdOrNull();
        return postService.getPosts(category, page, pageSize, currentUserId);
    }

    /**
     * 获取帖子详情。
     * <p>返回帖子的完整信息，包括作者、媒体、统计数以及当前用户的互动状态。</p>
     *
     * @param postId 帖子ID
     * @return 帖子详情响应
     */
    @GetMapping("/{id}")
    public PostResponse getPostDetail(@PathVariable("id") Long postId) {
        Long currentUserId = PostCurrentUserContext.getCurrentUserIdOrNull();
        return postService.getPostDetail(postId, currentUserId);
    }

    /**
     * 创建帖子。
     * <p>支持文本、图片、视频等多种媒体类型，自动进行文件类型校验和落盘存储。</p>
     *
     * @param category  板块分类（如 "social_find"、"general" 等）
     * @param content   帖子正文（最多2000字符）
     * @param anonymous 匿名标识（"true" 或 "false"）
     * @param images    图片数组（可选，最多9张，单张不超过5MB）
     * @param video     视频文件（可选，不超过100MB）
     * @return 创建的帖子信息
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostResponse createPost(@RequestPart("category") String category,
                                   @RequestPart("content") String content,
                                   @RequestPart("anonymous") String anonymous,
                                   @RequestPart(value = "images", required = false) MultipartFile[] images,
                                   @RequestPart(value = "video", required = false) MultipartFile video) {
        log.info("收到发帖请求，category={}, imageCount={}, hasVideo={}",
                category, images == null ? 0 : images.length, video != null && !video.isEmpty());
        return postService.createPost(category, content, anonymous, images, video);
    }
}
