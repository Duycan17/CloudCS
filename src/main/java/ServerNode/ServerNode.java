package ServerNode;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.Base64;
import java.util.List;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import ServerNode.ServerNode.Status;

public class ServerNode {
    OSClientV3 os = null;
    static List<String> stationList = new ArrayList<>();
    static List<Request> requestQueue = new ArrayList<>();
    static List<WorkerInstance> workerList = new ArrayList<>();
    static List<User> userIDList = new ArrayList<>();

    // For both requests (CREATED, BUSY, COMPLETE, CANCELLED) and worker instances (BUSY, CLOSING, IDLE,
    // INACTIVE)
    enum Status {
        BUSY, CANCELLED, CLOSING, CREATED, COMPLETE, IDLE, INACTIVE, ERROR
    };

    static class Request {
        int id; // Id of request
        String userID; // To associate the user with the request
        String workerInstance; // To associate the workerInstance working on the job
        String name; // Type of request
        Status status; // Current status of the request
        String data; // Return value
        int priority;
        Date startDate;
        Date endDate;
        long startTime;
        long endTime;

        public Request(int counter, String userID, Date startDate, long startTime, Status status) {
            this.id = counter;
            this.userID = userID;
            this.startDate = startDate;
            this.startTime = startTime;
            this.status = status;
        }

