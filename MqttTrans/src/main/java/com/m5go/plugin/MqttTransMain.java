package com.m5go.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.command.BlockingCommand;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.JCommandManager;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.message.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import net.mamoe.mirai.message.data.Image;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.QoS;
import org.jetbrains.annotations.NotNull;

class MqttTransMain extends PluginBase {
    MqttServer mqtt;
    Bot mybot;
    boolean responseflag;
    boolean enableflag;
    boolean acMode; //空调模式
    Config setting;
    Set<Long> managerSet;
    String acForm = "{\"cmd\":%d,\"power\":%d,\"mode\":%d,\"tmp\":%d,\"fan\":%d}";

    //空调操作
    void sendAcCmd(Friend sender, int c, int p, int m, int t, int f) {
        String cmd = String.format(acForm, c, p, m, t, f);
        mqtt.connection.publish("m5go/cmd/ac", cmd.getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                responseflag = true;
            }

            @Override
            public void onFailure(Throwable value) {
                sender.sendMessage("操作失败");
            }
        });
    }

    public void onLoad() {
        getLogger().info("MqttPlugin loaded!");
        setting = loadConfig("manager.yml");
        if (!setting.contains("manager")) {
            managerSet = new HashSet<>();
            managerSet.add((long) 1694957926);
            setting.set("manager", managerSet);
            setting.save();
        } else {
            managerSet = new HashSet<>(setting.getLongList("manager"));
        }
    }

    public void onEnable() {
        getLogger().info("MQTTPlugin enabled!");
        responseflag = false;
        enableflag = false;
        acMode = false;

        String usage = "/m help: 显示帮助信息\n" +
                "/m enable: 启动服务\n" +
                "/m 查询：查询实时环境数据\n" +
                "/m 空调：开启空调遥控模式\n" +
                "/m 联系人：查看紧急联系人列表\n" +
                "/m 新增联系人 [QQ号] [QQ号] ...\n" +
                "/m 天气 [城市]：设置天气定位\n" +
                "历史记录：查看过去24小时温湿度变化图";
        JCommandManager.getInstance().register(this,
                new BlockingCommand("m", new ArrayList<>(), "m5go 状态查询", usage) {
                    @Override
                    public boolean onCommandBlocking(@NotNull CommandSender commandSender, @NotNull List<String> list) {
                        if (list.size() < 1) {
                            return false;
                        }
                        switch (list.get(0)) {
                            case "help":
                                commandSender.sendMessageBlocking(usage);
                                break;
                            case "enable":
                                if (enableflag) {
                                    commandSender.sendMessageBlocking("MQTT已启动");
                                    return true;
                                }
                                enableflag = true;
                                mqtt = new MqttServer();
                                mybot = Bot.getInstance(1246562471);
                                mqtt.connection.listener(new Listener() {
                                    @Override
                                    public void onConnected() {
                                    }

                                    @Override
                                    public void onDisconnected() {
                                    }

                                    @Override
                                    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
                                        //接收订阅信息
                                        String topi = new String(topic.toByteArray());
                                        String msg = "";
                                        getLogger().debug(topi);
                                        //环境反馈
                                        switch (topi) {
                                            case "m5go/response/env": {     //环境反馈
                                                if (!responseflag) return;
                                                JsonObject resObj = JsonParser.parseString(new String(body.toByteArray())).getAsJsonObject();
                                                float tmp = resObj.get("tmp").getAsFloat();
                                                float hum = resObj.get("hum").getAsFloat();
                                                float press = resObj.get("press").getAsFloat();
                                                responseflag = false;
                                                msg = String.format("查询成功！\n当前温度：%.1f℃\n湿度：%.1f%%\n气压：%.1fkPa", tmp, hum, press);
                                                getLogger().debug(msg);
                                                break;
                                            }
                                            case "m5go/response/ac": {       //空调反馈
                                                if (!responseflag) return;
                                                String payload = new String(body.toByteArray());
                                                JsonObject resObj = JsonParser.parseString(payload).getAsJsonObject();
                                                int power = resObj.get("power").getAsInt();
                                                int mode = resObj.get("mode").getAsInt();
                                                int tmp = resObj.get("tmp").getAsInt();
                                                int fan = resObj.get("fan").getAsInt();
                                                float realtmp = resObj.get("realtmp").getAsFloat();

                                                String form = "空调已%s\n当前模式：%s\n设定温度：%d℃\n当前温度：%.1f℃\n风速：%s";

                                                String p = power == 1 ? "开启" : "关闭";
                                                String m = "";
                                                if (mode == 0) {
                                                    m = "自动";
                                                } else if (mode == 1) {
                                                    m = "制冷";
                                                } else if (mode == 2) {
                                                    m = "除湿";
                                                } else if (mode == 3) {
                                                    m = "送风";
                                                } else if (mode == 4) {
                                                    m = "制热";
                                                }

                                                String f = "";
                                                if (fan == 0) {
                                                    f = "自动";
                                                } else if (fan == 1) {
                                                    f = "低";
                                                } else if (fan == 2) {
                                                    f = "中";
                                                } else if (fan == 3) {
                                                    f = "高";
                                                }

                                                msg = String.format(form, p, m, tmp, realtmp, f);
                                                responseflag = false;
                                                break;
                                            }
                                            case "m5go/direct":
                                                String s = new String(body.toByteArray());
                                                if (s.equals("warning")) {
                                                    SimpleDateFormat fmt = new SimpleDateFormat("[MM-dd HH:mm:ss]");
                                                    String timestr = fmt.format(System.currentTimeMillis());
                                                    msg += timestr + "M5GO 触发警报！！！";
                                                } else if (s.equals("offline")) {
                                                    msg = "M5GO 设备已离线";
                                                } else {
                                                    msg = s;
                                                }
                                                break;
                                            case "m5go/env":
                                                MqttServer.saveToDB(new String(body.toByteArray()));
                                                break;
                                            case "m5go/test":
                                                msg = new String(body.toByteArray());
                                                break;
                                            default:
                                                return;
                                        }
                                        if (!msg.isEmpty()) {
                                            mybot.getFriend(1694957926).sendMessage(msg);
                                        }
                                        ack.run();
                                    }

                                    @Override
                                    public void onFailure(Throwable value) {
                                    }
                                });
                                commandSender.sendMessageBlocking("MQTT推送服务启动成功");

                                break;
                            case "查询":
                                if (!enableflag) {
                                    commandSender.sendMessageBlocking("服务尚未启动，查询失败\n输入/m enable启动服务");
                                    return true;
                                }
                                try {
                                    mqtt.connection.publish("m5go/cmd/env", "get".getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            responseflag = true;
                                        }

                                        @Override
                                        public void onFailure(Throwable value) {
                                            commandSender.sendMessageBlocking("查询失败");
                                        }
                                    });
//                                commandSender.sendMessageBlocking(mqtt.getMessage());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    commandSender.sendMessageBlocking(e.toString());
                                }
                                break;
                            case "空调":
                                if (!enableflag) {
                                    commandSender.sendMessageBlocking("服务尚未启动，操作失败\n输入/m enable启动服务");
                                    return true;
                                }
                                try {
                                    mqtt.connection.publish("m5go/cmd/ac", "{\"cmd\":0}".getBytes(), QoS.AT_MOST_ONCE, false, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            responseflag = true;
                                            acMode = true;
                                            commandSender.sendMessageBlocking("开启空调设置模式，退出请回复”退出“\n" +
                                                    "操作指令：\n[开启|关闭]\n[17-30] 设置温度\n[制冷|制热|除湿|送风|自动] 设置模式\n[高风|中风|低风|自动风] 设置风速");
                                        }

                                        @Override
                                        public void onFailure(Throwable value) {
                                            value.printStackTrace();
                                            commandSender.sendMessageBlocking("操作失败");
                                        }
                                    });


                                } catch (Exception e) {
                                    e.printStackTrace();
                                    commandSender.sendMessageBlocking(e.toString());
                                }
                                break;
                            case "新增联系人":
                                int listsize = list.size();
                                if (listsize < 2) {
                                    commandSender.sendMessageBlocking("指令格式错误！\n/m 新增联系人 1234 3456 ...");
                                    break;
                                }
                                for (int i = 1; i < listsize; i++) {
                                    try {
                                        long account = Long.parseLong(list.get(i));
                                        managerSet.add(account);
                                    } catch (NumberFormatException e) {
                                        commandSender.sendMessageBlocking("指令格式错误！\n/m 新增联系人 1234 3456 ...");
                                        break;
                                    }
                                }
                                commandSender.sendMessageBlocking("新增紧急联系人成功！");
                                break;
                            case "联系人":
                                String backmsg = "紧急联系人列表：\n";
                                for (Long s : managerSet) {
                                    backmsg += String.valueOf(s) + "\n";
                                }
                                commandSender.sendMessageBlocking(backmsg);
                                break;
                            case "天气":
                                if (list.size() != 2) {
                                    commandSender.sendMessageBlocking("指令格式错误！\n/m 天气 [城市]");
                                    break;
                                }
                                String city = list.get(1);
                                mqtt.connection.publish("m5go/cmd/city", city.getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        commandSender.sendMessageBlocking("天气定位已修改：" + city);
                                    }

                                    @Override
                                    public void onFailure(Throwable value) {
                                        commandSender.sendMessageBlocking("操作失败");
                                    }
                                });
                                break;
                            default:
                                commandSender.sendMessageBlocking(usage);
                                break;
                        }
                        return true;
                    }
                }
        );
        this.getEventListener().subscribeAlways(FriendMessageEvent.class, (FriendMessageEvent event) -> {
            Friend sender = event.getSender();
            String msg = event.getMessage().contentToString();
            if (managerSet.contains(sender.getId())) {
                if (!enableflag) {
                    sender.sendMessage("服务尚未启动，操作失败\n输入/m enable启动服务");
                    return;
                }
                if (acMode) {
                    if (msg.equals("退出")) {
                        acMode = false;
                        sender.sendMessage("已退出空调设置模式");
                    } else if (msg.contains("开")) {
                        sendAcCmd(sender, 1, 1, -1, -1, -1);
                    } else if (msg.contains("关")) {
                        sendAcCmd(sender, 1, 0, -1, -1, -1);
                    } else if (msg.equals("自动")) {
                        sendAcCmd(sender, 1, -1, 0, -1, -1);
                    } else if (msg.equals("制冷")) {
                        sendAcCmd(sender, 1, -1, 1, -1, -1);
                    } else if (msg.equals("除湿")) {
                        sendAcCmd(sender, 1, -1, 2, -1, -1);
                    } else if (msg.equals("送风")) {
                        sendAcCmd(sender, 1, -1, 3, -1, -1);
                    } else if (msg.equals("制热")) {
                        sendAcCmd(sender, 1, -1, 4, -1, -1);
                    } else if (msg.equals("自动风")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 0);
                    } else if (msg.equals("低风")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 1);
                    } else if (msg.equals("中风")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 2);
                    } else if (msg.equals("高风")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 3);
                    } else if (msg.matches("\\d+")) {
                        int i = Integer.parseInt(msg);
                        if (i < 17 || i > 30) {
                            sender.sendMessage("输入错误！温度范围：17~30");
                        } else {
                            sendAcCmd(sender, 1, -1, -1, i, -1);
                        }
                    }
                }
                if (msg.equals("历史记录")) {
                    String path = JChart.createPic(this.getDataFolder().getAbsolutePath() + "/env/");
                    Image img = sender.uploadImage(new File(path));
                    sender.sendMessage(img);
                }
            }

        });
    }

}
