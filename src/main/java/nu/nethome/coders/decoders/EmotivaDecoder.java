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
import nu.nethome.util.ps.FieldValue;
import nu.nethome.util.ps.ProtocolDecoder;
import nu.nethome.util.ps.ProtocolInfo;
import nu.nethome.util.ps.ProtocolMessage;

@Plugin
public class EmotivaDecoder extends PioneerDecoder implements ProtocolDecoder {

    protected int addressLo = 0;
    protected int addressHi = 0;
    protected int command = 0;
    protected int commandChecksum = 0;

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Emotiva", "Space Length", "Emotiva", 24, 5);
    }

    protected void addBit(int b) {
        if (m_BitCounter < 8) {
            addressLo >>= 1;
            addressLo |= (b << 7);
        } else if (m_BitCounter < 16) {
            addressHi >>= 1;
            addressHi |= (b << 7);
        } else if (m_BitCounter < 24) {
            command >>= 1;
            command |= (b << 7);
        } else if (m_BitCounter < 32) {
            commandChecksum >>= 1;
            commandChecksum |= (b << 7);
        }
        // Check if this is a complete message
        if (m_BitCounter == 31) {

            // It is, verify checksum
            if ((command != (commandChecksum ^ 0xFF))) {
                // Checksum error
                reportPartial();
                m_State = IDLE;
                return;
            }

            ProtocolMessage message = new ProtocolMessage("Emotiva", command, addressLo + (addressHi << 8), 3);
            message.setRawMessageByteAt(0, addressLo);
            message.setRawMessageByteAt(1, addressHi);
            message.setRawMessageByteAt(2, command);
            // It is, check if this really is a repeat
            if ((m_RepeatCount > 0) && (addressLo == m_LastCommand) && (command == m_LastAddress)) {
                message.setRepeat(m_RepeatCount);
            } else {
                // It is not a repeat, reset counter
                m_RepeatCount = 0;
            }
            message.addField(new FieldValue("Command", command));
            message.addField(new FieldValue("Address", addressLo + (addressHi << 8)));
            // Report the parsed message
            m_Sink.parsedMessage(message);
            m_State = TRAILING_BIT;
        }
        m_BitCounter++;
    }
}
