package cn.ayeez.vibecampus.post.service;

import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子业务服务。
 */
public interface PostService {

    /**
     * 创建帖子（支持文本、图片、视频）。
     *
     * @param category  板块分类
     * @param content   帖子正文
     * @param anonymous 匿名标识（支持 "true"/"false"）
     * @param images    图片列表
     * @param video     视频文件
     * @return 创建后的帖子数据
     */
    PostResponse createPost(String category, String content, String anonymous, MultipartFile[] images, MultipartFile video);

    /**
     * 查询帖子列表。
     */
    PostPageResponse getPosts(String category, Integer page, Integer pageSize);

    /**
     * 查询帖子详情。
     */
    PostResponse getPostDetail(Long postId);
}
