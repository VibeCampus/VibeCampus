package cn.ayeez.vibecampus.common.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户详细信息响应DTO
 * <p>用于返回用户的完整或部分信息，根据查看者身份动态调整字段</p>
 * <ul>
 *   <li>查看自己的信息：返回所有字段（包括邮箱、手机号等敏感信息）</li>
 *   <li>查看他人的信息：仅返回公开字段（昵称、用户名、ID等），隐藏敏感信息</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

    private Long id;

    private String username;

    private String nickname;
    private String avatar;
    // 用户邮箱（仅自己可见）
    private String email;
    // 用户手机号（仅自己可见）
    private String phone;
    // 性别：男 / 女 / 保密 / 其他
    private String gender;
    private String bio;
    private String major;
    private String joinedAt;
    // 用户状态：0-禁用，1-正常
    private Integer status;
    // 标识是否为用户本人
    private Boolean isCurrentUser;
    //是否用户是否关注此人
    private Boolean following;
    //关注数
    private Long followingCount;
    //粉丝数
    private Long followerCount;
    //发布数
    private Long postCount;
    //收藏数
    private Long favoriteCount;
}
