package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.user.mapper.UserMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@link UserService} 默认实现：委托 {@link cn.ayeez.vibecampus.user.mapper.UserMapper} 访问数据库。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserProfile getCurrentUser(Long userId) {
        return userMapper.selectById(userId);
    }
}
