package tn.amin.mpro2.features.util.message.command.api;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import de.robv.android.xposed.XposedBridge;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class GroqAPI extends AbstractAPI {
    private static final String API_URL = "https://api.groq.com/v1/endpoint";
    private static final String PREFS_NAME = "GroqAPIPrefs";
    private static final String TOKEN_KEY = "apiKey";

    private SharedPreferences sharedPreferences;

    public GroqAPI(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String cookGroq(String prompt, List<Map<String, String>> history) {
        try {
            if (prompt.startsWith("set") && prompt.length() > 3) {
                String token = prompt.substring(3).trim();
                saveToken(token);
                return "Token set.";
            }

            String apiKey = getToken();
            if (apiKey == null || apiKey.isEmpty()) {
                return "Set token first.";
            }

            JSONObject requestData = new JSONObject();
            JSONArray messagesArray = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant");
            messagesArray.put(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messagesArray.put(userMessage);

            for (Map<String, String> data : history) {
                JSONObject message = new JSONObject(data);
                messagesArray.put(message);
            }

            requestData.put("messages", messagesArray);
            requestData.put("model", "mixtral-8x7b-32768");
            requestData.put("temperature", 1);
            requestData.put("max_tokens", 1024);
            requestData.put("top_p", 1);
            requestData.put("stop", new JSONArray());
            requestData.put("stream", false);

            JSONObject response = postData(requestData, apiKey);
            return response.toString();
        } catch (Exception e) {
            XposedBridge.log(e);
        }
        return "Error processing request.";
    }

    private JSONObject postData(JSONObject requestData, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestData.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return new JSONObject(response.toString());
        }
    }

    private void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

    private String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }
}