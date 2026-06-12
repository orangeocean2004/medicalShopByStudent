package com.medshop.common;

import com.medshop.user.entity.User;
import com.medshop.user.mapper.UserMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 演示账号初始化器。
 *
 * <p>药品、知识库等静态数据由 data.sql 灌入；但用户密码需用与登录校验一致的
 * {@link PasswordEncoder} 生成 BCrypt 哈希，故在程序启动时写入，保证「解压即可登录」。
 * 已存在同手机号则跳过（幂等）。
 *
 * <p>演示账号（密码均为 123456）：
 * <ul>
 *   <li>13800000000 —— 消费者（李阿姨）</li>
 *   <li>13800000001 —— 执业药师（转人工接单方 / 药师工作台）</li>
 *   <li>13800000099 —— 运营管理员（管理后台）</li>
 * </ul>
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("13800000000", "李阿姨", 0);   // 消费者
        seedUser("13800000001", "张药师", 1);   // 执业药师
        seedUser("13800000099", "运营管理员", 3); // 运营（管理后台）
    }

    private void seedUser(String phone, String nickname, int role) {
        if (userMapper.findByPhone(phone) != null) {
            return; // 已存在，幂等跳过
        }
        User u = new User();
        u.setPhone(phone);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        u.setNickname(nickname);
        u.setRole(role);
        userMapper.insert(u);
    }
}
