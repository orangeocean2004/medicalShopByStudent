package com.medshop.consult.mapper;

import com.medshop.consult.entity.KnowledgeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 药学知识库数据访问层（P1，RAG）。SQL 见 mapper/KnowledgeMapper.xml。
 */
@Mapper
public interface KnowledgeMapper {

    /**
     * 取全部知识条目，供 mock 版做关键词召回打分。
     * 知识库规模小（课设场景），全量载入内存检索足够；真实版改为向量检索。
     */
    List<KnowledgeItem> selectAll();

    /** 真实版可按关键词在库侧粗筛（保留接口）。 */
    List<KnowledgeItem> searchByKeyword(@Param("keyword") String keyword);
}
