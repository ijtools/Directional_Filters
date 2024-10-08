/**
 * 
 */
package inra.ijpb.morphology.strel;

import java.util.ArrayList;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.algo.AlgoEvent;
import inra.ijpb.algo.AlgoListener;
import inra.ijpb.data.image.ImageUtils;
import inra.ijpb.morphology.Strel;

/**
 * A structuring element obtained by the composition of several structuring elements.
 * 
 * The structuring element do not need to be separable.
 * 
 * @see inra.ijpb.morphology.strel.SeparableStrel
 * 
 * @author dlegland
 *
 */
public class CompositeStrel extends AbstractStrel implements AlgoListener 
{
    ArrayList<Strel> strels;
    

    public CompositeStrel(Strel... strels)
    {
        this.strels = new ArrayList<Strel>(strels.length);
        for (Strel strel : strels)
        {
            this.strels.add(strel);
        }
    }
    
    public CompositeStrel(ArrayList<Strel> strels)
    {
        this.strels = new ArrayList<Strel>(strels.size());
        this.strels.addAll(strels);
    }
    
    
    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#closing(ij.process.ImageProcessor)
     */
    @Override
    public ImageProcessor closing(ImageProcessor image)
    {
        return erosion(dilation(image));
    }

    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#opening(ij.process.ImageProcessor)
     */
    @Override
    public ImageProcessor opening(ImageProcessor image)
    {
        return dilation(erosion(image));
    }


    public ImageProcessor dilation(ImageProcessor image)
    {
        // Allocate memory for result
        ImageProcessor result = image.duplicate();
        
        // Extract structuring elements
        int n = strels.size();
        
        // Dilation
        int i = 1;
        for (Strel strel : strels)
        {
            fireStatusChanged(this, createStatusMessage("Dilation", i, n));
            result = runDilation(result, strel);
            i++;
        }
        
        // clear status bar
        fireStatusChanged(this, "");
        
        return result;
    }

    public ImageProcessor erosion(ImageProcessor image) 
    {
        // Allocate memory for result
        ImageProcessor result = image.duplicate();
        
        // Extract structuring elements
        int n = strels.size();
        
        // Erosion
        int i = 1;
        for (Strel strel : strels)
        {
            fireStatusChanged(this, createStatusMessage("Erosion", i, n));
            result = runErosion(result, strel);
            i++;
        }
        
        // clear status bar
        fireStatusChanged(this, "");
        
        return result;
    }
    
    private String createStatusMessage(String opName, int i, int n)
    {
        String channel = this.getChannelName();
        if (channel == null)
            return opName + " " + i + "/" + n;
        else
            return opName + " " + channel + " " + i + "/" + n;
    }

    private ImageProcessor runDilation(ImageProcessor image, Strel strel)
    {
        strel.showProgress(this.showProgress());
        strel.addAlgoListener(this);
        if (strel instanceof InPlaceStrel)
        {
            ((InPlaceStrel) strel).inPlaceDilation(image);
        }
        else
        {
            image = strel.dilation(image);
        }
        strel.removeAlgoListener(this);
        return image;
    }
    
    private ImageProcessor runErosion(ImageProcessor image, Strel strel) 
    {
        strel.showProgress(this.showProgress());
        strel.addAlgoListener(this);
        if (strel instanceof InPlaceStrel)
        {
            ((InPlaceStrel) strel).inPlaceErosion(image);
        }
        else
        {
            image = strel.erosion(image);
        }
        strel.removeAlgoListener(this);
        return image;
    }
    
    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#getSize()
     */
    @Override
    public int[] getSize()
    {
        int[] size = new int[2];
        for (Strel strel : strels)
        {
            int[] strelSize = strel.getSize();
            size[0] += strelSize[0] - 1;
            size[1] += strelSize[1] - 1;
        }
        return size;
    }

    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#getMask()
     */
    @Override
    public int[][] getMask()
    {
        int[] size = getSize();
        int[] offset = getOffset();
        ImageProcessor image = new ByteProcessor(size[0], size[1]);
        image.set(offset[0], offset[1], 255);
        image = dilation(image);
        
        int[][] mask = new int[size[1]][size[0]];
        for (int y = 0; y < size[1]; y++)
        {
            for (int x = 0; x < size[0]; x++)
            {
                mask[y][x] = image.get(x, y);
            }
        }
        return mask;
    }

    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#getOffset()
     */
    @Override
    public int[] getOffset()
    {
        int[] off = new int[2];
        for (Strel strel : strels)
        {
            int[] strelOffset = strel.getOffset();
            off[0] += strelOffset[0];
            off[1] += strelOffset[1];
        }
        return off;
    }

    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#getShifts()
     */
    @Override
    public int[][] getShifts()
    {
        int[] size = getSize();
        int[] offset = getOffset();
        ImageProcessor image = new ByteProcessor(size[0], size[1]);
        image.set(offset[0], offset[1], 255);
        image = dilation(image);
        
        ArrayList<int[]> shiftList = new ArrayList<int[]>();
        for (int y = 0; y < size[1]; y++)
        {
            for (int x = 0; x < size[0]; x++)
            {
                if(image.get(x, y) > 0)
                {
                    shiftList.add(new int[] {x - offset[0], y - offset[0]});
                }
            }
        }
        
        int nShifts = shiftList.size();
        int[][] shifts = new int[nShifts][];
        for (int i = 0; i < nShifts; i++)
        {
            shifts[i] = shiftList.get(i);
        }
        return shifts;
    }

    /* (non-Javadoc)
     * @see inra.ijpb.morphology.Strel#reverse()
     */
    @Override
    public Strel reverse()
    {
        ArrayList<Strel> revStrels = new ArrayList<Strel>(this.strels.size());
        for (Strel strel : this.strels)
        {
            revStrels.add(strel.reverse());
        }
        return new CompositeStrel(revStrels);
    }

    /**
     * Propagates the event by changing the source.
     */
    public void algoProgressChanged(AlgoEvent evt)
    {
        this.fireProgressChanged(this, evt.getCurrentProgress(), evt.getTotalProgress());
    }
    
    /**
     * Propagates the event by changing the source.
     */
    public void algoStatusChanged(AlgoEvent evt)
    {
        this.fireStatusChanged(this, evt.getStatus());
    }
    
    public static final void main(String[] args)
    {
        System.out.println("hello...");
        
        Strel line1 = new LinearHorizontalStrel(3);
        Strel line2 = new LinearVerticalStrel(3);
        Strel line3 = new LinearDiagDownStrel(3);
        Strel strel = new CompositeStrel(line1, line2, line3);
        
        ImageProcessor image = new ByteProcessor(10, 10);
        image.set(5, 5, 255);
        ImageProcessor image2 = strel.dilation(image);
        
        ImageUtils.print(image2);
    }
}
