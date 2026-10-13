package com.li.zhitu.message.service;

import com.li.zhitu.message.domain.dto.MessageTemplateDTO;
import com.li.zhitu.message.domain.dto.MessageTemplateFormDTO;
import com.li.zhitu.message.domain.query.MessageTemplatePageQuery;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.message.domain.po.MessageTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 第三方短信平台签名和模板信息 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-08-19
 */
public interface IMessageTemplateService extends IService<MessageTemplate> {

    List<MessageTemplate> queryByNoticeTemplateId(Long id);

    Long saveMessageTemplate(MessageTemplateFormDTO messageTemplateDTO);

    void updateMessageTemplate(MessageTemplateFormDTO messageTemplateDTO);

    PageDTO<MessageTemplateDTO> queryMessageTemplates(MessageTemplatePageQuery pageQuery);

    MessageTemplateDTO queryMessageTemplate(Long id);
}
