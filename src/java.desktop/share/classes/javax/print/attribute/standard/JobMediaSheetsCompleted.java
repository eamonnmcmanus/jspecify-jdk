/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute.standard;

import org.jspecify.annotations.Nullable;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * Class {@code JobMediaSheetsCompleted} is an integer valued printing attribute
 * class that specifies the number of media sheets which have completed marking
 * and stacking for the entire job so far, whether those sheets have been
 * processed on one side or on both.
 * <p>
 * The {@code JobMediaSheetsCompleted} attribute describes the progress of the
 * job. This attribute is intended to be a counter. That is, the
 * {@code JobMediaSheetsCompleted} value for a job that has not started
 * processing must be 0. When the job's {@link JobState JobState} is
 * {@code PROCESSING} or {@code PROCESSING_STOPPED}, the
 * {@code JobMediaSheetsCompleted} value is intended to increase as the job is
 * processed; it indicates the amount of the job that has been processed at the
 * time the Print Job's attribute set is queried or at the time a print job
 * event is reported. When the job enters the {@code COMPLETED},
 * {@code CANCELED}, or {@code ABORTED} states, the
 * {@code JobMediaSheetsCompleted} value is the final value for the job.
 * <p>
 * <b>IPP Compatibility:</b> The integer value gives the IPP integer value. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 * @see JobMediaSheets
 * @see JobMediaSheetsSupported
 * @see JobKOctetsProcessed
 * @see JobImpressionsCompleted
 */
public final class JobMediaSheetsCompleted extends IntegerSyntax
        implements PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    private static final long serialVersionUID = 1739595973810840475L;

    /**
     * Construct a new job media sheets completed attribute with the given
     * integer value.
     *
     * @param  value Integer value
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public JobMediaSheetsCompleted(int value) {
        super (value, 0, Integer.MAX_VALUE);
    }

    /**
     * Returns whether this job media sheets completed attribute is equivalent
     * to the passed in object. To be equivalent, all of the following
     * conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class
     *   {@code JobMediaSheetsCompleted}.
     *   <li>This job media sheets completed attribute's value and
     *   {@code object}'s value are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this job media
     *         sheets completed attribute, {@code false} otherwise
     */
    
    
    public boolean equals(@Nullable Object object) {
        return (super.equals (object) &&
                object instanceof JobMediaSheetsCompleted);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobMediaSheetsCompleted}, the category is class
     * {@code JobMediaSheetsCompleted} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobMediaSheetsCompleted.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobMediaSheetsCompleted}, the category name is
     * {@code "job-media-sheets-completed"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-media-sheets-completed";
    }
}
