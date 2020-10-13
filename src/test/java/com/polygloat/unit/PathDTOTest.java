package com.polygloat.unit;

import com.polygloat.dtos.PathDTO;
import com.polygloat.exceptions.InvalidPathException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedList;

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


    void assertBasicPath(PathDTO pathDTO) {
        assertThat(pathDTO.getFullPath()).isEqualTo(testList);
        LinkedList<String> testList = getTestList();
        String last = testList.removeLast();
        assertThat(pathDTO.getPath()).isEqualTo(testList);
        assertThat(pathDTO.getName()).isEqualTo(last);
    }

    @Test
    void testValidation() {
        LinkedList<String> testList = getTestList();

        assertThatThrownBy(() -> {
            testList.addFirst("dfkjhdf.adfdf");
            PathDTO.fromFullPath(testList);
        }).isInstanceOf(InvalidPathException.class);

        testList.removeFirst();

        assertThatThrownBy(() -> {
            testList.add("");
            PathDTO.fromFullPath(testList);
        }).isInstanceOf(InvalidPathException.class);

    }

}
