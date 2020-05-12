#include <M5Stack.h>
#include <WiFi.h>
#include "EspMQTTClient.h"
#include <HTTPClient.h>
#include "time.h"
#include <Arduino_JSON.h>
#include "utility/M5Timer.h"
#include "DHT12.h"
#include <Wire.h> //The DHT12 uses I2C comunication.
#include "Adafruit_Sensor.h"
#include <Adafruit_BMP280.h>
#include <Adafruit_NeoPixel.h> //不用你还得留着？把你去掉还会卡我wifi？！WTF
#include <FastLED.h>
#include "GoPlus.h"

#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <ir_Gree.h>

const uint16_t kIrLed = 13; // ESP8266 GPIO pin to use. Recommended: 4 (D2).
IRGreeAC ac(kIrLed);        // Set the GPIO to be used for sending messages.

#define NUM_LEDS 10

CRGBArray<NUM_LEDS> leds;

GoPlus goPlus;

const char *ntpServer = "ntp.aliyun.com";
M5Timer M5timer;
int timer_id;

DHT12 dht12; //Preset scale CELSIUS and ID 0x5c.
Adafruit_BMP280 bme;

EspMQTTClient mqtt(
    "wifissid",
    "password",
    "1.1.1.1", // MQTT Broker server ip
    "m5go",           // Can be omitted if not needed
    "password",        // Can be omitted if not needed
    "m5go",           // Client name that uniquely identify your device
    1883              // The MQTT port, default to 1883. this line can be omitted
);

String location = "无锡";
uint32_t back_color = BLACK;
int8_t batteryLevel; //电量

//打印时间
void printLocalTime()
{
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo))
  {
    Serial.println("Failed to obtain time");
    return;
  }
  Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
}

//更新时间
void updateTimeStr()
{
  struct tm timeinfo;
  getLocalTime(&timeinfo);
  char timestr[10];
  char datestr[10];
  char weekstr[5];
  strftime(timestr, 10, "%H:%M:%S", &timeinfo);
  strftime(datestr, 10, "%m/%d", &timeinfo);
  strftime(weekstr, 5, "%a", &timeinfo);
  M5.Lcd.drawString(timestr, 22, 88, 7);   //时间
  M5.Lcd.drawString(datestr, 250, 85, 4);  //日期
  M5.Lcd.drawString(weekstr, 250, 115, 4); //星期
}

//环境传感器
void updateENV()
{
  float tmp = dht12.readTemperature();
  float hum = dht12.readHumidity();
  float pressure = bme.readPressure() / 1000.0;
  char ENVstr[30];
  //千万别用sprintf！！！！会溢出！！！
  snprintf(ENVstr, 30, "%.1fC/ %.1f%%/ %.1fkPa", tmp, hum, pressure);
  M5.Lcd.drawString(ENVstr, 35, 171, 4);
  return;
}

int last_bright = 1;
//led灯条亮度
void LEDbright(int bright)
{
  if (bright < 0)
    return;
  if (last_bright == bright)
    return;
  for (int i = 0; i < NUM_LEDS; i++)
  {
    leds[i] = CRGB(bright, bright, bright);
  }
  FastLED.show();
  last_bright = bright;
}

//计时器
void onTimer()
{
  updateTimeStr();
  struct tm timeinfo;
  getLocalTime(&timeinfo);
  if (timeinfo.tm_min == 1 && timeinfo.tm_sec == 1)
  { //每小时更新天气
    getWeather();
  }
  if (timeinfo.tm_sec % 3 == 1)
  { //每3s更新室内温度
    updateENV();
  }
  if (timeinfo.tm_min % 20 == 1 && timeinfo.tm_sec == 9) //每二十分钟上传一次数据
  {
    float tmp = dht12.readTemperature();
    float hum = dht12.readHumidity();
    float pressure = bme.readPressure() / 1000.0;

    char msg[80];
    snprintf(msg, 80, "{\"tmp\":%.1f,\"hum\":%.1f,\"press\":%.1f,\"time\":%d}", tmp, hum, pressure, mktime(&timeinfo));
    mqtt.publish("m5go/env", msg, 1);
  }
}
//初始化
void setup()
{
  Serial.begin(115200);
  //初始化mqtt
  mqtt.enableDebuggingMessages(); // Enable debugging messages sent to serial output
  mqtt.enableLastWillMessage("m5go/direct", "offline");
  M5.begin();
  M5.Power.begin();
  Wire.begin();
  if (!SPIFFS.begin(true))
  {
    Serial.println("SPIFFS Mount Failed");
  }
  while (!bme.begin(0x76))
  {
    Serial.println("Could not find a valid BMP280 sensor, check wiring!");
  }
  //初始化显示
  M5.Lcd.setBrightness(80);
  M5.Lcd.fillScreen(back_color);
  M5.Lcd.setTextColor(WHITE, back_color);
  M5.Lcd.drawString("00:00:00", 22, 88, 7); //时间
  M5.Lcd.setTextFont(4);
  M5.Lcd.drawString("-/-", 250, 85); //日期
  M5.Lcd.setTextFont(4);
  M5.Lcd.drawString("--", 250, 115); //星期
  M5.Lcd.setTextFont(4);
  M5.Lcd.setTextSize(1);
  M5.Lcd.drawString("--~--C AQI:--", 80, 41); //天气
  M5.Lcd.setTextFont(4);
  M5.Lcd.drawString("23°C/ 45%", 35, 171); //室内
  // M5.Lcd.drawPngFile(SPIFFS, "/lei.png", 31, 38);

  //初始化计时器
  timer_id = M5timer.setInterval(1000, onTimer);
  M5timer.disable(timer_id);
  FastLED.addLeds<NEOPIXEL, 15>(leds, NUM_LEDS);
  // pixels.begin();
  M5.Power.setPowerBoostOnOff(false);
  LEDbright(0);

  //初始化空调
  ac.begin();
  ac.setModel(YBOFB);
  // ac.on();
  ac.setFan(1);
  ac.setMode(kGreeCool);
  ac.setTemp(20); // 16-30C
  ac.setSwingVertical(true, kGreeSwingAuto);
  ac.setXFan(false);
  ac.setLight(true);

  ac.setSleep(false);
  ac.setTurbo(false);
}

