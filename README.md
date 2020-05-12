# IOT-Terminal
An IOT Terminal based on M5GO(chip: ESP32). The communication system is powered by QQbot.	https://github.com/luzy99/IOT-Terminal.git

> 本项目采用的设备是M5GO（esp32芯片），采用MQTT方式与服务器通信。借助[Mirai QQ机器人框架](https://github.com/mamoe/mirai)，实现QQ远程操控物联网设备。

## 项目使用说明

1. **M5STACK部分**

   `myapp`	该部分采用C语言编写，借助Arduino及第三方库编译。

   > **下列依赖库通过Arduino库管理工具安装**
   >
   > MQTT通信库：EspMQTTClient
   >
   > HTTP库：HTTPClient
   >
   > LED灯条控制：FastLED
   >
   > 红外解码库：IRremoteESP8266
   >
   > JSON解析库：Arduino_JSON

2. **服务器部分**

   > 系统版本：CentOS 7
   >
   > MQTT Broker：EMQX -[安装说明](https://github.com/luzy99/IOT-Terminal/blob/master/some%20tips.md)

   :warning:下列代码为`Mirai QQ 机器人框架`的插件，须依赖框架使用。详细信息请点[这里](https://github.com/mamoe/mirai)。

   `MqttTrans`为利用`Mirai`提供的Java API 编写的MQTT转发插件，同时包括`JFreeChart`图表绘制模块，及JDBC数据库连接访问模块。

## 项目技术报告

1. **前期构想**

   如今智能家居日趋流行， 许多家用电器都内置了使用手机操控的功能，但都略 显鸡肋。原因有以下几点：大多数远程操控的功能都需要安装独立 APP，为了开个空调、 开个风扇单独下载一堆 APP，显然大部分用户是不愿意的。而且这几乎只能实现远程下达 指令的功能，很难实现实时的用户消息推送（除非驻留后台）。 于是我就想借助已有的，用 户日常使用的平台（如 QQ、微信）完成消息的推送和指令传达的功能。如此，物联网终端 的功能将具有高度可拓展性（接入更多家电，传感器），同时可以减轻终端的计算压力。

2. **业务流程**

   ![image-20200512214328080](https://i.loli.net/2020/05/12/6GudMrUc1EvBisY.png)

3. **功能概述**

   1） 环境监测：实时温度、湿度、气压测量，定时上传数据。可通过QQ查看实时情况，并可实时绘制24小时温湿度走势图。

   2） 天气时钟：实时显示室内外天气状况。（可通过QQ发送指令修改城市）

   ​	<img src="https://i.loli.net/2020/05/13/UyTE9tfKOMCHXSa.png" alt="image-20200513002128917" style="zoom:67%;" />

   3）空调控制（可拓展为任何红外操控的家居）：远程/本地皆可控制，模式/温度/风速轻松操控。

   4）应急报警：长按B键触发，发出蜂鸣声并立即通知立即联系人(可事先通过QQ指令添加多个联系人)。同时按A、C键解除警报。

   <img src="https://i.loli.net/2020/05/13/deMPAyHE3JbCL1B.png" alt="image-20200513002836257" style="zoom:67%;" />

   5）夜灯功能：光线传感器+人体传感器，自动点亮LED，LED亮度多档可调，可设置彩灯模式。

   <img src="https://i.loli.net/2020/05/13/gwkCmRPyVc47qhx.png" alt="image-20200513003449572" style="zoom:67%;" />

4. **一些心得**见[some tips](https://github.com/luzy99/IOT-Terminal/blob/master/some%20tips.md)

