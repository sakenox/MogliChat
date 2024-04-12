package com.codecademy.chatbot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private RecyclerView chatsRV;
    private EditText userMsgEdit;
    private FloatingActionButton sendMsgFAB;
    private FloatingActionButton clearChatButton;
    private final String BOT_KEY = "bot";
    private final String USER_KEY = "user";
    private ArrayList<ChatsModal> chatsModalArrayList;
    private ChatRVAdapter chatRVAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatsRV = findViewById(R.id.idRVChats);
        userMsgEdit = findViewById(R.id.idEditMessage);
        sendMsgFAB = findViewById(R.id.idFABSend);
        clearChatButton = findViewById(R.id.clearChatButton);
        chatsModalArrayList = new ArrayList<>();
        chatRVAdapter = new ChatRVAdapter(chatsModalArrayList, this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        chatsRV.setLayoutManager(manager);
        chatsRV.setAdapter(chatRVAdapter);

        SharedPreferences sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", null);

        // If UID doesn't exist, generate a new one and save it
        if (uid == null) {
            uid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("uid", uid);
            editor.apply();
        }

        loadChatHistoryFromSharedPreferences();

        sendMsgFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userMsgEdit.getText().toString().isEmpty()){
                    Toast.makeText(MainActivity.this, "Please enter your message.", Toast.LENGTH_SHORT).show();
                    return;
                }
                getResponse(userMsgEdit.getText().toString());
                userMsgEdit.setText("");

            }
        });

        clearChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearChatHistory();
            }
        });
    }

    private void getResponse(String message){
        chatsModalArrayList.add(new ChatsModal(message, USER_KEY));
        saveMessageToSharedPreferences(message, USER_KEY);

        SharedPreferences sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", null);

        chatRVAdapter.notifyDataSetChanged();
        String url = "http://api.brainshop.ai/get?bid=181394&key=acnIib5zE3iOGpST&uid=" + uid + "&msg="+message;
        String BASE_URL = "http://api.brainshop.ai";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RetroFitAPI retroFitAPI = retrofit.create(RetroFitAPI.class);
        Call<MsgModal> call = retroFitAPI.getMessage(url);
        call.enqueue(new Callback<MsgModal>() {
            @Override
            public void onResponse(Call<MsgModal> call, Response<MsgModal> response) {
                if (response.isSuccessful()) {
                    MsgModal modal = response.body();
                    chatsModalArrayList.add(new ChatsModal(modal.getCnt(), BOT_KEY));
                    saveMessageToSharedPreferences(modal.getCnt(), BOT_KEY);
                    chatRVAdapter.notifyDataSetChanged();
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(Call<MsgModal> call, Throwable t) {
                chatsModalArrayList.add(new ChatsModal(t.getMessage(),BOT_KEY));
                saveMessageToSharedPreferences(t.getMessage(), BOT_KEY);
            }
        });
        }

    private void saveMessageToSharedPreferences(String message, String sender) {
        SharedPreferences sharedPreferences = getSharedPreferences("chat_history", MODE_PRIVATE);
        // Append the new message as a separate entry in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Use a unique key for each message entry, such as the current timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        editor.putString(timestamp, sender + ":" + message);
        editor.apply();
    }

    public void scrollToBottom() {
        // Scroll to the last item in the RecyclerView
        if (chatRVAdapter.getItemCount() > 0) {
            chatsRV.smoothScrollToPosition(chatRVAdapter.getItemCount() - 1);
        }
    }



    private void loadChatHistoryFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("chat_history", MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        if (!allEntries.isEmpty()) {
            Log.d("ChatHistory", "Retrieved chat history from SharedPreferences");
            chatsModalArrayList.clear();
            List<String> messageKeys = new ArrayList<>(allEntries.keySet());
            // Sort message keys based on timestamp or sequence number
            Collections.sort(messageKeys);
            // Iterate over sorted message keys and add corresponding messages to the RecyclerView adapter
            for (String key : messageKeys) {
                String message = (String) allEntries.get(key);
                // Split each message into sender and message content
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    String sender = parts[0];
                    String messageContent = parts[1];
                    // Add the message to the RecyclerView adapter
                    chatsModalArrayList.add(new ChatsModal(messageContent, sender));
                }
            }
            // Notify the adapter that the data set has changed
            chatRVAdapter.notifyDataSetChanged();
        } else {
            Log.d("ChatHistory", "No chat history found in SharedPreferences");
        }
    }

    private void clearChatHistory() {
        SharedPreferences sharedPreferences = getSharedPreferences("chat_history", MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
        chatsModalArrayList.clear(); // Clear the list of chat messages in the RecyclerView adapter
        chatRVAdapter.notifyDataSetChanged(); // Notify adapter that data set has changed
        Toast.makeText(MainActivity.this, "Chat history cleared", Toast.LENGTH_SHORT).show();
        // Clear the RecyclerView adapter and notify it

    }


    @Override
    protected void onResume() {
        super.onResume();
        scrollToBottom();
    }
}