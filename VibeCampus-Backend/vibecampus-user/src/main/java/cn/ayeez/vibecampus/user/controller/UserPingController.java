package cn.ayeez.vibecampus.user.controller;

import cn.ayeez.vibecampus.user.service.UserPingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserPingController {

    private final UserPingService userPingService;

    public UserPingController(UserPingService userPingService) {
        this.userPingService = userPingService;
    }

    @GetMapping("/ping")
    public String ping() {
        return userPingService.ping();
    }
}
