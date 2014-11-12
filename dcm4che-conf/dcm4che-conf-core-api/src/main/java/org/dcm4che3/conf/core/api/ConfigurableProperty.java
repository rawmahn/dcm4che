/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2014
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4che3.conf.core.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Marks a field, or a setter parameter of a configuration class to be a persistable configuration property of the bean
 *
 * @author Roman K
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigurableProperty {

    public static final String NO_DEFAULT_VALUE = "N/A";

    /**
     * Specifies that the annotated field/property is a collection, elements of which are stored as references
     * to actual values (like "dicomConfigurationRoot/dicomDevicesRoot/device1")
     * @return
     */
    boolean collectionOfReferences() default false;

    public enum EnumRepresentation {
        ORDINAL,
        STRING
    }

    /**
     * Name of the node. If not specified, the field/parameter name is used.
     *
     * @return
     */
    String name() default "";

    /**
     * Default for primitives (int, string, boolean). If default is not specified, the property is considered required.
     * For specifying whether an Object-typed property is not allowed to be null, use @NotNull
     * @return
     */
    String defaultValue() default NO_DEFAULT_VALUE;

    /**
     * Label to show in configuration UIs. If empty empty string (default), the name will be used.
     * @return
     */
    String label() default "";

    /**
     * Description to show in configuration UIs.
     * @return
     */
    String description() default "";

    EnumRepresentation enumRepresentation() default EnumRepresentation.STRING;
}
