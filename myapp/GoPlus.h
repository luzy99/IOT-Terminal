#ifndef __GOPLUS_H__
#define __GOPLUS_H__

#include <Wire.h>


#define GOPLUS_ADDR 0x5d  

#define HUB1_ADDR 0x40     
#define HUB2_ADDR 0x50     
#define HUB3_ADDR 0x60     
#define HUB_NUM0 0x06
#define HUB_NUM1 0x07


#define HUB_READ 0x00
#define HUB_WIRE 0x80

#define HUB_DIGITAL 0x00
#define HUB_ANALOG  0x01



class GoPlus {

  public:
    GoPlus();
    void begin();

    uint16_t hub1_a_read_value(uint8_t reg);
    uint16_t hub1_d_read_value(uint8_t reg);
    uint16_t hub2_d_read_value(uint8_t reg);
    uint16_t hub3_d_read_value(uint8_t reg);
    uint16_t hub2_a_read_value(uint8_t reg);
    uint16_t hub3_a_read_value(uint8_t reg);
    void hub1_wire_value(uint8_t reg,uint16_t value);
    void hub2_wire_value(uint8_t reg,uint16_t value);
    void hub3_wire_value(uint8_t reg,uint16_t value);
    
  public:

  private:
    
  private:

};


#endif
