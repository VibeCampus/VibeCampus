package cn.ayeez.vibecampus.user.service;

import cn.ayeez.vibecampus.comment.dto.CommentPageResponse;
import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.common.dto.UserProfileUpdateRequest;
import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import cn.ayeez.vibecampus.user.model.UserProfile;

/**
 * 用户读服务：按主键加载用户档案等（与认证 {@link AuthService} 解耦）。
 */
public interface UserService {

    UserProfile getCurrentUser(Long userId);

    UserDetailResponse getUserDetail(Long targetUserId, Long currentUserId);

    UserDetailResponse updateCurrentUser(Long currentUserId, UserProfileUpdateRequest request);

    String updateAvatar(Long currentUserId, String avatarUrl);

    void changePassword(Long currentUserId, String oldPassword, String newPassword);

    PostPageResponse getPostsByUserId(Long currentUserId, Long targetUserId, Integer page, Integer pageSize);

    CommentPageResponse getCommentsByUserId(Long id, Integer page, Integer pageSize);

    PostPageResponse getMyFavorite(Long currentUserId, Integer page, Integer pageSize);

    void deleteAccount(Long currentUserId, String authHeader);
}