//连接建立成功
void onConnectionEstablished()
{
  mqtt.subscribe("m5go/cmd/#", [](const String &topic, const String &payload) {
    if (topic == "m5go/cmd/env")
    {
      if (payload == "get")
      { //查询温度指令
        float tmp = dht12.readTemperature();
        float hum = dht12.readHumidity();
        float pressure = bme.readPressure() / 1000.0;
        char msg[80];

        snprintf(msg, 80, "{\"tmp\":%.1f,\"hum\":%.1f,\"press\":%.1f,\"battery\":%d}", tmp, hum, pressure, batteryLevel);
        mqtt.publish("m5go/response/env", msg, 1);
      }
    }
    if (topic == "m5go/cmd/ac")
    {
      setAC(payload);
    }
    if (topic == "m5go/cmd/city")
    {
      location = payload;
      getWeather();
    }
  });

  Serial.println("Connected to WiFi\n");
  configTime(8 * 3600, 0, ntpServer); //ntp校时
  printLocalTime();
  getWeather(); //获取天气
  M5timer.enable(timer_id);
}

int btC_mode = 0;
int l_mode[6] = {0, 50, 120, 255, -2, -1};
uint16_t port1, port3;
clock_t start_light = 0;
bool alarmMode = false;
uint8_t hue;

//主循环
void loop()
{
  mqtt.loop();
  M5timer.run();
  M5.update();

  //如果处于报警模式
  if (alarmMode)
  {
    M5.Lcd.drawPngFile(SPIFFS, "/warning.png", 60, 10);
    if (M5.BtnA.wasPressed() && M5.BtnC.wasPressed()) //A+C键解除
    {
      alarmMode = false;
      M5.Lcd.fillScreen(back_color);
      LEDbright(1);
      LEDbright(0);
      mqtt.publish("m5go/direct", "警报解除", 2);
      getWeather();
      return;
    }
    //彩色rgb
    for (int i = 0; i < NUM_LEDS / 2; i++)
    {
      // let's set an led value
      leds[i] = CHSV(hue++, 255, 255);
      leds(NUM_LEDS / 2, NUM_LEDS - 1) = leds(NUM_LEDS / 2 - 1, 0);
    }
    FastLED.show();
    M5.Speaker.beep();
    delay(200);
    for (int i = 0; i < NUM_LEDS; i++)
    {
      // let's set an led value
      leds[i] = CRGB(0, 0, 0);
    }
    FastLED.show();
    delay(200);
    return;
  }

  //长按5s报警
  if (M5.BtnB.pressedFor(4000))
  {
    alarmMode = true;
    hue = 0;
    mqtt.publish("m5go/direct", "warning", 2);
    M5.Lcd.drawPngFile(SPIFFS, "/warning.png", 60, 10);
    return;
  }

  batteryLevel = M5.Power.getBatteryLevel();
  if (M5.Power.isCharging())
  {
    batteryLevel = 101;
  }
  port1 = goPlus.hub1_d_read_value(HUB_NUM0); //light
  port3 = goPlus.hub3_d_read_value(HUB_NUM0); //pir

  //按钮C
  if (M5.BtnC.wasPressed())
  {
    M5.Lcd.wakeup();
    M5.Lcd.setBrightness(80);
    start_light = clock();
    btC_mode += 1;
    btC_mode %= 6;
    Serial.printf("BtnC %d\n", l_mode[btC_mode]);
    if (l_mode[btC_mode] == -1)
    {
      M5.Lcd.drawString("AUTO", 280, 220, 2); //AUTO
      LEDbright(0);
    }
    else
    {
      M5.Lcd.drawString("      ", 280, 220, 2);
      LEDbright(l_mode[btC_mode]);
    }
  }

  //按钮A
  if (M5.BtnA.wasPressed())
  {
    M5.Lcd.wakeup();
    M5.Lcd.setBrightness(80);
    start_light = clock();
    Serial.printf("BtnA %d/battery:%d%%\n", port1, batteryLevel);

#if SEND_GREE
    ac.setLight(!ac.getLight());
    Serial.println("Sending IR command to A/C ...");
    ac.send();
#endif // SEND_GREE
    printState();
  }

  //按钮B
  if (M5.BtnB.wasPressed())
  {
    M5.Lcd.wakeup();
    M5.Lcd.setBrightness(80);
    start_light = clock();
  }
  if (l_mode[btC_mode] == -1 && ((clock() - start_light) / CLOCKS_PER_SEC > 10))
  { //自动模式,延时10s
    if (port3 > 500)
    {
      if (port1 > 500)
      {
        LEDbright(200);
      }
      M5.Lcd.wakeup();
      M5.Lcd.setBrightness(80);
      start_light = clock();
    }
    else
    {
      LEDbright(0);
      M5.Lcd.setBrightness(0);
      M5.Lcd.sleep();
    }
  }
  if (l_mode[btC_mode] == -2)
  { //彩灯模式
    //彩色rgb
    for (int i = 0; i < NUM_LEDS / 2; i++)
    {
      // let's set an led value
      hue %= 256;
      leds[i] = CHSV(hue++, 255, 255);
      leds(NUM_LEDS / 2, NUM_LEDS - 1) = leds(NUM_LEDS / 2 - 1, 0);
    }
    FastLED.delay(90);
  }
}

