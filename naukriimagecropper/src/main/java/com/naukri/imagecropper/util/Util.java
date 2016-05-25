package com.naukri.imagecropper.util;

/**
 * Created by akash.singla on 3/16/2016.
 */
public class Util
{
    /**
     * Check Empty String.
     * @param str
     * @return
     */
    public static boolean isEmptyString(String str)
    {
        if(str != null && str.trim().length() > 0 && !str.equalsIgnoreCase("null"))
            return true;

        return false;
    }
}
