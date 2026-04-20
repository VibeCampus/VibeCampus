package cn.ayeez.vibecampus.user.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码哈希生成工具类（仅用于开发/测试环境）
 * 运行 main 方法即可生成 BCrypt 哈希值
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 测试密码
        String plainPassword = "Test@123456";
        String hash = encoder.encode(plainPassword);
        
        System.out.println("===========================================");
        System.out.println("明文密码: " + plainPassword);
        System.out.println("BCrypt 哈希值: " + hash);
        System.out.println("哈希值长度: " + hash.length());
        System.out.println("===========================================");
        System.out.println();
        System.out.println("SQL 更新语句:");
        System.out.println("UPDATE users SET password_hash = '" + hash + "' WHERE username = 'testuser';");
        System.out.println("===========================================");
        
        // 验证生成的哈希值
        boolean matches = encoder.matches(plainPassword, hash);
        System.out.println("验证结果: " + (matches ? "✓ 成功" : "✗ 失败"));
    }
}
