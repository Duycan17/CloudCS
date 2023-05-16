package ServerNode;

import java.io.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

class WorkerThread extends Thread {
    List<String> dataToSend = new ArrayList<>();
    int requestID = -1;


    WorkerThread(List<String> dts){
        dataToSend = dts;
    }

    public void run(){
        try{
            System.out.println("WorkerThread. Starting work.");
            //Does work

            //For testing. Sleeps for 60sec
            Thread.sleep(240000);
            System.out.println("WorkerThread. Finished work.");
            //Updates Work object

            Worker.newWork.setData("WorkerThreadDataSet");

            Worker.newWork.setStatus(Status.COMPLETE);

        }catch(Exception ex){
            System.out.println(ex);
            Worker.newWork.setStatus(Status.ERROR);
        }finally{
            System.out.println("WorkerThread exit!! ");
        }
    }
}