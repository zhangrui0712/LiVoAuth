package rpservice;

import cmd.Constants;
import util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class RPService {

    private static final String TAG = RPService.class.getSimpleName();

    private static final int port = Constants.RP_PORT;
    private static final int id = Constants.RP_ID;

    public static void start() {
        Log.v(TAG, "started service at port:" + port + " id is:" + id);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket mServerSocket = new ServerSocket(port);
                    while (true) {
                        Socket mSocket = mServerSocket.accept();
                        new RPConnectionService(id, mSocket).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
