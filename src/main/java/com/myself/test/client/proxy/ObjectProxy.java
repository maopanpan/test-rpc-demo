package com.myself.test.client.proxy;

import com.myself.test.client.ConnectManage;
import com.myself.test.client.RPCFuture;
import com.myself.test.client.RpcClientHandler;
import com.myself.test.protocol.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 类名称：ObjectProxy<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public class ObjectProxy<T> implements InvocationHandler, IAsyncObjectProxy {
    private static final Logger logger = LoggerFactory.getLogger(ObjectProxy.class);
    private final Class<T> clazz;

    public ObjectProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler" + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setParameters(args);
        logger.debug(method.getDeclaringClass().getName());
        logger.debug(method.getName());
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            logger.debug(method.getParameterTypes()[i].getName());
        }
        for (int i = 0; i < args.length; i++) {
            logger.debug(args[i].toString());
        }
        RpcClientHandler handler = ConnectManage.getInstance().chooseHandler();
        RPCFuture rpcFuture = handler.sendReqeust(rpcRequest);
        return rpcFuture.get();
    }

    @Override
    public RPCFuture call(String funcName, Object... args) {
        RpcClientHandler handler = ConnectManage.getInstance().chooseHandler();
        RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
        RPCFuture rpcFuture = handler.sendReqeust(request);
        return rpcFuture;
    }

    private RpcRequest createRequest(String name, String funcName, Object[] args) {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(name);
        rpcRequest.setMethodName(funcName);
        rpcRequest.setParameters(args);

        Class[] parameterTYpes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTYpes[i] = getClassType(args[i]);
        }
        rpcRequest.setParameterTypes(parameterTYpes);
        logger.debug(name);
        logger.debug(funcName);
        for (int i = 0; i < parameterTYpes.length; i++) {
            logger.debug(parameterTYpes[i].getName());
        }
        for (int i = 0; i < args.length; i++) {
            logger.debug(args[i].toString());
        }
        return rpcRequest;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }

}
