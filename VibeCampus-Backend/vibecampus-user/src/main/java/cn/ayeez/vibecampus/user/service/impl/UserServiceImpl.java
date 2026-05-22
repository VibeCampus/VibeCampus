package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.user.mapper.UserMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link UserService} 默认实现：委托 {@link cn.ayeez.vibecampus.user.mapper.UserMapper} 访问数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserProfile getCurrentUser(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public UserDetailResponse getUserDetail(Long targetUserId, Long currentUserId) {
        log.info("查询用户详细，targetUserId={}, currentUserId={}", targetUserId, currentUserId);

        // 查询目标用户的完整信息
        UserProfile userProfile = userMapper.selectFullProfileById(targetUserId);

        // 用户不存在，返回null
        if (userProfile == null ) {
            log.info("用户不存在，targetUserId={}", targetUserId);
            return null;
        }

        // 判断是否为当前用户本人
        boolean isCurrentUser = currentUserId != null && currentUserId.equals(targetUserId);

        // 构建响应对象
        UserDetailResponse response = UserDetailResponse.builder()
                .id(userProfile.getId())
                .username(userProfile.getUsername())
                .nickname(userProfile.getNickname())
                .gender(userProfile.getGender())
                .status(userProfile.getStatus())
                .isCurrentUser(isCurrentUser)
                .build();

        // 如果是当前用户本人，返回完整信息
        if (isCurrentUser) {
            response.setEmail(userProfile.getEmail());
            response.setPhone(userProfile.getPhone());
            log.info("返回当前用户完整信息，userId={}", targetUserId);
        }
        else {
            // 查看他人信息，敏感字段设为null
            response.setEmail(null);
            response.setPhone(null);
            log.info("返回其他用户公开信息，targetUserId={}, viewerId={}", targetUserId, currentUserId);
        }

        return response;
    }
}
