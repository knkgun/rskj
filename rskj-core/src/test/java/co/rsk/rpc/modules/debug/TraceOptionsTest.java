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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TraceOptionsTest {

    @Test
    public void testTraceOptions_constructedOnlyWithSupportedOptions_unsupportedOptionsMustBeEmpty() {
        // Given
        Map<String, String> traceOptions = new HashMap<>();
        traceOptions.put("disableStorage", "false");
        traceOptions.put("disableMemory", "false");
        traceOptions.put("disableStack", "false");

        // When
        TraceOptions options = new TraceOptions(traceOptions);

        // Then
        Assert.assertEquals(0, options.getUnsupportedOptions().size());
    }

    @Test
    public void testTraceOptions_constructedWithSomeUnsupportedOptions_unsupportedOptionsMustContainTheUnsupportedOptions() {
        // Given
        Map<String, String> traceOptions = new HashMap<>();
        traceOptions.put("disableStorages", "false");
        traceOptions.put("disablesMemory", "false");
        traceOptions.put("disableStack", "false");

        // When
        TraceOptions options = new TraceOptions(traceOptions);

        // Then
        Assert.assertTrue(options.getUnsupportedOptions().contains("disableStorages"));
        Assert.assertTrue(options.getUnsupportedOptions().contains("disablesMemory"));
    }

    @Test
    public void testTraceOptions_optionalFieldsSetToTrue_disabledFieldsMustContainGivenFields() {
        // Given
        Map<String, String> traceOptions = new HashMap<>();
        traceOptions.put("disableStorage", "true");
        traceOptions.put("disableMemory", "true");
        traceOptions.put("disableStack", "true");

        // When
        TraceOptions options = new TraceOptions(traceOptions);

        // Then
        Assert.assertEquals(0, options.getUnsupportedOptions().size());
        Assert.assertTrue(options.getDisabledFields().contains("disableStorage"));
        Assert.assertTrue(options.getDisabledFields().contains("disableMemory"));
        Assert.assertTrue(options.getDisabledFields().contains("disableStack"));
    }

    @Test
    public void testTraceOptions_optionalFieldsSetToFalse_disabledFieldsMustNotContainGivenFields() {
        // Given
        Map<String, String> traceOptions = new HashMap<>();
        traceOptions.put("disableStorage", "false");
        traceOptions.put("disableMemory", "false");
        traceOptions.put("disableStack", "false");

        // When
        TraceOptions options = new TraceOptions(traceOptions);

        // Then
        Assert.assertEquals(0, options.getUnsupportedOptions().size());
        Assert.assertFalse(options.getDisabledFields().contains("disableStorages"));
        Assert.assertFalse(options.getDisabledFields().contains("disableMemory"));
        Assert.assertFalse(options.getDisabledFields().contains("disableStack"));
    }

    @Test
    public void testTraceOptions_mixedCase_OK() {
        // Given
        Map<String, String> traceOptions = new HashMap<>();
        traceOptions.put("disableStorage", "false"); // Valid optional field set to false (Must not be added to the disabled fields).
        traceOptions.put("disableStack", "true"); // Valid optional field set to true (Must be added to the disabled fields).
        traceOptions.put("unsupported_1", "false"); // Unsupported field (Must be added to the unsupported options).
        traceOptions.put("unsupported_2", "4"); // Unsupported field (Must be added to the unsupported options).

        // When
        TraceOptions options = new TraceOptions(traceOptions);

        // Then
        Assert.assertFalse(options.getDisabledFields().contains("disableStorage"));
        Assert.assertTrue(options.getDisabledFields().contains("disableStack"));
        Assert.assertTrue(options.getUnsupportedOptions().contains("unsupported_1"));
        Assert.assertTrue(options.getUnsupportedOptions().contains("unsupported_2"));
    }

}
