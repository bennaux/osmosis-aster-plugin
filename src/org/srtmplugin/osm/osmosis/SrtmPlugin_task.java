package org.srtmplugin.osm.osmosis;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

/**
 * main class which implements all necessary methods for
 * srtm downloading, parsing and adding
 * 
 * @author Dominik Paluch
 * @modified Robert Greil
 * @modified Benno Kühnl
 * TODO Benno javadoc
 */
public class SrtmPlugin_task implements SinkSource, EntityProcessor {

    private static final Logger log = Logger.getLogger(SrtmPlugin_task.class.getName());
    private String tagName = "height";
    private Sink sink;
    private File asterDir = new File("./");
    private boolean replaceExistingTags = true;
    private Map<File, SoftReference<BufferedInputStream>> srtmMap = new HashMap<>(); // TODO Benno Wech
    private Map<String, SoftReference<GridCoverage2D>> asterMap = new HashMap<>();
    private Map<String, AsterTile> missingAsterTiles = new HashMap<>();
    
    private Interpolation interpolation;

    /**
     * Constructor <br>
     * 
     * @param asterDir local directory for downloading of srtm files
     * @param replaceExistingTags replace existing height tags? true/false
     * 
     */
    public SrtmPlugin_task(final File asterDir, final boolean replaceExistingTags, String tagName) {
        if (!asterDir.exists() || !asterDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory " + asterDir.getAbsolutePath());
        }
        this.asterDir = asterDir;
        this.replaceExistingTags = replaceExistingTags;
        this.tagName = tagName;
        
        this.interpolation = new InterpolationBilinear();
    }

    @Override
    public void process(EntityContainer entityContainer) {
        entityContainer.process(this);
    }

    @Override
    public void process(BoundContainer boundContainer) {
        sink.process(boundContainer);
    }

    @Override
    public void process(NodeContainer container) {
        //backup existing node entity
        Node node = container.getEntity();
        //backup lat and lon of node entity
        double lat = node.getLatitude();
        double lon = node.getLongitude();
        //try to get aster height
        Double asterHeight = new Double(asterHeight(lat, lon));

        //look for existing height tag
        Collection<Tag> tags = node.getTags();
        Tag pbf_tag = null;
        for (Tag tag : tags) {
            if (tag.getKey().equalsIgnoreCase(tagName)) {
                pbf_tag = tag;
                break;
            }
        }

        //work with possible existing height tag
        //check if it should be replaced or not
        boolean addHeight = true;
        if (pbf_tag != null) {
            if (asterHeight.isNaN()) {
                addHeight = false;
            } else {
                if (replaceExistingTags) {
                    tags.remove(pbf_tag);
                } else {
                    addHeight = false;
                }
            }
        }

        //add new aster height tag
        if (addHeight) {
            tags.add(new Tag(tagName, asterHeight.toString()));
        }

        //create new node entity with new srtm height tag
        CommonEntityData ced = new CommonEntityData(
                node.getId(),
                node.getVersion(),
                node.getTimestamp(),
                node.getUser(),
                node.getChangesetId(),
                tags);

        //distribute the new nodecontainer to the following sink
        sink.process(new NodeContainer(new Node(ced, lat, lon)));
    }

    @Override
    public void process(WayContainer container) {
        sink.process(container);
    }

    @Override
    public void process(RelationContainer container) {
        sink.process(container);
    }

    @Override
    public void complete() {
        sink.complete();
    }

