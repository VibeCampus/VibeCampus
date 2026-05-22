package cn.ayeez.vibecampus.post.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地上传文件静态资源映射：
 * 将磁盘目录 uploads 映射到 URL /uploads/**，便于前端直接预览媒体。
 * TODO(ayeez): 切换到图床/对象存储后可删除该本地静态映射，改为直接返回图床公网 URL。
 */
@Configuration
public class PostUploadResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadRoot + "/");
    }
}
