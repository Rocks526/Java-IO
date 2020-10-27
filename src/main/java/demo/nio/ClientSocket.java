package demo.nio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * 客户端
 */
@Slf4j
public class ClientSocket {

    private Boolean clientRunningFlag = true;

    private void start() throws IOException {
        //创建SocketChannel
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(Constant.ServerIP,Constant.ServerPort));
        //设置非阻塞
        socketChannel.configureBlocking(false);
        //监听服务端响应事件
        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);
        while (clientRunningFlag){

            //阻塞等待响应
            Integer selectedNum = selector.select();

            if (selectedNum.equals(0)){
                log.info("Client Selector returned by : {}",selectedNum);
            }

            //获取响应事件
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            for (SelectionKey selectionKey : selectionKeys){

                //可读事件
                if (selectionKey.isReadable()){
                    ReciveServerMsgHandler(selectionKey,selector);
                }

                //删除处理过的事件
                selectionKeys.remove(selectionKey);

            }

            //向服务器发送数据
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String msg = consoleReader.readLine();
            socketChannel.write(Charset.forName("UTF-8").encode(msg));

        }

    }

    private void ReciveServerMsgHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        //读取服务端响应
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        StringBuilder msg = new StringBuilder();
        while (channel.read(byteBuffer) > 0){
            //切换为读
            byteBuffer.flip();
            //数据拼接
            msg.append(Charset.forName("UTF-8").decode(byteBuffer));
        }
        System.out.println(msg);
        if (Constant.ClientStopCommand.equals(msg.toString())){
            clientRunningFlag = false;
        }
        //注册可读事件
        channel.register(selector,SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws IOException {

        ClientSocket clientSocket = new ClientSocket();
        clientSocket.start();

    }


}
