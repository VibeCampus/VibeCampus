package cn.ayeez.vibecampus.post.controller;

import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.service.PostService;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 看帖列表接口：支持按分类和分页查询。
     */
    @GetMapping
    public PostPageResponse getPosts(@RequestParam(value = "category", required = false) String category,
                                     @RequestParam(value = "page", required = false) Integer page,
                                     @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return postService.getPosts(category, page, pageSize);
    }

    /**
     * 帖子详情查询。
     */
    @GetMapping("/{id}")
    public PostResponse getPostDetail(@PathVariable("id") Long postId) {
        return postService.getPostDetail(postId);
    }

    /**
     * 用户发帖接口：与前端 CreatePostView + API 文档 9.3 对齐。
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
