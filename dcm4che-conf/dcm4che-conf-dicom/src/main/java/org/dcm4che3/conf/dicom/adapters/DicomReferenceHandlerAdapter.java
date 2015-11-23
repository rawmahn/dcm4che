/*
 * **** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 *  Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 *  The Initial Developer of the Original Code is
 *  Agfa Healthcare.
 *  Portions created by the Initial Developer are Copyright (C) 2014
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *  See @authors listed below
 *
 *  Alternatively, the contents of this file may be used under the terms of
 *  either the GNU General Public License Version 2 or later (the "GPL"), or
 *  the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 *  in which case the provisions of the GPL or the LGPL are applicable instead
 *  of those above. If you wish to allow use of your version of this file only
 *  under the terms of either the GPL or the LGPL, and not to allow others to
 *  use your version of this file under the terms of the MPL, indicate your
 *  decision by deleting the provisions above and replace them with the notice
 *  and other provisions required by the GPL or the LGPL. If you do not delete
 *  the provisions above, a recipient may use your version of this file under
 *  the terms of any one of the MPL, the GPL or the LGPL.
 *
 *  ***** END LICENSE BLOCK *****
 */
package org.dcm4che3.conf.dicom.adapters;

import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.api.internal.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.api.internal.BeanVitalizer;
import org.dcm4che3.conf.core.api.Configuration;
import org.dcm4che3.conf.core.adapters.DefaultReferenceAdapter;
import org.dcm4che3.conf.core.api.internal.ConfigurationManager;
import org.dcm4che3.conf.core.util.PathPattern;
import org.dcm4che3.conf.dicom.DicomPath;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;

import java.util.Iterator;


public class DicomReferenceHandlerAdapter<T> extends DefaultReferenceAdapter {
    public DicomReferenceHandlerAdapter(BeanVitalizer vitalizer, Configuration config) {
        super(vitalizer, config);
    }


    @Override
    protected Object getReferencedConfigurableObject(String uuid, BeanVitalizer vitalizer, AnnotatedConfigurableProperty property) {

        Configuration configuration = vitalizer.getContext(ConfigurationManager.class).getConfigurationStorage();
        Iterator deviceNameIterator = configuration.search(DicomPath.DeviceUUIDByAnyUUID.set("UUID", uuid).path());

        String deviceUUID;
        try {
            deviceUUID = (String) deviceNameIterator.next();
        } catch (Exception e) {
            throw new ConfigurationException("Cannot find a device that contains an object with UUID " + uuid, e);
        }


        // is this device already there?
        Device device = vitalizer.getInstanceFromThreadLocalPoolByUUID(deviceUUID, Device.class);

        if (device == null)
        deviceUUID



        return super.getReferencedConfigurableObject(uuid, vitalizer, property);
    }

    @Override
    public T fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer, Object parent) throws ConfigurationException {

        // Connection of a device. Get the device (it will grab the current one from threadLocal), and get the connection from there
        if (Connection.class.isAssignableFrom(property.getRawClass())) {

            try {
                String deviceName = null;
                try {
                    PathPattern.PathParser parser = DicomPath.ConnectionByCnRef.parse(configNode);
                    deviceName = parser.getParam("deviceName");
                } catch (IllegalArgumentException e) {
                    //noop
                }

                try {
                    PathPattern.PathParser parser = DicomPath.ConnectionByHostPortRef.parse(configNode);
                    deviceName = parser.getParam("deviceName");
                } catch (IllegalArgumentException e) {
                    //noop
                }

                try {
                    PathPattern.PathParser parser = DicomPath.ConnectionByHostRef.parse(configNode);
                    deviceName = parser.getParam("deviceName");
                } catch (IllegalArgumentException e) {
                    //noop
                }

                if (deviceName == null) throw new IllegalArgumentException();


                Device device = vitalizer.getContext(DicomConfiguration.class).findDevice(deviceName);
                Connection conn = (Connection) super.fromConfigNode(configNode, property, vitalizer, parent);

                return (T) device.connectionWithEqualsRDN(conn);

            } catch (Exception e) {
                throw new ConfigurationException("Cannot load referenced connection (" + configNode + ")", e);
            }
        } else if (Device.class.isAssignableFrom(property.getRawClass())) {
            try {

                PathPattern.PathParser parser = DicomPath.DeviceByNameRef.parse(configNode);
                String deviceName = parser.getParam("deviceName");

                return (T) vitalizer.getContext(DicomConfiguration.class).findDevice(deviceName);
            } catch (Exception e) {
                throw new ConfigurationException("Cannot load referenced device (" + configNode + ")", e);
            }
        } else if (ApplicationEntity.class.isAssignableFrom(property.getRawClass())) {
            try {

                PathPattern.PathParser parser = DicomPath.AEByTitleRef.parse(configNode);
                String aeName = parser.getParam("aeName");

                return (T) vitalizer.getContext(DicomConfiguration.class).findApplicationEntity(aeName);
            } catch (Exception e) {
                throw new ConfigurationException("Cannot load referenced AE (" + configNode + ")", e);
            }


        }

        return super.fromConfigNode(configNode, property, vitalizer, parent);
    }

}
