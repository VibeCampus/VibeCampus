package cn.ayeez.vibecampus.post.mapper;

import cn.ayeez.vibecampus.post.model.PostEntity;
import cn.ayeez.vibecampus.post.model.PostAuthorEntity;
import cn.ayeez.vibecampus.post.model.PostMediaEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostMapper {

    /**
     * 新增帖子主记录。
     */
    @Insert("""
            insert into posts (author_id, category_slug, content, post_type, status, anonymous)
            values (#{authorId}, #{categorySlug}, #{content}, #{postType}, #{status}, #{anonymous})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertPost(PostEntity postEntity);

    /**
     * 新增帖子媒体。
     */
    @Insert("""
            insert into post_media (post_id, media_type, url, sort_order)
            values (#{postId}, #{mediaType}, #{url}, #{sortOrder})
            """)
    int insertPostMedia(PostMediaEntity postMediaEntity);

    /**
     * 回查帖子，组装创建接口响应体。
     */
    @Select("""
            select id, author_id, category_slug, content, post_type, status, anonymous,
                   like_count, comment_count, favorite_count, created_at
            from posts
            where id = #{postId} and deleted_at is null
            """)
    PostEntity selectPostById(@Param("postId") Long postId);

    /**
     * 查询可展示帖子数量。
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select count(1)
            from posts
            where deleted_at is null
              and status = 0
              <if test="socialCategory">
                and category_slug in ('social_find', 'social_buddy', 'social_love')
              </if>
              <if test="!socialCategory and category != null and category != ''">
                and category_slug = #{category}
              </if>
            </script>
            """)
    long countVisiblePosts(@Param("category") String category, @Param("socialCategory") boolean socialCategory);

    /**
     * 分页查询可展示帖子。
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select id, author_id, category_slug, content, post_type, status, anonymous,
                   like_count, comment_count, favorite_count, created_at
            from posts
            where deleted_at is null
              and status = 0
              <if test="socialCategory">
                and category_slug in ('social_find', 'social_buddy', 'social_love')
              </if>
              <if test="!socialCategory and category != null and category != ''">
                and category_slug = #{category}
              </if>
            order by created_at desc, id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<PostEntity> selectVisiblePosts(@Param("category") String category,
                                        @Param("socialCategory") boolean socialCategory,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 按 id 查询可展示帖子详情。
     */
    @Select("""
            select id, author_id, category_slug, content, post_type, status, anonymous,
                   like_count, comment_count, favorite_count, created_at
            from posts
            where id = #{postId}
              and deleted_at is null
              and status = 0
            """)
    PostEntity selectVisiblePostById(@Param("postId") Long postId);

    /**
     * 回查图片 URL 列表。
     */
    @Select("""
            select url
            from post_media
            where post_id = #{postId} and media_type = 1
            order by sort_order asc, id asc
            """)
    List<String> selectImageUrlsByPostId(@Param("postId") Long postId);

    /**
     * 批量查询帖子图片媒体，避免列表查询 N+1。
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select post_id, media_type, url, sort_order
            from post_media
            where media_type = 1
              and post_id in
            <foreach collection="postIds" item="postId" open="(" separator="," close=")">
                #{postId}
            </foreach>
            order by post_id asc, sort_order asc, id asc
            </script>
            """)
    List<PostMediaEntity> selectImageMediaByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 查询帖子作者摘要信息。
     */
    @Select("""
            select id, username, avatar_url as avatar
            from users
            where id = #{authorId}
            """)
    PostAuthorEntity selectAuthorById(@Param("authorId") Long authorId);

    /**
     * 批量查询作者摘要信息。
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select id, username, avatar_url as avatar
            from users
            where id in
            <foreach collection="authorIds" item="authorId" open="(" separator="," close=")">
                #{authorId}
            </foreach>
            </script>
            """)
    List<PostAuthorEntity> selectAuthorsByIds(@Param("authorIds") List<Long> authorIds);

    /**
     * 更新帖子评论数量
     * @param postId
     * @return
     */
    @Update("""
            update posts
            set comment_count = comment_count + 1
            where id = #{postId}
              and deleted_at is null
            """)
    int incrementCommentCount(@Param("postId") Long postId);

    @Update("""
            update posts
            set comment_count = GREATEST(comment_count - 1, 0)
            where id = #{postId}
              and deleted_at is null
            """)
    int decrementCommentCount(@Param("postId") Long postId);

    /**
     * 更新帖子点赞数量
     * @param postId
     * @return
     */
    @Update("""
            update posts
            set like_count = like_count + 1
            where id = #{postId}
              and deleted_at is null
            """)
    int incrementLikeCount(@Param("postId") Long postId);

    @Update("""
            update posts
            set like_count = GREATEST(like_count - 1, 0)
            where id = #{postId}
              and deleted_at is null
            """)
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * 更新帖子收藏数量
     * @param postId
     * @return
     */
    @Update("""
            update posts
            set favorite_count = favorite_count + 1
            where id = #{postId}
              and deleted_at is null
            """)
    int incrementFavoriteCount(@Param("postId") Long postId);

    @Update("""
            update posts
            set favorite_count = GREATEST(favorite_count - 1, 0)
            where id = #{postId}
              and deleted_at is null
            """)
    int decrementFavoriteCount(@Param("postId") Long postId);

    /**
     * 插入帖子点赞记录
     * @param userId
     * @param postId
     * @return
     */
    @Insert("""
            insert ignore into post_likes (user_id, post_id)
            values (#{userId}, #{postId})
            """)
    int insertPostLike(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 删除帖子点赞记录
     * @param userId
     * @param postId
     * @return
     */
    @Delete("""
            delete from post_likes
            where user_id = #{userId}
              and post_id = #{postId}
            """)
    int deletePostLike(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 查询帖子点赞记录
     * @param userId
     * @param postId
     * @return
     */
    @Select("""
            select count(1)
            from post_likes
            where user_id = #{userId}
              and post_id = #{postId}
            """)
    int countPostLike(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 插入帖子收藏记录
     * @param userId
     * @param postId
     * @return
     */
    @Insert("""
            insert ignore into post_favorites (user_id, post_id)
            values (#{userId}, #{postId})
            """)
    int insertPostFavorite(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 删除帖子收藏记录
     * @param userId
     * @param postId
     * @return
     */
    @Delete("""
            delete from post_favorites
            where user_id = #{userId}
              and post_id = #{postId}
            """)
    int deletePostFavorite(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 查询用户是否收藏了某个帖子
     * @param userId    用户ID
     * @param postId    帖子ID
     * @return 收藏记录数（0或1）
     */
    @Select("""
            select count(1)
            from post_favorites
            where user_id = #{userId}
              and post_id = #{postId}
            """)
    int countPostFavorite(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 删除帖子
     * @param postId
     * @param authorId
     * @return
     */
    @Update("""
            update posts
            set deleted_at = now()
            where id = #{postId}
              and author_id = #{authorId}
              and deleted_at is null
            """)
    int softDeletePost(@Param("postId") Long postId, @Param("authorId") Long authorId);

    /**
     * 查询帖子详情
     * @param postId
     * @return
     */
    @Select("""
            select id, author_id, category_slug, content, post_type, status, anonymous,
                   like_count, comment_count, favorite_count, created_at
            from posts
            where id = #{postId}
            """)
    PostEntity selectPostByIdIncludeDeleted(@Param("postId") Long postId);

    /**
     * 查询热门帖子
     * @param limit
     * @return
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select id, suthor_id, category_slug, content, post_type, status, anonymous,
                   like_count, comment_count, favorite_count, created_at
            from posts
            where deleted_at is null and status = 0
            order by hot_score desc, id desc 
            limit #{limit}
            </script>
            """)
    List<PostEntity> selectHotPosts(@Param("limit") Integer limit);

    /**
     * 关键词搜索帖子
     * @param keyword
     * @param sort
     * @param offset
     * @param limit
     * @return
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select id, author_id, category_slug, content, post_type, status, anonymous,
                   like_count, comment_count, favorite_count, created_at
            from posts
            where deleted_at is null and status = 0
              and (
                  content like concat('%', #{keyword}, '%')
              )
            <choose>
                <when test="sort == 'likes">
                    order by like_count desc, id desc
                </when>
                <when test="sort == 'comments'">
                    order by comment_count desc, id desc
                </when>
                <otherwise>
                    order by created_at desc, id desc
                </otherwise>
            </choose>
            limit #{limit} offset #{offset}
            </script>
            """)
    List<PostEntity> searchPosts(@Param("keyword") String keyword,
                                 @Param("sort") String sort,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    /**
     * 统计关键词搜索结果数量
     * @param keyword
     * @return
     */
    // language=MyBatisXML
    @Select("""
            <script>
            select count(1)
            from posts
            where deleted_at is null and status = 0
              and (
                  content like concat('%', #{keyword}, '%')
              )
            </script>
            """)
    long countSearchResults(@Param("keyword") String keyword);
}
