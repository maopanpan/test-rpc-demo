package com.myself.test.client;

/**
 * 类名称：HelloService<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public interface HelloService {

    String hello(String name);
    String hello(Person person);
}
