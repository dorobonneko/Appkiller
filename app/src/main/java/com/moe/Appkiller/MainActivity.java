package com.moe.Appkiller;
 
import android.app.Activity;
import android.os.Bundle;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.io.PrintWriter;
import android.os.StrictMode;
import java.net.Socket;
import java.net.InetSocketAddress;

public class MainActivity extends Activity implements ToggleButton.OnCheckedChangeListener { 
     private ToggleButton status;
     private Thread check;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status=findViewById(R.id.status_switch);
        checkStatus();
    }
    private void checkStatus(){
        if(check!=null)return;
        final Socket s=new Socket();
        
        try {
            s.connect(new InetSocketAddress(43281));
            status.setOnCheckedChangeListener(null);
            status.setChecked(s.isConnected());
            status.setOnCheckedChangeListener(this);
            if(s.isConnected()){
                (check=new Thread(){
                    public void run(){
                        try {
                            s.getInputStream().read();
                            runOnUiThread(new Runnable(){

                                    @Override
                                    public void run() {
                                        status.setOnCheckedChangeListener(null);
                                        status.setChecked(false);
                                        status.setOnCheckedChangeListener(MainActivity.this);
                                        check=null;
                                    }
                                });
                        } catch (IOException e) {
                            try {
                                s.close();
                            } catch (IOException ee) {}
                        }
                        
                    }
                }).start();
            }else{
                s.close();
            }
        } catch (Exception e) {
            status.setOnCheckedChangeListener(null);
            status.setChecked(false);
            status.setOnCheckedChangeListener(this);
        }
    }
    @Override
    public void onCheckedChanged(final CompoundButton p1, boolean p2) {
        p1.setEnabled(false);
        if(p2){
            new Thread(){
                public void run(){
                    try {
                        Process p=Runtime.getRuntime().exec("su");
                        PrintWriter pw=new PrintWriter(p.getOutputStream());
                        pw.println("app_process -Djava.class.path=/data/data/com.moe.Appkiller/files/killer  /data/local/tmp com.moe.Appkiller.Killer > /data/data/com.moe.Appkiller/files/log&");
                        pw.println("sleep 1s");
                        pw.println("exit");
                        pw.flush();
                        try {
                            p.waitFor();
                        } catch (InterruptedException e) {}
                        p.destroy();
                        runOnUiThread(new Runnable(){

                                @Override
                                public void run() {
                                    checkStatus();
                                    p1.setEnabled(true);
                                }
                            });
                    } catch (IOException e) {}
                }
            }.start();
        }else{
            new Thread(){
                public void run(){
                    try {
                        Process p=Runtime.getRuntime().exec("su");
                        PrintWriter pw=new PrintWriter(p.getOutputStream());
                        pw.println("kill -9 $(netstat -lp|grep 43281|grep LISTEN|awk -F '[ /]+' '{print $7}')");
                        pw.println("kill -9 $(cat /data/data/com.moe.Appkiller/files/pid)");
                        pw.println("exit");
                        pw.flush();
                        try {
                            p.waitFor();
                        } catch (InterruptedException e) {}
                        pw.close();
                        p.destroy();
                        runOnUiThread(new Runnable(){

                                @Override
                                public void run() {
                                    checkStatus();
                                    p1.setEnabled(true);
                                }
                            });
                    } catch (IOException e) {}
                }
            }.start();
        }
    }

	
} 
