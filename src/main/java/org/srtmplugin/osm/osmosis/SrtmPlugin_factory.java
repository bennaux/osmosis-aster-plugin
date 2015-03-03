package org.srtmplugin.osm.osmosis;

import java.io.File;
import java.util.logging.Logger;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class SrtmPlugin_factory extends TaskManagerFactory {
    private static final Logger log = Logger.getLogger(SrtmPlugin_factory.class.getName());
    // Option to replace existing height tags, defaults to TRUE
    private static final String ARG_REPLACE_EXISTING = "repExisting";
    private static final boolean DEFAULT_REPLACE_EXISTING = true;
    
    // The name of the tag for storing elevation. Default: ele
    private static final String TAG_NAME = "tagName";
    private String tagName = "ele";
    
    // Directory where the ASTER DEM tiffs reside, defaults to ./
    private static final String ARG_ASTER_DIR = "asterDir";
    private static final String DEFAULT_ASTER_DIR = "./";

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        // Read arguments: asterDir, repExisting, tagName
        String asterDir = getStringArgument(taskConfig, ARG_ASTER_DIR, DEFAULT_ASTER_DIR);
        boolean replaceExistingTags = getBooleanArgument(taskConfig, ARG_REPLACE_EXISTING, DEFAULT_REPLACE_EXISTING);
        tagName = getStringArgument(taskConfig, TAG_NAME, tagName);

        File asterDirFile = new File(asterDir);

        // Get the machinery working
        SinkSource task = new SrtmPlugin_task(
                asterDirFile, 
                replaceExistingTags,
                tagName);

        return new SinkSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}
