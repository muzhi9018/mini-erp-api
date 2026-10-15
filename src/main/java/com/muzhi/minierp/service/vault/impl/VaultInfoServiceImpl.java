package com.muzhi.minierp.service.vault.impl;

import com.muzhi.minierp.entity.vault.VaultInfo;
import com.muzhi.minierp.mapper.vault.VaultInfoMapper;
import com.muzhi.minierp.service.vault.IVaultInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 个人密码库元数据；一个认证账户仅有一个库 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
public class VaultInfoServiceImpl extends ServiceImpl<VaultInfoMapper, VaultInfo> implements IVaultInfoService {

}
