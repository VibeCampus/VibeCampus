package cn.ayeez.vibecampus.user.controller;


import cn.ayeez.vibecampus.common.dto.UserDetailResponse;
import cn.ayeez.vibecampus.user.dto.FollowPageResponse;
import cn.ayeez.vibecampus.user.service.FollowService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 关注关系控制器。
 *
 * <h3>支持的路径：</h3>
 * <ul>
 *   <li>POST /api/users/{id}/follow - 关注用户</li>
 *   <li>DELETE /api/users/{id}/follow - 取消关注</li>
 *   <li>GET /api/users/{id}/following - 关注列表</li>
 *   <li>GET /api/users/{id}/followers - 粉丝列表</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService){
        this.followService = followService;
    }

    /**
     * 关注用户
     * @param id 用户ID
     * @param request HTTP请求
     * @return 关注结果
     */
    @PostMapping("/users/{id}/follow")
    public ResponseEntity<?> follow(@PathVariable Long id, HttpServletRequest request){
        // 从 JWT 认证过滤器注入的属性中获取当前用户ID
        Long currentUserId = (Long)request.getAttribute("currentUserId");

        log.info("收到当前用户查询请求，currentUserId={}", currentUserId);

        // 参数校验：理论上不应该为null，因为SecurityConfig要求认证
        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }



        //成功返回
        return ResponseEntity.ok(followService.follow(currentUserId,id));
    }

    /**
     * 取消关注用户
     * @param id 用户ID
     * @param request HTTP请求
     * @return 取消关注结果
     */
    @DeleteMapping("/users/{id}/follow")
    public ResponseEntity<?> unfollow(@PathVariable Long id,HttpServletRequest request){

        Long currentUserId = (Long)request.getAttribute("currentUserId");

        log.info("收到当前用户查询请求，currentUserId={}", currentUserId);

        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }

        return ResponseEntity.ok(followService.unfollow(currentUserId,id));
    }

    /**
     * 分页查询某用户的关注列表，按关注时间倒序 。
     * @param id 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
         */
    @GetMapping("/users/{userId}/following")
    public ResponseEntity<?> getFollowingList(@PathVariable("userId")Long id,
                                        @RequestParam(value = "page",defaultValue = "1")int page,
                                        @RequestParam(value = "pageSize",defaultValue = "20")Integer pageSize,
                                        HttpServletRequest request ){
        Long currentUserId = (Long)request.getAttribute("currentUserId");

        log.info("收到当前用户查询请求，currentUserId={}", currentUserId);

        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }

        return ResponseEntity.ok(followService.getFollowingList(currentUserId,id,page, pageSize));
    }


    /**
     * 分页查询某用户的粉丝列表，按粉丝加入时间倒序     
     * @param id 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<?> getFollowerList(@PathVariable("userId")Long id,
                                      @RequestParam(value = "page",defaultValue = "1")int page,
                                      @RequestParam(value = "pageSize",defaultValue = "20")Integer pageSize,
                                      HttpServletRequest request){
        Long currentUserId = (Long)request.getAttribute("currentUserId");

        log.info("收到当前用户查询请求，currentUserId={}", currentUserId);

        if (currentUserId == null || currentUserId <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "未登录或无效的会话");
            return ResponseEntity.status(401).body(error);
        }

        return ResponseEntity.ok(followService.getFollowerList(currentUserId, id, page, pageSize));
    }

}
