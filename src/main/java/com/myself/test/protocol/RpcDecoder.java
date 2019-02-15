package com.myself.test.protocol;

import com.myself.test.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 类名称：RpcDecoder<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private final Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() < 4) {
            return;
        }
        byteBuf.markReaderIndex();
        int length = byteBuf.readInt();
        if (byteBuf.readableBytes() < length) {
            byteBuf.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);

        Object obj = SerializationUtil.deserialize(bytes, genericClass);
        list.add(obj);
    }
}
