package com.li.zhitu.message.service;

import com.li.zhitu.message.domain.dto.SmsThirdPlatformDTO;
import com.li.zhitu.message.domain.dto.SmsThirdPlatformFormDTO;
import com.li.zhitu.message.domain.query.SmsThirdPlatformPageQuery;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.message.domain.po.SmsThirdPlatform;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 第三方云通讯平台 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-08-19
 */
public interface ISmsThirdPlatformService extends IService<SmsThirdPlatform> {

    List<SmsThirdPlatform> queryAllPlatform();

    Long saveSmsThirdPlatform(SmsThirdPlatformFormDTO thirdPlatformDTO);

    void updateSmsThirdPlatform(SmsThirdPlatformFormDTO thirdPlatformDTO);

    PageDTO<SmsThirdPlatformDTO> querySmsThirdPlatforms(SmsThirdPlatformPageQuery query);

    SmsThirdPlatformDTO querySmsThirdPlatform(Long id);
}
