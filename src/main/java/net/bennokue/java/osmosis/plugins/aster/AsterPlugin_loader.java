package net.bennokue.java.osmosis.plugins.aster;

import java.util.HashMap;
import java.util.Map;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

public class AsterPlugin_loader implements PluginLoader {

    @Override
    public Map<String, TaskManagerFactory> loadTaskFactories() {
        Map<String, TaskManagerFactory> factoryMap = new HashMap<>();
        AsterPlugin_factory asterplugin = new AsterPlugin_factory();

        // write-aster will be our task name
        factoryMap.put("write-aster", asterplugin);

        return factoryMap;
    }
}
