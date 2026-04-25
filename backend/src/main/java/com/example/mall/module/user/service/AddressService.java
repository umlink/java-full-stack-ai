package com.example.mall.module.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.module.user.entity.UserAddress;
import com.example.mall.module.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址业务逻辑
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AddressService {

    private final UserAddressMapper addressMapper;

    /**
     * 获取当前用户的地址列表（仅正常地址，按默认优先、更新时间倒序）
     */
    public List<UserAddress> getMyAddresses(Long userId) {
        return addressMapper.selectList(
                Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getStatus, 1)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt)
        );
    }

    /**
     * 获取单个地址（校验属于当前用户）
     */
    public UserAddress getById(Long id, Long userId) {
        UserAddress address = addressMapper.selectOne(
                Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getStatus, 1)
        );
        if (address == null) {
            throw new BusinessException(BizCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    /**
     * 新增地址
     * <ul>
     *   <li>校验不超过 20 个</li>
     *   <li>第一个地址自动设为默认</li>
     *   <li>如果设为默认，取消其他默认</li>
     * </ul>
     */
    public void create(UserAddress address, Long userId) {
        // 校验地址数量上限
        Long count = addressMapper.selectCount(
                Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getStatus, 1)
        );
        if (count >= 20) {
            throw new BusinessException(BizCode.ADDRESS_LIMIT);
        }

        // 第一个地址自动设为默认
        if (count == 0) {
            address.setIsDefault(true);
        }

        address.setUserId(userId);
        address.setStatus(1);

        // 如果设为默认，取消其他默认
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressMapper.update(null,
                    Wrappers.<UserAddress>lambdaUpdate()
                            .eq(UserAddress::getUserId, userId)
                            .eq(UserAddress::getIsDefault, true)
                            .set(UserAddress::getIsDefault, false)
            );
        }

        addressMapper.insert(address);
    }

    /**
     * 更新地址
     */
    public void update(UserAddress address, Long userId) {
        // 校验地址存在且属于当前用户
        UserAddress existing = addressMapper.selectOne(
                Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getId, address.getId())
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getStatus, 1)
        );
        if (existing == null) {
            throw new BusinessException(BizCode.ADDRESS_NOT_FOUND);
        }

        address.setUserId(userId);
        address.setUpdatedAt(null); // 让数据库自动更新时间

        // 如果设为默认，取消其他默认
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressMapper.update(null,
                    Wrappers.<UserAddress>lambdaUpdate()
                            .eq(UserAddress::getUserId, userId)
                            .eq(UserAddress::getIsDefault, true)
                            .set(UserAddress::getIsDefault, false)
            );
        }

        addressMapper.updateById(address);
    }

    /**
     * 设置默认地址
     */
    public void setDefault(Long id, Long userId) {
        // 校验地址存在且属于当前用户
        UserAddress address = addressMapper.selectOne(
                Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getStatus, 1)
        );
        if (address == null) {
            throw new BusinessException(BizCode.ADDRESS_NOT_FOUND);
        }

        // 取消当前默认
        addressMapper.update(null,
                Wrappers.<UserAddress>lambdaUpdate()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getIsDefault, true)
                        .set(UserAddress::getIsDefault, false)
        );

        // 设置新默认
        address.setIsDefault(true);
        addressMapper.updateById(address);
    }

    /**
     * 删除地址（软删除 status=0）
     */
    public void delete(Long id, Long userId) {
        // 校验地址存在且属于当前用户
        UserAddress address = addressMapper.selectOne(
                Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getStatus, 1)
        );
        if (address == null) {
            throw new BusinessException(BizCode.ADDRESS_NOT_FOUND);
        }

        addressMapper.update(null,
                Wrappers.<UserAddress>lambdaUpdate()
                        .eq(UserAddress::getId, id)
                        .set(UserAddress::getStatus, 0)
        );
    }
}
