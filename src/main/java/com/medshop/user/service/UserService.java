package com.medshop.user.service;

import com.medshop.common.BizException;
import com.medshop.common.security.JwtUtil;
import com.medshop.user.dto.LoginDTO;
import com.medshop.user.dto.LoginResultDTO;
import com.medshop.user.dto.UserProfileDTO;
import com.medshop.user.entity.User;
import com.medshop.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 用户业务层（P3）：注册、登录、查询。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册：手机号查重 → 密码 BCrypt 哈希 → 入库 → 直接签发令牌返回。
     */
    public LoginResultDTO register(LoginDTO dto) {
        if (userMapper.findByPhone(dto.getPhone()) != null) {
            throw new BizException("该手机号已注册");
        }
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : "用户" + dto.getPhone());
        user.setRole(0); // 注册默认消费者
        userMapper.insert(user);

        String token = jwtUtil.issue(user.getId(), user.getRole());
        return new LoginResultDTO(token, user.getId(), user.getRole());
    }

    /**
     * 登录：按手机号取用户 → 校验密码 → 签发令牌。
     */
    public LoginResultDTO login(LoginDTO dto) {
        User user = userMapper.findByPhone(dto.getPhone());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BizException("手机号或密码错误");
        }
        String token = jwtUtil.issue(user.getId(), user.getRole());
        return new LoginResultDTO(token, user.getId(), user.getRole());
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] ROLE_NAMES = {"消费者", "执业药师", "客服", "运营"};

    /** 取当前用户资料（个人中心展示）。 */
    public UserProfileDTO getProfile(Long userId) {
        User u = userMapper.findById(userId);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(u.getId());
        dto.setPhone(u.getPhone());
        dto.setNickname(u.getNickname());
        dto.setRole(u.getRole());
        dto.setRoleName(u.getRole() != null && u.getRole() >= 0 && u.getRole() < ROLE_NAMES.length
                ? ROLE_NAMES[u.getRole()] : "用户");
        dto.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : null);
        return dto;
    }

    /** 修改昵称。 */
    public void updateNickname(Long userId, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BizException("昵称不能为空");
        }
        if (nickname.trim().length() > 20) {
            throw new BizException("昵称不要超过 20 个字");
        }
        userMapper.updateNickname(userId, nickname.trim());
    }

    /** 修改密码：校验原密码 → 写入新密码哈希。 */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        User u = userMapper.findById(userId);
        if (u == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, u.getPasswordHash())) {
            throw new BizException("原密码不正确");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    /** 全部用户列表（运营人员管理），转为不含密码的 DTO。 */
    public java.util.List<UserProfileDTO> listAllUsers() {
        return userMapper.selectAll().stream().map(u -> {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(u.getId());
            dto.setPhone(u.getPhone());
            dto.setNickname(u.getNickname());
            dto.setRole(u.getRole());
            dto.setRoleName(u.getRole() != null && u.getRole() >= 0 && u.getRole() < ROLE_NAMES.length
                    ? ROLE_NAMES[u.getRole()] : "用户");
            dto.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : null);
            return dto;
        }).toList();
    }

    /**
     * 运营创建员工账号（药师/客服/运营）。手机号查重 → BCrypt → 指定角色入库。
     * @param role 仅允许 1药师 / 2客服 / 3运营，不允许创建消费者(走注册)
     */
    public UserProfileDTO createStaff(String phone, String password, String nickname, Integer role) {
        if (phone == null || !phone.matches("\\d{11}")) {
            throw new BizException("请填写 11 位手机号");
        }
        if (password == null || password.length() < 6) {
            throw new BizException("初始密码至少 6 位");
        }
        if (role == null || role < 1 || role > 3) {
            throw new BizException("员工角色只能是药师、客服或运营");
        }
        if (userMapper.findByPhone(phone) != null) {
            throw new BizException("该手机号已被注册");
        }
        User u = new User();
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setNickname(nickname != null && !nickname.trim().isEmpty()
                ? nickname.trim() : ROLE_NAMES[role] + phone.substring(7));
        u.setRole(role);
        userMapper.insert(u);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(u.getId());
        dto.setPhone(u.getPhone());
        dto.setNickname(u.getNickname());
        dto.setRole(u.getRole());
        dto.setRoleName(ROLE_NAMES[role]);
        return dto;
    }
}
