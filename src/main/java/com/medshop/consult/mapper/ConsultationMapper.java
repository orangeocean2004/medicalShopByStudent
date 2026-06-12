package com.medshop.consult.mapper;

import com.medshop.consult.entity.Consultation;
import com.medshop.consult.entity.ConsultationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 咨询会话与消息数据访问层（M2）。SQL 见 mapper/ConsultationMapper.xml。
 */
@Mapper
public interface ConsultationMapper {

    int insertConsultation(Consultation consultation);

    Consultation selectById(@Param("id") Long id);

    int insertMessage(ConsultationMessage message);

    /** 取会话全部消息（转人工时打包上下文、查看历史）。 */
    List<ConsultationMessage> selectMessages(@Param("consultationId") Long consultationId);

    /** 标记会话已转人工，并记录接手药师。 */
    int markTransferred(@Param("id") Long id, @Param("pharmacistId") Long pharmacistId);

    /** 结束人工、切回智能药师：清除转人工标记与接手药师。 */
    int markResumed(@Param("id") Long id);
}
