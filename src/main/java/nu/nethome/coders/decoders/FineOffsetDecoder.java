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

    protected static final String STATE_NAMES[] = {"IDLE", "READING_PREAMBLE", "READING_BIT_MARK", "READING_BIT_SPACE",
            "REPEAT_SCAN"};

    // This are the pulse length constants for the protocol. The default values may
    // be overridden by system properties
    public static final PulseLength FINE_OFFSET_LONG_MARK =
            new PulseLength(FineOffsetDecoder.class, "FINE_OFFSET_LONG_MARK", 1500, 1000, 2000);
    public static final PulseLength FINE_OFFSET_SHORT_MARK =
            new PulseLength(FineOffsetDecoder.class, "FINE_OFFSET_SHORT_MARK", 500, 250, 1000);
    public static final PulseLength FINE_OFFSET_SPACE =
            new PulseLength(FineOffsetDecoder.class, "FINE_OFFSET_SPACE", 1000, 500, 1500);

    public static final BitString.Field CHECKSUM = new BitString.Field(0, 8);
    public static final BitString.Field HUMIDITY = new BitString.Field(8, 8);
    public static final BitString.Field TEMP = new BitString.Field(16, 12);
    public static final BitString.Field IDENTITY = new BitString.Field(28, 12);

    private static final CRC8 crc = new CRC8Table(0x0131);
    public static final BitString.Field BYTE_0 = new BitString.Field(32, 8);
    public static final BitString.Field BYTE_1 = new BitString.Field(24, 8);
    public static final BitString.Field BYTE_2 = new BitString.Field(16, 8);
    public static final BitString.Field BYTE_3 = new BitString.Field(8, 8);
    public static final BitString.Field BYTE_4 = new BitString.Field(0, 8);

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

    /**
     * ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_
     * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0
     * x o x 1 x 1 x x  x b x b x b x b  x a x a x a x a   s
     */
    protected void addBit(boolean b) {
        data.addLsb(b);
        if (data.length() == 40) {
            decodeMessage(data);
        }
    }

    public void decodeMessage(BitString binaryMessage) {
        int checksum = binaryMessage.extractInt(CHECKSUM);
        int humidity = binaryMessage.extractInt(HUMIDITY);
        int temp = binaryMessage.extractInt(TEMP);
        int identity = binaryMessage.extractInt(IDENTITY);
        byte bytes[] = new byte[4];
        bytes[0] = (byte) binaryMessage.extractInt(BYTE_0);
        bytes[1] = (byte) binaryMessage.extractInt(BYTE_1);
        bytes[2] = (byte) binaryMessage.extractInt(BYTE_2);
        bytes[3] = (byte) binaryMessage.extractInt(BYTE_3);
        int calculatedChecksum = crc.calc(bytes);
        if (calculatedChecksum == checksum) {
            ProtocolMessage message = new ProtocolMessage("FineOffset", temp, identity, 5);
            message.setRawMessageByteAt(0, bytes[0]);
            message.setRawMessageByteAt(1, bytes[1]);
            message.setRawMessageByteAt(2, bytes[2]);
            message.setRawMessageByteAt(3, bytes[3]);
            message.setRawMessageByteAt(4, (byte) binaryMessage.extractInt(BYTE_4));
            message.addField(new FieldValue("Temp", temp));
            if (humidity <= 100) {
                message.addField(new FieldValue("Moisture", humidity));
            }
            message.addField(new FieldValue("Identity", identity));
            m_Sink.parsedMessage(message);
        }
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
