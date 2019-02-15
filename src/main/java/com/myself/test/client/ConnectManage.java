package com.myself.test.client;

import com.myself.test.protocol.RpcDecoder;
import com.myself.test.protocol.RpcEncoder;
import com.myself.test.protocol.RpcRequest;
import com.myself.test.protocol.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 类名称：ConnectManage<br>
 * 类描述：<br>
 * 创建时间：2019年02月15日<br>
 *
 * @author maopanpan
 * @version 1.0.0
 */
public class ConnectManage {
    private static final Logger logger = LoggerFactory.getLogger(ConnectManage.class);
    private volatile static ConnectManage connectManage;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers =
            new CopyOnWriteArrayList<>();
    private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long connectTimeoutMillis = 6000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRunning = Boolean.TRUE;

    private ConnectManage() {

    }

    public static ConnectManage getInstance() {
        if (connectManage == null) {
            synchronized (ConnectManage.class) {
                if (connectManage == null) {
                    connectManage = new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    public void updateConnectedServer(List<String> allServerAddress) {
        if (allServerAddress != null) {
            if (allServerAddress.size() > 0) {
                HashSet<InetSocketAddress> newAllServerNodeList = new HashSet<>();
                for (int i = 0; i < allServerAddress.size(); i++) {
                    String[] array = allServerAddress.get(i).split(":");
                    if (array.length == 2) {
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);
                        final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                        newAllServerNodeList.add(remotePeer);
                    }
                }

                for (final InetSocketAddress serverNodeAddress : newAllServerNodeList) {
                    if (!connectedServerNodes.keySet().contains(serverNodeAddress)) {
                        connectServerNode(serverNodeAddress);
                    }
                }

                for (int i = 0; i < connectedHandlers.size(); i++) {
                    RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
                    SocketAddress remotePeer = connectedServerHandler.getRemoteAddress();
                    if (!newAllServerNodeList.contains(remotePeer)) {
                        logger.info("Remove invalid server node " + remotePeer);
                        RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                        if (handler != null) {
                            handler.close();
                        }
                        connectedServerNodes.remove(remotePeer);
                        connectedHandlers.remove(connectedServerHandler);
                    }
                }
            } else {
                logger.error("No available server node. All server nodes are down!");
                for (final RpcClientHandler rpcClientHandler : connectedHandlers) {
                    SocketAddress socketAddress = rpcClientHandler.getRemoteAddress();
                    RpcClientHandler handler = connectedServerNodes.get(socketAddress);
                    handler.close();
                    connectedServerNodes.remove(rpcClientHandler);
                }
                connectedHandlers.clear();
            }
        }
    }

    public void reconnect(final RpcClientHandler handler, final SocketAddress socketAddress) {
        if (handler != null) {
            connectedHandlers.remove(handler);
            connectedServerNodes.remove(handler.getRemoteAddress());
        }
        connectServerNode((InetSocketAddress) socketAddress);
    }

    private void connectServerNode(InetSocketAddress serverNodeAddress) {
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel channel) throws Exception {
                                ChannelPipeline cp = channel.pipeline();
                                cp.addLast(new RpcEncoder(RpcRequest.class));
                                cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
                                cp.addLast(new RpcDecoder(RpcResponse.class));
                                cp.addLast(new RpcClientHandler());
                            }
                        });
                ChannelFuture channelFuture = bootstrap.connect(serverNodeAddress);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.debug("Successfully connect to remote server. remote peer = " + serverNodeAddress);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(handler);
                        }
                    }
                });
            }
        });
    }

    private void addHandler(RpcClientHandler handler) {
        connectedHandlers.add(handler);
        InetSocketAddress remoteAddress = (InetSocketAddress) handler.getChannel().remoteAddress();
        connectedServerNodes.put(remoteAddress, handler);
        signalAvailableHandler();
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler() {
        int size = connectedHandlers.size();
        while (isRunning && size <= 0) {
            try {
                boolean available = waitingForHandler();
                if (available) {
                    size = connectedHandlers.size();
                }
            } catch (InterruptedException e) {
                logger.error("Waiting for available node is interrupted! ", e);
                throw new RuntimeException("Can't connect any servers!", e);
            }
        }
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return connectedHandlers.get(index);
    }

    public void stop() {
        isRunning = false;
        for(int i = 0; i<connectedServerNodes.size(); i++) {
            RpcClientHandler rpcClientHandler = connectedHandlers.get(i);
            rpcClientHandler.close();
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
