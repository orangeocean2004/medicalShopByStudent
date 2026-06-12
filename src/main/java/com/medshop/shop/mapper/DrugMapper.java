package com.medshop.shop.mapper;

import com.medshop.shop.entity.Drug;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 药品数据访问层。SQL 见 mapper/DrugMapper.xml。
 */
@Mapper
public interface DrugMapper {

    /** 按关键词分页模糊查询药品（名称/分类/适应症）。 */
    List<Drug> selectByKeyword(@Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("size") int size);

    /** 按关键词统计总数。 */
    long countByKeyword(@Param("keyword") String keyword);

    /** 按ID查药品。 */
    Drug selectById(@Param("id") Long id);

    /**
     * 扣减库存：仅当库存足够时才扣减成功，返回受影响行数。
     * 用 WHERE stock >= quantity 防止超卖（乐观控制）。
     */
    int decreaseStock(@Param("drugId") Long drugId, @Param("quantity") int quantity);

    /** 全部药品列表（运营后台用，按ID升序）。 */
    List<Drug> selectAll();

    /** 新增药品（运营后台上架），回填自增主键。 */
    int insert(Drug drug);

    /** 更新药品信息（运营后台编辑）。 */
    int update(Drug drug);

    /** 直接设置库存为指定值（运营后台补货/盘点）。 */
    int updateStock(@Param("drugId") Long drugId, @Param("stock") int stock);
}
