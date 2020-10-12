package chat.bio;

import lombok.extern.slf4j.Slf4j;
import util.Constant;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 多人聊天室客户端 2
 */
@Slf4j
public class ChatClient2  extends ChatClient{


    public static void main(String[] args) {
        ChatClient2 chatClient = new ChatClient2();
        chatClient.start();
    }



}
