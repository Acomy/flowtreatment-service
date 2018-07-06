package com.bossien.flowtreatmentservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 类型转换工具类.
 *
 * @author gaobo
 */
public class LangUtil {

    private static Logger logger = LoggerFactory.getLogger(LangUtil.class);

    public static Boolean parseBoolean(Object value) {
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.valueOf((String) value);
            }
        }
        return false;
    }

    public static boolean parseBoolean(Object value, boolean defaultValue) {
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                try {
                    return Boolean.valueOf((String) value);
                } catch (Exception e) {
                    logger.warn("parse boolean value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }

    /**
     * @param value Integer或String值
     * @return Integer 返回类型
     * @Title: parseInt
     * @Description: Int解析方法，可传入Integer或String值
     */
    public static Integer parseInt(Object value) {
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                return Integer.valueOf((String) value);
            } else {
                return Integer.valueOf((String) value);
            }
        }
        return null;
    }

    public static Integer parseInt(Object value, Integer defaultValue) {
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                try {
                    return Integer.valueOf((String) value);
                } catch (Exception e) {
                    // e.printStackTrace();
                    // logger.warn("parse Integer value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }

    /***
     *
     * @Title: parseLong
     * @Description: long解析方法，可传入Long或String值
     * @param value
     *            Integer或String值
     * @param @return
     * @return Long 返回类型
     */
    public static Long parseLong(Object value) {
        if (value != null && !"".equals(value)) {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof String) {
                return Long.valueOf((String) value);
            } else if (value instanceof Integer) {
                return Long.valueOf(value + "");
            } else if(value instanceof Double){
                return Double.valueOf(value+"").longValue();
            }
        }
        return null;
    }

    public static Long parseLong(Object value, Long defaultValue) {
        if (value != null) {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof String) {
                try {
                    String s = String.valueOf(value);
                    if (s.contains(".")) {
                        Double aDouble = Double.parseDouble(s);
                        return aDouble.longValue();
                    } else {
                        return Long.parseLong(s);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    logger.warn("parse Long value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }

    /**
     * @param value Double或String值
     * @return Double 返回类型
     * @Title: parseDouble
     * @Description: Double解析方法，可传入Double或String值
     */
    public static Double parseDouble(Object value) {
        if (value != null) {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof String) {
                return Double.valueOf((String) value);
            }
        }
        return null;
    }

    /**
     * @param value Double或String值
     * @return Double 返回类型
     * @Title: parseDouble
     * @Description: Double解析方法，可传入Double或String值
     */
    public static Double parseDoubleLatterTwo(Object value, Double defaultValue) {
        try {
            if (value != null) {
                if (value instanceof Double) {
                    Double value1 = (Double) value;
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(value1);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                } else if (value instanceof Integer) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                } else if (value instanceof String) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                }else if (value instanceof Character) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                }else if (value instanceof Float) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                }else if (value instanceof Byte) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                }else if (value instanceof Long) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                }else if (value instanceof Short) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    DecimalFormat dFormat=new DecimalFormat("0.00");
                    String yearString=dFormat.format(aDouble);
                    Double temp= Double.valueOf(yearString);
                    return temp;
                }
            } else {
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            return defaultValue;
        }
        return defaultValue;
    }
    /**
     * @param value Double或String值
     * @return Double 返回类型
     * @Title: parseDouble
     * @Description: Double解析方法，可传入Double或String值
     */
    public static Double parseDoubleLatterFive(Object value, Double defaultValue) {
        try {
            if (value != null) {
                if (value instanceof Double) {
                    Double value1 = (Double) value;
                    BigDecimal bg = new BigDecimal(value1).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof Integer) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof String) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                }else if (value instanceof Long) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof Short) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                }else if (value instanceof Byte) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof Character) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                }else if (value instanceof Float) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(5, RoundingMode.UP);
                    return bg.doubleValue();
                }
            } else {
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            return defaultValue;
        }
        return defaultValue;
    }
    /**
     * @param value Double或String值
     * @return Double 返回类型
     * @Title: parseDouble
     * @Description: Double解析方法，可传入Double或String值
     */
    public static Double parseDoubleLatterFour(Object value, Double defaultValue) {
        try {
            if (value != null) {
                if (value instanceof Double) {
                    Double value1 = (Double) value;
                    BigDecimal bg = new BigDecimal(value1).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof Integer) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof String) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();

                }else if (value instanceof Long) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof Character) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();

                }else if (value instanceof Float) {
                    Double aDouble = Double.valueOf(String.valueOf(value));
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();
                } else if (value instanceof String) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();
                }else if (value instanceof Byte) {
                    Double aDouble = Double.valueOf((String) value);
                    BigDecimal bg = new BigDecimal(aDouble).setScale(4, RoundingMode.UP);
                    return bg.doubleValue();
                }
            } else {
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            return defaultValue;
        }
        return defaultValue;
    }
   public static String parserDoubleTwo(Double x1){
      return String.format("%.2f", x1);
   }

    /**
     * @param value
     * @param @return
     * @return String 返回类型
     * @Title: toString
     * @Description: toString实现，当对象为null时直接返回null
     */
    public static String toString(Object value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    /**
     * @param str1
     * @param str2
     * @return boolean 返回类型
     * @Title: stringEquals
     * @Description: 验证两个字符串是否相等
     */
    public static boolean stringEquals(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return false;
        }

        return str1.equals(str2);
    }

    /**
     * 将时间戳转换为时间
     *
     * @param s
     * @return
     */
    public static String parseDate(String s) {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = Long.parseLong(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    /**
     * 将时间转换为时间戳
     *
     * @param s
     * @return
     * @throws ParseException
     */
    public static String parseStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }

    /**
     * 将时间转换为format字符串
     *
     * @param
     * @return
     * @throws ParseException
     */
    public static String parseTimeChar(Date time, String format) {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        res = simpleDateFormat.format(time);
        return res;
    }

    public static String parseString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * List和Set的转化(Set转化成List)
     */
    public static <T> List<T> setToList(Set<T> set) {
        List<T> list = new ArrayList<>();
        list.addAll(set);
        return list;
    }

    public static Set<Long> setStringToLong(Set<String> set) {
        Set<Long> longs = new HashSet<>();
        for (String string : set) {
            Long aLong = LangUtil.parseLong(string);
            longs.add(aLong);
        }
        return longs;
    }

    public static Set<String> longToSetString(List<Long> set) {
        Set<String> longs = new HashSet<>();
        for (Long value : set) {
            longs.add(String.valueOf(value));
        }
        return longs;
    }


    /**
     * Created on 2017年9月7日
     * <p>Discription:[转list]</p>
     *
     * @param object
     * @return
     * @author:[ljb]
     */
    public static <T> List<T> parseToList(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof List<?>) {
            return (List<T>) object;
        }
        return null;
    }

    /**
     * 当浮点型数据位数超过10位之后，数据变成科学计数法显示。用此方法可以使其正常显示。
     *
     * @param value
     * @return Sting
     */
    public static String formatFloatNumber(double value) {
        if (value != 0.00) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("########.00");
            return df.format(value);
        } else {
            return "0.00";
        }

    }

    public static String formatFloatNumber(Double value) {
        if (value != null) {
            if (value.doubleValue() != 0.00) {
                java.text.DecimalFormat df = new java.text.DecimalFormat("########.00");
                return df.format(value.doubleValue());
            } else {
                return "0.00";
            }
        }
        return "";
    }

    public static Double average(List<Integer> fromList) {
        Long sumLong = 0L;
        for (Integer aLong : fromList) {
            if (aLong != null) {
                sumLong += aLong;
            }
        }
        Double average = (double) sumLong / fromList.size();
        return average;
    }

    public static Double max(List<Integer> fromList) {
        Collections.sort(fromList);
        Collections.reverse(fromList);
        return fromList.get(0).doubleValue();
    }


}
