public class Main {
    public static void main(String[] args) {
        int[] values = {2, 2};
        if (values[0] > values[1]) {
            int temp = values[0];
            values[0] = values[1];
            values[1] = temp;
        }
    }
}
