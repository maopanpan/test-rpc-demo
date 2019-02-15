package com.myself.test.client;

/**
 * 类名称：AsyncRPCCallback<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public interface AsyncRPCCallback {
    void success(Object result);

    void fail(Exception e);
}
