/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javadoc;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.io.File;


/**
 * This interface describes a source position: filename, line number,
 * and column number.
 *
 * @since 1.4
 * @author Neal M Gafter
 *
 * @deprecated
 *   The declarations in this package have been superseded by those
 *   in the package {@code jdk.javadoc.doclet}.
 *   For more information, see the <i>Migration Guide</i> in the documentation for that package.
 */
@NullMarked
@Deprecated(since="9", forRemoval=true)
@SuppressWarnings("removal")
public interface SourcePosition {
    /** The source file. Returns null if no file information is
     *  available.
     *
     *  @return the source file as a File.
     */
    
    File file();

    /** The line in the source file. The first line is numbered 1;
     *  0 means no line number information is available.
     *
     *  @return the line number in the source file as an integer.
     */
    
    int line();

    /** The column in the source file. The first column is
     *  numbered 1; 0 means no column information is available.
     *  Columns count characters in the input stream; a tab
     *  advances the column number to the next 8-column tab stop.
     *
     *  @return the column on the source line as an integer.
     */
    
    int column();

    /** Convert the source position to the form "Filename:line". */
    
    String toString();
}
