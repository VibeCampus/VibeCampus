package cn.ayeez.vibecampus.user.dto;

import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowPageResponse {
    private List<UserDetailResponse> list;
    private Long total;
    private Integer page;
    private Integer pageSize;
}