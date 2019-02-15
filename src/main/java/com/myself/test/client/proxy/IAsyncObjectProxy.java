package com.myself.test.client.proxy;

import com.myself.test.client.RPCFuture;

/**
 * 类名称：IAsyncObjectProxy<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public interface IAsyncObjectProxy {
    RPCFuture call(String funcName, Object... args);
}
