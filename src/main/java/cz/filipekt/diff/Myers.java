/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *  
 * @author Tomas Filipek
 */
class Myers {                                                        
    private final byte[] A;
    private final byte[] B;
    private final Vrchol finish;
    private final int sizeLimit;

    Myers(byte[] A, byte[] B, int sizeLimit) {
        this.A = A;
        this.B = B;
        this.sizeLimit = sizeLimit;
        finish = new Vrchol(A.length, B.length);
    }
    
    Myers(byte[] A, byte[] B){
        this(A,B,0);
    }
        
    Pair<Snake,Integer> findMiddleSnake(int Afrom, int Ato, int Bfrom, int Bto){
        if ((Ato < Afrom) || (Bto < Bfrom)){
            return null;
        }
        int n = Ato - Afrom;
        int m = Bto - Bfrom;
        int delta = n - m;        
        int vectorSize = (n + m + 2)/2;
        
        Vrchol leftUpper = new Vrchol(Afrom, Bfrom);
        Vrchol rightLower = new Vrchol(Ato, Bto);

        Vector Lvector = new Vector(new Snake[vectorSize*2], leftUpper.getDiagonal()-vectorSize, leftUpper.getDiagonal() + vectorSize);
        Vector Rvector = new Vector(new Snake[vectorSize*2], rightLower.getDiagonal()-vectorSize, rightLower.getDiagonal() + vectorSize);
                        
        for (int d = 0; d <= (n + m + 1) / 2; d++){
            if ((sizeLimit != 0) && (2*d > sizeLimit)){
                return null;
            }            
            if (d == 0){
                //forward direction
                Vrchol previous = leftUpper;
                Vrchol mid = previous;
                Vrchol end = mid.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);
                Snake s = new Snake(previous, mid, end);
                if (end.equals(new Vrchol(Ato, Bto))){
                    return new Pair<>(s,0);
                }
                Lvector.set(previous.getDiagonal(), s);
                
                //reverse direction
                previous = rightLower;
                mid = previous;
                end = mid.moveDiagonallyReverse(A, Afrom, Ato, B, Bfrom, Bto);
                s = new Snake(previous, mid, end);   
                Rvector.set(previous.getDiagonal(), s);                
            } else {
                for (int k = leftUpper.getDiagonal() - d; k <= leftUpper.getDiagonal() + d; k += 2){                    
                    Vrchol previous;
                    Vrchol mid;                    
                    boolean down = (k == leftUpper.getDiagonal() - d) || ((k != leftUpper.getDiagonal() + d) && (Lvector.get(k + 1).getEnd().compareTo(Lvector.get(k - 1).getEnd()) > 0));                                        
                    if (down){                        
                        previous = Lvector.get(k + 1).getEnd();
                        mid = previous.down();                                                
                    } else {
                        previous = Lvector.get(k - 1).getEnd();
                        mid = previous.right();
                    }                       
                    Vrchol end = mid.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);
                    if ((delta%2)!=0){
                        if ((k >= rightLower.getDiagonal() - d + 1) && (k <= rightLower.getDiagonal() + d - 1)){
                            Snake reverse = Rvector.get(k);
                            if ((reverse != null) && (reverse.getEnd().getX() <= end.getX())){
                                return new Pair<>(new Snake(previous, mid, end), (2*d) - 1);
                            }
                        }
                    }        
                    Lvector.set(k, new Snake(previous, mid, end));                                                                   
                }            
                
                for (int k = rightLower.getDiagonal() - d; k <= rightLower.getDiagonal() + d; k += 2){                
                    boolean up = (k == rightLower.getDiagonal() + d) || 
                            ((k != rightLower.getDiagonal() - d) && (Rvector.get(k + 1).getEnd().compareToReverse(Rvector.get(k - 1).getEnd(), n, m) > 0));
                    Vrchol previous;
                    Vrchol mid;
                    if (up){
                        previous = Rvector.get(k - 1).getEnd();
                        mid = previous.up();
                    } else {
                        previous = Rvector.get(k + 1).getEnd();
                        mid = previous.left();
                    }
                    Vrchol end = mid.moveDiagonallyReverse(A, Afrom, Ato, B, Bfrom, Bto);                
                    if ((delta%2) == 0){
                        if ((k >= leftUpper.getDiagonal() - d) && (k <= leftUpper.getDiagonal() + d)){                        
                            Snake forward = Lvector.get(k);
                            if ((forward != null) && (forward.getEnd().getX() >= end.getX())){
                                return new Pair<>(forward, 2*d);
                            }                        
                        }
                    }
                    Rvector.set(k, new Snake(previous, mid, end));
                }
            }
        }
        return null;
    }
    
    List<Snake> compare(int Afrom, int Ato, int Bfrom, int Bto){        
        final int n = Ato - Afrom;
        final int m = Bto - Bfrom;
        List<Snake> res = new ArrayList<>();
        if ((m==0) && (n>0)){
           for (int i = Afrom; i<Ato; i++){
               res.add(new Snake(new Vrchol(i, Bfrom), new Vrchol(i+1, Bfrom), new Vrchol(i+1, Bfrom)));
           }
           return res;
        }
        if ((n==0) && (m>0)){
           for (int i = Bfrom; i<Bto; i++){
               res.add(new Snake(new Vrchol(Afrom, i), new Vrchol(Afrom, i+1), new Vrchol(Afrom, i+1)));
           }
           return res;
        }
        if ((m==0) && (n==0)){
           return res;
        }
        Pair<Snake,Integer> middleSnake = findMiddleSnake(Afrom, Ato, Bfrom, Bto);
        if (middleSnake == null){
            return null;
        }
        int d = middleSnake.b;   
        Vrchol start = middleSnake.a.getStart();
        Vrchol end = middleSnake.a.getEnd();       
        if (d > 1){
            List<Snake> res1 = compare(Afrom, start.getX(), Bfrom, start.getY());
            List<Snake> res2 = compare(end.getX(), Ato, end.getY(), Bto);            
            res.addAll(res1);
            res.add(middleSnake.a);
            res.addAll(res2);
            return res;
        } else if (d == 1){
            Vrchol leftupper = new Vrchol(Afrom, Bfrom);
            Vrchol tail = leftupper.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);
            Snake s = new Snake(leftupper, leftupper, tail);
            res.add(s);
            res.add(middleSnake.a);
            return res;
       } else { // d == 0
            res.add(middleSnake.a);
            return res;
       }
    }

    List<Vector> goThrough(){
        Vector vector = new Vector(A.length + B.length);
        List<Vector> snapshots = new ArrayList<>();
        boolean found = false;
        
        for (int dist = 0; (dist <= (A.length+B.length)) && !found; dist++){
            if (dist == 0){
                Vrchol previous = Vrchol.ZERO;
                Vrchol mid = Vrchol.ZERO;
                Vrchol end = mid.moveDiagonally(A, B);
                if (end.equals(finish)){
                        found = true;
                }
                vector.set(0, new Snake(previous, mid, end));
            } else {            
                for (int k_line = -dist; k_line <= dist; k_line += 2){
                    boolean down = (k_line == -dist) || ((k_line != dist) && (vector.get(k_line + 1).getEnd().compareTo(vector.get(k_line - 1).getEnd()) > 0));                    
                    Vrchol previous;
                    Vrchol mid;
                    if (down){
                        previous = vector.get(k_line + 1).getEnd();
                        mid = previous.down();
                    } else {
                        previous = vector.get(k_line - 1).getEnd();
                        mid = previous.right();
                    }
                    Vrchol end = mid.moveDiagonally(A, B);
                    if (end.equals(finish)){
                        found = true;
                    }
                    vector.set(k_line, new Snake(previous, mid, end));
                }
            }
            snapshots.add(vector.clone());
        }
        return snapshots;
    }

    List<Snake> reconstructPath(List<Vector> snapshots){
        Vrchol previous = finish;
        List<Snake> res = new ArrayList<>();
        for (int pos = snapshots.size() - 1; pos >= 0; pos--){
            Snake item = null;
            for (Snake s : snapshots.get(pos).toArray()){                
                if ((s != null) && (s.getEnd().equals(previous))){
                    item = s;
                    break;
                }                
            }
            res.add(item);
            previous = item.getStart();
        }
        Collections.reverse(res);
        return res;
    }               
}
