package cn.ayeez.vibecampus.post.dto;

import cn.ayeez.vibecampus.common.dto.AuthorInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 帖子作者摘要信息。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostAuthorResponse extends AuthorInfo {
}
