package cn.ayeez.vibecampus.comment.mapper;

import cn.ayeez.vibecampus.comment.model.CommentEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("""
            insert into comments (post_id, parent_id, author_id, reply_to_user_id, content, status)
            values (#{postId}, #{parentId}, #{authorId}, #{replyToUserId}, #{content}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertComment(CommentEntity comment);

    // language=MyBatisXML
    @Select("""
            <script>
            select id, post_id, parent_id, author_id, reply_to_user_id, content, status,
                   like_count, created_at, updated_at, deleted_at
            from comments
            where post_id = #{postId}
              and parent_id is null
              and deleted_at is null
            order by created_at asc, id asc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<CommentEntity> selectTopLevelComments(@Param("postId") Long postId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    // language=MyBatisXML
    @Select("""
            <script>
            select count(1)
            from comments
            where post_id = #{postId}
              and parent_id is null
              and deleted_at is null
            </script>
            """)
    long countTopLevelComments(@Param("postId") Long postId);

    // language=MyBatisXML
    @Select("""
            <script>
            select id, post_id, parent_id, author_id, reply_to_user_id, content, status,
                   like_count, created_at, updated_at, deleted_at
            from comments
            where parent_id = #{parentId}
              and deleted_at is null
            order by created_at asc, id asc
            </script>
            """)
    List<CommentEntity> selectRepliesByParentId(@Param("parentId") Long parentId);

    @Select("""
            select id, post_id, parent_id, author_id, reply_to_user_id, content, status,
                   like_count, created_at, updated_at, deleted_at
            from comments
            where id = #{id}
              and deleted_at is null
            """)
    CommentEntity selectById(@Param("id") Long id);

    @Update("""
            update comments
            set like_count = like_count + 1
            where id = #{commentId}
              and deleted_at is null
            """)
    int incrementLikeCount(@Param("commentId") Long commentId);

    @Update("""
            update comments
            set like_count = greatest(like_count - 1, 0)
            where id = #{commentId}
              and deleted_at is null
            """)
    int decrementLikeCount(@Param("commentId") Long commentId);

    @Insert("""
            insert ignore into comment_likes (user_id, comment_id)
            values (#{userId}, #{commentId})
            """)
    int insertCommentLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Delete("""
            delete from comment_likes
            where user_id = #{userId}
              and comment_id = #{commentId}
            """)
    int deleteCommentLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Select("""
            select count(1)
            from comment_likes
            where user_id = #{userId}
              and comment_id = #{commentId}
            """)
    int countCommentLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Update("""
            update comments
            set deleted_at = now()
            where id = #{commentId}
              and author_id = #{authorId}
              and deleted_at is null
            """)
    int softDeleteComment(@Param("commentId") Long commentId, @Param("authorId") Long authorId);

    @Select("""
            select id, post_id, parent_id, author_id, reply_to_user_id, content, status,
                   like_count, created_at, updated_at, updated_at, deleted_at
            from comments
            where id = #{commentId}
            """)
    CommentEntity selectByIdIncludeDeleted(@Param("commentId") Long commentId);
}
