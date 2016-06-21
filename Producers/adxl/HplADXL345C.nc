/*
 * Copyright (c) 2009 DEXMA SENSORS SL
 * Copyright (c) 20011 ZOLERTIA LABS
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * - Neither the name of the copyright holders nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Implementation of ADXL345 accelerometer, as a part of Zolertia Z1 mote
 *
 * Credits goes to DEXMA SENSORS SL
 * @author: Xavier Orduna <xorduna@dexmatech.com>
 * @author: Jordi Soucheiron <jsoucheiron@dexmatech.com>
 * @author: Antonio Linan <alinan@zolertia.com>
 */

configuration HplADXL345C {
  provides interface GeneralIO as GeneralIO1;
  provides interface GeneralIO as GeneralIO2;
  provides interface GpioInterrupt as GpioInterrupt1;
  provides interface GpioInterrupt as GpioInterrupt2;
}

implementation {

  // import the component that manages all the GPIO of the magonode
   components AtmegaGeneralIOC as IO ;

   // wires the GeneralIO interface to the port PE7
   GeneralIO1 = IO.PortE7;

   GeneralIO2 = IO.PortE6;

   // loads the External Interrupt module
   components  AtmegaExtInterruptC ;
   GpioInterrupt1 = AtmegaExtInterruptC.GpioInterrupt[7]; // 0 -> INT0 (PD0)  ... 7 -> INT7 (PE7) Check the atmega128rfa1 for ports assignement!!!
   GpioInterrupt2 = AtmegaExtInterruptC.GpioInterrupt[6]; // 0 -> INT0 (PD0)  ... 7 -> INT7 (PE7) Check the atmega128rfa1 for ports assignement!!!


}
