package android.androidVNC;

public final class RotationCycleTest {
    public static void main(String[] args) {
        checkPanel(true, new int[] {0, 3, 2, 1});
        checkPanel(false, new int[] {1, 0, 3, 2});
        System.out.println("PASS: all four rotations on landscape- and portrait-native panels");
    }

    private static void checkPanel(boolean naturalLandscape, int[] expected) {
        for (int rotation = 0; rotation < 4; rotation++) {
            boolean landscape = (rotation % 2 == 0) == naturalLandscape;
            int current = RotationCycle.currentIndex(rotation, landscape);
            if (current != expected[rotation]) {
                throw new AssertionError("Wrong orientation for rotation " + rotation);
            }
            int next = (current + 1) % 4;
            if ((next % 2) == (current % 2)) {
                throw new AssertionError("Rotation must change the screen axis");
            }
        }
    }
}
