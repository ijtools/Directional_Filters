package inra.ijpb.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.directional.DirectionalFilter.Operation;
import inra.ijpb.morphology.directional.OrientedLineStrelFactory;
import inra.ijpb.morphology.strel.CompositeStrel;
import inra.ijpb.morphology.strel.SquareStrel;

public class OrientationStackPlugin implements PlugIn
{

	public OrientationStackPlugin()
	{
	}


	@Override
	public void run(String arg)
	{
		ImagePlus imagePlus = IJ.getImage();
		ImageProcessor image = imagePlus.getProcessor();
		
		// Create the configuration dialog
		GenericDialog gd = new GenericDialog("Orientation Stack");
		
		gd.addChoice("Operation", Operation.getAllLabels(), Operation.OPENING.toString());
		gd.addNumericField("Line_Length", 20, 0, 6, "pixels");
        gd.addNumericField("Line_Thickness", 1, 0, 6, "pixels");
		gd.addNumericField("Direction Number", 60, 0);
		
        gd.showDialog();        
        if (gd.wasCanceled()) 
        {
			return;
        }
        
		// extract chosen parameters
        Operation operation = Operation.fromLabel(gd.getNextChoice());
		int lineLength 	= (int) gd.getNextNumber();
        int lineThickness = (int) gd.getNextNumber();
        int nDirections = (int) gd.getNextNumber();
		
		int sizeX = image.getWidth();
		int sizeY = image.getHeight();
		ImageStack result = ImageStack.create(sizeX, sizeY, nDirections, image.getBitDepth());
		
		OrientedLineStrelFactory strelFactory = new OrientedLineStrelFactory(lineLength);
		
		// Iterate over the set of directions
		for (int i = 0; i < nDirections; i++)
		{
			IJ.showProgress(i, nDirections);
			
			// Create the structuring element for current orientation
			double theta = ((double) i) * 180.0 / nDirections;
			Strel strel = strelFactory.createStrel(theta);
			if (lineThickness > 1)
			{
			    strel = new CompositeStrel(strel, SquareStrel.fromDiameter(lineThickness));
			}

			// Apply oriented filter
			ImageProcessor filtered = operation.apply(image, strel);
			
			result.setProcessor(filtered, i+1);
		}
		IJ.showProgress(1, 1);
		
		String name = imagePlus.getShortTitle() + "-orient";
		ImagePlus resultPlus = new ImagePlus(name, result);
		
		resultPlus.show();
	}
}
