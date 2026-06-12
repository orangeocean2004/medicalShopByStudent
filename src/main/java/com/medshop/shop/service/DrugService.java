package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.common.PageResult;
import com.medshop.shop.dto.DrugDTO;
import com.medshop.shop.entity.Drug;
import com.medshop.shop.mapper.DrugMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 药品业务层（M1）：搜索与详情查询。
 */
@Service
public class DrugService {

    private final DrugMapper drugMapper;

    public DrugService(DrugMapper drugMapper) {
        this.drugMapper = drugMapper;
    }

    /**
     * 分页模糊查询药品（FP-SHOP-01）。
     * 关键词为空时返回全部药品（药品目录默认浏览），非空时按名称/分类/适应症模糊匹配。
     */
    public PageResult<DrugDTO> search(String keyword, int page, int size) {
        // 关键词为空 → 目录浏览模式：用空串走 LIKE '%%' 匹配全部
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        int offset = (page - 1) * size;

        List<Drug> drugs = drugMapper.selectByKeyword(kw, offset, size);
        long total = drugMapper.countByKeyword(kw);

        List<DrugDTO> list = new ArrayList<>();
        for (Drug d : drugs) {
            list.add(toDTO(d));
        }
        return new PageResult<>(total, list);
    }

    /**
     * 查询药品详情（FP-SHOP-02）。
     */
    public DrugDTO getById(Long drugId) {
        Drug drug = drugMapper.selectById(drugId);
        if (drug == null) {
            throw new BizException("药品不存在");
        }
        return toDTO(drug);
    }

    private DrugDTO toDTO(Drug d) {
        DrugDTO dto = new DrugDTO();
        dto.setId(d.getId());
        dto.setName(d.getName());
        dto.setCategory(d.getCategory());
        dto.setIsRx(d.getIsRx());
        dto.setPrice(d.getPrice());
        dto.setStock(d.getStock());
        dto.setIndication(d.getIndication());
        dto.setDosage(d.getDosage());
        dto.setContraindication(d.getContraindication());
        return dto;
    }
}
