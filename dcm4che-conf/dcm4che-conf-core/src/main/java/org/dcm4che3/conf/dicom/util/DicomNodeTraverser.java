package org.dcm4che3.conf.dicom.util;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.util.ConfigIterators;
import org.dcm4che3.conf.core.util.NodeTraverser;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.hl7.HL7Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DicomNodeTraverser extends NodeTraverser {

    public static final Logger log = LoggerFactory.getLogger(DicomNodeTraverser.class);

    private List<Class<?>> allExtensionClasses;

    public DicomNodeTraverser(List<Class<?>> allExtensionClasses) {
        this.allExtensionClasses = allExtensionClasses;
    }

    @Override
    public void traverseTree(Object node, Class nodeClass, NodeTraverser.EntryFilter filter) throws ConfigurationException {
        super.traverseTree(node, nodeClass, filter);

        // if because of any reason this is not a map (e.g. a reference or a custom adapter for a configurableclass),
        // we don't care about defaults
        if (!(node instanceof Map)) return;

        if (nodeClass.equals(Device.class)) {
            traverseExtensions(node, "deviceExtensions", filter);
        } else if (nodeClass.equals(ApplicationEntity.class)) {
            traverseExtensions(node, "aeExtensions", filter);
        } else if (nodeClass.equals(HL7Application.class)) {
            traverseExtensions(node, "hl7AppExtensions", filter);
        }

    }

    private void traverseExtensions(Object node, String whichExtensions, NodeTraverser.EntryFilter filter) throws ConfigurationException {

        Map<String, Object> extensions = null;
        try {
            extensions = ((Map<String, Map<String, Object>>) node).get(whichExtensions);
        } catch (ClassCastException e) {
            log.warn("Extensions are stored in a malformed format");
        }

        if (extensions == null) return;

        for (Map.Entry<String, Object> entry : extensions.entrySet()) {
            try {
                traverseTree(entry.getValue(), ConfigIterators.getExtensionClassBySimpleName(entry.getKey(), allExtensionClasses), filter);
            } catch (ClassNotFoundException e) {
                // noop
                log.warn("Extension class {} not found", entry.getKey());
            }
        }

    }
}
