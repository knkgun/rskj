/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.rpc.modules.debug;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TraceOptions {

    private static final String[] SUPPORTED_OPTIONS = {
            "disableStorage", "disableMemory", "disableStack"};
    private static final String[] OPTIONAL_FIELDS = {
            "disableStorage", "disableMemory", "disableStack"};

    private Set<String> disabledFields;
    private Set<String> unsupportedOptions;

    private TraceOptions() {}

    public TraceOptions(Map<String, String> traceOptions) {
        disabledFields = Arrays
                .stream(OPTIONAL_FIELDS)
                .filter(field -> traceOptions
                        .containsKey(field) && Boolean.parseBoolean(traceOptions.get(field)))
                .collect(Collectors.toSet());
        unsupportedOptions = traceOptions
                .keySet()
                .stream()
                .filter(key -> Arrays.stream(SUPPORTED_OPTIONS)
                        .noneMatch(option -> option.equals(key)))
                .collect(Collectors.toSet());
    }

    public Set<String> getDisabledFields() {
        return disabledFields;
    }

    public Set<String> getUnsupportedOptions() {
        return unsupportedOptions;
    }

}
