
public class TestConnection {

    public static void main(String[] args) {

        try {
            DBConnection.getConnection();

            System.out.println("Database Connected Successfully!");

        } catch (Exception e) {
            System.out.println("Connection Failed!");
            System.out.println(e.getMessage());
        }
    }
}
