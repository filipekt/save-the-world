/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt.diff;

/**
 *
 * @author filipekt
 */
class Vector implements Cloneable{
    final private Snake[] snakes;

    final private int from;

    final private int to;

    public Snake get(int i){
        if (i >= to){
            return null;
        }
        return snakes[i - from];
    }            
    public void set(int i, Snake s){
        snakes[i - from] = s;
    }
    Vector(int size){
        this.snakes = new Snake[(2 * size) + 1];            
        this.from = -size;
        this.to = size;            
    }
    Vector(Snake[] snakes, int from, int to){
        this.snakes = snakes;
        this.from = from;
        this.to = to;
    }
    @Override
    public Vector clone(){
        return new Vector(snakes.clone(), from, to);
    }
    public Snake[] toArray(){
        return snakes;
    }
}
