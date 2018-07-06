package com.bossien.flowtreatmentservice.utils;

import com.google.common.collect.Ordering;

import java.text.DecimalFormat;
import java.util.*;

/**
 * 用户快速排序集合工具类
 *
 * @author gb
 */
public class CollectionUtils {
    /**
     * 指定字端 从大到小
     *
     * @param resultTotalScores
     * @param field
     */
    public static void orderMapByField(List<Map<String, Object>> resultTotalScores, final String field) {
        Comparator comparator = getComparator(field);
        Ordering from = Ordering.from(comparator);
        List<Map<String, Object>> maps = from.reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(maps);
    }
    /**
     * 指定字端 从大到小
     *
     * @param resultTotalScores
     * @param ascField          ,descSecondField
     */
    public static void orderMapByFields(List<Map<String, Object>> resultTotalScores, final String ascField, final String descSecondField) {
        Comparator comparator = getSecondComparator(ascField, descSecondField);
        Ordering from = Ordering.from(comparator);
        List<Map<String, Object>> maps = from.reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(maps);
    }


    /**
     * 指定字端 从大到小
     *
     * @param resultTotalScores
     * @param ascField          ,descSecondField
     */
    public static void orderMapByFieldsAsc(List<Map<String, Object>> resultTotalScores, final String ascField, final String ascSecondField) {
        Comparator comparator = getSecondComparatorAsc(ascField, ascSecondField);
        Ordering from = Ordering.from(comparator);
        List<Map<String, Object>> maps = from.reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(maps);
    }

