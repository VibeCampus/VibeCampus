package cn.ayeez.vibecampus.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @Size(min = 3, max = 32, message = "用户名长度必须在3-32个字符之间")
    private String username;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String gender;

    @Size(max = 255, message = "个人简介不能超过255个字符")
    private String bio;
}
