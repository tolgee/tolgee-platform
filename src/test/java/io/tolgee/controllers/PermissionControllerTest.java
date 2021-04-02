package io.tolgee.controllers;

import io.tolgee.ITest;
import io.tolgee.dtos.request.PermissionEditDto;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.Permission;
import io.tolgee.model.Repository;
import io.tolgee.model.UserAccount;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;

import static io.tolgee.assertions.Assertions.assertThat;
import static io.tolgee.fixtures.UniqueStringGenerationKt.generateUniqueString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PermissionControllerTest extends SignedInControllerTest implements ITest {

    @Test()
    void editPermissionTest() throws Exception {
        Repository base = dbPopulator.createBase(generateUniqueString());
        UserAccount user = dbPopulator.createUserIfNotExists(generateUniqueString());
        permissionService.grantFullAccessToRepo(user, base);

        entityManager.refresh(user);
        Permission permission = user.getPermissions().stream().findFirst().orElseThrow(NotFoundException::new);
        PermissionEditDto dto = PermissionEditDto.builder().permissionId(permission.getId()).type(Permission.RepositoryPermissionType.EDIT).build();
        performAuthPost("/api/permission/edit", dto).andExpect(status().isOk()).andReturn();
        assertThat(permissionService.findById(permission.getId()).getType()).isEqualTo(Permission.RepositoryPermissionType.EDIT);
    }


}
