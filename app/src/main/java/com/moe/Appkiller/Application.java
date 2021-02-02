package com.moe.Appkiller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;
import java.io.FileOutputStream;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        File stop=new File(getFilesDir(),"killer");
        //if(!stop.exists()){
        try
        {
            ZipFile zf=new ZipFile(getPackageResourcePath());
            InputStream i=zf.getInputStream(zf.getEntry("classes.dex"));
            byte[] buff=new byte[128];
            OutputStream o=new FileOutputStream(stop);
            int len=-1;
            while((len=i.read(buff))!=-1){
                o.write(buff,0,len);
            }
            o.flush();
            o.close();
            i.close();
            zf.close();
            Process p=Runtime.getRuntime().exec("chmod 777 "+stop.getAbsolutePath());
            try {
                p.waitFor();
            } catch (InterruptedException e) {}
            p.destroy();
            //pw.flush();
        }
        catch (IOException e)
        {}
    }
    
    
    
}
