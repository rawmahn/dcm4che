package org.dcm4che.conf.core;


import org.dcm4che3.data.Attributes;
import org.dcm4che3.net.Device;

import javax.inject.Inject;

/**
 * Created by player on 30-Sep-14.
 */
@DynamicallyDecoratable
public class FullTextSearchService {


    @Inject
    Device d;



    public Attributes search(String searchString) {
        String str = prepareString(searchString);
        Attributes attrs = performSearch(str);
        return processResults(attrs);
    }

    private Attributes processResults(Attributes attrs) {
        return attrs;
    }

    private Attributes performSearch(String str) {
        return null;
    }

    public String prepareString(String searchString) {
        return searchString;
    }

}
