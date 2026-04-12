package cn.ayeez.vibecampus.admin.controller;

import cn.ayeez.vibecampus.admin.service.AdminPingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPingController {

    private final AdminPingService adminPingService;

    public AdminPingController(AdminPingService adminPingService) {
        this.adminPingService = adminPingService;
    }

    @GetMapping("/ping")
    public String ping() {
        return adminPingService.ping();
    }
}
