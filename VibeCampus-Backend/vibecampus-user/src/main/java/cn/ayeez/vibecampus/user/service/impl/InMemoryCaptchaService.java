package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.user.service.CaptchaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryCaptchaService implements CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private final Map<String, CaptchaEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void save(String captchaId, String answer) {
        if (captchaId == null || captchaId.isBlank() || answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "验证码生成失败");
        }
        cleanupExpired();
        entries.put(captchaId, new CaptchaEntry(normalize(answer), Instant.now().plus(TTL)));
    }

    @Override
    public void verify(String captchaId, String answer) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "captchaId不能为空");
        }
        if (answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不能为空");
        }
        CaptchaEntry entry = entries.remove(captchaId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已过期，请刷新后重试");
        }
        if (!entry.answer().equals(normalize(answer))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误");
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String normalize(String answer) {
        return answer.trim().toUpperCase(Locale.ROOT);
    }

    private record CaptchaEntry(String answer, Instant expiresAt) {
    }
}
