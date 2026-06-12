package com.medshop.consult.mapper;

import com.medshop.consult.dto.HandoffTicketDTO;
import com.medshop.consult.entity.HandoffTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 人工兜底工单数据访问层（P2，C-4）。SQL 见 mapper/HandoffMapper.xml。
 */
@Mapper
public interface HandoffMapper {

    int insert(HandoffTicket ticket);

    /** 全部工单（药师工作台，倒序，联表带出用户手机号）。 */
    List<HandoffTicketDTO> selectAll();

    /** 按ID查工单。 */
    HandoffTicket selectById(@Param("id") Long id);

    /** 药师接单/完成：更新状态与接手坐席。 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("agentId") Long agentId);

    /** 统计某坐席按状态接手的工单数（status 为 null 时统计该坐席接手的全部）。 */
    int countByAgent(@Param("agentId") Long agentId, @Param("status") Integer status);
}
