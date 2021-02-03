package com.moe.Appkiller;
import android.os.Looper;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.nio.channels.FileLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.File;

public class Killer implements Thread.UncaughtExceptionHandler,Runnable{
    private static ArrayList<String> whitelist=new ArrayList<>();
    static{
        whitelist.add("com.android.systemui");
        whitelist.add("com.android.packageinstaller");
        whitelist.add("com.google.android.gms");
        whitelist.add("com.android.permissioncontroller");
        whitelist.add("com.android.webview");
        whitelist.add("com.android.phone");
        whitelist.add("com.android.networkstack");
        whitelist.add("com.android.nfc");
        whitelist.add("com.android.keychain");
    }
    public static void main(String[] args){
        if(android.os.Process.myUid()>2000){
            System.out.println("权限不足！");
        }else{
            System.out.println(android.os.Process.myUid());
            Looper.prepare();
            System.out.println("创建环境");
            Killer killer=new Killer();
            Thread.setDefaultUncaughtExceptionHandler(killer);
            System.out.println("启动线程");
            ScheduledExecutorService pool=Executors.newSingleThreadScheduledExecutor();
            pool.scheduleAtFixedRate(killer,0,60,TimeUnit.SECONDS);
            try {
                final ServerSocket ss=new ServerSocket();
                ss.bind(new InetSocketAddress(43281));
                new Thread(){
                    public void run(){
                        
                        try {
                            while (true)
                                ss.accept();
                        } catch (IOException e) {}
                    }
                }.start();
                System.out.println("正在运行");
            } catch (IOException e) {
                System.out.println("43281端口占用");
            }
            
            Looper.loop();
        }
    }
    public Killer(){
        try {
            PrintWriter pw=new PrintWriter(new File("/data/data/com.moe.Appkiller/files/pid"));
            pw.print(android.os.Process.myPid());
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {}

    }
    @Override
    public void uncaughtException(Thread p1, Throwable p2) {
        System.out.println(p2.getMessage());
        for(StackTraceElement e:p2.getStackTrace()){
            System.out.println(e.toString());
        }
    }

    @Override
    public void run() {
        ArrayList<String> appprocess=new ArrayList<>();
        try {
            Process p=Runtime.getRuntime().exec("sh");
            PrintWriter pw=new PrintWriter(p.getOutputStream());
            BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
            pw.println("a=$(dumpsys activity p|grep \"^ *UID\"|tail -n +2)");
            pw.println("echo \"${a%%UID validation:*}\"|awk '{print $4}'|grep ^u0a|awk '{system(\"pm list packages -U|grep uid:\"substr($0,4)+10000)}'|awk -F '[ :]+' '{print $2}'");
            pw.println("echo //");
            //最近任务
            pw.println("echo \"$(dumpsys activity r|grep Activities|grep -v \"\\[\\]\"|awk -F '[ /]+' '{print $4}')\"");
            pw.println("echo whitelist");
            //电池优化白名单
            pw.println("cmd deviceidle whitelist|awk -F '[ ,]+' '{print $2}'");
            //通知
            pw.println("dumpsys notification|grep opPkg=|awk -F '=' '{print $2}'");
            //输入法
            pw.println("dumpsys input_method|grep mEnabledInputMethodsStrCache|awk -F '=' '{len=split($2,item,\":\");for(i=1;i<=len;i++){print item[i];}}'|cut -d / -f 1");
            pw.println("echo end");
            pw.println("dumpsys deviceidle step deep");
            pw.flush();
            String line=null;
            int mode=0;
            while((line=br.readLine().trim())!=null){
                if("end".equals(line))break;
                switch(mode){
                    case 0:
                    if("//".equals(line)){
                        mode=1;
                        appprocess.removeAll(whitelist);
                        break;
                        }
                        appprocess.add(line);
                        break;
               case 1:
                   if("whitelist".equals(line)){
                       mode=2;
                       break;
                   }
                    appprocess.remove(line);
                    pw.println("am set-inactive "+line+" true");
                    System.out.println("Recent "+line);
                    pw.flush();
                    break;
                case 2:
                    appprocess.remove(line);
                    break;
                }
            }
            Iterator<String> i=appprocess.iterator();
            while(i.hasNext()){
                String uid=i.next();
                pw.println("am force-stop "+uid);
                System.out.println("kill "+uid);
                pw.flush();
            }
            pw.println("exit");
            pw.flush();
            try {
                p.waitFor();
            } catch (InterruptedException e) {}
            p.destroy();
        } catch (IOException e) {}
    }


    
    
}
