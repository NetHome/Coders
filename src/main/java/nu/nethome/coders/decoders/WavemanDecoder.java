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
import nu.nethome.util.ps.ProtocolDecoder;

/**
 * The WavemanDecoder parses a set of pulse lengths and decodes a protocol used
 * by UPM-thermometers which is transmitted over 433MHz AM-modulated RF-signal.
 * The reception of the pulses may for example be made via the AudioProtocolSampler.
 * The WavemanDecoder implements the ProtocolDecoder-interface and accepts the pulses
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
 *  x o x o x o x x  x b x b x b x b  x a x a x a x a   s<br>
 *
 * This protocol is almost exactly the same as NEXA, so the entire implementation is reused.
 * The difference is that bit 2 and 4 of Byte 2 is always 1 in NEXA.
 *  
 * @author Stefan
 *
 */
@Plugin
public class WavemanDecoder extends NexaDecoder implements ProtocolDecoder{

	public void setup() {
		m_ProtocolName = "Waveman";
	}
}
