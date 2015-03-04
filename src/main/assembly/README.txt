OSMOSIS ASTER plugin -- README
Plugin Version: #PLUGIN_VERSION#

1) How to install this plugin
    - Unpack the jar and the #LIBS_FOLDER# folder into the plugins directory
      of your OSMOSIS installation.
    - Add the #LIBS_FOLDER# folder to the classpath of your OSMOSIS starter. 
      When using Windows, edit your bin/osmosis.bat:
        -cp "%PLEXUS_CP%"
            gets
        -cp "%PLEXUS_CP%";plugins/#LIBS_FOLDER#/*
      When using Linux, edit your bin/osmosis:
        MYAPP_CLASSPATH=$MYAPP_HOME/lib/default/plexus-classworlds-*.jar
            gets
        MYAPP_CLASSPATH=$MYAPP_HOME/lib/default/*:$MYAPP_HOME/bin/plugins/lib/*

2) How to use this plugin
    Note: It is not possible to download the ASTER DEM tiles automatically (at 
    least not without breaking the law). So, you need to have the DEM TIFF files 
    stored in a folder, called "ASTER folder" from now on.

    The cli call is built like this:
    osmosis <pre-tasks> --write-aster asterDir=... repExisting={true|false} tagName=... logLevel=... <post-tasks>

    asterDir:   Specify the folder where the ASTER tiff files reside on your system.
                Defaults to the local folder.

    repExisting:When set true, existing tags will be replaced, when set false, 
                they will be left untouched. 
                Defaults to true.

    tagName:    The tag name where the elevation will be stored.
                Defaults to "elevation".

3) Logging
    You can enable logging by adding the path to a logging.properties file of your
    choice to the osmosis(.bat), for example:
        -Djava.util.logging.config.file=my-logging.properties
    Put the command right into the EXEC command building line, eg. after 
        -Dclassworlds.conf="%MYAPP_HOME%\config\plexus.conf"
    Such a properties file could look like this:
        .handlers= java.util.logging.ConsoleHandler
        .level= ALL
        java.util.logging.ConsoleHandler.level = ALL
        net.bennokue.java.osmosis.plugins.aster.AsterPlugin_task.level = ALL
    Log levels can be: ALL, FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF.

4) Version history
    1.0 (2015-03-??) TODO Benno aktualisiere Datum
        Version 1.0 is based on "osmosis-srtm-plugin", Version 1.1.0 by Franz 
        Graf https://code.google.com/p/osmosis-srtm-plugin/
        The SRTM features where removed, ASTER features added, and the whole 
        project had been switched to Maven.

5) Issues
    None yet. =)

6) Contact
    kuehnl@cip.ifi.lmu.de