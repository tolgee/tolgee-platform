package com.polygloat.controllers;

import com.polygloat.model.Repository;
import org.springframework.test.web.servlet.MvcResult;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExportControllerTest extends SignedInControllerTest {

    @Test
    public void exportZipJson() throws Exception {
        Repository repository = dbPopulator.populate(generateUniqueString());
        commitTransaction();
        MvcResult mvcResult = performGet("/api/repository/" + repository.getId() + "/export/jsonZip")
                .andExpect(status().isOk()).andDo(MvcResult::getAsyncResult).andReturn();
        mvcResult.getResponse();
        Map<String, Long> fileSizes = parseZip(mvcResult.getResponse().getContentAsByteArray());


        repository.getLanguages().forEach(l -> {
            String name = l.getAbbreviation() + ".json";
            assertThat(fileSizes).containsKey(name);
            //assertThat(fileSizes.get(name)).isGreaterThan(0);
        });
        //cleanup
        repositoryService.deleteRepository(repository.getId());
    }


    private Map<String, Long> parseZip(byte[] responseContent) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(responseContent);
        ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream);

        HashMap<String, Long> result = new HashMap<>();

        ZipEntry nextEntry;
        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            result.put(nextEntry.getName(), nextEntry.getSize());
        }

        return result;
    }
}