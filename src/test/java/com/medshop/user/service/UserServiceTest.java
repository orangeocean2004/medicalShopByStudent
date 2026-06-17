package com.medshop.user.service;

import com.medshop.common.BizException;
import com.medshop.common.security.JwtUtil;
import com.medshop.user.dto.LoginDTO;
import com.medshop.user.dto.LoginResultDTO;
import com.medshop.user.entity.User;
import com.medshop.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试（P3 用户与权限）：注册查重、登录密码校验、
 * 改昵称/改密码边界、运营建员工的角色与手机号校验。
 * UserMapper / JwtUtil 用 Mockito 桩，PasswordEncoder 用真实 BCrypt 验证哈希往返。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock JwtUtil jwtUtil;
    // 用真实 BCrypt，保证 encode/matches 行为可信
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordEncoder, jwtUtil);
    }

    private LoginDTO dto(String phone, String pwd, String nick) {
        LoginDTO d = new LoginDTO();
        d.setPhone(phone);
        d.setPassword(pwd);
        d.setNickname(nick);
        return d;
    }

    // ---------- 注册 ----------

    @Test
    @DisplayName("注册成功：手机号未占用，密码被BCrypt哈希，默认角色为消费者(0)并签发令牌")
    void register_success() {
        when(userMapper.findByPhone("13800000000")).thenReturn(null);
        when(jwtUtil.issue(any(), eq(0))).thenReturn("TKN");

        LoginResultDTO r = userService.register(dto("13800000000", "123456", "小明"));

        assertEquals("TKN", r.getToken());
        assertEquals(0, r.getRole());
        // 断言入库的 User：角色0、昵称透传、密码为哈希而非明文
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(cap.capture());
        User saved = cap.getValue();
        assertEquals(0, saved.getRole());
        assertEquals("小明", saved.getNickname());
        assertNotEquals("123456", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("123456", saved.getPasswordHash()));
    }

    @Test
    @DisplayName("注册：未填昵称时用「用户+手机号」兜底")
    void register_defaultNickname() {
        when(userMapper.findByPhone(anyString())).thenReturn(null);
        when(jwtUtil.issue(any(), any())).thenReturn("TKN");

        userService.register(dto("13812345678", "abcdef", null));

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(cap.capture());
        assertEquals("用户13812345678", cap.getValue().getNickname());
    }

    @Test
    @DisplayName("注册：手机号已存在则抛业务异常，不入库")
    void register_duplicatePhone_throws() {
        when(userMapper.findByPhone("13800000000")).thenReturn(new User());

        BizException ex = assertThrows(BizException.class,
                () -> userService.register(dto("13800000000", "123456", "x")));
        assertTrue(ex.getMessage().contains("已注册"));
        verify(userMapper, never()).insert(any());
        verify(jwtUtil, never()).issue(any(), any());
    }

    // ---------- 登录 ----------

    @Test
    @DisplayName("登录成功：密码匹配则签发携带角色的令牌")
    void login_success() {
        User u = new User();
        u.setId(5L);
        u.setRole(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        when(userMapper.findByPhone("13800000001")).thenReturn(u);
        when(jwtUtil.issue(5L, 1)).thenReturn("PHARM_TKN");

        LoginResultDTO r = userService.login(dto("13800000001", "123456", null));

        assertEquals("PHARM_TKN", r.getToken());
        assertEquals(5L, r.getUserId());
        assertEquals(1, r.getRole());
    }

    @Test
    @DisplayName("登录：用户不存在抛「手机号或密码错误」")
    void login_userNotFound_throws() {
        when(userMapper.findByPhone(anyString())).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> userService.login(dto("13800000000", "123456", null)));
        assertTrue(ex.getMessage().contains("错误"));
        verify(jwtUtil, never()).issue(any(), any());
    }

    @Test
    @DisplayName("登录：密码不匹配抛「手机号或密码错误」")
    void login_wrongPassword_throws() {
        User u = new User();
        u.setId(1L);
        u.setRole(0);
        u.setPasswordHash(passwordEncoder.encode("correct-pwd"));
        when(userMapper.findByPhone("13800000000")).thenReturn(u);

        assertThrows(BizException.class,
                () -> userService.login(dto("13800000000", "wrong-pwd", null)));
        verify(jwtUtil, never()).issue(any(), any());
    }

    // ---------- 个人资料 / 改昵称 ----------

    @Test
    @DisplayName("查资料：角色码映射为角色名，不含密码哈希字段")
    void getProfile_mapsRoleName() {
        User u = new User();
        u.setId(9L);
        u.setPhone("13800000099");
        u.setNickname("运营小李");
        u.setRole(3);
        u.setPasswordHash("hash");
        when(userMapper.findById(9L)).thenReturn(u);

        var p = userService.getProfile(9L);
        assertEquals("运营", p.getRoleName());
        assertEquals(3, p.getRole());
        assertEquals("13800000099", p.getPhone());
    }

    @Test
    @DisplayName("查资料：用户不存在抛异常")
    void getProfile_notFound_throws() {
        when(userMapper.findById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> userService.getProfile(404L));
    }

    @Test
    @DisplayName("改昵称：正常去除首尾空格后入库")
    void updateNickname_trims() {
        userService.updateNickname(1L, "  新昵称  ");
        verify(userMapper).updateNickname(1L, "新昵称");
    }

    @Test
    @DisplayName("改昵称：空白抛异常")
    void updateNickname_blank_throws() {
        assertThrows(BizException.class, () -> userService.updateNickname(1L, "   "));
        verify(userMapper, never()).updateNickname(anyLong(), anyString());
    }

    @Test
    @DisplayName("改昵称：超过20字抛异常")
    void updateNickname_tooLong_throws() {
        String longName = "字".repeat(21);
        assertThrows(BizException.class, () -> userService.updateNickname(1L, longName));
        verify(userMapper, never()).updateNickname(anyLong(), anyString());
    }

    // ---------- 改密码 ----------

    @Test
    @DisplayName("改密码：原密码正确则写入新密码哈希")
    void changePassword_success() {
        User u = new User();
        u.setId(1L);
        u.setPasswordHash(passwordEncoder.encode("oldpwd"));
        when(userMapper.findById(1L)).thenReturn(u);

        userService.changePassword(1L, "oldpwd", "newpwd123");

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updatePassword(eq(1L), cap.capture());
        assertTrue(passwordEncoder.matches("newpwd123", cap.getValue()));
    }

    @Test
    @DisplayName("改密码：新密码不足6位抛异常（先于查库）")
    void changePassword_tooShort_throws() {
        assertThrows(BizException.class, () -> userService.changePassword(1L, "old", "123"));
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("改密码：原密码不正确抛异常")
    void changePassword_wrongOld_throws() {
        User u = new User();
        u.setId(1L);
        u.setPasswordHash(passwordEncoder.encode("realold"));
        when(userMapper.findById(1L)).thenReturn(u);

        BizException ex = assertThrows(BizException.class,
                () -> userService.changePassword(1L, "guess", "newpwd123"));
        assertTrue(ex.getMessage().contains("原密码"));
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    // ---------- 运营建员工 ----------

    @Test
    @DisplayName("建员工：合法药师(role=1)创建成功，密码哈希入库")
    void createStaff_success() {
        when(userMapper.findByPhone("13900000000")).thenReturn(null);
        var dto = userService.createStaff("13900000000", "init123", "王药师", 1);

        assertEquals(1, dto.getRole());
        assertEquals("执业药师", dto.getRoleName());
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(cap.capture());
        assertTrue(passwordEncoder.matches("init123", cap.getValue().getPasswordHash()));
    }

    @Test
    @DisplayName("建员工：手机号非11位抛异常")
    void createStaff_badPhone_throws() {
        assertThrows(BizException.class, () -> userService.createStaff("139", "init123", "x", 1));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("建员工：角色超出1~3（如0消费者/4）抛异常")
    void createStaff_illegalRole_throws() {
        assertThrows(BizException.class, () -> userService.createStaff("13900000000", "init123", "x", 0));
        assertThrows(BizException.class, () -> userService.createStaff("13900000000", "init123", "x", 4));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("建员工：初始密码不足6位抛异常")
    void createStaff_shortPwd_throws() {
        assertThrows(BizException.class, () -> userService.createStaff("13900000000", "123", "x", 2));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("建员工：手机号已被占用抛异常")
    void createStaff_duplicatePhone_throws() {
        when(userMapper.findByPhone("13900000000")).thenReturn(new User());
        assertThrows(BizException.class, () -> userService.createStaff("13900000000", "init123", "x", 1));
        verify(userMapper, never()).insert(any());
    }
}
