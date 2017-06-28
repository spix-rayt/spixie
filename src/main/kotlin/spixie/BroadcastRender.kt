package spixie

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import sun.misc.IOUtils
import java.net.URI

class BroadcastRender : Thread() {
    private val masterGroup = NioEventLoopGroup()
    private val slaveGroup = NioEventLoopGroup()

    override fun run() {
        try {
            val serverBootstrap = ServerBootstrap().group(masterGroup, slaveGroup).channel(NioServerSocketChannel::class.java).childHandler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                override fun initChannel(socketChannel: SocketChannel) {
                    socketChannel.pipeline().addLast("codec", HttpServerCodec())
                    socketChannel.pipeline().addLast("aggregator", HttpObjectAggregator(512 * 1024))
                    socketChannel.pipeline().addLast("request", object : ChannelInboundHandlerAdapter() {
                        @Throws(Exception::class)
                        override fun channelRead(ctx: ChannelHandlerContext, o: Any) {
                            if (o is FullHttpRequest) {
                                var uri = URI(o.uri())
                                uri = uri.normalize()
                                var content: ByteArray? = null
                                if (uri.path == "/") {
                                    content = IOUtils.readFully(javaClass.getClassLoader().getResourceAsStream("index.html"), -1, true)
                                }
                                if (uri.path == "/jquery-3.1.1.min.js") {
                                    content = IOUtils.readFully(javaClass.getClassLoader().getResourceAsStream("jquery-3.1.1.min.js"), -1, true)
                                }
                                if (uri.path.startsWith("/render")) {
                                    content = renderedImageByteArray
                                }
                                var byteBuf: ByteBuf?
                                if (content == null) {
                                    byteBuf = Unpooled.copiedBuffer("500 Error".toByteArray())
                                    ctx.writeAndFlush(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, byteBuf!!))
                                } else {
                                    byteBuf = Unpooled.copiedBuffer(content)
                                    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf!!)
                                    response.headers().add("content_type", "image/jpeg")
                                    ctx.writeAndFlush(response)
                                }

                                ctx.close()
                                byteBuf.release()
                            } else {
                                super.channelRead(ctx, o)
                            }
                        }

                        @Throws(Exception::class)
                        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                            ctx.writeAndFlush(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.copiedBuffer(cause.message!!.toByteArray())))
                        }
                    })
                }
            }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, false)
            serverBootstrap.bind(9900).sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        val broadcastRender = BroadcastRender()
        @Volatile var renderedImageByteArray: ByteArray? = null
    }
}
