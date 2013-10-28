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
public class DeltronicDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER_MARK = 1;
	protected static final int READING_HEADER_SPACE = 2;
	protected static final int READING_BIT_MARK = 3;
	protected static final int READING_BIT_SPACE = 4;
	protected static final int READING_BIT_SPACE_AFTER_SHORT = 5;
	protected static final int READING_BIT_SPACE_AFTER_LONG = 6;
	protected static final int READING_INTER_SPACE = 7;
	protected static final int READING_AFTER_BIT_MARK = 8;
	protected static final int REPEAT_SCAN = 12;
	protected int m_State = IDLE;

	protected static final int DELTRONIC_HEADER_MARK = 330 - 60;
	protected static final int DELTRONIC_HEADER_SPACE = 830 + 60;
	protected static final int DELTRONIC_LONG_MARK = 835 - 60;
	protected static final int DELTRONIC_SHORT_MARK = 280 - 60;
	protected static final int DELTRONIC_LONG_SPACE = 845 + 60;
	protected static final int DELTRONIC_SHORT_SPACE = 300 + 60;
	protected static final int DELTRONIC_INTER_SPACE = 850 + 60;
	protected static final int DELTRONIC_REPEAT = 8810 + 60;
	
	protected static final int MESSAGE_LENGTH = 12;
	
	
	long m_Data = 0;
	long m_LastData = 0;

	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected ProtocolDecoderSink m_Sink = null;
	protected double m_LastPulse = 4000;
	
	public StatePulseAnalyzer analyzer = new StatePulseAnalyzer();
	
	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("Deltronic", "Space Length", "Deltronic", 12, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < (standard * 0.20 + 45);
	}
	
	/**
	 * a = Address
	 * A = Button A
	 * B = Button B
	 * C = Button C
	 * D = Button D
	 * f = Off-bit
	 * n = On-bit
	 * 
	 *  ____Byte 1_____  ____Byte 0_____
	 *          3 2 1 0  7 6 5 4 3 2 1 0
	 *          a a a a  a a A B D C f n
	 */
	protected void addBit(int b) {
		m_Data <<= 1;
		m_Data |= b;
		// Check if this is a complete message
		if (m_BitCounter == (MESSAGE_LENGTH - 1)){
			// It is, create the message
			int command = getCommand((int) m_Data & 0x3);
			int button = getButton((int)(m_Data >> 2) & 0x0F);
			if ((command == -1) || (button == -1)) {
				// Bad data, do not process
				m_Sink.partiallyParsedMessage("Deltronic (Incorrect data)", m_BitCounter + 1);
				return;
			}
			int address = (int) m_Data >> 6;
			ProtocolMessage message = new ProtocolMessage("Deltronic", command, button + (address << 2), 2);
			message.setRawMessageByteAt(1, (int) (m_Data & 0xFF));
			message.setRawMessageByteAt(0, (int) ((m_Data >> 8) & 0xFF));
			
			message.addField(new FieldValue("Command", command));
			message.addField(new FieldValue("Button", button));
			message.addField(new FieldValue("Address", address));

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
			// analyzer.printPulses();
		}
		m_BitCounter++;
	}
	

	protected int getCommand(int data) {
		switch (data) {
		case 0x01:
			return 1;
		case 0x02:
			return 0;
		}
		return -1;
	}
	
	protected int getButton(int data) {
		switch (data) {
		case 0x01:
			return 2;
		case 0x02:
			return 3;
		case 0x04:
			return 1;
		case 0x08:
			return 0;
		}
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				// Make sure we had a reasonably long space before the header mark, so we don't fall into
				// the parsing states in the middle of a signal.
				if (pulseCompare(pulse, DELTRONIC_HEADER_MARK) && (m_LastPulse > (DELTRONIC_REPEAT / 2))) {
					m_State = READING_HEADER_SPACE;
					analyzer.addPulse("DELTRONIC_HEADER_MARK", pulse);
				}
				break;
			}
			case READING_HEADER_SPACE: {
				if (pulseCompare(pulse, DELTRONIC_HEADER_SPACE)) {
					m_State = READING_BIT_MARK;
					analyzer.addPulse("DELTRONIC_HEADER_SPACE", pulse);
					m_Data = 0;
					m_BitCounter = 0;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_MARK: {
				if (pulseCompare(pulse, DELTRONIC_LONG_MARK)) {
					m_State = READING_BIT_SPACE_AFTER_LONG;
					analyzer.addPulse("DELTRONIC_LONG_MARK", pulse);
					addBit(0);
				}
				else if (pulseCompare(pulse, DELTRONIC_SHORT_MARK)) {
					m_State = READING_BIT_SPACE_AFTER_SHORT;
					analyzer.addPulse("DELTRONIC_SHORT_MARK", pulse);
					addBit(1);
				} else {
					m_Sink.partiallyParsedMessage("Deltronic RBM" + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_SPACE_AFTER_LONG: {
				if (pulseCompare(pulse, DELTRONIC_SHORT_SPACE)) {
					m_State = READING_AFTER_BIT_MARK;
					analyzer.addPulse("DELTRONIC_SHORT_SPACE", pulse);
				}
				else {
					m_Sink.partiallyParsedMessage("Deltronic BSAL " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_BIT_SPACE_AFTER_SHORT: {
				if (pulseCompare(pulse, DELTRONIC_LONG_SPACE)) {
					m_State = READING_AFTER_BIT_MARK;
					analyzer.addPulse("DELTRONIC_LONG_SPACE", pulse);
				}
				else {
					m_Sink.partiallyParsedMessage("Deltronic BSAS " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_AFTER_BIT_MARK: {
				if (pulseCompare(pulse, DELTRONIC_SHORT_MARK)) {
					if (m_BitCounter == MESSAGE_LENGTH) {
						m_State = REPEAT_SCAN;
					} else {
						m_State = READING_INTER_SPACE;
					}
					analyzer.addPulse("DELTRONIC_SHORT_MARK", pulse);
				}
				else {
					m_Sink.partiallyParsedMessage("Deltronic ABM " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case READING_INTER_SPACE: {
				if (pulseCompare(pulse, DELTRONIC_LONG_SPACE)) {
					m_State = READING_BIT_MARK;
					analyzer.addPulse("DELTRONIC_INTER_SPACE", pulse);
				}
				else {
					m_Sink.partiallyParsedMessage("Deltronic IS " + Double.toString(pulse), m_BitCounter);
					m_State = IDLE;
				}
				break;
			}
			case REPEAT_SCAN: {
				if (pulseCompare(pulse, DELTRONIC_REPEAT)) {
					analyzer.addPulse("DELTRONIC_REPEAT", pulse);
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

