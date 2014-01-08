package com.lge.ccdevs.tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class TrackerServer extends Service {
    private final IBinder mBinder = new LocalBinder();
    
    private NotificationManager mNM;
    private String mMessage = "";
    
    // default ip
    public static String SERVERIP;

    // designate a port
    public static final int SERVERPORT = 5555;
    private Handler handler = new Handler();
    private Socket mSocket = null;    
    private Context mContext;
    
    
    
    public class LocalBinder extends Binder {
        TrackerServer getService() {
            return TrackerServer.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onCreate() {
        mContext = this;
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SERVERIP = intent.getStringExtra("com.lge.ccdevs.tracker.IP");
        
        Thread cThread = new Thread(new ServerThread());
        cThread.start();
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Toast.makeText(getApplicationContext(), "stop watching..", Toast.LENGTH_SHORT).show();
        
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
    
    public class ServerThread implements Runnable {

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String noti = "Connecting..";
                        Toast.makeText(mContext, noti, Toast.LENGTH_SHORT).show();
                        showNotification(R.drawable.ic_launcher, noti);
                    }
                });
                mSocket = new Socket(serverAddr, SERVERPORT);
                while (true) {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                        String line = null;
                        while ((line = in.readLine()) != null) {
                            Log.d("ServerActivity", line);
                            mMessage = line;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showNotification(R.drawable.ic_launcher, mMessage);
                                }
                            });
                        }
                        break;
                    } catch (Exception e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String noti = "Connection interrupted. Please reconnect your phones.";
                                Toast.makeText(mContext, noti, Toast.LENGTH_SHORT).show();
                                showNotification(R.drawable.ic_launcher, noti);
                            }
                        });
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String noti = "ERROR!!";
                        Toast.makeText(mContext, noti, Toast.LENGTH_SHORT).show();
                        showNotification(R.drawable.ic_launcher, noti);
                    }
                });
                e.printStackTrace();
                
                stopSelf();
            } 
        }
    }
    
    private void showNotification(int iconId, String msg) {
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MyTrackerActivity.class), 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        
        builder.setSmallIcon(iconId)
            .setAutoCancel(true)
            .setContentTitle("title")
            .setContentInfo("info")
            .setContentText(msg)
            .setTicker(msg) // null
            .setContentIntent(pi);
        
        builder.setContentIntent(pi);
        mNM.notify(R.layout.activity_main, builder.build());
    }
}
