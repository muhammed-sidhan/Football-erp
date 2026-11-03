import com.football.config.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ResetSampleUsers {
    public static void main(String[] args) throws Exception {
        String hash = BCrypt.hashpw("password", BCrypt.gensalt(10));
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET password=? WHERE username IN ('admin','coadmin')")) {
            statement.setString(1, hash);
            System.out.println("Updated sample users: " + statement.executeUpdate());
        }
    }
}
