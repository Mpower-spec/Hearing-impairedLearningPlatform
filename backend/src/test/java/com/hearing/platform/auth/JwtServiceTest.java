package com.hearing.platform.auth;

import com.hearing.platform.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void createsAndParsesToken() {
        AppProperties properties = new AppProperties();
        properties.setJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setJwtExpirationHours(1);
        JwtService service = new JwtService(properties);

        String token = service.createToken(new AuthUser(7L, "student", "演示学生", Roles.STUDENT));
        AuthUser parsed = service.parseToken(token);

        assertThat(parsed.id()).isEqualTo(7L);
        assertThat(parsed.username()).isEqualTo("student");
        assertThat(parsed.role()).isEqualTo(Roles.STUDENT);
    }
}