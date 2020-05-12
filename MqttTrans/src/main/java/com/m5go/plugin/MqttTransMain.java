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
    boolean acMode; //�յ�ģʽ
    Config setting;
    Set<Long> managerSet;
    String acForm = "{\"cmd\":%d,\"power\":%d,\"mode\":%d,\"tmp\":%d,\"fan\":%d}";

    //�յ�����
    void sendAcCmd(Friend sender, int c, int p, int m, int t, int f) {
        String cmd = String.format(acForm, c, p, m, t, f);
        mqtt.connection.publish("m5go/cmd/ac", cmd.getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                responseflag = true;
            }

            @Override
            public void onFailure(Throwable value) {
                sender.sendMessage("����ʧ��");
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

        String usage = "/m help: ��ʾ������Ϣ\n" +
                "/m enable: ��������\n" +
                "/m ��ѯ����ѯʵʱ��������\n" +
                "/m �յ��������յ�ң��ģʽ\n" +
                "/m ��ϵ�ˣ��鿴������ϵ���б�\n" +
                "/m ������ϵ�� [QQ��] [QQ��] ...\n" +
                "/m ���� [����]������������λ\n" +
                "��ʷ��¼���鿴��ȥ24Сʱ��ʪ�ȱ仯ͼ";
        JCommandManager.getInstance().register(this,
                new BlockingCommand("m", new ArrayList<>(), "m5go ״̬��ѯ", usage) {
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
                                    commandSender.sendMessageBlocking("MQTT������");
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
                                        //���ն�����Ϣ
                                        String topi = new String(topic.toByteArray());
                                        String msg = "";
                                        getLogger().debug(topi);
                                        //��������
                                        switch (topi) {
                                            case "m5go/response/env": {     //��������
                                                if (!responseflag) return;
                                                JsonObject resObj = JsonParser.parseString(new String(body.toByteArray())).getAsJsonObject();
                                                float tmp = resObj.get("tmp").getAsFloat();
                                                float hum = resObj.get("hum").getAsFloat();
                                                float press = resObj.get("press").getAsFloat();
                                                responseflag = false;
                                                msg = String.format("��ѯ�ɹ���\n��ǰ�¶ȣ�%.1f��\nʪ�ȣ�%.1f%%\n��ѹ��%.1fkPa", tmp, hum, press);
                                                getLogger().debug(msg);
                                                break;
                                            }
                                            case "m5go/response/ac": {       //�յ�����
                                                if (!responseflag) return;
                                                String payload = new String(body.toByteArray());
                                                JsonObject resObj = JsonParser.parseString(payload).getAsJsonObject();
                                                int power = resObj.get("power").getAsInt();
                                                int mode = resObj.get("mode").getAsInt();
                                                int tmp = resObj.get("tmp").getAsInt();
                                                int fan = resObj.get("fan").getAsInt();
                                                float realtmp = resObj.get("realtmp").getAsFloat();

                                                String form = "�յ���%s\n��ǰģʽ��%s\n�趨�¶ȣ�%d��\n��ǰ�¶ȣ�%.1f��\n���٣�%s";

                                                String p = power == 1 ? "����" : "�ر�";
                                                String m = "";
                                                if (mode == 0) {
                                                    m = "�Զ�";
                                                } else if (mode == 1) {
                                                    m = "����";
                                                } else if (mode == 2) {
                                                    m = "��ʪ";
                                                } else if (mode == 3) {
                                                    m = "�ͷ�";
                                                } else if (mode == 4) {
                                                    m = "����";
                                                }

                                                String f = "";
                                                if (fan == 0) {
                                                    f = "�Զ�";
                                                } else if (fan == 1) {
                                                    f = "��";
                                                } else if (fan == 2) {
                                                    f = "��";
                                                } else if (fan == 3) {
                                                    f = "��";
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
                                                    msg += timestr + "M5GO ��������������";
                                                } else if (s.equals("offline")) {
                                                    msg = "M5GO �豸������";
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
                                commandSender.sendMessageBlocking("MQTT���ͷ��������ɹ�");

                                break;
                            case "��ѯ":
                                if (!enableflag) {
                                    commandSender.sendMessageBlocking("������δ��������ѯʧ��\n����/m enable��������");
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
                                            commandSender.sendMessageBlocking("��ѯʧ��");
                                        }
                                    });
//                                commandSender.sendMessageBlocking(mqtt.getMessage());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    commandSender.sendMessageBlocking(e.toString());
                                }
                                break;
                            case "�յ�":
                                if (!enableflag) {
                                    commandSender.sendMessageBlocking("������δ����������ʧ��\n����/m enable��������");
                                    return true;
                                }
                                try {
                                    mqtt.connection.publish("m5go/cmd/ac", "{\"cmd\":0}".getBytes(), QoS.AT_MOST_ONCE, false, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            responseflag = true;
                                            acMode = true;
                                            commandSender.sendMessageBlocking("�����յ�����ģʽ���˳���ظ����˳���\n" +
                                                    "����ָ�\n[����|�ر�]\n[17-30] �����¶�\n[����|����|��ʪ|�ͷ�|�Զ�] ����ģʽ\n[�߷�|�з�|�ͷ�|�Զ���] ���÷���");
                                        }

                                        @Override
                                        public void onFailure(Throwable value) {
                                            value.printStackTrace();
                                            commandSender.sendMessageBlocking("����ʧ��");
                                        }
                                    });


                                } catch (Exception e) {
                                    e.printStackTrace();
                                    commandSender.sendMessageBlocking(e.toString());
                                }
                                break;
                            case "������ϵ��":
                                int listsize = list.size();
                                if (listsize < 2) {
                                    commandSender.sendMessageBlocking("ָ���ʽ����\n/m ������ϵ�� 1234 3456 ...");
                                    break;
                                }
                                for (int i = 1; i < listsize; i++) {
                                    try {
                                        long account = Long.parseLong(list.get(i));
                                        managerSet.add(account);
                                    } catch (NumberFormatException e) {
                                        commandSender.sendMessageBlocking("ָ���ʽ����\n/m ������ϵ�� 1234 3456 ...");
                                        break;
                                    }
                                }
                                commandSender.sendMessageBlocking("����������ϵ�˳ɹ���");
                                break;
                            case "��ϵ��":
                                String backmsg = "������ϵ���б�\n";
                                for (Long s : managerSet) {
                                    backmsg += String.valueOf(s) + "\n";
                                }
                                commandSender.sendMessageBlocking(backmsg);
                                break;
                            case "����":
                                if (list.size() != 2) {
                                    commandSender.sendMessageBlocking("ָ���ʽ����\n/m ���� [����]");
                                    break;
                                }
                                String city = list.get(1);
                                mqtt.connection.publish("m5go/cmd/city", city.getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        commandSender.sendMessageBlocking("������λ���޸ģ�" + city);
                                    }

                                    @Override
                                    public void onFailure(Throwable value) {
                                        commandSender.sendMessageBlocking("����ʧ��");
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
                    sender.sendMessage("������δ����������ʧ��\n����/m enable��������");
                    return;
                }
                if (acMode) {
                    if (msg.equals("�˳�")) {
                        acMode = false;
                        sender.sendMessage("���˳��յ�����ģʽ");
                    } else if (msg.contains("��")) {
                        sendAcCmd(sender, 1, 1, -1, -1, -1);
                    } else if (msg.contains("��")) {
                        sendAcCmd(sender, 1, 0, -1, -1, -1);
                    } else if (msg.equals("�Զ�")) {
                        sendAcCmd(sender, 1, -1, 0, -1, -1);
                    } else if (msg.equals("����")) {
                        sendAcCmd(sender, 1, -1, 1, -1, -1);
                    } else if (msg.equals("��ʪ")) {
                        sendAcCmd(sender, 1, -1, 2, -1, -1);
                    } else if (msg.equals("�ͷ�")) {
                        sendAcCmd(sender, 1, -1, 3, -1, -1);
                    } else if (msg.equals("����")) {
                        sendAcCmd(sender, 1, -1, 4, -1, -1);
                    } else if (msg.equals("�Զ���")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 0);
                    } else if (msg.equals("�ͷ�")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 1);
                    } else if (msg.equals("�з�")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 2);
                    } else if (msg.equals("�߷�")) {
                        sendAcCmd(sender, 1, -1, -1, -1, 3);
                    } else if (msg.matches("\\d+")) {
                        int i = Integer.parseInt(msg);
                        if (i < 17 || i > 30) {
                            sender.sendMessage("��������¶ȷ�Χ��17~30");
                        } else {
                            sendAcCmd(sender, 1, -1, -1, i, -1);
                        }
                    }
                }
                if (msg.equals("��ʷ��¼")) {
                    String path = JChart.createPic(this.getDataFolder().getAbsolutePath() + "/env/");
                    Image img = sender.uploadImage(new File(path));
                    sender.sendMessage(img);
                }
            }

        });
    }

}
