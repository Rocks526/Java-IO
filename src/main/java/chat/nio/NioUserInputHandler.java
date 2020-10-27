package chat.nio;

import lombok.SneakyThrows;
import util.Constant;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NioUserInputHandler implements Runnable {

    private ChatClinet chatClinet;

    public NioUserInputHandler(ChatClinet chatClinet) {
        this.chatClinet = chatClinet;
    }

    @SneakyThrows
    @Override
    public void run() {

        while (true){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String userInputMsg = bufferedReader.readLine();
            this.chatClinet.sendMsgToServer(userInputMsg);
            if (Constant.ClientStopCommand.equals(userInputMsg)){
                this.chatClinet.ClientExit();
                break;
            }
        }

    }
}
