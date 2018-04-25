package com.mst.karsac.connections;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.mst.karsac.Algorithm.ChitchatAlgo;
import com.mst.karsac.GlobalApp;
import com.mst.karsac.interest.Interest;
import com.mst.karsac.messages.Messages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class FileReceiveAsyncTask extends AsyncTask<Void, Void, String> {
    public static final String TAG = FileReceiveAsyncTask.class.getSimpleName();
    private Context context;
    MessageSerializer messageSerializer;
    String role;
    TsInterestsInterface myListener;
    MessageSerializer obtained_msg;

    public FileReceiveAsyncTask(Context context, MessageSerializer messageSerializer, String role) {
        this.context = context;
        this.messageSerializer = messageSerializer;
        myListener = (TsInterestsInterface) context;
        this.role = role;
    }

    @Override
    protected String doInBackground(Void... voids) {
        MessageSerializer incoming_msg;
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(BackgroundService.PORT));

            Log.d(TAG, "Server socket openend");
            Socket client = serverSocket.accept();
            Log.d(TAG, "Client InetAddress:" + client.getInetAddress());
            InetAddress wifiClientIp = client.getInetAddress();
            ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
            incoming_msg = (MessageSerializer) ois.readObject();
            if (incoming_msg.mode.contains(MessageSerializer.INTEREST_MODE)) {
                new ChitchatAlgo().growthAlgorithm(incoming_msg.my_interests, messageSerializer.my_interests);
                obtained_msg = incoming_msg;
                Log.d(TAG, incoming_msg.my_interests.get(0).getInterest());
                if (role.contains(BackgroundService.OWNER)) {
                    BackgroundService.FileTransferAsyncTask fileTransferAsyncTask = new BackgroundService.FileTransferAsyncTask(context, wifiClientIp, messageSerializer);
                    fileTransferAsyncTask.execute();
                    ois.close();
                    serverSocket.close();
                    return incoming_msg.mode + "|" + role;
                }
                if (role.contains(BackgroundService.CLIENT)) {
                    MessageSerializer message_transfer = new ChitchatAlgo().RoutingProtocol(incoming_msg.my_interests, messageSerializer.my_interests);
                    BackgroundService.FileTransferAsyncTask fileTransferAsyncTask = new BackgroundService.FileTransferAsyncTask(context, wifiClientIp, message_transfer);
                    fileTransferAsyncTask.execute();
                    ois.close();
                    serverSocket.close();
                    return incoming_msg.mode + "|" + role;
                }
            } else if (incoming_msg.mode.contains(MessageSerializer.MESSAGE_MODE)) {
                List<ImageMessage> received_msgs = incoming_msg.my_mesages;
                UpdateDbandSetImage(received_msgs);
                if(role.contains(BackgroundService.OWNER)){
                    MessageSerializer my_serialized_interest = myListener.getTsInterests();
                    MessageSerializer message_transfer = new ChitchatAlgo().RoutingProtocol(messageSerializer.my_interests, my_serialized_interest.my_interests);
                    BackgroundService.FileTransferAsyncTask fileTransferAsyncTask = new BackgroundService.FileTransferAsyncTask(context, wifiClientIp, message_transfer);
                    fileTransferAsyncTask.execute();
                }
                ois.close();
                serverSocket.close();
                return incoming_msg.mode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void UpdateDbandSetImage(List<ImageMessage> received_msgs) {
        File imagesFolder = new File(Environment.getExternalStorageDirectory(), "DTN-Images");
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }
        for(ImageMessage imageMessage : received_msgs){
            Messages img_msg = imageMessage.messages;
            Log.d(TAG, "obtained file name:" + img_msg.fileName);
            File image = new File(imagesFolder, img_msg.fileName);
            decodeBase64String(imageMessage.img_path, image);
            img_msg.imgPath = image.getAbsolutePath();
            GlobalApp.dbHelper.insertImageRecord(img_msg);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d("Inside Post", "Inside On onPostExecute");
        if (result != null) {
            if (result.contains("|")) {
                String[] results = result.split("|");
                if (results[0].contains(MessageSerializer.INTEREST_MODE) && results[1].contains(BackgroundService.OWNER)) {
                    FileReceiveAsyncTask fileReceiveAsyncTask = new FileReceiveAsyncTask(context, obtained_msg, results[1]);
                    fileReceiveAsyncTask.execute();
                } else if (results[0].contains(MessageSerializer.INTEREST_MODE) && results[1].contains(BackgroundService.OWNER)) {
                    FileReceiveAsyncTask fileReceiveAsyncTask = new FileReceiveAsyncTask(context, obtained_msg, results[1]);
                    fileReceiveAsyncTask.execute();
                }
            }
            else if(result.contains(MessageSerializer.MESSAGE_MODE)){
                myListener.notifyComplete();
            }
        }
    }

    public void decodeBase64String(String img_string, File image){
        try (FileOutputStream imageOutFile = new FileOutputStream(image)){
            // Converting a Base64 String into Image byte array
            byte[] imageByteArray = Base64.decode(img_string, Base64.DEFAULT);
            Log.d(TAG, "Print the received img:" + imageByteArray);
            imageOutFile.write(imageByteArray);
        } catch (FileNotFoundException e) {
            System.out.println("Image not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading the Image " + ioe);
        }
    }
}