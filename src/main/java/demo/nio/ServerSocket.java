package demo.nio;


import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 服务端
 */
@Slf4j
public class ServerSocket {


    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.start();
    }

    private void start() throws IOException {
        //创建Selector
        Selector selector = Selector.open();
        //创建ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //绑定端口
        serverSocketChannel.bind(new InetSocketAddress(Constant.ServerPort));
        //设置非阻塞
        serverSocketChannel.configureBlocking(false);
        //将Channel注册到Selector上，监听客户端连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true){
            //阻塞等待selector上的所有channel事件就绪 返回就绪的Channel个数
            Integer selectedNumber = selector.select();

            //正常来说 只有事件触发才会返回 Jdk存在bug 可能空轮训
            if (selectedNumber.equals(0)){
                log.info("Server Selector returned by : {}",selectedNumber);
            }

            //获取发生就绪事件的Channel集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            for (SelectionKey selectionKey : selectionKeys){

                //客户端连接事件
                if (selectionKey.isAcceptable()){
                    ClientConnectedHandler(selectionKey,selector);
                }

                //客户端发送消息事件
                if (selectionKey.isReadable()){
                    ClientSendMsgHandler(selectionKey,selector);
                }

                //移除本次的响应事件 => 也可以不移除 留给下次处理
                selectionKeys.remove(selectionKey);
            }

        }
    }

    private void ClientSendMsgHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        //获取客户端连接
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        //读取客户端发送的消息 向客户端发送响应 原样回复 加Server后缀
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        StringBuilder msg = new StringBuilder();
        while (channel.read(byteBuffer) > 0){
            //切换为读
            byteBuffer.flip();
            //数据拼接
            msg.append(Charset.forName("UTF-8").decode(byteBuffer));
        }
        msg.append("[from Server]");
        channel.write(Charset.forName("UTF-8").encode(msg.toString()));
        //注册可读事件
        channel.register(selector,SelectionKey.OP_READ);
    }


    private void ClientConnectedHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        //获取客户端连接 设置非阻塞
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        //向客户端发送响应
        String msg = "Server[" + Constant.ServerPort + "] connected success...";
        channel.write(Charset.forName("UTF-8").encode(msg));
        //注册可读事件
        channel.register(selector,SelectionKey.OP_READ);
    }

}
