package cn.ayeez.vibecampus.post.service.impl;

import cn.ayeez.vibecampus.post.service.PostPingService;
import org.springframework.stereotype.Service;

@Service
public class PostPingServiceImpl implements PostPingService {

    @Override
    public String ping() {
        return "post";
    }
}
