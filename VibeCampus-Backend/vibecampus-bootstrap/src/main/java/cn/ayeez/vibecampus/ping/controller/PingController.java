package cn.ayeez.vibecampus.ping.controller;

import cn.ayeez.vibecampus.ping.dto.PingResponse;
import cn.ayeez.vibecampus.ping.service.PingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author ayeez
 */
@RestController
@RequestMapping("/api")
public class PingController {

    private final PingService pingService;

    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return pingService.ping();
    }
}

