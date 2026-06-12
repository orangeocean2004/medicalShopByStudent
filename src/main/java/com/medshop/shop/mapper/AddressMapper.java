package com.medshop.shop.mapper;

import com.medshop.shop.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收货地址数据访问层。CRUD SQL 见 mapper/AddressMapper.xml，
 * 归属/计数校验保留为内联注解。
 */
@Mapper
public interface AddressMapper {

    /** 统计某地址是否属于该用户，用于下单时归属校验。 */
    @Select("SELECT COUNT(*) FROM user_address WHERE id = #{addressId} AND user_id = #{userId}")
    int countByIdAndUser(@Param("addressId") Long addressId, @Param("userId") Long userId);

    /** 统计某用户地址总数（用于首条自动设默认）。 */
    @Select("SELECT COUNT(*) FROM user_address WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);

    /** 某用户的全部地址（默认地址优先，其次按更新时间倒序）。 */
    List<Address> selectByUser(@Param("userId") Long userId);

    /** 按 id + 归属用户查（编辑/删除前校验所有权）。 */
    Address selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /** 新增地址，回填自增主键。 */
    int insert(Address address);

    /** 更新地址（仅限本人，WHERE 带 user_id）。 */
    int update(Address address);

    /** 删除地址（仅限本人）。 */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /** 清除某用户全部默认标记（设默认前调用）。 */
    int clearDefault(@Param("userId") Long userId);

    /** 将某地址设为默认（仅限本人）。 */
    int setDefault(@Param("id") Long id, @Param("userId") Long userId);
}
