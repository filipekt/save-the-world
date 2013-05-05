package cz.filipekt.diff;

/**
 * A vertex in the edit graph.
 * @author Tomas Filipek
 */
class Point {
            
    /**
     * Represents more a direction than an actual point.
     */
    static final Point DOWN = new Point(0, 1);
    
    /**
     * Represents more a direction than an actual point.
     */
    static final Point RIGHT = new Point(1, 0); 
    
    /**
     * Represents more a direction than an actual point.
     */
    static final Point ZERO = new Point(0, 0);

    /**
     * X-coordinate of this point.
     */
    private final int x;

    int getX() {        
        return x;
    }          

    /**
     * Y-coordinate of this point.
     */
    private final int y;

    int getY() {
        return y;
    }                          

    /**
     * An adjacent point in the downward direction.
     * @return 
     */
    Point down(){
        return new Point(x, y + 1);
    }
    
    /**
     * An adjacent point in the upward direction.
     * @return 
     */
    Point up(){
        return new Point(x, y - 1);
    }

    /**
     * An adjacent point to the left.
     * @return 
     */
    Point left(){
        return new Point(x - 1, y);
    }
    
    /**
     * An adjacent point to the right.
     * @return 
     */
    Point right(){
        return new Point(x + 1, y);
    }    
    
    /**
     * Finds the point at the end of the diagonal that begins here.
     * @param A
     * @param Afrom
     * @param Ato
     * @param B
     * @param Bfrom
     * @param Bto
     * @return 
     */
    Point moveDiagonally(byte[] A, int Afrom, int Ato, byte[] B, int Bfrom, int Bto){
        int ix = x;
        int iy = y;
        if ((ix < Afrom) || (iy < Bfrom)){
            return null;
        }
        while ((ix < Ato) && (iy < Bto) && (A[ix] == B[iy])){
            ix++;
            iy++;
        }
        return new Point(ix, iy);
    }
    
    /**
     * Finds the point at the beginning of the diagonal that ends here.
     * @param A
     * @param Afrom
     * @param Ato
     * @param B
     * @param Bfrom
     * @param Bto
     * @return 
     */
    Point moveDiagonallyReverse(byte[] A, int Afrom, int Ato, byte[] B, int Bfrom, int Bto){
        int ix = x;
        int iy = y;
        if ((ix > Ato) || (iy > Bto)){
            return null;
        }
        while ((ix > Afrom) && (iy > Bfrom) && (A[ix-1] == B[iy-1])){
            ix--;
            iy--;
        }
        return new Point(ix, iy);
    }

    /**
     * A simple subtraction, component-wise
     * @param v
     * @return 
     */
    Point minus(Point v){
        return new Point(getX() - v.getX(), getY() - v.getY());
    }
    
    /**
     * Returns the number of the diagonal this point is on.
     * @return 
     */
    int getDiagonal(){
        return x-y;
    }    

    @Override
    public boolean equals(Object o){
        if (o instanceof Point){
            Point v = (Point)o;
            return (getX() == v.getX()) && (getY() == v.getY());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.x;
        hash = 89 * hash + this.y;
        return hash;
    }

    Point(int x, int y){
        this.x = x;
        this.y = y;                
    }                
}
