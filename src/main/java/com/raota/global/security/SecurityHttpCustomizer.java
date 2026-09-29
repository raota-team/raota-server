package com.raota.global.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface SecurityHttpCustomizer {

    void customize(HttpSecurity http) throws Exception;

}
