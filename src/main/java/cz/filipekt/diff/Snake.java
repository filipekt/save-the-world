package cz.filipekt.diff;

import java.util.Objects;

/**
 * A snake as described in [Myers].
 * @author Tomas Filipek
 */
class Snake {    
    
    /**
     * Start point.
     */
    private final Point start;
    
    /**
     * Middle point.
     */
    private final Point mid;
    
    /**
     * End point.
     */
    private final Point end;

    Point getStart() {
        return start;
    }

    Point getMid() {
        return mid;
    }

    Point getEnd() {
        return end;
    }    

    Snake(Point start, Point mid, Point end) {
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

