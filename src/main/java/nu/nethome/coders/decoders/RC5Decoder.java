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


@Plugin
public class RC5Decoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int HI_IN = 2;
	protected static final int HI_BETWEEN = 3;
	protected static final int LO_IN = 4;
	protected static final int LO_BETWEEN = 5;
	
	protected static final int RC5SHORT = 889;
	protected static final int RC5LONG = 1778;
	protected static final int RC5REPEAT = 85000;

	protected int m_State = IDLE;
	
	protected int m_Header = 0;
	protected int m_Command = 0;
	protected int m_Address = 0;

	protected int m_LastHeader = 0;
	protected int m_LastCommand = 0;
	protected int m_LastAddress = 0;
		
	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected double m_LastValue = 0;
	protected ProtocolDecoderSink m_Sink = null;

	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("RC5", "Manchester", "Philips", 14, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < standard * 0.2;
	}
	
	protected void addBit(int b) {
		if (m_BitCounter < 3) {
			m_Header = (m_Header << 1) + b ;
		} 
		else if (m_BitCounter < 8) {
			m_Address = (m_Address << 1) + b ;
		} 
		else if (m_BitCounter < 14) {
			m_Command = (m_Command << 1) + b ;
		}	
			// Check if this is a complete message 
		if (m_BitCounter == 13){
			// It is, create the message
			ProtocolMessage message = new ProtocolMessage("RC5", m_Command, m_Address, 3);
			message.setRawMessageByteAt(2, m_Command);
			message.setRawMessageByteAt(1, m_Address);
			message.setRawMessageByteAt(0, m_Header);
			
			message.addField(new FieldValue("Header", m_Header));
			message.addField(new FieldValue("Address", m_Address));
			message.addField(new FieldValue("Command", m_Command));

			// Check if this is a repeat
			if ((m_Command == m_LastCommand) && (m_Address == m_LastAddress)
				 && (m_Header == m_LastHeader)) {
				m_RepeatCount++;
				message.setRepeat(m_RepeatCount);
			}
			else {
				// It is not a repeat, reset counter
				m_RepeatCount = 0;
			}
			// Report the parsed message
			m_Sink.parsedMessage(message);
			// Reset state
			m_State = IDLE;
			// Save message for repeat check
			m_LastCommand = m_Command;
			m_LastAddress = m_Address;
			m_LastHeader = m_Header; 
		}
		m_BitCounter++;
	}
	
	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, RC5SHORT) && state && (m_LastValue > 5000)) {
					m_Header = 0;
					m_Command = 0;
					m_Address = 0;
					m_BitCounter = 0;
					m_State = HI_BETWEEN;
					addBit(1);
				}
				else if (pulseCompare(pulse, RC5LONG) && state && (m_LastValue > 5000)) {
					m_Header = 0;
					m_Command = 0;
					m_Address = 0;
					m_BitCounter = 0;
					m_State = HI_IN;
					addBit(1);
					addBit(0);
				}
				if (pulseCompare(m_LastValue, RC5REPEAT) && (m_State != IDLE)) {
					m_RepeatCount++;
				}
				break;
			}
			case HI_IN: {
				if (pulseCompare(pulse, RC5SHORT)) {
					m_State = LO_BETWEEN;
				} else if (pulseCompare(pulse, RC5LONG)) {
					m_State = LO_IN;
					addBit(1);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case HI_BETWEEN: {
				if (pulseCompare(pulse, RC5SHORT)) {
					m_State = LO_IN;
					addBit(1);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case LO_IN: {
				if (pulseCompare(pulse, RC5SHORT)) {
					m_State = HI_BETWEEN;
					break;
				} else if (pulseCompare(pulse, RC5LONG)) {
					m_State = HI_IN;
					addBit(0);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case LO_BETWEEN: {
				if (pulseCompare(pulse, RC5SHORT)) {
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
