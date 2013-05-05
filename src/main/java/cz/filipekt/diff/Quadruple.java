package cz.filipekt.diff;

/**
 * A simple generic quadruple.
 * @author Tomas Filipek
 */
class Quadruple <A,B,C,D> {
    
    final A a;    
    final B b;
    final C c;
    final D d;

    Quadruple(A a, B b, C c, D d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    
}
