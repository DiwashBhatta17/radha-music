package com.radha.music;
import java.util.Locale;
final class SearchDetails {
 static String describe(long duration,long views){long seconds=Math.max(0,duration/1000);String length=duration<=0?"Duration unavailable":seconds>=3600?String.format(Locale.US,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);return length+(views<0?"":" · "+views(views)+" views");}
 static String views(long value){if(value<1000)return String.valueOf(value);double divisor=value>=1000000000?1000000000d:value>=1000000?1000000d:1000d;return String.format(Locale.US,"%.1f",value/divisor).replace(".0", "")+(divisor==1000000000d?"B":divisor==1000000d?"M":"K");}
}