    @Override
    public void release() {
        sink.release();
    }

    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }
    
    @Override
    public void initialize(Map<String, Object> metaData) {
    	// added in osmosis 0.41
    }
    
    private double asterHeight(double lat, double lon) {
        String filename = generateFileName(lat, lon);
        if (this.missingAsterTiles.containsKey(filename)) {
            log.fine("ASTER tile " + filename + " already marked as missing. Returning NaN.");
            return Double.NaN;
        }
        
        GridCoverage2D coverage = null;
        /*
         * Try to fetch a SoftReference to our coverage from this.asterMap and try to get the Coverage from the SoftReference.
         */
        SoftReference<GridCoverage2D> coverageReference = this.asterMap.get(filename);
        if (coverageReference != null) {
            coverage = coverageReference.get();
        }
        if (coverage == null) {
            File asterFile = new File(this.asterDir, filename);
            try {
                System.out.println("Trying to load ASTER file " + filename); // TODO Benno wech
                GeoTiffReader geotiffreader = new GeoTiffReader(asterFile);
                coverage = (GridCoverage2D) geotiffreader.read(null);
            }
            catch (IOException | IllegalArgumentException e) {
                // File not found!
                this.addMissingTile((int)Math.floor(lat), (int)Math.floor(lon), filename);
                this.log.fine("Added tile " + filename + " to missing tiles.");
                this.log.severe("Missing file: " + filename);
                System.out.println("Missing file: " + filename); // TODO Benno wech
                return Double.NaN;
            }
            this.asterMap.put(filename, new SoftReference<>(coverage));
        }
        return this.getInterpolatedElevation(coverage, lon, lat);
    }
    
    private void addMissingTile(int lat, int lon, String filename) {
        if (!this.missingAsterTiles.containsKey(filename)) {
            this.missingAsterTiles.put(filename, new AsterTile(lat, lon));
        }
    }
    
    private void addMissingTile(int lat, int lon) {
        this.addMissingTile(lat, lon, generateFileName(lat, lon));
    }
    
    private String generateFileName(double lat, double lon) {
        /*
         * Determine filename
         * The filename consists of ASTGTM2_N<y>E<x>.tif with x being the longitude in three digits and y being the latitude in two digits of the center of the lower left pixel. Example: ASTGTM2_N47E010.tif covers 47°--48° N / 10°--11° E.
         */
        int lowerLatitude = Math.abs((int) Math.floor(lat));
        int lowerLongitude = Math.abs((int) Math.floor(lon));
        String filename = "";
        String filename_northSouth, filename_westEast, filename_lowerLatitude, filename_lowerLongitude;
        
        if (lat > 0) {
            filename_northSouth = "N";
        } else {
            filename_northSouth = "S";
        }
        if (lon > 0) {
            filename_westEast = "E";
        } else {
            filename_westEast = "W";
        }
        
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMinimumIntegerDigits(2);
        filename_lowerLatitude = numberFormat.format(lowerLatitude);
        numberFormat.setMinimumIntegerDigits(3);
        filename_lowerLongitude = numberFormat.format(lowerLongitude);
        
        filename = "ASTGTM2_" + filename_northSouth + filename_lowerLatitude + filename_westEast + filename_lowerLongitude + ".tif";
        this.log.log(Level.FINER, "Generated filename: " + filename);
        return filename;
    }
    
    private double getInterpolatedElevation(GridCoverage2D coverage, double x, double y) {
        DirectPosition location = new DirectPosition2D(x, y);
        try {
            return this.getInterpolatedElevation(coverage, location);
        } catch (InvalidGridGeometryException | TransformException ex) {
            Logger.getLogger(SrtmPlugin_task.class.getName()).log(Level.SEVERE, null, ex);
            return Double.NaN;
        }
    }

    private double getInterpolatedElevation(GridCoverage2D coverage, DirectPosition location) throws InvalidGridGeometryException, TransformException {
        GridGeometry2D calculator = coverage.getGridGeometry();

        // Determine nearest grid cell
        GridCoordinates2D nearestGridCell = calculator.worldToGrid(location);
        DirectPosition nearestLocation = calculator.gridToWorld(nearestGridCell);
        /*
         * Get the "interpolation partners". Interpolating needs four sample
         * values and two values depicting the "inbetweenness" of the sample
         * asked for. The four values have their "footpoint" in the left upper
         * edge, so they are called ul, ur, dl, dr (up/down, left/right). At
         * first we get the grid coordinates.
         */
        float ul, ur, dl, dr, xfrac, yfrac;
        {
            GridCoordinates2D ulGCoord, urGCoord, dlGCoord, drGCoord;
            double[] locationArray = location.getCoordinate();
            double[] nearestLocationArray = nearestLocation.getCoordinate();
            // Sample lies southeast of the nearest grid cell
            if (locationArray[1] < nearestLocationArray[1] && locationArray[0] >= nearestLocationArray[0]) {
                ulGCoord = nearestGridCell;
                urGCoord = new GridCoordinates2D(nearestGridCell.x + 1, nearestGridCell.y);
                dlGCoord = new GridCoordinates2D(nearestGridCell.x, nearestGridCell.y + 1);
                drGCoord = new GridCoordinates2D(nearestGridCell.x + 1, nearestGridCell.y + 1);
            } // Sample lies southwest of the nearest grid cell
            else if (locationArray[1] < nearestLocationArray[1] && locationArray[0] < nearestLocationArray[0]) {
                ulGCoord = new GridCoordinates2D(nearestGridCell.x - 1, nearestGridCell.y);
                urGCoord = nearestGridCell;
                dlGCoord = new GridCoordinates2D(nearestGridCell.x - 1, nearestGridCell.y + 1);
                drGCoord = new GridCoordinates2D(nearestGridCell.x, nearestGridCell.y + 1);
            } // Sample lies northeast of the nearest grid cell
            else if (locationArray[1] >= nearestLocationArray[1] && locationArray[0] >= nearestLocationArray[0]) {
                ulGCoord = new GridCoordinates2D(nearestGridCell.x, nearestGridCell.y - 1);
                urGCoord = new GridCoordinates2D(nearestGridCell.x + 1, nearestGridCell.y - 1);
                dlGCoord = nearestGridCell;
                drGCoord = new GridCoordinates2D(nearestGridCell.x + 1, nearestGridCell.y);
            } // Sample lies northwest of the nearest grid cell
            // this "else" means: else if (locationArray[1] >= nearestLocationArray[1] && locationArray[0] < nearestLocationArray[0]) {
            else {
                ulGCoord = new GridCoordinates2D(nearestGridCell.x - 1, nearestGridCell.y - 1);
                urGCoord = new GridCoordinates2D(nearestGridCell.x, nearestGridCell.y - 1);
                dlGCoord = new GridCoordinates2D(nearestGridCell.x - 1, nearestGridCell.y);
                drGCoord = nearestGridCell;
            }

            /*
             * We have the grid coordinates. To get the xfrac and yfrac
             * (relative "inbetweenness", see Javadoc of Interpolation) for
             * interpolating, we also need the world coordinates.
             */
            DirectPosition ulWCoord = calculator.gridToWorld(ulGCoord);
            DirectPosition dlWCoord = calculator.gridToWorld(dlGCoord);
            DirectPosition urWCoord = calculator.gridToWorld(urGCoord);
            // DirectPosition drWCoord = calculator.gridToWorld(drGCoord); // Just for debugging
            xfrac = (float) ((locationArray[0] - ulWCoord.getCoordinate()[0]) / (urWCoord.getCoordinate()[0] - ulWCoord.getCoordinate()[0]));
            yfrac = (float) ((ulWCoord.getCoordinate()[1] - locationArray[1]) / (ulWCoord.getCoordinate()[1] - dlWCoord.getCoordinate()[1]));

            /*
             * We have the cells, we have xfrac and yfrac. Now we only need the
             * sample values.
             */
            ul = coverage.evaluate(ulGCoord, (int[]) null)[0];
            ur = coverage.evaluate(urGCoord, (int[]) null)[0];
            dl = coverage.evaluate(dlGCoord, (int[]) null)[0];
            dr = coverage.evaluate(drGCoord, (int[]) null)[0];
            
            /*
             * Stooooop! There are "special DN values": -9999 for void pixels, and 0 for sea water body (cited from ASTER Global DEM (ASTER GDEM) Quick Guide for V2). 0 is no problem, but we have to prevent -9999 being taken into account: Then we return NaN.
             */
            if (ul == -9999 || ur == -9999 || dl == -9999 || dr == -9999) {
                this.log.log(Level.INFO, "Void pixel found while looking for ({0}, {1}), returning NaN", new Object[]{locationArray[0], locationArray[1]});
                return Double.NaN;
            }

            /*
             * And now we forget everything but ul, ur, dl, dr, xfrac, yfrac.
             */
        }
        // Interpolate
        return this.interpolation.interpolate(ul, ur, dl, dr, xfrac, yfrac);
    }
   
    private class AsterTile {
        public int north;
        public int east;
        
        public AsterTile(int north, int east) {
            this.north = north;
            this.east = east;
        }
    }
}
