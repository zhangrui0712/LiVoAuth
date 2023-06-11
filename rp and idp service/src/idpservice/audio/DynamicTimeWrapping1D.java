
package idpservice.audio;

public class DynamicTimeWrapping1D extends DynamicTimeWrapping{
    
    private double[] test;      // query feature vector  
    private double[] reference; // reference feature vector. 
      
    public DynamicTimeWrapping1D(double[] test, double[] reference)
    {
        this.test = test;
        this.reference = reference;
    }
    
    
    /**
     * Calculate the minimum distance between two vectors.  
     * following is the DP formula: 
     * 
     *  D(n,m) = MIN {
     *              D(n-1,m-1) + 2d(n,m), 
     *              D(n-1,m-2) + 3d(n,m),
     *              D(n-2,m-1) + 3d(n,m),
     *              D(n-1,m) + d(n,m),
     *              D(n,m-1) + d(n,m)
     *            }
     *          
     * @return  distance between two vectors. 
     */
    @Override
    public double calDistance() {
        int n = test.length;
        int m = reference.length;
        if (n < 1 || m < 1){
            System.out.println("One of the feature vectors has zero length!");
            return -1;
        }
        
        // DP for calculating the minimum distance between two vector. 
        // DTW[i,j] = minimum distance between vector test[0..i] and reference[0..j]
        double[][] DTW = new double[n][m];
        
        // initialization. 
        for(int i = 0; i < n; i++)
            for(int j = 0; j < m; j++)
                DTW[i][j] = Double.MAX_VALUE;
        
        // initialize start point
        DTW[0][0] =  getDistance(test[0],reference[0]);  
        
        // initialize boundary cases. 
        for (int i = 1; i < n; i++)
            DTW[i][0] = DTW[i-1][0] + getDistance(test[i],reference[0]);  
        
        for(int i = 1; i < m; i++)
            DTW[0][i] = DTW[0][i-1] + getDistance(test[0],reference[i]);
        
        // DP comes here...
        for (int i = 1; i < n; i++)
        {
            for (int j = Math.max(1, i- globalPathConstraint/2); j < Math.min(m, i+ globalPathConstraint/2); j++)
            {   
                double cost = getDistance(test[i],reference[j]);
                // d1,d2,d3,d4,d5 denotes the five different moves. 
                double d1 = cost + DTW[i-1][j];
                double d2 = cost + DTW[i][j-1];
                double d3 = 2 * cost + DTW[i-1][j-1];
                double d4 = Double.MAX_VALUE;
                if (j >1)
                    d4 = 3 * cost + DTW[i-1][j-2];
                double d5 = Double.MAX_VALUE;
                if (i > 2)
                    d5 = 3 * cost + DTW[i-2][j-1];
                
                DTW[i][j] = getMin(d1,d2,d3,d4,d5); 
            }
        }
        
        return DTW[n-1][m-1];  
    }   
    
    // Euclidean distance between two points.  
    private double getDistance(double d1, double d2)
    {
        return Math.sqrt((d1-d2)* (d1-d2));
    }
    
}   
