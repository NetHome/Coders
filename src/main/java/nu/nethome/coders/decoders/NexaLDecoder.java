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
 *
 */
@Plugin
public class NexaLDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER_MARK = 1;
	protected static final int READING_HEADER_SPACE = 2;
	protected static final int READING_BIT_MARK_BEFORE = 3;
	protected static final int READING_BIT_SPACE = 4;
	protected static final int READING_BIT_MARK_AFTER_SHORT = 5;
	protected static final int READING_BIT_MARK_AFTER_LONG = 6;
	protected static final int READING_INTER_SPACE_SHORT = 7;
	protected static final int READING_INTER_SPACE_LONG = 8;
	protected static final int READING_LAST_BIT_MARK = 9;
	protected static final int READING_TRAILING_SPACE = 10;
	protected static final int READING_TRAILING_MARK = 11;
	protected static final int REPEAT_SCAN = 12;
	
	protected static final String s_StateNames[] = {"IDLE","READING_HEADER_MARK","READING_HEADER_SPACE",
		"READING_BIT_MARK_BEFORE","READING_BIT_SPACE","READING_BIT_MARK_AFTER_SHORT","READING_BIT_MARK_AFTER_LONG",
		"READING_INTER_SPACE_SHORT","READING_INTER_SPACE_LONG","READING_LAST_BIT_MARK","READING_TRAILING_SPACE",
		"READING_TRAILING_MARK","REPEAT_SCAN"};
	
	protected int m_State = IDLE;

