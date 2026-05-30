package cn.ayeez.vibecampus.common.dto.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞切换响应DTO（通用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeToggleResponse {

    /**
     * 当前是否已点赞
     */
    private Boolean liked;

    /**
     * 点赞总数
     */
    private Integer likeCount;

}
