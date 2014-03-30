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

import nu.nethome.util.ps.ProtocolDecoder;

import java.util.ArrayList;
import java.util.Collection;

public class Decoders {
    public static Collection<Class<? extends ProtocolDecoder>> getAllTypes() {
        Collection<Class<? extends ProtocolDecoder>> result = new ArrayList<Class<? extends ProtocolDecoder>>();
        result.add(DeltronicDecoder.class);
        result.add(EmotivaDecoder.class);
        result.add(HKDecoder.class);
        result.add(JVCDecoder.class);
        result.add(NexaDecoder.class);
        result.add(NexaFireDecoder.class);
        result.add(NexaLDecoder.class);
        result.add(PioneerDecoder.class);
        result.add(ProntoDecoder.class);
        result.add(RC5Decoder.class);
        result.add(RC6Decoder.class);
        result.add(RisingSunDecoder.class);
        result.add(SIRCDecoder.class);
        result.add(UPMDecoder.class);
        result.add(ViasatDecoder.class);
        result.add(WavemanDecoder.class);
        result.add(X10Decoder.class);
        result.add(ZhejiangDecoder.class);
        result.add(OregonDecoder.class);
        return result;
    }
}
