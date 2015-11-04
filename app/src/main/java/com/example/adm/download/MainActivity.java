package com.example.adm.download;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends ActionBarActivity {
    ProgressBar pb;
    TextView tv;
     int ThreadCount = 3;
     int finishedThread = 0;
     String path = "http://192.168.1.124/1234.avi";
     int length;
     int size;
    int currentProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pb = (ProgressBar)findViewById(R.id.pb);
        tv = (TextView)findViewById(R.id.tv);
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            tv.setText((long)pb.getProgress()*100/pb.getMax()+"%");
        }
    };
    public void click(View v){
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.connect();
                    if(conn.getResponseCode() == 200){
                        length = conn.getContentLength();
                        pb.setMax(length);
                        size = length/3;
                        for(int i=0;i<ThreadCount;i++){
                            int startIndex = i*size;
                            int endIndex = (i+1)*size-1;
                            if(i == ThreadCount-1){
                                endIndex = length -1;
                            }
                            new DownLoadThread(startIndex, endIndex, i).start();
                        }
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }.start();

    }
    class DownLoadThread extends Thread{
        int startIndex ;
        int endIndex;
        int threadId;

        public DownLoadThread(int startIndex, int endIndex, int threadId) {
            super();
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            URL url;
            try {
                File f = new File(Environment.getExternalStorageDirectory(),threadId+".txt");
                if(f.exists()){
                    FileInputStream fis = new FileInputStream(f);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    startIndex += Integer.parseInt(br.readLine());
                    fis.close();
                }
                url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Range", "bytes="+startIndex+"-"+endIndex);
                conn.connect();
                if(conn.getResponseCode() == 206){
                    InputStream is = conn.getInputStream();
                    byte[] b = new byte[1024];
                    int len = 0;
                    int total = 0;
                    File file = new File(Environment.getExternalStorageDirectory(),"1234.avi");
                    System.out.println(file.getPath());
                    RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    raf.seek(startIndex);
                    System.out.println("第"+threadId+"个线程下载区间为："+"start:"+this.startIndex+" end:"+this.endIndex);
                    while((len = is.read(b))!=-1){
                        raf.write(b, 0, len);
                        total += len;
                        //System.out.println("第"+threadId+"个线程下载了："+total);
                        currentProgress+=len;
                        pb.setProgress(currentProgress);

                        handler.sendEmptyMessage(1);
                        //生成一个临时记录文件
                        RandomAccessFile progressRaf = new RandomAccessFile(f, "rwd");
                        progressRaf.write((total+"").getBytes());
                        progressRaf.close();
                    }
                    System.out.println("第"+threadId+"个线程下载完成：");
                    raf.close();
                    finishedThread++;
                    synchronized (path) {
                        if(finishedThread == ThreadCount){
                            for (int i = 0; i < ThreadCount; i++) {
                                File fe = new File(Environment.getExternalStorageDirectory(),i + ".txt");
                                fe.delete();
                            }
                            finishedThread = 0;
                        }
                    }
                }

            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
