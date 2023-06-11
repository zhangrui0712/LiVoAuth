package idpservice.db;

import cmd.ToIdpChallengeCodeResCmd;
import idpservice.audio.pretreatment.GetFeatureCoffients;
import util.Log;
import java.sql.*;


public class DBManager {

    private static final String TAG = DBManager.class.getSimpleName();
    private static final String URL = "jdbc:mysql://localhost:3306/voice?"
    + "user=root&password=4267988&useUnicode=true&characterEncoding=UTF8";

    private static Connection conn;

    public static Connection getConnection() {
        if (conn == null) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(URL);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return conn;
    }

    public static void initdatabase() {
        Connection connection = getConnection();
        createDb(connection);
    }

    private static boolean createDb(Connection conn) {
        String sql = "create table if not exists user_profile("
                + "id int auto_increment,"
                + "user_id varchar(50) not null,"
                + "codenum varchar(2) not null,"
                + "codedata LONGTEXT NULL,"
                + "primary key(id),"
                + "unique(user_id,codenum))";
        Statement stmt;
        try {
            stmt = conn.createStatement();
            boolean b = stmt.execute(sql);
            if (b)
                Log.v(TAG, "success create db");
            return b;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //用户注册存储声纹信息
    public static boolean saveUserVoice(String uaId, ToIdpChallengeCodeResCmd responseCmd) {

        String sql = "insert into user_profile(user_id,codenum,codedata) values (?,?,?)";
        Connection conn = getConnection();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement(sql);
            conn.setAutoCommit(false);
            for (int i = 0; i < responseCmd.challengeCodes.size(); i++) {
                String challengeCode = responseCmd.challengeCodes.get(i);
                String data = voiceToString(responseCmd.voicePatternCodes.get(i));
                pst.setString(1, uaId);
                pst.setString(2, challengeCode);
                pst.setString(3, data);
                pst.addBatch();
                //Log.v(TAG, "insert " + uaId + "|" + challengeCode + "|" + data);
            }
            Log.v(TAG, "-----数据插入成功！-------");
            
            pst.executeBatch(); //批量执行
            conn.commit();//提交事务
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
                //回滚
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            Log.e(TAG, "error insert challenge data !!");
        }

        return false;
    }

    //更新声纹信息
    public static boolean uodateUserVoice(String uaId, ToIdpChallengeCodeResCmd responseCmd) {
        String sql = "update user_profile set codedata = ? where user_id = ? and codenum = ?";
        Connection conn = getConnection();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement(sql);
            conn.setAutoCommit(false);
            for (int i = 0; i < responseCmd.challengeCodes.size(); i++) {
                String challengeCode = responseCmd.challengeCodes.get(i);
                String data = voiceToString(responseCmd.voicePatternCodes.get(i));
                pst.setString(1, uaId);
                pst.setString(2, challengeCode);
                pst.setString(3, data);
                pst.addBatch();
            }

            pst.executeBatch(); //批量执行
            conn.commit();//提交事务
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
                //回滚
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            Log.e(TAG, "error insert challenge data !!");
        }

        return false;
    }

    //删除声纹信息
    public static boolean deleteUserVoice(String uaId) {
        Connection conn = getConnection();
        String sql = "DELETE FROM user_profile where user_id = ?";
        PreparedStatement pst = null;

        try {
            pst = conn.prepareStatement(sql);
            pst.setString(1, uaId);
            pst.execute();
            Log.v(TAG, "delete user voice " + uaId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static String voiceToString(float[] f) {
        GetFeatureCoffients featureCoffients = new GetFeatureCoffients(f);
        double voicefeaturevector[][] = featureCoffients.getvoiceFeatureVector();
        StringBuilder codeData = new StringBuilder();
        for (int ii = 0; ii < voicefeaturevector.length; ii++) {
            for (int j = 0; j < voicefeaturevector[0].length; j++) {
                if (ii > 0 || j > 0) {
                    codeData.append(",");
                }
                codeData.append(voicefeaturevector[ii][j]);
            }
        }
        return codeData.toString();
    }

    public static double[][] getUserVoiceData(String user_id, String code_number) throws SQLException {
    	
    	String sql = "SELECT * FROM user_profile where user_id= ? and codenum = ?";
    	PreparedStatement pst = null;
        pst = conn.prepareStatement(sql);
  
        pst.setString(1, user_id);
        pst.setString(2, code_number);
        ResultSet rs =  pst.executeQuery();
   
        String[] patternStrArr = null;
        double pattern[][] = null;
        String thisLine = null;
        while (rs.next()) {
            thisLine = rs.getString("codedata");
            if (thisLine != null) {
                patternStrArr = thisLine.split(",");
//                System.out.println("length=" + patternStrArr.length);
            }

            int rowCount = patternStrArr.length / 39;
            pattern = new double[rowCount][39];

            for (int i = 0; i < rowCount; i++) {
                for (int k = 0; k < 39; k++) {
                    pattern[i][k] = Double.parseDouble(patternStrArr[i * 39 + k]);
//                    System.out.print("pattern [" + i + "][" + k + "] =" + pattern[i][k]);
                }
//                System.out.println();
            }
//            System.out.println("???????????");
        }
        return pattern;

    }
}
