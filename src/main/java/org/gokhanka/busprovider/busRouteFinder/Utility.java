package org.gokhanka.busprovider.busRouteFinder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.google.gson.Gson;
import spark.ResponseTransformer;
/**
 * Utility class for the project
 * 
 * Holding all parametric and fixed values used in the project
 * if dynamic configuration is needed these parameters can be used
 * 
 * Plus: utility methods are here
 * @author gokhanka
 *
 */
public class Utility {

    public static int           NEGATIVE_ONE                 = -1;
    public static int           ZERO                         = 0;
    public static int           ONE                          = 1;
    public static int           TWO                          = 2;
    public static String        DELIMETER                    = " ";
    public static int           MAX_NUM_OF_ROUTES            = 100000;
    public static int           COUNT_OF_ROUTES_PER_THEAD    = 1000;
    public static int           HTTP_PORT                    = 8088;
    public static int           HTTP_THEAD_MAX               = 100;
    public static int           HTTP_THEAD_MIN               = 5;
    public static int           HTTP_IDLE_TIME               = 3000;
    public static int           MAX_ALLOWED_REQUEST          = 100;
    public static int           MAX_ALLOWED_REQUEST_DURATION = 1000;
    public static long          RESPONSE_TIMEOUT             = 2000;
    public static int           FILE_READER_THREAD_COUNT     = 10;
    public static String        START                        = "start";
    public static String        STOP                         = "stop";
    public static String        BLOCK                        = "block";
    public static boolean       TRUE                         = true;
    public static boolean       FALSE                        = false;
    private static final Logger logger                       = LogManager.getLogger();

    public static boolean validateInput(String arr, String dep) {
        boolean result = TRUE;
        if (arr == null || dep == null || arr.length() < 1 || dep.length() < 1)
            result = FALSE;
        return result;
    }
    /**
     * binary search method for the given sorted int[] a
     * @param a
     * @param low
     * @param high
     * @param key
     * @return
     */
    public static int binarySearch(int[] a, int low, int high, int key) {
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (key < a[mid])
                high = mid - 1;
            else if (key > a[mid])
                low = mid + 1;
            else
                return mid;
        }
        return NEGATIVE_ONE;
    }
    /**
     * search method for finding bus route between arrival and departure station
     * in the search in fact it is not important if key1 is arrival or departure station
     * as it is assumed that the link between them is bidirectional
     * @param sortedArray
     * @param key1 - departure
     * @param key2 - arrival
     * @return
     */
    public static boolean ifBothExist(int[] sortedArray, int key1, int key2) {
        boolean result = FALSE;
        if (key1 == key2) {
            logger.warn("found key1 {} equals key2 {}", key1, key2);
            result = TRUE;
        } else {
            // it is assumed that all lines are bi-directional
            int len = sortedArray.length - 1;
            int ref = binarySearch(sortedArray, 0, len, key1);
            if (ref != NEGATIVE_ONE) {
                if (logger.isTraceEnabled())
                    logger.trace("found key1 {} at index {}", key1, ref);
                ref = binarySearch(sortedArray, 0, len, key2);
                if (ref != NEGATIVE_ONE) {
                    if (logger.isTraceEnabled())
                        logger.trace("found key2 {} at index {}", key2, ref);
                    result = TRUE;
                }
            }
        }
        return result;
    }

    public static void quit() {
        logger.error("Goodbye ...");
        System.out.println("Goodbye ...");
        System.exit(0);
    }

    public static String toJson(Object object) {
        return new Gson().toJson(object);
    }

    public static ResponseTransformer json() {
        return Utility::toJson;
    }

    public static String retriveFile(String[] input) {
        String result = null;
        if (input.length != Utility.TWO) {
            logger.error("I need Input File Location as the second commandline argument after command as start|block");
        } else {
            if (input[1].length() < Utility.TWO) {
                logger.error("I can not find Input File with such short input param");
            } else {
                result = input[1].trim();
            }
        }
        return result;
    }
}
