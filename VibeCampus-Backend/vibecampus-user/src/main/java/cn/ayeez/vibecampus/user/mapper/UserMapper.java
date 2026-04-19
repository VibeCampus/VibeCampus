package cn.ayeez.vibecampus.user.mapper;

import cn.ayeez.vibecampus.user.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表数据访问：约定表名为 {@code users}，列含 {@code id, username, phone, password_hash}。
 * <p>{@code password_hash} 通过列别名映射为 {@link UserProfile#getPasswordHash()}。</p>
 */
@Mapper
public interface UserMapper {

    /**
     * 按主键查询单条用户（用于「当前用户」等场景）。
     */
    @Select("""
            select id, username, phone, password_hash AS passwordHash
            from users
            where id = #{userId}
            """)
    UserProfile selectById(@Param("userId") Long userId);

    /**
     * 按登录账号查询：{@code account} 与用户名或手机号匹配，取第一条。
     * <p>用于 {@code POST /api/auth/login} 根据前端传入的 account 定位用户。</p>
     */
    @Select("""
            select id, username, phone, password_hash AS passwordHash
            from users
            where username = #{account} OR phone = #{account}
            limit 1
            """)
    UserProfile selectByAccount(@Param("account") String account);
}
