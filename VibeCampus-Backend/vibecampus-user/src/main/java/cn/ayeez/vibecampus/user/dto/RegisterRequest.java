package cn.ayeez.vibecampus.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

    /**
     * 用户注册请求体：包含注册所需的基本信息
     * <p>使用 Bean Validation 进行字段校验，确保数据完整性</p>
     */
    @Data
    public class RegisterRequest {

        /**
         * 用户名：3-32个字符，用于登录和展示
         * - 不能为空
         * - 长度限制在3到32个字符之间
         */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度必须在3-32个字符之间")
        private String username;

        /**
         * 密码：6-64个字符，将进行BCrypt哈希加密存储
         * - 不能为空
         * - 长度限制在6到64个字符之间（BCrypt限制最大72字节）
         */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度必须在6-64个字符之间")
        private String password;

        /**
         * 手机号：可选，如果提供则必须符合中国大陆手机号格式
         * - 用于找回密码或接收通知
         * - 数据库中设置为唯一约束
         */
        private String phone;

        /**
         * 邮箱：可选，如果提供则必须符合邮箱格式
         * - 用于找回密码或接收通知
         * - 数据库中设置为唯一约束
         */
        @Email(message = "邮箱格式不正确")
        private String email;

        /**
         * 昵称：可选，用户的显示名称
         * - 如果不提供，默认使用用户名
         * - 最大64个字符
         */
        @Size(max = 64, message = "昵称长度不能超过64个字符")
        private String nickname;
}