//获取天气
void getWeather()
{
  HTTPClient http;
  String key = "appid=42456966&appsecret=MYz6nzQA";
  String weather_url = "http://www.tianqiapi.com/free/day?" + key + "&city=" + location;
  http.begin(weather_url); //HTTP

  int httpCode = http.GET();

  if (httpCode > 0)
  {
    // file found at server
    if (httpCode == HTTP_CODE_OK)
    {
      String payload = http.getString();
      Serial.println(payload);
      JSONVar res = JSON.parse(payload.c_str());
      if (JSON.typeof(res) == "undefined")
      {
        Serial.println("Parsing input failed!");
        return;
      }
      String wea_str = (String)(const char *)res["tem_night"] + "~" + (String)(const char *)res["tem_day"] + "C " + "AQI:" + (String)(const char *)res["air"];
      M5.Lcd.setTextFont(4);
      M5.Lcd.setTextSize(1);
      Serial.println(wea_str.c_str());
      M5.Lcd.drawString(wea_str.c_str(), 80, 41); //天气
      String imgpath = "/" + (String)(const char *)res["wea_img"] + ".png";
      M5.Lcd.fillRect(31, 38, 36, 36, back_color);
      M5.Lcd.drawPngFile(SPIFFS, imgpath.c_str(), 31, 38);
    }
  }
  else
  {
    Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
  }
  http.end();
}

void printState()
{
  // Display the settings.
  Serial.println("GREE A/C remote is in the following state:");
  Serial.printf("  %s\n", ac.toString().c_str());
  // Display the encoded IR sequence.
  unsigned char *ir_code = ac.getRaw();
  Serial.print("IR Code: 0x");
  for (uint8_t i = 0; i < kGreeStateLength; i++)
    Serial.printf("%02X", ir_code[i]);
  Serial.println();
}

//设置空调[模式，温度]
/*
const uint8_t kGreeAuto = 0;
const uint8_t kGreeCool = 1;
const uint8_t kGreeDry = 2;
const uint8_t kGreeFan = 3;
const uint8_t kGreeHeat = 4;
{
  "cmd": 0, //get:0,set:1
  "on":1,
  "tmp": 21,
  "mode": 1,
  "timer": 60
}
*/
void setAC(const String &jsonstr)
{
  JSONVar res = JSON.parse(jsonstr.c_str());
  if (JSON.typeof(res) == "undefined")
  {
    Serial.println("Parsing input failed!");
    return;
  }
  if ((int)res["cmd"] == 0)
  {
    Serial.printf("  %s\n", ac.toString().c_str());
    getAcState();
  }
  else
  {
    int power = (int)res["power"];
    int tmp = (int)res["tmp"];
    int mode = (int)res["mode"];
    int timer = (int)res["timer"];
    int fan = (int)res["fan"];

    if (power == 1)
    {
      ac.on();
    }
    else if (power == 0)
    {
      ac.off();
    }
    if (mode != -1)
      ac.setMode(mode);
    if (tmp != -1)
      ac.setTemp(tmp);
    if (fan != -1)
      ac.setFan(fan);
    ac.send();
    getAcState();
    printState();
  }
}

void getAcState()
{
  int power = ac.getPower();
  int mode = ac.getMode();
  int tmp = ac.getTemp();
  int fan = ac.getFan();
  float realtmp = dht12.readTemperature();
  char msg[80];
  snprintf(msg, 80, "{\"power\":%d,\"mode\":%d,\"tmp\":%d,\"fan\":%d,\"realtmp\":%.1f}", power, mode, tmp, fan, realtmp);
  mqtt.publish("m5go/response/ac", msg, 1);
}