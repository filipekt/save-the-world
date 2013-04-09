/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt;

import cz.filipekt.diff.EditScript;

/**
 *
 * @author filipekt
 */
public class Test {
    
    public static final int size = 10000;
    public static void main (String[] args){
        byte[] a = new byte[size];
        int j = 0;
        for (int i = 0; i<size; i++){
//            a[i] = (byte)i;
            a[i] = 1;
        }
        
        byte[] b;
        b = new byte[size];
        j = 0;
        for (int i = 0; i<size; i++){
            b[i] = (byte) (((byte)i) * 2);
//            b[i] = (byte)(i+1);
        }     
//        a = new byte[]{(byte)0};
//        b = a;
        printarray(a, 50);        
        printarray(b, 50);
        EditScript es = EditScript.createScript(a, b);
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
