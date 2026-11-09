package android.androidVNC;

/** Maps display rotation to Landscape/Portrait/ReverseLandscape/ReversePortrait. */
final class RotationCycle {
    private RotationCycle() { }

    static int currentIndex(int rotation, boolean landscape) {
        boolean sideways = rotation == 1 || rotation == 3;
        boolean naturalLandscape = sideways ? !landscape : landscape;
        // Display rotation is relative to the panel's natural orientation.
        int[] indices = naturalLandscape ? new int[] {0, 3, 2, 1}
                : new int[] {1, 0, 3, 2};
        return indices[rotation & 3];
    }
}
