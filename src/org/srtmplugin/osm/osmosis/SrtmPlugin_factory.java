package org.srtmplugin.osm.osmosis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class SrtmPlugin_factory extends TaskManagerFactory {

    private static final Logger log = Logger.getLogger(SrtmPlugin_factory.class.getName());
    
    //local directory for saving the downloaded *.hgt files
    private static final String ARG_LOCAL_DIR = "locDir";
    //default: tempdir
    private static final String DEFAULT_LOCAL_DIR = System.getProperty("java.io.tmpdir");
    //url for server basedir
    private static final String ARG_SERVER_BASE = "srvBase";
    //default: defined at srtmservers.properties
    private static String DEFAULT_SERVER_BASE = "";
    //subdirs for differenty countries, separated by semicolon
    private static final String ARG_SERVER_SUB_DIRS = "srvSubDirs";
    //default: defined at srtmservers.properties
    private static String DEFAULT_SERVER_SUB_DIRS = "";
    //local use of plugin (only available hgt files inside local directory)
    private static final String ARG_LOCAL_ONLY = "locOnly";
    //default: false
    private static final boolean DEFAULT_LOCAL_ONLY = false;
    //replace existing "height" tags of nodes
    private static final String ARG_REPLACE_EXISTING = "repExisting";
    //default: true
    private static final boolean DEFAULT_REPLACE_EXISTING = true;
    // tag name
    private static final String TAG_NAME = "tagName";
    private String tagName = "height";
    
    private boolean serverBaseSet = false;
    private boolean serverSubDirsSet = false;

    /**
     * checks a string for an trailing slash
     * @param s input string w/o trailing slash
     * @return string s with added trailing slash
     */
    private String checkForTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s;
        } else {
            return s += "/";
        }
    }

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        String localDir = getStringArgument(taskConfig, ARG_LOCAL_DIR, DEFAULT_LOCAL_DIR);
        boolean replaceExistingTags = getBooleanArgument(taskConfig, ARG_REPLACE_EXISTING, DEFAULT_REPLACE_EXISTING);
        tagName = getStringArgument(taskConfig, TAG_NAME, tagName);

        boolean tmpDir = false;
        if (localDir.equals(System.getProperty("java.io.tmpdir"))) {
            tmpDir = true;
        }

        File lDir = new File(localDir);

        SinkSource task = new SrtmPlugin_task(
                lDir,
                tmpDir,
                replaceExistingTags,
                tagName);
        return new SinkSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}
