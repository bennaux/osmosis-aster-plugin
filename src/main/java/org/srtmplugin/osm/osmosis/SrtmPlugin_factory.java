package org.srtmplugin.osm.osmosis;

import java.io.File;
import java.util.logging.Level;
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
    
    // Log level of the task logger. Defaults to OFF.
    private static final String ARG_LOG_LEVEL = "logLevel";
    private static final String DEFAULT_LOG_LEVEL = "";

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        // Read arguments: asterDir, repExisting, tagName, logLevel
        String asterDir = getStringArgument(taskConfig, ARG_ASTER_DIR, DEFAULT_ASTER_DIR);
        boolean replaceExistingTags = getBooleanArgument(taskConfig, ARG_REPLACE_EXISTING, DEFAULT_REPLACE_EXISTING);
        tagName = getStringArgument(taskConfig, TAG_NAME, tagName);
        String logLevelArg = getStringArgument(taskConfig, ARG_LOG_LEVEL, DEFAULT_LOG_LEVEL);
        Level logLevel = getLogLevel(logLevelArg);

        File asterDirFile = new File(asterDir);

        // Get the machinery working
        SinkSource task = new SrtmPlugin_task(
                asterDirFile, 
                replaceExistingTags,
                tagName, 
                logLevel);
        return new SinkSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
    
    /**
     * Parse the given log level string and make a Level out of it.
     * @param argument The given argument for logLevel.
     * @return A {@link Level}. It defaults to {@code null}, which means 
     * that it will be inherited from OSMOSIS' log level.
     */
    private static Level getLogLevel(String argument) {
        switch (argument.toUpperCase()) {
            case "ALL":
                return Level.ALL;
            case "FINEST":
                return Level.FINEST;
            case "FINER":
                return Level.FINER;
            case "FINE": 
                return Level.FINE;
            case "CONFIG":
                return Level.CONFIG;
            case "INFO":
                return Level.INFO;
            case "WARNING":
                return Level.WARNING;
            case "SEVERE":
                return Level.SEVERE;
            case "OFF":
                return Level.OFF;
            case "":
                return null;
            default:
                System.out.print("Cannot understand the logLevel ");
                System.out.print(argument);
                System.out.println(". Using null.");
                return null;
        }
    }
}
