package org.dcm4che3.conf.core.adapters;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.AnnotatedConfigurableProperty;
import org.dcm4che3.conf.core.BeanVitalizer;
import org.dcm4che3.util.Base64;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman K
 */
public class X509CertificateAdapter implements ConfigTypeAdapter<X509Certificate, String> {

    private CertificateFactory certificateFactory;

    @Override
    public X509Certificate fromConfigNode(String configNode, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        try {
            final byte[] base64 = Base64.fromBase64(configNode);
            return (X509Certificate) getX509Factory().generateCertificate(new ByteArrayInputStream(base64));
        } catch (CertificateException e) {
            throw new ConfigurationException("Cannot initialize X509 certificate converter", e);
        } catch (Exception e) {
            throw new ConfigurationException("Cannot read the X509 certificate", e);
        }
    }

    private CertificateFactory getX509Factory() throws CertificateException {
        if (certificateFactory == null) certificateFactory = CertificateFactory.getInstance("X509");
        return certificateFactory;
    }

    @Override
    public String toConfigNode(X509Certificate object, AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        try {
            return Base64.toBase64(object.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new ConfigurationException("Cannot encode X509 certificate", e);
        }
    }

    @Override
    public Map<String, Object> getSchema(AnnotatedConfigurableProperty property, BeanVitalizer vitalizer) throws ConfigurationException {
        Map<String, Object> metadata =  new HashMap<String, Object>();
        metadata.put("type", "string");
        metadata.put("metatype", "base64|x509");
        return metadata;
    }

    @Override
    public String normalize(Object configNode) throws ConfigurationException {
        return (String) configNode;
    }
}
