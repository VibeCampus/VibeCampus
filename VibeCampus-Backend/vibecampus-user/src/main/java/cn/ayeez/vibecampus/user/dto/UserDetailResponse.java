package cn.ayeez.vibecampus.user.dto;


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
    // 用户邮箱（仅自己可见）
    private String email;
    // 用户手机号（仅自己可见）
    private String phone;
    // 性别：0-保密，1-男，2-女，3-其他
    private Integer gender;
    // 用户状态：0-禁用，1-正常
    private Integer status;
    // 标识是否为用户本人
    private Boolean isCurrentUser;
}
