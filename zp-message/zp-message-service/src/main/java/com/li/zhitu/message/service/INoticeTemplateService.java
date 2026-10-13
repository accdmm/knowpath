package com.li.zhitu.message.service;

import com.li.zhitu.message.domain.dto.NoticeTemplateDTO;
import com.li.zhitu.message.domain.dto.NoticeTemplateFormDTO;
import com.li.zhitu.message.domain.query.NoticeTemplatePageQuery;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.message.domain.po.NoticeTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 通知模板 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-08-19
 */
public interface INoticeTemplateService extends IService<NoticeTemplate> {

    Long saveNoticeTemplate(NoticeTemplateFormDTO noticeTemplateFormDTO);

    void updateNoticeTemplate(NoticeTemplateFormDTO noticeTemplateFormDTO);

    PageDTO<NoticeTemplateDTO> queryNoticeTemplates(NoticeTemplatePageQuery pageQuery);

    NoticeTemplateDTO queryNoticeTemplate(Long id);

    NoticeTemplate queryByCode(String code);
}
