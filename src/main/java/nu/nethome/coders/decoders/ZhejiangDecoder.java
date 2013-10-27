/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stestr@nethome.nu>
 *
 * This file is part of OpenNetHome.
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nu.nethome.coders.decoders;

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.FieldValue;
import nu.nethome.util.ps.ProtocolDecoder;
import nu.nethome.util.ps.ProtocolMessage;
import nu.nethome.util.ps.PulseLength;

/**
 * The ZhejiangDecoder parses a set of pulse lengths and decodes a protocol used
 * by lamp remote controls which is transmitted over 433MHz AM-modulated RF-signal.
 * The reception of the pulses may for example be made via the AudioProtocolSampler.
 * The ZhejiangDecoder implements the ProtocolDecoder-interface and accepts the pulses
 * one by one. It contains a state machine, and when a complete message is decoded,
 * this is reported over the ProtocolDecoderSink-interface which is given at
 * construction.
 * <p/>
 * The protocol is mark length encoded and the protocol messages has the following
 * layout:<br>
 * <p/>
 * s = Start bit = 0<br>
 * a = Channel 1 not selected<br>
 * b = Channel 2 not selected<br>
 * c = Channel 3 not selected<br>
 * d = Channel 4 not selected<br>
 * e = Channel 5 not selected<br>
 * f = Button A not pressed<br>
 * g = Button B not pressed<br>
 * h = Button C not pressed<br>
 * i = Button D not pressed<br>
 * j = Button E not pressed<br>
 * o = On/Off-bit<br>
 * <br>
 *_S_ ____Byte 2_____  ____Byte 1_____  ____Byte 0_____ <br>
 * 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0 <br>
 * x  o 0 1 0 j 0 i 0  h 0 g 0 f 0 1 e  1 d 1 c 1 b 1 a <br>
 * <p/>
 * This protocol is almost exactly the same as NEXA, so the entire implementation is reused.
 *
 * @author Stefan
 */
@Plugin
public class ZhejiangDecoder extends NexaDecoder implements ProtocolDecoder{

    // This are the pulse length constants for the protocol. The default values may
    // be overridden by system properties
    public static final PulseLength ZHEJ_LONG_MARK =
            new PulseLength(ZhejiangDecoder.class, "ZHEJ_LONG_MARK", 430, 370, 550);
    public static final PulseLength ZHEJ_SHORT_MARK =
            new PulseLength(ZhejiangDecoder.class, "ZHEJ_SHORT_MARK", 140, 100, 220);
    public static final PulseLength ZHEJ_LONG_SPACE =
            new PulseLength(ZhejiangDecoder.class, "ZHEJ_LONG_SPACE", 450, 370, 550);
    public static final PulseLength ZHEJ_SHORT_SPACE =
            new PulseLength(ZhejiangDecoder.class, "ZHEJ_SHORT_SPACE", 170, 100, 220);
    public static final PulseLength ZHEJ_REPEAT =
            new PulseLength(ZhejiangDecoder.class, "ZHEJ_REPEAT", 4560, 500);

    public void setup() {
        m_ProtocolName = "Zhejiang";
        LONG_MARK = ZHEJ_LONG_MARK;
        SHORT_MARK = ZHEJ_SHORT_MARK;
        LONG_SPACE = ZHEJ_LONG_SPACE;
        SHORT_SPACE = ZHEJ_SHORT_SPACE;
        REPEAT = ZHEJ_REPEAT;
    }

    protected int bytemap(int raw) {
        return (raw & 1) + ((raw >> 1) & 2) + ((raw >> 2) & 4) + ((raw >> 3) & 8) + ((raw >> 4) & 0x10);
    }

    protected void addBit(int b) {
        // Shift in data
        m_Data >>= 1;
        m_Data |= (b << 24);
        // Check if this is a complete message
        if (m_BitCounter == 24) {
            // It is, create the message
            int command = ((m_Data >> 23) & 1) ^ 1;
            int rawButton = bytemap(((m_Data >> 11) ^ 0x3FF) & 0x3FF);
            int address = bytemap((m_Data ^ 0x3FF) & 0x3FF);
            int button = bitPosToInt(rawButton);

            // Sender ends a message sequence by a signal saying "no button is pressed".
            // We ignore that message.
            if (rawButton == 0) {
                m_State = IDLE;
                m_RepeatCount = 0;
                return;
            }

            ProtocolMessage message = new ProtocolMessage(m_ProtocolName, command, (rawButton << 8) + address, 4);
            message.setRawMessageByteAt(0, (m_Data >> 24) & 0x1);
            message.setRawMessageByteAt(1, (m_Data >> 16) & 0xFF);
            message.setRawMessageByteAt(2, (m_Data >> 8) & 0xFF);
            message.setRawMessageByteAt(3, (m_Data) & 0xFF);

            message.addField(new FieldValue("Command", command));
            message.addField(new FieldValue("Button", button));
            message.addField(new FieldValue("Address", address));

            // It is, check if this really is a repeat
            if ((m_RepeatCount > 0) && (m_Data == m_LastData)) {
                message.setRepeat(m_RepeatCount);
            } else {
                // It is not a repeat, reset counter
                m_RepeatCount = 0;
            }
            // Report the parsed message
            m_Sink.parsedMessage(message);
            m_State = REPEAT_SCAN;
        }
        m_BitCounter++;
    }

    private int bitPosToInt(int rawButton) {
        int shift = rawButton;
        for (int i = 0; i < 5; i++) {
            if ((shift & 1) == 1) {
                return i;
            }
            shift >>= 1;
        }
        return 5;
    }
}
