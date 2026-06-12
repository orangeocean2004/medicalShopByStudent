package com.medshop.user.mapper;

import com.medshop.user.entity.PrivacyAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 隐私授权数据访问层（C-3）。SQL 见 mapper/PrivacyMapper.xml。
 */
@Mapper
public interface PrivacyMapper {

    /** 查询某用户某授权范围的现有记录。 */
    PrivacyAuthorization findByUserAndScope(@Param("userId") Long userId, @Param("scope") String scope);

    int insert(PrivacyAuthorization auth);

    /** 更新授权状态与撤回时间。 */
    int updateStatus(PrivacyAuthorization auth);
}
