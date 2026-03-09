package org.example;

import java.util.ArrayList;
import java.util.List;

public final class ReferenceMergeSort {

    private ReferenceMergeSort() {}

    public static MergeSort.Result sortWithTrace(int[] input) {
        if (input == null) throw new IllegalArgumentException("input == null");

        int[] a = input.clone();
        List<TraceEvent> t = new ArrayList<>();
        t.add(new TraceEvent(TracePoint.ENTER_SORT, 0, a.length - 1, -1));

        sortRec(a, 0, a.length - 1, t);
        return new MergeSort.Result(a, t);
    }

    private static void sortRec(int[] a, int l, int r, List<TraceEvent> t) {
        if (l >= r) {
            t.add(new TraceEvent(TracePoint.BASE_CASE, l, r, -1));
            return;
        }

        int m = l + (r - l) / 2;
        t.add(new TraceEvent(TracePoint.SPLIT, l, m, r));

        sortRec(a, l, m, t);
        sortRec(a, m + 1, r, t);
        merge(a, l, m, r, t);
    }

    private static void merge(int[] a, int l, int m, int r, List<TraceEvent> t) {
        t.add(new TraceEvent(TracePoint.ENTER_MERGE, l, m, r));

        int n1 = m - l + 1;
        int n2 = r - m;

        int[] left = new int[n1];
        int[] right = new int[n2];

        System.arraycopy(a, l, left, 0, n1);
        System.arraycopy(a, m + 1, right, 0, n2);

        int i = 0, j = 0, k = l;

        while (i < n1 && j < n2) {
            t.add(new TraceEvent(TracePoint.COMPARE, l + i, (m + 1) + j, -1));

            if (left[i] <= right[j]) {
                a[k++] = left[i++];
                t.add(new TraceEvent(TracePoint.TAKE_LEFT, i - 1, j, k - 1));
            } else {
                a[k++] = right[j++];
                t.add(new TraceEvent(TracePoint.TAKE_RIGHT, i, j - 1, k - 1));
            }
        }

        while (i < n1) {
            a[k++] = left[i++];
            t.add(new TraceEvent(TracePoint.DRAIN_LEFT, i - 1, -1, k - 1));
        }

        while (j < n2) {
            a[k++] = right[j++];
            t.add(new TraceEvent(TracePoint.DRAIN_RIGHT, -1, j - 1, k - 1));
        }

        t.add(new TraceEvent(TracePoint.WRITE_BACK, l, r, -1));
    }
}