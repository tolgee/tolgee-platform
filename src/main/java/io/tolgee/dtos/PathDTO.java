package io.tolgee.dtos;

import io.tolgee.helpers.TextHelper;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PathDTO {
    public static final char DELIMITER = '.';

    private LinkedList<String> fullPath = new LinkedList<>();

    private PathDTO() {
    }

    public static PathDTO fromFullPath(String fullPath) {
        PathDTO pathDTO = new PathDTO();
        pathDTO.add(TextHelper.splitOnNonEscapedDelimiter(fullPath, DELIMITER));
        return pathDTO;
    }

    public static PathDTO fromFullPath(List<String> path) {
        PathDTO pathDTO = new PathDTO();
        pathDTO.add(path);
        return pathDTO;
    }

    public static PathDTO fromPathAndName(String path, String name) {
        PathDTO pathDTO = new PathDTO();
        List<String> items = TextHelper.splitOnNonEscapedDelimiter(path, DELIMITER);
        if (path.isEmpty()) {
            items = Collections.emptyList();
        }
        pathDTO.add(items);
        pathDTO.add(name);
        return pathDTO;
    }

    public static PathDTO fromPathAndName(List<String> path, String name) {
        PathDTO pathDTO = new PathDTO();
        pathDTO.add(path);
        pathDTO.add(name);
        return pathDTO;
    }

    public String getName() {
        return fullPath.getLast();
    }

    public String getFullPathString() {
        return getFullPath().stream().map(i -> i.replaceAll("\\" + DELIMITER, "\\\\" + DELIMITER)).collect(Collectors.joining("."));
    }

    public List<String> getPath() {
        LinkedList<String> path = new LinkedList<>(this.fullPath);
        path.removeLast();
        return path;
    }

    public List<String> getFullPath() {
        return new LinkedList<>(this.fullPath);
    }

    private void add(String item) {
        this.fullPath.add(item);
    }

    private void add(List<String> list) {
        this.fullPath.addAll(list);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PathDTO pathDTO = (PathDTO) o;

        return getFullPath().equals(pathDTO.getFullPath());
    }

    @Override
    public int hashCode() {
        return getFullPath().hashCode();
    }

    public void setFullPath(LinkedList<String> fullPath) {
        this.fullPath = fullPath;
    }

    public String toString() {
        return "PathDTO(fullPath=" + this.getFullPath() + ")";
    }
}
