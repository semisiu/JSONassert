package org.skyscreamer.jsonassert.diff.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.internal.Diff;
import org.assertj.core.util.diff.Delta;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DiffViewer {

    public static String diff(Object expected, Object actual) {
        try {
            return new DiffViewer().printDiff(expected, actual);
        } catch (IOException e) {
            return "Unable to generate diff view. " + e.getMessage();
        }
    }

    private String printDiff(Object expected, Object actual) throws IOException {
        ImmutablePair<Integer, Iterable<String>> expectedPair = spiltJsonByNewLineAndFixedLength(expected);
        List<String> expectedList2 = Lists.newArrayList(expectedPair.getValue().iterator());

        ImmutablePair<Integer, Iterable<String>> actualPair = spiltJsonByNewLineAndFixedLength(actual);
        List<String> actualList2 = Lists.newArrayList(actualPair.getValue().iterator());

        List<Delta<String>> diff = prepareDiff(expectedList2, actualList2);
        List<Delta<String>> reversed = ImmutableList.copyOf(diff).reverse();
        for (Delta<String> delta : reversed) {
            int deltaDiff = delta.getOriginal().getLines().size() - delta.getRevised().getLines().size();
            if (deltaDiff > 0) {
                for (int i = 0; i < Math.abs(deltaDiff); ++i) {
                    expectedList2.add(delta.getRevised().getPosition() + 1, "");
                }
            }
            if (deltaDiff < 0) {
                for (int i = 0; i < Math.abs(deltaDiff); ++i) {
                    actualList2.add(delta.getOriginal().getPosition() + 1, "");
                }
            }
        }
        return generateFormattedView(expectedPair.getKey(), expectedList2.iterator(), actualPair.getKey(), actualList2.iterator());
    }

    private List<Delta<String>> prepareDiff(List<String> expectedList2, List<String> actualList2) throws IOException {
        InputStream expectedInputStream = IOUtils.toInputStream(expectedList2.stream().collect(Collectors.joining("\n")), "UTF-8");
        InputStream actualInputStream = IOUtils.toInputStream(actualList2.stream().collect(Collectors.joining("\n")), "UTF-8");
        return new Diff().diff(expectedInputStream, actualInputStream);
    }

    private String generateFormattedView(Integer expectedMaxLength,
                                         Iterator<String> expectedIterator,
                                         Integer actualMaxLength,
                                         Iterator<String> actualIterator) {
        List<String> prettyTable = new LinkedList<>();
        String format = "%-" + (expectedMaxLength + 1) + "s| %-" + (actualMaxLength + 1) + "s";
        String formatWithChange = "%-" + (expectedMaxLength + 1) + "s* %-" + (actualMaxLength + 1) + "s";
        prettyTable.add(String.format(format, "Expected", "Actual"));
        prettyTable.add(StringUtils.repeat("-", expectedMaxLength + actualMaxLength + 3));
        while (expectedIterator.hasNext() || actualIterator.hasNext()) {
            String left = expectedIterator.hasNext() ? expectedIterator.next() : "";
            String right = actualIterator.hasNext() ? actualIterator.next() : "";
            prettyTable.add(String.format(StringUtils.stripEnd(left, ",").equals(StringUtils.stripEnd(right, ",")) ? format : formatWithChange, left, right));
        }
        return Joiner.on("\n").join(prettyTable);
    }

    private ImmutablePair<Integer, Iterable<String>> spiltJsonByNewLineAndFixedLength(Object json) throws IOException {
        Splitter splitter = Splitter.on("\n");
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        LinkedList<String> fixedByLength = new LinkedList<>();
        String jsonAsString = mapper.writeValueAsString(mapper.readValue((String) json, Object.class));
        int maxLineLength = 50;
        Iterable<String> splitted = splitter.split(jsonAsString);
        int maxLineLengthInJson = 0;
        for (String s : splitted) {
            if (s.length() > maxLineLength) {
                fixedByLength.addAll(Splitter.fixedLength(maxLineLength).splitToList(s));
                maxLineLengthInJson = maxLineLength;
            } else {
                fixedByLength.add(s);
                if (s.length() > maxLineLengthInJson) {
                    maxLineLengthInJson = s.length();
                }
            }
        }
        return ImmutablePair.of(maxLineLengthInJson, fixedByLength);
    }

}
