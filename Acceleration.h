#ifndef ACCELERATION_H
#define ACCELERATION_H

#include "AM.h"

typedef nx_struct acceleration_msg{
  nx_int16_t x_acceleration;
  nx_int16_t y_acceleration;
  nx_int16_t z_acceleration;
  nx_uint16_t origin;
  nx_uint16_t quality;
  nx_uint16_t link_path_value;
  nx_am_addr_t link_path_addr;
} acceleration_msg_t;

enum {
  AM_ACCELERATION_MSG = 30,
  CTP_COLLECTION_ID=30,
  ROOT_ID=50,
};

#endif
