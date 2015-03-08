/**
 * Copyright (C) 2005-2015, Stefan Strömberg <stefangs@nethome.nu>
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

import nu.nethome.coders.RollerTrol;
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import static nu.nethome.coders.RollerTrol.*;

@Plugin
public class RollerTrolDecoder implements ProtocolDecoder {

    protected static final int IDLE = 0;
    protected static final int READING_LONG_PREAMBLE_MARK = 1;
    protected static final int READING_LONG_PREAMBLE_SPACE = 2;
    protected static final int READING_SHORT_PREAMBLE_MARK = 3;
    protected static final int READING_SHORT_PREAMBLE_SPACE = 4;
    protected static final int READING_MARK = 5;
    protected static final int READING_SHORT_SPACE = 6;
    protected static final int READING_LONG_SPACE = 7;
    protected static final int REPEAT_SCAN = 10;

    public static final BitString.Field BYTE4 = new BitString.Field(32, 8);
    public static final BitString.Field BYTE3 = new BitString.Field(24, 8);
    public static final BitString.Field BYTE2 = new BitString.Field(16, 8);
    public static final BitString.Field BYTE1 = new BitString.Field(8, 8);
    public static final BitString.Field BYTE0 = new BitString.Field(0, 8);

    protected ProtocolDecoderSink m_Sink = null;
    BitString data = new BitString();
    protected int state = IDLE;
    private int repeat;

    public void setTarget(ProtocolDecoderSink sink) {
        m_Sink = sink;
    }

    public ProtocolInfo getInfo() {
        return RollerTrol.ROLLERTROL_PROTOCOL_INFO;
    }

    protected void addBit(boolean b) {
        data.addMsb(b);
        if (data.length() == MESSAGE_BIT_LENGTH) {
            decodeMessage(data);
        }
    }

    public void decodeMessage(BitString binaryMessage) {
        int houseCode = binaryMessage.extractInt(HOUSE_CODE);
        int deviceCode = binaryMessage.extractInt(DEVICE_CODE);
        int command = binaryMessage.extractInt(COMMAND);
        int checkSum = binaryMessage.extractInt(CHECK_SUM);
        int calculatedCheckSum = calculateChecksum(binaryMessage);
        if (checkSum == calculatedCheckSum) {
            byte bytes[] = new byte[5];
            bytes[0] = (byte) binaryMessage.extractInt(BYTE4);
            bytes[1] = (byte) binaryMessage.extractInt(BYTE3);
            bytes[2] = (byte) binaryMessage.extractInt(BYTE2);
            bytes[3] = (byte) binaryMessage.extractInt(BYTE1);
            bytes[4] = (byte) binaryMessage.extractInt(BYTE0);
            ProtocolMessage message = new ProtocolMessage(ROLLER_TROL_PROTOCOL_NAME, command, deviceCode, 5);
            message.setRawMessageByteAt(0, bytes[0]);
            message.setRawMessageByteAt(1, bytes[1]);
            message.setRawMessageByteAt(2, bytes[2]);
            message.setRawMessageByteAt(3, bytes[3]);
            message.setRawMessageByteAt(4, bytes[4]);

            message.addField(new FieldValue(HOUSE_CODE_NAME, houseCode));
            message.addField(new FieldValue(DEVICE_CODE_NAME, deviceCode));
            message.addField(new FieldValue(COMMAND_NAME, command));
            message.setRepeat(repeat);
            m_Sink.parsedMessage(message);
        }
        state = REPEAT_SCAN;
    }

    public int parse(double pulse, boolean bitstate) {
        switch (state) {
            case IDLE: {
                if (RollerTrol.LONG_PREAMBLE_MARK.matches(pulse) && bitstate) {
                    data.clear();
                    repeat = 0;
                    state = READING_LONG_PREAMBLE_SPACE;
                }
                break;
            }
            case READING_LONG_PREAMBLE_SPACE: {
                if (RollerTrol.LONG_PREAMBLE_SPACE.matches(pulse)) {
                    state = READING_SHORT_PREAMBLE_MARK;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_SHORT_PREAMBLE_MARK: {
                if (RollerTrol.SHORT_PREAMBLE_MARK.matches(pulse)) {
                    state = READING_SHORT_PREAMBLE_SPACE;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_SHORT_PREAMBLE_SPACE: {
                if (RollerTrol.SHORT.matches(pulse)) {
                    state = READING_MARK;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_MARK: {
                if (RollerTrol.SHORT.matches(pulse)) {
                    state = READING_LONG_SPACE;
                } else if (RollerTrol.LONG.matches(pulse)) {
                    state = READING_SHORT_SPACE;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_SHORT_SPACE: {
                if (RollerTrol.SHORT.matches(pulse)) {
                    state = READING_MARK;
                    addBit(true);
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_LONG_SPACE: {
                if (RollerTrol.LONG.matches(pulse)) {
                    state = READING_MARK;
                    addBit(false);
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case REPEAT_SCAN: {
                if (RollerTrol.LONG_PREAMBLE_MARK.matches(pulse) && bitstate) {
                    data.clear();
                    repeat++;
                    state = READING_LONG_PREAMBLE_SPACE;
                } else if (!RollerTrol.LONG.matches(pulse) && !RollerTrol.SHORT.matches(pulse)) {
                    state = IDLE;
                }
                break;
            }

        }
        return state;
    }

    private void quitParsing(double pulseLength) {
        if (data.length() > 5) {
            m_Sink.partiallyParsedMessage(String.format("RollerTrol Pulse: %g ms, State: %d", pulseLength, state), data.length());
        }
        state = IDLE;
    }
}