    /**
     * 指定字端 默认小到大
     *
     * @param resultTotalScores
     * @param field
     */
    public static void orderMapByFieldNoReverse(List<Map<String, Object>> resultTotalScores, final String field) {
        List<Map<String, Object>> maps = Ordering.from(new Comparator<Map<String, Object>>() {

            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return o1.get(field) == null || o2.get(field) == null
                        ? 0 : String.valueOf(o1.get(field)).compareTo(String.valueOf(o2.get(field)));
            }
        }).sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(maps);
    }

    public static void orderList(List<String> resultTotalScores) {
        List<String> strings = Ordering.from(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        }).reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(strings);
    }

    public static void orderLongList(List<Long> resultTotalScores) {
        List<Long> strings = Ordering.from(new Comparator<Long>() {

            @Override
            public int compare(Long o1, Long o2) {
                return o1.compareTo(o2);
            }
        }).reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(strings);
    }

    public static void orderIntegerList(List<Integer> resultTotalScores) {
        List<Integer> strings = Ordering.from(new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        }).reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(strings);
    }

    public static void orderLongListNoReverse(List<Long> resultTotalScores) {
        List<Long> strings = Ordering.from(new Comparator<Long>() {

            @Override
            public int compare(Long o1, Long o2) {
                return o1.compareTo(o2);
            }
        }).sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(strings);
    }

    public static void orderDoubleList(List<Double> resultTotalScores) {
        List<Double> strings = Ordering.from(new Comparator<Double>() {

            @Override
            public int compare(Double o1, Double o2) {
                return o1.compareTo(o2);
            }
        }).reverse().sortedCopy(resultTotalScores);
        resultTotalScores.clear();
        resultTotalScores.addAll(strings);
    }

    public static boolean checkNotNullList(List<?> list) {
        if (null != list && list.size() != 0) {
            return true;
        } else {
            return false;
        }
    }

    private static Comparator getComparator(final String field) {
        return new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret =0 ;
                Object o11 = o1.get(field);
                Object o12 = o2.get(field);
                if(o11== null){
                    return 0;
                }
                if(o12== null){
                    return 0;
                }
                if (o11 instanceof  Integer && o12 instanceof  Integer) {
                    ret = ((Integer) o11).compareTo((Integer)o12);
                } else if (o11 instanceof  Double && o12 instanceof  Double) {

                    ret = ((Double)o11).compareTo((Double)o12);
                } else if (o11 instanceof  Long && o12 instanceof  Long) {

                    ret = ((Long)o11).compareTo((Long)o12);
                } else if (o11 instanceof  Float && o12 instanceof  Float) {

                    ret = ((Float)o11).compareTo((Float)o12);
                } else if (o11 instanceof  Date && o12 instanceof  Date) {

                    ret = ((Date)o11).compareTo((Date)o12);
                } else if (o11 instanceof  String && o12 instanceof  String) {

                    ret = ((String)o11).compareTo((String)o12);
                } else {
                    ret = 0;
                }
                return ret;
            }
        };
    }

    private static Comparator getSecondComparator(final String firstField, final String secondField) {
        return new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret = 0;
                try {
                    Object o11 = o1.get(firstField);
                    Object o12 = o2.get(firstField);
                    if(o11== null){
                       return 0;
                    }
                    if(o12== null){
                        return 0;
                    }
                    if (o11 instanceof  Integer && o12 instanceof  Integer) {

                        ret = ((Integer) o11).compareTo((Integer)o12);
                    } else if (o11 instanceof  Double && o12 instanceof  Double) {

                        ret = ((Double)o11).compareTo((Double)o12);
                    } else if (o11 instanceof  Long && o12 instanceof  Long) {

                        ret = ((Long)o11).compareTo((Long)o12);
                    } else if (o11 instanceof  Float && o12 instanceof  Float) {

                        ret = ((Float)o11).compareTo((Float)o12);
                    } else if (o11 instanceof  Date && o12 instanceof  Date) {

                        ret = ((Date)o11).compareTo((Date)o12);
                    } else if (o11 instanceof  String && o12 instanceof  String) {

                        ret = ((String)o11).compareTo((String)o12);
                    } else {
                        ret = 0;
                    }
                    if(ret==0){
                        Object o112 = o1.get(secondField);
                        Object o111 = o2.get(secondField);
                        if (o111 instanceof  Integer && o112 instanceof  Integer) {

                            ret = ((Integer) o111).compareTo((Integer)o112);
                        } else if (o111 instanceof  Double && o112 instanceof  Double) {

                            ret = ((Double)o111).compareTo((Double)o112);
                        } else if (o111 instanceof  Long && o112 instanceof  Long) {

                            ret = ((Long)o111).compareTo((Long)o112);
                        } else if (o111 instanceof  Float && o112 instanceof  Float) {

                            ret = ((Float)o111).compareTo((Float)o112);
                        } else if (o111 instanceof  Date && o112 instanceof  Date) {

                            ret = ((Date)o111).compareTo((Date)o112);
                        } else if (o111 instanceof  String && o112 instanceof  String) {
                            ret = ((String)o111).compareTo((String)o112);
                        } else {
                            ret = 0;
                        }
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                return ret;
            }
        };

    }
    private static Comparator getSecondComparatorAsc(final String firstField, final String secondField) {
        return new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret = 0;
                try {
                    Object o11 = o1.get(firstField);
                    Object o12 = o2.get(firstField);
                    if(o11== null){
                        return 0;
                    }
                    if(o12== null){
                        return 0;
                    }
                    if (o11 instanceof  Integer && o12 instanceof  Integer) {

                        ret = ((Integer) o11).compareTo((Integer)o12);
                    } else if (o11 instanceof  Double && o12 instanceof  Double) {

                        ret = ((Double)o11).compareTo((Double)o12);
                    } else if (o11 instanceof  Long && o12 instanceof  Long) {

                        ret = ((Long)o11).compareTo((Long)o12);
                    } else if (o11 instanceof  Float && o12 instanceof  Float) {

                        ret = ((Float)o11).compareTo((Float)o12);
                    } else if (o11 instanceof  Date && o12 instanceof  Date) {

                        ret = ((Date)o11).compareTo((Date)o12);
                    } else if (o11 instanceof  String && o12 instanceof  String) {

                        ret = ((String)o11).compareTo((String)o12);
                    } else {
                        ret = 0;
                    }
                    if(ret==0){
                        Object o111 = o1.get(secondField);
                        Object o112 = o2.get(secondField);
                        if(o111== null){
                            return 0;
                        }
                        if(o112== null){
                            return 0;
                        }
                        if (o111 instanceof  Integer && o112 instanceof  Integer) {

                            ret = ((Integer) o111).compareTo((Integer)o112);
                        } else if (o111 instanceof  Double && o112 instanceof  Double) {

                            ret = ((Double)o111).compareTo((Double)o112);
                        } else if (o111 instanceof  Long && o112 instanceof  Long) {

                            ret = ((Long)o111).compareTo((Long)o112);
                        } else if (o111 instanceof  Float && o112 instanceof  Float) {

                            ret = ((Float)o111).compareTo((Float)o112);
                        } else if (o111 instanceof  Date && o112 instanceof  Date) {

                            ret = ((Date)o111).compareTo((Date)o112);
                        } else if (o111 instanceof  String && o112 instanceof  String) {

                            ret = ((String)o111).compareTo((String)o112);
                        } else {
                            ret = 0;
                        }
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                return ret;
            }
        };
    }
    public static void main(final String[] args) {
        Double aDouble = convertScore(98.00, 1000*132l);
        Double aDouble1 = convertScore(540.98, 1000*23l);
        Double aDouble2 = convertScore(60.00, 1000*109l);
        Double aDouble3 = convertScore(60.00, 1000*15l);
        System.out.println(aDouble);
        System.out.println(aDouble1);
        System.out.println(aDouble2);
        System.out.println(aDouble3);
       Double aDouble4 = parseScore(24820.0361631, 22122l);
        System.out.println(aDouble4);
        System.out.println("------>"+aDouble4);
        List<Map<String, Object>> integers = new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("score", 6L);
        objectMap.put("battel", 0.00);
        integers.add(objectMap);
        Map<String, Object> objectMap1 = new HashMap<>();
        objectMap1.put("score", 3L);
        objectMap1.put("battel", 0.01);
        integers.add(objectMap1);
        Map<String, Object> objectMap2 = new HashMap<>();
        objectMap2.put("score", 3L);
        objectMap2.put("battel", 0.05);
        integers.add(objectMap2);
        Map<String, Object> objectMap3 = new HashMap<>();
        objectMap3.put("score", 5L);
        objectMap3.put("battel", null);
        integers.add(objectMap3);
        Map<String, Object> objectMap4 = new HashMap<>();
        objectMap4.put("score", null);
        objectMap4.put("battel", 0.00);
        integers.add(objectMap4);
        //orderMapByField(integers,"score");
        OrderByMapContainsIntegerLittleToBig(integers,"score");
        System.out.println("score:"+integers.toString());
      //  orderMapByFieldsAsc(integers, "score", "battel");
        System.out.println(integers);
        Map<String, List<Integer>> point = new HashMap<>();
        for (int i = 0; i < integers.size(); i++) {
            Map<String, Object> objectMap5 = integers.get(i);
            String battelKey = objectMap5.get("battel").toString();
            List<Integer> indexes = new ArrayList<>();
            indexes.add(i);
            if (point.get(battelKey) != null) {
                List<Integer> integers1 = point.get(battelKey);
                integers1.addAll(indexes);
            } else {
                point.put(battelKey, indexes);
            }
        }
     //   System.out.println(point);
    }

    public static void OrderByMapContainsIntegerLittleToBig(List<Map<String, Object>> result, final String integerField) {
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret ;
                Object o11 = o1.get(integerField);
                Object o12 = o2.get(integerField);
                if(o11== null){
                    return 0;
                }
                if(o12== null){
                    return 0;
                }
                if (o11 instanceof  Integer && o12 instanceof  Integer) {

                    ret = ((Integer) o11).compareTo((Integer)o12);
                } else if (o11 instanceof  Double && o12 instanceof  Double) {

                    ret = ((Double)o11).compareTo((Double)o12);
                } else if (o11 instanceof  Long && o12 instanceof  Long) {

                    ret = ((Long)o11).compareTo((Long)o12);
                } else if (o11 instanceof  Float && o12 instanceof  Float) {

                    ret = ((Float)o11).compareTo((Float)o12);
                } else if (o11 instanceof  Date && o12 instanceof  Date) {

                    ret = ((Date)o11).compareTo((Date)o12);
                } else if (o11 instanceof  String && o12 instanceof  String) {

                    ret = ((String)o11).compareTo((String)o12);
                } else {
                    ret = 0;
                }
                return ret ;
            }
        });
    }

    public static void OrderByMapContainsIntegerBigToLittle(List<Map<String, Object>> result, final String integerField) {
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret ;
                Object o11 = o1.get(integerField);
                Object o12 = o2.get(integerField);
                if(o11== null){
                    return 0;
                }
                if(o12== null){
                    return 0;
                }
                if (o11 instanceof  Integer && o12 instanceof  Integer) {

                    ret = ((Integer) o12).compareTo((Integer)o11);
                } else if (o11 instanceof  Double && o12 instanceof  Double) {

                    ret = ((Double)o12).compareTo((Double)o11);
                } else if (o11 instanceof  Long && o12 instanceof  Long) {

                    ret = ((Long)o12).compareTo((Long)o11);
                } else if (o11 instanceof  Float && o12 instanceof  Float) {

                    ret = ((Float)o12).compareTo((Float)o11);
                } else if (o11 instanceof  Date && o12 instanceof  Date) {

                    ret = ((Date)o12).compareTo((Date)o11);
                } else if (o11 instanceof  String && o12 instanceof  String) {

                    ret = ((String)o12).compareTo((String)o11);
                } else {
                    ret = 0;
                }
                return ret ;
            }
        });
    }

    public static void OrderByMapContainsDoubleLittleToBig(List<Map<String, Object>> result, final String integerField) {
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret ;
                Object o11 = o1.get(integerField);
                Object o12 = o2.get(integerField);
                if(o11== null){
                    return 0;
                }
                if(o12== null){
                    return 0;
                }
                if (o11 instanceof  Integer && o12 instanceof  Integer) {

                    ret = ((Integer) o11).compareTo((Integer)o12);
                } else if (o11 instanceof  Double && o12 instanceof  Double) {

                    ret = ((Double)o11).compareTo((Double)o12);
                } else if (o11 instanceof  Long && o12 instanceof  Long) {

                    ret = ((Long)o11).compareTo((Long)o12);
                } else if (o11 instanceof  Float && o12 instanceof  Float) {

                    ret = ((Float)o11).compareTo((Float)o12);
                } else if (o11 instanceof  Date && o12 instanceof  Date) {

                    ret = ((Date)o11).compareTo((Date)o12);
                } else if (o11 instanceof  String && o12 instanceof  String) {

                    ret = ((String)o11).compareTo((String)o12);
                } else {
                    ret = 0;
                }
                return ret ;
            }
        });
    }

    public static void OrderByMapContainsDoubleBigToLittle(List<Map<String, Object>> result, final String integerField) {
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int ret ;
                Object o11 = o1.get(integerField);
                Object o12 = o2.get(integerField);
                if(o11== null){
                    return 0;
                }
                if(o12== null){
                    return 0;
                }
                if (o11 instanceof  Integer && o12 instanceof  Integer) {

                    ret = ((Integer) o12).compareTo((Integer)o11);
                } else if (o11 instanceof  Double && o12 instanceof  Double) {

                    ret = ((Double)o12).compareTo((Double)o11);
                } else if (o11 instanceof  Long && o12 instanceof  Long) {

                    ret = ((Long)o12).compareTo((Long)o11);
                } else if (o11 instanceof  Float && o12 instanceof  Float) {

                    ret = ((Float)o12).compareTo((Float)o11);
                } else if (o11 instanceof  Date && o12 instanceof  Date) {

                    ret = ((Date)o12).compareTo((Date)o11);
                } else if (o11 instanceof  String && o12 instanceof  String) {

                    ret = ((String)o12).compareTo((String)o11);
                } else {
                    ret = 0;
                }
                return ret ;
            }
        });
    }


    public static void OrderByMapContainsLongLittleToBig(List<Map<String, Object>> result, final String integerField) {
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Long rank = 0L;
                Long rank1 = 0L;
                if (o1.get(integerField) != null) {
                    rank = (Long) o1.get(integerField);
                }
                if (o2.get(integerField) != null) {
                    rank1 = (Long) o2.get(integerField);
                }
                if (rank > rank1) {
                    return 1;
                } else if (rank < rank1) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

   public static Double convertScore(Double score,Long duration){
       long x = Math.round(score*100);
       Double y =(double) 800/duration;
       if(y.intValue()==1){
           y = 0.00 ;
       }
       Double result = x+y;
       DecimalFormat df = new DecimalFormat("#.00000000");
       String format = df.format(result);
       return  Double.valueOf(format);
   }
    public static Double parseScore(Double resultScore,Long duration){
        String s = resultScore.toString();
        String[] split = s.split("\\.");
        Long aLong = LangUtil.parseLong(split[0]);
        Double bLong =LangUtil.parseDouble("0."+split[1]);
        Double score = aLong/100.00;
        double v = 800 / bLong;
        System.out.println(score);
        System.out.println(v);
        return  score;
    }

}
