package cn.ayeez.vibecampus.comment.mapper;

import cn.ayeez.vibecampus.common.dto.AuthorInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论模块专用的帖子相关操作Mapper
 * <p>用于打破 vibecampus-comment 和 vibecampus-post 之间的循环依赖</p>
 */
@Mapper
public interface CommentPostMapper {

    /**
     * 增加帖子评论数
     *
     * @param postId 帖子ID
     */
    @Update("""
            update posts
            set comment_count = comment_count + 1,
                updated_at = now()
            where id = #{postId}
              and deleted_at is null
            """)
    void incrementCommentCount(@Param("postId") Long postId);

    /**
     * 减少帖子评论数
     *
     * @param postId 帖子ID
     */
    @Update("""
            update posts
            set comment_count = greatest(comment_count - 1, 0),
                updated_at = now()
            where id = #{postId}
              and deleted_at is null
            """)
    void decrementCommentCount(@Param("postId") Long postId);

    /**
     * 根据ID查询作者信息
     *
     * @param authorId 作者ID
     * @return 作者信息
     */
    @Select("""
            select id, username, avatar
            from users
            where id = #{authorId}
            """)
    AuthorInfo selectAuthorById(@Param("authorId") Long authorId);

    /**
     * 批量查询作者信息
     *
     * @param authorIds 作者ID列表
     * @return 作者信息列表
     */
    @Select("""
            <script>
            select id, username, avatar
            from users
            where id in
            <foreach collection="authorIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<AuthorInfo> selectAuthorsByIds(@Param("authorIds") List<Long> authorIds);
}
