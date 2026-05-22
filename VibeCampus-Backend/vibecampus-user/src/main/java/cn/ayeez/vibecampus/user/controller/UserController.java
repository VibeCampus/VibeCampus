package cn.ayeez.vibecampus.user.controller;


import cn.ayeez.vibecampus.common.dto.ChangePasswordRequest;
import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.common.dto.UserProfileUpdateRequest;
import cn.ayeez.vibecampus.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 用户信息查询控制器（统一接口）
 * <p>提供用户个人信息查询功能，自动根据查看者身份返回不同详细程度的信息</p>
 * <ul>
 *   <li>查看自己：返回完整信息（包括邮箱、手机号等敏感字段）</li>
 *   <li>查看他人：仅返回公开信息（昵称、用户名、ID等）</li>
 * </ul>
 * 
 * <h3>支持的路径：</h3>
 * <ul>
 *   <li>GET /api/user/me - 查看当前登录用户信息</li>
 *   <li>PUT /api/user/me - 更新当前登录用户信息</li>
 *   <li>POST /api/user/me/avatar - 上传头像</li>
 *   <li>PUT /api/user/me/password - 修改密码</li>
 *   <li>GET /api/users/{userId} - 查看指定用户信息</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {
    private static final Path AVATAR_UPLOAD_BASE_DIR = Paths.get("uploads", "avatars");
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询当前登录用户的详细信息
     * <p>路径：GET /api/user/me</p>
     * <p>返回当前用户的完整信息（包括敏感字段）</p>
     *
     * @param request HTTP请求对象，用于从属性中获取当前登录用户ID
     * @return 当前用户的完整详细信息
     */
    @GetMapping({"/user/me", "/users/me"})
    public ResponseEntity<?> getCurrentUserDetail(HttpServletRequest request) {
        // 从 JWT 认证过滤器注入的属性中获取当前用户ID
        Long currentUserId = (Long) request.getAttribute("currentUserId");

        log.info("收到当前用户查询请求，currentUserId={}", currentUserId);

        // 参数校验：理论上不应该为null，因为SecurityConfig要求认证
        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }

        // 调用服务层查询用户详细（自己查看自己，返回完整信息）
        UserDetailResponse userDetail = userService.getUserDetail(currentUserId, currentUserId);

        // 用户不存在，返回404（理论上不应该发生，因为用户已登录）
        if (userDetail == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户不存在");
            return ResponseEntity.status(404).body(error);
        }

        // 返回当前用户的完整信息
        return ResponseEntity.ok(userDetail);
    }

    @PutMapping({"/user/me", "/users/me"})
    public ResponseEntity<?> updateCurrentUser(
            @Valid @RequestBody UserProfileUpdateRequest requestBody,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }
        return ResponseEntity.ok(userService.updateCurrentUser(currentUserId, requestBody));
    }

    @PostMapping({"/user/me/avatar", "/users/me/avatar"})
    public ResponseEntity<?> uploadAvatar(
            @RequestPart("avatar") MultipartFile avatar,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }
        String avatarUrl = userService.updateAvatar(currentUserId, saveAvatar(avatar));
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @PutMapping({"/user/me/password", "/users/me/password"})
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest requestBody,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }
        userService.changePassword(currentUserId, requestBody.getOldPassword(), requestBody.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * 查询指定用户的详细信息（统一接口）
     * <p>路径：GET /api/users/{userId}</p>
     * <p>根据当前登录用户与目标用户的关系，自动返回不同详细程度的信息</p>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>个人中心：传入自己的userId，获取完整信息</li>
     *   <li>查看他人主页：传入他人userId，获取公开信息</li>
     * </ul>
     *
     * <h3>权限说明：</h3>
     * <ul>
     *   <li>需要携带有效的JWT token</li>
     *   <li>查看自己：返回完整信息（包括邮箱、手机号等敏感字段）</li>
     *   <li>查看他人：仅返回公开信息（昵称、用户名、ID等）</li>
     * </ul>
     *
     * @param userId 目标用户ID（路径参数）
     * @param request HTTP请求对象，用于从属性中获取当前登录用户ID
     * @return 用户详细信息，包含isCurrentUser标识
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(
            @PathVariable Long userId,
            HttpServletRequest request) {

        // 从 JWT 认证过滤器注入的属性中获取当前用户ID
        Long currentUserId = (Long) request.getAttribute("currentUserId");

        log.info("收到用户查询请求，targetUserId={}, currentUserId={}", userId, currentUserId);

        // 参数校验
        if (userId == null || userId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "无效的用户ID");
            return ResponseEntity.badRequest().body(error);
        }

        // 检查当前用户是否已认证
        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }

        // 调用服务层查询用户详细
        UserDetailResponse userDetail = userService.getUserDetail(userId, currentUserId);

        // 用户不存在，返回404
        if (userDetail == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "用户不存在");
            return ResponseEntity.status(404).body(error);
        }

        // 返回用户详细信息，包含isCurrentUser标识
        return ResponseEntity.ok(userDetail);
    }

    private String saveAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像文件不能为空");
        }
        if (avatar.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像文件不能超过5MB");
        }
        String contentType = avatar.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像仅支持jpg/png/webp/gif");
        }
        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        try {
            LocalDate now = LocalDate.now();
            Path relativeDirectory = Paths.get(String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()));
            Path targetDirectory = AVATAR_UPLOAD_BASE_DIR.resolve(relativeDirectory);
            Files.createDirectories(targetDirectory);
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetPath = targetDirectory.resolve(fileName);
            try (InputStream inputStream = avatar.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/avatars/" + relativeDirectory.toString().replace("\\", "/") + "/" + fileName;
        }
        catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "头像保存失败，请稍后重试");
        }
    }
}
