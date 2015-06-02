package com.game.globomb;

import android.os.Build;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by tdgunes on 02/06/15.
 */
public class Utility {
    private static final String[] NAMES = {"Deniz", "Taha", "Eren", "Enes", "Ömür", "Bahadır", "Yunus"};

    public static boolean inEmulator () {
        return Build.FINGERPRINT.startsWith("generic");
    }

    public static double getRandomLatitude () {
        return Utility.getRandomInRange(-20, 20);
    }

    public static double getRandomLongitude() {
        return Utility.getRandomInRange(-30, 30);
    }

    public static double getRandomInRange(int min, int max) {
        Random r = new Random();
        return (double) r.nextInt(max-min + 1) + min;
    }

    public static String getRandomName(){
        return NAMES[new Random().nextInt(NAMES.length)];
    }

}
