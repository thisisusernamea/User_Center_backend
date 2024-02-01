package com.atyupi.usercenter_rear.service.impl;

import com.atyupi.usercenter_rear.common.ErrorCode;
import com.atyupi.usercenter_rear.exception.BusinessException;
import com.atyupi.usercenter_rear.model.domain.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atyupi.usercenter_rear.service.UserService;
import com.atyupi.usercenter_rear.mapper.UserMapper;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.atyupi.usercenter_rear.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 常俊杰
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2023-09-04 08:25:26
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    //加盐，用户混淆密码
    private static final String SALT = "yupi";

    @Resource
    UserMapper userMapper;

    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode) {
        //1.校验 非空
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        //用户名不小于4位
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        //密码不小于8位
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        //星球编号长度不得大于5位
        if(planetCode.length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号过长");
        }
        //账号不能包含特殊字符
        String validPattern = "[\\u00A0\\s\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号不能包含特殊字符");
        }
        //密码和校验密码相同
        if(userAccount.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入密码不一致");
        }
        //用户账号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号已存在");
        }
        //星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode",planetCode);
        count = userMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号已存在");
        }
        //2.对密码进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //3.向数据库中插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        int insertResult = userMapper.insert(user);
            //实体类中id类型为包装类Long,userRegister方法中返回类型为long,若user.getId()结果为null作为方法返回值返回,此时会发生拆箱错误
        if(insertResult != 1){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统内部错误,数据插入失败");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request
     * @return
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验 非空
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"用户账号或密码不能为空");
        }
        //用户名不小于4位
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        //密码不小于8位
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        //账户不能包含特殊字符
        String validPattern = "[\\u00A0\\s\"`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号不能包含特殊字符");
        }
        //2.查询用户账户、密码是否存在
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            log.info("user login failed,userAccount cannot match userPassword!");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号密码不匹配");
        }
        //3.登录成功返回用户脱敏信息
        User safetyUser = getSafetyUser(user);
        //4.记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE,safetyUser);
        return safetyUser;
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除用户登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 用户脱敏
     * @param originUser 数据库中的用户记录
     * @return 用户脱敏记录
     */
    @Override
    public User getSafetyUser(User originUser){
        if(originUser == null){
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 根据标签搜索用户（内存查询版）
     * @param tagNameList 用户拥有的标签列表
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList){

        //1.查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        //2.在内存中判断用户是否包含要求的标签
        Gson gson = new Gson();
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)){
                return false;
            }
            //反序列化将json字符串转换为Java对象                     不支持List<String>.class
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>(){}.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName:tagNameList) {
                if(!tempTagNameSet.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 根据标签查询用户（sql查询版）
     * @param tagNameList
     * @return
     */
    @Deprecated
    public List<User> searchUserByTagsBySql(List<String> tagNameList){
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName:tagNameList) {
            queryWrapper = queryWrapper.like("tags",tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        //用户脱敏
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }
}




