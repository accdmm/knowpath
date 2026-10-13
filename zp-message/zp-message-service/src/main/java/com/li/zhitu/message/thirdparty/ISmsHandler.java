package com.li.zhitu.message.thirdparty;

import com.li.zhitu.api.dto.sms.SmsInfoDTO;
import com.li.zhitu.message.domain.po.MessageTemplate;

/**
 * 第三方接口对接平台
 */
public interface ISmsHandler {

    /**
     * 发送短信
     */
    void send(SmsInfoDTO platformSmsInfoDTO, MessageTemplate template);


}
