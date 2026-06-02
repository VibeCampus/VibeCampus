package cn.ayeez.vibecampus.user.mapper;


import cn.ayeez.vibecampus.user.model.UserProfile;
import org.apache.ibatis.annotations.*;
import java.util.List;


/**
 * 关注关系数据访问：表 {@code follows}，列 follower_id / following_id。
 */
@Mapper
public interface FollowMapper {

    /**
     * 按 follower 和 following 查询关注记录数（0 或 1）。
     */
    @Select("""
            select count(1) from follows where follower_id = #{currentUserId} AND following_id = #{id}
            """)
    int selectFollow(@Param("currentUserId")Long currentUserId,@Param("id")Long id);


    /**
     * 插入一条关注记录
     */
    @Insert("""
            insert into follows(follower_id,following_id) 
            values (#{currentUserId},#{id})
            """)
    int insertFollower(@Param("currentUserId") Long currentUserId,@Param("id") Long id);


    /**
     * 按follower和following删除一条关注记录
     */
    @Delete("""
        delete from follows where follower_id = #{currentUserId} AND following_id = #{id}
            """)
    int deleteFollow(@Param("currentUserId")Long currentUserId,@Param("id")Long id);


    /**
     * 统计某用户的粉丝数量（关注他的人）
     */
    @Select("""
            select count(*) from follows where following_id = #{id} and follower_id is not null
        """)
    Long countFollowers(@Param("id")Long id);


    /**
     * 统计某用户的关注数量（他关注的人）
     */
    @Select("""
            select count(*) from follows where follower_id = #{id} and following_id is not null
        """)
    Long countFollowing(@Param("id")Long id);

    /**
     * 分页查询某用户的关注列表，按关注时间倒序
     */
    @Select("""
            select u.id,u.username,u.avatar_url,u.bio
            from follows f join users u on f.following_id = u.id
            where f.follower_id = #{id} 
            order by f.created_at desc
            limit #{pageSize} offset #{offset}
            """)
    List<UserProfile> selectFollowingList(@Param("id")Long id,
                                          @Param("pageSize")int pageSize,
                                           @Param("offset")int offset );

    /**
     * 页查询某用户的粉丝列表，按用户创建时间倒序
     */
    @Select("""
            select u.id,u.username,u.avatar_url,u.bio
            from follows f join users u on f.follower_id = u.id
            where f.following_id = #{id} 
            order by u.created_at desc
            limit #{pageSize} offset #{offset}
            """)
    List<UserProfile> selectFollowerList(@Param("id")Long id,
                                          @Param("pageSize")int pageSize,
                                           @Param("offset")int offset);
}
