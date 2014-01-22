package com.novemberain.langohr;

public class Util {
  public static boolean isFalsey(Object val) {
    return (val == null || false == val);
  }

  public static boolean isTruthy(Object val) {
    return !isFalsey(val);
  }
}
