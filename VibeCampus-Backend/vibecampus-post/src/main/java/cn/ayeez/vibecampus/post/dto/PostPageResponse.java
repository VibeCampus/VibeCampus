package cn.ayeez.vibecampus.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 帖子列表分页响应体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPageResponse {

    private List<PostResponse> list;

    private Long total;

    private Integer page;

    private Integer pageSize;
}
