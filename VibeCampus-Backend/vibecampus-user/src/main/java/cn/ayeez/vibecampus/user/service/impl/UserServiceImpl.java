package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.comment.dto.CommentPageResponse;
import cn.ayeez.vibecampus.comment.dto.CommentResponse;
import cn.ayeez.vibecampus.comment.mapper.CommentMapper;
import cn.ayeez.vibecampus.comment.model.CommentEntity;
import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.common.dto.UserProfileUpdateRequest;
import cn.ayeez.vibecampus.post.dto.PostAuthorResponse;
import cn.ayeez.vibecampus.post.dto.PostPageResponse;
import cn.ayeez.vibecampus.post.dto.PostResponse;
import cn.ayeez.vibecampus.post.mapper.PostMapper;
import cn.ayeez.vibecampus.post.model.PostAuthorEntity;
import cn.ayeez.vibecampus.post.model.PostEntity;
import cn.ayeez.vibecampus.post.model.PostMediaEntity;
import cn.ayeez.vibecampus.user.mapper.FollowMapper;
import cn.ayeez.vibecampus.user.mapper.UserMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.JwtTokenService;
import cn.ayeez.vibecampus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;



/**
 * {@link UserService} 默认实现：委托 {@link cn.ayeez.vibecampus.user.mapper.UserMapper} 访问数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final JwtTokenService jwtTokenService;
    private final FollowMapper followMapper;

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
            response.setFollowing(null);
            log.info("返回当前用户完整信息，userId={}", targetUserId);
        }
        else {
            // 查看他人信息，敏感字段设为null
            response.setEmail(null);
            response.setPhone(null);
            response.setFollowing(isFollowing(currentUserId, targetUserId));
            log.info("返回其他用户公开信息，targetUserId={}, viewerId={}", targetUserId, currentUserId);
        }

        response.setPostCount(postMapper.countUserPosts(targetUserId, !isCurrentUser));
        response.setFollowingCount(followMapper.countFollowing(targetUserId));
        response.setFollowerCount(followMapper.countFollowers(targetUserId));
        response.setFavoriteCount(postMapper.countMyFavorite(targetUserId));

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

    private Boolean isFollowing(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || currentUserId.equals(targetUserId)) {
            return null;
        }
        return followMapper.selectFollow(currentUserId, targetUserId) == 1;
    }


    @Override
    public void deleteAccount(Long currentUserId, String authHeader) {

        userMapper.deleteCount(currentUserId);
        jwtTokenService.revokeAccessToken(authHeader);

    }

    //查询用户帖子
    @Override
    public PostPageResponse getPostsByUserId(Long currentUserId, Long targetUserId, Integer page, Integer pageSize){

        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePage - 1) * safePageSize;

        boolean hideAnonymous = !currentUserId.equals(targetUserId);

        long total = postMapper.countUserPosts(targetUserId, hideAnonymous);
        if(total == 0){
            return new PostPageResponse(Collections.emptyList(),0L,safePage,safePageSize);
        }

        List<PostEntity> posts = postMapper.selectUserPosts(targetUserId,offset,safePageSize, hideAnonymous);
        List<Long> postIds = posts.stream().map(PostEntity::getId).toList();
        Map<Long, List<String>> imageMap = buildImageMap(postIds);
        Map<Long, PostAuthorResponse> authorMap = buildAuthorMap(posts);
        List<PostResponse> list = new ArrayList<>(posts.size());
        for(PostEntity post : posts){
            list.add(toPostResponse(
                    post,
                    imageMap.getOrDefault(post.getId(),Collections.emptyList()),
                    authorMap.get(post.getAuthorId())
            ));
        }
        return new PostPageResponse(list, total, safePage, safePageSize);
    }

    //查询用户评论
    @Override
    public CommentPageResponse getCommentsByUserId(Long userId, Integer page, Integer pageSize) {

        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePage - 1) * safePageSize;

        long total =commentMapper.countUserComments(userId);
        if(total == 0){
            return new CommentPageResponse(Collections.emptyList(),0L,safePage,safePageSize);
        }
        List<CommentEntity> comments = commentMapper.selectUserComments(userId,offset,safePageSize);
        Set<Long> postIds = comments.stream().
                map(CommentEntity::getPostId).
                collect(Collectors.toSet());
       Map<Long,String> contentMap = postMapper.selectPostByIds(new ArrayList<>(postIds)).stream()
               .collect(Collectors.toMap(PostEntity::getId,PostEntity::getContent));
       List<CommentResponse> list = new ArrayList<>();
       for(CommentEntity comment : comments){
           list.add(toCommentResponse(comment,contentMap.getOrDefault(comment.getPostId(),"帖子已删除")));
       }


        return new CommentPageResponse(list,total,safePage,safePageSize);
    }

    private CommentResponse toCommentResponse(CommentEntity comment, String title) {
        CommentResponse r = new CommentResponse();
        r.setId(comment.getId());
        r.setPostId(comment.getPostId());
        r.setLikes(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
        r.setPostTitle(title);
        r.setContent(comment.getContent());
        r.setTime(comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString());
        return r;
    }

    @Override
    public PostPageResponse getMyFavorite(Long currentUserId, Integer page, Integer pageSize) {
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePage - 1) * safePageSize;

        long total =postMapper.countMyFavorite(currentUserId);
        if(total == 0){
            return new PostPageResponse(Collections.emptyList(),0L,safePage,safePageSize);
        }
        List<PostEntity> posts = postMapper.selectMyFavorites(currentUserId,offset,safePageSize);
        List<Long> postIds = posts.stream().map(PostEntity::getId).toList();
        Map<Long, List<String>> imageMap = buildImageMap(postIds);
        Map<Long, PostAuthorResponse> authorMap = buildAuthorMap(posts);
        List<PostResponse> list = new ArrayList<>(posts.size());
        for(PostEntity post : posts){
            list.add(toPostResponse(
                    post,
                    imageMap.getOrDefault(post.getId(),Collections.emptyList()),
                    authorMap.get(post.getAuthorId())
            ));
        }
        return new PostPageResponse(list, total, safePage, safePageSize);
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

    private PostResponse toPostResponse(PostEntity post, List<String> images, PostAuthorResponse authorResponse) {
        boolean anonymous = post.getAnonymous()!=null && post.getAnonymous() == 1;
        return new PostResponse(
                post.getId(),
                post.getCategorySlug(),
                post.getContent(),
                anonymous,
                anonymous ? null : post.getAuthorId(),
                anonymous ? null : authorResponse,
                images == null ? Collections.emptyList() : images,
                post.getCreatedAt() == null ? null : post.getCreatedAt().toString(),
                post.getLikeCount() == null ? 0 : post.getLikeCount(),
                post.getCommentCount() == null ? 0 : post.getCommentCount(),
                post.getFavoriteCount() == null ? 0 : post.getFavoriteCount(),
                null,
                null
        );
    }

    private Map<Long, PostAuthorResponse> buildAuthorMap(List<PostEntity> posts) {
        if(posts == null || posts.isEmpty()){
            return Collections.emptyMap();
        }
        Set<Long> authorIds = new HashSet<>();
        for(PostEntity post : posts){
            if(post.getAuthorId()!=null){
                authorIds.add(post.getAuthorId());
            }
        }

        if(authorIds.isEmpty()){
            return Collections.emptyMap();
        }

        List<PostAuthorEntity> authorEntities = postMapper.selectAuthorsByIds(new ArrayList<>(authorIds));
        HashMap<Long, PostAuthorResponse> authorMap = new HashMap<>();
        for(PostAuthorEntity authorEntity : authorEntities){
            PostAuthorResponse author = new PostAuthorResponse();
            author.setId(authorEntity.getId());
            author.setUsername(authorEntity.getUsername());
            author.setAvatar(authorEntity.getAvatar());
            authorMap.put(authorEntity.getId(), author);
        }
        return authorMap;
    }

    private Map<Long, List<String>> buildImageMap(List<Long> postIds) {
        if(postIds == null || postIds.isEmpty()){
            return Collections.emptyMap();
        }
        List<PostMediaEntity> mediaEntities = postMapper.selectImageMediaByPostIds(postIds);
        Map<Long, List<String>> imageMap = new HashMap<>();
        for(PostMediaEntity mediaEntity : mediaEntities){
            imageMap.computeIfAbsent(mediaEntity.getPostId(), key -> new ArrayList<>()).add(mediaEntity.getUrl());
        }

        return imageMap;
    }

    private int normalizePageSize(Integer pageSize) {
        if(pageSize == null|| pageSize <= 0){
            return DEFAULT_PAGE_SIZE;
        }
        if(pageSize > MAX_PAGE_SIZE){
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST,"pageSize不能超过100");
        }
        return pageSize;
    }

    private int normalizePage(Integer page) {
        if(page == null || page <= 0)
        {
            return DEFAULT_PAGE;
        }
        return page;
    }

}
