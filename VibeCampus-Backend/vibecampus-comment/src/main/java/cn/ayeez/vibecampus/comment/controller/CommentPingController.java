package cn.ayeez.vibecampus.comment.controller;

import cn.ayeez.vibecampus.comment.service.CommentPingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comment")
public class CommentPingController {

    private final CommentPingService commentPingService;

    public CommentPingController(CommentPingService commentPingService) {
        this.commentPingService = commentPingService;
    }

    @GetMapping("/ping")
    public String ping() {
        return commentPingService.ping();
    }
}
