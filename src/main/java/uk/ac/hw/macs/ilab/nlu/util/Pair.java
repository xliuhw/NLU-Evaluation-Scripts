package uk.ac.hw.macs.ilab.nlu.util;

import java.util.Objects;

/**
 *
 * @author X.Liu
 */
public class Pair<K, V> {

    private final K element0;
    private final V element1;

    public static <K, V> Pair<K, V> createPair(K element0, V element1) {
        return new Pair<K, V>(element0, element1);
    }

    public Pair(K element0, V element1) {
        this.element0 = element0;
        this.element1 = element1;
    }

    public K getElement0() {
        return element0;
    }

    public V getElement1() {
        return element1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        }
        Pair p2 = (Pair) obj;

        // to make case insensitive
        if (this.element0 instanceof String && this.element1 instanceof String) {
            String s0 = (String) this.element0;
            String s1 = (String) this.element1;
            String s0p2 = (String) p2.getElement0();
            String s1p2 = (String) p2.getElement1();
            if (s0.equalsIgnoreCase(s0p2) && s1.equalsIgnoreCase(s1p2)) {
                return true;
            } else {
                return false;
            }

        } else if (this.element0.equals(p2.getElement0())
                && this.element1.equals(p2.getElement1())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.element0);
        hash = 79 * hash + Objects.hashCode(this.element1);
        return hash;
    }

    @Override
    public String toString() {
        String p = "<" + this.element0 + ", " + this.element1 + ">";
        return p;
    }
}
