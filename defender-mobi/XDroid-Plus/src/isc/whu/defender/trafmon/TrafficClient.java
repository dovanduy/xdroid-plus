package isc.whu.defender.trafmon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.SparseArray;

public class TrafficClient {
    private static final String SOCKET_NAME = "isc.whu.defender";
    private PrintWriter out;
    private BufferedReader in;
    private LocalSocket localSocket;

    private SparseArray<PTraffic> map;

    public SparseArray<PTraffic> getAppsTraffic() {
        map = new SparseArray<PTraffic>();
        try {
            localSocket = new LocalSocket();
            LocalSocketAddress localSocketAddress = new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT);
            localSocket.connect(localSocketAddress);
            in = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
            out = new PrintWriter(localSocket.getOutputStream(), true);

            out.println("all");
            char[] line = new char[256];
            while (in.read(line, 0, line.length) != -1 ) {
                process(new String(line).trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }
            if (localSocket != null) {
                try {
                    localSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                localSocket = null;
            }
        }
        return map;
    }

    private void process(String line) {
        String[] res = line.split("\\:");
        
        int uid = Integer.parseInt(res[0]);
        double param1 = Double.parseDouble(res[1]);
        double param2 = Double.parseDouble(res[2]);
        
        if (Double.isNaN(param1)) {
        	param1 = 0.0;
        }
        if (Double.isNaN(param2)) {
        	param2 = 0.0;
        }
        
        PTraffic appTraffic = new PTraffic(uid, param1, param2);

        map.put(uid, appTraffic);
    }
}
