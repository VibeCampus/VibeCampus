package cn.ayeez.vibecampus.admin.service.impl;

import cn.ayeez.vibecampus.admin.service.AdminPingService;
import org.springframework.stereotype.Service;

@Service
public class AdminPingServiceImpl implements AdminPingService {

    @Override
    public String ping() {
        return "admin";
    }
}
