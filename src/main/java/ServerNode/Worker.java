package ServerNode;

import java.io.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

enum Status {
    BUSY, CANCELLED, CLOSING, COMPLETE, IDLE, INACTIVE, ERROR
};

class Work {

    Status status;
    String data;

    public Work(Status status) {
        this.status = status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Status getStatus() {
        return status;
    }

    public String getData() {
        return data;
    }
}

class Worker {

    static Work newWork = new Work(Status.IDLE);

    public static void main(String args[]) throws Exception {

        // To split line
        String splitBy = ",";

        while (true) {
            // Creates socket for connection
            Socket soc = new Socket("localhost", 9020);

            // Input Output stream creation, note: "Data" to send back to server, "Object"
            // to receive from server
            DataOutputStream out = new DataOutputStream(soc.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(soc.getInputStream());

            // Initial connection
            // Reads in data object from the server
            System.out.println("Worker: Recieved Object");
            Object rawInput = objectInput.readObject();

            // Converts it back to a String array to work with
            // Dividing in data for use
            String line = rawInput.toString();
            String lineArray[] = line.split(splitBy);

            // Download stuff here

            // Create thread using Work objects i.e.
            List<String> dataToSend = new ArrayList<>();
            dataToSend.add(lineArray[0]); // Request type
            dataToSend.add(lineArray[1]); // StationID
            dataToSend.add(lineArray[2]); // Year
            dataToSend.add(lineArray[3]); // Data from download
            WorkerThread sct = new WorkerThread(dataToSend);
            sct.start();
            newWork.setStatus(Status.BUSY);
            System.out.println("Worker: Created Thread.");
            System.out.println("Worker RequestType: " + lineArray[0] + ", stationID: " + lineArray[1] + ", ipAddress: "
                    + lineArray[2] + ", dataLoc: " + lineArray[3]);

            // Sends data back to the server
            out.writeBytes("BUSY Work started on Request: " + lineArray[0] + "\n");
            Thread.sleep(11000);
            while (true) {
                // Reads in data object from the serverThread
                Object rawInput1 = objectInput.readObject();
                // Converts it back to a String array to work with
                // Dividing in data for use
                line = rawInput1.toString();
                System.out.println("While RequestType: " + line);
                String whileArray[] = line.split(splitBy);
                System.out.println("Worker. Status: " + newWork.getStatus());
                if (sct.isAlive()) {
                    System.out.println("Worker. Thread is alive");
                    if (whileArray[0].contains("stop")) {
                        System.out.println("Worker. Thread has been cancelled");
                        newWork.setStatus(Status.CANCELLED);
                        out.writeBytes("CANCELLED " + "\n");
                    } else {
                        if (newWork.getStatus() == Status.BUSY) {
                            System.out.println("Worker. Thread is busy");
                            out.writeBytes("BUSY " + "\n");
                        }
                        if (newWork.getStatus() == Status.ERROR) {
                            System.out.println("Worker. Thread has errored");
                            out.writeBytes("ERROR " + "\n");
                        }
                    }
                } else {
                    System.out.println("Worker. Thread is not alive");
                    if (newWork.getStatus() == Status.COMPLETE) {
                        System.out.println("Worker. Thread is complete");

                        out.writeBytes("COMPLETE " + newWork.getData() + "\n");
                    }
                    if (newWork.getStatus() == Status.ERROR) {
                        System.out.println("Worker. Thread has errored");
                        out.writeBytes("ERROR " + "\n");
                    }
                }
                if (newWork.getStatus() == Status.COMPLETE || newWork.getStatus() == Status.CANCELLED) {
                    System.out.println("Closing WorkerThread");
                    // Cleanup
                    sct.sleep(1000);
                    sct.stop();
                    newWork.setStatus(Status.IDLE);
                    newWork.setData("");
//					out.close();
//					soc.close();
                    break;
                }
            }
        }
    }
}