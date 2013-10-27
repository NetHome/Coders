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

import nu.nethome.coders.decoders.ZhejiangDecoder;
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

@Plugin
public class ZhejiangEncoder implements ProtocolEncoder {

    public static final int ZHEJIANG_RAW_MESSAGE_LENGTH = 25;

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Zhejiang", "Mark Length", "Zhejiang", 25, 5);
    }

    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        int command = 0;
        int address = 0;
        int button  = 0;
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Command")) {
                command = field.getValue();
            } else if (field.getName().equals("Address")) {
                address = field.getValue();
            } else if (field.getName().equals("Button")) {
                button = field.getValue();
            } else {
                throw new BadMessageException(field);
            }
        }
        if (command < 0 || command > 1 || address < 0 || address > 31 || button < 0 || button > 4) {
            throw new BadMessageException(null);
        }
        return encode(command, button, address);
    }

    @Override
    public int modulationFrequency(Message message) {
        return 0;
    }

    public static Message buildMessage(int command, int button, int address) {
        ProtocolMessage result = new ProtocolMessage("Zhejiang", command, address, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("Address", address));
        result.addField(new FieldValue("Button", button));
        return result;
    }

    private int[] encode(int command, int button, int address) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        long message = 0x2003FF;
        message = copyBit(command, 0, message, 21, false);
        message = copyBit(command, 0, message, 23, true);
        int buttonBit = 1 << button;
        message = copyBit(buttonBit, 0, message, 11, true);
        message = copyBit(buttonBit, 1, message, 13, true);
        message = copyBit(buttonBit, 2, message, 15, true);
        message = copyBit(buttonBit, 3, message, 17, true);
        message = copyBit(buttonBit, 4, message, 19, true);

        message = copyBit(address, 0, message, 0, true);
        message = copyBit(address, 1, message, 2, true);
        message = copyBit(address, 2, message, 4, true);
        message = copyBit(address, 3, message, 6, true);
        message = copyBit(address, 4, message, 8, true);

        // Encode message bits
        for (int j = 0; j < (ZHEJIANG_RAW_MESSAGE_LENGTH); j++) {
            if ((message & 1) == 1) {
                result.add(ZhejiangDecoder.ZHEJ_LONG_MARK.length());
                result.add(ZhejiangDecoder.ZHEJ_SHORT_SPACE.length());
            } else {
                result.add(ZhejiangDecoder.ZHEJ_SHORT_MARK.length());
                result.add(ZhejiangDecoder.ZHEJ_LONG_SPACE.length());
            }
            message >>= 1;
        }
        result.remove(result.size() - 1);
        result.add(ZhejiangDecoder.ZHEJ_REPEAT.length());

        int resultArray[] = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            resultArray[i] = result.get(i);
        }
        return resultArray;
    }

    private long copyBit(int source, int sourceBit, long destination, int destinationBit, boolean invert) {
        if ((((source >> sourceBit) & 1) == 1) ^ invert) {
            return destination | (1L << destinationBit);
        }
        return destination & ~(1L << destinationBit);
    }
}
