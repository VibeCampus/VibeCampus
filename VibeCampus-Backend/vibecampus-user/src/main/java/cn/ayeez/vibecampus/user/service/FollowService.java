package cn.ayeez.vibecampus.user.service;

import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.user.dto.FollowPageResponse;

import java.util.List;
import java.util.Map;
/**
 * 关注服务：处理用户关注 / 取关 / 关注列表 / 粉丝列表。
 */
public interface FollowService {

    Map<String,Object> follow(Long currentUserId,  Long id);

    Map<String,Object> unfollow(Long currentUserId,  Long id);

    FollowPageResponse getFollowingList(Long currentUserId, Long id, int page, Integer pageSize);

    FollowPageResponse getFollowerList(Long currentUserId, Long id, int page, Integer pageSize);

}
