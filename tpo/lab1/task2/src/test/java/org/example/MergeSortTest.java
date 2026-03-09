package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MergeSortTest {

    static Stream<int[]> datasets() {
        return Stream.of(
                new int[]{},                         // пустой
                new int[]{1},                        // один
                new int[]{1, 2, 3, 4, 5},             // отсортирован
                new int[]{5, 4, 3, 2, 1},             // обратный
                new int[]{2, 2, 2, 2},                // дубликаты
                new int[]{3, 1, 2, 1, 3, 0, -5},       // смешанный
                new int[]{0, -1, -1, 10, 7, 7, 2},     // дубликаты+отриц
                new int[]{9, 8, 7, 1, 2, 3, 0}         // произвольный
        );
    }

    @ParameterizedTest
    @MethodSource("datasets")
    void sortedOutputMatchesJdk(int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);

        MergeSort.Result res = MergeSort.sortWithTrace(input);
        assertArrayEquals(expected, res.sorted);
    }

    @ParameterizedTest
    @MethodSource("datasets")
    void traceMatchesReference(int[] input) {
        MergeSort.Result actual = MergeSort.sortWithTrace(input);
        MergeSort.Result ref = ReferenceMergeSort.sortWithTrace(input);

        assertArrayEquals(ref.sorted, actual.sorted);
        assertEquals(ref.trace, actual.trace);
    }

    @Test
    void randomDatasetsTraceAndSort() {
        Random rnd = new Random(123);
        for (int tc = 0; tc < 50; tc++) {
            int n = rnd.nextInt(40);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(21) - 10;

            MergeSort.Result actual = MergeSort.sortWithTrace(a);
            MergeSort.Result ref = ReferenceMergeSort.sortWithTrace(a);

            assertArrayEquals(ref.sorted, actual.sorted);
            assertEquals(ref.trace, actual.trace);
        }
    }

    @Test
    void nullInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> MergeSort.sortWithTrace(null));
        assertThrows(IllegalArgumentException.class, () -> ReferenceMergeSort.sortWithTrace(null));
    }
}