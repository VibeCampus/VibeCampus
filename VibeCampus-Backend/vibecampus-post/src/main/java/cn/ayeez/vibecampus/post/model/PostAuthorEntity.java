package cn.ayeez.vibecampus.post.model;

import cn.ayeez.vibecampus.common.dto.AuthorInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子作者查询实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostAuthorEntity extends AuthorInfo {
}
