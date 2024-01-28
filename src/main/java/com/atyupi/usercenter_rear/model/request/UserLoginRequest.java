package com.atyupi.usercenter_rear.model.request;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 5544977521392906894L;
    private String userAccount;
    private String userPassword;
}
