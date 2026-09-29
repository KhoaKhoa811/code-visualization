public class Main {
    public static void main(String[] args) {
        byte[] tooLarge = new byte[128 * 1024 * 1024];
        System.out.println(tooLarge.length);
    }
}
