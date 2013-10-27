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
import nu.nethome.util.ps.FieldValue;
import nu.nethome.util.ps.ProtocolMessage;
import nu.nethome.util.ps.PulseLength;

/**
 * The RisingSunDecoder parses a set of pulse lengths and decodes a protocol used
 * by UPM-thermometers which is transmitted over 433MHz AM-modulated RF-signal.
 * The reception of the pulses may for example be made via the AudioProtocolSampler.
 * The RisingSunDecoder implements the ProtocolDecoder-interface and accepts the pulses
 * one by one. It contains a state machine, and when a complete message is decoded,
 * this is reported over the ProtocolDecoderSink-interface which is given at 
 * construction.
 * 
 * The protocol is mark length encoded and the protocol messages has the following
 * layout:<br>
 * 
 * s = Start bit = 0<br>
 * a = Channel 1 not selected<br>
 * b = Channel 2 not selected<br>
 * c = Channel 3 not selected<br>
 * d = Channel 4 not selected<br>
 * e = Button 1 not pressed<br>
 * f = Button 2 not pressed<br>
 * g = Button 3 not pressed<br>
 * h = Button 4 not pressed<br>
 * o = On/Off-bit<br>
 * <br>
 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_<br>
 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0<br>
 *  x o x x x x x x  x h x g x f x e  x d x c x b x a   s<br>
 *
 * This protocol is almost exactly the same as NEXA, so the entire implementation is reused.
 *  
 * @author Stefan
 *
 */

@Plugin
public class RisingSunDecoder extends NexaDecoder{

	protected final static int buttonmapping[] = {10,11,12,13,14,15,16,4,18,19,10,3,22,2,1,0};
	
	// This are the pulse length constants for the protocol. The default values may
	// be overridden by system properties
	public static final PulseLength RISING_SUN_LONG_MARK = 
		new PulseLength(RisingSunDecoder.class,"RISING_SUN_LONG_MARK", 1300, 200);
	public static final PulseLength RISING_SUN_SHORT_MARK = 
		new PulseLength(RisingSunDecoder.class,"RISING_SUN_SHORT_MARK", 450, 200);
	public static final PulseLength RISING_SUN_LONG_SPACE = 
		new PulseLength(RisingSunDecoder.class,"RISING_SUN_LONG_SPACE", 1280, 200);
	public static final PulseLength RISING_SUN_SHORT_SPACE = 
		new PulseLength(RisingSunDecoder.class,"RISING_SUN_SHORT_SPACE", 420, 150);
	public static final PulseLength RISING_SUN_REPEAT = 
		new PulseLength(RisingSunDecoder.class,"RISING_SUN_REPEAT", 13400, 500);

	public void setup() {
		m_ProtocolName = "RisingSun";
		LONG_MARK = RISING_SUN_LONG_MARK;
		SHORT_MARK = RISING_SUN_SHORT_MARK;
		LONG_SPACE = RISING_SUN_LONG_SPACE;
		SHORT_SPACE = RISING_SUN_SHORT_SPACE;
		REPEAT = RISING_SUN_REPEAT;
	}
	
	protected int bytemap(int raw) {
		return buttonmapping[(raw & 1) + ((raw >> 1) & 2) + ((raw >> 2) & 4) + ((raw >> 3) & 8)];
	}
	
	protected void addBit(int b) {
		// Shift in data
		m_Data >>= 1;
		m_Data |= (b << 24);
		// Check if this is a complete message
		if (m_BitCounter == 24){
			// It is, create the message
			int command = (m_Data >> 23) & 1;
			int button = bytemap((m_Data >> 9) & 0xFF);
			int address = bytemap((m_Data >> 1) & 0xFF);
			
			// Sender ends a message sequence by a signal saying "no button is pressed". 
			// We ignore that message.
			if (button == 0) {
				m_State = IDLE;
				m_RepeatCount = 0;
				return;
			}
			
			ProtocolMessage message = new ProtocolMessage(m_ProtocolName, command, (button << 4) + address, 4);
			message.setRawMessageByteAt(3, m_Data & 0x1);
			message.setRawMessageByteAt(0, (m_Data >> 17) & 0xFF);
			message.setRawMessageByteAt(1, (m_Data >> 9) & 0xFF);
			message.setRawMessageByteAt(2, (m_Data >> 1) & 0xFF);
			
			message.addField(new FieldValue("Command", command));
			message.addField(new FieldValue("Button", button));
			message.addField(new FieldValue("Channel", address));

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
			m_State = REPEAT_SCAN;
		}
		m_BitCounter++;
	}
}
