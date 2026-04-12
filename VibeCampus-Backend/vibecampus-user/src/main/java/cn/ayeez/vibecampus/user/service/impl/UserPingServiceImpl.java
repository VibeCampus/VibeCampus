package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.user.service.UserPingService;
import org.springframework.stereotype.Service;

@Service
public class UserPingServiceImpl implements UserPingService {

    @Override
    public String ping() {
        return "user";
    }
}
