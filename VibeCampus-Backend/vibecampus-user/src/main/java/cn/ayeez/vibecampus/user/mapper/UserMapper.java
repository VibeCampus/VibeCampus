package cn.ayeez.vibecampus.user.mapper;

import cn.ayeez.vibecampus.user.model.UserProfile;
import org.apache.ibatis.annotations.*;

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
            select id, username, phone, email, gender, password_hash AS passwordHash
            from users
            where id = #{userId}
            """)
    UserProfile selectById(@Param("userId") Long userId);

    /**
     * 按登录账号查询：{@code account} 与用户名或手机号匹配，取第一条。
     * <p>用于 {@code POST /api/auth/login} 根据前端传入的 account 定位用户。</p>
     */
    @Select("""
            select id, username, phone, email, gender, password_hash as passwordHash
            from users
            where username = #{account} or phone = #{account} or email = #{account}
            limit 1
            """)
    UserProfile selectByAccount(@Param("account") String account);

    /**
     * 插入新用户记录
     * 用于用户注册流程，将注册信息持久化到数据库
     * username 用户名，必须唯一
     * passwordHash BCrypt加密后的密码哈希值（成本因子为10）
     * phone 手机号，可选，如果提供则必须唯一
     * email 邮箱，可选，如果提供则必须唯一
     * nickname 昵称，可选
     * @return 插入的记录数（成功为1）
     */
    @Insert("""
            insert into users (username, password_hash, phone, email, nickname, gender, status)
            values (#{username}, #{passwordHash}, #{phone}, #{email}, #{nickname}, #{gender}, 1)
            """)
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insertUser(UserProfile user);

}
