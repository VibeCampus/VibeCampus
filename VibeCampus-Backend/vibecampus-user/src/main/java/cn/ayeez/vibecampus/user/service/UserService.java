package cn.ayeez.vibecampus.user.service;

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
}
