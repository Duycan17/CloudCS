package ServerNode;
import java.io.*;
import java.net.*;
import java.util.*;
class Client
{
    public static void main(String args[]) throws Exception
    {
        String requestID = "-1";
        int year = 2000;
        //connect to server running on local host at 9000 port number
        Socket soc=new Socket("localhost",9010);
        //open an input buffer to read data from socket
        DataOutputStream out=new DataOutputStream(soc.getOutputStream());
        BufferedReader in=new BufferedReader(new InputStreamReader(soc.getInputStream()  ));

        Scanner input = new Scanner(System.in);
        System.out.println("Are you a new user?");
        String yesOrNo = input.nextLine();
        //Send userID
        System.out.println("Client. 1");
        out.writeBytes("" + yesOrNo + "\n");
        System.out.println("Client. 2");
        //Server replies with get userID and password
        String authReturn = in.readLine();
        System.out.println("Client. 3");
        System.out.println("AuthReturn: " + authReturn);
        System.out.println("Please enter your UserID.");
        String userID = input.nextLine();
        System.out.println("Please enter password.");
        String password = input.nextLine();
        System.out.println("Please enter request type.");
        System.out.println("1. Average Month Max");
        System.out.println("2. Average Month Min");
        System.out.println("3. Average Year Max");
        System.out.println("4. Average Year Min");
        System.out.println("5. Find Month High");
        System.out.println("7. Stop");
        System.out.println("6. Find Month Low");
        System.out.println("7. Stop");
        System.out.println("8. Status");

        String request = input.nextLine();
        System.out.println("Please enter stationID.");
        String stationID = input.nextLine();
        if(request.contains("status") || request.contains("stop")) {
            System.out.println("Please enter requestID.");
            requestID = input.nextLine();
        }

        //Send UserID, Password, ServiceRequest and StationID
        out.writeBytes("" + userID + " " + password + " " + request + " " + stationID + " " + year + " " + requestID +"\n");

        //Receive outcome
        System.out.println("CLIENT " + in.readLine());

    }
}