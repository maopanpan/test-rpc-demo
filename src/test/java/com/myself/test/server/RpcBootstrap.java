package com.myself.test.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 类名称：RpcBootstrap<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public class RpcBootstrap {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("server-spring.xml");
    }
}
