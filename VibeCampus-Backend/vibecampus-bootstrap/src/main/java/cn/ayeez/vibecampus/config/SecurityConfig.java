package cn.ayeez.vibecampus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;


/**
 * @author ayeez
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtRevocationFilter jwtRevocationFilter;

    public SecurityConfig(JwtRevocationFilter jwtRevocationFilter) {
        this.jwtRevocationFilter = jwtRevocationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .addFilterBefore(jwtRevocationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
