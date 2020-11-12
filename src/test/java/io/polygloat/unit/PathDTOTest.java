package io.polygloat.unit;

import io.polygloat.dtos.PathDTO;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathDTOTest {

    private static final String testFullPath = "item1.item2.item1.item1.last";
    private LinkedList<String> testList = new LinkedList<>(Arrays.asList(testFullPath.split("\\.", 0)));

    public LinkedList<String> getTestList() {
        return new LinkedList<>(testList);
    }

    @Test
    void testFromFullPath() {
        PathDTO pathDTO = PathDTO.fromFullPath("item1.item2.item1.item1.last");
        assertBasicPath(pathDTO);
    }

    @Test
    void testFromFullPathList() {
        PathDTO pathDTO = PathDTO.fromFullPath(testList);
        assertBasicPath(pathDTO);
    }

    @Test
    void testFromNamePath() {
        LinkedList<String> testList = getTestList();
        String name = testList.removeLast();
        PathDTO pathDTO = PathDTO.fromPathAndName(String.join(".", testList), name);
        assertThat(pathDTO.getName()).isEqualTo(name);
        assertThat(pathDTO.getFullPath()).isEqualTo(getTestList());
    }

    @Test
    void testFromNamePathList() {
        LinkedList<String> testList = getTestList();
        String name = testList.removeLast();
        PathDTO pathDTO = PathDTO.fromPathAndName(testList, name);
        assertThat(pathDTO.getName()).isEqualTo(name);
        assertThat(pathDTO.getFullPath()).isEqualTo(getTestList());
    }

    @Test
    void escaping() {
        String fullPath = "aaa.aaa\\.aaaa.a";
        var testList = PathDTO.fromFullPath(fullPath).getFullPath();
        assertThat(testList).isEqualTo(List.of("aaa","aaa.aaaa", "a"));
        assertThat(PathDTO.fromFullPath(testList).getFullPathString()).isEqualTo(fullPath);
    }

    void assertBasicPath(PathDTO pathDTO) {
        assertThat(pathDTO.getFullPath()).isEqualTo(testList);
        LinkedList<String> testList = getTestList();
        String last = testList.removeLast();
        assertThat(pathDTO.getPath()).isEqualTo(testList);
        assertThat(pathDTO.getName()).isEqualTo(last);
    }

}
