package com.medshop.user.controller;

import com.medshop.common.Result;
import com.medshop.common.UserContext;
import com.medshop.user.dto.UserProfileDTO;
import com.medshop.user.service.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户个人中心控制层。任意登录用户均可管理自己的资料。
 * <ul>
 *   <li>GET  /api/v1/users/me            当前用户资料</li>
 *   <li>PUT  /api/v1/users/me            修改昵称</li>
 *   <li>PUT  /api/v1/users/me/password   修改密码</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserProfileDTO> me() {
        Long userId = UserContext.requireUserId();
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public Result<Void> updateProfile(@RequestBody NicknameReq req) {
        Long userId = UserContext.requireUserId();
        userService.updateNickname(userId, req.nickname);
        return Result.success();
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@RequestBody PasswordReq req) {
        Long userId = UserContext.requireUserId();
        userService.changePassword(userId, req.oldPassword, req.newPassword);
        return Result.success();
    }

    public static class NicknameReq {
        public String nickname;
    }

    public static class PasswordReq {
        @NotBlank(message = "原密码不能为空")
        public String oldPassword;
        @NotBlank(message = "新密码不能为空")
        public String newPassword;
    }
}
