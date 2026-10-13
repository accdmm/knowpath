package com.li.zhitu.message.service;

import com.li.zhitu.api.dto.sms.SmsInfoDTO;
import com.li.zhitu.api.dto.user.UserDTO;
import com.li.zhitu.message.domain.po.NoticeTemplate;

import java.util.List;

public interface ISmsService {
    void sendMessageByTemplate(NoticeTemplate noticeTemplate, List<UserDTO> users);

    void sendMessage(SmsInfoDTO smsInfoDTO);

    void sendMessageAsync(SmsInfoDTO smsInfoDTO);
}
