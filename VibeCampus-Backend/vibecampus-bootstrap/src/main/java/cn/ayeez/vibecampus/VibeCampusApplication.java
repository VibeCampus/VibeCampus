package cn.ayeez.vibecampus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.ayeez.vibecampus")
@MapperScan(basePackages = {
        "cn.ayeez.vibecampus.user.mapper",
        "cn.ayeez.vibecampus.post.mapper",
        "cn.ayeez.vibecampus.comment.mapper",
        "cn.ayeez.vibecampus.admin.mapper"
})
public class VibeCampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeCampusApplication.class, args);
    }
}