//	protected static final int NEXA_HEADER_MARK = 290; //142 - 438
//	protected static final int NEXA_HEADER_SPACE = 2615; // 2021 - 3218
//	protected static final int NEXA_MARK = 250; //283 + 60;
//	protected static final int NEXA_LONG_SPACE = 1080; //1065 - 60;
//	protected static final int NEXA_SHORT_SPACE = 280; // 283 - 60;
//	protected static final int NEXA_LONG_INTER_SPACE = 1120; //1099 - 60;
//	protected static final int NEXA_SHORT_INTER_SPACE = 325; //383; // 323 - 60;
//	protected static final int NEXA_TRAILING_SPACE = 280; //383; // 323 - 60;
//	protected static final int NEXA_REPEAT = 9600; //9557 - 60;

	// This are the pulse length constants for the protocol. The default values may
	// be overridden by system properties
	public static final PulseLength NEXA_HEADER_MARK = 
		new PulseLength(NexaLDecoder.class,"NEXA_HEADER_MARK", 290, 200, 410);
	public static final PulseLength NEXA_HEADER_SPACE = 
		new PulseLength(NexaLDecoder.class,"NEXA_HEADER_SPACE", 2615, 553);
	public static final PulseLength NEXA_MARK = 
		new PulseLength(NexaLDecoder.class,"NEXA_MARK", 250, 170, 435); //365
	public static final PulseLength NEXA_LONG_SPACE = 
		new PulseLength(NexaLDecoder.class,"NEXA_LONG_SPACE", 1100, 250);
	public static final PulseLength NEXA_SHORT_SPACE = 
		new PulseLength(NexaLDecoder.class,"NEXA_SHORT_SPACE", 280, 130, 366); // 180
	public static final PulseLength NEXA_LONG_INTER_SPACE = 
		new PulseLength(NexaLDecoder.class,"NEXA_LONG_INTER_SPACE", 1120, 254);	
	public static final PulseLength NEXA_SHORT_INTER_SPACE = 
		new PulseLength(NexaLDecoder.class,"NEXA_SHORT_INTER_SPACE", 325, 150, 420); //200
	public static final PulseLength NEXA_REPEAT = 
		new PulseLength(NexaLDecoder.class,"NEXA_REPEAT", 9600, 1950);
	
	
	long m_Data = 0;
	long m_LastData = 0;

	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;
	public StatePulseAnalyzer analyzer = new StatePulseAnalyzer();
	private double m_LastPulse = NEXA_REPEAT.length() / 2;
	private boolean m_PrintAnalyze = false;
	
	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("NexaL", "Space Length", "Nexa", 32, 5);
	}

	/**
	 * a = Address
	 * s = On/Off-bit
	 * g = Group bit
	 * b = Button
	 * 
	 *  ____Byte 3_____  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____
	 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0
	 *  a a a a a a a a  a a a a a a a a  a a a a a a a a  a a g s b b b b  
	 */
	protected void addBit(int b) {
		m_Data <<= 1;
		m_Data |= b;
		// Check if this is a complete message
		if (m_BitCounter == 31){
			// It is, create the message
            decodeMessage(m_Data);
		}
		m_BitCounter++;
	}

    public void decodeMessage(long binaryMessage) {
        int command = (int)(binaryMessage >> 4) & 0x1;
        int address = (int)((binaryMessage >> 6) & ((1<<26) - 1));
        int button = (int)((binaryMessage & 0x0F) + ((binaryMessage & 0x20) >> 1) + 1);
        ProtocolMessage message = new ProtocolMessage("NexaL", command, address, 4);
        message.setRawMessageByteAt(3, (int) (binaryMessage & 0xFF));
        message.setRawMessageByteAt(2, (int) ((binaryMessage >> 8) & 0xFF));
        message.setRawMessageByteAt(1, (int) ((binaryMessage >> 16) & 0xFF));
        message.setRawMessageByteAt(0, (int) ((binaryMessage >> 24) & 0xFF));

        message.addField(new FieldValue("Command", command));
        message.addField(new FieldValue("Address", address));
        message.addField(new FieldValue("Button", button));

        // It is, check if this really is a repeat
        if ((m_RepeatCount > 0) && (binaryMessage == m_LastData)) {
            message.setRepeat(m_RepeatCount);
        }
        else {
            // It is not a repeat, reset counter
            m_RepeatCount = 0;
        }
        // Report the parsed message
        m_Sink.parsedMessage(message);
        if (m_PrintAnalyze) {
            analyzer.printPulses();
        }
        m_State = READING_LAST_BIT_MARK;
    }

    /* (non-Javadoc)
     * @see ssg.ir.IRDecoder#parse(java.lang.Double)
     */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (NEXA_HEADER_MARK.matches(pulse)  && (m_LastPulse  > (NEXA_REPEAT.length() / 2))) {
					m_State = READING_HEADER_SPACE;
				}
				break;
			}
			case READING_HEADER_SPACE: {
				if (NEXA_HEADER_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK_BEFORE;
					m_Data = 0;
					m_BitCounter = 0;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_MARK_BEFORE: {
				if (NEXA_MARK.matches(pulse)) {
					m_State = READING_BIT_SPACE;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL BMB " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_SPACE: {
				if (NEXA_SHORT_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK_AFTER_SHORT;
					addBit(0);
				}
				else if (NEXA_LONG_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK_AFTER_LONG;
					addBit(1);
				} else {
					m_Sink.partiallyParsedMessage("NexaL BS " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_MARK_AFTER_SHORT: {
				if (NEXA_MARK.matches(pulse)) {
					m_State = READING_INTER_SPACE_LONG;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL BMAS " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_MARK_AFTER_LONG: {
				if (NEXA_MARK.matches(pulse)) {
					m_State = READING_INTER_SPACE_SHORT;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL BMAL " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_INTER_SPACE_SHORT: {
				if (NEXA_SHORT_INTER_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK_BEFORE;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL ISS " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_INTER_SPACE_LONG: {
				if (NEXA_LONG_INTER_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK_BEFORE;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL ISL " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_LAST_BIT_MARK: {
				if (NEXA_MARK.matches(pulse)) {
					m_State = READING_TRAILING_SPACE;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL LBM " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
					m_RepeatCount = 0;
				}
				break;
			}
			case READING_TRAILING_SPACE: {
				if (NEXA_LONG_INTER_SPACE.matches(pulse) || NEXA_SHORT_INTER_SPACE.matches(pulse)) {
					m_State = READING_TRAILING_MARK;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL TS " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
					m_RepeatCount = 0;
				}
				break;
			}
			case READING_TRAILING_MARK: {
				if (NEXA_MARK.matches(pulse)) {
					m_State = REPEAT_SCAN;
				}
				else {
					m_Sink.partiallyParsedMessage("NexaL TM " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
					m_RepeatCount = 0;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (NEXA_REPEAT.matches(pulse)) {
					m_RepeatCount += 1; // Start repeat sequence
					// Save this sequence
					m_LastData = m_Data;
				}
				else {
					m_RepeatCount = 0;
				}
				m_State = IDLE;
				break;
			}
		}
		m_LastPulse = pulse;
        return m_State;
	}
}

