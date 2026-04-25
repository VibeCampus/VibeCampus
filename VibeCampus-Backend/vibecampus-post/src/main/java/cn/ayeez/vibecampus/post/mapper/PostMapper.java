package cn.ayeez.vibecampus.post.mapper;

import cn.ayeez.vibecampus.post.model.PostEntity;
import cn.ayeez.vibecampus.post.model.PostAuthorEntity;
import cn.ayeez.vibecampus.post.model.PostMediaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
