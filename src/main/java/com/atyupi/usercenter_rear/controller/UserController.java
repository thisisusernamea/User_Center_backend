package com.atyupi.usercenter_rear.controller;

import com.atyupi.usercenter_rear.common.BaseResponse;
import com.atyupi.usercenter_rear.common.ErrorCode;
import com.atyupi.usercenter_rear.common.ResultUtils;
import com.atyupi.usercenter_rear.constant.UserConstant;
import com.atyupi.usercenter_rear.exception.BusinessException;
import com.atyupi.usercenter_rear.model.domain.User;
import com.atyupi.usercenter_rear.model.request.UserLoginRequest;
import com.atyupi.usercenter_rear.model.request.UserRegisterRequest;
import com.atyupi.usercenter_rear.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.atyupi.usercenter_rear.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;

    /**
     *用户注册请求
     * @param userRegisterRequest json请求参数对应的java对象
     * @return 新用户Id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录请求
     * @param userLoginRequest json请求参数对应的java对象
     * @param request
     * @return 用户脱敏记录
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest== null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User result = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(result);
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据用户名查询用户请求
     * @param username 请求参数
     * @param request
     * @return 用户列表
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request){
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username",username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> result = userList.stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(result);
    }

    /**
     * 管理员删除用户请求
     * @param id 请求参数
     * @param request
     * @return 是否成功删除的布尔值
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request){
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if(id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户的实时信息
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User currentUser = userService.getById(user.getId());
        User result = userService.getSafetyUser(currentUser);
        return ResultUtils.success(result);
    }

    /**
     * 鉴权 -> 是否为管理员
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

}
