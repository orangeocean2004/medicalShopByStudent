package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.common.PageResult;
import com.medshop.shop.dto.DrugDTO;
import com.medshop.shop.entity.Drug;
import com.medshop.shop.mapper.DrugMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DrugService 单元测试（M1 搜索/详情）：关键词去空、空词走目录浏览、
 * 分页参数纠偏（offset/size 边界）、详情不存在拦截。
 */
@ExtendWith(MockitoExtension.class)
class DrugServiceTest {

    @Mock DrugMapper drugMapper;
    DrugService service;

    @BeforeEach
    void setUp() {
        service = new DrugService(drugMapper);
    }

    private Drug d(long id, String name) {
        Drug dr = new Drug();
        dr.setId(id);
        dr.setName(name);
        dr.setPrice(new BigDecimal("9.90"));
        dr.setStock(10);
        dr.setIsRx(0);
        return dr;
    }

    @Test
    @DisplayName("搜索：关键词去首尾空格后传入mapper，返回总数与列表")
    void search_trimsKeyword() {
        when(drugMapper.selectByKeyword(eq("感冒"), anyInt(), anyInt()))
                .thenReturn(List.of(d(5001L, "感冒灵")));
        when(drugMapper.countByKeyword("感冒")).thenReturn(1L);

        PageResult<DrugDTO> r = service.search("  感冒  ", 1, 10);

        assertEquals(1L, r.getTotal());
        assertEquals(1, r.getList().size());
        assertEquals("感冒灵", r.getList().get(0).getName());
    }

    @Test
    @DisplayName("搜索：空关键词走目录浏览，以空串匹配全部")
    void search_blankKeyword_listsAll() {
        when(drugMapper.selectByKeyword(eq(""), anyInt(), anyInt()))
                .thenReturn(List.of(d(5001L, "A"), d(5002L, "B")));
        when(drugMapper.countByKeyword("")).thenReturn(2L);

        PageResult<DrugDTO> r = service.search(null, 1, 10);

        assertEquals(2L, r.getTotal());
        verify(drugMapper).selectByKeyword(eq(""), anyInt(), anyInt());
    }

    @Test
    @DisplayName("搜索：page<1 纠正为1，offset从0开始")
    void search_pageBelowOne_corrected() {
        when(drugMapper.selectByKeyword(anyString(), anyInt(), anyInt())).thenReturn(List.of());
        when(drugMapper.countByKeyword(anyString())).thenReturn(0L);

        service.search("x", 0, 10);

        ArgumentCaptor<Integer> offset = ArgumentCaptor.forClass(Integer.class);
        verify(drugMapper).selectByKeyword(anyString(), offset.capture(), eq(10));
        assertEquals(0, offset.getValue());
    }

    @Test
    @DisplayName("搜索：size非法(0或>100)纠正为默认10")
    void search_illegalSize_defaultsTo10() {
        when(drugMapper.selectByKeyword(anyString(), anyInt(), anyInt())).thenReturn(List.of());
        when(drugMapper.countByKeyword(anyString())).thenReturn(0L);

        service.search("x", 1, 0);
        verify(drugMapper).selectByKeyword(anyString(), eq(0), eq(10));

        service.search("x", 1, 999);
        verify(drugMapper, times(2)).selectByKeyword(anyString(), eq(0), eq(10));
    }

    @Test
    @DisplayName("搜索：第2页offset=(page-1)*size")
    void search_secondPage_offset() {
        when(drugMapper.selectByKeyword(anyString(), anyInt(), anyInt())).thenReturn(List.of());
        when(drugMapper.countByKeyword(anyString())).thenReturn(0L);

        service.search("x", 2, 20);
        verify(drugMapper).selectByKeyword(anyString(), eq(20), eq(20));
    }

    @Test
    @DisplayName("详情：存在则返回DTO字段映射正确")
    void getById_success() {
        Drug dr = d(5001L, "感冒灵");
        dr.setCategory("感冒用药");
        when(drugMapper.selectById(5001L)).thenReturn(dr);

        DrugDTO dto = service.getById(5001L);
        assertEquals(5001L, dto.getId());
        assertEquals("感冒灵", dto.getName());
        assertEquals("感冒用药", dto.getCategory());
    }

    @Test
    @DisplayName("详情：药品不存在抛异常")
    void getById_notFound_throws() {
        when(drugMapper.selectById(anyLong())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(9999L));
    }
}
