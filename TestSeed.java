import java.util.Random;

public class TestSeed {
    public static void main(String[] args) {
        Random r = new Random(12345);
        System.out.print("Rolls: ");
        for (int i = 0; i < 10; i++) {
            System.out.print((r.nextInt(6) + 1) + " ");
        }
    }
}
