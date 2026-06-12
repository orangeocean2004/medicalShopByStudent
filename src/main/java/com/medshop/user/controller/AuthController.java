package com.medshop.user.controller;

import com.medshop.common.Result;
import com.medshop.user.dto.LoginDTO;
import com.medshop.user.dto.LoginResultDTO;
import com.medshop.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制层（P3）。
 * <ul>
 *   <li>API-01 用户登录：POST /api/v1/auth/login</li>
 *   <li>用户注册：POST /api/v1/auth/register</li>
 * </ul>
 * 这两个接口在 WebConfig 中被排除鉴权，可匿名访问。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<LoginResultDTO> register(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.register(dto));
    }
}
