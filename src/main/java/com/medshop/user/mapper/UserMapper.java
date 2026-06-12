package com.medshop.user.mapper;

import com.medshop.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层。SQL 见 mapper/UserMapper.xml。
 */
@Mapper
public interface UserMapper {

    /** 按手机号查用户（登录、注册查重）。 */
    User findByPhone(@Param("phone") String phone);

    /** 按ID查用户。 */
    User findById(@Param("id") Long id);

    /** 插入用户，回填自增主键。 */
    int insert(User user);

    /** 按角色取一个可用坐席（药师/客服），用于人工兜底分配。 */
    User findOneByRole(@Param("role") Integer role);

    /** 刷新用户最近活跃时间为当前时刻（坐席在线心跳）。 */
    int touchActive(@Param("id") Long id);

    /**
     * 统计某角色「在线」坐席数：最近活跃时间晚于 since 的视为在线。
     * @param since 在线时间下界字符串，格式须与 SQLite CURRENT_TIMESTAMP 一致（'yyyy-MM-dd HH:mm:ss'，UTC）
     */
    int countOnlineByRole(@Param("role") Integer role, @Param("since") String since);

    /** 取一个在线坐席（最近活跃优先），无在线坐席返回 null。since 格式同上。 */
    User findOnlineByRole(@Param("role") Integer role, @Param("since") String since);

    /** 更新昵称。 */
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /** 更新密码哈希。 */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 全部用户（人员管理列表，倒序）。 */
    List<User> selectAll();

    /** 按角色统计用户数。 */
    int countByRole(@Param("role") Integer role);
}
