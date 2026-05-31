package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.user.dto.FollowPageResponse;
import cn.ayeez.vibecampus.user.mapper.FollowMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


/**
 * {@link FollowService} 默认实现：委托 {@link FollowMapper} 访问数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private final FollowMapper followMapper;

    /**
     * 关注用户
     * @param currentUserId 当前用户ID
     * @param id 用户ID
     */
    @Override
    public Map<String, Object> follow(Long currentUserId,  Long id) {

        // 不能关注自己
        if(currentUserId.equals(id))
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"不能关注自己");
        }

        // 重复保持关注幂等
        try {
            followMapper.insertFollower(currentUserId,id);
            return Map.of("following",true);
        }
        catch (DuplicateKeyException ex) {

           return Map.of("following",true);
        }

    }

    /**
     * 取消关注用户
     * @param currentUserId 当前用户ID
     * @param id 用户ID
     */
    @Override
    public Map<String, Object> unfollow(Long currentUserId,  Long id) {

        //重复取消保持幂等
        followMapper.deleteFollow(currentUserId,id);


        return Map.of("following",false);
    }

    /**
     * 分页查询某用户的关注列表，按关注时间倒序 。
     *
     * @param currentUserId 当前用户ID
     * @param id            用户ID
     * @param page          页码
     * @param pageSize      每页数量
     * @return 分页结果
     */
    @Override
    public FollowPageResponse getFollowingList(Long currentUserId, Long id, int page, Integer pageSize) {


        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePage - 1) * safePageSize;
        Long total = followMapper.countFollowing(id);
        //分页查询，返回多条数据
        List<UserProfile> userProfiles = followMapper.selectFollowingList(id, safePageSize, offset);
        List<UserDetailResponse> list = userProfiles.stream().map(u -> toUserDetailResponse(u, currentUserId)).toList();

        return new FollowPageResponse(list,total,safePage,safePageSize);
    }


    /**
     * 分页查询某用户的粉丝列表，按粉丝加入时间倒序     
     * @param currentUserId 当前用户ID
     * @param id 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @Override
    public FollowPageResponse getFollowerList(Long currentUserId, Long id, int page, Integer pageSize) {
        int safePage = normalizePage(page);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePage - 1) * safePageSize;
        long total = followMapper.countFollowers(id);

        //分页查询，返回多条数据
        List<UserProfile> userProfiles = followMapper.selectFollowerList(id,safePageSize,offset);
        List<UserDetailResponse> list = userProfiles.stream().map(u->toUserDetailResponse(u,currentUserId)).toList();
        return new FollowPageResponse(list,total,safePage,safePageSize);
    }







    /**
     * 进行数据的包装
     * @param userProfile 用户信息
     * @param currentUserId 当前用户ID
     * @return 包装后的用户详情
     */
    private UserDetailResponse toUserDetailResponse(UserProfile userProfile, Long currentUserId) {
        return UserDetailResponse.builder()
                .id(userProfile.getId())
                .username(userProfile.getUsername())
                .avatar(userProfile.getAvatarUrl())
                .bio(userProfile.getBio())
                .following(isFollowing(currentUserId,userProfile.getId())).build();
    }

    /**
     * 判断是否已关注
     */
    private Boolean isFollowing(Long currentUserId, Long id) {
        int i = followMapper.selectFollow(currentUserId,id);
        return i == 1;
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
