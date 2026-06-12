package com.medshop.shop.mapper;

import com.medshop.shop.dto.PrescriptionDTO;
import com.medshop.shop.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 处方数据访问层（M4）。SQL 见 mapper/PrescriptionMapper.xml。
 */
@Mapper
public interface PrescriptionMapper {

    /** 插入处方，回填自增主键。 */
    int insert(Prescription p);

    /** 按ID查（含图片，审核用）。 */
    Prescription selectById(@Param("id") Long id);

    /** 我的处方列表（不含大图，倒序）。 */
    List<PrescriptionDTO> selectByUser(@Param("userId") Long userId);

    /** 待审处方列表（药师工作台，含图片）。 */
    List<PrescriptionDTO> selectPending();

    /** 更新审核结果：状态、备注、审核药师、审核时间。 */
    int updateReview(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("comment") String comment,
                     @Param("reviewerId") Long reviewerId);

    /**
     * 统计某用户对某处方药「审核通过」的处方数（下单放行判定）。
     * @return >0 表示该用户已有该药的有效处方
     */
    int countApprovedByUserAndDrug(@Param("userId") Long userId, @Param("drugId") Long drugId);

    /** 统计某药师按状态审核的处方数（status 为 null 时统计该药师审核的全部）。 */
    int countByReviewer(@Param("reviewerId") Long reviewerId, @Param("status") Integer status);
}
