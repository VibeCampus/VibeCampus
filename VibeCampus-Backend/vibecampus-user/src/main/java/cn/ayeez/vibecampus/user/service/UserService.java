package cn.ayeez.vibecampus.user.service;

import cn.ayeez.vibecampus.user.dto.UserDetailResponse;
import cn.ayeez.vibecampus.user.model.UserProfile;

/**
 * 用户读服务：按主键加载用户档案等（与认证 {@link AuthService} 解耦）。
 */
public interface UserService {

    /**
     * 根据用户主键查询档案；无记录时返回 {@code null}，由调用方决定如何响应。
     *
     * @param userId 用户 ID
     * @return 用户读模型，不存在则为 null
     */
    UserProfile getCurrentUser(Long userId);

    /**
     * 查询用户详细信息
     * 1. 查看自己的信息：返回完整信息
     * 2. 查看他人信息：返回部分信息（隐藏敏感信息）
     * targetUserI 目标用户ID
     * currentUserId 当前用户ID
     * return 用户详细信息响应对象，若不存在则返回null
     */
    UserDetailResponse getUserDetail(Long targetUserId, Long currentUserId);
}
