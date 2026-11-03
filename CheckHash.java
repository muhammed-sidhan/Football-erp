import org.mindrot.jbcrypt.BCrypt;

public class CheckHash {
    public static void main(String[] args) {
        System.out.println(BCrypt.checkpw("password", args[0]));
    }
}
