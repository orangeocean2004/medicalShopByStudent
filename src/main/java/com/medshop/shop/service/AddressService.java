package com.medshop.shop.service;

import com.medshop.common.BizException;
import com.medshop.shop.entity.Address;
import com.medshop.shop.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址业务层：消费者维护自己的多个收货地址。
 *
 * <p>所有写操作都以 userId 为归属边界，越权(改/删他人地址)在 mapper 的
 * {@code WHERE user_id} 处被拦截，影响行数为 0 时抛业务异常。
 * 默认地址保证至多一个：首条地址自动设默认；设默认前先清除其余默认。
 */
@Service
public class AddressService {

    private final AddressMapper addressMapper;

    public AddressService(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public List<Address> list(Long userId) {
        return addressMapper.selectByUser(userId);
    }

    /** 新增地址。首条自动设为默认；显式要求设默认时清除其余默认。 */
    @Transactional
    public Address create(Long userId, Address addr) {
        validate(addr);
        addr.setUserId(userId);

        boolean first = addressMapper.countByUser(userId) == 0;
        boolean wantDefault = first || (addr.getIsDefault() != null && addr.getIsDefault() == 1);
        if (wantDefault) {
            addressMapper.clearDefault(userId);
            addr.setIsDefault(1);
        } else {
            addr.setIsDefault(0);
        }
        addressMapper.insert(addr);
        return addr;
    }

    /** 更新地址内容（不在此切换默认，设默认走 setDefault）。 */
    public Address update(Long userId, Long id, Address addr) {
        validate(addr);
        Address existing = addressMapper.selectByIdAndUser(id, userId);
        if (existing == null) {
            throw new BizException("地址不存在或无权操作");
        }
        addr.setId(id);
        addr.setUserId(userId);
        addressMapper.update(addr);
        addr.setIsDefault(existing.getIsDefault());
        return addr;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Address existing = addressMapper.selectByIdAndUser(id, userId);
        if (existing == null) {
            throw new BizException("地址不存在或无权操作");
        }
        addressMapper.deleteByIdAndUser(id, userId);
        // 删除的是默认地址，则把剩余的第一条提升为默认，避免无默认
        if (existing.getIsDefault() != null && existing.getIsDefault() == 1) {
            List<Address> rest = addressMapper.selectByUser(userId);
            if (!rest.isEmpty()) {
                addressMapper.setDefault(rest.get(0).getId(), userId);
            }
        }
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        if (addressMapper.countByIdAndUser(id, userId) == 0) {
            throw new BizException("地址不存在或无权操作");
        }
        addressMapper.clearDefault(userId);
        addressMapper.setDefault(id, userId);
    }

    private void validate(Address a) {
        if (a.getReceiver() == null || a.getReceiver().trim().isEmpty()) {
            throw new BizException("请填写收货人");
        }
        if (a.getPhone() == null || !a.getPhone().trim().matches("\\d{11}")) {
            throw new BizException("请填写 11 位联系电话");
        }
        if (a.getRegion() == null || a.getRegion().trim().isEmpty()) {
            throw new BizException("请填写所在地区");
        }
        if (a.getDetail() == null || a.getDetail().trim().isEmpty()) {
            throw new BizException("请填写详细地址");
        }
    }
}
