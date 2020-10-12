package chat.bio;

import util.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    private BufferedReader reader;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public void run() {
        //接收用户输入 发送服务端 如果用户输入结束标识 则更新ClientRunningFlag
        reader = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            try {
                String msg = reader.readLine();
                chatClient.sendMsgToServer(msg);
                if (Constant.ClientStopCommand.equals(msg)){
                    chatClient.updateFlag();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
