package ir.amv.snippets.referenceinitialsetup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BreakingCode {
  private static int amir = 0;

  public int getAmir() {
    return amir;
  }

  public static void method() {
    Connection conn = null;
    try {
      conn = DriverManager.getConnection("jdbc:mysql://localhost/test?" +
          "user=steve&password=blue"); // Sensitive
      String uname = "steve";
      String password = "blue";
      conn = DriverManager.getConnection("jdbc:mysql://localhost/test?" +
          "user=" + uname + "&password=" + password); // Sensitive

      java.net.PasswordAuthentication pa = new java.net.PasswordAuthentication("userName",
          "1234".toCharArray());  // Sensitive
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
