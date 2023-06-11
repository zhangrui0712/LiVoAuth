package idpservice;

import cmd.Constants;
import idpservice.db.DBManager;
import rpservice.RPConnectionService;
import rpservice.RPService;
import util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class IDPService {

    private static final String TAG = IDPService.class.getSimpleName();
    private static final int IDP_PORT = Constants.IDP_PORT;

    public static void start() {
        DBManager.initdatabase();
        ServerSocket mServerSocket;
        try {
            mServerSocket = new ServerSocket(IDP_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "error start idp service!");
            return;
        }

        Log.v(TAG, "started at port:" + IDP_PORT);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket socket = mServerSocket.accept();
                        new IDPConnectionService(socket).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public static String genUaId(String clientId, int rpId) {
        return clientId + "@" + rpId;
    }
}
