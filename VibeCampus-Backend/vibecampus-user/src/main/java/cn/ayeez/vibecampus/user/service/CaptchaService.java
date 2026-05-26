package cn.ayeez.vibecampus.user.service;

public interface CaptchaService {

    void save(String captchaId, String answer);

    void verify(String captchaId, String answer);
}
