/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt.diff;

import java.math.BigInteger;

/**
 *
 * @author filipekt
 */
class Vrchol implements Comparable<Vrchol>{
            
    static final Vrchol DOWN = new Vrchol(0, 1);
    static final Vrchol RIGHT = new Vrchol(1, 0);   
    static final Vrchol ZERO = new Vrchol(0, 0);

    private final int x;

    int getX() {
        return x;
    }          

    private final int y;

    int getY() {
        return y;
    }            

    private int getValue(){
        return x + y;
    }
    
    private int getReverseValue(int xBase, int yBase){
        return xBase - x + yBase - y;
    }

    Vrchol down(){
        return new Vrchol(x, y + 1);
    }
    
    Vrchol up(){
        return new Vrchol(x, y - 1);
    }

    Vrchol left(){
        return new Vrchol(x - 1, y);
    }
    
    Vrchol right(){
        return new Vrchol(x + 1, y);
    }
    
    Vrchol diagonalRight(){
        return new Vrchol(x + 1, y + 1);
    }
    
    Vrchol diagonalLeft(){
        return new Vrchol(x - 1, y - 1);
    }

    Vrchol moveDiagonally(byte[] A, byte[] B){
//        int ix = getX();
//        int iy = getY();
//        while ((ix < A.length) && (iy < B.length) && (A[ix] == B[iy])){
//            ix++;
//            iy++;
//        }
//        return new Vrchol(ix, iy);
        return moveDiagonally(A, 0, A.length, B, 0, B.length);
    }
    
    Vrchol moveDiagonally(byte[] A, int Afrom, int Ato, byte[] B, int Bfrom, int Bto){
        int ix = getX();
        int iy = getY();
        if ((ix < Afrom) || (iy < Bfrom)){
            return null;
        }
        while ((ix < Ato) && (iy < Bto) && (A[ix] == B[iy])){
            ix++;
            iy++;
        }
        return new Vrchol(ix, iy);
    }
    
    Vrchol moveDiagonallyReverse(byte[] A, int Afrom, int Ato, byte[] B, int Bfrom, int Bto){
        int ix = getX();
        int iy = getY();
        if ((ix > Ato) || (iy > Bto)){
            return null;
        }
        while ((ix > Afrom) && (iy > Bfrom) && (A[ix-1] == B[iy-1])){
            ix--;
            iy--;
        }
        return new Vrchol(ix, iy);
    }

    Vrchol minus(Vrchol v){
        return new Vrchol(getX() - v.getX(), getY() - v.getY());
    }

    @Override
    public int compareTo(Vrchol t) {
        return Integer.compare(getValue(), t.getValue());
    }
        
    public int compareToReverse(Vrchol t, int xBase, int yBase) {
        return Integer.compare(getReverseValue(xBase, yBase), t.getReverseValue(xBase, yBase));        
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Vrchol){
            Vrchol v = (Vrchol)o;
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

    Vrchol(int x, int y){
        this.x = x;
        this.y = y;                
    }           

    @Override
    public String toString() {
        return x + "," + y;
    }
    
    int getDiagonal(){
        return x-y;
    }
    
}
