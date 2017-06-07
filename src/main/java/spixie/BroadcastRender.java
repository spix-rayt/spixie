package spixie;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import sun.misc.IOUtils;

import java.net.URI;

public class BroadcastRender extends Thread {
    public final static BroadcastRender broadcastRender = new BroadcastRender();
    public static volatile byte[] renderedImageByteArray = null;
    private final EventLoopGroup masterGroup = new NioEventLoopGroup();
    private final EventLoopGroup slaveGroup = new NioEventLoopGroup();

    public BroadcastRender() {

    }

    @Override
    public void run() {
        try{
            final ServerBootstrap serverBootstrap = new ServerBootstrap().group(masterGroup, slaveGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast("codec",new HttpServerCodec());
                    socketChannel.pipeline().addLast("aggregator", new HttpObjectAggregator(512*1024));
                    socketChannel.pipeline().addLast("request", new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
                            if(o instanceof FullHttpRequest){
                                FullHttpRequest request = (FullHttpRequest) o;
                                URI uri = new URI(request.uri());
                                uri = uri.normalize();
                                byte[] content = null;
                                if(uri.getPath().equals("/")){
                                    content = IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("index.html"), -1, true);
                                }
                                if(uri.getPath().equals("/jquery-3.1.1.min.js")){
                                    content = IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("jquery-3.1.1.min.js"), -1, true);
                                }
                                if(uri.getPath().startsWith("/render")){
                                    content = renderedImageByteArray;
                                }
                                ByteBuf byteBuf = null;
                                if(content == null){
                                    byteBuf = Unpooled.copiedBuffer("500 Error".getBytes());
                                    ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, byteBuf));
                                }else{
                                    byteBuf = Unpooled.copiedBuffer(content);
                                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
                                    response.headers().add("content_type", "image/jpeg");
                                    ctx.writeAndFlush(response);
                                }

                                ctx.close();
                                if(byteBuf != null){
                                    byteBuf.release();
                                }
                            }else{
                                super.channelRead(ctx,o);
                            }
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.copiedBuffer(cause.getMessage().getBytes())));
                        }
                    });
                }
            }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, false);
            ChannelFuture sync = serverBootstrap.bind(9900).sync();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
