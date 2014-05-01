/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome (http://www.nethome.nu).
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
import nu.nethome.util.ps.*;

/**
 * @author Stefan
 */
@Plugin
public class FineOffsetDecoder implements ProtocolDecoder {
    protected static final int IDLE = 0;
    protected static final int READING_PREAMBLE = 1;
    protected static final int READING_BIT_MARK = 2;
    protected static final int READING_BIT_SPACE = 3;
    protected static final int REPEAT_SCAN = 4;


    // This are the pulse length constants for the protocol. The default values may
    // be overridden by system properties
    public static final PulseLength FINE_OFFSET_LONG_MARK =
            new PulseLength(FineOffsetDecoder.class, "FINE_OFFSET_LONG_MARK", 1500, 1000, 2000);
    public static final PulseLength FINE_OFFSET_SHORT_MARK =
            new PulseLength(FineOffsetDecoder.class, "FINE_OFFSET_SHORT_MARK", 500, 250, 1000);
    public static final PulseLength FINE_OFFSET_SPACE =
            new PulseLength(FineOffsetDecoder.class, "FINE_OFFSET_SPACE", 1000, 500, 1500);

    /**
     * I = sensor type (and identity)
     * i = sensor identity
     * t = temperature t / 10
     * h = humidity
     * c = checksum
     *
     * ____Byte 0_____  ____Byte 1_____  ____Byte 2_____  ____Byte 3_____  ____Byte 6_____
     * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0
     * I I I I i i i i  i i i i s t t t  t t t t t t t t  h h h h h h h h  c c c c c c c c
     */

    public static final BitString.Field CHECKSUM = new BitString.Field(0, 8);
    public static final BitString.Field HUMIDITY = new BitString.Field(8, 8);
    public static final BitString.Field TEMP = new BitString.Field(16, 11);
    public static final BitString.Field TEMP_SIGN = new BitString.Field(27, 1);
    public static final BitString.Field IDENTITY = new BitString.Field(28, 12);
    public static final BitString.Field SENSOR_TYPE = new BitString.Field(36, 4);


    /**
     * I = sensor type (and identity)
     * i = sensor identity
     * t = temperature (t - 400) / 10
     * r = rain lo bits 0.3 mm per count
     * R = rain high bits
     * u = unknown
     * c = checksum
     *
     * ____Byte 0_____  ____Byte 1_____  ____Byte 2_____  ____Byte 3_____  ____Byte 4_____  ____Byte 5_____  ____Byte 6_____
     * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0
     * I I I I i i i i  i i i i t t t t  t t t t t t t t  r r r r r r r r  0 0 0 0 R R R R  u u u u u u u u  c c c c c c c c
     */

    public static final BitString.Field UNKNOWN1 = new BitString.Field(8, 12);
    public static final BitString.Field UNKNOWN2 = new BitString.Field(20, 4);
    public static final BitString.Field RAIN_LO = new BitString.Field(24, 8);
    public static final BitString.Field RAIN_HI = new BitString.Field(16, 4);
    public static final BitString.Field TEMP_RAIN = new BitString.Field(32, 12);
    public static final BitString.Field IDENTITY_RAIN = new BitString.Field(44, 12);


    private static final CRC8 crc = new CRC8Table(0x0131);
    public static final BitString.Field BYTE6 = new BitString.Field(48, 8);
    public static final BitString.Field BYTE5 = new BitString.Field(40, 8);
    public static final BitString.Field BYTE4 = new BitString.Field(32, 8);
    public static final BitString.Field BYTE3 = new BitString.Field(24, 8);
    public static final BitString.Field BYTE2 = new BitString.Field(16, 8);
    public static final BitString.Field BYTE1 = new BitString.Field(8, 8);
    public static final BitString.Field BYTE0 = new BitString.Field(0, 8);

    protected ProtocolDecoderSink m_Sink = null;
    BitString data = new BitString();
    protected int state = IDLE;
    private int preambleCount;

    public void setTarget(ProtocolDecoderSink sink) {
        m_Sink = sink;
    }

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Fineoffset", "Mark Length", "Fineoffset", 40, 1);
    }

    protected void addBit(boolean b) {
        data.addLsb(b);
        if (data.length() == 40) {
            decodeMessage(data);
        }
        if (data.length() == 56) {
            decodeRainMessage(data);
        }
    }

    private void decodeRainMessage(BitString binaryMessage) {
        int rain = (binaryMessage.extractInt(RAIN_LO) + (binaryMessage.extractInt(RAIN_HI) << 8)) * 3;
        int temp = binaryMessage.extractInt(TEMP_RAIN) - 400;
        int identity = binaryMessage.extractInt(IDENTITY_RAIN);
        int checksum = binaryMessage.extractInt(CHECKSUM);
        byte bytes[] = new byte[6];
        bytes[0] = (byte) binaryMessage.extractInt(BYTE6);
        bytes[1] = (byte) binaryMessage.extractInt(BYTE5);
        bytes[2] = (byte) binaryMessage.extractInt(BYTE4);
        bytes[3] = (byte) binaryMessage.extractInt(BYTE3);
        bytes[4] = (byte) binaryMessage.extractInt(BYTE2);
        bytes[5] = (byte) binaryMessage.extractInt(BYTE1);
        int calculatedChecksum = crc.calc(bytes);
        if (calculatedChecksum == checksum) {
            ProtocolMessage message = new ProtocolMessage("FineOffset", temp, identity, 7);
            message.setRawMessageByteAt(0, bytes[0]);
            message.setRawMessageByteAt(1, bytes[1]);
            message.setRawMessageByteAt(2, bytes[2]);
            message.setRawMessageByteAt(3, bytes[3]);
            message.setRawMessageByteAt(4, bytes[4]);
            message.setRawMessageByteAt(5, bytes[5]);
            message.setRawMessageByteAt(6, (byte) binaryMessage.extractInt(BYTE6));
            message.addField(new FieldValue("Temp", temp));
            message.addField(new FieldValue("Rain", rain));
            message.addField(new FieldValue("Identity", identity));
            m_Sink.parsedMessage(message);
        }
        state = IDLE;
    }

    public void decodeMessage(BitString binaryMessage) {
        int sensorType = binaryMessage.extractInt(SENSOR_TYPE);
        if (sensorType == 3) {
            return;
        }
        int checksum = binaryMessage.extractInt(CHECKSUM);
        int humidity = binaryMessage.extractInt(HUMIDITY);
        int tempSign = binaryMessage.extractInt(TEMP_SIGN);
        int temp = binaryMessage.extractInt(TEMP) * (tempSign == 1 ? -1 : 1);
        int identity = binaryMessage.extractInt(IDENTITY);
        byte bytes[] = new byte[4];
        bytes[0] = (byte) binaryMessage.extractInt(BYTE4);
        bytes[1] = (byte) binaryMessage.extractInt(BYTE3);
        bytes[2] = (byte) binaryMessage.extractInt(BYTE2);
        bytes[3] = (byte) binaryMessage.extractInt(BYTE1);
        int calculatedChecksum = crc.calc(bytes);
        if (calculatedChecksum == checksum) {
            ProtocolMessage message = new ProtocolMessage("FineOffset", temp, identity, 5);
            message.setRawMessageByteAt(0, bytes[0]);
            message.setRawMessageByteAt(1, bytes[1]);
            message.setRawMessageByteAt(2, bytes[2]);
            message.setRawMessageByteAt(3, bytes[3]);
            message.setRawMessageByteAt(4, (byte) binaryMessage.extractInt(BYTE0));
            message.addField(new FieldValue("Temp", temp));
            if (humidity <= 100) {
                message.addField(new FieldValue("Moisture", humidity));
            }
            message.addField(new FieldValue("Identity", identity));
            m_Sink.parsedMessage(message);
        }
        state = IDLE;
    }

    public int parse(double pulse, boolean isMark) {
        switch (this.state) {
            case IDLE: {
                if (FINE_OFFSET_SHORT_MARK.matches(pulse) && isMark) {
                    this.state = READING_PREAMBLE;
                    preambleCount = 1;
                }
                break;
            }
            case READING_PREAMBLE: {
                if (FINE_OFFSET_SHORT_MARK.matches(pulse) && isMark) {
                    ++preambleCount;
                } else if (FINE_OFFSET_LONG_MARK.matches(pulse) && isMark && preambleCount >= 4) {
                    state = READING_BIT_SPACE;
                    data.clear();
                    addBit(false);
                } else if (FINE_OFFSET_SPACE.matches(pulse) && !isMark) {
                    // Ok, continue
                } else {
                    state = IDLE;
                }
                break;
            }
            case READING_BIT_MARK: {
                if (FINE_OFFSET_SHORT_MARK.matches(pulse) && isMark) {
                    this.state = READING_BIT_SPACE;
                    addBit(true);
                } else if (FINE_OFFSET_LONG_MARK.matches(pulse) && isMark) {
                    this.state = READING_BIT_SPACE;
                    addBit(false);
                } else {
                    this.state = IDLE;
                }
                break;
            }
            case READING_BIT_SPACE: {
                if (FINE_OFFSET_SPACE.matches(pulse)) {
                    this.state = READING_BIT_MARK;
                } else {
                    this.state = IDLE;
                }
                break;
            }
        }
        return this.state;
    }
}
