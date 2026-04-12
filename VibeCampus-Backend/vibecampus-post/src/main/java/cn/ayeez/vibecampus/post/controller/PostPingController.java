package cn.ayeez.vibecampus.post.controller;

import cn.ayeez.vibecampus.post.service.PostPingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/post")
public class PostPingController {

    private final PostPingService postPingService;

    public PostPingController(PostPingService postPingService) {
        this.postPingService = postPingService;
    }

    @GetMapping("/ping")
    public String ping() {
        return postPingService.ping();
    }
}
