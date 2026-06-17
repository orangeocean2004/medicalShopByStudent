package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.shop.entity.Address;
import com.medshop.shop.mapper.AddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AddressService 单元测试：默认地址唯一性、首条自动默认、删默认补位、
 * 归属越权拦截、字段校验。
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock AddressMapper addressMapper;
    AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressMapper);
    }

    private Address addr(String receiver, String phone, String region, String detail) {
        Address a = new Address();
        a.setReceiver(receiver);
        a.setPhone(phone);
        a.setRegion(region);
        a.setDetail(detail);
        return a;
    }

    private Address valid() {
        return addr("张三", "13800000000", "北京市朝阳区", "某街道1号");
    }

    // ---------- 新增 ----------

    @Test
    @DisplayName("新增第一条地址：自动设为默认，并先清除其余默认")
    void create_firstAddress_becomesDefault() {
        when(addressMapper.countByUser(1L)).thenReturn(0);

        Address result = addressService.create(1L, valid());

        assertEquals(1, result.getIsDefault());
        assertEquals(1L, result.getUserId());
        verify(addressMapper).clearDefault(1L);
        verify(addressMapper).insert(result);
    }

    @Test
    @DisplayName("新增非首条且未要求默认：保持非默认，不清除既有默认")
    void create_nonFirst_notDefault() {
        when(addressMapper.countByUser(1L)).thenReturn(2);
        Address a = valid();
        a.setIsDefault(0);

        Address result = addressService.create(1L, a);

        assertEquals(0, result.getIsDefault());
        verify(addressMapper, never()).clearDefault(anyLong());
        verify(addressMapper).insert(result);
    }

    @Test
    @DisplayName("新增非首条但显式要求默认：清除旧默认后置为默认")
    void create_nonFirst_wantDefault() {
        when(addressMapper.countByUser(1L)).thenReturn(3);
        Address a = valid();
        a.setIsDefault(1);

        Address result = addressService.create(1L, a);

        assertEquals(1, result.getIsDefault());
        verify(addressMapper).clearDefault(1L);
    }

    @Test
    @DisplayName("新增：收货人为空抛异常，不入库")
    void create_blankReceiver_throws() {
        Address a = addr("  ", "13800000000", "北京", "街道1号");
        assertThrows(BizException.class, () -> addressService.create(1L, a));
        verify(addressMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增：手机号非11位抛异常")
    void create_badPhone_throws() {
        Address a = addr("张三", "138", "北京", "街道1号");
        assertThrows(BizException.class, () -> addressService.create(1L, a));
        verify(addressMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增：地区/详细地址为空抛异常")
    void create_blankRegionOrDetail_throws() {
        assertThrows(BizException.class,
                () -> addressService.create(1L, addr("张三", "13800000000", "", "街道")));
        assertThrows(BizException.class,
                () -> addressService.create(1L, addr("张三", "13800000000", "北京", "")));
    }

    // ---------- 更新 ----------

    @Test
    @DisplayName("更新：本人地址更新成功，保留原默认标记")
    void update_success_keepsDefaultFlag() {
        Address existing = valid();
        existing.setIsDefault(1);
        when(addressMapper.selectByIdAndUser(10L, 1L)).thenReturn(existing);

        Address patch = addr("李四", "13900000000", "上海", "新街道2号");
        Address result = addressService.update(1L, 10L, patch);

        assertEquals(10L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(1, result.getIsDefault()); // 沿用既有默认
        verify(addressMapper).update(patch);
    }

    @Test
    @DisplayName("更新：地址不属于该用户（越权）抛异常，不更新")
    void update_notOwned_throws() {
        when(addressMapper.selectByIdAndUser(10L, 2L)).thenReturn(null);
        assertThrows(BizException.class, () -> addressService.update(2L, 10L, valid()));
        verify(addressMapper, never()).update(any());
    }

    // ---------- 删除 ----------

    @Test
    @DisplayName("删除默认地址：把剩余第一条提升为默认")
    void delete_default_promotesNext() {
        Address existing = valid();
        existing.setIsDefault(1);
        when(addressMapper.selectByIdAndUser(10L, 1L)).thenReturn(existing);
        Address rest = valid();
        rest.setId(11L);
        when(addressMapper.selectByUser(1L)).thenReturn(List.of(rest));

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteByIdAndUser(10L, 1L);
        verify(addressMapper).setDefault(11L, 1L);
    }

    @Test
    @DisplayName("删除默认地址且无剩余：不再设默认")
    void delete_default_noRest() {
        Address existing = valid();
        existing.setIsDefault(1);
        when(addressMapper.selectByIdAndUser(10L, 1L)).thenReturn(existing);
        when(addressMapper.selectByUser(1L)).thenReturn(List.of());

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteByIdAndUser(10L, 1L);
        verify(addressMapper, never()).setDefault(anyLong(), anyLong());
    }

    @Test
    @DisplayName("删除非默认地址：不触发提升默认逻辑")
    void delete_nonDefault_noPromotion() {
        Address existing = valid();
        existing.setIsDefault(0);
        when(addressMapper.selectByIdAndUser(10L, 1L)).thenReturn(existing);

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteByIdAndUser(10L, 1L);
        verify(addressMapper, never()).selectByUser(anyLong());
        verify(addressMapper, never()).setDefault(anyLong(), anyLong());
    }

    @Test
    @DisplayName("删除：地址不属于该用户（越权）抛异常")
    void delete_notOwned_throws() {
        when(addressMapper.selectByIdAndUser(10L, 2L)).thenReturn(null);
        assertThrows(BizException.class, () -> addressService.delete(2L, 10L));
        verify(addressMapper, never()).deleteByIdAndUser(anyLong(), anyLong());
    }

    // ---------- 设默认 ----------

    @Test
    @DisplayName("设默认：先清除全部默认再置目标为默认（保证唯一）")
    void setDefault_clearsThenSets() {
        when(addressMapper.countByIdAndUser(10L, 1L)).thenReturn(1);

        addressService.setDefault(1L, 10L);

        var inOrder = inOrder(addressMapper);
        inOrder.verify(addressMapper).clearDefault(1L);
        inOrder.verify(addressMapper).setDefault(10L, 1L);
    }

    @Test
    @DisplayName("设默认：地址不属于该用户抛异常，不改动")
    void setDefault_notOwned_throws() {
        when(addressMapper.countByIdAndUser(10L, 2L)).thenReturn(0);
        assertThrows(BizException.class, () -> addressService.setDefault(2L, 10L));
        verify(addressMapper, never()).clearDefault(anyLong());
        verify(addressMapper, never()).setDefault(anyLong(), anyLong());
    }
}
