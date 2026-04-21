package cn.ayeez.vibecampus.ping.service;

import cn.ayeez.vibecampus.ping.dto.PingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PingService {

    private static final String DEPLOY_MARKER = "cicd-check-20260421";
    private final String applicationName;

    public PingService(@Value("${spring.application.name:VibeCampus-Backend}") String applicationName) {
        this.applicationName = applicationName;
    }

    public PingResponse ping() {
        return new PingResponse(applicationName, "OK-" + DEPLOY_MARKER, Instant.now());
    }
}

