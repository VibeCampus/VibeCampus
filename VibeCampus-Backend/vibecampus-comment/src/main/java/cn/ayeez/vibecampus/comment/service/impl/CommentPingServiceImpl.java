package cn.ayeez.vibecampus.comment.service.impl;

import cn.ayeez.vibecampus.comment.service.CommentPingService;
import org.springframework.stereotype.Service;

@Service
public class CommentPingServiceImpl implements CommentPingService {

    @Override
    public String ping() {
        return "comment";
    }
}
