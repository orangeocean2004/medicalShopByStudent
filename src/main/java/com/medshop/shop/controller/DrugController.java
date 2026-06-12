package com.medshop.shop.controller;

import com.medshop.common.PageResult;
import com.medshop.common.Result;
import com.medshop.shop.dto.DrugDTO;
import com.medshop.shop.service.DrugService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 药品控制层（M1）。
 * <ul>
 *   <li>API-02 药品搜索：GET /api/v1/drugs</li>
 *   <li>API-03 药品详情：GET /api/v1/drugs/{id}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/drugs")
public class DrugController {

    private final DrugService drugService;

    public DrugController(DrugService drugService) {
        this.drugService = drugService;
    }

    @GetMapping
    public Result<PageResult<DrugDTO>> searchDrugs(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(drugService.search(keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<DrugDTO> getDrugDetail(@PathVariable Long id) {
        return Result.success(drugService.getById(id));
    }
}
