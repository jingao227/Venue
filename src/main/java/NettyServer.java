//package test.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.SystemPropertyUtil;

import java.awt.*;
import java.util.HashMap;

public class NettyServer {

    private static int MaxConnection = 999;

    private static int connectedNum;

    private static volatile ChannelGroup channelGroup;

    public static void main(String[] args) throws InterruptedException {
        final int[] colors = new int[11];
        colors[0] = 0xFF000000;
        colors[1] = 0xFF444444;
        colors[2] = 0xFF888888;
        colors[3] = 0xFFCCCCCC;
        colors[4] = 0xFFFFFFFF;
        colors[5] = 0xFFFF0000;
        colors[6] = 0xFF00FF00;
        colors[7] = 0xFF0000FF;
        colors[8] = 0xFFFFFF00;
        colors[9] = 0xFF00FFFF;
        colors[10] = 0xFFFF0000;


        NioEventLoopGroup parent = new NioEventLoopGroup(1);
        NioEventLoopGroup child = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap().group(parent, child).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ObjectEncoder());
                            ch.pipeline().addLast(new ChannelGroupHandler());
                            System.out.println("新连接启动");
                        }

                    });
            channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            ChannelFuture f = b.bind(8888).sync();
            System.out.println("服务器启动");

            new Thread(new Runnable() {

                //@Override
                public void run() {
                    try {
                        System.out.println("启动模拟任务");
                        int i = 0;
                        while (true) {
//                        for (int j = 0; j < 100; j++) {
//                            channelGroup.writeAndFlush(String.valueOf(i++)).sync();
                            //f (i >= 11) i = 0;
                            channelGroup.writeAndFlush(String.valueOf(i++) + "," + String.valueOf(100)).sync();
                            System.out.println("发送" + i);
                            //channelGroup.writeAndFlush(String.valueOf(colors[i]) + "," + String.valueOf(100)).sync();
                            //System.out.println("发送" + i++);
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

            f.channel().closeFuture().sync();
        } finally {
            child.shutdownGracefully();
            parent.shutdownGracefully();
            System.out.println("服务器关闭");
        }
    }

    private static class ChannelGroupHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            System.out.println("连接加入channelgroup");
            if (connectedNum < MaxConnection) {
                channelGroup.add(ctx.channel());
                super.channelRegistered(ctx);
                connectedNum ++;
            } else {
                ctx.channel().writeAndFlush("已达到连接上限");
//                System.out.println("已达到连接上限");
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            channelGroup.remove(ctx.channel());
            super.channelUnregistered(ctx);
            connectedNum --;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.err.println("异常，关闭连接");
            cause.printStackTrace();
            ctx.close();
        }

    }
}
