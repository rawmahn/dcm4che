package org.dcm4che3.conf.dicom;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Device;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by aprvf on 14.06.2016.
 */
public class TestGenerics {


    @Test
    public void testName() throws Exception {

        CommonDicomConfiguration.DicomConfigurationRootNode root = new CommonDicomConfiguration.DicomConfigurationRootNode();

        Q<CommonDicomConfiguration.DicomConfigurationRootNode> q = new Q<>();

        q
                .inValuesOf(CommonDicomConfiguration.DicomConfigurationRootNode::getDevices)
                .inElementsOf(Device::getApplicationEntities)
                .where((ae) -> ae
                                .prop(ApplicationEntity::getAETitle)
                                .eq("DCM4CHEE")
                )
                .forEach((ae) -> System.out.println(ae.getDescription()));


    }

    private boolean myEq(String hostname, String s) {
        return true;
    }

    private class Q<F> {

        <T> Q<T> in(Function<F, T> f) {
            return (Q<T>) this;
        }

        <C, T extends Collection<C>> Q<C> inElementsOf(Function<F, T> f) {
            return (Q<C>) this;
        }

        <K, V, T extends Map<K, V>> Q<V> inValuesOf(Function<F, T> f) {
            return (Q<V>) this;
        }

        Collection<F> where(Consumer<Q<F>> predicate) {
            return (Collection<F>) this;
        }

        <T> Q<T> prop(Function<F, T> f) {
            return (Q<T>) this;
        }

        void eq(Object o) {

        }


        <T> T getRootForQuery(Class<T> clazz) {
            return (T) new Object();
        }

        public boolean equals(String hostname, String s) {
            return false;
        }

        public boolean any(Device d) {

        }

        public <T> T any(Collection<T> list) {
            return list.stream().findFirst().get();
        }


        private class P<T> {


        }
    }
}
