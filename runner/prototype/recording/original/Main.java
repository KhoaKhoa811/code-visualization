public class Main {
    public static void main(String[] args) {
        int[] values = {3, 1};
        values[0] = values[1];
        // Development probe outside the recorded region.
        System.out.print("FINAL=" + values[0] + "," + values[1]);
        System.err.print("PROBE");
    }
}
