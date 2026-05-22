package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.common.dto.UserProfileUpdateRequest;
import cn.ayeez.vibecampus.user.mapper.UserMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@link UserService} 默认实现：委托 {@link cn.ayeez.vibecampus.user.mapper.UserMapper} 访问数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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

        UserDetailResponse response = toUserDetailResponse(userProfile, isCurrentUser);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDetailResponse updateCurrentUser(Long currentUserId, UserProfileUpdateRequest request) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或会话已过期");
        }
        if (request == null || isBlankPatch(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需要修改一项资料");
        }
        UserProfile existing = userMapper.selectFullProfileById(currentUserId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        UserProfile patch = new UserProfile();
        patch.setId(currentUserId);
        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (username.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
            }
            patch.setUsername(username);
            patch.setNickname(username);
        }
        if (request.getPhone() != null) {
            String phone = normalizeNullable(request.getPhone());
            if (phone != null && !phone.matches("^1[3-9]\\d{9}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不正确");
            }
            patch.setPhone(phone);
        }
        if (request.getEmail() != null) {
            patch.setEmail(normalizeNullable(request.getEmail()));
        }
        if (request.getGender() != null) {
            patch.setGender(parseGender(request.getGender()));
        }
        if (request.getBio() != null) {
            patch.setBio(normalizeNullable(request.getBio()));
        }

        try {
            int rows = userMapper.updateProfile(
                    currentUserId,
                    request.getUsername() != null,
                    patch.getUsername(),
                    patch.getNickname(),
                    request.getPhone() != null,
                    patch.getPhone(),
                    request.getEmail() != null,
                    patch.getEmail(),
                    request.getGender() != null,
                    patch.getGender(),
                    request.getBio() != null,
                    patch.getBio());
            if (rows <= 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
            }
        }
        catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名、手机号或邮箱已被使用");
        }
        return getUserDetail(currentUserId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(Long currentUserId, String avatarUrl) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或会话已过期");
        }
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像上传失败");
        }
        int rows = userMapper.updateAvatar(currentUserId, avatarUrl);
        if (rows <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return avatarUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long currentUserId, String oldPassword, String newPassword) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或会话已过期");
        }
        UserProfile user = userMapper.selectFullProfileById(currentUserId);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "旧密码错误");
        }
        userMapper.updatePassword(currentUserId, passwordEncoder.encode(newPassword));
    }

    private UserDetailResponse toUserDetailResponse(UserProfile userProfile, boolean isCurrentUser) {
        return UserDetailResponse.builder()
                .id(userProfile.getId())
                .username(userProfile.getUsername())
                .nickname(userProfile.getNickname())
                .avatar(userProfile.getAvatarUrl())
                .gender(formatGender(userProfile.getGender()))
                .bio(userProfile.getBio())
                .major(userProfile.getMajor())
                .joinedAt(userProfile.getCreatedAt() == null ? null : userProfile.getCreatedAt().toLocalDate().toString())
                .status(userProfile.getStatus())
                .isCurrentUser(isCurrentUser)
                .build();
    }

    private boolean isBlankPatch(UserProfileUpdateRequest request) {
        return request.getUsername() == null
                && request.getPhone() == null
                && request.getEmail() == null
                && request.getGender() == null
                && request.getBio() == null;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Integer parseGender(String gender) {
        if (gender == null || gender.isBlank() || "保密".equals(gender)) {
            return 0;
        }
        if ("男".equals(gender)) {
            return 1;
        }
        if ("女".equals(gender)) {
            return 2;
        }
        if ("其他".equals(gender)) {
            return 3;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender取值非法");
    }

    private String formatGender(Integer gender) {
        if (gender == null || gender == 0) {
            return "保密";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        if (gender == 3) {
            return "其他";
        }
        return "保密";
    }
}
