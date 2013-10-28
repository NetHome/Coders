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
public class RC6Decoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int READING_HEADER = 1;
	protected static final int HI_IN = 2;
	protected static final int HI_BETWEEN = 3;
	protected static final int LO_IN = 4;
	protected static final int LO_BETWEEN = 5;
	
	protected static final int RC6SHORT = 444;
	protected static final int RC6LONG = 889;

	protected int m_State = IDLE;
	
	protected int m_Header = 0;
	protected int m_Command = 0;
	protected int m_Address = 0;
	protected int m_Extra = 0;

	protected int m_LastCommand = 0;
	protected int m_LastAddress = 0;
	protected int m_LastExtra = 0;
		
	protected int m_BitCounter = 0;
	protected int m_RepeatCount = 0;
	protected double m_LastValue = 0;
	protected ProtocolDecoderSink m_Sink = null;

	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("RC6", "Manchester", "Philips", 20, 5);
	}

	protected boolean pulseCompare(double candidate, double standard) {
		return Math.abs(standard - candidate) < standard * 0.2;
	}
	
	protected void addBit(int b) {
		if (m_BitCounter < 4) {
			m_Header = (m_Header << 1) + b ;
		} 
		else if (m_BitCounter == 4){
			m_Extra = b;
		} 
		else if (m_BitCounter < 13) {
			m_Address = (m_Address << 1) + b ;
		} 
		else if (m_BitCounter < 21) {
			m_Command = (m_Command << 1) + b ;
		}	
			// Check if this is a complete message 
		if (m_BitCounter == 20){
			// It is, create the message
			ProtocolMessage message = new ProtocolMessage("RC6", m_Command, m_Address, 3);
			message.setRawMessageByteAt(0, m_Command);
			message.setRawMessageByteAt(1, m_Address);
			message.setRawMessageByteAt(2, m_Extra);
			
			message.addField(new FieldValue("Header", m_Header));
			message.addField(new FieldValue("Flip", m_Extra));
			message.addField(new FieldValue("Address", m_Address));
			message.addField(new FieldValue("Command", m_Command));

			// Check if this is a repeat
			if ((m_RepeatCount > 0) && (m_Command == m_LastCommand) && (m_Address == m_LastAddress)
				 && (m_Extra == m_LastExtra)) {
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
			m_LastExtra = m_Extra; 
		}
		m_BitCounter++;
	}
	
	/**
	 * Corrects for the fact that the trailer bit is double as long as the other bits. We handle this
	 * by adjusting the received puls lengths when we get to the trailer bit. 
	 * @param pulse The measured pulse
	 * @param bitCounter Which bit we are currently processing
	 * @return The adjusted pulse length
	 */
	protected double trailerBitAdjust(double pulse, int bitCounter) {
		if ((bitCounter == 4) && ((m_State == HI_IN) || (m_State == LO_IN)) && pulseCompare(pulse, 1333)) {
			return RC6LONG;
		}
		else if ((bitCounter == 4) && ((m_State == HI_BETWEEN) || (m_State == LO_BETWEEN))) {
			return pulse / 2.0;
		}
		else if ((bitCounter == 5) && ((m_State == HI_IN) || (m_State == LO_IN))) {
			if (pulseCompare(pulse, 889)) {
				return RC6SHORT;
			}
			else if (pulseCompare(pulse, 1333)) {
				return RC6LONG;
			}
		}
		return pulse;
	}
	
	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		pulse = trailerBitAdjust(pulse, m_BitCounter);
		switch (m_State) {
			case IDLE: {
				if (pulseCompare(pulse, 2666.0) && state) {
					m_State = READING_HEADER;
					m_Header = 0;
					m_Command = 0;
					m_Address = 0;
					m_Extra = 0;
					m_BitCounter = 0;

					if (pulseCompare(m_LastValue, 85000.0)) {
						m_RepeatCount++;
					}
					else {
						m_RepeatCount = 0;
					}
					break;
				}
			}
			case READING_HEADER: {
				if (pulseCompare(pulse, 889.0)) {
					m_State = LO_BETWEEN;
				}
				else {
					m_State = IDLE;
				}
				break;
			}
			case HI_IN: {
				if (pulseCompare(pulse, RC6SHORT)) {
					m_State = LO_BETWEEN;
				} else if (pulseCompare(pulse, RC6LONG)) {
					m_State = LO_IN;
					addBit(0);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case HI_BETWEEN: {
				if (pulseCompare(pulse, RC6SHORT)) {
					m_State = LO_IN;
					addBit(0);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case LO_IN: {
				if (pulseCompare(pulse, RC6SHORT)) {
					m_State = HI_BETWEEN;
					break;
				} else if (pulseCompare(pulse, RC6LONG)) {
					m_State = HI_IN;
					addBit(1);
				} else {
					m_State = IDLE;
				}
				break;
			}
			case LO_BETWEEN: {
				if (pulseCompare(pulse, RC6SHORT)) {
					m_State = HI_IN;
					addBit(1);
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
