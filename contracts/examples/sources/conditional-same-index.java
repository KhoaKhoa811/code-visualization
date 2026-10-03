public class Main {
    public static void main(String[] args) {
        int[] values = {3};
        if (values[0] > values[0]) {
            int temp = values[0];
            values[0] = values[0];
            values[0] = temp;
        }
    }
}
