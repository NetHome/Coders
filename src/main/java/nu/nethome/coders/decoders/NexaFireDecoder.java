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
import nu.nethome.util.ps.*;

/**
 * The NexaFireDecoder parses a set of pulse lengths and decodes a protocol used
 * by Nexa Fire Detector transmitted over 433MHz AM-modulated RF-signal.
 * The reception of the pulses may for example be made via the AudioProtocolSampler.
 * The NexaDecoder implements the ProtocolDecoder-interface and accepts the pulses
 * one by one. It contains a state machine, and when a complete message is decoded,
 * this is reported over the ProtocolDecoderSink-interface which is given at 
 * construction.
 * 
 * The protocol is mark length encoded and the protocol messages has the following
 * layout:<br>
 * 
 * a = Address<br>
 * <br>
 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____<br>
 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0<br>
 *  a a a a a a a a  a a a a a a a a  a a a a a a a a<br>
 *
 * @author Stefan
 *
 */
@Plugin
public class NexaFireDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER_SPACE = 1;
	protected static final int READING_BIT_MARK = 2;
	protected static final int READING_BIT_SPACE = 3;
	protected static final int READING_TRAILING_MARK = 4;
	protected static final int REPEAT_SCAN = 5;

	protected static final String s_StateNames[] = {"IDLE","READING_BIT_MARK","READING_BIT_SHORT_SPACE",
			"READING_BIT_LONG_SPACE","REPEAT_SCAN"};
	
	protected int m_State = IDLE;
		
	// This are the pulse length constants for the protocol. The default values may
	// be overridden by system properties
	public static final PulseLength NEXAF_HEADER_MARK = 
		new PulseLength(NexaDecoder.class,"NEXAF_HEADER_MARK", 8100, 400);
	public static final PulseLength NEXAF_HEADER_SPACE = 
		new PulseLength(NexaDecoder.class,"NEXAF_HEADER_SPACE", 900, 100);
	public static final PulseLength NEXAF_MARK = 
		new PulseLength(NexaDecoder.class,"NEXAF_MARK", 800, 200);
	public static final PulseLength NEXAF_LONG_SPACE = 
		new PulseLength(NexaDecoder.class,"NEXAF_LONG_SPACE", 2740, 300);
	public static final PulseLength NEXAF_SHORT_SPACE = 
		new PulseLength(NexaDecoder.class,"NEXAF_SHORT_SPACE", 1400, 200);
	public static final PulseLength NEXAF_REPEAT = 
		new PulseLength(NexaDecoder.class,"NEXAF_REPEAT", 14500, 2000);

	protected PulseLength HEADER_MARK = NEXAF_HEADER_MARK;
	protected PulseLength HEADER_SPACE = NEXAF_HEADER_SPACE;
	protected PulseLength MARK = NEXAF_MARK;
	protected PulseLength LONG_SPACE = NEXAF_LONG_SPACE;
	protected PulseLength SHORT_SPACE = NEXAF_SHORT_SPACE;
	protected PulseLength REPEAT = NEXAF_REPEAT;

	int m_Data = 0;
	int m_LastData = 0;

	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;
	private double m_LastPulse = REPEAT.length() / 2;
	public StatePulseAnalyzer analyzer = new StatePulseAnalyzer();
	private boolean m_PrintAnalyze = false;
	protected String m_ProtocolName;

	public  void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	/**
	 * Template method pattern. Used to initiate variables that may be modified by
	 * subclasses.
	 */
	public void setup() {
		m_ProtocolName = "NexaFire";
	}
	
	public NexaFireDecoder() {
        setup();
    }
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo(m_ProtocolName, "Space Length", m_ProtocolName, 24, 5);
	}

	/**
 * 
 * a = Address<br>
 * <br>
 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____<br>
 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0<br>
 *  a a a a a a a a  a a a a a a a a  a a a a a a a a<br>
 *
	 */
	protected void addBit(int b) {
		// Shift in data
		m_Data >>= 1;
		m_Data |= (b << 23);
		// Check if this is a complete message
		if (m_BitCounter == 23){
			// It is, create the message
			ProtocolMessage message = new ProtocolMessage(m_ProtocolName, 1, m_Data, 1);
			message.setRawMessageByteAt(0, m_Data);
			
			message.addField(new FieldValue("Address", m_Data));

			// It is, check if this really is a repeat
			if ((m_RepeatCount > 0) && (m_Data == m_LastData)) {
				message.setRepeat(m_RepeatCount);
			}
			else {
				// It is not a repeat, reset counter
				m_RepeatCount = 0;
			}
			// Report the parsed message
			m_Sink.parsedMessage(message);
			m_State = READING_TRAILING_MARK;
			if (m_PrintAnalyze) {
				analyzer.printPulses();
			}
		}
		m_BitCounter++;
	}

	/**
	 * Report that part of a message was parsed, but aborted due to a non valid
	 * pulse length.
	 * 
	 * @param state Current state when parsing was aborted
	 * @param length Length of the pulse which was not accepted
	 */
	protected void partiallyParsed(String state, double length) {
		if (m_BitCounter > 1) {
			m_Sink.partiallyParsedMessage(m_ProtocolName + " " + state + ": " + Double.toString(length), m_BitCounter);
		}
	}

	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (HEADER_MARK.matches(pulse) && state && (m_LastPulse > (REPEAT.length() / 2))) {
					m_State = READING_HEADER_SPACE;
					m_Data = 0;
					m_BitCounter = 0;
				}
				break;
			}
			case READING_HEADER_SPACE: {
				if (HEADER_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK;
				} else {
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_MARK: {
				if (MARK.matches(pulse) && state) {
					m_State = READING_BIT_SPACE;
				} else {
					partiallyParsed("M" ,pulse);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_SPACE: {
				if (LONG_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK;
					addBit(1);
				} else if (SHORT_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK;
					addBit(0);
				} else {
					partiallyParsed("S", pulse);
					m_State = IDLE;
				}
				break;
			}
			case READING_TRAILING_MARK: {
				if (MARK.matches(pulse)) {
					m_State = REPEAT_SCAN;
				} else {
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (REPEAT.matches(pulse)) {
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
		m_LastPulse  = pulse;
        return m_State;
	}
}

