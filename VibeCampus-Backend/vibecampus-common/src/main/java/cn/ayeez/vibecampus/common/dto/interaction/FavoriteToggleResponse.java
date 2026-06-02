package cn.ayeez.vibecampus.common.dto.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏切换响应DTO（通用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteToggleResponse {

    /**
     * 当前是否已收藏
     */
    private Boolean favorited;

    /**
     * 收藏总数
     */
    private Integer favoriteCount;

}
