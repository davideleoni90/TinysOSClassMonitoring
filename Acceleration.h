#ifndef ACCELERATION_H
#define ACCELERATION_H

#include "AM.h"

enum {
  AM_ACCELERATION_MSG = 30,
  CTP_COLLECTION_ID=30,
  ROOT_ID=50,
  SAMPLING_PERIOD=2000,
  WAITING_PERIOD_RADIO=1000,
  WAITING_PERIOD_SERIAL=1000,
  UART_QUEUE_DEPTH=20,
  SEND_QUEUE_DEPTH=20,
  NUMBER_OF_MOTES=4
};

typedef nx_struct acceleration_msg{
  nx_int16_t x_acceleration;
  nx_int16_t y_acceleration;
  nx_int16_t z_acceleration;
  nx_uint16_t message_path[NUMBER_OF_MOTES];
  nx_uint16_t hopcount;
  nx_uint16_t origin;
  nx_uint16_t quality;
  nx_uint16_t link_path_value;
  nx_am_addr_t link_path_addr;
} acceleration_msg_t;

#endif
