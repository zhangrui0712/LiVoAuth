package authority;


import com.alibaba.fastjson.JSON;
import util.CyptoUtils;

import java.util.Date;
import java.util.Random;

/**
 *对原来的报文进行组装，加上时间戳 随机字符串 和签名，来验证报文是否正确
 * 原来的数据传输 格式：
 *          {
 *              CMD
 *          }
 *
 * 现在数据传输格式：
 *          {
 *              "timestamp": 1414587457000,
 *              "noncestr" :"Qm3WZLTPz0wzccnW",
 *              "signature":"0f9de62fce790f9a083d5c99e95740ceb90c27ed",
 *              "data":{
 *                     CMD
 *              }
 *
 *          }
 *
 */
public class AuthCmd   {


    /**
     *  有效的时间间隔  单位：毫秒
     *  客户端上报的时间戳与服务器当前时间戳是否在这个间隔内
     *
     */
    public static final int TIMRINTERVAL = 30 *1000;

    /**
     * noncestr 缓存器
     * 这里是30(30 * 1000)秒清除一次noncestr
     */
    public static CacheMap cacheMap =new CacheMap(30*1000);


    //时间戳
    public long timestamp;
    //随机字符串
    public String noncestr;

    //签名
    public String signature;

    //报文  注：有效数据
    public Object cmdData;






    /**
     * 校验 签名
     * @return
     */
    public  boolean checkSignature(){
        String signature= getSHASignature();
        return this.signature.equals(signature);
    }


    /**
     * 签名生成规则如下：参与签名的字段包括noncestr（随机字符串）, 有效的secret(固定), timestamp（时间戳）, 对所有待签名参数按照字段名的ASCII 码从小到大排序（字典序）后，
     * 使用URL键值对的格式（即key1=value1&key2=value2…）拼接成字符串shaStr。
     * 对string1作sha256加密，字段名和字段值都采用原始值
     * @return
     */
    private String getSHASignature(){


        String shaStr = "timestamp="+timestamp+"&noncestr="+noncestr+"&data="+cmdData.toString()+"&secret"+ CyptoUtils.KEY;

        return CyptoUtils.getSHA256Str(shaStr);
    }


    /**
     * 校验时间
     * @return
     */
    private  boolean checkTimestamp(){
        long nowtime =new Date().getTime();
        //获取时间差
        long timeinterval= (nowtime-timestamp);
        if(timeinterval<0){
            timeinterval=timeinterval*-1;
        }
        return timeinterval>= 0 && timeinterval<(AuthCmd.TIMRINTERVAL);
    }


    /**
     * 校验随机字符
     * @return
     */
    private  boolean checkNoncestr(){
       return  cacheMap.get(noncestr)==null;
    }


    /**
     * 校验数据
     * 返回有效数据
     * @param payload  解析的内容
     * @param clazz    转换类
     * @param <T>
     * @return
     */
    public static <T> T getReadCmd(String payload,Class<T> clazz)  {

        AuthCmd cmd =  JSON.parseObject(payload, AuthCmd.class);

        if(!cmd.checkTimestamp()){
            throw new RuntimeException("时间戳失效");
        }


        if(!cmd.checkNoncestr()){
            throw new RuntimeException("随机字符已使用");
        }


        if(!cmd.checkSignature()){
            throw new RuntimeException("签名不正确");
        }
        cacheMap.put(cmd.noncestr,cmd.timestamp);
        return  JSON.parseObject(cmd.cmdData.toString(), clazz);
    }


    /**
     * 组包
     * @param cmdData 发送的内容
     * @return
     */
    public static String getWriteCmd(String cmdData){
        AuthCmd cmd =new AuthCmd();
        cmd.timestamp =new Date().getTime();
        cmd.noncestr=getRandomString(16);
        cmd.cmdData= cmdData;
        cmd.signature= cmd.getSHASignature();
        return JSON.toJSONString(cmd);
    }


    /**
     * 获取随机字符
     * @param length 字符长度
     * @return
     */
    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }


}
