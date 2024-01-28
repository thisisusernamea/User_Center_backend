package com.atyupi.usercenter_rear;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.atyupi.usercenter_rear.mapper")
public class UserCenterRearApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserCenterRearApplication.class, args);
    }

}
