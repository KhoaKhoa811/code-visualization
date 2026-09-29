public class Main {
    public static void main(String[] args) {
        int[] a = {3, 1};
        a[0] = a[1];
        System.out.print("ARRAY=" + a[0] + "," + a[1]);
        System.err.print("STDERR");
    }
}
