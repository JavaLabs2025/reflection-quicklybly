package org.example.generator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PackageUtilsTest {

    @TempDir
    private Path tempDir;

    private URL tempUrl;

    private final ByteBuddy byteBuddy = new ByteBuddy();
    private final String packageName = "org.example.kek";

    @BeforeEach
    void setUp() throws IOException {

        saveClassUsingByteBuddy(packageName + ".Service1");
        saveClassUsingByteBuddy(packageName + ".Service2");
        saveClassUsingByteBuddy(packageName + ".utils.Helper");

        tempUrl = tempDir.toUri().toURL();
    }

    private void saveClassUsingByteBuddy(String name) throws IOException {
        try (DynamicType object = byteBuddy.subclass(Object.class).name(name).make()) {
            object.saveIn(tempDir.toFile());
        }
    }

    @Test
    void shouldFindAllImpl() throws IOException, URISyntaxException {
        try (var classLoader = new URLClassLoader(
                new URL[]{tempUrl},
                Thread.currentThread().getContextClassLoader()
        )
        ) {
            Set<Class<?>> result = PackageUtils.getClassesInPackage(packageName, classLoader);

            assertThat(result.size()).isEqualTo(3);
            List.of(
                    packageName + ".Service1",
                    packageName + ".Service2",
                    packageName + ".utils.Helper"
            ).forEach(name -> assertThat(
                            result.stream().anyMatch(c -> c.getName().equals(name))
                    ).isTrue()
            );
        }
    }
}