        public void setWorkerInstance(String workerInstance) {
            this.workerInstance = workerInstance;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public void setData(String data) {
            this.data = data;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public int getID() {
            return id;
        }

        public int getPriority() {
            return priority;
        }

        public String getWorkerInstance() {
            return workerInstance;
        }

        public String getName() {
            return name;
        }

        public Status getStatus() {
            return status;
        }

        public String getData() {
            return data;
        }

        public Date getStartDate() {
            return startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }

    static class WorkerInstance {

        String name;
        int requestID;
        Status status;
        String ipAddress;
        String serverID;
        ServerNodeThread sct;

        public WorkerInstance(String name, Status status) {
            this.name = name;
            this.status = status;
        }

        public void setRequestID(int requestID) {
            this.requestID = requestID;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public void setIPAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public void setServerID(String serverID) {
            this.serverID = serverID;
        }

        public void setThread(ServerNodeThread sct) {
            this.sct = sct;
        }

        public int getRequestID() {
            return requestID;
        }

        public String getName() {
            return name;
        }

        public Status getStatus() {
            return status;
        }

        public String getIPAddress() {
            return ipAddress;
        }

        public String getServerID() {
            return serverID;
        }

        public ServerNodeThread getThread() {
            return sct;
        }
    }

    static class User {

        String userID;
        String password;

        public User(String userID, String password) {
            this.userID = userID;
            this.password = password;
        }

        public void setuserID(String userID) {
            this.userID = userID;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUserID() {
            return userID;
        }

        public String getPassword() {
            return password;
        }
    }

    public static void ServerStartup(int workerInstanceNum, String userName, String password, String projectID,
                                     String imageID, String securityGroupID) {
        ServerNode openstack = new ServerNode(userName, password, projectID);// Build the -openstack client and
        // authenticate
        openstack.getflavors();
        String[] serverDets = openstack.createServer(imageID, securityGroupID);// Creating a new VM
        String serverIP = serverDets[0];
        String serverid = serverDets[1];
        String test2 = openstack.getIP(serverid);
        workerList.get(workerInstanceNum).setIPAddress(serverIP);
        workerList.get(workerInstanceNum).setServerID(serverid);
        System.out.println("IP " + serverIP + " " + test2);
        System.out.println(" Successfully Created Virtual Machine(VM) with server id" + serverid
                + " and temp folder inside VM Please log in to nectar cloud to verify ");

    }

    public ServerNode(String userName, String password, String projectID) {
        os = OSFactory.builderV3()// Setting up openstack client with -OpenStack factory
                .endpoint("https://keystone.rc.nectar.org.au:5000/v3")// Openstack endpoint
                .credentials(userName, password, Identifier.byName("Default"))// Passing
                // credentials
                .scopeToProject(Identifier.byId(projectID))// Project id
                .authenticate();// verify the authentication
    }

    // Creating a new instance or VM or Server
    public String[] createServer(String imageID, String securityGroupID) {
        String script = Base64.getEncoder().encodeToString(
                ("#!/bin/bash\n" + "cd /home/ubuntu \n" + "sudo javac Worker.java \n" + "sudo java Worker \n")
                        .getBytes());// encoded with Base64. Creates a temporary directory
        ServerCreate server = Builders.server()// creating a VM server
                .name("Assignment2Worker")// VM or instance name
                .flavor("406352b0-2413-4ea6-b219-1a4218fd7d3b")// flavour id
                .image(imageID)// -image id
                .keypairName("assignment2")// key pair name
                .addSecurityGroup(securityGroupID) // Security group ID (allow SSH)
                .userData(script).build();// build the VM with above configuration

        Server booting = os.compute().servers().boot(server);
        String ipAddress = booting.getAccessIPv4();
        String id = booting.getId();
        return new String[] { ipAddress, id };
    }

    public String getIP(String serverid) {
        String ip = os.compute().servers().get(serverid).getAccessIPv4();
        while (ip == null || ip.length() == 0) {
            try {
                Thread.sleep(1000);
                // System.out.println("Waiting");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ip = os.compute().servers().get(serverid).getAccessIPv4();
        }
        return ip;
    }

    public void getflavors() {
        List<? extends Flavor> flavors = os.compute().flavors().list();
        for (Flavor f : flavors) {
            System.out.println(f.getId().toString() + " " + f.getName());
        }

    }

    // Delete a Server
    public void deleteServer(String serverid) {
        os.compute().servers().delete(serverid);// delete the VM orserver
    }

    public static boolean checkForUserIDs(String userID, String password) {
        for (int i = 0; i < userIDList.size(); i++) {
//			System.out.println("UserIDList: " + userIDList.get(i).getUserID() + ", userID: " + userID);
//			System.out.println("UserIDListPassword: " + userIDList.get(i).getPassword() + ", password: " + password);
            if (userIDList.get(i).getUserID().equals(userID) && userIDList.get(i).getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final int LENGTH = 6;
    public static String generateRandomString() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }
    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        // Create 4 base workers // Need to finish this with everyones instances
//		ServerStartup(1, "lbos@utas.edu.au", "NGM3NTU2MTI2YTczN2Nj", "e7e3cee98ded4a42aa606e0e677f1022", null, null); //Last two are imageID & SecurityGroupID
//		ServerStartup(2, "lbos@utas.edu.au", "NGM3NTU2MTI2YTczN2Nj", "e7e3cee98ded4a42aa606e0e677f1022", null, null); //Last two are imageID & SecurityGroupID
//		ServerStartup(3, "lbos@utas.edu.au", "NGM3NTU2MTI2YTczN2Nj", "e7e3cee98ded4a42aa606e0e677f1022", null, null); //Last two are imageID & SecurityGroupID
//		ServerStartup(4, "lbos@utas.edu.au", "NGM3NTU2MTI2YTczN2Nj", "e7e3cee98ded4a42aa606e0e677f1022", null, null); //Last two are imageID & SecurityGroupID
//		ServerStartup(5, "lbos@utas.edu.au", "NGM3NTU2MTI2YTczN2Nj", "e7e3cee98ded4a42aa606e0e677f1022", null, null); //Last two are imageID & SecurityGroupID

        // Creates WorkerInstance objects
        WorkerInstance instanceOne = new WorkerInstance("lukeOne", Status.IDLE);
        WorkerInstance instanceTwo = new WorkerInstance("manOne", Status.IDLE);
        WorkerInstance instanceThree = new WorkerInstance("manTwo", Status.IDLE);
        WorkerInstance instanceFour = new WorkerInstance("ducOne", Status.IDLE);
        WorkerInstance instanceFive = new WorkerInstance("ducTwo", Status.IDLE);

        workerList.add(instanceOne);
        workerList.add(instanceTwo);
//		workerList.add(instanceThree);
//		workerList.add(instanceFour);
//		workerList.add(instanceFive);

        // for testing
        workerList.get(1).setIPAddress("192.168.0.2");

        // To divide each line
        String line = "";
        String splitBy = ",";
        String serverIP = "0.0.0.0";
        String serverDataLoc = "/home/ubuntu/ImAFakeAddress.csv";

        System.out.println("Server. Starting...");

        // Actual sending of the data
        try {
            ServerSocket serverClient = new ServerSocket(9010); // For client/worker connection
            ServerSocket serverWorker = new ServerSocket(9020); // For client/worker connection
            int counter = 0;
            System.out.println("Server. Server Started .... waiting for clients connection.");
            while (true) {
                // accept connection
                Socket socClient = serverClient.accept();
                DataOutputStream outClient = new DataOutputStream(socClient.getOutputStream());
                BufferedReader inClient = new BufferedReader(new InputStreamReader(socClient.getInputStream()));
                String userID = "123";
                // New user authenticate
                String firstAuth = "" + inClient.readLine() + "\n";
                String inputArrAuth[] = firstAuth.split(" ", 2);
                if (inputArrAuth[0].contains("yes") || inputArrAuth[0].contains("Yes")) {
                    int min = 1000;
                    int max = 10000;
                    int newUserID = (int) Math.floor(Math.random() * (max - min + 1) + min);
                    newUserID = Integer.valueOf(String.valueOf(newUserID) + String.valueOf(counter));
                    String password = generateRandomString();
                    String stringID = Integer.toString(newUserID);
                    User newUser = new User(stringID, password);
                    userIDList.add(newUser);
                    outClient.writeBytes("New account created. Your new UserID is: " + newUser.getUserID()
                            + ", and your password is: " + newUser.getPassword() + "\n");
                } else {
                    outClient.writeBytes("Existing account login." + "\n");
                }

                // Splits input from client into small array for switch statement
                String input = "" + inClient.readLine() + "\n";
                String inputArr[] = input.split(" ", 6);
                System.out.println("Server. Input: " + input);

                // Authenticate UserID and Password
                if (!checkForUserIDs(inputArr[0], inputArr[1])) {
                    System.out.println("Server. Failed to Authenticate.");
                    outClient.writeBytes("Failed to Authenticate." + "\n");
                    outClient.close();
                    socClient.close();
                } else {

                    //Start timer
                    long startTime = System.currentTimeMillis();
                    Date startDate = new Date();

                    // Creates new request and adds it to the requestQueue,
                    Request newRequest = new Request(counter, userID, startDate, startTime, Status.CREATED);
                    List<String> dataToSend = new ArrayList<>();
                    switch (inputArr[2]) {
                        case "1":
                            System.out.println("Server. Request created: averageMonthMax");
                            newRequest.setName("averageMonthMax");
                            newRequest.setPriority(2);
                            requestQueue.add(newRequest);
                            outClient.writeBytes("Request created. Request ID: " + counter + "\n");
                            dataToSend.add(inputArr[2]);
                            dataToSend.add(inputArr[3]);
                            dataToSend.add(inputArr[4]);
                            dataToSend.add(serverIP);
                            dataToSend.add(serverDataLoc);
                            break;
                        case "2":
                            System.out.println("Server. Request created: averageMonthMin");
                            newRequest.setName("averageMonthMin");
                            newRequest.setPriority(2);
                            requestQueue.add(newRequest);
                            outClient.writeBytes("Request created. Request ID: " + counter + "\n");
                            dataToSend.add(inputArr[2]);
                            dataToSend.add(inputArr[3]);
                            dataToSend.add(inputArr[4]);
                            dataToSend.add(serverIP);
                            dataToSend.add(serverDataLoc);
                            break;
                        case "3":
                            System.out.println("Server. Request created: averageYearMax");
                            newRequest.setName("averageYearMax");
                            newRequest.setPriority(1);
                            requestQueue.add(newRequest);
                            outClient.writeBytes("Request created. Request ID: " + counter + "\n");
                            dataToSend.add(inputArr[2]);
                            dataToSend.add(inputArr[3]);
                            dataToSend.add(inputArr[4]);
                            dataToSend.add(serverIP);
                            dataToSend.add(serverDataLoc);
                            break;
                        case "4":
                            System.out.println("Server. Request created: averageYearMin");
                            newRequest.setName("averageYearMin");
                            newRequest.setPriority(1);
                            requestQueue.add(newRequest);
                            outClient.writeBytes("Request created. Request ID: " + counter + "\n");
                            dataToSend.add(inputArr[2]);
                            dataToSend.add(inputArr[3]);
                            dataToSend.add(inputArr[4]);
                            dataToSend.add(serverIP);
                            dataToSend.add(serverDataLoc);
                            break;
                        case "5":
                            System.out.println("Server. Request created: findMonthHigh");
                            newRequest.setName("findMonthHigh");
                            newRequest.setPriority(2);
                            requestQueue.add(newRequest);
                            outClient.writeBytes("Request created. Request ID: " + counter + "\n");
                            dataToSend.add(inputArr[2]);
                            dataToSend.add(inputArr[3]);
                            dataToSend.add(inputArr[4]);
                            dataToSend.add(serverIP);
                            dataToSend.add(serverDataLoc);
                            break;
                        case "6":
                            System.out.println("Server. Request created: findMonthLow");
                            newRequest.setName("findMonthLow");
                            newRequest.setPriority(2);
                            requestQueue.add(newRequest);
                            outClient.writeBytes("Request created. Request ID: " + counter + "\n");
                            dataToSend.add(inputArr[2]);
                            dataToSend.add(inputArr[3]);
                            dataToSend.add(inputArr[4]);
                            dataToSend.add(serverIP);
                            dataToSend.add(serverDataLoc);
                            break;
                        case "7":
                            System.out.println("Server. Request: stop");
                            int requestIDStop = Integer.parseInt(inputArr[5].trim());
                            requestQueue.get(requestIDStop).setStatus(Status.CANCELLED);
                            outClient.writeBytes("Request cancelled. Request ID: " + requestIDStop + "\n");
                            //To keep counter and requestID's lined up
                            counter--;
                            break;
                        case "8":
                            System.out.println("Server. Request: status");
                            int requestIDStatus = Integer.parseInt(inputArr[5].trim());
                            //If complete
                            if(requestQueue.get(requestIDStatus).getStatus() == Status.COMPLETE) {
                                long duration = (requestQueue.get(requestIDStatus).getEndTime() - requestQueue.get(requestIDStatus).getStartTime()) / 1000;
                                double cost = duration * 1.2;
                                outClient.writeBytes("Request status. Request ID: " + requestQueue.get(requestIDStatus).getStatus()
                                        + ", Result: " + requestQueue.get(requestIDStatus).getData()
                                        + ", StartDate: " + requestQueue.get(requestIDStatus).getStartDate()
                                        + ", EndDate: " + requestQueue.get(requestIDStatus).getEndDate()
                                        + ", Duration: " + duration
                                        + ", Cost: $" + cost + "\n");
                            } else {
                                //Not complete
                                outClient.writeBytes("Request status. Request ID: " + requestQueue.get(requestIDStatus).getStatus() + "\n");
                            }

                            //To keep counter and requestID's lined up
                            counter--;
                            break;
                    }
                    // Close clients connection
                    outClient.close();
                    socClient.close();

                    // If the request is not a status check, create thread and send dataToSend to it
                    // for work
                    if (inputArr[2].contains("status") || inputArr[2].contains("stop")) {
                    } else {
                        // Check the workerList for available workers
                        for (int i = 0; i < workerList.size(); i++) {
                            if (workerList.get(i).status == Status.IDLE) {
                                System.out.println("Server. Found available worker: " + workerList.get(i).getName());

                                for(int priority = 1; priority < 4; priority++) {
                                    // Goes through requestQueue for next task
                                    for (int j = 0; j < requestQueue.size(); j++) {
//										System.out.println("Priority: " + priority);
                                        //Time consuming priority
                                        if(priority == requestQueue.get(j).getPriority()) {
                                            if(requestQueue.get(j).getStatus() == Status.CREATED) {
                                                System.out.println("Server. Found request. ID: " + requestQueue.get(j).getID());
                                                // Set associations for workerlist and requestqueue
                                                workerList.get(i).setRequestID(requestQueue.get(j).getID());
                                                requestQueue.get(j).setWorkerInstance(workerList.get(i).getName());
                                                ServerNode.workerList.get(i).setStatus(Status.BUSY);
                                                ServerNode.requestQueue.get(j).setStatus(Status.BUSY);

                                                // Create ServerNodeThread
                                                Socket socWorker = serverWorker.accept();
                                                ServerNodeThread sct = new ServerNodeThread(socWorker, dataToSend,
                                                        requestQueue.get(j).getID()); // send the request to a separate thread
                                                workerList.get(i).setThread(sct);
                                                sct.start();
                                                break;
                                            }
                                        }
                                    }
                                }
                                break;
                                // If no available workers, create new instance
                            } else if (i == workerList.size() - 1) {
//						WorkerInstance instanceSix  = new WorkerInstance("edOne", Status.IDLE);
//						WorkerInstance instanceSeven  = new WorkerInstance("edTwo", Status.IDLE);
//						workerList.add(instanceSix);
//						workerList.add(instanceSeven);
                                // Extra code here
                                System.out.println("Server. Put scalable code here, initiate extra instances.");
                            }
                        }
                    }

                    counter++;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}