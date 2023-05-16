package ServerNode;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

import ServerNode.ServerNode.Status;
class ServerNodeThread extends Thread {
    Socket socWorker;
    List<String> dataToSend = new ArrayList<>();
    int requestID = -1;


    ServerNodeThread(Socket inSocket, List<String> dts, int id){
        socWorker = inSocket;
        dataToSend = dts;
        requestID = id;
    }
    public void run(){
        try{

            //while loop to poll results from worker
            ObjectOutputStream outWorker = new ObjectOutputStream(socWorker.getOutputStream());
            BufferedReader inWorker = new BufferedReader(new InputStreamReader(socWorker.getInputStream()));

            //Initial work request
            outWorker.writeObject(dataToSend);
//			System.out.println("ST: Sending Object");
            String incomingData = inWorker.readLine();
//			System.out.println("ST: IncomingData: " + incomingData);

            //Check for status via polling every 10sec
            while(true) {
                //sleep for 10 seconds
                Thread.sleep(10000);
                List<String> whileData = new ArrayList<>();
                System.out.println("ServerThread. RequestID: " + requestID + ", Status: " + ServerNode.requestQueue.get(requestID).getStatus());
                if(ServerNode.requestQueue.get(requestID).getStatus() == Status.BUSY) {
//					System.out.println("ST. Status");
                    whileData.add("status");			//RequestType
                    whileData.add(dataToSend.get(1));	//stationID
                    whileData.add(dataToSend.get(2));	//Year
                    whileData.add(dataToSend.get(3));	//ServerID
                    whileData.add(dataToSend.get(4));	//DataLoc
                } else if (ServerNode.requestQueue.get(requestID).getStatus() == Status.CANCELLED) {
//					System.out.println("ST. Stop");
                    whileData.add("stop");
                    whileData.add(dataToSend.get(1));
                    whileData.add(dataToSend.get(2));
                    whileData.add(dataToSend.get(3));
                    whileData.add(dataToSend.get(4));
                }
                outWorker.writeObject(whileData);
//				System.out.println("ST: whileData: " + whileData);
//				System.out.println("ST: Sending status check");
                incomingData = inWorker.readLine();
                System.out.println("ST: Recieving IncomingData: " + incomingData);

                String inputArr[] = incomingData.split(" ", 2);
//				System.out.println("ST. InputArr[0]: " + inputArr[0] + ", inputArr[1]: " + inputArr[1] + ".");

                if(inputArr[0] == "BUSY") {
                    ServerNode.requestQueue.get(requestID).setStatus(Status.BUSY);
                }

                //Reset workerList
                if(inputArr[0].contains("COMPLETE") || inputArr[0].contains("CANCELLED")) {
                    ServerNode.requestQueue.get(requestID).setData(inputArr[1]);
                    ServerNode.requestQueue.get(requestID).setStatus(Status.COMPLETE);

                    System.out.println("ST. Request: " + requestID + " complete.");
                    for(int j = 0; j < ServerNode.workerList.size(); j++) {
                        System.out.println("ST. WorkerList ID: " + ServerNode.workerList.get(j).getRequestID());
                        if(ServerNode.workerList.get(j).getRequestID() == requestID) {
                            ServerNode.workerList.get(j).setStatus(Status.IDLE);
                            System.out.println("ST. Worker Status updated to IDLE.");
                            break;
                        }
                    }
                }

                //Close thread as request is complete
                if(ServerNode.requestQueue.get(requestID).getStatus() == Status.COMPLETE) {
                    Date endDate = new Date();
                    long endTime = System.currentTimeMillis();
                    ServerNode.requestQueue.get(requestID).setEndDate(endDate);
                    ServerNode.requestQueue.get(requestID).setEndTime(endTime);
                    break;
                }
            }
            outWorker.close();
            socWorker.close();

        }catch(Exception ex){
            System.out.println(ex);
        }finally{
            System.out.println("ServerThread exit!! ");
        }
    }
}