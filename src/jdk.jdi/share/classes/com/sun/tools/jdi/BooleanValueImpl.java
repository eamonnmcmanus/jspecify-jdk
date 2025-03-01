/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.jdi;

import org.jspecify.annotations.Nullable;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public class BooleanValueImpl extends PrimitiveValueImpl
                              implements BooleanValue
{
    private boolean value;

    BooleanValueImpl(VirtualMachine aVm, boolean aValue) {
        super(aVm);
        value = aValue;
    }

    
    
    public boolean equals(@Nullable Object obj) {
        if ((obj != null) && (obj instanceof BooleanValue)) {
            return (value == ((BooleanValue)obj).value()) &&
                   super.equals(obj);
        } else {
            return false;
        }
    }

    public int hashCode() {
        /*
         * TO DO: Better hash code
         */
        return intValue();
    }

    public Type type() {
        return vm.theBooleanType();
    }

    public boolean value() {
        return value;
    }

    public boolean booleanValue() {
        return value;
    }

    public byte byteValue() {
        return (byte)(value ? 1 : 0);
    }

    public char charValue() {
        return (char)(value ? 1 : 0);
    }

    public short shortValue() {
        return (short)(value ? 1 : 0);
    }

    public int intValue() {
        return (value ? 1 : 0);
    }

    public long longValue() {
        return (value ? 1 : 0);
    }

    public float floatValue() {
        return (float)(value ? 1.0 : 0.0);
    }

    public double doubleValue() {
        return (value ? 1.0 : 0.0);
    }

    public String toString() {
        return "" + value;
    }

    byte typeValueKey() {
        return JDWP.Tag.BOOLEAN;
    }
}
