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

package nu.nethome.coders.encoders;

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

/**
 * User: Stefan
 * Date: 2013-01-27
 * Time: 21:06
 */
@Plugin
public class EmotivaEncoder implements ProtocolEncoder {


    public static final int EMOTIVA_RAW_MESSAGE_LENGTH = 32;
    public static final int EMOTIVA_HEADER_MARK = 9100;
    public static final int EMOTIVA_HEADER_SPACE = 4400;
    public static final int EMOTIVA_MARK = 630;
    public static final int EMOTIVA_LONG_SPACE = 1600;
    public static final int EMOTIVA_SHORT_SPACE = 500;
    public static final int EMOTIVA_REPEAT_SPACE = 9100;

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Emotiva", "Space Length", "Emotiva", 24, 5);
    }

    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        int command = 0;
        int address = 0;
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Command")) {
                command = field.getValue();
            } else if (field.getName().equals("Address")) {
                address = field.getValue();
            } else {
                throw new BadMessageException(field);
            }
        }
        return encode(command, address);
    }

    @Override
    public int modulationFrequency(Message message) {
        return 40000;
    }

    public static Message buildMessage(int command, int address) {
        ProtocolMessage result = new ProtocolMessage("Emotiva", command, address, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("Address", address));
        return result;
    }

    private int[] encode(int command, int address) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        long message = address + (command << 16) + ((command ^ 0xFF) << 24);
        // Encode header
        result.add(EMOTIVA_HEADER_MARK);
        result.add(EMOTIVA_HEADER_SPACE);

        // Encode message bits
        for (int j = 0; j < (EMOTIVA_RAW_MESSAGE_LENGTH); j++) {
            result.add(EMOTIVA_MARK);
            if ((message & 1) == 1) {
                result.add(EMOTIVA_LONG_SPACE);
            } else {
                result.add(EMOTIVA_SHORT_SPACE);
            }
            message >>= 1;
        }
        result.add(EMOTIVA_MARK);
        result.add(EMOTIVA_REPEAT_SPACE);

        int resultArray[] = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            resultArray[i] = result.get(i);
        }
        return resultArray;
    }
}
