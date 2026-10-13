package com.li.zhitu.message.service;

import com.li.zhitu.message.domain.dto.UserInboxDTO;
import com.li.zhitu.message.domain.dto.UserInboxFormDTO;
import com.li.zhitu.api.dto.user.UserDTO;
import com.li.zhitu.message.domain.query.UserInboxQuery;
import com.li.zhitu.common.domain.dto.PageDTO;
import com.li.zhitu.message.domain.po.NoticeTemplate;
import com.li.zhitu.message.domain.po.UserInbox;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 用户通知记录 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-08-19
 */
public interface IUserInboxService extends IService<UserInbox> {

    void saveNoticeToInbox(NoticeTemplate noticeTemplate, List<UserDTO> users);

    PageDTO<UserInboxDTO> queryUserInBoxesPage(UserInboxQuery query);

    Long sentMessageToUser(UserInboxFormDTO userInboxFormDTO);
}
