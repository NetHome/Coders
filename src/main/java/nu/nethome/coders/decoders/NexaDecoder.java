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
 * The NexaDecoder parses a set of pulse lengths and decodes a protocol used
 * by UPM-thermometers which is transmitted over 433MHz AM-modulated RF-signal.
 * The reception of the pulses may for example be made via the AudioProtocolSampler.
 * The NexaDecoder implements the ProtocolDecoder-interface and accepts the pulses
 * one by one. It contains a state machine, and when a complete message is decoded,
 * this is reported over the ProtocolDecoderSink-interface which is given at 
 * construction.
 * 
 * The protocol is mark length encoded and the protocol messages has the following
 * layout:<br>
 * 
 * s = Start bit = 0<br>
 * b = Button number<br>
 * a = Address<br>
 * o = On/Off-bit<br>
 * <br>
 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_<br>
 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0<br>
 *  x o x 1 x 1 x x  x b x b x b x b  x a x a x a x a   s<br>
 *
 * @author Stefan
 *
 */
@Plugin
public class NexaDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_BIT_MARK = 1;
	protected static final int READING_BIT_SHORT_SPACE = 2;
	protected static final int READING_BIT_LONG_SPACE = 3;
	protected static final int REPEAT_SCAN = 4;

	protected static final String s_StateNames[] = {"IDLE","READING_BIT_MARK","READING_BIT_SHORT_SPACE",
			"READING_BIT_LONG_SPACE","REPEAT_SCAN"};
	
	protected int m_State = IDLE;
		
	// This are the pulse length constants for the protocol. The default values may
	// be overridden by system properties
	public static final PulseLength NEXA_LONG_MARK = 
		new PulseLength(NexaDecoder.class,"NEXA_LONG_MARK", 1065, 243);
	public static final PulseLength NEXA_SHORT_MARK = 
		new PulseLength(NexaDecoder.class,"NEXA_SHORT_MARK", 345, 99);
	public static final PulseLength NEXA_LONG_SPACE = 
		new PulseLength(NexaDecoder.class,"NEXA_LONG_SPACE", 1105, 251);
	public static final PulseLength NEXA_SHORT_SPACE = 
		new PulseLength(NexaDecoder.class,"NEXA_SHORT_SPACE", 385, 107);
	public static final PulseLength NEXA_REPEAT = 
		new PulseLength(NexaDecoder.class,"NEXA_REPEAT", 11550, 2340);

	protected PulseLength LONG_MARK = NEXA_LONG_MARK;
	protected PulseLength SHORT_MARK = NEXA_SHORT_MARK;
	protected PulseLength LONG_SPACE = NEXA_LONG_SPACE;
	protected PulseLength SHORT_SPACE = NEXA_SHORT_SPACE;
	protected PulseLength REPEAT = NEXA_REPEAT;

	int m_Data = 0;
	int m_LastData = 0;

	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;
	private double m_LastPulse = REPEAT.length() / 2;
	public StatePulseAnalyzer analyzer = new StatePulseAnalyzer();
	private boolean m_PrintAnalyze = false;
	protected String m_ProtocolName;

	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
		setup();
	}

    public NexaDecoder() {
        setup();
    }
	
	/**
	 * Template method pattern. Used to initiate variables that may be modified by
	 * subclasses.
	 */
	public void setup() {
		m_ProtocolName = "Nexa";
	}

	public ProtocolInfo getInfo() {
		return new ProtocolInfo(m_ProtocolName, "Mark Length", m_ProtocolName, 25, 5);
	}

	/**
	 * s = Start bit = 0
	 * b = Button number
	 * a = Address
	 * o = On/Off-bit
	 * 
	 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_
	 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0
	 *  x o x 1 x 1 x x  x b x b x b x b  x a x a x a x a   s
	 */
	protected void addBit(int b) {
		// Shift in data
		m_Data >>= 1;
		m_Data |= (b << 24);
		// Check if this is a complete message
		if (m_BitCounter == 24){
			// It is, create the message
            decodeMessage(m_Data);
		}
		m_BitCounter++;
	}

    public void decodeMessage(int binaryMessage) {
        int command = (binaryMessage >> 23) & 1;
        int button =  ((binaryMessage >> 9) & 1) + ((binaryMessage >> 10) & 2) + ((binaryMessage >> 11) & 4) + ((binaryMessage >> 12) & 8) + 1;
        int address =  ((binaryMessage >> 1) & 1) + ((binaryMessage >> 2) & 2) + ((binaryMessage >> 3) & 4) + ((binaryMessage >> 4) & 8);
        ProtocolMessage message = new ProtocolMessage(m_ProtocolName, command, (button << 8) + address, 4);
        message.setRawMessageByteAt(3, binaryMessage & 0x1);
        message.setRawMessageByteAt(2, (binaryMessage >> 1) & 0xFF);
        message.setRawMessageByteAt(1, (binaryMessage >> 9) & 0xFF);
        message.setRawMessageByteAt(0, (binaryMessage >> 17) & 0xFF);

        message.addField(new FieldValue("Command", command));
        message.addField(new FieldValue("Button", button));
        message.addField(new FieldValue("HouseCode", address));

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
        m_State = REPEAT_SCAN;
        if (m_PrintAnalyze) {
            analyzer.printPulses();
        }
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
				if (LONG_MARK.matches(pulse) && state && (m_LastPulse > (REPEAT.length() / 2))) {
					m_State = READING_BIT_SHORT_SPACE;
					m_Data = 0;
					m_BitCounter = 0;
					addBit(1);
				} else if (SHORT_MARK.matches(pulse) && state && (m_LastPulse > (REPEAT.length() / 2))) {
					m_State = READING_BIT_LONG_SPACE;
					m_Data = 0;
					m_BitCounter = 0;					
					addBit(0);
				}
				break;
			}
			case READING_BIT_MARK: {
				if (LONG_MARK.matches(pulse) && state) {
					m_State = READING_BIT_SHORT_SPACE;
					addBit(1);
				} else if (SHORT_MARK.matches(pulse) && state) {
					m_State = READING_BIT_LONG_SPACE;
					addBit(0);
				}
				else {
					partiallyParsed("M" ,pulse);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_SHORT_SPACE: {
				if (SHORT_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK;
				}
				else {
					partiallyParsed("SS", pulse);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_LONG_SPACE: {
				if (LONG_SPACE.matches(pulse)) {
					m_State = READING_BIT_MARK;
				} else if (SHORT_SPACE.matches(pulse)) {
					// Special signalling - group sending
					m_State = READING_BIT_MARK;
				} else {
					partiallyParsed("LS", pulse);
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

