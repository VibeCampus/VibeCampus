package cn.ayeez.vibecampus.comment.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类：映射数据库 comments 表。
 * <p>支持一级评论和二级回复，通过 parent_id 区分：</p>
 * <ul>
 *   <li>parent_id = null：一级评论，直接挂在帖子下</li>
 *   <li>parent_id != null：二级回复，挂在某条一级评论下</li>
 * </ul>
 */
@Data
public class CommentEntity {

    /** 评论主键ID */
    private Long id;

    /** 所属帖子ID */
    private Long postId;

    /**
     * 父评论ID（用于二级回复）
     * <ul>
     *   <li>null：表示一级评论</li>
     *   <li>非null：表示这是对某条评论的回复</li>
     * </ul>
     */
    private Long parentId;

    /** 评论作者ID */
    private Long authorId;

    /**
     * 被回复的用户ID（仅二级回复时有值）
     * <p>用于展示 "@某某" 的效果</p>
     */
    private Long replyToUserId;

    /** 评论内容文本 */
    private String content;

    /**
     * 评论状态
     * <ul>
     *   <li>0：正常显示</li>
     *   <li>1：隐藏</li>
     *   <li>2：管理员删除</li>
     * </ul>
     */
    private Integer status;

    /** 点赞数 */
    private Integer likeCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     * <p>非null表示该评论已被删除，查询时需过滤 deleted_at is null</p>
     */
    private LocalDateTime deletedAt;

}
