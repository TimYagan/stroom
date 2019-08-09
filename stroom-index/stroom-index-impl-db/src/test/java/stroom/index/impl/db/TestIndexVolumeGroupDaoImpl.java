package stroom.index.impl.db;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.assertj.core.data.Offset;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.DataChangedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import stroom.index.impl.IndexVolumeGroupDao;
import stroom.index.shared.IndexVolumeGroup;
import stroom.util.AuditUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestIndexVolumeGroupDaoImpl {
    private static IndexVolumeGroupDao indexVolumeGroupDao;

    @BeforeAll
    static void beforeAll() {
        final Injector injector = Guice.createInjector(new IndexDbModule(), new TestModule());
        indexVolumeGroupDao = injector.getInstance(IndexVolumeGroupDao.class);
    }

    @Test
    void testCreate() {
        // Given
        final var groupName = TestData.createVolumeGroupName();

        // When
        final var group = createGroup(groupName);

        // Then
        assertThat(group.getName()).isEqualTo(groupName);
    }


    @Test
    void testUpdate() {
        // Given
        final var groupName = TestData.createVolumeGroupName();
        createGroup(groupName);
        final var newGroupName = TestData.createVolumeGroupName();
        final var reloadedGroup = indexVolumeGroupDao.get(groupName);

        // When
        reloadedGroup.setName(newGroupName);
        indexVolumeGroupDao.update(reloadedGroup);

        // Then
        final var updatedGroup = indexVolumeGroupDao.get(newGroupName);
        assertThat(updatedGroup).isNotNull();
        assertThat(updatedGroup.getName()).isEqualTo(newGroupName);
        assertThat(indexVolumeGroupDao.get(groupName)).isNull();
    }


    @Test
    void testDelete() {
        // Given
        final var groupName = TestData.createVolumeGroupName();
        final var group = createGroup(groupName);

        // When
        indexVolumeGroupDao.delete(group.getId());

        // Then
        final var deletedGroup = indexVolumeGroupDao.get(groupName);
        assertThat(deletedGroup).isNull();
    }

    @Test
    void testDeleteByName() {
        // Given
        final var groupName = TestData.createVolumeGroupName();
        final var group = createGroup(groupName);

        // When
        indexVolumeGroupDao.delete(groupName);

        // Then
        final var deletedGroup = indexVolumeGroupDao.get(groupName);
        assertThat(deletedGroup).isNull();
    }


    @Test
    void getNamesAndAll() {
        // Given
        final long now = System.currentTimeMillis();
        final Set<String> namesToDelete = IntStream.range(0, 3)
                .mapToObj(TestData::createVolumeGroupName).collect(Collectors.toSet());
        final Set<String> names = IntStream.range(0, 7)
                .mapToObj(TestData::createVolumeGroupName).collect(Collectors.toSet());

        // When
        namesToDelete.forEach(this::createGroup);
        names.forEach(this::createGroup);
        final List<String> allNames1 = indexVolumeGroupDao.getNames();
        final List<IndexVolumeGroup> allGroups1 = indexVolumeGroupDao.getAll();
        final List<IndexVolumeGroup> foundGroups1 = Stream.concat(namesToDelete.stream(), names.stream())
                .map(indexVolumeGroupDao::get)
                .collect(Collectors.toList());

        namesToDelete.forEach(indexVolumeGroupDao::delete);
        final List<String> allNames2 = indexVolumeGroupDao.getNames();
        final List<IndexVolumeGroup> allGroups2 = indexVolumeGroupDao.getAll();
        final List<IndexVolumeGroup> foundGroups2 = names.stream()
                .map(indexVolumeGroupDao::get)
                .collect(Collectors.toList());

        // Then
        assertThat(allNames1).containsAll(namesToDelete);
        assertThat(allNames1).containsAll(names);
        assertThat(foundGroups1.stream().map(IndexVolumeGroup::getName))
                .containsAll(namesToDelete);
        assertThat(foundGroups1.stream().map(IndexVolumeGroup::getName))
                .containsAll(names);

        assertThat(allNames2).doesNotContainAnyElementsOf(namesToDelete);
        assertThat(allNames2).containsAll(names);
        assertThat(allGroups2.stream().map(IndexVolumeGroup::getName))
                .doesNotContainAnyElementsOf(namesToDelete);
        assertThat(allGroups2.stream().map(IndexVolumeGroup::getName))
                .containsAll(names);

        final Consumer<IndexVolumeGroup> checkAuditFields = indexVolumeGroup -> {
            assertThat(indexVolumeGroup.getCreateUser())
                    .isEqualTo(TestModule.TEST_USER);
            assertThat(indexVolumeGroup.getUpdateUser())
                    .isEqualTo(TestModule.TEST_USER);
            assertThat(indexVolumeGroup.getCreateTimeMs())
                    .isCloseTo(now, Offset.offset(1000L));
            assertThat(indexVolumeGroup.getUpdateTimeMs())
                    .isCloseTo(now, Offset.offset(1000L));
        };
        assertThat(foundGroups1).allSatisfy(checkAuditFields);
        assertThat(foundGroups2).allSatisfy(checkAuditFields);
    }

    @Test
    void testCreateGetDelete() {
        // Given
        final Long now = System.currentTimeMillis();
        final String groupName = TestData.createVolumeGroupName();

        // When
        final IndexVolumeGroup created = createGroup(groupName);
        final IndexVolumeGroup retrieved = indexVolumeGroupDao.get(groupName);

        // Then
        assertThat(Stream.of(created, retrieved)).allSatisfy(i -> {
            assertThat(i).isNotNull();
            assertThat(i.getName()).isEqualTo(groupName);
            assertThat(i.getCreateUser()).isEqualTo(TestModule.TEST_USER);
            assertThat(i.getCreateTimeMs()).isCloseTo(now, Offset.offset(1000L));
        });

        indexVolumeGroupDao.delete(groupName);

        final IndexVolumeGroup retrievedAfterDelete = indexVolumeGroupDao.get(groupName);
        assertThat(retrievedAfterDelete).isNull();
    }

    @Test
    void testCreateWithDuplicateName() {
        // Given
        final Long now = System.currentTimeMillis();
        final String groupName = TestData.createVolumeGroupName();

        // When
        final IndexVolumeGroup createdOnce = createGroup(groupName);
        final Long createdTimeMs = createdOnce.getCreateTimeMs();
        assertThat(createdTimeMs).isCloseTo(now, Offset.offset(100L));
        final IndexVolumeGroup retrievedOnce = indexVolumeGroupDao.get(groupName);

        // Put some delay in it, so that the audit time fields will definitely be different
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final IndexVolumeGroup createdTwice = createGroup(groupName);
        final IndexVolumeGroup retrievedTwice = indexVolumeGroupDao.get(groupName);

        // Make sure they are all the same
        assertThat(Stream.of(createdOnce, retrievedOnce, createdTwice, retrievedTwice))
                .allSatisfy(i -> {
                    assertThat(i.getCreateTimeMs()).isEqualTo(createdTimeMs);
                    assertThat(i.getUpdateTimeMs()).isEqualTo(createdTimeMs);
                    assertThat(i.getCreateUser()).isEqualTo(TestModule.TEST_USER);
                    assertThat(i.getUpdateUser()).isEqualTo(TestModule.TEST_USER);
                });
    }

    private IndexVolumeGroup createGroup(final String name) {
        final IndexVolumeGroup indexVolumeGroup = new IndexVolumeGroup();
        indexVolumeGroup.setName(name);
        AuditUtil.stamp(TestModule.TEST_USER, indexVolumeGroup);
        return indexVolumeGroupDao.getOrCreate(indexVolumeGroup);
    }
}
