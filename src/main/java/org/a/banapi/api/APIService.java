package org.a.banapi.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.a.banapi.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class APIService {
    private final ConfigManager configManager;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public APIService(ConfigManager configManager) {
        this.configManager = configManager;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 获取封禁列表（使用x-api-key认证）
     * @return 封禁列表数据
     * @throws IOException 如果API请求失败
     */
    public List<Map<String, Object>> getBans() throws IOException {
        String url = configManager.getApiUrl() + "/bans";
        String apiKey = configManager.getApiKey();

        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取封禁列表失败，状态码: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            return gson.fromJson(body.string(), listType);
        }
    }

    /**
     * 检查玩家是否被封禁
     * @param playerName 玩家名称
     * @return 如果玩家被封禁则返回true，否则返回false
     * @throws IOException 如果API请求失败
     */
    public boolean checkBan(String playerName) throws IOException {
        String url = configManager.getApiUrl() + "/bans/" + playerName;
        String apiKey = configManager.getApiKey();

        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    return false; // 玩家未被封禁
                }
                throw new IOException("检查封禁状态失败，状态码: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            Map<String, Object> result = gson.fromJson(body.string(), Map.class);
            return result.containsKey("banned") && (boolean) result.get("banned");
        }
    }
    
    /**
     * 获取玩家的封禁详情
     * @param playerName 玩家名称
     * @return 封禁详情，如果玩家未被封禁则返回null
     * @throws IOException 如果API请求失败
     */
    public Map<String, Object> getBanDetails(String playerName) throws IOException {
        // 首先尝试通过玩家名称查询
        Map<String, Object> banInfo = getBanDetailsByName(playerName);
        if (banInfo != null) {
            return banInfo;
        }

        // 如果通过名称查询不到，尝试获取玩家ID并再次查询
        String playerId = getPlayerId(playerName);
        if (playerId != null) {
            return getBanDetailsById(playerId);
        }

        return null;
    }

    private Map<String, Object> getBanDetailsByName(String playerName) throws IOException {
        String url = configManager.getApiUrl() + "/bans/name/" + playerName;
        return executeBanApiRequest(url, playerName);
    }

    private Map<String, Object> getBanDetailsById(String playerId) throws IOException {
        String url = configManager.getApiUrl() + "/bans/id/" + playerId;
        return executeBanApiRequest(url, playerId);
    }

    private String getPlayerId(String playerName) throws IOException {
        String url = configManager.getApiUrl() + "/players/" + playerName + "/id";
        String apiKey = configManager.getApiKey();

        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }

    private Map<String, Object> executeBanApiRequest(String url, String identifier) throws IOException {
        String apiKey = configManager.getApiKey();

        System.out.println("请求封禁详情: " + url);
        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println("API响应状态码: " + response.code());

            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    System.out.println("标识符 " + identifier + " 未被封禁 (404)");
                    return null;
                }
                throw new IOException("获取封禁详情失败，状态码: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            String responseBody = body.string();
            System.out.println("API响应体: " + responseBody);

            // 解析为封禁列表（类型安全）
            Type banListType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> banList = (List<Map<String, Object>>) gson.fromJson(responseBody, banListType);
            
            if (banList == null || banList.isEmpty()) {
                System.out.println("API返回的封禁列表为空");
                return null;
            }

            // 遍历列表查找匹配的玩家
            for (Map<String, Object> banEntry : banList) {
                String nickname = (String) banEntry.get("nickname");
                Integer id = ((Double) banEntry.get("id")).intValue();
                
                if (identifier.equalsIgnoreCase(nickname) || identifier.equals(id.toString())) {
                    // 直接返回API原始数据，确保包含isReleased字段
                    return banEntry;
                }
            }

            System.out.println("未找到匹配 " + identifier + " 的封禁记录");
            // 返回一个包含isReleased=false的默认封禁记录
            Map<String, Object> defaultBan = new HashMap<>();
            defaultBan.put("isReleased", false);
            defaultBan.put("isPermanent", true);
            defaultBan.put("reason", "未知原因");
            return defaultBan;
        } catch (Exception e) {
            System.out.println("获取标识符 " + identifier + " 封禁详情时出错: " + e.getMessage());
            throw e;
        }
    }
    

    /**
     * 获取统计信息
     * @return 统计数据
     * @throws IOException 如果API请求失败
     */
    public Map<String, Object> getStats() throws IOException {
        String url = configManager.getApiUrl() + "/stats";
        String apiKey = configManager.getApiKey();

        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取统计信息失败，状态码: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            return gson.fromJson(body.string(), Map.class);
        }
    }
    
    /**
     * 添加封禁记录
     * @param nickname 玩家名称
     * @param reason 封禁原因
     * @param admin 管理员名称
     * @param isPermanent 是否永久封禁
     * @param duration 封禁时长（毫秒），仅当isPermanent为false时有效
     * @return 封禁记录数据
     * @throws IOException 如果API请求失败
     */
    public Map<String, Object> addBan(String nickname, String reason, String admin, boolean isPermanent, Long duration) throws IOException {
        String url = configManager.getApiUrl() + "/ban";
        String apiKey = configManager.getApiKey();
        
        // 构建请求体
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("nickname", nickname);
        requestMap.put("reason", reason);
        requestMap.put("admin", admin);
        requestMap.put("isPermanent", isPermanent);
        requestMap.put("duration", duration);
        String requestBody = gson.toJson(requestMap);
        
        RequestBody body = RequestBody.create(requestBody, JSON);
        
        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .post(body)
                .build();
                
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("添加封禁记录失败，状态码: " + response.code());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }
            
            return gson.fromJson(responseBody.string(), Map.class);
        }
    }
    
    /**
     * 更新封禁状态
     * @param id 封禁记录ID
     * @param isReleased 是否解除封禁
     * @return 更新后的封禁记录数据
     * @throws IOException 如果API请求失败
     */
    public Map<String, Object> updateBanStatus(int id, boolean isReleased) throws IOException {
        String url = configManager.getApiUrl() + "/ban/" + id;
        String apiKey = configManager.getApiKey();
        
        // 构建请求体
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("isReleased", isReleased);
        String requestBody = gson.toJson(requestMap);
        
        RequestBody body = RequestBody.create(requestBody, JSON);
        
        Request request = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .patch(body)
                .build();
                
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("更新封禁状态失败，状态码: " + response.code());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }
            
            return gson.fromJson(responseBody.string(), Map.class);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
