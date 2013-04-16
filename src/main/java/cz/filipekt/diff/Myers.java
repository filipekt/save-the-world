package cz.filipekt.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Contains the implementation of the Myers' diff algorithm and 
 * a simple heuristic by Paul Eggert.
 * @author Tomas Filipek
 */
class Myers {     
    
    /**
     * The resulting script will be relative to this array. 
     */
    private final byte[] A;
    
    /**
     * The resulting script will contain all the differences of this array to array A.
     */
    private final byte[] B;
       
    /**
     * If the final script size is greater than this, null is returned instead.
     */
    private final int sizeLimit;       
    
    /**
     * Paths through the edit graph which are longer than this are considered expensive
     * and are avoided when using the heuristic.
     * When constructing longest reaching paths on each diagonal and the distance 
     * grows over this limit, search is stopped and a suboptimal middle snake is returned.
     */
    private final int tooExpensive = 1024;   
    
    /**
     * Intermediate result during adding up the script size.
     */
    private int size = 0;

    Myers(byte[] A, byte[] B, int sizeLimit) {
        this.A = A;
        this.B = B;
        this.sizeLimit = sizeLimit;        
    }   
        
    /**
     * Finds a middle snake in the edit graph.
     * @param Afrom X-coordinate of the edit graph initial point.
     * @param Ato X-coordinate of the edit graph end point.
     * @param Bfrom Y-coordinate of the edit graph initial point.
     * @param Bto Y-coordinate of the edit graph end point.
     * @param heuristic If true, heuristics may be used to save time, but a suboptimal snake may be returned.
     * @return A tuple containing a middle snake and a length of the path ([Afrom,Bfrom] --> [Ato,Bto]) the 
     * snake belongs to.
     */
    private Quadruple<Snake,Integer,Boolean,Boolean> findMiddleSnake(int Afrom, int Ato, int Bfrom, int Bto, boolean heuristic){
        if ((Ato < Afrom) || (Bto < Bfrom)){
            return null;
        }
        int n = Ato - Afrom;
        int m = Bto - Bfrom;
        int delta = n - m;        
        int vectorSize;                
        Point leftUpper = new Point(Afrom, Bfrom);
        Point rightLower = new Point(Ato, Bto);        
        if (heuristic){
            vectorSize = tooExpensive + 1;            
        } else {
            vectorSize = (n + m + 1)/2;
        }   
        int Lfrom = leftUpper.getDiagonal() - vectorSize;
        int Rfrom = rightLower.getDiagonal() - vectorSize;
                     
        int[] Left = new int[vectorSize * 2 + 1];
        Arrays.fill(Left, Integer.MIN_VALUE);                
        int[] Right = new int[vectorSize * 2 + 1];
        Arrays.fill(Right, Integer.MIN_VALUE);                
                        
        for (int d = 0; d <= (n + m + 1) / 2; d++){
            if ((sizeLimit > 0) && (2*d > sizeLimit)){
                return null;
            }            
            if (d == 0){
                //forward direction                
                Point previous = leftUpper;
                Point mid = previous;
                Point end = mid.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);                                     
                Snake s = new Snake(previous, mid, end);
                if (end.equals(new Point(Ato, Bto))){
                    return new Quadruple<>(s,0,Boolean.TRUE,Boolean.TRUE);
                }
                Left[previous.getDiagonal() - Lfrom] = end.getX();
                
                //reverse direction
                previous = rightLower;
                mid = previous;
                end = mid.moveDiagonallyReverse(A, Afrom, Ato, B, Bfrom, Bto);
                Right[previous.getDiagonal() - Rfrom] = end.getX();                
            } else {                
                for (int k = leftUpper.getDiagonal() - d; k <= leftUpper.getDiagonal() + d; k += 2){                    
                    Point previous;
                    Point mid;                    
                    boolean down = (k == leftUpper.getDiagonal() - d) || 
                            ((k != leftUpper.getDiagonal() + d) && (Left[k+1-Lfrom] > Left[k-1-Lfrom] + 1));                                      
                    if (down){                        
                        int prevX = Left[k+1-Lfrom];
                        previous = new Point(prevX, prevX-(k+1));
                        mid = previous.down();                                          
                    } else {
                        int prevX = Left[k-1-Lfrom];
                        previous = new Point(prevX, prevX-(k-1));
                        mid = previous.right();
                    }                       
                    Point end = mid.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);                    
                    if ((delta%2)!=0){
                        if ((k >= rightLower.getDiagonal() - d + 1) && (k <= rightLower.getDiagonal() + d - 1)){
                            int reverseX = Right[k - Rfrom];
                            Point reverse = new Point(reverseX, reverseX - k);
                            if ((reverseX != -1) && (reverse.getX() <= end.getX())){
                                return new Quadruple<>(new Snake(previous, mid, end), (2*d) - 1, Boolean.TRUE, Boolean.TRUE);
                            }
                        }
                    }   
                    Left[k-Lfrom] = end.getX();                                             
                }            
                
                for (int k = rightLower.getDiagonal() - d; k <= rightLower.getDiagonal() + d; k += 2){                
                    boolean up = (k == rightLower.getDiagonal() + d) || 
                            ((k != rightLower.getDiagonal() - d) && (Right[k+1-Rfrom] < Right[k-1-Rfrom] + 1));
                    Point previous;
                    Point mid;
                    if (up){
                        int prevX = Right[k-1-Rfrom];
                        previous = new Point(prevX, prevX - (k-1));
                        mid = previous.up();
                    } else {
                        int prevX = Right[k+1-Rfrom];
                        previous = new Point(prevX, prevX - (k+1));
                        mid = previous.left();
                    }
                    Point end = mid.moveDiagonallyReverse(A, Afrom, Ato, B, Bfrom, Bto);                      
                    if ((delta%2) == 0){
                        if ((k >= leftUpper.getDiagonal() - d) && (k <= leftUpper.getDiagonal() + d)){                                                    
                                int forwardX = Left[k-Lfrom];
                                Point forward = new Point(forwardX, forwardX-k);
                                if ((forwardX != -1) && (forward.getX() >= end.getX())){
                                    return new Quadruple<>(new Snake(previous, mid, end), 2*d, Boolean.TRUE, Boolean.TRUE);
                                }
                        }
                    }
                    Right[k-Rfrom] = end.getX();
                }
                
                if (heuristic){                                        
                    if (d >= tooExpensive){
                        int leftBestX = 0;
                        int leftBestXY = 0;
                        for (int k = leftUpper.getDiagonal() - d; k <= leftUpper.getDiagonal() + d; k += 2){
                            int x = Left[k-Lfrom] > Ato ? Ato : Left[k-Lfrom];
                            int y = x - k;
                            if (Bto < y){
                                y = Bto;
                                x = y + k;
                            }
                            if ((x+y) > leftBestXY){
                                leftBestXY = x + y;
                                leftBestX = x;
                            }
                        }
                        int rightBestX = Ato;
                        int rightBestXY = Ato + Bto;
                        for (int k = rightLower.getDiagonal() - d; k <= rightLower.getDiagonal() + d; k += 2){
                            int x = Right[k-Rfrom] > Afrom ? Right[k-Rfrom] : Afrom;
                            int y = x - k;
                            if (y < Bfrom){
                                y = Bfrom;
                                x = y + k;
                            }
                            if ((x+y) < rightBestXY){
                                rightBestXY = x + y;
                                rightBestX = x;
                            }
                        }
                        if ((Ato + Bto - rightBestXY) > (leftBestXY - (Afrom + Bfrom))){
                            int x = rightBestX;
                            int y = rightBestXY - rightBestX;
                            int k = x - y;
                            Point end = new Point(x, y);
                            Point mid = end.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);
                            Point start;                            
                            if (Right[k+1-Rfrom] < Right[k-1-Rfrom] + 1){
                                start = mid.right();
                            } else {
                                start = mid.down();
                            }                            
                            return new Quadruple<>(new Snake(start, mid, end), d, Boolean.FALSE, Boolean.FALSE);
                        } else {
                            int x = leftBestX;
                            int y = leftBestXY - leftBestX;
                            int k = x - y;
                            Point end = new Point(x, y);
                            Point mid = end.moveDiagonallyReverse(A, Afrom, Ato, B, Bfrom, Bto);
                            Point start;
                            if (Left[k+1-Lfrom] > Left[k-1-Lfrom] + 1){
                                start = mid.up();
                            } else {
                                start = mid.left();
                            }
                            return new Quadruple<>(new Snake(start, mid, end), d, Boolean.FALSE, Boolean.TRUE);
                        }
                    }
                    
                }                
            }
        }
        return null;
    }
    
    /**
     * Finds a path through the edit graph and returns it as a list of edit operations
     * on array A.
     * @param heuristics
     * @return 
     */
    List<Operation> compare(boolean heuristics){
        List<Operation> ops = compare(0, A.length, 0, B.length, heuristics, true);
        size = 0;
        return ops;
    }
    
    /**
     * Finds a path through the edit graph and returns it as a list of edit operations
     * on array A.
     * @param Afrom X-coordinate of the edit graph initial point.
     * @param Ato X-coordinate of the edit graph end point.
     * @param Bfrom Z-coordinate of the edit graph initial point.
     * @param Bto Z-coordinate of the edit graph end point.
     * @param heuristics If true, heuristics may be used to save time, but a suboptimal path may be returned.
     * @return 
     */
    private List<Operation> compare(int Afrom, int Ato, int Bfrom, int Bto, boolean heuristics, boolean addup){
        if ((sizeLimit > 0) && (size > sizeLimit)){
            return null;
        }
        final int n = Ato - Afrom;
        final int m = Bto - Bfrom;
        List<Snake> res = new LinkedList<>();
        if ((m==0) && (n>0)){
            if (addup){
                size += n;
            }
            if ((sizeLimit > 0) && (size > sizeLimit)){
                return null;
            }            
            for (int i = Afrom; i<Ato; i++){
                res.add(new Snake(new Point(i, Bfrom), new Point(i+1, Bfrom), new Point(i+1, Bfrom)));
            }
            return fromSnakes(res);
        }
        if ((n==0) && (m>0)){
            if (addup){
                size += m;
            }
            if ((sizeLimit > 0) && (size > sizeLimit)){
                return null;
            }
            for (int i = Bfrom; i<Bto; i++){
               res.add(new Snake(new Point(Afrom, i), new Point(Afrom, i+1), new Point(Afrom, i+1)));
            }
            return fromSnakes(res);
        }
        if ((m==0) && (n==0)){
           return new LinkedList<>();
        }
        Quadruple<Snake,Integer,Boolean,Boolean> middleSnake = findMiddleSnake(Afrom, Ato, Bfrom, Bto, heuristics);
        if (middleSnake == null){
            return null;
        }
        int d = middleSnake.b;   
        boolean optimal = middleSnake.c;
        boolean leftIsAddedUp = middleSnake.d;
        if (addup){
            size += d;
        }
        if ((sizeLimit > 0) && (size > sizeLimit)){
            return null;
        }
        if ((middleSnake.a.getEnd().getX() < middleSnake.a.getStart().getX()) ||
                (middleSnake.a.getEnd().getY() < middleSnake.a.getStart().getY())){                                
            Point start_old = middleSnake.a.getStart();
            Point middle_old = middleSnake.a.getMid();
            Point end_old = middleSnake.a.getEnd();
            if (middle_old.equals(end_old)){
                middleSnake = new Quadruple<>(new Snake(end_old, start_old, start_old), middleSnake.b, middleSnake.c, middleSnake.d);
            } else {
                middleSnake = new Quadruple<>(new Snake(end_old, middleSnake.a.getMid(), start_old), middleSnake.b, middleSnake.c, middleSnake.d);
            }
        }
        Point start = middleSnake.a.getStart();
        Point end = middleSnake.a.getEnd();        
        if (d > 1){
            boolean leftAddUp = addup && !optimal && !leftIsAddedUp;
            List<Operation> res_left = compare(Afrom, start.getX(), Bfrom, start.getY(), heuristics, leftAddUp);
            if (res_left == null){
                return null;
            }            
            boolean rightAddUp = addup && !optimal && leftIsAddedUp;
            List<Operation> res_right = compare(end.getX(), Ato, end.getY(), Bto, heuristics, rightAddUp);            
            if (res_right == null){
                return null;
            }            
            List<Operation> res1 = new ArrayList<>();            
            res1.addAll(res_left);
            res1.addAll(fromSnakes(middleSnake.a));
            res1.addAll(res_right);
            return res1;
        } else if (d == 1){
            Point leftupper = new Point(Afrom, Bfrom);
            Point tail = leftupper.moveDiagonally(A, Afrom, Ato, B, Bfrom, Bto);
            Snake s = new Snake(leftupper, leftupper, tail);
            res.add(s);
            res.add(middleSnake.a);
            return fromSnakes(res);
       } else { // d == 0
            return fromSnakes(middleSnake.a);
       }
    }
    
    /**
     * Constructs a list of operations from a sequence of snakes in the edit graph.
     * @param snakes
     * @return 
     */
    private List<Operation> fromSnakes(List<Snake> snakes){        
        List<Operation> ops = new ArrayList<>();                                            
        for (Snake s : snakes){
            Point op1 = s.getMid().minus(s.getStart());
            Point op2 = s.getEnd().minus(s.getMid());            
            if (op1.equals(Point.DOWN)){
                ops.add(new Insert(B[s.getStart().getY()]));
            } else if (op1.equals(Point.RIGHT)){
                ops.add(new Delete());
            } else if (op1.equals(Point.ZERO)){
                //no operation
            } else {
                ops.add(new Diagonal(op1.getX()));
            }            
            if (op2.equals(Point.DOWN)){
                ops.add(new Insert(B[s.getMid().getY()]));
            } else if (op2.equals(Point.RIGHT)){
                ops.add(new Delete());
            } else if (op2.equals(Point.ZERO)){
                //no operation
            } else {
                ops.add(new Diagonal(op2.getX()));
            }
        }
        return ops;
    }
    
    /**
     * Constructs a list of (1 to 2) operations from a single snake in the edit graph.
     * @param s
     * @return 
     */
    private List<Operation> fromSnakes(Snake s){
        return fromSnakes(Arrays.asList(s));
    }
}
