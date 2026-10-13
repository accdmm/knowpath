package com.li.zhitu.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.zhitu.api.dto.user.LoginFormDTO;
import com.li.zhitu.api.dto.user.UserDTO;
import com.li.zhitu.common.domain.dto.LoginUserDTO;
import com.li.zhitu.user.domain.dto.UserFormDTO;
import com.li.zhitu.user.domain.po.User;
import com.li.zhitu.user.domain.vo.UserDetailVO;

/**
 * <p>
 * 学员用户表 服务类
 * </p>
 *
 * @author 李梦豪
 * @since 2022-06-28
 */
public interface IUserService extends IService<User> {
    LoginUserDTO queryUserDetail(LoginFormDTO loginDTO, boolean isStaff);

    void resetPassword(Long userId);

    UserDetailVO myInfo();

    void addUserByPhone(User user, String code);

    void updatePasswordByPhone(String cellPhone, String code, String password);

    Long saveUser(UserDTO userDTO);

    void updateUser(UserDTO userDTO);

    void updateUserWithPassword(UserFormDTO userDTO);
}
