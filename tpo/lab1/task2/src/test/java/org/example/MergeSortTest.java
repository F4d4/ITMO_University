package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeSortTest {

    @Test
    void shouldSortRandomArray() {
        int[] array = {5, 2, 9, 1, 5, 6};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2, 5, 5, 6, 9}, array);
    }

    @Test
    void shouldSortAlreadySortedArray() {
        int[] array = {1, 2, 3, 4, 5};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, array);
    }

    @Test
    void shouldSortReverseArray() {
        int[] array = {5, 4, 3, 2, 1};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, array);
    }

    @Test
    void shouldSortArrayWithDuplicates() {
        int[] array = {3, 1, 2, 3, 1, 2};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 1, 2, 2, 3, 3}, array);
    }

    @Test
    void shouldSortArrayWithNegativeNumbers() {
        int[] array = {0, -1, 5, -3, 2};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{-3, -1, 0, 2, 5}, array);
    }

    @Test
    void shouldSortArrayWithOnlyNegativeNumbers() {
        int[] array = {-5, -1, -3, -2, -4};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{-5, -4, -3, -2, -1}, array);
    }

    @Test
    void shouldSortArrayWithZeros() {
        int[] array = {0, 0, 0, 0};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{0, 0, 0, 0}, array);
    }

    @Test
    void shouldSortArrayWithPositiveAndNegativeDuplicates() {
        int[] array = {2, -1, 2, -1, 0, 0};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{-1, -1, 0, 0, 2, 2}, array);
    }

    @Test
    void shouldHandleSingleElementArray() {
        int[] array = {42};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{42}, array);
    }

    @Test
    void shouldHandleTwoElementsSortedArray() {
        int[] array = {1, 2};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2}, array);
    }

    @Test
    void shouldHandleTwoElementsUnsortedArray() {
        int[] array = {2, 1};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2}, array);
    }

    @Test
    void shouldHandleEmptyArray() {
        int[] array = {};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{}, array);
    }

    @Test
    void shouldHandleNullArray() {
        assertDoesNotThrow(() -> MergeSort.sort(null));
    }

    @Test
    void shouldSortLargeValues() {
        int[] array = {Integer.MAX_VALUE, 0, Integer.MIN_VALUE, -1, 1};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}, array);
    }

    @Test
    void shouldSortOddLengthArray() {
        int[] array = {7, 3, 5, 1, 9};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, array);
    }

    @Test
    void shouldSortEvenLengthArray() {
        int[] array = {8, 4, 6, 2, 7, 1};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2, 4, 6, 7, 8}, array);
    }

    @Test
    void shouldSortArrayWhereAllElementsEqual() {
        int[] array = {5, 5, 5, 5, 5};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{5, 5, 5, 5, 5}, array);
    }

    @Test
    void shouldSortArrayWithRepeatedBoundaryValues() {
        int[] array = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        MergeSort.sort(array);
        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE},
                array
        );
    }

    @Test
    void shouldNotChangeSortedArrayWithDuplicates() {
        int[] array = {1, 1, 2, 2, 3, 3};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 1, 2, 2, 3, 3}, array);
    }

    @Test
    void shouldSortLongRandomLikeArray() {
        int[] array = {12, 4, 7, 3, 15, 8, 1, 10, 6, 2, 14, 5, 9, 11, 13};
        MergeSort.sort(array);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, array);
    }
}