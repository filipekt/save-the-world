/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt;

import cz.filipekt.diff.EditScript;

/**
 * Just for testing purposes.
 * @author filipekt
 */
class Test {
    
    public static final int size = 20000;
    public static final int size2 = 10000;
    
    public static void main (String[] args){
        byte[] a = new byte[size];       
        for (int i = 0; i<size; i++){
            a[i] = (byte) (1);
        }
        
        byte[] b = new byte[size2];
        for (int i = 0; i<size2; i++){
            b[i] = (byte) (i);
        }   
//        b = a;
        printarray(a, 50);        
        printarray(b, 50);
        EditScript es = EditScript.createScript(a, b, 66000, true);
        if (es != null){
            printarray(es.applyTo(a), 50);
        } else {
            System.out.println("Skript nevytvoren.");
        }        
    } 
    
    private static void printarray(byte[] src, int count){
        if (src == null){
            return;
        }
        int limit = (src.length > count) ? count : src.length;
        for (int i = 0; i<limit; i++){
            System.out.print(src[i] + " ");
        }
        System.out.println();
    }
}
