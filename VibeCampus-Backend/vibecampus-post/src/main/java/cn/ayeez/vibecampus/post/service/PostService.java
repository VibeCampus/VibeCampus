package cn.ayeez.vibecampus.post.service;

import cn.ayeez.vibecampus.common.dto.interaction.FavoriteToggleResponse;
import cn.ayeez.vibecampus.common.dto.interaction.LikeToggleResponse;
import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     *
     * @param category    分类
     * @param page        页码
     * @param pageSize    每页条数
     * @param currentUserId 当前用户ID（用于填充点赞/收藏状态）
     * @return 分页帖子列表
     */
    PostPageResponse getPosts(String category, Integer page, Integer pageSize, Long currentUserId);

    /**
     * 查询帖子详情。
     *
     * @param postId        帖子ID
     * @param currentUserId 当前用户ID（用于填充点赞/收藏状态）
     * @return 帖子详情
     */
    PostResponse getPostDetail(Long postId, Long currentUserId);

    /**
     * 切换帖子点赞状态。
     */
    LikeToggleResponse togglePostLike(Long postId, Long currentUserId);

    /**
     * 切换帖子收藏状态。
     */
    FavoriteToggleResponse togglePostFavorite(Long postId, Long currentUserId);

    /**
     * 删除帖子（软删除）。
     */
    void deletePost(Long postId, Long currentUserId);

    /**
     * 搜索帖子。
     */
    PostPageResponse searchPosts(String keyword, String sort, Integer page, Integer pageSize, Long currentUserId);

    /**
     * 获取热门帖子。
     */
    List<PostResponse> getHotPosts(Integer limit, Long currentUserId);
}
