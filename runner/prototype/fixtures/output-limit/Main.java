public class Main {
    public static void main(String[] args) {
        String chunk = "x".repeat(8192);
        while (true) {
            System.out.print(chunk);
            System.err.print(chunk);
        }
    }
}
