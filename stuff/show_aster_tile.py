# For viewing an ASTER tile. Clicks on it print the hit 
# geo-position and elevation at the python CLI.
#
# For using it, you need GDAL, e.g. from OsGeo4W, matplotlib and tk
# In Linux, install them eg with 
#    apt-get install python-gdal python-tk python-matplotlib
# 
# Inspired by 
#  http://stackoverflow.com/questions/24956653/read-elevation-using-gdal-python-from-geotiff
#  http://stackoverflow.com/questions/15721094/detecting-mouse-event-in-an-image-with-matplotlib
from osgeo import gdal
gdal.UseExceptions()
# Adjust this line to get the desired file
ds = gdal.Open('<path_to_file>')
band = ds.GetRasterBand(1)
elevation = band.ReadAsArray()
import matplotlib.pyplot as plt
nrows, ncols = elevation.shape
x0, dx, dxdy, y0, dydx, dy = ds.GetGeoTransform()
x1 = x0 + dx * ncols
y1 = y0 + dy * nrows
ax = plt.gca()
fig = plt.gcf()
implot = ax.imshow(elevation, cmap='gist_earth', extent=[x0, x1, y1, y0])
def onclick(event):
    if event.xdata != None and event.ydata != None:
        clickX = round((event.xdata-x0)/dx)
        clickY = round((event.ydata-y0)/dy)
        clickElevation = elevation[clickY][clickX]
        print("Position: " +  str(event.xdata) + "," + str(event.ydata))
        print("Elevation: " + str(clickElevation))
cid = fig.canvas.mpl_connect('button_press_event', onclick)
plt.show()
