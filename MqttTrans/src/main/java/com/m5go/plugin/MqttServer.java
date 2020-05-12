package com.m5go.plugin;

import java.net.URISyntaxException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;


public class MqttServer {
    private final static String CLIENT_ID = "javaserver";
    private final static short KEEP_ALIVE = 30;// 低耗网络，但是又需要及时获取数据，心跳30s

    public MQTT mqtt;
    //    public BlockingConnection connection;
    CallbackConnection connection;

    Topic[] topics = {new Topic("m5go/response/#", QoS.AT_LEAST_ONCE),
//            new Topic("m5go/test", QoS.AT_MOST_ONCE),
            new Topic("m5go/env", QoS.AT_MOST_ONCE),
            new Topic("m5go/direct", QoS.AT_MOST_ONCE)};

    static Connection conn = null;

    //数据库地址
    static String url = "jdbc:mysql://1.1.1.1:3306/m5go";
    //MySQL配置时的用户名
    static String user = "m5go";
    //MySQL配置时的密码
    static String password = "password";
    static String driver = "com.mysql.jdbc.Driver";

    public MqttServer() {
        connect();
        mqtt = new MQTT();
        try {
            mqtt.setHost("1.1.1.1", 1883);
            mqtt.setKeepAlive(KEEP_ALIVE);
            mqtt.setClientId(CLIENT_ID);
            //服务器认证用户名
            mqtt.setUserName("server");
            //服务器认证密码
            mqtt.setPassword("password");
            connection = mqtt.callbackConnection();
            connection.connect(new Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    //订阅主题
                    connection.subscribe(topics,
                            new Callback<byte[]>() {
                                // 订阅主题成功
                                public void onSuccess(byte[] qoses) {
                                    System.out.println("========订阅成功=======");
                                }

                                // 订阅主题失败
                                public void onFailure(Throwable value) {
                                    System.out.println("========订阅失败=======");
                                    connection.disconnect(null);
                                }
                            });
                }

                @Override
                public void onFailure(Throwable value) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    String getMessage(){
//        Message message = null;
//        byte[] payload;
//        try {
//            message = connection.receive();
//            System.out.println(message.getTopic());
//            payload = message.getPayload();
//            message.ack();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return e.toString();
//        }
//        JsonObject resObj = JsonParser.parseString(new String(payload)).getAsJsonObject();
//        float tmp = resObj.get("tmp").getAsFloat();
//        float hum = resObj.get("hum").getAsFloat();
//        float press = resObj.get("press").getAsFloat();
//        String msg = String.format("查询成功！\n当前温度：%.1f℃\n湿度：%.1f%%\n气压：%.1fkPa",tmp,hum,press);
//        return msg;
//    }

    public static void main(String[] args) {
//        try {
//            MqttServer mm = new MqttServer();
//            mm.connection.listener(new Listener() {
//                @Override
//                public void onConnected() {
//                }
//
//                @Override
//                public void onDisconnected() {
//                }
//
//                @Override
//                public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
//                    String topi = new String(topic.toByteArray());
//                    System.out.println("=============receive msg================\n"
//                            + new String(topi) + new String(body.toByteArray()));
//                    ack.run();
//                }
//
//                @Override
//                public void onFailure(Throwable value) {
//                }
//            });
//            while (true) {
//
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        connect();
        HashMap<String, ArrayList<Double>> re = getData();
        System.out.println(re);
    }

    //连接数据库
    public static void connect() {
        try {
            //加载驱动程序
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connection to Mysql has been established.");

//            Statement s = conn.createStatement();
//            String sql=	"CREATE TABLE IF NOT EXISTS todolist ("
//                    +"memoID INTEGER PRIMARY KEY autoincrement,"
//                    +"title TEXT,"
//                    +"address TEXT,"
//                    +"startTime integer,"
//                    +"endTime integer,"
//                    +"detail TEXT,"
//                    +"alarm integer,"
//                    +"remindTime integer,"
//                    +"editTime integer"
//                    +")";
//            s.executeUpdate(sql);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    // 存储环境信息
    public static void saveToDB(String payload) {
        JsonObject resObj = JsonParser.parseString(payload).getAsJsonObject();
        float tmp = resObj.get("tmp").getAsFloat();
        float hum = resObj.get("hum").getAsFloat();
        float press = resObj.get("press").getAsFloat();
        long time = resObj.get("time").getAsLong() * 1000;

        PreparedStatement s;

        try {
            s = conn.prepareStatement(
                    "INSERT INTO env ("
                            + "tmp,hum,press,time)"
                            + "VALUES(?,?,?,?)");
            s.setFloat(1, tmp);
            s.setFloat(2, hum);
            s.setFloat(3, press);
            s.setTimestamp(4, new Timestamp(time));

            s.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //从数据库获取数据
    public static HashMap<String, ArrayList<Double>> getData() {
        PreparedStatement s;
        HashMap<String, ArrayList<Double>> result = new HashMap<String, ArrayList<Double>>();
        ArrayList<Double> tmp = new ArrayList<Double>();
        ArrayList<Double> hum = new ArrayList<Double>();
        ArrayList<Double> time = new ArrayList<Double>();
        long yesterday = System.currentTimeMillis() - 24 * 3600 * 1000;
        try {
            s = conn.prepareStatement(
                    "SELECT tmp,hum,time FROM env WHERE time > ?");
            s.setLong(1, yesterday);

            ResultSet rs = s.executeQuery();
            ResultSetMetaData rm = rs.getMetaData();
            while (rs.next()) {
                tmp.add(rs.getDouble(1));
                hum.add(rs.getDouble(2));
                time.add((double) rs.getTimestamp(3).getTime());
            }
            result.put("tmp", tmp);
            result.put("hum", hum);
            result.put("time", time);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
