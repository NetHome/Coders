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
 * Decoder for the RF protocol used by devices manufactured by UPM, typically
 * temperature sensors.
 * 
 * @author Stefan Str�mberg
 */
@Plugin
public class UPMDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int HI_IN = 2;
	protected static final int HI_BETWEEN = 3;
	protected static final int LO_IN = 4;
	protected static final int LO_BETWEEN = 5;
	protected static final int REPEAT_SCAN = 6;
	
	protected static final int UPMSHORT = 1000;
	protected static final int UPMLONG = 2000;
	protected static final int UPMEND1 = 300;
	protected static final int UPMEND2 = 610;
	protected static final int UPMREPEAT_MIN = 70000;
	protected static final int UPMREPEAT_MAX = 100000;

	protected int m_State = IDLE;
	protected long m_Message = 0;
	protected long m_LastMessage = 0;
	protected int m_BitCounter = 0;
	protected ProtocolDecoderSink m_Sink = null;
	private int m_RepeatCount = 0;
		
	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("UPM", "Manchester", "UPM", 14, 5);
	}

	/**
	 * Compare a received pulse length with an expected length. If the candidate pulse
	 * is within the tolerance, of the expected, return true otherwise false
	 * 
	 * @param candidate Pulse length to evaluate
	 * @param standard Pulse length to evaluate against
	 * @return true if the candidate is close enough to the standard
	 */
	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < (standard * 0.2 + 26);
	}
	
	/**
	 * Add a new received bit to the current message
	 * @param b the new bit to add
	 */
	protected void addBit(int b) {
		// Rotate in the new bit
		m_Message <<= 1;
		m_Message |= b;
		m_BitCounter++;
		
		// Check if Message is complete
		if (m_BitCounter > 35)
		{
			// It is, create the message
			m_BitCounter = 0;

            decodeMessage(m_Message);
		}
	}

    public void decodeMessage(long binaryMessage) {
        ProtocolMessage message = new ProtocolMessage("UPM", 0, 0, 5);
        message.setRawMessageByteAt(4, (int)binaryMessage &  0x00000000F);
        message.setRawMessageByteAt(3, (int)(binaryMessage & 0x000000FF0) >> 4);
        message.setRawMessageByteAt(2, (int)(binaryMessage & 0x0000FF000) >> 12);
        message.setRawMessageByteAt(1, (int)(binaryMessage & 0x00FF00000) >> 20);
        message.setRawMessageByteAt(0, (int)(binaryMessage >> 28));
			
			/*
			 * My current understanding of the UPM data message:
			 * The message consists of four bytes.
			 * x = Wake Up Code (1100)
			 * c = House Code (0 - 15)
			 * d = Device Code (1 - 4) ?
			 * p = Primary value - Temperature/Rain/Wind speed value (low bits)
			 * P = Primary value - Temperature/Rain/Wind speed value (high bits)
			 * s = Secondary value - Humidity/Wind direction (low bits)
			 * S = Secondary value - Humidity/Wind direction (high bits)
			 * b = Low battery indication
			 * z = Sequence number 0 - 2. Messages are sent in bursts of 3. For some senders this is always 0
			 * C = Checksum. bit 1 is XOR of odd bits, bit 0 XOR of even bits in message
			 * 
			 * If HouseCode = 10 and deviceCode = 2, then p and P is Wind speed
			 * and h and H is Wind direction
			 * 
			 * If HouseCode = 10 and deviceCode = 3, then p and P is rain
			 * 
			 * ____Byte 0_____  ____Byte 1_____  ____Byte 2_____  ____Byte 3_____  _Nib4__
			 * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  3 2 1 0
			 * x x x x c c c c  d d 1 1 b S S S  s s s s 0 P P P  p p p p p p p p  z z C C
			 *                                            
			 * Temp (C) = RawValue / 16 - 50
			 * Rain (total mm) = RawValue * 0,7
			 * Wind Speed (mph)= RawValue (* 1/3,6 for km/h)
			 * Humidity (%) = RawValue
			 * Wind direction (deg) = RawValue * 45
			 * 
			 */
        int houseCode  = 0;
        int deviceCode = 0;
        int primary = 0;
        int secondary = 0;
        int windSpeed = 0;
        int windDirection = 0;
        int rainfall = 0;
        int lowBattery = 0;
        int sequenceNumber = 0;
        int checksum = 0;

        houseCode = message.getRawMessage()[0] & 0x0F;
        deviceCode = ((message.getRawMessage()[1] >> 6) & 0x03) + 1;
        primary = ((message.getRawMessage()[2] & 0x0F) << 8) + message.getRawMessage()[3];
        secondary = ((message.getRawMessage()[1] & 0x07) << 4) + (message.getRawMessage()[2] >> 4);
        lowBattery = (message.getRawMessage()[1] >> 3) & 0x01;
        sequenceNumber = (message.getRawMessage()[4] >> 2) & 0x03;
        checksum = message.getRawMessage()[4] & 0x03;

        // Calculate checksum from message
        int calculatedChecksum = 0;
        for (int bcount = 2; bcount < 36; bcount += 2) {
            calculatedChecksum ^= (binaryMessage >> bcount) & 0x03;
        }

        message.addField(new FieldValue("HouseCode", houseCode));
        message.addField(new FieldValue("DeviceCode", deviceCode));
        message.addField(new FieldValue("Primary", primary));
        message.addField(new FieldValue("Secondary", secondary));

        // Battery Indicator
        message.addField(new FieldValue("LowBattery", lowBattery));

        message.setCommand(primary);
        message.setAddress(deviceCode + (houseCode << 4));

        // Protocol Information
        message.addField(new FieldValue("SequenceNumber", sequenceNumber));

        // Check if this really is a repeat
        if ((m_RepeatCount > 0) && ((m_LastMessage >> 4) == (binaryMessage >> 4))) {
            message.setRepeat(m_RepeatCount);
        }
        else {
            // It is not a repeat, reset counter
            m_RepeatCount = 0;
        }

        // Report the signal if the checksum is correct
        if (checksum == calculatedChecksum) {
            // Report the parsed message
            m_Sink.parsedMessage(message);
        } else {
            m_Sink.partiallyParsedMessage("UPM Checksum Error", m_BitCounter);
        }
        // Reset state
        m_State = REPEAT_SCAN;
    }

    /**
	 * Report that parsing of a protocol message was aborted due to an error in
	 * the received data.
	 * @param length The length of the bad pulse received
	 */
	protected void partiallyParsed(double length) {
		if (m_BitCounter > 1) {
			m_Sink.partiallyParsedMessage("UPM: " + Double.toString(length), m_BitCounter);
		}
	}
	
	/** 
	 * This is a state machine for decoding Biphase Differential Manchester encoding.
	 * Input is pulses, and it will call handleBit() as soon as it discoverers a
	 * new bit in the pulse sequence. 
	 * Mid bit transition is clocking only
	 * Transition at start of bit period represents zero
	 * No transition at start of bit period represents one
	 *  
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, UPMSHORT)) {
					m_State = HI_BETWEEN;
					m_BitCounter = 0;
					m_Message = 0;
					break;
				} else if (pulseCompare(pulse, UPMLONG)) {
					m_State = HI_IN;
					m_BitCounter = 0;
					m_Message = 0;
					addBit(0);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case HI_IN: {
				if (pulseCompare(pulse, UPMSHORT)) {
					m_State = LO_BETWEEN;
				} else if (pulseCompare(pulse, UPMLONG)) {
					m_State = LO_IN;
					addBit(0);
				} else {
					partiallyParsed(pulse);
					m_State = IDLE;
				}
				break;
			}
			case HI_BETWEEN: {
				if (pulseCompare(pulse, UPMSHORT)) {
					m_State = LO_IN;
					addBit(1);
				} else {
					partiallyParsed(pulse);
					m_State = IDLE;
				}
				break;
			}
			case LO_IN: {
				if (pulseCompare(pulse, UPMSHORT)) {
					m_State = HI_BETWEEN;
					break;
				} else if (pulseCompare(pulse, UPMLONG)) {
					m_State = HI_IN;
					addBit(0);
				} else {
					partiallyParsed(pulse);
					m_State = IDLE;
				}
				break;
			}
			case LO_BETWEEN: {
				if (pulseCompare(pulse, UPMSHORT)) {
					m_State = HI_IN;
					addBit(1);
				} else {
					partiallyParsed(pulse);
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (pulseCompare(pulse, UPMSHORT) || pulseCompare(pulse, UPMEND1) || pulseCompare(pulse, UPMEND2) ||
						pulseCompare(pulse, UPMLONG) ) {
					break;
				} else	if ((pulse > UPMREPEAT_MIN) && (pulse < UPMREPEAT_MAX)) {
					m_RepeatCount++;
					m_State = IDLE;
					m_LastMessage = m_Message;
				} else {
					m_RepeatCount = 0;
					m_State = IDLE;
				}
				break;
			}
		}
        return m_State;
	}
}
