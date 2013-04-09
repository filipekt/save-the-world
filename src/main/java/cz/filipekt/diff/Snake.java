/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt.diff;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 *
 * @author filipekt
 */
class Snake {
    static final Snake ZERO = new Snake(Vrchol.ZERO, Vrchol.ZERO, Vrchol.ZERO);    
    
    private final Vrchol start;
    private final Vrchol mid;
    private final Vrchol end;

    Vrchol getStart() {
        return start;
    }

    Vrchol getMid() {
        return mid;
    }

    Vrchol getEnd() {
        return end;
    }
    
    boolean overlap(Snake s){
        Collection<Vrchol> points1 = this.getPoints();
        Collection<Vrchol> points2 = s.getPoints();
        points1.retainAll(points2);
        return !points1.isEmpty();
    }        
    
    private Collection<Vrchol> getPoints(){
        Collection<Vrchol> res = new HashSet<>();
        res.add(start);
        Vrchol v = mid;
        while (v.getX() < end.getX()){
            res.add(v);
            v = v.diagonalRight();
        }
        res.add(end);
        return res;
    }

    Snake(Vrchol start, Vrchol mid, Vrchol end) {
        this.start = start;
        this.mid = mid;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Snake){
            Snake s = (Snake) o;
            return (s.start == start) && (s.mid == mid) && (s.end == end);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.start);
        hash = 61 * hash + Objects.hashCode(this.mid);
        hash = 61 * hash + Objects.hashCode(this.end);
        return hash;
    }
}

