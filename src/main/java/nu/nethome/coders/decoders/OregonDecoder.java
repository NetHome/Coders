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


@Plugin
public class OregonDecoder implements ProtocolDecoder {
    protected static final int IDLE = 0;
    protected static final int PREAMBLE = 1;
    protected static final int HI_IN = 2;
    protected static final int HI_BETWEEN = 3;
    protected static final int LO_IN = 4;
    protected static final int LO_BETWEEN = 5;

    protected static final int OREGON_SHORT = 490;
    protected static final int OREGON_LONG = 975;
    protected static final int MIN_PREAMBLE_PULSES = 16;
    protected static final BitString.Field NIBBLE = new BitString.Field(0, 4);
    protected static final int MAX_NIBBLES = 20;


    /**
     * The protocol is organized as a stream of nibbles (4 bits). These are the definitions of the different fields
     */
    public static final int SYNC_NIBBLE = 0;            // Sync, always 0xA
    public static final int SENSOR_TYPE = 1;            // 4 Nibbles
    public static final int CHANNEL = 5;                // 1 Nibble, channel set on the sensor
    public static final int IDENTITY = 6;               // 2 Nibbles, identity of sensor, changes on power loss
    public static final int FLAGS = 8;                  // 1 Nibble, flags
    public static final int LOW_BATTERY_BIT = 0x04;     // Low power indication bit in FLAGS
    public static final int TEMP_VALUE = 9;             // 3 Nibbles, temperature as BCD in reverse order
    public static final int TEMP_SIGN = 12;             // 1 Nibble, <> 0 means negative temperature
    public static final int MOISTURE_VALUE = 13;        // 2 Nibbles, moisture value as BCD in reverse order
    public static final int CHECKSUM = 16;              // 2 Nibbles
    public static final int MESSAGE_LENGTH = 19;

    protected int m_State = IDLE;

    private BitString data = new BitString();

    protected int nibbleCounter = 0;
    protected int m_RepeatCount = 0;
    protected double m_LastValue = 0;
    protected ProtocolDecoderSink m_Sink = null;
    private int preambleCount;
    private boolean isInvertedBit;
    private int invertedBit;
    private byte[] nibbles = new byte[MAX_NIBBLES];

    public void setTarget(ProtocolDecoderSink sink) {
        m_Sink = sink;
    }

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Oregon", "Manchester", "Oregon Scientific", 19 * 4, 2);
    }

    protected boolean pulseCompare(double candidate, double standard) {
        return Math.abs(standard - candidate) < standard * 0.2;
    }

    protected void addBit(int b) {
        if (isInvertedBit) {
            invertedBit = b;
        } else if (b == invertedBit) {
            m_State = IDLE;
            return;
        } else {
            data.addMsb(b != 0);
            if (data.length() == 4) {
                addNibble((byte) data.extractInt(NIBBLE));
                data.clear();
            }
        }
        isInvertedBit = !isInvertedBit;
    }

    protected void addNibble(byte nibble) {
        nibbles[nibbleCounter++] = nibble;
        // Check if this is a complete message
        if (nibbleCounter == MESSAGE_LENGTH) {
            decodeMessage(nibbles);
        }
    }

    public void decodeMessage(byte[] nibbles) {
        int sensorType = (nibbles[SENSOR_TYPE] << 12) + (nibbles[SENSOR_TYPE + 1] << 8) + (nibbles[SENSOR_TYPE + 2] << 4) + nibbles[SENSOR_TYPE + 3];
        int channel = nibbles[CHANNEL];
        int rollingId = (nibbles[IDENTITY] << 4) + nibbles[IDENTITY + 1];
        int lowBattery = (nibbles[FLAGS] & LOW_BATTERY_BIT) != 0 ? 1 : 0;
        int temp = (nibbles[TEMP_VALUE + 2] * 100 + nibbles[TEMP_VALUE + 1] * 10 + nibbles[TEMP_VALUE]) * (nibbles[TEMP_SIGN] != 0 ? -1 : 1);
        int moisture = nibbles[MOISTURE_VALUE + 1] * 10 + nibbles[MOISTURE_VALUE];
        int checksum = (nibbles[CHECKSUM + 1] << 4) + nibbles[CHECKSUM];
        int calculatedChecksum = 0;
        for (int i = 1; i < 16; i++) {
            calculatedChecksum += nibbles[i];
        }

        ProtocolMessage message = new ProtocolMessage("Oregon", temp, channel, 2);
        message.setRawMessageByteAt(1, moisture);
        message.setRawMessageByteAt(0, temp);

        message.addField(new FieldValue("SensorId", sensorType));
        message.addField(new FieldValue("Channel", channel));
        message.addField(new FieldValue("Id", rollingId));
        message.addField(new FieldValue("Temp", temp));
        message.addField(new FieldValue("Moisture", moisture));
        message.addField(new FieldValue("LowBattery", lowBattery));

        // Report the parsed message
        m_Sink.parsedMessage(message);
        // Reset state
        m_State = IDLE;
    }

    public int parse(double pulse, boolean isMark) {
        switch (m_State) {
            case IDLE: {
                if (pulseCompare(pulse, OREGON_LONG) && isMark) {
                    m_State = PREAMBLE;
                    preambleCount = 0;
                }
                break;
            }
            case PREAMBLE: {
                if (pulseCompare(pulse, OREGON_LONG)) {
                    preambleCount++;
                } else if (pulseCompare(pulse, OREGON_SHORT) && !isMark && (preambleCount > MIN_PREAMBLE_PULSES)) {
                    data.clear();
                    nibbleCounter = 0;
                    isInvertedBit = true;
                    m_State = HI_BETWEEN;
                } else {
                    m_State = IDLE;
                }
                break;
            }
            case HI_IN: {
                if (pulseCompare(pulse, OREGON_SHORT)) {
                    m_State = LO_BETWEEN;
                } else if (pulseCompare(pulse, OREGON_LONG)) {
                    m_State = LO_IN;
                    addBit(1);
                } else {
                    m_State = IDLE;
                }
                break;
            }
            case HI_BETWEEN: {
                if (pulseCompare(pulse, OREGON_SHORT)) {
                    m_State = LO_IN;
                    addBit(1);
                } else {
                    m_State = IDLE;
                }
                break;
            }
            case LO_IN: {
                if (pulseCompare(pulse, OREGON_SHORT)) {
                    m_State = HI_BETWEEN;
                    break;
                } else if (pulseCompare(pulse, OREGON_LONG)) {
                    m_State = HI_IN;
                    addBit(0);
                } else {
                    m_State = IDLE;
                }
                break;
            }
            case LO_BETWEEN: {
                if (pulseCompare(pulse, OREGON_SHORT)) {
                    m_State = HI_IN;
                    addBit(0);
                } else {
                    m_State = IDLE;
                }
                break;
            }
        }
        m_LastValue = pulse;
        return m_State;
    }
}
